package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
 * Hanging Rack (slaktställning): two posts two blocks high, a crossbeam and a wooden gambrel hanging from it. A hauled
 * carcass right-clicked onto it hangs head down by the hind legs ({@link CarcassEntity#hang}), its belly to the front
 * ({@code FACING}, towards whoever placed it). Needs the block above free. Breaking it drops the carcass.
 */
public class HangingRackBlock extends Block {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    /** Height of the gambrel hook above the rack's base, in blocks (the model's gambrel at y = 25 px). */
    public static final float HOOK = 25 / 16.0F;
    private static final VoxelShape SHAPE_X = Shapes.or(box(1, 0, 7, 3, 32, 9), box(13, 0, 7, 15, 32, 9), box(0, 28, 6.5, 16, 31, 9.5));
    private static final VoxelShape SHAPE_Z = Shapes.or(box(7, 0, 1, 9, 32, 3), box(7, 0, 13, 9, 32, 15), box(6.5, 28, 0, 9.5, 31, 16));

    public HangingRackBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // the frame stands two blocks high
        if (!context.getLevel().getBlockState(context.getClickedPos().above()).canBeReplaced()) return null;
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /** The beam runs across the front: along x when it faces north or south. */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? SHAPE_X : SHAPE_Z;
    }

    static boolean occupied(ServerLevel level, BlockPos pos) {
        return !level.getEntitiesOfClass(CarcassEntity.class, new AABB(pos).inflate(1, 3, 1), c -> pos.equals(c.hungAt())).isEmpty();
    }

    /** Hangs {@code carcass} (hauled by {@code player}) on the rack at {@code pos}, if it fits and the hook is free. */
    static void tryHang(ServerLevel level, BlockPos pos, BlockState state, CarcassEntity carcass, Player player) {
        if (carcass.isHuge()) player.sendOverlayMessage(Component.translatable("message.mechanical.hanging_rack.too_big"));
        else if (occupied(level, pos)) player.sendOverlayMessage(Component.translatable("message.mechanical.hanging_rack.occupied"));
        else {
            carcass.hang(pos, state.getValue(FACING));
            level.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 1.0F, 0.8F);
            level.playSound(null, pos, SoundEvents.SLIME_SQUISH, SoundSource.BLOCKS, 0.6F, 0.6F);
        }
    }
}
