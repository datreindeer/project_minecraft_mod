package com.mechanical.wildlife.cold;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ENTITY_TYPES;
import static com.mechanical.Mechanical.ITEMS;
import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.mechanical.BearRugBlock;
import com.mechanical.Butchery;
import com.mechanical.equipment.CloakItem;
import com.mechanical.wildlife.Wildlife;
import com.mechanical.wildlife.coldhunters.ColdhuntersFauna;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * The cold: Polar Bear (in place of vanilla's), Arctic Fox and Snow Leopard. Entity types, meat, pelts (cured on the
 * Drying Rack through {@link ColdhuntersFauna#CURES}), the goods made from them (Polar Bear Cloak, Polar Bear Rug, Arctic
 * Fox Hat, Snow Leopard Rug, Snow Leopard Boots), the poisonous Polar Bear Liver, spawn eggs, attributes, spawn rules
 * (spawns are data: scripts/gen_fauna_cold.py) and butchering yields. Every vanilla polar bear is replaced by ours.
 * Docs: docs/fauna/cold.md.
 */
@EventBusSubscriber(modid = MODID)
public final class ColdFauna {
    public static final DeferredHolder<EntityType<?>, EntityType<PolarBear>> POLAR_BEAR = ENTITY_TYPES.registerEntityType(
            "polar_bear", PolarBear::new, MobCategory.CREATURE, b -> b.sized(1.5F, 1.5F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<ArcticFox>> ARCTIC_FOX = ENTITY_TYPES.registerEntityType(
            "arctic_fox", ArcticFox::new, MobCategory.CREATURE, b -> b.sized(0.55F, 0.6F).clientTrackingRange(8));
    public static final DeferredHolder<EntityType<?>, EntityType<SnowLeopard>> SNOW_LEOPARD = ENTITY_TYPES.registerEntityType(
            "snow_leopard", SnowLeopard::new, MobCategory.CREATURE, b -> b.sized(0.9F, 0.95F).clientTrackingRange(10));

    // ---- meat (raw perishes in a day, cooked in three; smokable: gen_fauna_cold.py) ----
    public static final DeferredItem<Item> RAW_POLAR_BEAR = food("raw_polar_bear", 3, 0.3F);
    public static final DeferredItem<Item> COOKED_POLAR_BEAR = food("cooked_polar_bear", 8, 0.8F);
    public static final DeferredItem<Item> RAW_ARCTIC_FOX = food("raw_arctic_fox", 1, 0.2F);
    public static final DeferredItem<Item> COOKED_ARCTIC_FOX = food("cooked_arctic_fox", 3, 0.5F);
    public static final DeferredItem<Item> RAW_SNOW_LEOPARD = food("raw_snow_leopard", 2, 0.3F);
    public static final DeferredItem<Item> COOKED_SNOW_LEOPARD = food("cooked_snow_leopard", 6, 0.7F);
    /** Far too rich in vitamin A: eaten, it poisons (Nausea, Poison, Hunger) - the old hunters' warning. */
    public static final DeferredItem<Item> POLAR_BEAR_LIVER = ITEMS.registerSimpleItem("polar_bear_liver", p -> p.food(
            new FoodProperties.Builder().nutrition(2).saturationModifier(0.1F).build(),
            Consumables.defaultFood().onConsume(new ApplyStatusEffectsConsumeEffect(List.of(new MobEffectInstance(MobEffects.NAUSEA, 400, 0),
                    new MobEffectInstance(MobEffects.POISON, 300, 1), new MobEffectInstance(MobEffects.HUNGER, 600, 1)))).build()));

    // ---- pelts: raw off the carcass, cured on a Drying Rack ----
    public static final DeferredItem<Item> RAW_POLAR_BEAR_PELT = ITEMS.registerSimpleItem("raw_polar_bear_pelt");
    public static final DeferredItem<Item> POLAR_BEAR_PELT = ITEMS.registerSimpleItem("polar_bear_pelt");
    public static final DeferredItem<Item> RAW_ARCTIC_FOX_PELT = ITEMS.registerSimpleItem("raw_arctic_fox_pelt");
    public static final DeferredItem<Item> ARCTIC_FOX_FUR = ITEMS.registerSimpleItem("arctic_fox_fur");
    public static final DeferredItem<Item> RAW_SNOW_LEOPARD_PELT = ITEMS.registerSimpleItem("raw_snow_leopard_pelt");
    public static final DeferredItem<Item> SNOW_LEOPARD_PELT = ITEMS.registerSimpleItem("snow_leopard_pelt");

    // ---- goods ----
    /** A whole polar bear hide over the shoulders (Cloak slot): the warmest thing a hunter can wear. */
    private static final ArmorMaterial POLAR_CLOAK_MATERIAL = fur(10, 3, 12, "polar_bear_pelt", "polar_bear_cloak");
    public static final DeferredItem<CloakItem> POLAR_BEAR_CLOAK = ITEMS.registerItem("polar_bear_cloak",
            p -> new CloakItem(p, POLAR_CLOAK_MATERIAL, true), p -> CloakItem.properties(p, POLAR_CLOAK_MATERIAL));
    public static final DeferredItem<Item> ARCTIC_FOX_HAT = ITEMS.registerSimpleItem("arctic_fox_hat",
            p -> p.humanoidArmor(fur(7, 1, 15, "arctic_fox_fur", "arctic_fox_hat"), ArmorType.HELMET));
    /** Soft, thick-soled boots of snow leopard fur: the first {@link #BOOT_SAFE_FALL} blocks of a fall don't hurt. */
    public static final DeferredItem<Item> SNOW_LEOPARD_BOOTS = ITEMS.registerSimpleItem("snow_leopard_boots",
            p -> p.humanoidArmor(fur(9, 1, 15, "snow_leopard_pelt", "snow_leopard_boots"), ArmorType.BOOTS));
    static final double BOOT_SAFE_FALL = 4;
    public static final DeferredBlock<BearRugBlock> POLAR_BEAR_RUG = rug("polar_bear_rug", MapColor.SNOW);
    public static final DeferredItem<BlockItem> POLAR_BEAR_RUG_ITEM = ITEMS.registerSimpleBlockItem("polar_bear_rug", POLAR_BEAR_RUG);
    public static final DeferredBlock<BearRugBlock> SNOW_LEOPARD_RUG = rug("snow_leopard_rug", MapColor.COLOR_LIGHT_GRAY);
    public static final DeferredItem<BlockItem> SNOW_LEOPARD_RUG_ITEM = ITEMS.registerSimpleBlockItem("snow_leopard_rug", SNOW_LEOPARD_RUG);

    public static final DeferredItem<SpawnEggItem> POLAR_BEAR_SPAWN_EGG = egg("polar_bear_spawn_egg", POLAR_BEAR);
    public static final DeferredItem<SpawnEggItem> ARCTIC_FOX_SPAWN_EGG = egg("arctic_fox_spawn_egg", ARCTIC_FOX);
    public static final DeferredItem<SpawnEggItem> SNOW_LEOPARD_SPAWN_EGG = egg("snow_leopard_spawn_egg", SNOW_LEOPARD);

    static {
        ColdhuntersFauna.CURES.putAll(Map.of(RAW_POLAR_BEAR_PELT, POLAR_BEAR_PELT, RAW_ARCTIC_FOX_PELT, ARCTIC_FOX_FUR, RAW_SNOW_LEOPARD_PELT, SNOW_LEOPARD_PELT));
    }

    private static DeferredItem<Item> food(String name, int nutrition, float saturation) {
        return ITEMS.registerSimpleItem(name, p -> p.food(new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).build()));
    }

    private static DeferredItem<SpawnEggItem> egg(String name, DeferredHolder<EntityType<?>, ? extends EntityType<?>> type) {
        return ITEMS.registerItem(name, SpawnEggItem::new, p -> p.spawnEgg(type.get()));
    }

    private static DeferredBlock<BearRugBlock> rug(String name, MapColor colour) {
        return BLOCKS.registerBlock(name, BearRugBlock::new, p -> p.mapColor(colour).strength(0.2F).sound(SoundType.WOOL).noOcclusion());
    }

    /** A one-piece fur armour material: its own equipment asset, repaired with {@code repair}. */
    private static ArmorMaterial fur(int durability, int defense, int enchantability, String repair, String asset) {
        return new ArmorMaterial(durability, Map.of(ArmorType.HELMET, defense, ArmorType.CHESTPLATE, defense, ArmorType.LEGGINGS, defense,
                ArmorType.BOOTS, defense, ArmorType.BODY, defense), enchantability, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F,
                TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "repairs_" + repair)),
                ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(MODID, asset)));
    }

    private static Butchery.Extra extra(Supplier<Item> item, float chance, int min, int max) {
        return new Butchery.Extra(item, chance, min, max);
    }

    /** Loads this class so its registrations happen, and registers the butchering yields. Call from the mod constructor. */
    public static void init() {
        Supplier<Item> bone = () -> Items.BONE;
        // first pass: the pelt; second: meat, then fat, claws, the liver, sinew and bone. Counts for a flint knife.
        Butchery.register(POLAR_BEAR::get, new Butchery.Yield(RAW_POLAR_BEAR, 4, 6, List.of(extra(RAW_POLAR_BEAR_PELT, 1.0F, 1, 1)),
                List.of(extra(Wildlife.BEAR_FAT, 0.9F, 2, 4), extra(Wildlife.BEAR_CLAW, 0.6F, 1, 3), extra(POLAR_BEAR_LIVER, 1.0F, 1, 1),
                        extra(Wildlife.SINEW, 0.6F, 1, 2))));
        Butchery.toughHide(POLAR_BEAR::get);
        Butchery.register(ARCTIC_FOX::get, new Butchery.Yield(RAW_ARCTIC_FOX, 1, 1, List.of(extra(RAW_ARCTIC_FOX_PELT, 0.9F, 1, 1)), List.of()));
        Butchery.register(SNOW_LEOPARD::get, new Butchery.Yield(RAW_SNOW_LEOPARD, 1, 3, List.of(extra(RAW_SNOW_LEOPARD_PELT, 1.0F, 1, 1)),
                List.of(extra(Wildlife.SINEW, 0.5F, 1, 2), extra(bone, 0.5F, 1, 2))));
    }

    @SubscribeEvent
    static void attributes(EntityAttributeCreationEvent event) {
        event.put(POLAR_BEAR.get(), PolarBear.createAttributes().build());
        event.put(ARCTIC_FOX.get(), ArcticFox.createAttributes().build());
        event.put(SNOW_LEOPARD.get(), SnowLeopard.createAttributes().build());
    }

    @SubscribeEvent
    static void spawnRules(RegisterSpawnPlacementsEvent event) {
        // bear and fox: snow, ice or grass in daylight; the snow leopard on the goat's rock and snow
        event.register(POLAR_BEAR.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (t, level, reason, pos, random) -> (level.getBlockState(pos.below()).is(BlockTags.POLAR_BEARS_SPAWNABLE_ON_ALTERNATE)
                        || level.getBlockState(pos.below()).is(BlockTags.FOXES_SPAWNABLE_ON)) && level.getRawBrightness(pos, 0) > 8,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ARCTIC_FOX.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (t, level, reason, pos, random) -> level.getBlockState(pos.below()).is(BlockTags.FOXES_SPAWNABLE_ON) && level.getRawBrightness(pos, 0) > 8,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(SNOW_LEOPARD.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Goat::checkGoatSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    /** Vanilla's polar bear never enters the world (old saves too): ours takes its place, a cub as a cub. */
    @SubscribeEvent
    static void onJoin(EntityJoinLevelEvent event) {
        Entity e = event.getEntity();
        if (e.getType() != EntityTypes.POLAR_BEAR || !(event.getLevel() instanceof ServerLevel level)) return;
        event.setCanceled(true);
        boolean cub = e instanceof net.minecraft.world.entity.AgeableMob a && a.isBaby();
        // next tick: adding entities while a chunk is loading its own is not safe
        level.getServer().execute(() -> {
            PolarBear bear = POLAR_BEAR.get().create(level, EntitySpawnReason.CONVERSION);
            if (bear == null) return;
            bear.snapTo(e.getX(), e.getY(), e.getZ(), e.getYRot(), 0);
            if (cub) bear.setBaby(true);
            level.addFreshEntity(bear);
        });
    }

    /** Snow Leopard Boots: the first few blocks of a fall are taken by the soft soles. */
    @SubscribeEvent
    static void onFall(LivingFallEvent event) {
        if (event.getEntity().getItemBySlot(EquipmentSlot.FEET).is(SNOW_LEOPARD_BOOTS.get())) event.setDistance(Math.max(0, event.getDistance() - BOOT_SAFE_FALL));
    }

    private ColdFauna() {}
}
