package com.mechanical;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ITEMS;

import java.util.function.Supplier;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Bronze Age ceramics. Clay Balls are thrown on the {@link PottersWheelBlock} into greenware (Unfired Storage Jar /
 * Unfired Water Jug), fired in the Clay Kiln at 600° ({@link KilnBlockEntity#RECIPES}). The {@link StorageJarBlock}
 * keeps food: sealed, it ages at {@link #JAR_RATE} of the normal rate. The {@link WaterJugBlock} holds
 * {@link WaterJugBlock#MAX} pours of water. Data: scripts/gen_pottery.py, textures: scripts/gen_pottery_textures.py.
 */
public final class Pottery {
    /** Food in a sealed Storage Jar (cool, dark, airtight) ages at this share of the normal rate (Bark Basket: 1/3). */
    public static final float JAR_RATE = 1 / 4F;

    public static final DeferredBlock<PottersWheelBlock> POTTERS_WHEEL = BLOCKS.registerBlock("potters_wheel", PottersWheelBlock::new, p -> p
            .mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD).noOcclusion());
    public static final DeferredItem<BlockItem> POTTERS_WHEEL_ITEM = ITEMS.registerSimpleBlockItem("potters_wheel", POTTERS_WHEEL);
    public static final Supplier<BlockEntityType<PottersWheelBlockEntity>> POTTERS_WHEEL_ENTITY = Mechanical.BLOCK_ENTITIES.register("potters_wheel",
            () -> new BlockEntityType<>(PottersWheelBlockEntity::new, POTTERS_WHEEL.get()));

    public static final DeferredItem<Item> UNFIRED_STORAGE_JAR = ITEMS.registerSimpleItem("unfired_storage_jar");
    public static final DeferredItem<Item> UNFIRED_WATER_JUG = ITEMS.registerSimpleItem("unfired_water_jug");

    public static final DeferredBlock<StorageJarBlock> STORAGE_JAR = BLOCKS.registerBlock("storage_jar", StorageJarBlock::new, p -> p
            .mapColor(MapColor.TERRACOTTA_ORANGE).strength(0.8F).sound(SoundType.DECORATED_POT).noOcclusion());
    public static final DeferredItem<BlockItem> STORAGE_JAR_ITEM = ITEMS.registerItem("storage_jar",
            p -> new BlockItem(STORAGE_JAR.get(), p), p -> p.useBlockDescriptionPrefix().stacksTo(16));
    public static final Supplier<BlockEntityType<StorageJarBlockEntity>> STORAGE_JAR_ENTITY = Mechanical.BLOCK_ENTITIES.register("storage_jar",
            () -> new BlockEntityType<>(StorageJarBlockEntity::new, STORAGE_JAR.get()));

    public static final DeferredBlock<WaterJugBlock> WATER_JUG = BLOCKS.registerBlock("water_jug", WaterJugBlock::new, p -> p
            .mapColor(MapColor.TERRACOTTA_ORANGE).strength(0.8F).sound(SoundType.DECORATED_POT).noOcclusion().randomTicks());
    /** One per stack: each carries its own water level (block_state component). */
    public static final DeferredItem<WaterJugItem> WATER_JUG_ITEM = ITEMS.registerItem("water_jug",
            p -> new WaterJugItem(WATER_JUG.get(), p), p -> p.useBlockDescriptionPrefix().stacksTo(1));

    public static void init() {}

    private Pottery() {}
}
