package com.mechanical;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ITEMS;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * The Fire Pit: a big 2x2 campfire in a ring of rocks (Stone Age). Crafted from 8 rock chunks round a Firewood and placed
 * on flat solid ground like a bed; lit with the Fire Drill, fed Firewood, 8 food slots on two spits. Burns twice as fast
 * as a campfire, lights all four blocks at 15, keeps monsters and predators {@link #REACH} times as far off and warms
 * {@link #REACH} times as much and as far; smokes three times as much, holds out longer in rain and wind, ages a camp twice as
 * fast and draws bigger raids (CampAge, people.Raids). Block: {@link FirePitBlock}, fire and food: {@link FirePitBlockEntity},
 * spits: client.FirePitRenderer. Data: scripts/gen_firepit.py, textures: scripts/gen_firepit_textures.py.
 */
public final class FirePit {
    public static final int SLOTS = 8;
    /** Firewood stacked on the pit, waiting to burn. Each piece is {@link Campfires#FIREWOOD_TICKS} of campfire burning. */
    public static final int STOCK_MAX = 16;
    /** Burns this many times as fast as a campfire (so the same firewood lasts half as long). */
    public static final int DRAIN = 2;
    /** The big ash bed glows longer than a campfire's (3 min). */
    public static final int EMBER_TICKS = 3600;
    /** Light, warmth and the distance monsters and predators keep, as a multiple of a campfire's. */
    public static final double REACH = 1.5;
    /** Indoor smoke and soot build up this many times as fast as from a campfire; the ground scorches twice as fast. */
    public static final float SMOKE = 3, SCORCH = 2;
    /** A burning Fire Pit makes the camp age this many times as fast. */
    public static final int CAMP_AGE = 2;
    /** Raids on a camp with a burning Fire Pit at dusk: this many extra raiders (cap raised by the same), and this chance multiplier. */
    public static final int RAID_EXTRA = 2;
    public static final float RAID_CHANCE = 1.5F;

    public static final DeferredBlock<FirePitBlock> FIRE_PIT = BLOCKS.registerBlock("fire_pit", FirePitBlock::new, p -> p
            .mapColor(MapColor.STONE).strength(1.5F).sound(SoundType.STONE).noOcclusion().pushReaction(PushReaction.IMMOVEABLE)
            .lightLevel(s -> s.getValue(FirePitBlock.LIT) ? 15 : s.getValue(FirePitBlock.EMBERS) ? 3 : 0));
    public static final DeferredItem<BlockItem> FIRE_PIT_ITEM = ITEMS.registerSimpleBlockItem("fire_pit", FIRE_PIT);
    public static final Supplier<BlockEntityType<FirePitBlockEntity>> FIRE_PIT_ENTITY = Mechanical.BLOCK_ENTITIES.register("fire_pit",
            () -> new BlockEntityType<>(FirePitBlockEntity::new, FIRE_PIT.get()));

    public static void init() {}

    public static boolean isPit(BlockState state) {
        return state.getBlock() instanceof FirePitBlock;
    }

    public static boolean isLit(BlockState state) {
        return isPit(state) && state.getValue(FirePitBlock.LIT);
    }

    /** Burning or glowing (smokes a Smoking Rack over it). */
    public static boolean smoulders(BlockState state) {
        return isPit(state) && (state.getValue(FirePitBlock.LIT) || state.getValue(FirePitBlock.EMBERS));
    }

    /** Unlit (cold or embers): the Fire Drill can light it. */
    public static boolean canLight(BlockState state) {
        return isPit(state) && !state.getValue(FirePitBlock.LIT);
    }

    /** The block entity of the pit that {@code pos} is part of, or null. */
    public static FirePitBlockEntity entity(Level level, BlockPos pos, BlockState state) {
        return isPit(state) && level.getBlockEntity(FirePitBlock.controller(state, pos)) instanceof FirePitBlockEntity pit ? pit : null;
    }

    private FirePit() {}
}
