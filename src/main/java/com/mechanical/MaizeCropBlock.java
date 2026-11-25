package com.mechanical;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Maize on farmland: a crop two blocks tall. The lower half is the crop (it ticks, grows by {@link Farming}'s rules and gives
 * the harvest); from {@link #TALL_FROM} it puts up an upper half that only shows the same age. A stalk with no room above
 * waits at {@link #TALL_FROM} - 1. Breaking either half takes the plant (the lower half's loot drops either way).
 */
public class MaizeCropBlock extends FarmCropBlock {
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final int TALL_FROM = 4;
    private static final VoxelShape[] LOWER = Block.boxes(7, age -> Block.column(12.0, 0.0, Math.min(16, 3 + age * 3)));
    private static final VoxelShape[] UPPER = Block.boxes(7, age -> Block.column(12.0, 0.0, Math.max(2, (age - 3) * 4)));

    public MaizeCropBlock(Supplier<Item> seed, Properties properties) {
        super(seed, properties);
        registerDefaultState(defaultBlockState().setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HALF);
    }

    public static boolean isUpper(BlockState state) {
        return state.getBlock() instanceof MaizeCropBlock && state.getValue(HALF) == DoubleBlockHalf.UPPER;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return (isUpper(state) ? UPPER : LOWER)[getAge(state)];
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return !isUpper(state) && super.isRandomlyTicking(state);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (isUpper(state)) return;
        if (getAge(state) == TALL_FROM - 1 && !roomAbove(level, pos)) return; // no room to grow tall
        super.randomTick(state, level, pos, random);
    }

    private boolean roomAbove(LevelReader level, BlockPos pos) {
        BlockState above = level.getBlockState(pos.above());
        return above.isAir() || above.is(this) || above.canBeReplaced() && above.getFluidState().isEmpty();
    }

    /** The lower half changed age: the upper half follows (put up, aged, or taken down). */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (isUpper(state)) return;
        BlockPos up = pos.above();
        BlockState above = level.getBlockState(up);
        if (getAge(state) >= TALL_FROM) {
            BlockState top = state.setValue(HALF, DoubleBlockHalf.UPPER);
            if (above != top && (above.is(this) || roomAbove(level, pos))) level.setBlock(up, top, Block.UPDATE_ALL);
        } else if (isUpper(above)) {
            level.setBlock(up, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (!isUpper(state)) return super.canSurvive(state, level, pos);
        BlockState below = level.getBlockState(pos.below());
        return below.is(this) && !isUpper(below) && getAge(below) >= TALL_FROM;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir,
            BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        if (isUpper(state) && dir == Direction.DOWN && neighbor.is(this) && !isUpper(neighbor) && getAge(neighbor) >= TALL_FROM)
            return state.setValue(AGE, getAge(neighbor));
        // a tall lower half whose top was broken goes too (its loot drops through the shape update)
        if (!isUpper(state) && dir == Direction.UP && getAge(state) >= TALL_FROM && !(neighbor.is(this) && isUpper(neighbor)))
            return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, level, ticks, pos, dir, neighborPos, neighbor, random);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, net.minecraft.world.level.block.BonemealSource source) {
        return !isUpper(state) && super.isValidBonemealTarget(level, pos, state, source);
    }
}
