package com.mechanical.wildlife.elkwight;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ENTITY_TYPES;
import static com.mechanical.Mechanical.ITEMS;
import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.Map;

import com.mechanical.Butchery;
import com.mechanical.Mechanical;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * The Elkwight (docs/elkwight-plan.md, phases 3-5): the elk-headed boss of the Scorched Barrens ruins ({@link Elkwight}),
 * his thrown axe ({@link ThrownElkAxe}), the axe he leaves (a heavy load, {@link ElkwightAxeEntity}, and its trophy
 * block {@link ElkwightAxeBlock}), and what his colossal carcass gives: Elkwight Hide (the Elkwight Cloak), Elkwight
 * Bones (the Bone Charm) and the Antler Crown, a wall trophy ({@link AntlerCrownBlock}). Data: scripts/gen_elkwight_boss.py,
 * textures: scripts/gen_elkwight_boss_textures.py.
 */
@EventBusSubscriber(modid = MODID)
public final class Elkwights {
    /** Knife tier needed to skin him: above iron and steel, the top tier (the Ancient Ritual Knife). */
    public static final int KNIFE_TIER = com.mechanical.KnifeItem.RITUAL;

    public static final DeferredHolder<EntityType<?>, EntityType<Elkwight>> ELKWIGHT = ENTITY_TYPES.registerEntityType("elkwight",
            Elkwight::new, MobCategory.MONSTER, b -> b.sized(1.2F, 3.6F).eyeHeight(3.2F).fireImmune().clientTrackingRange(12));
    public static final DeferredHolder<EntityType<?>, EntityType<ThrownElkAxe>> THROWN_AXE = ENTITY_TYPES.registerEntityType("thrown_elkwight_axe",
            ThrownElkAxe::new, MobCategory.MISC, b -> b.noLootTable().sized(1.2F, 1.2F).fireImmune().clientTrackingRange(10).updateInterval(1));
    public static final DeferredHolder<EntityType<?>, EntityType<ElkwightAxeEntity>> AXE = ENTITY_TYPES.registerEntityType("elkwight_axe",
            ElkwightAxeEntity::new, MobCategory.MISC, b -> b.noLootTable().sized(1.4F, 0.5F).fireImmune().clientTrackingRange(10).updateInterval(1));

    /** The axe stood up on its haft as a trophy: glows faintly green. Breaking it gives the heavy axe back. */
    public static final DeferredBlock<ElkwightAxeBlock> AXE_TROPHY = BLOCKS.registerBlock("elkwight_axe_trophy", ElkwightAxeBlock::new, p -> p
            .mapColor(MapColor.COLOR_GRAY).strength(5.0F, 1200.0F).sound(SoundType.NETHERITE_BLOCK).noOcclusion().lightLevel(s -> 4)
            .pushReaction(PushReaction.IMMOVEABLE));
    /** The Antler Crown, a wall trophy: placed against the side of a block. */
    public static final DeferredBlock<AntlerCrownBlock> ANTLER_CROWN = BLOCKS.registerBlock("elkwight_antler_crown", AntlerCrownBlock::new, p -> p
            .mapColor(MapColor.TERRACOTTA_WHITE).strength(1.5F).sound(SoundType.BONE_BLOCK).noOcclusion().noCollision());
    public static final DeferredItem<BlockItem> ANTLER_CROWN_ITEM = ITEMS.registerSimpleBlockItem("elkwight_antler_crown", ANTLER_CROWN,
            p -> p.rarity(Rarity.RARE));

