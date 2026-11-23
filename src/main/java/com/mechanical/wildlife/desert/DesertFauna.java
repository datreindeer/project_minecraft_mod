package com.mechanical.wildlife.desert;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ENTITY_TYPES;
import static com.mechanical.Mechanical.ITEMS;

import java.util.List;

import com.mechanical.BearRugBlock;
import com.mechanical.Butchery;
import com.mechanical.Butchery.Extra;
import com.mechanical.Butchery.Yield;
import com.mechanical.Mechanical;
import com.mechanical.wildlife.FlockBird;
import com.mechanical.wildlife.Wildlife;
import com.mechanical.wildlife.meadow.MeadowFauna;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
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
 * Desert and badlands fauna (docs/fauna-expansion-plan.md, "Desert / Gypsum Flats" and "Badlands / Ochre Badlands"):
 * Desert Scorpion, Fennec Fox (tamed: a Tame Fennec that warns of enemies at night), Wild Camel (tamed: a Tame Camel, a
 * pack animal that goes far without water), Sand Viper, Puma, Bighorn Sheep, Rattlesnake and Condor. Entity types, the
 * items they give, spawn eggs, attributes, spawn rules and butchering yields. Spawns are in data (biome modifiers
 * add_*_desert), data and lang: scripts/gen_fauna_desert.py, textures: scripts/gen_fauna_desert_textures.py, docs:
 * docs/fauna/desert.md.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class DesertFauna {
    // ---- animals ----
    public static final DeferredHolder<EntityType<?>, EntityType<DesertScorpion>> DESERT_SCORPION = ENTITY_TYPES.registerEntityType(
            "desert_scorpion", DesertScorpion::new, MobCategory.CREATURE, b -> b.sized(0.7F, 0.35F).clientTrackingRange(8));
    public static final DeferredHolder<EntityType<?>, EntityType<FennecFox>> FENNEC_FOX = ENTITY_TYPES.registerEntityType(
            "fennec_fox", FennecFox::new, MobCategory.CREATURE, b -> b.sized(0.45F, 0.5F).clientTrackingRange(8));
    /** A tamed fennec: follows, sits on command and yips when something hostile comes near at night. */
    public static final DeferredHolder<EntityType<?>, EntityType<TameFennec>> TAME_FENNEC = ENTITY_TYPES.registerEntityType(
            "tame_fennec", TameFennec::new, MobCategory.CREATURE, b -> b.sized(0.45F, 0.5F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<WildCamel>> WILD_CAMEL = ENTITY_TYPES.registerEntityType(
            "wild_camel", WildCamel::new, MobCategory.CREATURE, b -> b.sized(1.5F, 2.4F).clientTrackingRange(10));
    /** A tamed camel: the tame onager's pack animal (a vanilla donkey underneath), slower, stronger and hardy without water. */
    public static final DeferredHolder<EntityType<?>, EntityType<TameCamel>> TAME_CAMEL = ENTITY_TYPES.registerEntityType(
            "tame_camel", TameCamel::new, MobCategory.CREATURE, b -> b.sized(1.5F, 2.4F).eyeHeight(2.3F).passengerAttachments(2.3F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<SandViper>> SAND_VIPER = ENTITY_TYPES.registerEntityType(
            "sand_viper", SandViper::new, MobCategory.CREATURE, b -> b.sized(0.5F, 0.25F).clientTrackingRange(8));
    public static final DeferredHolder<EntityType<?>, EntityType<Puma>> PUMA = ENTITY_TYPES.registerEntityType(
            "puma", Puma::new, MobCategory.CREATURE, b -> b.sized(0.9F, 0.95F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<BighornSheep>> BIGHORN_SHEEP = ENTITY_TYPES.registerEntityType(
            "bighorn_sheep", BighornSheep::new, MobCategory.CREATURE, b -> b.sized(0.9F, 1.3F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<Rattlesnake>> RATTLESNAKE = ENTITY_TYPES.registerEntityType(
            "rattlesnake", Rattlesnake::new, MobCategory.CREATURE, b -> b.sized(0.5F, 0.25F).clientTrackingRange(8));
    public static final DeferredHolder<EntityType<?>, EntityType<Condor>> CONDOR = ENTITY_TYPES.registerEntityType(
            "condor", Condor::new, MobCategory.CREATURE, b -> b.sized(1.0F, 1.2F).clientTrackingRange(16));

    // ---- meat (scrap-sized like the other game) ----
    public static final DeferredItem<Item> RAW_SCORPION = food("raw_scorpion", 1, 0.1F);
    public static final DeferredItem<Item> COOKED_SCORPION = food("cooked_scorpion", 3, 0.5F);
    public static final DeferredItem<Item> RAW_FENNEC = food("raw_fennec", 1, 0.1F);
    public static final DeferredItem<Item> COOKED_FENNEC = food("cooked_fennec", 3, 0.5F);
    public static final DeferredItem<Item> CAMEL_MEAT = food("camel_meat", 1, 0.1F);
    public static final DeferredItem<Item> COOKED_CAMEL_MEAT = food("cooked_camel_meat", 3, 0.8F);
    public static final DeferredItem<Item> PUMA_MEAT = food("puma_meat", 1, 0.1F);
    public static final DeferredItem<Item> COOKED_PUMA_MEAT = food("cooked_puma_meat", 3, 0.6F);
    public static final DeferredItem<Item> BIGHORN_MUTTON = food("bighorn_mutton", 1, 0.1F);
    public static final DeferredItem<Item> COOKED_BIGHORN_MUTTON = food("cooked_bighorn_mutton", 3, 0.8F);

    // ---- hides and parts ----
    /** The scorpion's venom sac: 8 arrows round it make Arrows of Poison. */
    public static final DeferredItem<Item> SCORPION_VENOM = ITEMS.registerSimpleItem("scorpion_venom");
    /** Small pelts are fleshed with a knife in the grid. */
    public static final DeferredItem<Item> RAW_FENNEC_PELT = ITEMS.registerSimpleItem("raw_fennec_pelt");
    public static final DeferredItem<Item> FENNEC_PELT = ITEMS.registerSimpleItem("fennec_pelt");
    /** Scraped (knife, crafting grid) into Scraped Hides for the Drying Rack: camel 3, bighorn 2. */
    public static final DeferredItem<Item> RAW_CAMEL_HIDE = ITEMS.registerSimpleItem("raw_camel_hide");
    public static final DeferredItem<Item> RAW_BIGHORN_HIDE = ITEMS.registerSimpleItem("raw_bighorn_hide");
    /** The fat of the hump: a tanning fat (#mechanical:tanning_fats) and lamp oil like tallow. */
    public static final DeferredItem<Item> CAMEL_HUMP_FAT = ITEMS.registerSimpleItem("camel_hump_fat");
    /** The sand viper's long hollow fang: barbs arrows at the Fletching Table. */
    public static final DeferredItem<Item> VIPER_FANG = ITEMS.registerSimpleItem("viper_fang");
    /** Fat-tanned in the crafting grid (knife + #mechanical:tanning_fats) into a Puma Pelt. */
    public static final DeferredItem<Item> RAW_PUMA_PELT = ITEMS.registerSimpleItem("raw_puma_pelt");
    public static final DeferredItem<Item> PUMA_PELT = ITEMS.registerSimpleItem("puma_pelt");
    /** A ram's great curled horn: one does for the two ibex horns of a Horn Bow. */
    public static final DeferredItem<Item> BIGHORN_HORN = ITEMS.registerSimpleItem("bighorn_horn");
    /** The rattle off a rattlesnake's tail. */
    public static final DeferredItem<Item> RATTLESNAKE_RATTLE = ITEMS.registerSimpleItem("rattlesnake_rattle");
    /** A condor's great flight feather: fletches 6 Eagle-Fletched Arrows. */
    public static final DeferredItem<Item> CONDOR_FEATHER = ITEMS.registerSimpleItem("condor_feather");

    // ---- made from them ----
    public static final DeferredBlock<BearRugBlock> PUMA_RUG = BLOCKS.registerBlock("puma_rug", BearRugBlock::new, p -> p
            .mapColor(MapColor.COLOR_ORANGE).strength(0.2F).sound(SoundType.WOOL).noOcclusion());
    public static final DeferredItem<BlockItem> PUMA_RUG_ITEM = ITEMS.registerSimpleBlockItem("puma_rug", PUMA_RUG);

    public static final DeferredItem<SpawnEggItem> DESERT_SCORPION_SPAWN_EGG = egg("desert_scorpion_spawn_egg", DESERT_SCORPION);
    public static final DeferredItem<SpawnEggItem> FENNEC_FOX_SPAWN_EGG = egg("fennec_fox_spawn_egg", FENNEC_FOX);
    public static final DeferredItem<SpawnEggItem> WILD_CAMEL_SPAWN_EGG = egg("wild_camel_spawn_egg", WILD_CAMEL);
    public static final DeferredItem<SpawnEggItem> SAND_VIPER_SPAWN_EGG = egg("sand_viper_spawn_egg", SAND_VIPER);
    public static final DeferredItem<SpawnEggItem> PUMA_SPAWN_EGG = egg("puma_spawn_egg", PUMA);
    public static final DeferredItem<SpawnEggItem> BIGHORN_SHEEP_SPAWN_EGG = egg("bighorn_sheep_spawn_egg", BIGHORN_SHEEP);
    public static final DeferredItem<SpawnEggItem> RATTLESNAKE_SPAWN_EGG = egg("rattlesnake_spawn_egg", RATTLESNAKE);
    public static final DeferredItem<SpawnEggItem> CONDOR_SPAWN_EGG = egg("condor_spawn_egg", CONDOR);

    private static DeferredItem<Item> food(String name, int nutrition, float saturation) {
        return ITEMS.registerSimpleItem(name, p -> p.food(new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).build()));
    }

    private static DeferredItem<SpawnEggItem> egg(String name, DeferredHolder<EntityType<?>, ? extends EntityType<?>> type) {
        return ITEMS.registerItem(name, SpawnEggItem::new, p -> p.spawnEgg(type.get()));
    }

    private static Extra x(java.util.function.Supplier<Item> item, float chance, int min, int max) {
        return new Extra(item, chance, min, max);
    }

    /** Registers everything (class load) and the butchering yields; call from the mod constructor. */
    public static void init() {
        // the scorpion's venom sac comes out with the little meat in its tail
        Butchery.register(DESERT_SCORPION::get, new Yield(RAW_SCORPION, 1, 1, List.of(), List.of(x(SCORPION_VENOM, 0.8F, 1, 1))));
        Butchery.register(FENNEC_FOX::get, new Yield(RAW_FENNEC, 1, 1, List.of(x(RAW_FENNEC_PELT, 0.9F, 1, 1)), List.of(x(Wildlife.SINEW, 0.3F, 1, 1))));
        Butchery.register(WILD_CAMEL::get, new Yield(CAMEL_MEAT, 3, 5, List.of(x(RAW_CAMEL_HIDE, 0.8F, 1, 1)),
                List.of(x(CAMEL_HUMP_FAT, 0.7F, 1, 2), x(Wildlife.SINEW, 0.6F, 1, 2))));
        // snakes: the shared snakeskin, snake meat and venom gland (MeadowFauna), and each one's own part
        Butchery.register(SAND_VIPER::get, new Yield(MeadowFauna.RAW_SNAKE, 1, 1, List.of(x(MeadowFauna.SNAKESKIN, 0.85F, 1, 1)),
                List.of(x(MeadowFauna.VENOM_GLAND, 0.75F, 1, 1), x(VIPER_FANG, 0.6F, 1, 2))));
        Butchery.register(RATTLESNAKE::get, new Yield(MeadowFauna.RAW_SNAKE, 1, 2, List.of(x(MeadowFauna.SNAKESKIN, 0.85F, 1, 1), x(RATTLESNAKE_RATTLE, 1.0F, 1, 1)),
                List.of(x(MeadowFauna.VENOM_GLAND, 0.75F, 1, 1))));
        Butchery.register(PUMA::get, new Yield(PUMA_MEAT, 1, 3, List.of(x(RAW_PUMA_PELT, 1.0F, 1, 1)),
                List.of(x(Wildlife.SINEW, 0.5F, 1, 1), x(() -> Items.BONE, 0.6F, 1, 2))));
        Butchery.register(BIGHORN_SHEEP::get, new Yield(BIGHORN_MUTTON, 2, 3, List.of(x(RAW_BIGHORN_HIDE, 0.7F, 1, 1)),
                List.of(x(BIGHORN_HORN, 0.6F, 1, 2), x(Wildlife.SINEW, 0.4F, 1, 1))));
    }

    @SubscribeEvent
    static void attributes(EntityAttributeCreationEvent event) {
        event.put(DESERT_SCORPION.get(), DesertScorpion.createAttributes().build());
        event.put(FENNEC_FOX.get(), FennecFox.createAttributes().build());
        event.put(TAME_FENNEC.get(), TameFennec.createAttributes().build());
        event.put(WILD_CAMEL.get(), WildCamel.createAttributes().build());
        event.put(TAME_CAMEL.get(), TameCamel.createAttributes().build());
        event.put(SAND_VIPER.get(), SandViper.createAttributes().build());
        event.put(PUMA.get(), Puma.createAttributes().build());
        event.put(BIGHORN_SHEEP.get(), BighornSheep.createAttributes().build());
        event.put(RATTLESNAKE.get(), Rattlesnake.createAttributes().build());
        event.put(CONDOR.get(), FlockBird.createAttributes(14.0).build());
    }

    /** Dry country: sand, red sand, terracotta, coarse dirt, sandstone or grass. Reads only this column. */
    static boolean dryGround(LevelAccessor level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.is(BlockTags.SAND) || below.is(BlockTags.TERRACOTTA) || below.is(Blocks.COARSE_DIRT) || below.is(Blocks.SANDSTONE)
                || below.is(Blocks.RED_SANDSTONE) || below.is(BlockTags.ANIMALS_SPAWNABLE_ON) || below.is(BlockTags.BASE_STONE_OVERWORLD);
    }

    @SubscribeEvent
    static void spawnRules(RegisterSpawnPlacementsEvent event) {
        // day animals in daylight; the night creatures (scorpion, fennec, puma, rattlesnake) at any light
        for (EntityType<? extends Animal> type : List.<EntityType<? extends Animal>>of(WILD_CAMEL.get(), SAND_VIPER.get(), BIGHORN_SHEEP.get(), CONDOR.get())) {
            event.register(type, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (t, level, reason, pos, random) -> dryGround(level, pos) && level.getRawBrightness(pos, 0) > 8, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        }
        for (EntityType<? extends Animal> type : List.<EntityType<? extends Animal>>of(DESERT_SCORPION.get(), FENNEC_FOX.get(), PUMA.get(), RATTLESNAKE.get())) {
            event.register(type, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (t, level, reason, pos, random) -> dryGround(level, pos), RegisterSpawnPlacementsEvent.Operation.REPLACE);
        }
    }

    private DesertFauna() {}
}
