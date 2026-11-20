package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Farmland fertility 0-3, a state added to vanilla farmland by FarmlandBlockMixin. Kept apart from {@link Farming} so
 * the mixin can load it while vanilla's blocks are still being built (no registry references here).
 */
public final class Fertility {
    public static final IntegerProperty FERTILITY = IntegerProperty.create("fertility", 0, 3);
    /** Crop Stakes stand on this farmland: a ripe crop on it is harvested and replanted with a right-click (Farming). */
    public static final BooleanProperty STAKED = BooleanProperty.create("staked");
    public static final int MAX = 3, NEW_FIELD = 2;
    /** A random tick on fallow farmland (nothing growing on it) restores 1 with this chance: ~once per Minecraft day. */
    private static final float FALLOW_CHANCE = 1 / 18F;
    /** Hot (ambient over this), dry weather and no water nearby: moisture drops a second step half the time. */
    private static final float DROUGHT_TEMP = 15, DROUGHT_CHANCE = 0.5F;

    public static int get(BlockState soil) {
        return soil.hasProperty(FERTILITY) ? soil.getValue(FERTILITY) : NEW_FIELD;
    }

    /** Changes the fertility of the farmland at {@code pos} by {@code delta}; false if it isn't farmland or nothing changed. */
    public static boolean add(Level level, BlockPos pos, int delta) {
        BlockState soil = level.getBlockState(pos);
        if (!soil.hasProperty(FERTILITY)) return false;
        int now = soil.getValue(FERTILITY), next = Math.clamp(now + delta, 0, MAX);
        if (next == now) return false;
        level.setBlock(pos, soil.setValue(FERTILITY, next), Block.UPDATE_CLIENTS);
        return true;
    }

    /** After vanilla's farmland random tick: fallow fields recover, hot dry ones dry out faster. */
    public static void farmlandTick(ServerLevel level, BlockPos pos, RandomSource random, boolean nearWater) {
        BlockState soil = level.getBlockState(pos);
        if (!(soil.getBlock() instanceof FarmlandBlock)) return; // dried back to dirt
        if (level.getBlockState(pos.above()).isAir() && soil.getValue(FERTILITY) < MAX && random.nextFloat() < FALLOW_CHANCE) {
            soil = soil.setValue(FERTILITY, soil.getValue(FERTILITY) + 1);
            level.setBlock(pos, soil, Block.UPDATE_CLIENTS);
        }
        int moisture = soil.getValue(FarmlandBlock.MOISTURE);
        if (moisture > 0 && !nearWater && !level.isRainingAt(pos.above()) && random.nextFloat() < DROUGHT_CHANCE
                && Temperature.ambient(level, pos.above()) > DROUGHT_TEMP) {
            level.setBlock(pos, soil.setValue(FarmlandBlock.MOISTURE, moisture - 1), Block.UPDATE_CLIENTS);
        }
    }

    private Fertility() {}
}
