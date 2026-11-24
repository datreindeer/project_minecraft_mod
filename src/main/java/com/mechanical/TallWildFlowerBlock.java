package com.mechanical;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** A two-block wild flower (Great Mullein, Desert Candle) with a season like {@link WildFlowerBlock}; the lower half keeps both in step. */
public class TallWildFlowerBlock extends DoublePlantBlock {
    private final WildFlowerBlock.Bloom bloom;
    private final boolean dry;

    public TallWildFlowerBlock(WildFlowerBlock.Bloom bloom, boolean dry, Properties properties) {
        super(properties);
        this.bloom = bloom;
        this.dry = dry;
        registerDefaultState(defaultBlockState().setValue(WildFlowerBlock.BLOOMING, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WildFlowerBlock.BLOOMING);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return super.mayPlaceOn(state, level, pos) || dry && Flora.dryGround(state);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState s = super.getStateForPlacement(context);
        return s == null ? null : s.setValue(WildFlowerBlock.BLOOMING, bloom.now(context.getLevel(), context.getClickedPos()));
    }

    /** The top half blooms like the bottom one (vanilla puts a default top). */
    @Override
    public void setPlacedBy(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.@Nullable LivingEntity by,
            net.minecraft.world.item.ItemStack stack) {
        level.setBlockAndUpdate(pos.above(), copyWaterloggedFrom(level, pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER)));
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean now = bloom.now(level, pos);
        if (state.getValue(WildFlowerBlock.BLOOMING) == now) return;
        level.setBlock(pos, state.setValue(WildFlowerBlock.BLOOMING, now), Block.UPDATE_CLIENTS);
        BlockState top = level.getBlockState(pos.above());
        if (top.is(this)) level.setBlock(pos.above(), top.setValue(WildFlowerBlock.BLOOMING, now), Block.UPDATE_CLIENTS);
    }
}
