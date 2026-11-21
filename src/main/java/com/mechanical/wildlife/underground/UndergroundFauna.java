package com.mechanical.wildlife.underground;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ENTITY_TYPES;
import static com.mechanical.Mechanical.ITEMS;
import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.mechanical.Butchery;
import com.mechanical.wildlife.mountain.MountainFauna;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import com.mechanical.KnifeItem;
import com.mechanical.wildlife.Wildlife;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Underground fauna (docs/fauna/underground.md): the Giant Centipede and the creatures of Nordic folklore (Vittra, Troll,
 * Lindworm), plus the cave spawns of the Cave Hyena and Cave Bear (their entities and goods live in MountainFauna). All
 * spawn by {@link Underground}'s darkness rule: hyena packs and cave bears at Y 40 to 0, centipedes and vittra at Y 0 to
 * -30, trolls below -30 and, rarely, the lindworm below -45. Data: scripts/gen_fauna_underground.py; textures:
 * scripts/gen_fauna_underground_textures.py.
 */
@EventBusSubscriber(modid = MODID)
public final class UndergroundFauna {
    public static final DeferredHolder<EntityType<?>, EntityType<GiantCentipede>> GIANT_CENTIPEDE = ENTITY_TYPES.registerEntityType(
            "giant_centipede", GiantCentipede::new, MobCategory.CREATURE, b -> b.sized(0.9F, 0.4F).clientTrackingRange(10));

    // ---- meat (raw perishes in a day, cooked in three; gen_fauna_underground.py) ----
    public static final DeferredItem<Item> RAW_CENTIPEDE = food("raw_centipede", 2, 0.1F);
    public static final DeferredItem<Item> COOKED_CENTIPEDE = food("cooked_centipede", 5, 0.6F);
    // ---- parts ----
    public static final DeferredItem<Item> CENTIPEDE_CHITIN = ITEMS.registerSimpleItem("centipede_chitin");
    public static final DeferredItem<Item> CENTIPEDE_FANG = ITEMS.registerSimpleItem("centipede_fang");
    public static final DeferredItem<Item> CENTIPEDE_VENOM_SAC = ITEMS.registerSimpleItem("centipede_venom_sac");

