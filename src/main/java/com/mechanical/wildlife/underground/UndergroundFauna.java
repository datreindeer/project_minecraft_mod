package com.mechanical.wildlife.underground;

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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Underground fauna (docs/fauna/underground.md): the Giant Centipede, plus the cave spawns of the Cave Hyena and Cave Bear
 * (their entities and goods live in MountainFauna). All three spawn by {@link Underground}'s darkness rule: hyena packs and
 * cave bears at Y 40 to 0, centipedes at Y 0 to -30. Data: scripts/gen_fauna_underground.py; textures:
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

    public static final DeferredItem<SpawnEggItem> GIANT_CENTIPEDE_SPAWN_EGG = ITEMS.registerItem("giant_centipede_spawn_egg", SpawnEggItem::new,
            p -> p.spawnEgg(GIANT_CENTIPEDE.get()));

    private static DeferredItem<Item> food(String name, int nutrition, float saturation) {
        return ITEMS.registerSimpleItem(name, p -> p.food(new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).build()));
    }

    private static Butchery.Extra extra(Supplier<Item> item, float chance, int min, int max) {
        return new Butchery.Extra(item, chance, min, max);
    }

    /** Loads this class, registers the centipede's butchering yield and the three depth bands. Call from the mod constructor. */
    public static void init() {
        // first pass: the chitin plates (its "hide"); second: meat, fangs and the venom sac. No bones in a centipede.
        Butchery.register(GIANT_CENTIPEDE::get, new Butchery.Yield(RAW_CENTIPEDE, 1, 2, List.of(extra(CENTIPEDE_CHITIN, 1.0F, 2, 4)),
                List.of(extra(CENTIPEDE_FANG, 0.7F, 1, 2), extra(CENTIPEDE_VENOM_SAC, 0.5F, 1, 1))));
        Underground.addBand(MountainFauna.CAVE_HYENA, 0, 40, 3, 3, 5);
        Underground.addBand(MountainFauna.CAVE_BEAR, 0, 40, 1, 1, 1);
        Underground.addBand(GIANT_CENTIPEDE, -30, 0, 4, 1, 2);
    }

    @SubscribeEvent
    static void attributes(EntityAttributeCreationEvent event) {
        event.put(GIANT_CENTIPEDE.get(), GiantCentipede.createAttributes().build());
    }

    /** Natural spawns come only from Underground's spawner; this rule covers anything else (spawners, structures). */
    @SubscribeEvent
    static void spawnRules(RegisterSpawnPlacementsEvent event) {
        event.register(GIANT_CENTIPEDE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (t, level, reason, pos, random) -> Underground.canSpawn(level, pos, -30, 0), RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private UndergroundFauna() {}
}
