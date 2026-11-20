package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Soot: a thin black layer on block faces near a burning fire (grown by {@link Smoke}), mostly on the ceiling right
 * above it and on walls close by. Each hit makes it darker ({@link #STAGE} 1-3, shared by all faces of the block).
 * It never washes off; scraping it off (faster with a knife) gives the Soot item.
 */
public class SootBlock extends MultifaceBlock {
    public static final int MAX_STAGE = 3;
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 1, MAX_STAGE);
    private static final int CEILING = 6, WALL = 3;

    public SootBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(STAGE, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(STAGE);
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        float progress = super.getDestroyProgress(state, player, level, pos);
        return player.getMainHandItem().getItem() instanceof KnifeItem ? progress * 4 : progress;
    }

    /**
     * One bit of soot from a fire whose smoke rises from {@code start}: 2 in 3 on the ceiling over it (spread like a
     * tent, most right above), else on a wall up to {@link #WALL} blocks to the side. Nothing if there is no surface.
     */
    public static void deposit(ServerLevel level, BlockPos start, RandomSource random) {
        if (random.nextInt(3) < 2) {
            // triangular spread -2..2 around the column, then up to the first thing that stops smoke
            BlockPos p = start.offset(random.nextInt(3) + random.nextInt(3) - 2, 0, random.nextInt(3) + random.nextInt(3) - 2);
            for (int i = 0; i < CEILING && !blocks(level, p); i++, p = p.above()) {
                if (blocks(level, p.above())) {
                    add(level, p, Direction.UP);
                    return;
                }
            }
        } else {
            Direction d = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            BlockPos p = start.offset(0, random.nextInt(4) - 1, 0).relative(d.getClockWise(), random.nextInt(3) - 1);
            for (int i = 0; i < WALL; i++) {
                p = p.relative(d);
                if (blocks(level, p)) return;
                if (blocks(level, p.relative(d))) {
                    add(level, p, d);
                    return;
                }
            }
        }
    }

    /**
     * The hearth wears in: one spot within 2 blocks of the fire changes a step. Plants there burn away; grass, podzol,
     * mycelium and moss become dirt, dirt becomes coarse dirt, and then (or on stone, gravel, sand ...) the ground gets
     * a layer of soot and ash that darkens.
     */
    public static void scorch(ServerLevel level, BlockPos fire, RandomSource random) {
        int dx = random.nextInt(5) - 2, dz = random.nextInt(5) - 2;
        if (dx == 0 && dz == 0) return;
        BlockPos at = fire.offset(dx, 0, dz);
        BlockState here = level.getBlockState(at);
        if (here.is(BlockTags.REPLACEABLE_BY_TREES) || here.is(BlockTags.FLOWERS) || here.is(BlockTags.SAPLINGS)) {
            level.destroyBlock(at, false); // burnt up
            return;
        }
        if (!here.isAir() && !here.is(Mechanical.SOOT.get())) return;
        BlockPos ground = at.below();
        BlockState g = level.getBlockState(ground);
        if (g.is(Blocks.GRASS_BLOCK) || g.is(Blocks.PODZOL) || g.is(Blocks.MYCELIUM) || g.is(Blocks.MOSS_BLOCK)) level.setBlock(ground, Blocks.DIRT.defaultBlockState(), UPDATE_ALL);
        else if (g.is(Blocks.DIRT)) level.setBlock(ground, Blocks.COARSE_DIRT.defaultBlockState(), UPDATE_ALL);
        else add(level, at, Direction.DOWN);
    }

    private static boolean blocks(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.isAir() && !state.is(Mechanical.SOOT.get());
    }

    /**
     * Soot on the {@code face} side of the air at {@code pos}, or one stage darker if it is already there. It only darkens
     * once the soot beside it on the same surface is as dark: a lighter neighbour (or bare surface next to it) takes the
     * deposit instead, so a patch fades out over a block or two (3, 2, 1, none) instead of ending in a hard edge.
     */
    static void add(ServerLevel level, BlockPos pos, Direction face) {
        BlockState old = level.getBlockState(pos);
        SootBlock soot = Mechanical.SOOT.get();
        if (old.is(soot) && hasFace(old, face)) {
            int stage = old.getValue(STAGE);
            if (stage >= MAX_STAGE) return;
            java.util.List<BlockPos> lighter = new java.util.ArrayList<>(4);
            for (Direction d : Direction.values()) {
                if (d.getAxis() == face.getAxis()) continue;
                BlockPos n = pos.relative(d);
                if (stageOn(level, n, face) < stage) lighter.add(n);
            }
            // the chain ends: each step goes to a strictly lighter block
            if (!lighter.isEmpty()) add(level, lighter.get(level.getRandom().nextInt(lighter.size())), face);
            else level.setBlock(pos, old.setValue(STAGE, stage + 1), UPDATE_ALL);
            return;
        }
        BlockState state = soot.getStateForPlacement(old, level, pos, face); // null if that face can't hold it (not a full side)
        if (state != null) level.setBlock(pos, state, UPDATE_ALL);
    }

    /** How dark soot on {@code face} at {@code pos} is: 0 if it could go there but there is none, MAX_STAGE + 1 if it can't. */
    static int stageOn(ServerLevel level, BlockPos pos, Direction face) {
        BlockState state = level.getBlockState(pos);
        SootBlock soot = Mechanical.SOOT.get();
        if (state.is(soot) && hasFace(state, face)) return state.getValue(STAGE);
        if (!state.isAir() && !state.is(soot) || soot.getStateForPlacement(state, level, pos, face) == null) return MAX_STAGE + 1;
        return state.is(soot) ? state.getValue(STAGE) : 0; // a new face on soot that is already there shares its stage
    }

    /** Sweeping (Broom): one stage lighter, the lightest is swept away. True if there was soot. */
    public static boolean sweep(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(Mechanical.SOOT.get())) return false;
        int stage = state.getValue(STAGE);
        if (stage > 1) level.setBlock(pos, state.setValue(STAGE, stage - 1), UPDATE_ALL);
        else level.removeBlock(pos, false);
        return true;
    }
}
