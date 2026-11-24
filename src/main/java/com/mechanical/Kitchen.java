package com.mechanical;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ITEMS;
import static com.mechanical.Mechanical.MODID;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Dishes and cookware per age (docs/flora-and-food-plan.md, section 6). Each age's cookware opens its dishes:
 * Stone Age the Clay Cooking Pot ({@link ClayPotBlock}, in the embers of a campfire) and the campfire; Bronze Age the
 * Bronze Kettle (Kettle), the Bronze Griddle and the Bronze Ladle; Iron Age the Iron Cauldron (hung in the tripod over a
 * Fire Pit), the Iron Skillet, the Fermenting Barrel and the Cheese Press ({@link CookwareBlock}), and sea salt from a
 * {@link SaltPanBlock}. A dish is a fixed set of ingredients in one kind of cookware ({@link #DISHES}); twice the set
 * makes twice the dish. A pot dish needs at least its tier of pot (clay 0, bronze 1, iron 2), so the kettle and the
 * cauldron also cook the clay pot's dishes. Data: scripts/gen_herbs.py, models scripts/herbs_cookware.py, textures
 * scripts/gen_cookware_textures.py.
 */
public final class Kitchen {
    /** Which cookware a dish is made in. */
    public enum Tool { POT, PAN, BARREL, PRESS }

    /** Cook times in ticks: pans on a lit campfire, the barrel and press with no fire at all. */
    public static final int PAN_TIME = 600, PAN_BURN = 1200, BARREL_TIME = 12000, PRESS_TIME = 9600;

    // ---- cookware ----
    public static final DeferredBlock<ClayPotBlock> CLAY_COOKING_POT = BLOCKS.registerBlock("clay_cooking_pot", ClayPotBlock::new, p -> p
            .mapColor(MapColor.TERRACOTTA_ORANGE).strength(0.8F).sound(SoundType.DECORATED_POT).noOcclusion().pushReaction(PushReaction.POPPED));
    public static final DeferredItem<BlockItem> CLAY_COOKING_POT_ITEM = ITEMS.registerSimpleBlockItem("clay_cooking_pot", CLAY_COOKING_POT);
    public static final DeferredItem<Item> UNFIRED_CLAY_COOKING_POT = ITEMS.registerSimpleItem("unfired_clay_cooking_pot");
    public static final DeferredItem<Item> IRON_CAULDRON = ITEMS.registerSimpleItem("iron_cauldron", p -> p.stacksTo(1));
    public static final DeferredItem<Item> BRONZE_LADLE = ITEMS.registerSimpleItem("bronze_ladle", p -> p.durability(250));

    public static final DeferredBlock<CookwareBlock> BRONZE_GRIDDLE = cookware("bronze_griddle", CookwareBlock.Kind.GRIDDLE, MapColor.COLOR_ORANGE, SoundType.COPPER);
    public static final DeferredBlock<CookwareBlock> IRON_SKILLET = cookware("iron_skillet", CookwareBlock.Kind.SKILLET, MapColor.COLOR_BLACK, SoundType.IRON);
    public static final DeferredBlock<CookwareBlock> FERMENTING_BARREL = cookware("fermenting_barrel", CookwareBlock.Kind.BARREL, MapColor.WOOD, SoundType.WOOD);
    public static final DeferredBlock<CookwareBlock> CHEESE_PRESS = cookware("cheese_press", CookwareBlock.Kind.PRESS, MapColor.WOOD, SoundType.WOOD);
    public static final Supplier<BlockEntityType<CookwareBlockEntity>> COOKWARE_ENTITY = Mechanical.BLOCK_ENTITIES.register("cookware",
            () -> new BlockEntityType<>(CookwareBlockEntity::new, BRONZE_GRIDDLE.get(), IRON_SKILLET.get(), FERMENTING_BARREL.get(), CHEESE_PRESS.get()));

    public static final DeferredBlock<SaltPanBlock> SALT_PAN = BLOCKS.registerBlock("salt_pan", SaltPanBlock::new, p -> p
            .mapColor(MapColor.TERRACOTTA_ORANGE).strength(1.0F).sound(SoundType.DECORATED_POT).noOcclusion().randomTicks());
    public static final DeferredItem<BlockItem> SALT_PAN_ITEM = ITEMS.registerSimpleBlockItem("salt_pan", SALT_PAN);

    private static DeferredBlock<CookwareBlock> cookware(String name, CookwareBlock.Kind kind, MapColor colour, SoundType sound) {
        DeferredBlock<CookwareBlock> block = BLOCKS.registerBlock(name, p -> CookwareBlock.create(kind, p),
                p -> p.mapColor(colour).strength(kind.fire ? 1.5F : 2.0F).sound(sound).noOcclusion());
        ITEMS.registerSimpleBlockItem(name, block);
        return block;
    }

    // ---- dishes ----
    private static DeferredItem<Item> bowl(String name, int nutrition) {
        return ITEMS.registerSimpleItem(name, p -> p.stacksTo(1).food(new FoodProperties.Builder().nutrition(nutrition).saturationModifier(0.6F).build())
                .usingConvertsTo(Items.BOWL));
    }

    private static DeferredItem<Item> dish(String name, int nutrition) {
        return ITEMS.registerSimpleItem(name, p -> p.food(new FoodProperties.Builder().nutrition(nutrition).saturationModifier(0.6F).build()));
    }

    // Stone Age (Clay Cooking Pot, campfire)
    public static final DeferredItem<Item> NETTLE_SOUP = bowl("nettle_soup", 4);
    public static final DeferredItem<Item> ROOT_POTTAGE = bowl("root_pottage", 5);
    public static final DeferredItem<Item> BERRY_MASH = bowl("berry_mash", 4);
    public static final DeferredItem<Item> HERBAL_TEA = bowl("herbal_tea", 1);
    public static final DeferredItem<Item> HERB_STUFFED_FISH = ITEMS.registerSimpleItem("herb_stuffed_fish");
    public static final DeferredItem<Item> HERB_ROASTED_FISH = dish("herb_roasted_fish", 7);
    // Bronze Age (Bronze Kettle, Bronze Griddle)
    public static final DeferredItem<Item> PEA_SOUP = bowl("pea_soup", 6);
    public static final DeferredItem<Item> ONION_GARLIC_STEW = bowl("onion_garlic_stew", 8);
    public static final DeferredItem<Item> SPICED_PORRIDGE = bowl("spiced_porridge", 7);
    public static final DeferredItem<Item> TEA = bowl("tea", 1);
    public static final DeferredItem<Item> ELDERBERRY_SYRUP = dish("elderberry_syrup", 3);
    public static final DeferredItem<Item> SLOE_JAM = dish("sloe_jam", 3);
    public static final DeferredItem<Item> FLATBREAD = dish("flatbread", 4);
    public static final DeferredItem<Item> RAW_HONEY_CAKE = ITEMS.registerSimpleItem("raw_honey_cake");
    public static final DeferredItem<Item> HONEY_CAKE = dish("honey_cake", 5);
    // Iron Age (Iron Cauldron, Iron Skillet, Fermenting Barrel, Cheese Press)
    public static final DeferredItem<Item> CABBAGE_SOUP = bowl("cabbage_soup", 9);
    public static final DeferredItem<Item> VEGETABLE_POTTAGE = bowl("vegetable_pottage", 8);
    public static final DeferredItem<Item> FRIED_EGGS_AND_ONION = dish("fried_eggs_and_onion", 8);
    public static final DeferredItem<Item> RAW_SAUSAGE = ITEMS.registerSimpleItem("raw_sausage");
    public static final DeferredItem<Item> SAUSAGE = dish("sausage", 6);
    public static final DeferredItem<Item> SAUERKRAUT = dish("sauerkraut", 3);
    public static final DeferredItem<Item> CHEESE = dish("cheese", 4);

    // ---- recipes ----

    /** One ingredient slot: any of these item ids or #tags. */
    public record Ing(List<String> ids) {
        public boolean test(ItemStack s) {
            String id = BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
            for (String i : ids) {
                if (i.startsWith("#") ? s.is(TagKey.create(Registries.ITEM, Identifier.parse(i.substring(1)))) : i.equals(id)) return true;
            }
            return false;
        }
    }

    private static Ing of(String... ids) {
        return new Ing(List.of(ids));
    }

    /** A dish: made in this cookware of at least this tier, from exactly these ingredients, giving count of result. */
    public record Dish(String name, Tool tool, int tier, List<Ing> ings, Supplier<Item> result, int count) {
        public boolean bowl() {
            return new ItemStack(result.get()).has(net.minecraft.core.component.DataComponents.USE_REMAINDER);
        }
    }

    private static final Ing ALLIUM = of("mechanical:wild_onion", "mechanical:onion", "mechanical:wild_garlic", "mechanical:garlic",
            "mechanical:wild_leek", "mechanical:leek");
    private static final Ing ROOT = of("mechanical:wild_carrot", "minecraft:carrot", "mechanical:wild_parsnip", "mechanical:parsnip",
            "mechanical:wild_turnip", "mechanical:turnip", "minecraft:beetroot");
    private static final Ing MEAT = of("#mechanical:smokable_meat");
    private static final Ing HONEY = of("mechanical:wild_honey", "minecraft:honey_bottle");
    private static final Ing EGG = of("#mechanical:eggs");
    private static final Ing MILK = of("minecraft:milk_bucket", "mechanical:bronze_milk_bucket");

    public static final List<Dish> DISHES = List.of(
            // Stone Age: the clay pot
            new Dish("nettle_soup", Tool.POT, 0, List.of(of("mechanical:nettle_stalks"), of("mechanical:nettle_stalks"), ALLIUM), NETTLE_SOUP, 3),
            new Dish("root_pottage", Tool.POT, 0, List.of(ROOT, ROOT, ALLIUM), ROOT_POTTAGE, 3),
            new Dish("berry_mash", Tool.POT, 0, List.of(of("#mechanical:dryable_berries"), of("#mechanical:dryable_berries"),
                    of("mechanical:hazelnuts", "mechanical:beechnuts")), BERRY_MASH, 2),
            new Dish("herbal_tea", Tool.POT, 0, List.of(of("mechanical:chamomile_flowers", "mechanical:mint_leaves", "mechanical:yarrow_leaves",
                    "mechanical:meadowsweet_flowers")), HERBAL_TEA, 2),
            // Bronze Age: the kettle and the griddle
            new Dish("pea_soup", Tool.POT, 1, List.of(of("mechanical:peas"), of("mechanical:peas"), of("mechanical:peas"), ALLIUM), PEA_SOUP, 4),
            new Dish("onion_garlic_stew", Tool.POT, 1, List.of(of("mechanical:onion"), of("mechanical:onion"), of("mechanical:garlic"), MEAT), ONION_GARLIC_STEW, 4),
            new Dish("spiced_porridge", Tool.POT, 1, List.of(of("#mechanical:grains"), of("#mechanical:grains"), of("mechanical:cinnamon_bark"), HONEY),
                    SPICED_PORRIDGE, 3),
            new Dish("tea", Tool.POT, 1, List.of(of("mechanical:tea_leaves")), TEA, 3),
            new Dish("elderberry_syrup", Tool.POT, 1, List.of(of("mechanical:elderberries"), of("mechanical:elderberries"), of("mechanical:elderberries"), HONEY),
                    ELDERBERRY_SYRUP, 2),
            new Dish("sloe_jam", Tool.POT, 1, List.of(of("mechanical:sloes"), of("mechanical:sloes"), of("mechanical:sloes"), HONEY), SLOE_JAM, 2),
            new Dish("flatbread", Tool.PAN, 1, List.of(of("mechanical:dough")), FLATBREAD, 2),
            new Dish("honey_cake", Tool.PAN, 1, List.of(of("mechanical:raw_honey_cake")), HONEY_CAKE, 1),
            // Iron Age: the cauldron, the skillet, the barrel and the press
            new Dish("cabbage_soup", Tool.POT, 2, List.of(of("mechanical:cabbage"), of("mechanical:leek"), of("mechanical:celery"), MEAT), CABBAGE_SOUP, 6),
            new Dish("vegetable_pottage", Tool.POT, 2, List.of(of("mechanical:turnip"), of("mechanical:parsnip"), of("mechanical:onion"), of("mechanical:celery")),
                    VEGETABLE_POTTAGE, 6),
            new Dish("fried_eggs_and_onion", Tool.PAN, 2, List.of(EGG, EGG, of("mechanical:onion", "mechanical:wild_onion")), FRIED_EGGS_AND_ONION, 1),
            new Dish("sausage", Tool.PAN, 2, List.of(of("mechanical:raw_sausage")), SAUSAGE, 1),
            new Dish("sauerkraut", Tool.BARREL, 2, List.of(of("mechanical:cabbage"), of("mechanical:cabbage"), of("mechanical:salt")), SAUERKRAUT, 3),
            new Dish("cheese", Tool.PRESS, 2, List.of(MILK, MILK, of("mechanical:salt")), CHEESE, 4));

    /** A dish found in a pot or pan: {@code times} sets of its ingredients. */
    public record Match(Dish dish, int times) {
        public int count() {
            return dish.count() * times;
        }
    }

    /** The dish these contents make in this cookware of this tier, or null. Every item must belong to the dish. */
    public static @Nullable Match match(Tool tool, int tier, List<ItemStack> contents) {
        if (contents.isEmpty()) return null;
        for (Dish d : DISHES) {
            if (d.tool() != tool || d.tier() > tier || contents.size() % d.ings().size() != 0) continue;
            int times = contents.size() / d.ings().size();
            if (assign(d.ings(), contents, 0, new int[d.ings().size()], times)) return new Match(d, times);
        }
        return null;
    }

    /** Backtracking: can contents[i..] each take a slot that still has room (each slot takes {@code times} items)? */
    private static boolean assign(List<Ing> ings, List<ItemStack> contents, int i, int[] used, int times) {
        if (i == contents.size()) return true;
        for (int k = 0; k < ings.size(); k++) {
            if (used[k] < times && ings.get(k).test(contents.get(i))) {
                used[k]++;
                if (assign(ings, contents, i + 1, used, times)) return true;
                used[k]--;
            }
        }
        return false;
    }

    /** Could this item be part of any dish of this cookware and tier (so it may go in)? */
    public static boolean isIngredient(Tool tool, int tier, ItemStack s) {
        for (Dish d : DISHES) {
            if (d.tool() == tool && d.tier() <= tier) for (Ing i : d.ings()) if (i.test(s)) return true;
        }
        return false;
    }

    /** Pot tier of a vessel hung in a tripod or sitting in the embers: clay 0, bronze 1, iron 2. */
    public static int potTier(ItemStack vessel) {
        if (vessel.is(CLAY_COOKING_POT_ITEM.get())) return 0;
        if (vessel.is(IRON_CAULDRON.get())) return 2;
        return 1;
    }

    /** The results of a match, in stacks. */
    public static List<ItemStack> results(Match m) {
        List<ItemStack> out = new ArrayList<>();
        Item item = m.dish().result().get();
        for (int n = m.count(); n > 0; ) {
            int k = Math.min(n, item.getDefaultMaxStackSize());
            out.add(new ItemStack(item, k));
            n -= k;
        }
        return out;
    }

    public static void init() {}

    private Kitchen() {}
}
