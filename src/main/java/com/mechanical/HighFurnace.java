package com.mechanical;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ITEMS;
import static com.mechanical.Mechanical.MODID;

import java.util.function.Supplier;

import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * The High Furnace, the Steel Age's great smelting furnace (docs/steel-furnace-plan.md; docs/ages/04-steel-age.md). A
 * tower of {@link #BANDED_FURNACE_CASING} round an empty inside 2..3 wide and deep and 5..8 layers tall on a floor of
 * {@link #SILICA_BRICKS} ({@link HighFurnaceShape}), its bottom ring holding the {@link #HIGH_FURNACE_FRONT} (the
 * controller, {@link HighFurnaceBlockEntity}), {@link #FIREBOX Fireboxes} and {@link #TAPHOLE Tapholes}. Charcoal in the
 * Fireboxes and air from {@link #GREAT_BELLOWS Great Bellows} on water power heat it to 1600° (phases 0-2 built; the
 * melting, pouring and casting come in the later phases). Data: scripts/gen_high_furnace.py, textures:
 * scripts/gen_high_furnace_textures.py, test: {@link HighFurnaceTest}.
 */
public final class HighFurnace {
    /** How hot the bottom ring glows: 0 cold, 1 red, 2 orange, 3 yellow-white (set by the Front when the step changes). */
    public static final IntegerProperty HEAT = IntegerProperty.create("heat", 0, 3);

    public static final DeferredItem<Item> CRUSHED_QUARTZITE = ITEMS.registerSimpleItem("crushed_quartzite");
    public static final DeferredItem<Item> UNFIRED_SILICA_BRICK = ITEMS.registerSimpleItem("unfired_silica_brick");
    public static final DeferredItem<Item> SILICA_BRICK = ITEMS.registerSimpleItem("silica_brick");
    public static final DeferredItem<Item> STEEL_HOOP = ITEMS.registerSimpleItem("steel_hoop");
    public static final DeferredItem<Item> BRONZE_TUYERE_NOZZLE = ITEMS.registerSimpleItem("bronze_tuyere_nozzle");
    public static final DeferredItem<Item> UNFIRED_CLAY_TUYERE_MOLD = ITEMS.registerSimpleItem("unfired_clay_tuyere_mold");
    public static final DeferredBlock<ClayMoldBlock> CLAY_TUYERE_MOLD = Casting.mold("clay_tuyere_mold", ClayMoldBlock.Shape.TUYERE);
    public static final DeferredItem<BlockItem> CLAY_TUYERE_MOLD_ITEM = ITEMS.registerSimpleBlockItem("clay_tuyere_mold", CLAY_TUYERE_MOLD);
    /** Silica brick fires in the Clay Kiln at this heat (Charcoal + bellows; an Iron Bellows makes it easy). */
    public static final int SILICA_TEMP = 1250, SILICA_TICKS = 600;

    public static final DeferredBlock<Block> SILICA_BRICKS = BLOCKS.registerSimpleBlock("silica_bricks", p -> p
            .mapColor(MapColor.QUARTZ).strength(3.0F, 9.0F).sound(SoundType.STONE).requiresCorrectToolForDrops());
    public static final DeferredItem<BlockItem> SILICA_BRICKS_ITEM = ITEMS.registerSimpleBlockItem("silica_bricks", SILICA_BRICKS);
    public static final DeferredBlock<HighFurnaceWallBlock.Casing> BANDED_FURNACE_CASING = BLOCKS.registerBlock("banded_furnace_casing", HighFurnaceWallBlock.Casing::new,
            p -> wall(p).lightLevel(s -> s.getValue(HEAT) * 4));
    public static final DeferredItem<BlockItem> BANDED_FURNACE_CASING_ITEM = ITEMS.registerSimpleBlockItem("banded_furnace_casing", BANDED_FURNACE_CASING);
    public static final DeferredBlock<HighFurnaceWallBlock.Facing> FIREBOX = BLOCKS.registerBlock("firebox", HighFurnaceWallBlock.Facing::new,
            p -> wall(p).lightLevel(s -> s.getValue(HEAT) == 0 ? 0 : 7 + s.getValue(HEAT) * 2));
    public static final DeferredItem<BlockItem> FIREBOX_ITEM = ITEMS.registerSimpleBlockItem("firebox", FIREBOX);
    public static final DeferredBlock<HighFurnaceWallBlock.Facing> TAPHOLE = BLOCKS.registerBlock("taphole", HighFurnaceWallBlock.Facing::new,
            p -> wall(p).lightLevel(s -> s.getValue(HEAT) * 2));
    public static final DeferredItem<BlockItem> TAPHOLE_ITEM = ITEMS.registerSimpleBlockItem("taphole", TAPHOLE);
    public static final DeferredBlock<HighFurnaceFrontBlock> HIGH_FURNACE_FRONT = BLOCKS.registerBlock("high_furnace_front", HighFurnaceFrontBlock::new,
            p -> wall(p).lightLevel(s -> s.getValue(HEAT) == 0 ? 0 : 9 + s.getValue(HEAT) + (s.getValue(HEAT) == 3 ? 1 : 0)));
    public static final DeferredItem<BlockItem> HIGH_FURNACE_FRONT_ITEM = ITEMS.registerSimpleBlockItem("high_furnace_front", HIGH_FURNACE_FRONT);
    public static final Supplier<BlockEntityType<HighFurnaceBlockEntity>> HIGH_FURNACE_ENTITY = Mechanical.BLOCK_ENTITIES.register("high_furnace",
            () -> new BlockEntityType<>(HighFurnaceBlockEntity::new, HIGH_FURNACE_FRONT.get()));
    public static final Supplier<MenuType<HighFurnaceMenu>> HIGH_FURNACE_MENU = Mechanical.MENUS.register("high_furnace",
            () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(HighFurnaceMenu::client));

    public static final DeferredBlock<GreatBellowsBlock> GREAT_BELLOWS = BLOCKS.registerBlock("great_bellows", GreatBellowsBlock::new,
            p -> p.mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD).noOcclusion().forceSolidOn().ignitedByLava());
    public static final DeferredItem<BlockItem> GREAT_BELLOWS_ITEM = ITEMS.registerSimpleBlockItem("great_bellows", GREAT_BELLOWS);
    public static final Supplier<BlockEntityType<GreatBellowsBlockEntity>> GREAT_BELLOWS_ENTITY = Mechanical.BLOCK_ENTITIES.register("great_bellows",
            () -> new BlockEntityType<>(GreatBellowsBlockEntity::new, GREAT_BELLOWS.get()));

    /** Phase 5: the iron the High Furnace reduces from ore, cast in an Iron Ingot Mold; Finery-free for now: it melts back,
     * mixes 1:2 with Wrought Iron into Steel in the pool. */
    public static final DeferredItem<Item> PIG_IRON_INGOT = ITEMS.registerSimpleItem("pig_iron_ingot");
    public static final DeferredBlock<IngotMoldBlock> IRON_INGOT_MOLD = ironMold("iron_ingot_mold", IngotMoldBlock.Kind.INGOT),
            IRON_PLATE_MOLD = ironMold("iron_plate_mold", IngotMoldBlock.Kind.PLATE), IRON_NUGGET_MOLD = ironMold("iron_nugget_mold", IngotMoldBlock.Kind.NUGGET);
    public static final DeferredItem<BlockItem> IRON_INGOT_MOLD_ITEM = ITEMS.registerSimpleBlockItem("iron_ingot_mold", IRON_INGOT_MOLD),
            IRON_PLATE_MOLD_ITEM = ITEMS.registerSimpleBlockItem("iron_plate_mold", IRON_PLATE_MOLD),
            IRON_NUGGET_MOLD_ITEM = ITEMS.registerSimpleBlockItem("iron_nugget_mold", IRON_NUGGET_MOLD);
    public static final Supplier<BlockEntityType<IngotMoldBlock.Entity>> INGOT_MOLD_ENTITY = Mechanical.BLOCK_ENTITIES.register("iron_ingot_mold",
            () -> new BlockEntityType<>(IngotMoldBlock.Entity::new, IRON_INGOT_MOLD.get(), IRON_PLATE_MOLD.get(), IRON_NUGGET_MOLD.get(), HighFurnace.PIG_MOLD.get()));

    // Phase 4b: the casting tier (Casting Sand, Pig Bed, Molding Flask, Wooden Patterns, Pattern Bench, Foundry Table and Basin)
    public static final DeferredItem<Item> COAL_DUST = ITEMS.registerSimpleItem("coal_dust");
    public static final DeferredBlock<Block> CASTING_SAND = BLOCKS.registerBlock("casting_sand", CastingSandBlock::new,
            p -> p.mapColor(MapColor.COLOR_GRAY).strength(0.6F).sound(SoundType.SAND));
    public static final DeferredItem<BlockItem> CASTING_SAND_ITEM = ITEMS.registerSimpleBlockItem("casting_sand", CASTING_SAND);
    public static final DeferredBlock<Block> BURNT_CASTING_SAND = BLOCKS.registerSimpleBlock("burnt_casting_sand",
            p -> p.mapColor(MapColor.COLOR_BLACK).strength(0.6F).sound(SoundType.SAND));
    public static final DeferredItem<BlockItem> BURNT_CASTING_SAND_ITEM = ITEMS.registerSimpleBlockItem("burnt_casting_sand", BURNT_CASTING_SAND);
    public static final DeferredBlock<RefractoryTroughBlock.SandRunner> SAND_RUNNER = BLOCKS.registerBlock("sand_runner", RefractoryTroughBlock.SandRunner::new,
            p -> p.mapColor(MapColor.COLOR_GRAY).strength(0.6F).sound(SoundType.SAND).noOcclusion());
    public static final DeferredBlock<PigMoldBlock> PIG_MOLD = BLOCKS.registerBlock("pig_mold", PigMoldBlock::new,
            p -> p.mapColor(MapColor.COLOR_GRAY).strength(0.6F).sound(SoundType.SAND));
    public static final DeferredBlock<Block> SLAG_STONE = BLOCKS.registerSimpleBlock("slag_stone",
            p -> p.mapColor(MapColor.COLOR_BLACK).strength(2.0F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops());
    public static final DeferredItem<BlockItem> SLAG_STONE_ITEM = ITEMS.registerSimpleBlockItem("slag_stone", SLAG_STONE);
    public static final DeferredBlock<Block> CAST_BASALT = BLOCKS.registerSimpleBlock("cast_basalt",
            p -> p.mapColor(MapColor.COLOR_BLACK).strength(2.5F, 6.0F).sound(SoundType.BASALT).requiresCorrectToolForDrops());
    public static final DeferredItem<BlockItem> CAST_BASALT_ITEM = ITEMS.registerSimpleBlockItem("cast_basalt", CAST_BASALT);
    public static final DeferredBlock<FoundryTableBlock> FOUNDRY_TABLE = BLOCKS.registerBlock("foundry_table", FoundryTableBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());
    public static final DeferredItem<BlockItem> FOUNDRY_TABLE_ITEM = ITEMS.registerSimpleBlockItem("foundry_table", FOUNDRY_TABLE);
    public static final DeferredBlock<FoundryBasinBlock> FOUNDRY_BASIN = BLOCKS.registerBlock("foundry_basin", FoundryBasinBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());
    public static final DeferredItem<BlockItem> FOUNDRY_BASIN_ITEM = ITEMS.registerSimpleBlockItem("foundry_basin", FOUNDRY_BASIN);
    public static final Supplier<BlockEntityType<FoundryBasinBlock.Entity>> FOUNDRY_BASIN_ENTITY = Mechanical.BLOCK_ENTITIES.register("foundry_basin",
            () -> new BlockEntityType<>(FoundryBasinBlock.Entity::new, FOUNDRY_BASIN.get()));
    public static final DeferredBlock<MoldingFlaskBlock> MOLDING_FLASK = BLOCKS.registerBlock("molding_flask", MoldingFlaskBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(2.5F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());
    public static final DeferredItem<BlockItem> MOLDING_FLASK_ITEM = ITEMS.registerSimpleBlockItem("molding_flask", MOLDING_FLASK);
    public static final Supplier<BlockEntityType<MoldingFlaskBlock.Entity>> MOLDING_FLASK_ENTITY = Mechanical.BLOCK_ENTITIES.register("molding_flask",
            () -> new BlockEntityType<>(MoldingFlaskBlock.Entity::new, MOLDING_FLASK.get()));
    /** One Wooden Pattern per flask shape (and the Pig Pattern for the Pig Bed), cut at the Pattern Bench; they wear by one per use. */
    public static final java.util.Map<MoldingFlaskBlock.FlaskShape, DeferredItem<Item>> PATTERNS = patterns();
    public static final DeferredItem<Item> PIG_PATTERN = ITEMS.registerSimpleItem("pig_pattern", p -> p.durability(HighFurnace.PATTERN_USES));
    public static final DeferredBlock<PatternBenchBlock> PATTERN_BENCH = BLOCKS.registerBlock("pattern_bench", PatternBenchBlock::new,
            p -> p.mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD).noOcclusion().ignitedByLava());
    public static final DeferredItem<BlockItem> PATTERN_BENCH_ITEM = ITEMS.registerSimpleBlockItem("pattern_bench", PATTERN_BENCH);
    public static final Supplier<MenuType<PatternBenchBlock.Menu>> PATTERN_BENCH_MENU = Mechanical.MENUS.register("pattern_bench",
            () -> new MenuType<>(PatternBenchBlock.Menu::new, FeatureFlags.VANILLA_SET));
    public static final DeferredItem<Item> CAST_IRON_GEAR = ITEMS.registerSimpleItem("cast_iron_gear");
    public static final DeferredItem<Item> CAST_IRON_POT = ITEMS.registerSimpleItem("cast_iron_pot");
    public static final int PATTERN_USES = 128;
    // metal blocks the basin casts (vanilla has Copper and Gold; Tin's is in Mechanical); 9 ingots craft one too (gen_metals.py)
    public static final DeferredBlock<Block> BRONZE_BLOCK = metalBlock("bronze_block"), STEEL_BLOCK = metalBlock("steel_block"), LEAD_BLOCK = metalBlock("lead_block"),
            SILVER_BLOCK = metalBlock("silver_block"), PIG_IRON_BLOCK = metalBlock("pig_iron_block"), WROUGHT_IRON_BLOCK = metalBlock("wrought_iron_block");
    public static final DeferredItem<BlockItem> BRONZE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("bronze_block", BRONZE_BLOCK),
            STEEL_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("steel_block", STEEL_BLOCK), LEAD_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("lead_block", LEAD_BLOCK),
            SILVER_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("silver_block", SILVER_BLOCK),
            PIG_IRON_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("pig_iron_block", PIG_IRON_BLOCK),
            WROUGHT_IRON_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("wrought_iron_block", WROUGHT_IRON_BLOCK);

    // Phases 5-7: the Finery Hearth, Slag Stone Bricks, glass, the Sight Glass, Loam Molds, the Furnace Salamander, coke
    public static final DeferredBlock<FineryHearthBlock> FINERY_HEARTH = BLOCKS.registerBlock("finery_hearth", FineryHearthBlock::new,
            p -> p.mapColor(MapColor.COLOR_RED).strength(2.5F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion()
                    .lightLevel(s -> s.getValue(FineryHearthBlock.LIT) ? 13 : 0));
    public static final DeferredItem<BlockItem> FINERY_HEARTH_ITEM = ITEMS.registerSimpleBlockItem("finery_hearth", FINERY_HEARTH);
    public static final Supplier<BlockEntityType<FineryHearthBlock.Entity>> FINERY_HEARTH_ENTITY = Mechanical.BLOCK_ENTITIES.register("finery_hearth",
            () -> new BlockEntityType<>(FineryHearthBlock.Entity::new, HighFurnace.FINERY_HEARTH.get()));
    public static final DeferredBlock<Block> SLAG_STONE_BRICKS = BLOCKS.registerSimpleBlock("slag_stone_bricks",
            p -> p.mapColor(MapColor.COLOR_BLACK).strength(2.0F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops());
    public static final DeferredItem<BlockItem> SLAG_STONE_BRICKS_ITEM = ITEMS.registerSimpleBlockItem("slag_stone_bricks", SLAG_STONE_BRICKS);
    public static final DeferredItem<Item> GLASS_BATCH = ITEMS.registerSimpleItem("glass_batch");
    public static final DeferredBlock<HighFurnaceWallBlock> SIGHT_GLASS = BLOCKS.registerBlock("sight_glass", HighFurnaceWallBlock::new,
            p -> p.mapColor(MapColor.QUARTZ).strength(3.5F, 9.0F).sound(SoundType.GLASS).requiresCorrectToolForDrops().noOcclusion());
    public static final DeferredItem<BlockItem> SIGHT_GLASS_ITEM = ITEMS.registerSimpleBlockItem("sight_glass", SIGHT_GLASS);
    public static final DeferredItem<Item> LOAM = ITEMS.registerSimpleItem("loam"),
            STRICKLE_BOARD_BELL = ITEMS.registerSimpleItem("strickle_board_bell"), STRICKLE_BOARD_CAULDRON = ITEMS.registerSimpleItem("strickle_board_cauldron"),
            UNFIRED_LOAM_MOLD_BELL = ITEMS.registerSimpleItem("unfired_loam_mold_bell"), UNFIRED_LOAM_MOLD_CAULDRON = ITEMS.registerSimpleItem("unfired_loam_mold_cauldron"),
            LOAM_MOLD_BELL = ITEMS.registerSimpleItem("loam_mold_bell"), LOAM_MOLD_CAULDRON = ITEMS.registerSimpleItem("loam_mold_cauldron");
    public static final DeferredItem<Item> FURNACE_SALAMANDER = ITEMS.registerSimpleItem("furnace_salamander", p -> p.stacksTo(1));
    public static final DeferredItem<Item> COKE = ITEMS.registerSimpleItem("coke");
    public static final DeferredBlock<CokeOvenBlock> COKE_OVEN = BLOCKS.registerBlock("coke_oven", CokeOvenBlock::new,
            p -> p.mapColor(MapColor.QUARTZ).strength(3.0F, 9.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion()
                    .lightLevel(s -> s.getValue(CokeOvenBlock.LIT) ? 10 : 0));
    public static final DeferredItem<BlockItem> COKE_OVEN_ITEM = ITEMS.registerSimpleBlockItem("coke_oven", COKE_OVEN);
    public static final Supplier<BlockEntityType<CokeOvenBlock.Entity>> COKE_OVEN_ENTITY = Mechanical.BLOCK_ENTITIES.register("coke_oven",
            () -> new BlockEntityType<>(CokeOvenBlock.Entity::new, HighFurnace.COKE_OVEN.get()));
    public static final DeferredBlock<Block> HOT_BLAST_STOVE = BLOCKS.registerSimpleBlock("hot_blast_stove",
            p -> p.mapColor(MapColor.METAL).strength(3.5F, 9.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());
    public static final DeferredItem<BlockItem> HOT_BLAST_STOVE_ITEM = ITEMS.registerSimpleBlockItem("hot_blast_stove", HOT_BLAST_STOVE);
    public static final DeferredItem<SteelLadleItem> STEEL_LADLE = ITEMS.registerItem("steel_ladle", SteelLadleItem::new, p -> p.stacksTo(1));

    private static DeferredBlock<IngotMoldBlock> ironMold(String name, IngotMoldBlock.Kind kind) {
        return BLOCKS.registerBlock(name, p -> new IngotMoldBlock(p, kind),
                p -> p.mapColor(MapColor.METAL).strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());
    }

    private static DeferredBlock<Block> metalBlock(String name) {
        return BLOCKS.registerSimpleBlock(name, p -> p.mapColor(MapColor.METAL).strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    }

    private static java.util.Map<MoldingFlaskBlock.FlaskShape, DeferredItem<Item>> patterns() {
        java.util.Map<MoldingFlaskBlock.FlaskShape, DeferredItem<Item>> m = new java.util.EnumMap<>(MoldingFlaskBlock.FlaskShape.class);
        for (MoldingFlaskBlock.FlaskShape s : MoldingFlaskBlock.FlaskShape.values())
            m.put(s, ITEMS.registerSimpleItem("wooden_pattern_" + s.id, p -> p.durability(128)));
        return m;
    }

    /** Phase 4: Refractory Troughs and the Trough Gate (TroughFlow). */
    public static final DeferredBlock<RefractoryTroughBlock> REFRACTORY_TROUGH = BLOCKS.registerBlock("refractory_trough", RefractoryTroughBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(2.5F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());
    public static final DeferredItem<BlockItem> REFRACTORY_TROUGH_ITEM = ITEMS.registerSimpleBlockItem("refractory_trough", REFRACTORY_TROUGH);
    public static final DeferredBlock<RefractoryTroughBlock.Gate> TROUGH_GATE = BLOCKS.registerBlock("trough_gate", RefractoryTroughBlock.Gate::new,
            p -> p.mapColor(MapColor.METAL).strength(2.5F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());
    public static final DeferredItem<BlockItem> TROUGH_GATE_ITEM = ITEMS.registerSimpleBlockItem("trough_gate", TROUGH_GATE);
    public static final Supplier<BlockEntityType<TroughBlockEntity>> TROUGH_ENTITY = Mechanical.BLOCK_ENTITIES.register("refractory_trough",
            () -> new BlockEntityType<>(TroughBlockEntity::new, REFRACTORY_TROUGH.get(), TROUGH_GATE.get(), HighFurnace.SAND_RUNNER.get()));

    public static final DeferredBlock<PouringSpoutBlock> POURING_SPOUT = BLOCKS.registerBlock("pouring_spout", PouringSpoutBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()
                    .lightLevel(s -> s.getValue(PouringSpoutBlock.POURING) ? 15 : 0));
    public static final DeferredItem<BlockItem> POURING_SPOUT_ITEM = ITEMS.registerSimpleBlockItem("pouring_spout", POURING_SPOUT);
    public static final Supplier<BlockEntityType<PouringSpoutBlockEntity>> POURING_SPOUT_ENTITY = Mechanical.BLOCK_ENTITIES.register("pouring_spout",
            () -> new BlockEntityType<>(PouringSpoutBlockEntity::new, POURING_SPOUT.get()));

    /** Advancements granted from code (see scripts/gen_advancements.py, the Steel Age tab). */
    public static final Identifier FORMED_ADVANCEMENT = Identifier.fromNamespaceAndPath(MODID, "steel_age/high_furnace"),
            FULL_BLAST_ADVANCEMENT = Identifier.fromNamespaceAndPath(MODID, "steel_age/wind_of_the_river"),
            WHITE_HEAT_ADVANCEMENT = Identifier.fromNamespaceAndPath(MODID, "steel_age/white_heat"),
            LAYER_CAKE_ADVANCEMENT = Identifier.fromNamespaceAndPath(MODID, "steel_age/layer_cake"),
            POUR_ADVANCEMENT = Identifier.fromNamespaceAndPath(MODID, "steel_age/pour"),
            PIG_IRON_ADVANCEMENT = Identifier.fromNamespaceAndPath(MODID, "steel_age/pig_iron"),
            CAST_STEEL_ADVANCEMENT = Identifier.fromNamespaceAndPath(MODID, "steel_age/cast_steel"),
            WALLOON_ADVANCEMENT = Identifier.fromNamespaceAndPath(MODID, "steel_age/walloon_forge"),
            BELL_ADVANCEMENT = Identifier.fromNamespaceAndPath(MODID, "steel_age/bell_founder"),
            SALAMANDER_ADVANCEMENT = Identifier.fromNamespaceAndPath(MODID, "steel_age/salamander"),
            COKE_ADVANCEMENT = Identifier.fromNamespaceAndPath(MODID, "steel_age/coke"),
            HOT_BLAST_ADVANCEMENT = Identifier.fromNamespaceAndPath(MODID, "steel_age/hot_blast");

    private static Block.Properties wall(Block.Properties p) {
        return p.mapColor(MapColor.QUARTZ).strength(3.5F, 9.0F).sound(SoundType.STONE).requiresCorrectToolForDrops();
    }

    public static void init() {
        Molten.selfCheck();
        MeltPool.selfCheck();
        PouringSpoutBlockEntity.SINKS.add(new IngotMoldBlock.Sink());
        PouringSpoutBlockEntity.SINKS.add(new FoundryBasinBlock.Sink());
        PouringSpoutBlockEntity.SINKS.add(new MoldingFlaskBlock.Sink());
    }

    private HighFurnace() {}
}
