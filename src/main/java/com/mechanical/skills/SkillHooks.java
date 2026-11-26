package com.mechanical.skills;

import static com.mechanical.Mechanical.MODID;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import com.mechanical.Butchery;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
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

    /** Player-placed ore and stone positions per dimension, so placing and re-breaking them pays nothing. */
    // shortcut: forgotten on server restart, persist in SavedData if players farm across restarts
    private static final Map<ResourceKey<Level>, LinkedHashSet<Long>> PLACED = new HashMap<>();
    private static final int MAX_PLACED = 20_000;

    static boolean paysMining(BlockState state) {
        return state.is(ORES) || state.is(BlockTags.BASE_STONE_OVERWORLD);
    }

    /** Ores 10, other natural stone (the mod's stones are in base_stone_overworld) 1; blocks a player placed pay nothing. */
    @SubscribeEvent
    static void onDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer p) || !paysMining(event.getState())) return;
        var placed = PLACED.get(event.getLevel().dimension());
        if (placed != null && placed.remove(event.getPos().asLong())) return;
        Skills.addXp(p, Skill.MINING, event.getState().is(ORES) ? 10 : 1);
        if (event.getState().is(ORES) && SkillPerks.MINING_ORE.roll(p)) SkillPerks.addExtra(event);
    }

    @SubscribeEvent
    static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !(event.getLevel() instanceof Level level)) return;
        BlockState state = event.getPlacedBlock();
        if (state.getBlock() instanceof CropBlock crop) {
            Skills.addXp(p, Skill.FARMING, 1);
            if (!crop.isMaxAge(state) && SkillPerks.FARM_GROW.roll(p)) level.setBlock(event.getPos(), crop.getStateForAge(crop.getAge(state) + 1), Block.UPDATE_ALL);
        }
        if (!paysMining(state)) return;
        var placed = PLACED.computeIfAbsent(level.dimension(), k -> new LinkedHashSet<>());
        placed.add(event.getPos().asLong());
        if (placed.size() > MAX_PLACED) {
            var it = placed.iterator();
            it.next();
            it.remove();
        }
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
