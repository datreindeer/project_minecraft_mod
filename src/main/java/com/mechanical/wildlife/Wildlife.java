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

/** Forest wildlife: entity types, items they give, spawn eggs, attributes and spawn rules. Spawns are in data (biome modifier). */
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

    // what they give when butchered (see Butchery)
    public static final DeferredItem<Item> VENISON_SCRAPS = food("venison_scraps", 1, 0.1F);
    public static final DeferredItem<Item> COOKED_VENISON_SCRAPS = food("cooked_venison_scraps", 3, 0.8F);
    public static final DeferredItem<Item> SINEW = ITEMS.registerSimpleItem("sinew");
    public static final DeferredItem<Item> ANTLER = ITEMS.registerSimpleItem("antler");
    public static final DeferredItem<Item> LARD = ITEMS.registerSimpleItem("lard");
    public static final DeferredItem<Item> TUSK = ITEMS.registerSimpleItem("tusk");

    public static final DeferredItem<SpawnEggItem> ROE_DEER_SPAWN_EGG = egg("roe_deer_spawn_egg", ROE_DEER);
    public static final DeferredItem<SpawnEggItem> RED_DEER_SPAWN_EGG = egg("red_deer_spawn_egg", RED_DEER);
    public static final DeferredItem<SpawnEggItem> WILD_BOAR_SPAWN_EGG = egg("wild_boar_spawn_egg", WILD_BOAR);
    public static final DeferredItem<SpawnEggItem> HARE_SPAWN_EGG = egg("hare_spawn_egg", HARE);
    public static final DeferredItem<SpawnEggItem> PHEASANT_SPAWN_EGG = egg("pheasant_spawn_egg", PHEASANT);

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
    }

    @SubscribeEvent
    static void spawnRules(RegisterSpawnPlacementsEvent event) {
        for (var type : new EntityType<?>[] {ROE_DEER.get(), RED_DEER.get(), WILD_BOAR.get(), HARE.get(), PHEASANT.get()}) {
            register(event, type);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Animal> void register(RegisterSpawnPlacementsEvent event, EntityType<?> type) {
        event.register((EntityType<T>) type, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Animal::checkAnimalSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private Wildlife() {}
}
