package com.mechanical.wildlife.temperate;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ENTITY_TYPES;
import static com.mechanical.Mechanical.ITEMS;
import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.mechanical.BearRugBlock;
import com.mechanical.Birds;
import com.mechanical.Butchery;
import com.mechanical.Mechanical;
import com.mechanical.wildlife.FlockBird;
import com.mechanical.wildlife.Wildlife;
import com.mechanical.wildlife.coldhunters.ColdhuntersFauna;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.RemoveStatusEffectsConsumeEffect;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Temperate fauna: plains, broadleaf and dark forest, fens and swamps, rivers and coasts. New animals: Wisent, European
 * Pond Turtle, Water Vole, Giant Stag Beetle, Woodpecker, Tawny Owl, Skylark, Lapwing, Bittern, White-tailed Sea Eagle and
 * Eel. Extensions of existing ones: the red fox's night raids (RedFox, vanilla's fox), the stoat's dance (StoatDance, the
 * Ermine), wild boar sounders with a tusker and grey seal colonies (SoundersAndColonies), the salmon run (SalmonRun).
 * Mechanics: leeches (Leeches), eels and crayfish in the Fish Trap (FishTrapBlock), Bumblebee Nests, Grub Logs, Fox Dens.
 * Entity types, items, blocks, spawn eggs, attributes, spawn rules and butchering yields. Data: scripts/gen_fauna_temperate.py,
 * textures: scripts/gen_fauna_temperate_textures.py, docs: docs/fauna/temperate.md.
 */
