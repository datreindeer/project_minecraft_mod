package com.mechanical.people;

import static com.mechanical.Mechanical.ENTITY_TYPES;
import static com.mechanical.Mechanical.ITEMS;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/** Stone Age people: the tribesperson entity, its spawn egg, and the camp / hunting band worldgen features. */
@EventBusSubscriber(modid = "mechanical")
public final class People {
    public static final DeferredHolder<EntityType<?>, EntityType<Tribesperson>> TRIBESPERSON = ENTITY_TYPES.registerEntityType(
            "tribesperson", Tribesperson::new, MobCategory.MISC, b -> b.sized(0.6F, 1.8F).eyeHeight(1.62F).clientTrackingRange(10));
    public static final DeferredItem<SpawnEggItem> TRIBESPERSON_SPAWN_EGG = ITEMS.registerItem("tribesperson_spawn_egg", SpawnEggItem::new,
            p -> p.spawnEgg(TRIBESPERSON.get()));

    /** Call from the mod constructor: loads the registrations and registers the worldgen features. */
    public static void init(IEventBus modEventBus) {
        com.mechanical.worldgen.PeopleFeatures.FEATURE_TYPES.register(modEventBus);
    }

    @SubscribeEvent
    static void attributes(EntityAttributeCreationEvent event) {
        event.put(TRIBESPERSON.get(), Tribesperson.createAttributes().build());
    }

    private People() {}
}
