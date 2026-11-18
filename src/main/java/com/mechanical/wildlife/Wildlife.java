package com.mechanical.wildlife;

import static com.mechanical.Mechanical.ENTITY_TYPES;
import static com.mechanical.Mechanical.ITEMS;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/** Wildlife (forest, open country, cold): entity types, items they give, spawn eggs, attributes and spawn rules. Spawns are in data (biome modifier). */
@EventBusSubscriber(modid = "mechanical")
public final class Wildlife {
    public static final DeferredHolder<EntityType<?>, EntityType<RoeDeer>> ROE_DEER = ENTITY_TYPES.registerEntityType(
            "roe_deer", RoeDeer::new, MobCategory.CREATURE, b -> b.sized(0.7F, 1.2F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<RedDeer>> RED_DEER = ENTITY_TYPES.registerEntityType(
            "red_deer", RedDeer::new, MobCategory.CREATURE, b -> b.sized(1.0F, 1.6F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<WildBoar>> WILD_BOAR = ENTITY_TYPES.registerEntityType(
            "wild_boar", WildBoar::new, MobCategory.CREATURE, b -> b.sized(0.9F, 0.9F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<Hare>> HARE = ENTITY_TYPES.registerEntityType(
            "hare", Hare::new, MobCategory.CREATURE, b -> b.sized(0.5F, 0.5F).clientTrackingRange(8));
    public static final DeferredHolder<EntityType<?>, EntityType<Pheasant>> PHEASANT = ENTITY_TYPES.registerEntityType(
            "pheasant", Pheasant::new, MobCategory.CREATURE, b -> b.sized(0.5F, 0.7F).clientTrackingRange(8));
    // open country
    public static final DeferredHolder<EntityType<?>, EntityType<Aurochs>> AUROCHS = ENTITY_TYPES.registerEntityType(
            "aurochs", Aurochs::new, MobCategory.CREATURE, b -> b.sized(1.3F, 1.6F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<WildHorse>> WILD_HORSE = ENTITY_TYPES.registerEntityType(
            "wild_horse", WildHorse::new, MobCategory.CREATURE, b -> b.sized(1.3F, 1.6F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<Mouflon>> MOUFLON = ENTITY_TYPES.registerEntityType(
            "mouflon", Mouflon::new, MobCategory.CREATURE, b -> b.sized(0.9F, 1.3F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<BrownBear>> BROWN_BEAR = ENTITY_TYPES.registerEntityType(
            "brown_bear", BrownBear::new, MobCategory.CREATURE, b -> b.sized(1.3F, 1.4F).clientTrackingRange(10));
    // cold
    public static final DeferredHolder<EntityType<?>, EntityType<Moose>> MOOSE = ENTITY_TYPES.registerEntityType(
            "moose", Moose::new, MobCategory.CREATURE, b -> b.sized(1.5F, 2.2F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<Reindeer>> REINDEER = ENTITY_TYPES.registerEntityType(
            "reindeer", Reindeer::new, MobCategory.CREATURE, b -> b.sized(0.95F, 1.5F).clientTrackingRange(10));

    // small forest animals
    public static final DeferredHolder<EntityType<?>, EntityType<Squirrel>> SQUIRREL = ENTITY_TYPES.registerEntityType(
            "squirrel", Squirrel::new, MobCategory.CREATURE, b -> b.sized(0.4F, 0.45F).clientTrackingRange(8));
    public static final DeferredHolder<EntityType<?>, EntityType<Hedgehog>> HEDGEHOG = ENTITY_TYPES.registerEntityType(
            "hedgehog", Hedgehog::new, MobCategory.CREATURE, b -> b.sized(0.45F, 0.4F).clientTrackingRange(8));
    public static final DeferredHolder<EntityType<?>, EntityType<Badger>> BADGER = ENTITY_TYPES.registerEntityType(
            "badger", Badger::new, MobCategory.CREATURE, b -> b.sized(0.7F, 0.6F).clientTrackingRange(10));
    // rare mini-boss
    public static final DeferredHolder<EntityType<?>, EntityType<GiantDeer>> GIANT_DEER = ENTITY_TYPES.registerEntityType(
            "giant_deer", GiantDeer::new, MobCategory.CREATURE, b -> b.sized(1.6F, 2.4F).clientTrackingRange(12));

    // what they give when butchered (see Butchery)
    public static final DeferredItem<Item> VENISON_SCRAPS = food("venison_scraps", 1, 0.1F);
    public static final DeferredItem<Item> COOKED_VENISON_SCRAPS = food("cooked_venison_scraps", 3, 0.8F);
    public static final DeferredItem<Item> SINEW = ITEMS.registerSimpleItem("sinew");
    public static final DeferredItem<Item> ANTLER = ITEMS.registerSimpleItem("antler");
    public static final DeferredItem<Item> LARD = ITEMS.registerSimpleItem("lard");
    public static final DeferredItem<Item> TUSK = ITEMS.registerSimpleItem("tusk");
    public static final DeferredItem<Item> HORN = ITEMS.registerSimpleItem("horn");
    public static final DeferredItem<Item> BEAR_PELT = ITEMS.registerSimpleItem("bear_pelt");
    public static final DeferredItem<Item> BEAR_FAT = ITEMS.registerSimpleItem("bear_fat");
    public static final DeferredItem<Item> BEAR_CLAW = ITEMS.registerSimpleItem("bear_claw");
    public static final DeferredItem<Item> SQUIRREL_PELT = ITEMS.registerSimpleItem("squirrel_pelt");
    /** Always comes off a butchered giant deer; an altar offering. */
    public static final DeferredItem<Item> GIANT_ANTLER = ITEMS.registerSimpleItem("giant_antler",
            p -> p.rarity(net.minecraft.world.item.Rarity.EPIC).stacksTo(16));

    public static final DeferredItem<SpawnEggItem> ROE_DEER_SPAWN_EGG = egg("roe_deer_spawn_egg", ROE_DEER);
    public static final DeferredItem<SpawnEggItem> RED_DEER_SPAWN_EGG = egg("red_deer_spawn_egg", RED_DEER);
    public static final DeferredItem<SpawnEggItem> WILD_BOAR_SPAWN_EGG = egg("wild_boar_spawn_egg", WILD_BOAR);
    public static final DeferredItem<SpawnEggItem> HARE_SPAWN_EGG = egg("hare_spawn_egg", HARE);
    public static final DeferredItem<SpawnEggItem> PHEASANT_SPAWN_EGG = egg("pheasant_spawn_egg", PHEASANT);
    public static final DeferredItem<SpawnEggItem> AUROCHS_SPAWN_EGG = egg("aurochs_spawn_egg", AUROCHS);
    public static final DeferredItem<SpawnEggItem> WILD_HORSE_SPAWN_EGG = egg("wild_horse_spawn_egg", WILD_HORSE);
    public static final DeferredItem<SpawnEggItem> MOUFLON_SPAWN_EGG = egg("mouflon_spawn_egg", MOUFLON);
    public static final DeferredItem<SpawnEggItem> BROWN_BEAR_SPAWN_EGG = egg("brown_bear_spawn_egg", BROWN_BEAR);
    public static final DeferredItem<SpawnEggItem> MOOSE_SPAWN_EGG = egg("moose_spawn_egg", MOOSE);
    public static final DeferredItem<SpawnEggItem> REINDEER_SPAWN_EGG = egg("reindeer_spawn_egg", REINDEER);
    public static final DeferredItem<SpawnEggItem> SQUIRREL_SPAWN_EGG = egg("squirrel_spawn_egg", SQUIRREL);
    public static final DeferredItem<SpawnEggItem> HEDGEHOG_SPAWN_EGG = egg("hedgehog_spawn_egg", HEDGEHOG);
    public static final DeferredItem<SpawnEggItem> BADGER_SPAWN_EGG = egg("badger_spawn_egg", BADGER);
    public static final DeferredItem<SpawnEggItem> GIANT_DEER_SPAWN_EGG = egg("giant_deer_spawn_egg", GIANT_DEER);

    private static DeferredItem<Item> food(String name, int nutrition, float saturation) {
        return ITEMS.registerSimpleItem(name, p -> p.food(new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).build()));
    }

    private static DeferredItem<SpawnEggItem> egg(String name, DeferredHolder<EntityType<?>, ? extends EntityType<?>> type) {
        return ITEMS.registerItem(name, SpawnEggItem::new, p -> p.spawnEgg(type.get()));
    }

    /** Loads this class so its registrations happen; call from the mod constructor. */
    public static void init() {}

    @SubscribeEvent
    static void attributes(EntityAttributeCreationEvent event) {
        event.put(ROE_DEER.get(), RoeDeer.createAttributes().build());
        event.put(RED_DEER.get(), RedDeer.createAttributes().build());
        event.put(WILD_BOAR.get(), WildBoar.createAttributes().build());
        event.put(HARE.get(), Hare.createAttributes().build());
        event.put(PHEASANT.get(), Pheasant.createAttributes().build());
        event.put(AUROCHS.get(), Aurochs.createAttributes().build());
        event.put(WILD_HORSE.get(), WildHorse.createAttributes().build());
        event.put(MOUFLON.get(), Mouflon.createAttributes().build());
        event.put(MOOSE.get(), Moose.createAttributes().build());
        event.put(BROWN_BEAR.get(), BrownBear.createAttributes().build());
        event.put(REINDEER.get(), Reindeer.createAttributes().build());
        event.put(SQUIRREL.get(), Squirrel.createAttributes().build());
        event.put(HEDGEHOG.get(), Hedgehog.createAttributes().build());
        event.put(BADGER.get(), Badger.createAttributes().build());
        event.put(GIANT_DEER.get(), GiantDeer.createAttributes().build());
    }

    @SubscribeEvent
    static void spawnRules(RegisterSpawnPlacementsEvent event) {
        for (var type : new EntityType<?>[] {ROE_DEER.get(), RED_DEER.get(), WILD_BOAR.get(), HARE.get(), PHEASANT.get(),
                AUROCHS.get(), WILD_HORSE.get(), MOOSE.get(), REINDEER.get(), SQUIRREL.get(), HEDGEHOG.get(), GIANT_DEER.get()}) {
            register(event, type);
        }
        // bears only turn up near wild berry bushes
        event.register(BROWN_BEAR.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> Animal.checkAnimalSpawnRules(type, level, reason, pos, random)
                        && com.mechanical.Foraging.nearBerryBush(level, pos, 16),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        // badgers dig their setts into slopes
        event.register(BADGER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> Animal.checkAnimalSpawnRules(type, level, reason, pos, random) && Badger.onSlope(level, pos),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        // mouflon also lives on stone and gravel (windswept hills, peaks): the goat rule
        event.register(MOUFLON.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.animal.goat.Goat::checkGoatSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Animal> void register(RegisterSpawnPlacementsEvent event, EntityType<?> type) {
        event.register((EntityType<T>) type, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Animal::checkAnimalSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private Wildlife() {}
}
