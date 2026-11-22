package com.mechanical.wildlife.savanna;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ENTITY_TYPES;
import static com.mechanical.Mechanical.ITEMS;
import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.Map;

import com.mechanical.Butchery;
import com.mechanical.Butchery.Extra;
import com.mechanical.Butchery.Yield;
import com.mechanical.wildlife.Wildlife;
import com.mechanical.wildlife.mountain.MountainFauna;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Savanna fauna: the day predators and their prey. Spotted Hyena (a daytime CaveHyena: packs that steal your kills and
 * trail wounded animals), Cheetah (a sprinter that gives up after a few seconds), Gazelle (fast, skittish herds; its horn
 * makes the Signal Horn), Ostrich (kicks when cornered, lays a meal-sized egg), Aardvark (digs at Termite Mounds by night)
 * and the Termite Mound itself (termites are fished out with a stick and pounded into Chitin Paste, a glue). The Cave
 * Lion's day in the shade is in CaveLion. Spawns and the mound feature are data: scripts/gen_fauna_savanna.py, textures:
 * scripts/gen_fauna_savanna_textures.py, docs: docs/fauna/savanna.md.
 */
@EventBusSubscriber(modid = MODID)
public final class SavannaFauna {
    /** Entity tag that keeps a day animal awake whatever the time (game tests run at whatever hour the server has). */
    public static final String ALWAYS_ACTIVE = "mechanical.always_active";

