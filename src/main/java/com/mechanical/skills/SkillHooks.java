package com.mechanical.skills;

import static com.mechanical.Mechanical.MODID;

import java.util.ArrayList;
import java.util.LinkedHashSet;

import com.mojang.serialization.Codec;

import com.mechanical.Butchery;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/** XP from the vanilla-style events; the mod's own crafts call {@link Skills#addXp} where they happen. */
@EventBusSubscriber(modid = MODID)
public final class SkillHooks {
    private static final TagKey<Block> ORES = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "ores"));

    private SkillHooks() {}

    /** Player-placed ore and stone positions per level, so placing and re-breaking them pays nothing, also after a restart. */
    static final class Placed extends SavedData {
        static final int MAX = 20_000;
        static final Codec<Placed> CODEC = Codec.LONG.listOf().xmap(Placed::new, d -> new ArrayList<>(d.set));
        static final SavedDataType<Placed> TYPE = new SavedDataType<>(Identifier.fromNamespaceAndPath(MODID, "placed_blocks"), () -> new Placed(java.util.List.of()), CODEC);
        private final LinkedHashSet<Long> set;

        Placed(java.util.List<Long> positions) {
            set = new LinkedHashSet<>(positions);
        }

        static Placed get(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(TYPE);
        }

        void add(BlockPos pos) {
            set.add(pos.asLong());
            if (set.size() > MAX) {
                var it = set.iterator();
                it.next();
                it.remove();
            }
            setDirty();
        }

        boolean remove(BlockPos pos) {
            boolean had = set.remove(pos.asLong());
            if (had) setDirty();
            return had;
        }

        boolean has(BlockPos pos) {
            return set.contains(pos.asLong());
        }
    }

    static boolean paysMining(BlockState state) {
        return state.is(ORES) || state.is(BlockTags.BASE_STONE_OVERWORLD);
    }

    /** Ores 10, other natural stone (the mod's stones are in base_stone_overworld) 1; blocks a player placed pay nothing. */
    @SubscribeEvent
    static void onDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer p) || !paysMining(event.getState())) return;
        if (Placed.get(event.getLevel()).remove(event.getPos())) return;
        boolean ore = event.getState().is(ORES);
        // silk touch gives the ore block back, which can be placed and mined again: no XP, no extra
        if (ore && event.getDrops().stream().anyMatch(d -> d.getItem().getItem() == event.getState().getBlock().asItem())) return;
        Skills.addXp(p, Skill.MINING, ore ? 10 : 1);
        if (ore && SkillPerks.MINING_ORE.roll(p)) SkillPerks.addExtra(event);
    }

    /** Stone and cobble made by lava meeting water pay nothing either. */
    @SubscribeEvent
    static void onFluidPlace(BlockEvent.FluidPlaceBlockEvent event) {
        if (event.getLevel() instanceof Level level) remember(level, event.getPos());
    }

    @SubscribeEvent
    static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !(event.getLevel() instanceof Level level)) return;
        BlockState state = event.getPlacedBlock();
        if (state.getBlock() instanceof CropBlock crop) {
            if (!crop.isMaxAge(state) && SkillPerks.FARM_GROW.roll(p)) level.setBlock(event.getPos(), crop.getStateForAge(crop.getAge(state) + 1), Block.UPDATE_ALL);
        }
        if (!paysMining(state)) return;
        remember(level, event.getPos());
    }

    static void remember(Level level, BlockPos pos) {
        if (level instanceof ServerLevel sl) Placed.get(sl).add(pos);
    }

    @SubscribeEvent
    static void onFished(ItemFishedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        Skills.addXp(p, Skill.FISHING, 15);
        // the event's drop list is a copy, so the extra fish goes straight to the player
        if (SkillPerks.FISH_EXTRA.roll(p)) SkillPerks.give(p, SkillPerks.extra(event.getDrops()));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer p && Butchery.leavesCarcass(event.getEntity())) Skills.addXp(p, Skill.HUNTING, 25);
    }
}
