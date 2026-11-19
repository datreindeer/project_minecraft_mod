package com.mechanical;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ITEMS;
import static com.mechanical.Mechanical.MODID;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Root crops get wild sources (docs/farming.md, "Rotfrukter"). Wild Carrot grows on meadows and plains; its thin root is
 * sown on farmland as a Wild Carrot crop that, harvested ripe, now and then gives a real Carrot. Sea Beet on beaches and
 * stony shores gives Sea Beet Leaves and sometimes Beetroot Seeds. Potatoes come only from rare tribe trades
 * (Tribesperson). The Root Cellar keeps produce 4x longer when it is dug in (RootCellarBlockEntity).
 * Data: scripts/gen_roots.py, textures: scripts/gen_roots_textures.py.
 */
public final class Roots {
    /** Produce that keeps 4x longer in a dug-in Root Cellar (other perishables 2x). */
    public static final TagKey<Item> CELLAR_PRODUCE = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "cellar_produce"));
    public static final float CELLAR_PRODUCE_RATE = 1 / 4F, CELLAR_OTHER_RATE = 1 / 2F;
    /** Sky light beside the cellar at most this on every side: dug in, dark and cool. */
    public static final int CELLAR_MAX_SKY_LIGHT = 7;

    public static final DeferredBlock<FarmCropBlock> WILD_CARROT_CROP = BLOCKS.registerBlock("wild_carrot_crop",
            p -> new FarmCropBlock(() -> Roots.WILD_CARROT.get(), p), Farming::cropProperties);
    public static final DeferredItem<BlockItem> WILD_CARROT = ITEMS.registerItem("wild_carrot", p -> new BlockItem(WILD_CARROT_CROP.get(), p),
            p -> p.food(new FoodProperties.Builder().nutrition(1).saturationModifier(0.3F).build()));
    public static final DeferredItem<Item> SEA_BEET_LEAVES = ITEMS.registerSimpleItem("sea_beet_leaves",
            p -> p.food(new FoodProperties.Builder().nutrition(1).saturationModifier(0.2F).build()));

    public static final DeferredBlock<WildPlantBlock> WILD_CARROT_PLANT = BLOCKS.registerBlock("wild_carrot_plant", WildPlantBlock::new, Roots::wildProperties);
    /** Grows on sand, gravel and shore rock as well as soil. */
    public static final DeferredBlock<WildPlantBlock> SEA_BEET = BLOCKS.registerBlock("sea_beet", p -> new WildPlantBlock(p) {
        @Override
        protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
            return state.is(BlockTags.SUPPORTS_VEGETATION) || state.is(BlockTags.SAND) || state.is(Blocks.GRAVEL) || state.is(BlockTags.BASE_STONE_OVERWORLD);
        }
    }, Roots::wildProperties);

    public static final DeferredBlock<RootCellarBlock> ROOT_CELLAR = BLOCKS.registerBlock("root_cellar", RootCellarBlock::new, p -> p
            .mapColor(MapColor.WOOD).strength(1.5F).sound(SoundType.WOOD).noOcclusion().ignitedByLava());
    public static final DeferredItem<BlockItem> ROOT_CELLAR_ITEM = ITEMS.registerSimpleBlockItem("root_cellar", ROOT_CELLAR);
    public static final Supplier<BlockEntityType<RootCellarBlockEntity>> ROOT_CELLAR_ENTITY = Mechanical.BLOCK_ENTITIES.register("root_cellar",
            () -> new BlockEntityType<>(RootCellarBlockEntity::new, ROOT_CELLAR.get()));

    private static Block.Properties wildProperties(Block.Properties p) {
        return p.mapColor(MapColor.PLANT).noCollision().instabreak().sound(SoundType.GRASS).offsetType(Block.OffsetType.XZ).pushReaction(PushReaction.POPPED);
    }

    public static void init() {}

    /** A Root Cellar here is dug in: little daylight reaches any side of it (a box on open ground or in an open pit isn't). */
    public static boolean dugIn(Level level, BlockPos pos) {
        for (Direction d : Direction.values()) {
            if (level.getBrightness(LightLayer.SKY, pos.relative(d)) > CELLAR_MAX_SKY_LIGHT) return false;
        }
        return true;
    }

    private Roots() {}
}