@EventBusSubscriber(modid = MODID)
public final class TemperateFauna {
    // ---- animals ----
    public static final DeferredHolder<EntityType<?>, EntityType<Wisent>> WISENT = ENTITY_TYPES.registerEntityType(
            "wisent", Wisent::new, MobCategory.CREATURE, b -> b.sized(1.4F, 1.8F).eyeHeight(1.2F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<PondTurtle>> POND_TURTLE = ENTITY_TYPES.registerEntityType(
            "pond_turtle", PondTurtle::new, MobCategory.CREATURE, b -> b.sized(0.55F, 0.35F).clientTrackingRange(8));
    public static final DeferredHolder<EntityType<?>, EntityType<WaterVole>> WATER_VOLE = ENTITY_TYPES.registerEntityType(
            "water_vole", WaterVole::new, MobCategory.CREATURE, b -> b.sized(0.4F, 0.3F).clientTrackingRange(8));
    public static final DeferredHolder<EntityType<?>, EntityType<StagBeetle>> STAG_BEETLE = ENTITY_TYPES.registerEntityType(
            "stag_beetle", StagBeetle::new, MobCategory.CREATURE, b -> b.sized(0.4F, 0.25F).clientTrackingRange(6));
    public static final DeferredHolder<EntityType<?>, EntityType<Woodpecker>> WOODPECKER = ENTITY_TYPES.registerEntityType(
            "woodpecker", Woodpecker::new, MobCategory.CREATURE, b -> b.sized(0.4F, 0.45F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<TawnyOwl>> TAWNY_OWL = ENTITY_TYPES.registerEntityType(
            "tawny_owl", TawnyOwl::new, MobCategory.CREATURE, b -> b.sized(0.45F, 0.7F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<Skylark>> SKYLARK = ENTITY_TYPES.registerEntityType(
            "skylark", (t, l) -> new Skylark(t, l, Skylark.SKYLARK), MobCategory.CREATURE, b -> b.sized(0.35F, 0.35F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<Skylark>> LAPWING = ENTITY_TYPES.registerEntityType(
            "lapwing", (t, l) -> new Skylark(t, l, Skylark.LAPWING), MobCategory.CREATURE, b -> b.sized(0.4F, 0.45F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<Bittern>> BITTERN = ENTITY_TYPES.registerEntityType(
            "bittern", Bittern::new, MobCategory.CREATURE, b -> b.sized(0.5F, 0.8F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<SeaEagle>> SEA_EAGLE = ENTITY_TYPES.registerEntityType(
            "sea_eagle", SeaEagle::new, MobCategory.CREATURE, b -> b.sized(0.8F, 0.8F).clientTrackingRange(12));
    public static final DeferredHolder<EntityType<?>, EntityType<Eel>> EEL = ENTITY_TYPES.registerEntityType(
            "eel", Eel::new, MobCategory.WATER_AMBIENT, b -> b.sized(0.6F, 0.25F).eyeHeight(0.15F).clientTrackingRange(6));

    // ---- meat (scrap-sized like the other game; small birds like Birds') ----
    public static final DeferredItem<Item> RAW_WISENT = food("raw_wisent", 1, 0.1F);
    public static final DeferredItem<Item> COOKED_WISENT = food("cooked_wisent", 3, 0.9F);
    public static final DeferredItem<Item> POND_TURTLE_MEAT = food("pond_turtle_meat", 1, 0.1F);
    public static final DeferredItem<Item> COOKED_POND_TURTLE_MEAT = food("cooked_pond_turtle_meat", 3, 0.7F);
    public static final DeferredItem<Item> RAW_WATER_VOLE = food("raw_water_vole", 1, 0.1F);
    public static final DeferredItem<Item> COOKED_WATER_VOLE = food("cooked_water_vole", 2, 0.5F);
    public static final DeferredItem<Item> RAW_SKYLARK = rawBird("raw_skylark", 1);
    public static final DeferredItem<Item> COOKED_SKYLARK = food("cooked_skylark", 3, 0.6F);
    public static final DeferredItem<Item> RAW_LAPWING = rawBird("raw_lapwing", 1);
    public static final DeferredItem<Item> COOKED_LAPWING = food("cooked_lapwing", 4, 0.6F);
    public static final DeferredItem<Item> RAW_BITTERN = rawBird("raw_bittern", 2);
    public static final DeferredItem<Item> COOKED_BITTERN = food("cooked_bittern", 5, 0.7F);
    public static final DeferredItem<Item> PLUCKED_SKYLARK = ITEMS.registerSimpleItem("plucked_skylark", p -> p.stacksTo(16));
    public static final DeferredItem<Item> PLUCKED_LAPWING = ITEMS.registerSimpleItem("plucked_lapwing", p -> p.stacksTo(16));
    public static final DeferredItem<Item> PLUCKED_BITTERN = ITEMS.registerSimpleItem("plucked_bittern", p -> p.stacksTo(16));
    public static final DeferredItem<Item> SKYLARK_EGG = ITEMS.registerSimpleItem("skylark_egg", p -> p.stacksTo(16));
    public static final DeferredItem<Item> LAPWING_EGG = ITEMS.registerSimpleItem("lapwing_egg", p -> p.stacksTo(16));
    /** Dies as this (like vanilla fish); gutted with a knife into Raw Eel. */
    public static final DeferredItem<Item> WHOLE_EEL = ITEMS.registerSimpleItem("whole_eel");
    public static final DeferredItem<Item> RAW_EEL = food("raw_eel", 2, 0.1F);
    public static final DeferredItem<Item> COOKED_EEL = food("cooked_eel", 6, 0.9F);
    /** From the Fish Trap in Reaping (FishTrapBlock): boiled or roasted whole. */
    public static final DeferredItem<Item> CRAYFISH = food("crayfish", 1, 0.1F);
    public static final DeferredItem<Item> COOKED_CRAYFISH = food("cooked_crayfish", 3, 0.6F);
    /** From a Bumblebee Nest: sweet, and it soothes a sore throat (a little Regeneration). */
    public static final DeferredItem<Item> WILD_HONEY = ITEMS.registerSimpleItem("wild_honey", p -> p.food(
            new FoodProperties.Builder().nutrition(3).saturationModifier(0.4F).build(),
            Consumables.defaultFood().onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0))).build()));
    /** Beetle larvae dug out of a Grub Log: bait (snares, the hook), and a mouthful if you must. */
    public static final DeferredItem<Item> GRUB = food("grub", 1, 0.1F);

    // ---- hides, pelts and parts ----
    public static final DeferredItem<Item> RAW_FOX_PELT = ITEMS.registerSimpleItem("raw_fox_pelt");
    /** The red fox's pelt cured on a Drying Rack (ColdhuntersFauna.CURES). */
    public static final DeferredItem<Item> FOX_FUR = ITEMS.registerSimpleItem("fox_fur");
    /** Scraped (knife, crafting grid) into 3 Scraped Hides, or fat-tanned with the hair on into a Wisent Rug. */
    public static final DeferredItem<Item> RAW_WISENT_HIDE = ITEMS.registerSimpleItem("raw_wisent_hide");
    public static final DeferredItem<Item> POND_TURTLE_SHELL = ITEMS.registerSimpleItem("pond_turtle_shell", p -> p.stacksTo(16));
    /** The stag beetle's glossy chestnut wing cases: ground into pigment. */
    public static final DeferredItem<Item> BEETLE_SHELL = ITEMS.registerSimpleItem("beetle_shell");
    public static final DeferredItem<Item> WOODPECKER_FEATHER = ITEMS.registerSimpleItem("woodpecker_feather");
    /** Laid on the skin, it draws out poison (Poison and Nausea gone, a moment's Regeneration): the old leech cure. */
    public static final DeferredItem<Item> LEECH = ITEMS.registerSimpleItem("leech", p -> p.stacksTo(16)
            .component(DataComponents.CONSUMABLE, Consumable.builder().consumeSeconds(2.0F).animation(ItemUseAnimation.BRUSH)
                    .sound(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.SLIME_SQUISH_SMALL)).hasConsumeParticles(false)
                    .onConsume(new RemoveStatusEffectsConsumeEffect(net.minecraft.core.HolderSet.direct(MobEffects.POISON, MobEffects.NAUSEA)))
                    .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0))).build()));

    // ---- goods ----
    private static final ArmorMaterial FOX_HAT_MATERIAL = fur(7, 1, 15, "fox_fur", "fox_fur_hat");
    public static final DeferredItem<Item> FOX_FUR_HAT = ITEMS.registerSimpleItem("fox_fur_hat", p -> p.humanoidArmor(FOX_HAT_MATERIAL, ArmorType.HELMET));
    public static final DeferredBlock<BearRugBlock> WISENT_RUG = BLOCKS.registerBlock("wisent_rug", BearRugBlock::new, p -> p
            .mapColor(MapColor.COLOR_BROWN).strength(0.2F).sound(SoundType.WOOL).noOcclusion());
    public static final DeferredItem<BlockItem> WISENT_RUG_ITEM = ITEMS.registerSimpleBlockItem("wisent_rug", WISENT_RUG);

    // ---- blocks of the wild ----
    public static final DeferredBlock<GrubLogBlock> GRUB_LOG = BLOCKS.registerBlock("grub_log", GrubLogBlock::new, p -> p
            .mapColor(MapColor.TERRACOTTA_BROWN).strength(0.8F).sound(SoundType.NETHER_WOOD).randomTicks());
    public static final DeferredItem<BlockItem> GRUB_LOG_ITEM = ITEMS.registerSimpleBlockItem("grub_log", GRUB_LOG);
    public static final DeferredBlock<BumblebeeNestBlock> BUMBLEBEE_NEST = BLOCKS.registerBlock("bumblebee_nest", BumblebeeNestBlock::new, p -> p
            .mapColor(MapColor.GRASS).strength(0.4F).sound(SoundType.MOSS).noOcclusion().randomTicks().pushReaction(PushReaction.POPPED));
    public static final DeferredItem<BlockItem> BUMBLEBEE_NEST_ITEM = ITEMS.registerSimpleBlockItem("bumblebee_nest", BUMBLEBEE_NEST);
    public static final DeferredBlock<FoxDenBlock> FOX_DEN = BLOCKS.registerBlock("fox_den", FoxDenBlock::new, p -> p
            .mapColor(MapColor.DIRT).strength(0.5F).sound(SoundType.GRAVEL).noOcclusion());
    public static final DeferredItem<BlockItem> FOX_DEN_ITEM = ITEMS.registerSimpleBlockItem("fox_den", FOX_DEN);
    public static final DeferredBlock<FoxTracksBlock> FOX_TRACKS = BLOCKS.registerBlock("fox_tracks", FoxTracksBlock::new, p -> p
            .mapColor(MapColor.DIRT).strength(0.0F).noCollision().noOcclusion().randomTicks().sound(SoundType.GRASS)
            .pushReaction(PushReaction.POPPED).replaceable());

    public static final DeferredItem<SpawnEggItem> WISENT_SPAWN_EGG = egg("wisent_spawn_egg", WISENT);
    public static final DeferredItem<SpawnEggItem> POND_TURTLE_SPAWN_EGG = egg("pond_turtle_spawn_egg", POND_TURTLE);
    public static final DeferredItem<SpawnEggItem> WATER_VOLE_SPAWN_EGG = egg("water_vole_spawn_egg", WATER_VOLE);
    public static final DeferredItem<SpawnEggItem> STAG_BEETLE_SPAWN_EGG = egg("stag_beetle_spawn_egg", STAG_BEETLE);
    public static final DeferredItem<SpawnEggItem> WOODPECKER_SPAWN_EGG = egg("woodpecker_spawn_egg", WOODPECKER);
    public static final DeferredItem<SpawnEggItem> TAWNY_OWL_SPAWN_EGG = egg("tawny_owl_spawn_egg", TAWNY_OWL);
    public static final DeferredItem<SpawnEggItem> SKYLARK_SPAWN_EGG = egg("skylark_spawn_egg", SKYLARK);
    public static final DeferredItem<SpawnEggItem> LAPWING_SPAWN_EGG = egg("lapwing_spawn_egg", LAPWING);
    public static final DeferredItem<SpawnEggItem> BITTERN_SPAWN_EGG = egg("bittern_spawn_egg", BITTERN);
    public static final DeferredItem<SpawnEggItem> SEA_EAGLE_SPAWN_EGG = egg("sea_eagle_spawn_egg", SEA_EAGLE);
    public static final DeferredItem<SpawnEggItem> EEL_SPAWN_EGG = egg("eel_spawn_egg", EEL);

    static {
        ColdhuntersFauna.CURES.put(RAW_FOX_PELT, FOX_FUR);
    }

    private static DeferredItem<Item> food(String name, int nutrition, float saturation) {
        return ITEMS.registerSimpleItem(name, p -> p.food(new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).build()));
    }

    /** Raw bird meat: like vanilla raw chicken, a chance of Hunger when eaten raw. */
    private static DeferredItem<Item> rawBird(String name, int nutrition) {
        return ITEMS.registerSimpleItem(name, p -> p.food(new FoodProperties.Builder().nutrition(nutrition).saturationModifier(0.3F).build(), Consumables.CHICKEN));
    }

    private static DeferredItem<SpawnEggItem> egg(String name, DeferredHolder<EntityType<?>, ? extends EntityType<?>> type) {
        return ITEMS.registerItem(name, SpawnEggItem::new, p -> p.spawnEgg(type.get()));
    }

    /** A one-piece fur armour material: its own equipment asset, repaired with {@code repair}. */
    private static ArmorMaterial fur(int durability, int defense, int enchantability, String repair, String asset) {
        return new ArmorMaterial(durability, Map.of(ArmorType.HELMET, defense, ArmorType.CHESTPLATE, defense, ArmorType.LEGGINGS, defense,
                ArmorType.BOOTS, defense, ArmorType.BODY, defense), enchantability, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F,
                TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "repairs_" + repair)),
                ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(MODID, asset)));
    }

    private static Butchery.Extra x(Supplier<Item> item, float chance, int min, int max) {
        return new Butchery.Extra(item, chance, min, max);
    }

    /** Loads this class so its registrations happen, and registers the butchering yields. Call from the mod constructor. */
    public static void init() {
        // the red fox is vanilla's fox: its pelt now (VanillaGame no longer lists it)
        Butchery.register(() -> EntityTypes.FOX, new Butchery.Yield(Mechanical.GAME_MEAT_SCRAPS, 1, 1, List.of(x(RAW_FOX_PELT, 0.9F, 1, 1)),
                List.of(x(Wildlife.SINEW, 0.3F, 1, 1))));
        Butchery.register(WISENT::get, new Butchery.Yield(RAW_WISENT, 3, 6, List.of(x(RAW_WISENT_HIDE, 1.0F, 1, 1)),
                List.of(x(Wildlife.HORN, 0.6F, 1, 2), x(Wildlife.SINEW, 0.6F, 1, 2), x(Wildlife.LARD, 0.3F, 1, 2))));
        Butchery.toughHide(WISENT::get);
        // the shell comes off first; a little meat inside
        Butchery.register(POND_TURTLE::get, new Butchery.Yield(POND_TURTLE_MEAT, 1, 1, List.of(x(POND_TURTLE_SHELL, 1.0F, 1, 1)), List.of()));
        Butchery.register(WATER_VOLE::get, new Butchery.Yield(RAW_WATER_VOLE, 1, 1, List.of(), List.of()));
        Butchery.wholeMeat(WATER_VOLE::get);
        // small birds: plucked, then their own meat
        Butchery.register(SKYLARK::get, new Butchery.Yield(RAW_SKYLARK, 1, 1, List.of(x(() -> Items.FEATHER, 1.0F, 1, 1)), List.of()));
        Butchery.register(LAPWING::get, new Butchery.Yield(RAW_LAPWING, 1, 1, List.of(x(() -> Items.FEATHER, 1.0F, 1, 2)), List.of()));
        Butchery.register(BITTERN::get, new Butchery.Yield(RAW_BITTERN, 1, 1, List.of(x(() -> Items.FEATHER, 1.0F, 2, 3)), List.of()));
        Birds.registerEdible(SKYLARK::get, PLUCKED_SKYLARK);
        Birds.registerEdible(LAPWING::get, PLUCKED_LAPWING);
        Birds.registerEdible(BITTERN::get, PLUCKED_BITTERN);
        // no carcass: the beetle leaves its shell, the eel dies whole (loot tables); woodpecker, tawny owl and sea eagle are
        // flock birds that aren't game birds, so they drop feathers and parts (loot) like the eagle-owl
        Butchery.neverCarcass(STAG_BEETLE::get);
    }

    @SubscribeEvent
    static void attributes(EntityAttributeCreationEvent event) {
        event.put(WISENT.get(), Wisent.createAttributes().build());
        event.put(POND_TURTLE.get(), PondTurtle.createAttributes().build());
        event.put(WATER_VOLE.get(), WaterVole.createAttributes().build());
        event.put(STAG_BEETLE.get(), StagBeetle.createAttributes().build());
        event.put(WOODPECKER.get(), FlockBird.createAttributes(4.0).build());
        event.put(TAWNY_OWL.get(), FlockBird.createAttributes(5.0).build());
        event.put(SKYLARK.get(), FlockBird.createAttributes(3.0).build());
        event.put(LAPWING.get(), FlockBird.createAttributes(4.0).build());
        event.put(BITTERN.get(), FlockBird.createAttributes(8.0).build());
        event.put(SEA_EAGLE.get(), FlockBird.createAttributes(14.0).build());
        event.put(EEL.get(), Eel.createAttributes().build());
    }

    /** Natural ground in daylight (or any light, for the woodland floor), not leaves or water. */
    private static boolean ground(LevelAccessor level, BlockPos pos, boolean needsLight) {
        BlockState below = level.getBlockState(pos.below());
        boolean soil = below.is(BlockTags.ANIMALS_SPAWNABLE_ON) || below.is(Blocks.MUD) || below.is(Blocks.MOSS_BLOCK) || below.is(Blocks.PODZOL)
                || below.is(Blocks.COARSE_DIRT) || below.is(BlockTags.SAND) || below.is(Blocks.GRAVEL) || below.is(Blocks.STONE)
                || below.is(BlockTags.BASE_STONE_OVERWORLD) || below.is(BlockTags.LOGS);
        return soil && (!needsLight || level.getRawBrightness(pos, 0) > 8);
    }

    @SubscribeEvent
    static void spawnRules(RegisterSpawnPlacementsEvent event) {
        for (EntityType<? extends Animal> type : List.<EntityType<? extends Animal>>of(WISENT.get(), POND_TURTLE.get(), WATER_VOLE.get(),
                WOODPECKER.get(), TAWNY_OWL.get(), SKYLARK.get(), LAPWING.get(), BITTERN.get(), SEA_EAGLE.get())) {
            event.register(type, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (t, level, reason, pos, random) -> ground(level, pos, true), RegisterSpawnPlacementsEvent.Operation.REPLACE);
        }
        event.register(STAG_BEETLE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (t, level, reason, pos, random) -> ground(level, pos, false), RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(EEL.get(), SpawnPlacementTypes.IN_WATER, Heightmap.Types.OCEAN_FLOOR,
                WaterAnimal::checkSurfaceWaterAnimalSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    /** Foxes and wild cats hunt water voles too (as they hunt field voles). */
    @SubscribeEvent
    static void onJoin(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && (event.getEntity() instanceof net.minecraft.world.entity.animal.fox.Fox
                || event.getEntity() instanceof com.mechanical.wildlife.meadow.WildCat) && event.getEntity() instanceof net.minecraft.world.entity.Mob mob) {
            mob.targetSelector.addGoal(5, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(mob, WaterVole.class, 20, true, false, null));
        }
    }

    private TemperateFauna() {}
}
