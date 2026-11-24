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
    public static final DeferredBlock<HighFurnaceWallBlock> BANDED_FURNACE_CASING = BLOCKS.registerBlock("banded_furnace_casing", HighFurnaceWallBlock::new,
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
            () -> new MenuType<>(HighFurnaceMenu::new, FeatureFlags.VANILLA_SET));

    public static final DeferredBlock<GreatBellowsBlock> GREAT_BELLOWS = BLOCKS.registerBlock("great_bellows", GreatBellowsBlock::new,
            p -> p.mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD).noOcclusion().forceSolidOn().ignitedByLava());
    public static final DeferredItem<BlockItem> GREAT_BELLOWS_ITEM = ITEMS.registerSimpleBlockItem("great_bellows", GREAT_BELLOWS);
    public static final Supplier<BlockEntityType<GreatBellowsBlockEntity>> GREAT_BELLOWS_ENTITY = Mechanical.BLOCK_ENTITIES.register("great_bellows",
            () -> new BlockEntityType<>(GreatBellowsBlockEntity::new, GREAT_BELLOWS.get()));

    /** Advancements granted from code (see scripts/gen_advancements.py, the Steel Age tab). */
    public static final Identifier FORMED_ADVANCEMENT = Identifier.fromNamespaceAndPath(MODID, "steel_age/high_furnace"),
            FULL_BLAST_ADVANCEMENT = Identifier.fromNamespaceAndPath(MODID, "steel_age/wind_of_the_river"),
            WHITE_HEAT_ADVANCEMENT = Identifier.fromNamespaceAndPath(MODID, "steel_age/white_heat");

    private static Block.Properties wall(Block.Properties p) {
        return p.mapColor(MapColor.QUARTZ).strength(3.5F, 9.0F).sound(SoundType.STONE).requiresCorrectToolForDrops();
    }

    public static void init() {
        MeltPool.selfCheck();
    }

    private HighFurnace() {}
}
