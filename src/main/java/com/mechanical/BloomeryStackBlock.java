package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A section of the Bloomery's fire-brick shaft. TOP (no stack above it): the open mouth where the charge goes in and the
 * smoke comes out; lit, the charge glows in it. LIT is set by the hearth ({@link BloomeryBlockEntity}).
 */
public class BloomeryStackBlock extends Block {
    public static final BooleanProperty TOP = BooleanProperty.create("top");
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final VoxelShape OPEN = Shapes.join(Shapes.block(), box(4, 10, 4, 12, 16, 12), BooleanOp.ONLY_FIRST);

    public BloomeryStackBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(TOP, true).setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TOP, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(TOP, !context.getLevel().getBlockState(context.getClickedPos().above()).is(this));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (direction == Direction.UP) return state.setValue(TOP, !neighbourState.is(this));
        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(TOP) ? OPEN : Shapes.block();
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT) || !state.getValue(TOP)) return;
        double x = pos.getX() + 0.35 + random.nextDouble() * 0.3, z = pos.getZ() + 0.35 + random.nextDouble() * 0.3;
        level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, pos.getY() + 1.0, z, 0, 0.07, 0);
        if (random.nextInt(3) == 0) level.addParticle(ParticleTypes.FLAME, x, pos.getY() + 0.95, z, 0, 0.03, 0);
        if (random.nextInt(8) == 0) level.addParticle(ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 0, 0, 0);
    }
}
