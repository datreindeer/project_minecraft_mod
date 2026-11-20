package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Hanging Rack (slaktställning): two posts three blocks high, a crossbeam and a wooden gambrel hanging from it, built of
 * three blocks ({@link Part}, like a door) so each third has its own hitbox. A hauled carcass right-clicked onto any part
 * hangs head down by the hind legs from the bottom block's position ({@link CarcassEntity#hang}), its belly to the front
 * ({@code FACING}, towards whoever placed it). Needs the two blocks above free. Breaking any part removes the whole rack
 * (only the broken part drops the item) and drops the carcass.
 */
public class HangingRackBlock extends Block {
    public enum Part implements StringRepresentable {
        LOWER, MIDDLE, UPPER;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    /** Height of the gambrel hook above the rack's base, in blocks (the model's gambrel at y = 41 px). */
    public static final float HOOK = 41 / 16.0F;
    // per part, built facing north (beam along x): posts, and the crossbeam at 44-47 px in the top block
    private static final VoxelShape POSTS_X = Shapes.or(box(1, 0, 7, 3, 16, 9), box(13, 0, 7, 15, 16, 9));
    private static final VoxelShape POSTS_Z = Shapes.or(box(7, 0, 1, 9, 16, 3), box(7, 0, 13, 9, 16, 15));
    private static final VoxelShape TOP_X = Shapes.or(box(1, 0, 7, 3, 13, 9), box(13, 0, 7, 15, 13, 9), box(0, 12, 6.5, 16, 15, 9.5));
    private static final VoxelShape TOP_Z = Shapes.or(box(7, 0, 1, 9, 13, 3), box(7, 0, 13, 9, 13, 15), box(6.5, 12, 0, 9.5, 15, 16));

    public HangingRackBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, Part.LOWER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // the frame stands three blocks high
        BlockPos pos = context.getClickedPos();
        if (!context.getLevel().isInsideBuildHeight(pos.above(2)) || !context.getLevel().getBlockState(pos.above()).canBeReplaced(context)
                || !context.getLevel().getBlockState(pos.above(2)).canBeReplaced(context)) return null;
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity by, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(PART, Part.MIDDLE), Block.UPDATE_ALL);
        level.setBlock(pos.above(2), state.setValue(PART, Part.UPPER), Block.UPDATE_ALL);
    }

    /** A part whose neighbour part is gone goes too (no drop: only the part that was broken drops the rack). */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
            BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        Part part = state.getValue(PART);
        Part expected = direction == Direction.UP && part != Part.UPPER ? Part.values()[part.ordinal() + 1]
                : direction == Direction.DOWN && part != Part.LOWER ? Part.values()[part.ordinal() - 1] : null;
        if (expected != null && !(neighbourState.is(this) && neighbourState.getValue(PART) == expected
                && neighbourState.getValue(FACING) == state.getValue(FACING))) return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    /** The bottom block of the rack that {@code state} at {@code pos} is part of. */
    static BlockPos base(BlockPos pos, BlockState state) {
        return pos.below(state.getValue(PART).ordinal());
    }

    /** The beam runs across the front: along x when it faces north or south. */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        boolean x = state.getValue(FACING).getAxis() == Direction.Axis.Z;
        return state.getValue(PART) == Part.UPPER ? (x ? TOP_X : TOP_Z) : x ? POSTS_X : POSTS_Z;
    }

    public static boolean occupied(ServerLevel level, BlockPos pos) {
        return !level.getEntitiesOfClass(CarcassEntity.class, new AABB(pos).inflate(1, 3, 1), c -> pos.equals(c.hungAt())).isEmpty();
    }

    /** Hangs {@code carcass} (hauled by {@code player}) on the rack at {@code pos}, if it fits and the hook is free. */
    static void tryHang(ServerLevel level, BlockPos clicked, BlockState state, CarcassEntity carcass, Player player) {
        BlockPos pos = base(clicked, state);
        if (carcass.isHuge()) player.sendOverlayMessage(Component.translatable("message.mechanical.hanging_rack.too_big"));
        else if (occupied(level, pos)) player.sendOverlayMessage(Component.translatable("message.mechanical.hanging_rack.occupied"));
        else {
            carcass.hang(pos, state.getValue(FACING));
            level.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 1.0F, 0.8F);
            level.playSound(null, pos, SoundEvents.SLIME_SQUISH, SoundSource.BLOCKS, 0.6F, 0.6F);
        }
    }
}