    public static final DeferredItem<Item> HIDE = ITEMS.registerSimpleItem("elkwight_hide", p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> BONE = ITEMS.registerSimpleItem("elkwight_bone", p -> p.rarity(Rarity.UNCOMMON));

    /** Elkwight hide over the shoulders: as warm as fur (tag mechanical:fur_insulation) and armour 4, toughness 1. */
    public static final ArmorMaterial HIDE_MATERIAL = new ArmorMaterial(30, Map.of(ArmorType.HELMET, 2, ArmorType.CHESTPLATE, 4,
            ArmorType.LEGGINGS, 3, ArmorType.BOOTS, 2, ArmorType.BODY, 4), 14, SoundEvents.ARMOR_EQUIP_LEATHER, 1.0F, 0.05F,
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "repairs_elkwight_hide")),
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(MODID, "elkwight_hide")));
    public static final DeferredItem<com.mechanical.equipment.CloakItem> CLOAK = ITEMS.registerItem("elkwight_cloak",
            p -> new com.mechanical.equipment.CloakItem(p, HIDE_MATERIAL, true),
            p -> com.mechanical.equipment.CloakItem.properties(p, HIDE_MATERIAL).rarity(Rarity.RARE));

    /** Bone Charm (Charm slot): +4 max health (two hearts) and 10 % knockback resistance while worn. */
    public static final DeferredItem<Item> BONE_CHARM = ITEMS.registerSimpleItem("bone_charm", p -> p.stacksTo(1).rarity(Rarity.RARE)
            .attributes(ItemAttributeModifiers.builder()
                    .add(Attributes.MAX_HEALTH, new AttributeModifier(Identifier.fromNamespaceAndPath(MODID, "bone_charm"), 4.0,
                            AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.ARMOR)
                    .add(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(Identifier.fromNamespaceAndPath(MODID, "bone_charm"), 0.1,
                            AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.ARMOR)
                    .build()));

    public static final DeferredItem<SpawnEggItem> SPAWN_EGG = ITEMS.registerItem("elkwight_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ELKWIGHT.get()));

    /** Block tag of what he may smash through or crack (natural stone, earth, sand, leaves ...; ElkwightTerrain). */
    public static final TagKey<Block> BREAKABLE = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MODID, "elkwight_breakable"));

    private static Butchery.Extra extra(java.util.function.Supplier<Item> item, float chance, int min, int max) {
        return new Butchery.Extra(item, chance, min, max);
    }

    /** Loads the registrations and the carcass rules. Call from the mod constructor. */
    public static void init(IEventBus modEventBus) {
        // first pass: the hide (always two); second: a heap of meat, the Antler Crown (always one) and his great bones
        Butchery.register(ELKWIGHT::get, new Butchery.Yield(Mechanical.GAME_MEAT_SCRAPS, 10, 14, List.of(extra(HIDE, 1.0F, 2, 2)),
                List.of(extra(() -> ANTLER_CROWN_ITEM.get(), 1.0F, 1, 1), extra(BONE, 1.0F, 3, 4), extra(com.mechanical.wildlife.Wildlife.SINEW, 1.0F, 2, 3))));
        Butchery.colossal(ELKWIGHT::get);
        Butchery.requireKnife(ELKWIGHT::get, KNIFE_TIER);
        // the ritual knife's fine parts: his heart and fine sinew (no trophy skull: the Antler Crown is his trophy)
        Butchery.registerRitual(ELKWIGHT::get, List.of(extra(com.mechanical.people.OakFolk.ANIMAL_HEART, 1.0F, 1, 1),
                extra(com.mechanical.people.OakFolk.FINE_SINEW, 1.0F, 2, 3)));
        ElkwightTest.init(modEventBus);
    }

    /** Grants a code-driven advancement (criterion "done", scripts/gen_advancements.py by_code()). */
    static void award(net.minecraft.server.level.ServerPlayer player, Identifier id) {
        var holder = player.level().getServer().getAdvancements().get(id);
        if (holder != null) player.getAdvancements().award(holder, "done");
    }

    @SubscribeEvent
    static void attributes(EntityAttributeCreationEvent event) {
        event.put(ELKWIGHT.get(), Elkwight.createAttributes().build());
    }

    private Elkwights() {}
}
