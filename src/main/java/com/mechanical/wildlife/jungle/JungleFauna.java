package com.mechanical.wildlife.jungle;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ENTITY_TYPES;
import static com.mechanical.Mechanical.ITEMS;

import java.util.List;
import java.util.function.Supplier;

import com.mechanical.BearRugBlock;
import com.mechanical.Butchery;
import com.mechanical.Butchery.Extra;
import com.mechanical.Butchery.Yield;
import com.mechanical.Mechanical;
import com.mechanical.wildlife.Wildlife;
import com.mechanical.wildlife.meadow.MeadowFauna;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
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
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Jungle and mangrove fauna. Jungle / bamboo jungle: Tiger (stalks hidden in the thicket; Tiger Pelt, rug, mantle), Python
 * (lies in trees and at the water's edge and constricts what passes, see {@link Grip}), Orangutan (throws fruit and
 * sticks, steals what lies on the ground), Monitor Lizard (venomous bite, eats carcasses) and Poison Dart Frog (poison for
 * the Blowpipe's darts). Mangrove swamp: Saltwater Crocodile (lurks in the water, drags prey under in a death roll;
 * Crocodile Hide armour), Mudskipper (walks on the mud; bait), Fiddler Crab (ducks into its mud hole; crab meat) and
 * Proboscis Monkey (noisy: its alarm honk sends every animal round it running).
 * Data and lang: scripts/gen_fauna_jungle.py, textures: scripts/gen_fauna_jungle_textures.py, docs: docs/fauna/jungle.md.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class JungleFauna {
    // ---- animals ----
    public static final DeferredHolder<EntityType<?>, EntityType<Tiger>> TIGER = ENTITY_TYPES.registerEntityType(
            "tiger", Tiger::new, MobCategory.CREATURE, b -> b.sized(1.1F, 1.1F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<Python>> PYTHON = ENTITY_TYPES.registerEntityType(
            "python", Python::new, MobCategory.CREATURE, b -> b.sized(0.9F, 0.45F).clientTrackingRange(8));
    public static final DeferredHolder<EntityType<?>, EntityType<Orangutan>> ORANGUTAN = ENTITY_TYPES.registerEntityType(
            "orangutan", Orangutan::new, MobCategory.CREATURE, b -> b.sized(0.9F, 1.3F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<MonitorLizard>> MONITOR_LIZARD = ENTITY_TYPES.registerEntityType(
            "monitor_lizard", MonitorLizard::new, MobCategory.CREATURE, b -> b.sized(0.9F, 0.5F).clientTrackingRange(8));
    public static final DeferredHolder<EntityType<?>, EntityType<DartFrog>> POISON_DART_FROG = ENTITY_TYPES.registerEntityType(
            "poison_dart_frog", DartFrog::new, MobCategory.CREATURE, b -> b.sized(0.3F, 0.25F).clientTrackingRange(6));
    public static final DeferredHolder<EntityType<?>, EntityType<Crocodile>> SALTWATER_CROCODILE = ENTITY_TYPES.registerEntityType(
            "saltwater_crocodile", Crocodile::new, MobCategory.CREATURE, b -> b.sized(1.4F, 0.7F).eyeHeight(0.55F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<Mudskipper>> MUDSKIPPER = ENTITY_TYPES.registerEntityType(
            "mudskipper", Mudskipper::new, MobCategory.CREATURE, b -> b.sized(0.35F, 0.25F).clientTrackingRange(6));
    public static final DeferredHolder<EntityType<?>, EntityType<FiddlerCrab>> FIDDLER_CRAB = ENTITY_TYPES.registerEntityType(
            "fiddler_crab", FiddlerCrab::new, MobCategory.CREATURE, b -> b.sized(0.4F, 0.25F).clientTrackingRange(6));
    public static final DeferredHolder<EntityType<?>, EntityType<ProboscisMonkey>> PROBOSCIS_MONKEY = ENTITY_TYPES.registerEntityType(
            "proboscis_monkey", ProboscisMonkey::new, MobCategory.CREATURE, b -> b.sized(0.6F, 0.95F).clientTrackingRange(10));

    // ---- meat (game-sized like the warm fauna) ----
    public static final DeferredItem<Item> TIGER_MEAT = food("tiger_meat", 1, 0.1F);
    public static final DeferredItem<Item> COOKED_TIGER_MEAT = food("cooked_tiger_meat", 3, 0.6F);
    public static final DeferredItem<Item> PYTHON_MEAT = food("python_meat", 1, 0.2F);
    public static final DeferredItem<Item> COOKED_PYTHON_MEAT = food("cooked_python_meat", 4, 0.6F);
    public static final DeferredItem<Item> ORANGUTAN_MEAT = food("orangutan_meat", 1, 0.1F);
    public static final DeferredItem<Item> COOKED_ORANGUTAN_MEAT = food("cooked_orangutan_meat", 3, 0.6F);
    public static final DeferredItem<Item> MONITOR_MEAT = food("monitor_meat", 1, 0.2F);
    public static final DeferredItem<Item> COOKED_MONITOR_MEAT = food("cooked_monitor_meat", 3, 0.7F);
    public static final DeferredItem<Item> CROCODILE_MEAT = food("crocodile_meat", 1, 0.2F);
    public static final DeferredItem<Item> COOKED_CROCODILE_MEAT = food("cooked_crocodile_meat", 4, 0.8F);
    public static final DeferredItem<Item> MONKEY_MEAT = food("monkey_meat", 1, 0.1F);
    public static final DeferredItem<Item> COOKED_MONKEY_MEAT = food("cooked_monkey_meat", 3, 0.6F);
    /** A whole mudskipper (what it drops): eaten like a small raw fish, cooked, or set as snare / hook bait. */
    public static final DeferredItem<Item> MUDSKIPPER_ITEM = ITEMS.registerSimpleItem("mudskipper",
            p -> p.food(new FoodProperties.Builder().nutrition(1).saturationModifier(0.1F).build()));
    public static final DeferredItem<Item> COOKED_MUDSKIPPER = food("cooked_mudskipper", 3, 0.6F);
    public static final DeferredItem<Item> RAW_CRAB_MEAT = ITEMS.registerSimpleItem("raw_crab_meat",
            p -> p.food(new FoodProperties.Builder().nutrition(1).saturationModifier(0.2F).build(), Consumables.CHICKEN));
    public static final DeferredItem<Item> COOKED_CRAB_MEAT = food("cooked_crab_meat", 4, 0.7F);

    // ---- hides and parts ----
    /** Fresh off the carcass; fat-tanned in the grid (knife + #mechanical:tanning_fats) like the leopard's. */
    public static final DeferredItem<Item> RAW_TIGER_PELT = ITEMS.registerSimpleItem("raw_tiger_pelt");
    public static final DeferredItem<Item> TIGER_PELT = ITEMS.registerSimpleItem("tiger_pelt");
    /** The whole shed-length skin: + knife -> 3 Snakeskin. */
    public static final DeferredItem<Item> PYTHON_SKIN = ITEMS.registerSimpleItem("python_skin");
    /** Long red hair: spun (2 in the grid) into String. */
    public static final DeferredItem<Item> ORANGUTAN_HAIR = ITEMS.registerSimpleItem("orangutan_hair");
    /** Beaded monitor skin: 2 -> 1 Leather. */
    public static final DeferredItem<Item> MONITOR_SKIN = ITEMS.registerSimpleItem("monitor_skin");
    /** Scraped off a dead Poison Dart Frog's back: tips Darts into Poison Darts. */
    public static final DeferredItem<Item> FROG_POISON = ITEMS.registerSimpleItem("frog_poison");
    /** Fat-tanned like the pelts; the scaly hard leather of the Crocodile Hide armour. */
    public static final DeferredItem<Item> RAW_CROCODILE_HIDE = ITEMS.registerSimpleItem("raw_crocodile_hide");
    public static final DeferredItem<Item> CROCODILE_HIDE = ITEMS.registerSimpleItem("crocodile_hide");

    // ---- made from them ----
    public static final ArmorMaterial TIGER_FUR = new ArmorMaterial(9,
            java.util.Map.of(ArmorType.HELMET, 1, ArmorType.CHESTPLATE, 3, ArmorType.LEGGINGS, 2, ArmorType.BOOTS, 1, ArmorType.BODY, 3),
            14, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F, itemTag("repairs_tiger_fur"), asset("tiger_fur"));
    /** Worn in the Cloak slot over the armour: a striped mantle, the hunter's trophy. */
    public static final DeferredItem<com.mechanical.equipment.CloakItem> TIGER_PELT_MANTLE = ITEMS.registerItem("tiger_pelt_mantle",
            p -> new com.mechanical.equipment.CloakItem(p, TIGER_FUR, true), p -> com.mechanical.equipment.CloakItem.properties(p, TIGER_FUR));
    public static final DeferredBlock<BearRugBlock> TIGER_RUG = BLOCKS.registerBlock("tiger_rug", BearRugBlock::new, p -> p
            .mapColor(MapColor.COLOR_ORANGE).strength(0.2F).sound(SoundType.WOOL).noOcclusion());
    public static final DeferredItem<BlockItem> TIGER_RUG_ITEM = ITEMS.registerSimpleBlockItem("tiger_rug", TIGER_RUG);
    /** Hard scaly leather: tougher than walrus hide, a little heavier (knockback resistance), no warmth. */
    public static final ArmorMaterial CROCODILE_HIDE_MATERIAL = new ArmorMaterial(16,
            java.util.Map.of(ArmorType.HELMET, 2, ArmorType.CHESTPLATE, 5, ArmorType.LEGGINGS, 4, ArmorType.BOOTS, 2, ArmorType.BODY, 5),
            9, SoundEvents.ARMOR_EQUIP_LEATHER, 1.0F, 0.05F, itemTag("repairs_crocodile_hide_armor"), asset("crocodile_hide"));
    public static final DeferredItem<Item> CROCODILE_HIDE_HELMET = armour("crocodile_hide_helmet", ArmorType.HELMET);
    public static final DeferredItem<Item> CROCODILE_HIDE_CUIRASS = armour("crocodile_hide_cuirass", ArmorType.CHESTPLATE);
    public static final DeferredItem<Item> CROCODILE_HIDE_LEGGINGS = armour("crocodile_hide_leggings", ArmorType.LEGGINGS);
    public static final DeferredItem<Item> CROCODILE_HIDE_BOOTS = armour("crocodile_hide_boots", ArmorType.BOOTS);
    /** A hollow bamboo tube: right-click fires a Dart (Poison Darts first) from the inventory. */
    public static final DeferredItem<BlowpipeItem> BLOWPIPE = ITEMS.registerItem("blowpipe", BlowpipeItem::new, p -> p.stacksTo(1).durability(160));
    public static final DeferredItem<Item> DART = ITEMS.registerSimpleItem("dart");
    public static final DeferredItem<Item> POISON_DART = ITEMS.registerSimpleItem("poison_dart");

    public static final DeferredItem<SpawnEggItem> TIGER_SPAWN_EGG = egg("tiger_spawn_egg", TIGER);
    public static final DeferredItem<SpawnEggItem> PYTHON_SPAWN_EGG = egg("python_spawn_egg", PYTHON);
    public static final DeferredItem<SpawnEggItem> ORANGUTAN_SPAWN_EGG = egg("orangutan_spawn_egg", ORANGUTAN);
    public static final DeferredItem<SpawnEggItem> MONITOR_LIZARD_SPAWN_EGG = egg("monitor_lizard_spawn_egg", MONITOR_LIZARD);
    public static final DeferredItem<SpawnEggItem> POISON_DART_FROG_SPAWN_EGG = egg("poison_dart_frog_spawn_egg", POISON_DART_FROG);
    public static final DeferredItem<SpawnEggItem> SALTWATER_CROCODILE_SPAWN_EGG = egg("saltwater_crocodile_spawn_egg", SALTWATER_CROCODILE);
    public static final DeferredItem<SpawnEggItem> MUDSKIPPER_SPAWN_EGG = egg("mudskipper_spawn_egg", MUDSKIPPER);
    public static final DeferredItem<SpawnEggItem> FIDDLER_CRAB_SPAWN_EGG = egg("fiddler_crab_spawn_egg", FIDDLER_CRAB);
    public static final DeferredItem<SpawnEggItem> PROBOSCIS_MONKEY_SPAWN_EGG = egg("proboscis_monkey_spawn_egg", PROBOSCIS_MONKEY);

    private static TagKey<Item> itemTag(String name) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mechanical.MODID, name));
    }

    private static ResourceKey<net.minecraft.world.item.equipment.EquipmentAsset> asset(String name) {
        return ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Mechanical.MODID, name));
    }

    private static DeferredItem<Item> food(String name, int nutrition, float saturation) {
        return ITEMS.registerSimpleItem(name, p -> p.food(new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).build()));
    }

    private static DeferredItem<Item> armour(String name, ArmorType type) {
        return ITEMS.registerSimpleItem(name, p -> p.humanoidArmor(CROCODILE_HIDE_MATERIAL, type));
    }

    private static DeferredItem<SpawnEggItem> egg(String name, DeferredHolder<EntityType<?>, ? extends EntityType<?>> type) {
        return ITEMS.registerItem(name, SpawnEggItem::new, p -> p.spawnEgg(type.get()));
    }

    private static Extra x(Supplier<Item> item, float chance, int min, int max) {
        return new Extra(item, chance, min, max);
    }

    /** Registers everything (class load) and the butchering yields; call from the mod constructor. */
    public static void init() {
        Butchery.register(TIGER::get, new Yield(TIGER_MEAT, 2, 4, List.of(x(RAW_TIGER_PELT, 1.0F, 1, 1)),
                List.of(x(Wildlife.SINEW, 0.6F, 1, 2), x(() -> Items.BONE, 0.7F, 1, 2))));
        Butchery.toughHide(TIGER::get);
        // the whole skin peels off first, then the long body is meat
        Butchery.register(PYTHON::get, new Yield(PYTHON_MEAT, 2, 3, List.of(x(PYTHON_SKIN, 1.0F, 1, 1)), List.of()));
        Butchery.alwaysCarcass(PYTHON::get);
        Butchery.register(ORANGUTAN::get, new Yield(ORANGUTAN_MEAT, 1, 3, List.of(x(ORANGUTAN_HAIR, 0.9F, 1, 3), x(Mechanical.RAW_HIDE, 0.3F, 1, 1)),
                List.of(x(Wildlife.SINEW, 0.4F, 1, 1))));
        Butchery.register(MONITOR_LIZARD::get, new Yield(MONITOR_MEAT, 1, 2, List.of(x(MONITOR_SKIN, 0.8F, 1, 1)),
                List.of(x(MeadowFauna.VENOM_GLAND, 0.5F, 1, 1))));
        Butchery.register(SALTWATER_CROCODILE::get, new Yield(CROCODILE_MEAT, 3, 5, List.of(x(RAW_CROCODILE_HIDE, 1.0F, 1, 1)),
                List.of(x(Wildlife.SINEW, 0.5F, 1, 2), x(() -> Items.BONE, 0.6F, 1, 2))));
        Butchery.toughHide(SALTWATER_CROCODILE::get); // armoured back: tusk-grade knife or better
        Butchery.register(PROBOSCIS_MONKEY::get, new Yield(MONKEY_MEAT, 1, 2, List.of(x(Mechanical.RAW_HIDE, 0.3F, 1, 1)), List.of()));
        // tiny ones just drop their loot (loot tables): the frog its poison, the mudskipper itself, the crab its meat
        Butchery.neverCarcass(POISON_DART_FROG::get);
        Butchery.neverCarcass(MUDSKIPPER::get);
        Butchery.neverCarcass(FIDDLER_CRAB::get);
    }

    @SubscribeEvent
    static void attributes(EntityAttributeCreationEvent event) {
        event.put(TIGER.get(), Tiger.createAttributes().build());
        event.put(PYTHON.get(), Python.createAttributes().build());
        event.put(ORANGUTAN.get(), Orangutan.createAttributes().build());
        event.put(MONITOR_LIZARD.get(), MonitorLizard.createAttributes().build());
        event.put(POISON_DART_FROG.get(), DartFrog.createAttributes().build());
        event.put(SALTWATER_CROCODILE.get(), Crocodile.createAttributes().build());
        event.put(MUDSKIPPER.get(), Mudskipper.createAttributes().build());
        event.put(FIDDLER_CRAB.get(), FiddlerCrab.createAttributes().build());
        event.put(PROBOSCIS_MONKEY.get(), ProboscisMonkey.createAttributes().build());
    }

    /** Jungle floor and mangrove mud: grass, podzol, moss, mud, mangrove roots, sand; in daylight. Reads only this column. */
    static boolean wetGround(LevelAccessor level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return (below.is(BlockTags.ANIMALS_SPAWNABLE_ON) || below.is(Blocks.MUD) || below.is(Blocks.MOSS_BLOCK) || below.is(Blocks.PODZOL)
                || below.is(Blocks.MUDDY_MANGROVE_ROOTS) || below.is(Blocks.MANGROVE_ROOTS) || below.is(BlockTags.SAND))
                && level.getRawBrightness(pos, 0) > 8;
    }

    @SubscribeEvent
    static void spawnRules(RegisterSpawnPlacementsEvent event) {
        for (EntityType<? extends Animal> type : List.<EntityType<? extends Animal>>of(TIGER.get(), ORANGUTAN.get(), MONITOR_LIZARD.get(),
                POISON_DART_FROG.get(), SALTWATER_CROCODILE.get(), MUDSKIPPER.get(), FIDDLER_CRAB.get(), PROBOSCIS_MONKEY.get())) {
            wet(event, type, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES);
        }
        // pythons also turn up lying on the canopy: the leaves count as ground for them
        event.register(PYTHON.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING,
                (t, level, reason, pos, random) -> wetGround(level, pos) || level.getBlockState(pos.below()).is(BlockTags.LEAVES),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private static <T extends Animal> void wet(RegisterSpawnPlacementsEvent event, EntityType<T> type, Heightmap.Types heightmap) {
        event.register(type, SpawnPlacementTypes.ON_GROUND, heightmap, (t, level, reason, pos, random) -> wetGround(level, pos),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private JungleFauna() {}
}
