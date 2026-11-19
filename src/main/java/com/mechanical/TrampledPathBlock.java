package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.SnowyBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.lighting.LightEngine;

/**
 * Worn Grass and Trodden Path ({@link TrampledPaths}): a full grass-like block whose top shows bare earth running out
 * toward neighbouring path ground (NORTH..WEST, from the tag mechanical:path_connects) and fraying into grass elsewhere.
 * RECENT: walked on lately, so it does not grow back yet. PERSISTENT: a worldgen trail, never grows back.
 */
public class TrampledPathBlock extends SnowyBlock {
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH, EAST = BlockStateProperties.EAST,
            SOUTH = BlockStateProperties.SOUTH, WEST = BlockStateProperties.WEST;
    public static final BooleanProperty RECENT = BooleanProperty.create("recent");
    public static final BooleanProperty PERSISTENT = BlockStateProperties.PERSISTENT;
    /** Chance per random tick that RECENT wears off: about one in-game day (~18 random ticks). */
    static final float FORGET = 1 / 18F;
    /** Chance per random tick to grow back one stage once not RECENT: ~25 random ticks, about 1.4 in-game days. */
    static final float REGROW = 0.04F;

    public TrampledPathBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false)
                .setValue(WEST, false).setValue(RECENT, false).setValue(PERSISTENT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(NORTH, EAST, SOUTH, WEST, RECENT, PERSISTENT);
    }

    static BooleanProperty side(Direction d) {
        return switch (d) {
            case NORTH -> NORTH;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            default -> WEST;
        };
    }

    /** {@code state} with its four connections read from the neighbours (worldgen and placement: no updateShape there). */
    public static BlockState connect(BlockState state, BlockGetter level, BlockPos pos) {
        for (Direction d : Direction.Plane.HORIZONTAL)
            state = state.setValue(side(d), level.getBlockState(pos.relative(d)).is(TrampledPaths.CONNECTS));
        return state;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return connect(super.getStateForPlacement(context), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir,
            BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (dir.getAxis().isHorizontal()) return state.setValue(side(dir), neighbourState.is(TrampledPaths.CONNECTS));
        return super.updateShape(state, level, ticks, pos, dir, neighbourPos, neighbourState, random);
    }

    /** Like grass: dies to dirt under an opaque block or water. */
    static boolean canStayAlive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState above = level.getBlockState(pos.above());
        if (above.is(Blocks.SNOW) && above.getValue(SnowLayerBlock.LAYERS) == 1) return true;
        if (above.getFluidState().isFull()) return false;
        return LightEngine.getLightDampeningInto(state, above, Direction.UP, above.getLightDampening()) < 15;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!canStayAlive(state, level, pos)) {
            level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
        } else if (state.getValue(PERSISTENT)) {
            // worldgen trails stay
        } else if (state.getValue(RECENT)) {
            if (random.nextFloat() < FORGET) level.setBlock(pos, state.setValue(RECENT, false), UPDATE_CLIENTS | UPDATE_INVISIBLE);
        } else if (random.nextFloat() < REGROW && level.getMaxLocalRawBrightness(pos.above()) >= 9) {
            level.setBlockAndUpdate(pos, regrown(state));
        }
    }

    /** One stage back toward grass: Trodden Path to Worn Grass, Worn Grass to a Grass Block. */
    static BlockState regrown(BlockState state) {
        return state.is(TrampledPaths.TRODDEN_PATH) ? TrampledPaths.WORN_GRASS.get().withPropertiesOf(state)
                : Blocks.GRASS_BLOCK.defaultBlockState().setValue(SNOWY, state.getValue(SNOWY));
    }
}
