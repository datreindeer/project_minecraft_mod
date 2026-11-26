package com.mechanical.skills;

import static com.mechanical.Mechanical.MODID;

import com.mechanical.Butchery;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
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

    /** Ores 10, other natural stone (the mod's stones are in base_stone_overworld) 1. */
    @SubscribeEvent
    static void onDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer p)) return;
        if (event.getState().is(ORES)) Skills.addXp(p, Skill.MINING, 10);
        else if (event.getState().is(BlockTags.BASE_STONE_OVERWORLD)) Skills.addXp(p, Skill.MINING, 1);
    }

    @SubscribeEvent
    static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && event.getPlacedBlock().getBlock() instanceof CropBlock) Skills.addXp(p, Skill.FARMING, 1);
    }

    @SubscribeEvent
    static void onFished(ItemFishedEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) Skills.addXp(p, Skill.FISHING, 15);
    }

    @SubscribeEvent
    static void onDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer p && Butchery.leavesCarcass(event.getEntity())) Skills.addXp(p, Skill.HUNTING, 25);
    }
}
