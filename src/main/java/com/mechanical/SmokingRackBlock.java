package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A wooden frame to hang up to {@link #MAX} strips of raw meat or fish on (right-click), over or next to a fire. In smoke
 * (a lit or glowing campfire up to 2 blocks below or right beside it, or a smoky room, see {@link Smoke}) they turn into
 * Smoked Meat / Smoked Fish after {@link SmokingRackBlockEntity#SMOKE_SECONDS}; without smoke they spoil as usual. Empty
 * hand takes everything off. The blockstate only shows what hangs there; the items themselves live in the block entity.
 */
public class SmokingRackBlock extends Block implements EntityBlock {
    public enum Content implements StringRepresentable {
        EMPTY, RAW_MEAT, RAW_FISH, SMOKED_MEAT, SMOKED_FISH, ROTTEN;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static final int MAX = 4;
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<Content> CONTENT = EnumProperty.create("content", Content.class);
    public static final IntegerProperty STRIPS = IntegerProperty.create("strips", 0, MAX);
    private static final VoxelShape SHAPE_NS = box(0, 0, 5, 16, 16, 11), SHAPE_EW = box(5, 0, 0, 11, 16, 16);

    public SmokingRackBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(CONTENT, Content.EMPTY).setValue(STRIPS, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CONTENT, STRIPS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? SHAPE_NS : SHAPE_EW;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        Content raw = stack.is(Preserving.SMOKABLE_MEAT) ? Content.RAW_MEAT : stack.is(Preserving.SMOKABLE_FISH) ? Content.RAW_FISH : null;
        Content now = state.getValue(CONTENT);
        if (raw == null || state.getValue(STRIPS) >= MAX || now != Content.EMPTY && now != raw) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SmokingRackBlockEntity rack) {
            rack.hang(stack.consumeAndReturn(1, player), level.getGameTime());
            level.setBlock(pos, state.setValue(CONTENT, raw).setValue(STRIPS, state.getValue(STRIPS) + 1), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.8F, 0.9F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(STRIPS) == 0) return InteractionResult.PASS;
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SmokingRackBlockEntity rack) {
            for (ItemStack taken : rack.takeAll()) player.getInventory().placeItemBackInInventory(taken, Prediction.SERVER_ONLY);
            level.setBlock(pos, state.setValue(CONTENT, Content.EMPTY).setValue(STRIPS, 0), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 0.8F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    /** Smoke reaches the rack: a burning or glowing campfire up to 2 below it or right beside it, or a smoky room. */
    public static boolean inSmoke(Level level, BlockPos pos) {
        if (smoulders(level.getBlockState(pos.below())) || smoulders(level.getBlockState(pos.below(2)))) return true;
        for (Direction d : Direction.Plane.HORIZONTAL) if (smoulders(level.getBlockState(pos.relative(d)))) return true;
        return Smoke.smokeAt(level, pos) >= 0.3F;
    }

    private static boolean smoulders(BlockState state) {
        return CampfireBlock.isLitCampfire(state) || Campfires.isEmbers(state);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SmokingRackBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != Preserving.SMOKING_RACK_ENTITY.get()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<SmokingRackBlockEntity>) (l, pos, s, rack) -> rack.serverTick((ServerLevel) l, pos, s);
    }
}
