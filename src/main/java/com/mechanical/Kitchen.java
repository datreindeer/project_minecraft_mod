package com.mechanical;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ITEMS;
import static com.mechanical.Mechanical.MODID;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
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
 * {@link SaltPanBlock}; Steel Age (phase 6) the Steel Pan, the Mortar and Pestle and the Sieve (worked by hand: each
 * empty-hand click grinds or shakes, {@link CookwareBlock.Kind#clicks}) and the Steel Knife Set. The Eastern Caravan's
 * spices (pepper, cinnamon, ginger, saffron) are ground into blends that the steel dishes want. A dish is a fixed set of ingredients in one kind of cookware ({@link #DISHES}); twice the set
 * makes twice the dish. A pot dish needs at least its tier of pot (clay 0, bronze 1, iron 2), so the kettle and the
 * cauldron also cook the clay pot's dishes. Data: scripts/gen_herbs.py, models scripts/herbs_cookware.py, textures
 * scripts/gen_cookware_textures.py.
 */
public final class Kitchen {
    /** Which cookware a dish is made in. */
    public enum Tool { POT, PAN, BARREL, PRESS, MORTAR, SIEVE }

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
    public static final DeferredBlock<CookwareBlock> STEEL_PAN = cookware("steel_pan", CookwareBlock.Kind.STEEL_PAN, MapColor.METAL, SoundType.IRON);
    public static final DeferredBlock<CookwareBlock> MORTAR_AND_PESTLE = cookware("mortar_and_pestle", CookwareBlock.Kind.MORTAR, MapColor.QUARTZ, SoundType.STONE);
    public static final DeferredBlock<CookwareBlock> SIEVE = cookware("sieve", CookwareBlock.Kind.SIEVE, MapColor.WOOD, SoundType.WOOD);
    public static final Supplier<BlockEntityType<CookwareBlockEntity>> COOKWARE_ENTITY = Mechanical.BLOCK_ENTITIES.register("cookware",
            () -> new BlockEntityType<>(CookwareBlockEntity::new, BRONZE_GRIDDLE.get(), IRON_SKILLET.get(), FERMENTING_BARREL.get(), CHEESE_PRESS.get(),
                    STEEL_PAN.get(), MORTAR_AND_PESTLE.get(), SIEVE.get()));
    /** Steel Age butchering: a roll of fine knives that gets more out of every carcass than a single steel knife. */
    public static final DeferredItem<KnifeItem> STEEL_KNIFE_SET = ITEMS.registerItem("steel_knife_set", p -> new KnifeItem(p, 2.8F, 2.0F, KnifeItem.IRON),
            p -> p.sword(Smithing.STEEL, 1.5F, -2.0F));

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

    /** Drunk, not eaten (cider, ale, juice); the effects, if any, when it is finished. */
    private static DeferredItem<Item> drink(String name, int nutrition, MobEffectInstance... effects) {
        return ITEMS.registerSimpleItem(name, p -> {
            var use = Consumables.defaultDrink();
            if (effects.length > 0) use.onConsume(new ApplyStatusEffectsConsumeEffect(List.of(effects)));
            return p.stacksTo(16).food(new FoodProperties.Builder().nutrition(nutrition).saturationModifier(0.3F).build(), use.build());
        });
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
    public static final DeferredItem<Item> ROAST_WITH_HERBS = dish("roast_with_herbs", 10);
    public static final DeferredItem<Item> CIDER = drink("cider", 3);
    public static final DeferredItem<Item> ALE = drink("ale", 4);
    /** Pies: raw (crafted) and baked in the Clay Kiln (KilnBlockEntity.RECIPES), one per fruit of the fruit trees. */
    public static final List<String> PIE_FRUITS = List.of("apple", "plum", "rowan", "bird_cherry");
    public static final Map<String, DeferredItem<Item>> RAW_PIES = new LinkedHashMap<>(), PIES = new LinkedHashMap<>();
    static {
        for (String f : PIE_FRUITS) {
            RAW_PIES.put(f, ITEMS.registerSimpleItem("raw_" + f + "_pie"));
            PIES.put(f, dish(f + "_pie", 8));
        }
    }
    // Steel Age (Mortar and Pestle, Sieve, Steel Pan): the caravan's spices ground into blends
    public static final DeferredItem<Item> KITCHEN_PEPPER = ITEMS.registerSimpleItem("kitchen_pepper");
    public static final DeferredItem<Item> SWEET_SPICE = ITEMS.registerSimpleItem("sweet_spice");
    public static final DeferredItem<Item> GOLDEN_SPICE = ITEMS.registerSimpleItem("golden_spice");
    public static final DeferredItem<Item> PICKLING_SPICE = ITEMS.registerSimpleItem("pickling_spice");
    public static final DeferredItem<Item> HERB_SALT = ITEMS.registerSimpleItem("herb_salt");
    public static final DeferredItem<Item> FINE_FLOUR = ITEMS.registerSimpleItem("fine_flour");
    public static final DeferredItem<Item> SEA_BUCKTHORN_JUICE = drink("sea_buckthorn_juice", 2, new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
    public static final DeferredItem<Item> SAFFRON_RICE = dish("saffron_rice", 10);
    public static final DeferredItem<Item> PEPPERED_STEAK = dish("peppered_steak", 10);
    public static final DeferredItem<Item> SPICED_PILAF = bowl("spiced_pilaf", 11);
    public static final DeferredItem<Item> PICKLED_VEGETABLES = dish("pickled_vegetables", 4);
    public static final DeferredItem<Item> RAW_GINGERBREAD = ITEMS.registerSimpleItem("raw_gingerbread");
    public static final DeferredItem<Item> GINGERBREAD = dish("gingerbread", 6);

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
    private static final Ing HERB = of("#mechanical:culinary_herbs");
    private static final Ing PICKLING_VEG = of("mechanical:onion", "mechanical:wild_onion", "mechanical:turnip", "mechanical:parsnip", "minecraft:carrot",
            "minecraft:beetroot", "mechanical:cabbage", "mechanical:celery");

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
            new Dish("cheese", Tool.PRESS, 2, List.of(MILK, MILK, of("mechanical:salt")), CHEESE, 4),
            new Dish("roast_with_herbs", Tool.PAN, 2, List.of(MEAT, MEAT, HERB, HERB), ROAST_WITH_HERBS, 2),
            new Dish("cider", Tool.BARREL, 2, List.of(of("mechanical:crab_apple"), of("mechanical:crab_apple"), of("mechanical:crab_apple"),
                    of("mechanical:crab_apple")), CIDER, 3),
            // ale: barley bittered with hops, or with bog myrtle as the old gale ale was
            new Dish("ale", Tool.BARREL, 2, List.of(of("mechanical:barley_grain"), of("mechanical:barley_grain"), of("mechanical:barley_grain"),
                    of("mechanical:hop_cones", "mechanical:bog_myrtle_leaves")), ALE, 3),
            // Steel Age: blends in the mortar (the caravan's spices; Herb Salt from what grows here)
            new Dish("kitchen_pepper", Tool.MORTAR, 3, List.of(of("mechanical:black_pepper"), of("mechanical:ginger_root"), of("mechanical:cinnamon_bark")),
                    KITCHEN_PEPPER, 3),
            new Dish("sweet_spice", Tool.MORTAR, 3, List.of(of("mechanical:cinnamon_bark"), of("mechanical:cinnamon_bark"), of("mechanical:ginger_root")),
                    SWEET_SPICE, 3),
            new Dish("golden_spice", Tool.MORTAR, 3, List.of(of("mechanical:saffron"), of("mechanical:black_pepper"), of("mechanical:mustard_seeds")),
                    GOLDEN_SPICE, 3),
            new Dish("pickling_spice", Tool.MORTAR, 3, List.of(of("mechanical:mustard_seeds"), of("mechanical:dill_seeds", "mechanical:dill"),
                    of("mechanical:black_pepper")), PICKLING_SPICE, 3),
            new Dish("herb_salt", Tool.MORTAR, 3, List.of(of("mechanical:salt"), HERB, HERB), HERB_SALT, 2),
            // the sieve
            new Dish("fine_flour", Tool.SIEVE, 3, List.of(of("mechanical:flour")), FINE_FLOUR, 1),
            new Dish("sea_buckthorn_juice", Tool.SIEVE, 3, List.of(of("mechanical:sea_buckthorn_berries"), of("mechanical:sea_buckthorn_berries"),
                    of("mechanical:sea_buckthorn_berries"), HONEY), SEA_BUCKTHORN_JUICE, 2),
            // the steel pan, and the iron cauldron cooking with the blends
            new Dish("saffron_rice", Tool.PAN, 3, List.of(of("mechanical:rice"), of("mechanical:rice"), of("mechanical:golden_spice"), ALLIUM), SAFFRON_RICE, 2),
            new Dish("peppered_steak", Tool.PAN, 3, List.of(MEAT, MEAT, of("mechanical:kitchen_pepper")), PEPPERED_STEAK, 2),
            new Dish("spiced_pilaf", Tool.POT, 2, List.of(of("mechanical:rice"), of("mechanical:rice"), of("mechanical:kitchen_pepper"), MEAT, ALLIUM),
                    SPICED_PILAF, 5),
            new Dish("pickled_vegetables", Tool.BARREL, 2, List.of(PICKLING_VEG, PICKLING_VEG, PICKLING_VEG, of("mechanical:pickling_spice"),
                    of("mechanical:salt")), PICKLED_VEGETABLES, 5));

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
