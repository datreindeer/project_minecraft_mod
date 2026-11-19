package com.mechanical;

import java.util.function.Supplier;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;

/** Barley, einkorn, emmer and peas on farmland: a vanilla crop (growth rules in {@link Farming}) sown from its own grain. */
public class FarmCropBlock extends CropBlock {
    private final Supplier<Item> seed;

    public FarmCropBlock(Supplier<Item> seed, Properties properties) {
        super(properties);
        this.seed = seed;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return seed.get();
    }
}
