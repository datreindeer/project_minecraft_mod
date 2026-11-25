package com.mechanical;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ITEMS;

import java.util.Map;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Flora 4 (docs/farming.md, "Majs"): maize tamed from teosinte, the Wild Carrot pattern. The Wild Maize plant (teosinte) grows
 * on warm grassland and gives Teosinte, sown as the wild crop; ripe it gives Teosinte and now and then Maize Kernels
 * (Herbs.tamedFrom). Maize Kernels grow into the two-block {@link MaizeCropBlock}; ripe it gives Corn Cobs: eaten, roasted
 * at a fire, or shelled into kernels again (seed, and Grain for the quern and porridge). Data: scripts/gen_flora.py,
 * textures: scripts/gen_vines_textures.py.
 */
public final class Maize {
    public static final DeferredBlock<WildPlantBlock> WILD_MAIZE_PLANT = BLOCKS.registerBlock("wild_maize_plant", WildPlantBlock::new, p -> p
            .mapColor(MapColor.PLANT).noCollision().instabreak().sound(SoundType.GRASS).offsetType(Block.OffsetType.XZ).pushReaction(PushReaction.POPPED));
    public static final DeferredBlock<FarmCropBlock> WILD_MAIZE_CROP = BLOCKS.registerBlock("wild_maize_crop",
            p -> new FarmCropBlock(() -> Maize.TEOSINTE.get(), p), Farming::cropProperties);
    public static final DeferredItem<BlockItem> TEOSINTE = ITEMS.registerItem("teosinte", p -> new BlockItem(WILD_MAIZE_CROP.get(), p));
    public static final DeferredBlock<MaizeCropBlock> MAIZE_CROP = BLOCKS.registerBlock("maize_crop",
            p -> new MaizeCropBlock(() -> Maize.KERNELS.get(), p), Farming::cropProperties);
    public static final DeferredItem<BlockItem> KERNELS = ITEMS.registerItem("maize_kernels", p -> new BlockItem(MAIZE_CROP.get(), p));
    public static final DeferredItem<Item> CORN_COB = ITEMS.registerSimpleItem("corn_cob",
            p -> p.food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.3F).build()));
    public static final DeferredItem<Item> ROASTED_CORN = ITEMS.registerSimpleItem("roasted_corn",
            p -> p.food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.6F).build()));

    /** A warm-season crop: frost-tender, wants summer heat (Temperature.ambient scale, as Farming.climate). */
    static final Farming.Climate CLIMATE = new Farming.Climate(11, 15, 21, 26, 4, false, new float[] {0.7F, 1.3F, 0.8F, 0});

    static void climates(Map<Block, Farming.Climate> m) {
        m.put(WILD_MAIZE_CROP.get(), CLIMATE);
        m.put(MAIZE_CROP.get(), CLIMATE);
    }

    public static void init() {}

    private Maize() {}
}