    // ---- animals ----
    public static final DeferredHolder<EntityType<?>, EntityType<SpottedHyena>> SPOTTED_HYENA = ENTITY_TYPES.registerEntityType(
            "spotted_hyena", SpottedHyena::new, MobCategory.CREATURE, b -> b.sized(0.9F, 1.0F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<Cheetah>> CHEETAH = ENTITY_TYPES.registerEntityType(
            "cheetah", Cheetah::new, MobCategory.CREATURE, b -> b.sized(0.8F, 0.95F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<Gazelle>> GAZELLE = ENTITY_TYPES.registerEntityType(
            "gazelle", Gazelle::new, MobCategory.CREATURE, b -> b.sized(0.7F, 1.1F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<Ostrich>> OSTRICH = ENTITY_TYPES.registerEntityType(
            "ostrich", Ostrich::new, MobCategory.CREATURE, b -> b.sized(0.9F, 2.1F).eyeHeight(1.95F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<Aardvark>> AARDVARK = ENTITY_TYPES.registerEntityType(
            "aardvark", Aardvark::new, MobCategory.CREATURE, b -> b.sized(0.8F, 0.8F).clientTrackingRange(8));

    // ---- meat (the hyena's is the cave hyena's Hyena Meat) ----
    public static final DeferredItem<Item> CHEETAH_MEAT = food("cheetah_meat", 1, 0.1F);
    public static final DeferredItem<Item> COOKED_CHEETAH_MEAT = food("cooked_cheetah_meat", 3, 0.6F);
    public static final DeferredItem<Item> GAZELLE_MEAT = food("gazelle_meat", 1, 0.1F);
    public static final DeferredItem<Item> COOKED_GAZELLE_MEAT = food("cooked_gazelle_meat", 3, 0.8F);
    public static final DeferredItem<Item> OSTRICH_MEAT = food("ostrich_meat", 1, 0.1F);
    public static final DeferredItem<Item> COOKED_OSTRICH_MEAT = food("cooked_ostrich_meat", 3, 0.8F);
    public static final DeferredItem<Item> AARDVARK_MEAT = food("aardvark_meat", 1, 0.1F);
    public static final DeferredItem<Item> COOKED_AARDVARK_MEAT = food("cooked_aardvark_meat", 3, 0.7F);
    /** Laid on the ground by ostrich hens; one roasted egg is a whole meal. */
    public static final DeferredItem<Item> OSTRICH_EGG = ITEMS.registerSimpleItem("ostrich_egg", p -> p.stacksTo(4));
    public static final DeferredItem<Item> COOKED_OSTRICH_EGG = ITEMS.registerSimpleItem("cooked_ostrich_egg",
            p -> p.stacksTo(4).food(new FoodProperties.Builder().nutrition(12).saturationModifier(0.9F).build()));
    /** Fished out of a Termite Mound with a stick: a crunchy mouthful, or pounded into Chitin Paste. */
    public static final DeferredItem<Item> TERMITES = ITEMS.registerSimpleItem("termites",
            p -> p.food(new FoodProperties.Builder().nutrition(1).saturationModifier(0.2F).build()));

    // ---- hides and parts ----
    /** The spotted hyena's coarse hide: scraped (knife) into 2 Scraped Hides. */
    public static final DeferredItem<Item> HYENA_HIDE = ITEMS.registerSimpleItem("hyena_hide");
    /** Fat-tanned in the crafting grid (knife + #mechanical:tanning_fats) into a Cheetah Pelt. */
    public static final DeferredItem<Item> RAW_CHEETAH_PELT = ITEMS.registerSimpleItem("raw_cheetah_pelt");
    public static final DeferredItem<Item> CHEETAH_PELT = ITEMS.registerSimpleItem("cheetah_pelt");
    public static final DeferredItem<Item> RAW_GAZELLE_HIDE = ITEMS.registerSimpleItem("raw_gazelle_hide");
    public static final DeferredItem<Item> GAZELLE_HORN = ITEMS.registerSimpleItem("gazelle_horn");
    public static final DeferredItem<Item> OSTRICH_FEATHER = ITEMS.registerSimpleItem("ostrich_feather");
    public static final DeferredItem<Item> RAW_OSTRICH_HIDE = ITEMS.registerSimpleItem("raw_ostrich_hide");
    public static final DeferredItem<Item> RAW_AARDVARK_HIDE = ITEMS.registerSimpleItem("raw_aardvark_hide");
    public static final DeferredItem<Item> AARDVARK_CLAW = ITEMS.registerSimpleItem("aardvark_claw");
    /** A glue pounded from termites: stands in for Hide Glue. */
    public static final DeferredItem<Item> CHITIN_PASTE = ITEMS.registerSimpleItem("chitin_paste");

    // ---- made from them ----
    /** A gazelle horn hollowed into a horn to blow: heard far off, and it tells your tribe mates where you are. */
    public static final DeferredItem<SignalHornItem> SIGNAL_HORN = ITEMS.registerItem("signal_horn", SignalHornItem::new,
            p -> p.stacksTo(1).durability(120));
    private static final ArmorMaterial CHEETAH_CLOAK_MATERIAL = new ArmorMaterial(8, Map.of(ArmorType.HELMET, 1, ArmorType.CHESTPLATE, 1,
            ArmorType.LEGGINGS, 1, ArmorType.BOOTS, 1, ArmorType.BODY, 1), 18, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F,
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "repairs_cheetah_pelt")),
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(MODID, "cheetah_cloak")));
    /** A hunter's trophy for the Cloak slot: a cheetah pelt over the shoulders, the tail hanging down the back. */
    public static final DeferredItem<com.mechanical.equipment.CloakItem> CHEETAH_CLOAK = ITEMS.registerItem("cheetah_cloak",
            p -> new com.mechanical.equipment.CloakItem(p, CHEETAH_CLOAK_MATERIAL, true),
            p -> com.mechanical.equipment.CloakItem.properties(p, CHEETAH_CLOAK_MATERIAL));

    // ---- the termite mound ----
    public static final DeferredBlock<TermiteMoundBlock> TERMITE_MOUND = BLOCKS.registerBlock("termite_mound", TermiteMoundBlock::new,
            p -> p.mapColor(MapColor.TERRACOTTA_ORANGE).strength(1.2F, 3.0F).sound(SoundType.ROOTED_DIRT));
    public static final DeferredItem<BlockItem> TERMITE_MOUND_ITEM = ITEMS.registerSimpleBlockItem("termite_mound", TERMITE_MOUND);

    public static final DeferredItem<SpawnEggItem> SPOTTED_HYENA_SPAWN_EGG = egg("spotted_hyena_spawn_egg", SPOTTED_HYENA);
    public static final DeferredItem<SpawnEggItem> CHEETAH_SPAWN_EGG = egg("cheetah_spawn_egg", CHEETAH);
    public static final DeferredItem<SpawnEggItem> GAZELLE_SPAWN_EGG = egg("gazelle_spawn_egg", GAZELLE);
    public static final DeferredItem<SpawnEggItem> OSTRICH_SPAWN_EGG = egg("ostrich_spawn_egg", OSTRICH);
    public static final DeferredItem<SpawnEggItem> AARDVARK_SPAWN_EGG = egg("aardvark_spawn_egg", AARDVARK);

    /** The Signal Horn's call: heard 256 blocks off. */
    public static final SoundEvent SIGNAL_HORN_SOUND = SoundEvent.createFixedRangeEvent(Identifier.fromNamespaceAndPath(MODID, "item.signal_horn.blow"), 256.0F);

    private static DeferredItem<Item> food(String name, int nutrition, float saturation) {
        return ITEMS.registerSimpleItem(name, p -> p.food(new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).build()));
    }

    private static DeferredItem<SpawnEggItem> egg(String name, DeferredHolder<EntityType<?>, ? extends EntityType<?>> type) {
        return ITEMS.registerItem(name, SpawnEggItem::new, p -> p.spawnEgg(type.get()));
    }

    private static Extra x(java.util.function.Supplier<Item> item, float chance, int min, int max) {
        return new Extra(item, chance, min, max);
    }

    /** Registers everything (class load), the butchering yields and the game tests; call from the mod constructor. */
    public static void init(IEventBus modEventBus) {
        Butchery.register(SPOTTED_HYENA::get, new Yield(MountainFauna.HYENA_MEAT, 1, 3, List.of(x(HYENA_HIDE, 0.85F, 1, 1)),
                List.of(x(MountainFauna.HYENA_FANG, 0.3F, 1, 2), x(Wildlife.SINEW, 0.4F, 1, 1), x(() -> Items.BONE, 0.5F, 1, 2))));
        Butchery.register(CHEETAH::get, new Yield(CHEETAH_MEAT, 1, 2, List.of(x(RAW_CHEETAH_PELT, 1.0F, 1, 1)),
                List.of(x(Wildlife.SINEW, 0.6F, 1, 2), x(() -> Items.BONE, 0.5F, 1, 1))));
        // the horns come off with the hide (outer pass); lean meat and sinew under it
        Butchery.register(GAZELLE::get, new Yield(GAZELLE_MEAT, 1, 3, List.of(x(RAW_GAZELLE_HIDE, 0.6F, 1, 1), x(GAZELLE_HORN, 0.9F, 1, 2)),
                List.of(x(Wildlife.SINEW, 0.5F, 1, 2))));
        Butchery.register(OSTRICH::get, new Yield(OSTRICH_MEAT, 3, 5, List.of(x(OSTRICH_FEATHER, 1.0F, 2, 4), x(RAW_OSTRICH_HIDE, 0.5F, 1, 1)),
                List.of(x(Wildlife.SINEW, 0.6F, 1, 2), x(() -> Items.BONE, 0.5F, 1, 2))));
        Butchery.register(AARDVARK::get, new Yield(AARDVARK_MEAT, 2, 3, List.of(x(RAW_AARDVARK_HIDE, 0.7F, 1, 1)),
                List.of(x(AARDVARK_CLAW, 0.6F, 1, 2), x(Wildlife.LARD, 0.4F, 1, 1))));
        SavannaTest.init(modEventBus);
    }

    @SubscribeEvent
    static void attributes(EntityAttributeCreationEvent event) {
        event.put(SPOTTED_HYENA.get(), SpottedHyena.createAttributes().build());
        event.put(CHEETAH.get(), Cheetah.createAttributes().build());
        event.put(GAZELLE.get(), Gazelle.createAttributes().build());
        event.put(OSTRICH.get(), Ostrich.createAttributes().build());
        event.put(AARDVARK.get(), Aardvark.createAttributes().build());
    }

    @SubscribeEvent
    static void registerSounds(RegisterEvent event) {
        event.register(Registries.SOUND_EVENT, SIGNAL_HORN_SOUND.location(), () -> SIGNAL_HORN_SOUND);
    }

    /** Savanna ground: grass, coarse dirt, sand, in daylight. Reads only this column. */
    private static boolean savannaGround(LevelAccessor level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return (below.is(BlockTags.ANIMALS_SPAWNABLE_ON) || below.is(Blocks.COARSE_DIRT) || below.is(BlockTags.SAND)) && level.getRawBrightness(pos, 0) > 8;
    }

    @SubscribeEvent
    static void spawnRules(RegisterSpawnPlacementsEvent event) {
        for (EntityType<? extends Animal> type : List.<EntityType<? extends Animal>>of(SPOTTED_HYENA.get(), CHEETAH.get(), GAZELLE.get(),
                OSTRICH.get(), AARDVARK.get())) {
            register(event, type);
        }
    }

    private static <T extends Animal> void register(RegisterSpawnPlacementsEvent event, EntityType<T> type) {
        event.register(type, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (t, level, reason, pos, random) -> savannaGround(level, pos), RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private SavannaFauna() {}
}