    /** Overlapping chitin plates sewn on a leather backing: a light chestplate (4) long before bronze. */
    public static final ArmorMaterial CHITIN = new ArmorMaterial(12, Map.of(ArmorType.HELMET, 2, ArmorType.CHESTPLATE, 4, ArmorType.LEGGINGS, 3,
            ArmorType.BOOTS, 1, ArmorType.BODY, 4), 10, SoundEvents.ARMOR_EQUIP_TURTLE, 0.0F, 0.0F,
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "repairs_centipede_chitin")),
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(MODID, "chitin_vest")));
    public static final DeferredItem<Item> CHITIN_VEST = ITEMS.registerSimpleItem("chitin_vest", p -> p.humanoidArmor(CHITIN, ArmorType.CHESTPLATE));

    // ---- the folk of the dark ----
    public static final DeferredHolder<EntityType<?>, EntityType<Vittra>> VITTRA = ENTITY_TYPES.registerEntityType(
            "vittra", Vittra::new, MobCategory.CREATURE, b -> b.sized(0.6F, 1.5F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<Troll>> TROLL = ENTITY_TYPES.registerEntityType(
            "troll", Troll::new, MobCategory.CREATURE, b -> b.sized(1.2F, 2.7F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<Lindworm>> LINDWORM = ENTITY_TYPES.registerEntityType(
            "lindworm", Lindworm::new, MobCategory.CREATURE, b -> b.sized(1.8F, 1.3F).clientTrackingRange(12));

    // vittra: grey hide, big night eyes
    public static final DeferredItem<Item> VITTRA_MEAT = food("vittra_meat", 2, 0.1F);
    public static final DeferredItem<Item> COOKED_VITTRA_MEAT = food("cooked_vittra_meat", 5, 0.6F);
    public static final DeferredItem<Item> VITTRA_HIDE = ITEMS.registerSimpleItem("vittra_hide");
    public static final DeferredItem<Item> VITTRA_EYE = ITEMS.registerSimpleItem("vittra_eye");
    /** A hood of soft grey vittra hide: no vittra hears the one who wears it (Vittra#muffled). */
    public static final ArmorMaterial VITTRA_HIDE_MATERIAL = armor("vittra_hood", 6, 1, 2, 2, 1, 2, 12, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F, "repairs_vittra_hide");
    public static final DeferredItem<Item> VITTRA_HOOD = ITEMS.registerSimpleItem("vittra_hood", p -> p.humanoidArmor(VITTRA_HIDE_MATERIAL, ArmorType.HELMET));

    // troll: warty hide, coarse hair, tusks
    public static final DeferredItem<Item> TROLL_MEAT = food("troll_meat", 3, 0.15F);
    public static final DeferredItem<Item> COOKED_TROLL_MEAT = food("cooked_troll_meat", 8, 0.8F);
    public static final DeferredItem<Item> TROLL_HIDE = ITEMS.registerSimpleItem("troll_hide");
    public static final DeferredItem<Item> TROLL_HAIR = ITEMS.registerSimpleItem("troll_hair");
    public static final DeferredItem<Item> TROLL_TUSK = ITEMS.registerSimpleItem("troll_tusk");
    /** Thick troll hide over a sinew-sewn lining: a heavy coat (armour 6, toughness 1) for the deep. */
    public static final ArmorMaterial TROLL_HIDE_MATERIAL = armor("trollhide_coat", 20, 2, 6, 5, 2, 6, 9, SoundEvents.ARMOR_EQUIP_LEATHER, 1.0F, 0.05F, "repairs_troll_hide");
    public static final DeferredItem<Item> TROLLHIDE_COAT = ITEMS.registerSimpleItem("trollhide_coat", p -> p.humanoidArmor(TROLL_HIDE_MATERIAL, ArmorType.CHESTPLATE));
    /** A troll's tusk on a stick: a skinning knife that cuts the thickest hides (troll, lindworm, bears) like the Tusk Dagger. */
    public static final ToolMaterial TROLL_TUSK_MATERIAL = new ToolMaterial(BlockTags.INCORRECT_FOR_WOODEN_TOOL, 220, 3.0F, 1.0F, 5,
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "troll_tusk_tool_materials")));
    public static final DeferredItem<KnifeItem> TROLL_TUSK_KNIFE = ITEMS.registerItem("troll_tusk_knife",
            p -> new KnifeItem(p, 1.4F, 1.2F, KnifeItem.TUSK, 1.6F), p -> p.sword(TROLL_TUSK_MATERIAL, 1.5F, -1.8F));
    /** What daylight leaves of a troll (Troll#petrify): a lumpy grey-green boulder. */
    public static final DeferredBlock<Block> TROLL_STONE = BLOCKS.registerSimpleBlock("troll_stone", p -> p.mapColor(MapColor.STONE)
            .instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 9.0F).sound(SoundType.STONE));

    // lindworm: scales, fangs, venom, its heart
    public static final DeferredItem<Item> LINDWORM_MEAT = food("lindworm_meat", 3, 0.2F);
    public static final DeferredItem<Item> COOKED_LINDWORM_MEAT = food("cooked_lindworm_meat", 10, 1.0F);
    public static final DeferredItem<Item> LINDWORM_SCALE = ITEMS.registerSimpleItem("lindworm_scale", p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> LINDWORM_FANG = ITEMS.registerSimpleItem("lindworm_fang", p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> LINDWORM_VENOM_GLAND = ITEMS.registerSimpleItem("lindworm_venom_gland");
    /** Like Sigurd's dragon heart: eaten, its strength passes to you (Strength II, Resistance I 3 min, Regeneration II 20 s). */
    public static final DeferredItem<Item> LINDWORM_HEART = ITEMS.registerSimpleItem("lindworm_heart", p -> p.rarity(Rarity.RARE).food(
            new FoodProperties.Builder().nutrition(6).saturationModifier(0.8F).alwaysEdible().build(),
            Consumables.defaultFood().onConsume(new ApplyStatusEffectsConsumeEffect(List.of(new MobEffectInstance(MobEffects.STRENGTH, 3600, 1),
                    new MobEffectInstance(MobEffects.RESISTANCE, 3600, 0), new MobEffectInstance(MobEffects.REGENERATION, 400, 1)))).build()));
    /** Overlapping lindworm scales: better than iron, close to what diamond will be (3/8/6/3, toughness 3). */
    public static final ArmorMaterial LINDWORM_SCALE_MATERIAL = armor("lindworm_scale", 35, 3, 8, 6, 3, 11, 15, SoundEvents.ARMOR_EQUIP_CHAIN, 3.0F, 0.1F, "repairs_lindworm_scale");
    public static final DeferredItem<Item> LINDWORM_SCALE_HELMET = ITEMS.registerSimpleItem("lindworm_scale_helmet", p -> p.humanoidArmor(LINDWORM_SCALE_MATERIAL, ArmorType.HELMET).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> LINDWORM_SCALE_CHESTPLATE = ITEMS.registerSimpleItem("lindworm_scale_chestplate", p -> p.humanoidArmor(LINDWORM_SCALE_MATERIAL, ArmorType.CHESTPLATE).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> LINDWORM_SCALE_LEGGINGS = ITEMS.registerSimpleItem("lindworm_scale_leggings", p -> p.humanoidArmor(LINDWORM_SCALE_MATERIAL, ArmorType.LEGGINGS).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> LINDWORM_SCALE_BOOTS = ITEMS.registerSimpleItem("lindworm_scale_boots", p -> p.humanoidArmor(LINDWORM_SCALE_MATERIAL, ArmorType.BOOTS).rarity(Rarity.UNCOMMON));
    /** Two lindworm fangs bound into a blade: 7 damage, quicker than a sword of iron, 1200 uses. */
    public static final ToolMaterial LINDWORM_FANG_MATERIAL = new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 1200, 7.0F, 3.0F, 15,
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "repairs_lindworm_fang")));
    public static final DeferredItem<Item> LINDWORM_FANG_BLADE = ITEMS.registerSimpleItem("lindworm_fang_blade", p -> p.sword(LINDWORM_FANG_MATERIAL, 3.0F, -2.2F).rarity(Rarity.UNCOMMON));

    public static final DeferredItem<SpawnEggItem> VITTRA_SPAWN_EGG = ITEMS.registerItem("vittra_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(VITTRA.get()));
    public static final DeferredItem<SpawnEggItem> TROLL_SPAWN_EGG = ITEMS.registerItem("troll_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(TROLL.get()));
    public static final DeferredItem<SpawnEggItem> LINDWORM_SPAWN_EGG = ITEMS.registerItem("lindworm_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(LINDWORM.get()));

    public static final DeferredItem<SpawnEggItem> GIANT_CENTIPEDE_SPAWN_EGG = ITEMS.registerItem("giant_centipede_spawn_egg", SpawnEggItem::new,
            p -> p.spawnEgg(GIANT_CENTIPEDE.get()));

    private static DeferredItem<Item> food(String name, int nutrition, float saturation) {
        return ITEMS.registerSimpleItem(name, p -> p.food(new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).build()));
    }

    private static ArmorMaterial armor(String asset, int durability, int helmet, int chest, int legs, int boots, int body, int enchant,
            net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent> sound, float toughness, float knockback, String repairTag) {
        return new ArmorMaterial(durability, Map.of(ArmorType.HELMET, helmet, ArmorType.CHESTPLATE, chest, ArmorType.LEGGINGS, legs,
                ArmorType.BOOTS, boots, ArmorType.BODY, body), enchant, sound, toughness, knockback,
                TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, repairTag)),
                ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(MODID, asset)));
    }

    private static Butchery.Extra extra(Supplier<Item> item, float chance, int min, int max) {
        return new Butchery.Extra(item, chance, min, max);
    }

    /** Loads this class, registers the butchering yields and the depth bands. Call from the mod constructor. */
    public static void init() {
        // first pass: the chitin plates (its "hide"); second: meat, fangs and the venom sac. No bones in a centipede.
        Butchery.register(GIANT_CENTIPEDE::get, new Butchery.Yield(RAW_CENTIPEDE, 1, 2, List.of(extra(CENTIPEDE_CHITIN, 1.0F, 2, 4)),
                List.of(extra(CENTIPEDE_FANG, 0.7F, 1, 2), extra(CENTIPEDE_VENOM_SAC, 0.5F, 1, 1))));
        Underground.addBand(MountainFauna.CAVE_HYENA, 0, 40, 3, 3, 5);
        Underground.addBand(MountainFauna.CAVE_BEAR, 0, 40, 1, 1, 1);
        Underground.addBand(GIANT_CENTIPEDE, -30, 0, 4, 1, 2);
        // vittra: hide first, then the meat, the big eyes and a few bones
        Butchery.register(VITTRA::get, new Butchery.Yield(VITTRA_MEAT, 1, 2, List.of(extra(VITTRA_HIDE, 0.9F, 1, 2)),
                List.of(extra(VITTRA_EYE, 0.6F, 1, 2), extra(() -> Items.BONE, 0.6F, 1, 2))));
        // troll: the warty hide and the hair, then a lot of meat, tusks, bones and sinew; a tough hide (tusk-grade knife)
        Butchery.register(TROLL::get, new Butchery.Yield(TROLL_MEAT, 4, 6, List.of(extra(TROLL_HIDE, 1.0F, 3, 5), extra(TROLL_HAIR, 0.8F, 1, 3)),
                List.of(extra(TROLL_TUSK, 0.7F, 1, 2), extra(() -> Items.BONE, 0.9F, 3, 5), extra(Wildlife.SINEW, 0.7F, 1, 3))));
        Butchery.toughHide(TROLL::get);
        // lindworm: the scales, then meat, fangs, the venom gland, its heart (always one: chance 1 isn't scaled) and bones
        Butchery.register(LINDWORM::get, new Butchery.Yield(LINDWORM_MEAT, 6, 10, List.of(extra(LINDWORM_SCALE, 1.0F, 8, 12)),
                List.of(extra(LINDWORM_FANG, 1.0F, 2, 2), extra(LINDWORM_VENOM_GLAND, 0.8F, 1, 1), extra(LINDWORM_HEART, 1.0F, 1, 1),
                        extra(() -> Items.BONE, 1.0F, 4, 8))));
        Butchery.toughHide(LINDWORM::get);
        Underground.addBand(VITTRA, -30, 0, 3, 2, 3);
        Underground.addBand(TROLL, -64, -30, 20, 1, 2);
        Underground.addBand(LINDWORM, -64, -45, 1, 1, 1);
    }

    @SubscribeEvent
    static void attributes(EntityAttributeCreationEvent event) {
        event.put(GIANT_CENTIPEDE.get(), GiantCentipede.createAttributes().build());
        event.put(VITTRA.get(), Vittra.createAttributes().build());
        event.put(TROLL.get(), Troll.createAttributes().build());
        event.put(LINDWORM.get(), Lindworm.createAttributes().build());
    }

    /** Natural spawns come only from Underground's spawner; this rule covers anything else (spawners, structures). */
    @SubscribeEvent
    static void spawnRules(RegisterSpawnPlacementsEvent event) {
        event.register(GIANT_CENTIPEDE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (t, level, reason, pos, random) -> Underground.canSpawn(level, pos, -30, 0), RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(VITTRA.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (t, level, reason, pos, random) -> Underground.canSpawn(level, pos, -30, 0), RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(TROLL.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (t, level, reason, pos, random) -> Underground.canSpawn(level, pos, -64, -30), RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(LINDWORM.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (t, level, reason, pos, random) -> Underground.canSpawn(level, pos, -64, -45), RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private UndergroundFauna() {}
}
