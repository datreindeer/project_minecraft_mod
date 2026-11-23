package com.mechanical;

import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Planting on the half steps of the hills (worldgen.SmoothGround's Grass Slab, Dirt Slab, Sand Slab ...): right-clicking
 * the top of a soil slab with a plant (any flower, bush, sapling or mushroom item, vanilla or mod) first makes the slab the
 * full block it is half of, so the plant stands on solid ground instead of floating or being refused. Only when the plant
 * could then grow there; otherwise the slab stays as it was.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class SlabPlanting {
    private static Map<Block, Block> full;

    /** Soil slab -> the full block it is half of. */
    static Map<Block, Block> full() {
        if (full == null) full = Map.ofEntries(Map.entry(EarthBlocks.GRASS_SLAB.get(), Blocks.GRASS_BLOCK), Map.entry(EarthBlocks.DIRT_SLAB.get(), Blocks.DIRT),
                Map.entry(EarthBlocks.COARSE_DIRT_SLAB.get(), Blocks.COARSE_DIRT), Map.entry(EarthBlocks.PODZOL_SLAB.get(), Blocks.PODZOL),
                Map.entry(EarthBlocks.MOSS_SLAB.get(), Blocks.MOSS_BLOCK), Map.entry(EarthBlocks.MUD_SLAB.get(), Blocks.MUD),
                Map.entry(EarthBlocks.PEAT_SLAB.get(), EarthBlocks.PEAT.get()), Map.entry(EarthBlocks.LOESS_SLAB.get(), EarthBlocks.LOESS.get()),
                Map.entry(EarthBlocks.TURF_SLAB.get(), EarthBlocks.TURF.get()), Map.entry(EarthBlocks.SAND_SLAB.get(), Blocks.SAND),
                Map.entry(EarthBlocks.RED_SAND_SLAB.get(), Blocks.RED_SAND), Map.entry(EarthBlocks.GRAVEL_SLAB.get(), Blocks.GRAVEL),
                Map.entry(EarthBlocks.BEACH_PEBBLE_SLAB.get(), EarthBlocks.BEACH_PEBBLES.get()),
                Map.entry(EarthBlocks.BURNT_SOIL_SLAB.get(), com.mechanical.worldgen.ScorchedBarrens.BURNT_SOIL.get()));
        return full;
    }

    private SlabPlanting() {}

    /** Makes the soil slab at soil a full block if plant could then stand on it; true if it did. */
    public static boolean fill(Level level, BlockPos soil, BlockState plant) {
        BlockState was = level.getBlockState(soil);
        Block whole = full().get(was.getBlock());
        if (whole == null || !level.getBlockState(soil.above()).canBeReplaced()) return false;
        level.setBlock(soil, whole.defaultBlockState(), Block.UPDATE_ALL);
        if (plant.canSurvive(level, soil.above())) return true;
        level.setBlock(soil, was, Block.UPDATE_ALL);
        return false;
    }

    @SubscribeEvent
    static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getFace() != Direction.UP || !(event.getItemStack().getItem() instanceof BlockItem item)
                || !(item.getBlock() instanceof VegetationBlock)) return;
        fill(event.getLevel(), event.getPos(), item.getBlock().defaultBlockState());
    }
}
