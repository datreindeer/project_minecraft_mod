package com.mechanical;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ITEMS;
import static com.mechanical.Mechanical.MODID;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Flora and food plan phases 4 and 4b (docs/flora-and-food-plan.md, section 5): wild vegetables grow in their climate
 * and can be tamed (Wild Carrot -> Carrot pattern): a wild crop harvested ripe on farmland gives its wild produce and
 * now and then the cultivated one ({@link #domesticate}). Crops only villagers sell (Broad Beans, Dill, Mustard) and the
 * Eastern Caravan's (Ginger, Saffron, Rice) grow by the same rules. Herbs ({@link HerbBlock}) are cut with a knife and
 * grow back. Growth rules in {@link Farming}; dishes and cookware in {@link Kitchen}. Data: scripts/gen_herbs.py,
 * textures: scripts/gen_herbs_textures.py.
 */
public final class Herbs {
    /** Spices and herbs: a stew with them in it fills you a little longer (Kettle.finish), and some dishes want one. */
    public static final TagKey<Item> CULINARY_HERBS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "culinary_herbs"));
    public static final TagKey<Item> SPICES = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "spices"));

    /** Chance a ripe wild crop also gives the tamed seed; better on good soil (fertility >= 2) in its best temperature. */
    public static final float TAME_CHANCE = 0.25F, TAME_CHANCE_GOOD = 0.35F;

    /** A vegetable: the tamed one's food, and the climate both forms share (Farming.Climate). */
    record Veg(String name, int nutrition, float saturation, Farming.Climate climate) {}

    static final List<Veg> VEGETABLES = List.of(
            new Veg("garlic", 1, 0.2F, new Farming.Climate(5, 8, 13, 17, 0, true, new float[] {1.1F, 1.0F, 0.6F, 0})),
            new Veg("onion", 2, 0.3F, new Farming.Climate(5, 8, 13, 17, 0, true, new float[] {1.1F, 1.0F, 0.7F, 0})),
            new Veg("cabbage", 2, 0.4F, new Farming.Climate(3, 6, 12, 16, -2, false, new float[] {1.0F, 0.9F, 1.1F, 0})),
            new Veg("leek", 2, 0.3F, new Farming.Climate(3, 6, 12, 16, -3, false, new float[] {0.9F, 1.0F, 1.1F, 0})),
            new Veg("parsnip", 2, 0.4F, new Farming.Climate(3, 6, 12, 16, -2, true, new float[] {1.0F, 1.0F, 0.9F, 0})),
            new Veg("celery", 1, 0.2F, new Farming.Climate(7, 10, 14, 18, 2, false, new float[] {0.8F, 1.2F, 0.8F, 0})),
            new Veg("turnip", 2, 0.3F, new Farming.Climate(4, 7, 12, 16, -1, true, new float[] {1.1F, 0.9F, 1.0F, 0})),
            new Veg("lettuce", 1, 0.3F, new Farming.Climate(6, 9, 13, 17, 2, false, new float[] {1.2F, 0.9F, 0.9F, 0})));

    public static final Map<String, DeferredBlock<WildPlantBlock>> WILD_PLANTS = new LinkedHashMap<>();
    public static final Map<String, DeferredBlock<FarmCropBlock>> WILD_CROPS = new LinkedHashMap<>(), CROPS = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<BlockItem>> WILD_ITEMS = new LinkedHashMap<>(), ITEMS_BY_NAME = new LinkedHashMap<>();

    private static FoodProperties food(int nutrition, float saturation) {
        return new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).build();
    }

    /** A crop sown from its own item (the seed or the produce itself, like vanilla carrots). */
    private static void crop(String crop, String item, FoodProperties food, Map<String, DeferredBlock<FarmCropBlock>> crops, Map<String, DeferredItem<BlockItem>> items, String key) {
        DeferredBlock<FarmCropBlock> block = BLOCKS.registerBlock(crop + "_crop", p -> new FarmCropBlock(() -> items.get(key).get(), p), Farming::cropProperties);
        crops.put(key, block);
        items.put(key, food == null ? ITEMS.registerItem(item, p -> new BlockItem(block.get(), p)) : ITEMS.registerItem(item, p -> new BlockItem(block.get(), p), p -> p.food(food)));
    }

    static {
        for (Veg v : VEGETABLES) {
            WILD_PLANTS.put(v.name(), BLOCKS.registerBlock("wild_" + v.name() + "_plant", WildPlantBlock::new, Herbs::wildProperties));
            crop("wild_" + v.name(), "wild_" + v.name(), food(1, 0.2F), WILD_CROPS, WILD_ITEMS, v.name());
            crop(v.name(), v.name(), food(v.nutrition(), v.saturation()), CROPS, ITEMS_BY_NAME, v.name());
        }
        // grown only from bought seed: villagers (broad beans, dill, mustard) or the Eastern Caravan (ginger, saffron, rice)
        crop("broad_bean", "broad_beans", food(1, 0.3F), CROPS, ITEMS_BY_NAME, "broad_bean");
        crop("dill", "dill_seeds", null, CROPS, ITEMS_BY_NAME, "dill");
        crop("mustard", "mustard_seeds", null, CROPS, ITEMS_BY_NAME, "mustard");
        crop("ginger", "ginger_root", null, CROPS, ITEMS_BY_NAME, "ginger");
        crop("saffron", "saffron_corm", null, CROPS, ITEMS_BY_NAME, "saffron");
        crop("rice", "rice", null, CROPS, ITEMS_BY_NAME, "rice");
    }

    public static final DeferredItem<Item> DILL = ITEMS.registerSimpleItem("dill");
    public static final DeferredItem<Item> SAFFRON = ITEMS.registerSimpleItem("saffron");

    /** Climates of the crops that aren't vegetables (Temperature.ambient scale): a row each, as in Farming.climate. */
    private static final Map<String, Farming.Climate> OTHER_CLIMATES = Map.of(
            "broad_bean", new Farming.Climate(4, 7, 12, 16, -1, false, new float[] {1.2F, 1.0F, 0.4F, 0}),
            "dill", new Farming.Climate(7, 10, 15, 19, 2, false),
            "mustard", new Farming.Climate(5, 8, 13, 17, 0, false, new float[] {1.2F, 1.0F, 0.6F, 0}),
            "ginger", new Farming.Climate(14, 18, 23, 27, 10, true, new float[] {0.6F, 1.3F, 0.8F, 0}), // savanna heat
            "saffron", new Farming.Climate(10, 13, 19, 24, 4, true, new float[] {0.5F, 0.8F, 1.3F, 0}), // flowers in autumn
            "rice", new Farming.Climate(13, 17, 23, 27, 8, false, new float[] {0.6F, 1.3F, 0.7F, 0}));

    // ---- herbs: cut with a knife, they grow back; the plant itself can be dug up and planted in a herb garden ----
    /** Herb plant id -> what cutting it gives. */
    static final Map<String, String> HERB_KINDS = new LinkedHashMap<>();
    static {
        HERB_KINDS.put("wild_thyme", "thyme");
        HERB_KINDS.put("wild_marjoram", "marjoram");
        HERB_KINDS.put("yarrow", "yarrow_leaves");
        HERB_KINDS.put("chamomile", "chamomile_flowers");
        HERB_KINDS.put("water_mint", "mint_leaves");
        HERB_KINDS.put("sorrel", "sorrel_leaves");
        HERB_KINDS.put("sage", "sage_leaves");
        HERB_KINDS.put("rosemary", "rosemary_sprigs");
        HERB_KINDS.put("bog_myrtle", "bog_myrtle_leaves");
        HERB_KINDS.put("meadowsweet", "meadowsweet_flowers");
        HERB_KINDS.put("hops", "hop_cones");
        HERB_KINDS.put("wild_mustard", "mustard_seeds");
    }
    public static final Map<String, Supplier<Item>> HERB_ITEMS = new LinkedHashMap<>();
    public static final Map<String, DeferredBlock<HerbBlock>> HERBS = new LinkedHashMap<>();
    static {
        for (var e : HERB_KINDS.entrySet()) {
            String item = e.getValue();
            if (!HERB_ITEMS.containsKey(item)) HERB_ITEMS.put(item, item.equals("mustard_seeds") ? () -> ITEMS_BY_NAME.get("mustard").get() : ITEMS.registerSimpleItem(item));
            Supplier<Item> gives = HERB_ITEMS.get(item);
            DeferredBlock<HerbBlock> block = BLOCKS.registerBlock(e.getKey(), p -> new HerbBlock(gives, p), p -> wildProperties(p).randomTicks());
            HERBS.put(e.getKey(), block);
            ITEMS.registerSimpleBlockItem(e.getKey(), block);
        }
    }

    // ---- the Eastern Caravan's goods that don't grow here ----
    public static final DeferredItem<Item> BLACK_PEPPER = ITEMS.registerSimpleItem("black_pepper");
    public static final DeferredItem<Item> CINNAMON_BARK = ITEMS.registerSimpleItem("cinnamon_bark");
    public static final DeferredItem<Item> SESAME_SEEDS = ITEMS.registerSimpleItem("sesame_seeds");
    public static final DeferredItem<Item> TEA_LEAVES = ITEMS.registerSimpleItem("tea_leaves");
    public static final DeferredItem<Item> INDIGO = ITEMS.registerSimpleItem("indigo");
    public static final DeferredItem<Item> DRIED_DATES = ITEMS.registerSimpleItem("dried_dates", p -> p.food(food(2, 0.4F)));
    public static final DeferredItem<Item> DRIED_FIGS = ITEMS.registerSimpleItem("dried_figs", p -> p.food(food(2, 0.4F)));

    private static Block.Properties wildProperties(Block.Properties p) {
        return p.mapColor(MapColor.PLANT).noCollision().instabreak().sound(SoundType.GRASS).offsetType(Block.OffsetType.XZ).pushReaction(PushReaction.POPPED);
    }

    public static void init() {}

    /** Adds this phase's crops to Farming's climate table. */
    static void climates(Map<Block, Farming.Climate> m) {
        for (Veg v : VEGETABLES) {
            m.put(WILD_CROPS.get(v.name()).get(), v.climate());
            m.put(CROPS.get(v.name()).get(), v.climate());
        }
        OTHER_CLIMATES.forEach((name, c) -> m.put(CROPS.get(name).get(), c));
    }

    /** The tamed vegetable a ripe wild crop may give, or null if this isn't a wild crop. */
    public static Item tamedFrom(Block wildCrop) {
        for (var e : WILD_CROPS.entrySet()) if (e.getValue().get() == wildCrop) return ITEMS_BY_NAME.get(e.getKey()).get();
        return null;
    }

    /** This wild crop's chance of a tamed seed where it stands: the soil (read before the harvest wears it) and the warmth. */
    public static float tameChance(ServerLevel level, BlockPos pos, BlockState crop) {
        Farming.Climate c = Farming.climate(crop.getBlock());
        BlockState soil = level.getBlockState(pos.below());
        boolean good = c != null && soil.hasProperty(Fertility.FERTILITY) && Fertility.get(soil) >= 2 && c.factor(Temperature.ambient(level, pos)) >= 1;
        return good ? TAME_CHANCE_GOOD : TAME_CHANCE;
    }

    /**
     * Rice grows only in a flooded paddy: water right beside its farmland (a channel between the rows) or beside the plant
     * itself (farmland sunk below a flooded terrace). Every other crop is fine anywhere.
     */
    public static boolean paddyOk(Level level, BlockPos crop) {
        if (!level.getBlockState(crop).is(CROPS.get("rice").get())) return true;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (level.getFluidState(crop.below().relative(d)).is(FluidTags.WATER) || level.getFluidState(crop.relative(d)).is(FluidTags.WATER)) return true;
        }
        return false;
    }

    /** A ripe wild crop harvested: now and then its tamed seed joins the drops (Farming.onDrops, before the soil wears). */
    static void domesticate(ServerLevel level, BlockPos pos, BlockState state, List<ItemEntity> drops) {
        Item tamed = tamedFrom(state.getBlock());
        if (tamed == null || level.getRandom().nextFloat() >= tameChance(level, pos, state)) return;
        drops.add(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, new ItemStack(tamed)));
    }

    private Herbs() {}
}
