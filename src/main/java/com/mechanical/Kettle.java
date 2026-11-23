package com.mechanical;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ITEMS;
import static com.mechanical.Mechanical.MODID;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.mechanical.wildlife.Wildlife;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Bronze Age: the Bronze Kettle, cast in a Clay Kettle Mold (8 units, {@link Bronze#BRONZE_KETTLE}) and hung in a
 * {@link TripodBlock} standing over a Campfire or a Fire Pit. Fill it with water, drop things in, and it simmers while the
 * fire under it burns (a Fire Pit {@link #PIT_RATE} times as fast; embers only keep it warm). What goes in first decides
 * the {@link Mode}: food makes stew (or broth, for whole plucked birds), fat and bones render Tallow, hides and fish
 * bones boil down to Hide Glue, bark makes a tanning liquor that turns Scraped Hides into Leather. The pot itself:
 * {@link TripodBlockEntity}; the liquid and chunks: client.KettleRenderer. Data: scripts/gen_kettle.py, textures:
 * scripts/gen_kettle_textures.py.
 */
public final class Kettle {
    /** Ingredients a kettle holds; a Poor Bronze kettle is thinner and holds less. */
    public static final int CAPACITY = 8, POOR_CAPACITY = 6;
    /** Stew bowls a pot gives at most, and about how much hunger each bowl fills. Broth: 2 bowls a bird, up to 8. */
    public static final int MAX_PORTIONS = 6, PORTION_NUTRITION = 7, MAX_BROTH = 8, BROTH_PER_BIRD = 2;
    /** Stewing beats roasting: the cooked value times this, plus a bit for every different ingredient, up to the cap. */
    public static final float BONUS = 1.25F, VARIETY_BONUS = 0.05F, MAX_BONUS = 1.5F;
    /** Simmering time in campfire ticks (a Fire Pit cooks {@link #PIT_RATE} times as fast). */
    public static final int FOOD_TIME = 1800, RENDER_TIME = 2400, TAN_TIME = 3600;
    public static final float PIT_RATE = 1.5F, EMBER_RATE = 0.35F;
    /** Heat: boils at {@link #BOIL}; embers can't bring it past {@link #WARM}. */
    public static final float BOIL = 100, WARM = 60, HEAT_UP = 0.5F, COOL = 0.25F;

    public enum Mode { FOOD, RENDER, TAN }

    public static final DeferredBlock<TripodBlock> TRIPOD = BLOCKS.registerBlock("tripod", TripodBlock::new, p -> p
            .mapColor(MapColor.WOOD).strength(1.0F).sound(SoundType.WOOD).noCollision().noOcclusion().pushReaction(PushReaction.POPPED).ignitedByLava());
    public static final DeferredItem<BlockItem> TRIPOD_ITEM = ITEMS.registerSimpleBlockItem("tripod", TRIPOD);
    public static final Supplier<BlockEntityType<TripodBlockEntity>> TRIPOD_ENTITY = Mechanical.BLOCK_ENTITIES.register("tripod",
            () -> new BlockEntityType<>(TripodBlockEntity::new, TRIPOD.get()));

    /** A bowl of stew: its FOOD is set when ladled, from what went into the pot ({@link #INGREDIENTS}). */
    public static final DeferredItem<Item> STEW = ITEMS.registerSimpleItem("stew", p -> p.stacksTo(1)
            .food(new FoodProperties.Builder().nutrition(1).saturationModifier(0.6F).build()).usingConvertsTo(Items.BOWL));
    public static final DeferredItem<Item> BROTH = ITEMS.registerSimpleItem("broth", p -> p.stacksTo(1)
            .food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.6F).build()).usingConvertsTo(Items.BOWL));
    /** Boiled from hides and fish bones; binds like Cordage at the Tool Station, on the Sinew Bow and in Bellows. */
    public static final DeferredItem<Item> HIDE_GLUE = ITEMS.registerSimpleItem("hide_glue");
    /** A kettle-full of blood caught under a Hanging Rack ({@link HangingRackBlock#KETTLE}), scooped up with a Bucket (a Bronze Bucket: BronzeTools.BRONZE_BLOOD_BUCKET). */
    public static final DeferredItem<Item> BLOOD_BUCKET = ITEMS.registerSimpleItem("blood_bucket", p -> p.stacksTo(1).craftRemainder(Items.BUCKET));
    /** Blood, flour and lard stuffed into a casing (crafted from a blood bucket); cooked on a campfire or in a smoker into Blood Sausage. */
    public static final DeferredItem<Item> RAW_BLOOD_SAUSAGE = ITEMS.registerSimpleItem("raw_blood_sausage");
    public static final DeferredItem<Item> BLOOD_SAUSAGE = ITEMS.registerSimpleItem("blood_sausage",
            p -> p.food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.8F).build()));

    /** What went into a stew, one entry per ingredient (shown in its tooltip). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<Item>>> INGREDIENTS = Casting.COMPONENTS.registerComponentType(
            "stew_ingredients", b -> b.persistent(BuiltInRegistries.ITEM.byNameCodec().listOf())
                    .networkSynchronized(ByteBufCodecs.registry(Registries.ITEM).apply(ByteBufCodecs.list())));

    /** Advancements granted from code (criterion "done"), see scripts/gen_advancements.py. */
    public static final Identifier HEARTY_STEW_ADVANCEMENT = Identifier.fromNamespaceAndPath(MODID, "food/hearty_stew");
    public static final Identifier TANNED_ADVANCEMENT = Identifier.fromNamespaceAndPath(MODID, "bronze_age/tanned");
    /** A stew with at least this many ingredients earns {@link #HEARTY_STEW_ADVANCEMENT}. */
    public static final int HEARTY = 4;

    public static void init() {}

    public static int capacity(ItemStack kettle) {
        return Bronze.isPoor(kettle) ? POOR_CAPACITY : CAPACITY;
    }

    public static int cookTime(Mode mode) {
        return switch (mode) {
            case FOOD -> FOOD_TIME;
            case RENDER -> RENDER_TIME;
            case TAN -> TAN_TIME;
        };
    }

    // ---- what can go in ----

    static boolean isPlucked(ItemStack s) {
        return Birds.isPlucked(s);
    }

    private static boolean renders(ItemStack s) {
        return s.is(Wildlife.LARD.get()) || s.is(Wildlife.BEAR_FAT.get()) || s.is(Items.BONE)
                || s.is(Mechanical.RAW_HIDE.get()) || s.is(Items.RABBIT_HIDE) || s.is(Mechanical.FISH_BONES.get());
    }

    /** The mode this item would put the pot in, or null if it can't go in at all. */
    public static Mode modeOf(ServerLevel level, ItemStack s) {
        if (s.is(Mechanical.BARK_TAG) || s.is(Mechanical.SCRAPED_HIDE.get())) return Mode.TAN;
        if (renders(s)) return Mode.RENDER;
        return cookedValue(level, s) != null ? Mode.FOOD : null;
    }

    /** A food with no side effects and no bowl or bottle of its own; null otherwise. */
    private static FoodProperties plainFood(ItemStack s) {
        FoodProperties food = s.get(DataComponents.FOOD);
        Consumable eat = s.get(DataComponents.CONSUMABLE);
        if (food == null || s.has(DataComponents.USE_REMAINDER) || eat != null && !eat.onConsumeEffects().isEmpty()) return null;
        return food;
    }

    /** Things with no food value of their own that still feed a stew. */
    private static FoodProperties fallback(ItemStack s) {
        if (s.is(Mechanical.BOILED_EGG.get())) return Mechanical.PEELED_EGG.get().components().get(DataComponents.FOOD);
        if (s.is(Farming.GRAINS) || s.is(Farming.FLOUR.get()) || s.is(Items.BROWN_MUSHROOM) || s.is(Items.RED_MUSHROOM))
            return new FoodProperties(2, 2.4F, false);
        return null;
    }

    private static FoodProperties value(ItemStack s) {
        FoodProperties f = plainFood(s);
        if (f == null) f = fallback(s);
        return f == null ? null : new FoodProperties(f.nutrition() * s.getCount(), f.saturation() * s.getCount(), false);
    }

    /** What one of this item is worth in a stew: as it comes off a campfire if it cooks there, else as it is; null if it can't go in. */
    public static FoodProperties cookedValue(ServerLevel level, ItemStack stack) {
        if (stack.is(STEW.get()) || stack.is(BROTH.get())) return null;
        ItemStack one = stack.copyWithCount(1);
        ItemStack cooked = cooked(level, one);
        FoodProperties f = cooked.isEmpty() ? null : value(cooked);
        return f != null ? f : value(one);
    }

    /** The campfire result of one of this item, or empty. */
    static ItemStack cooked(ServerLevel level, ItemStack one) {
        SingleRecipeInput input = new SingleRecipeInput(one);
        return level.recipeAccess().getRecipeFor(RecipeType.CAMPFIRE_COOKING, input, level).map(r -> r.value().assemble(input)).orElse(ItemStack.EMPTY);
    }

    // ---- what comes out ----

    /** Solid things to take out with an empty hand, and bowls to ladle ({@code portion} times {@code portions}). */
    public record Result(List<ItemStack> outputs, ItemStack portion, int portions) {}

    /** What a finished pot of these ingredients (all one mode) gives. Called once; the ingredients are used up. */
    public static Result finish(ServerLevel level, List<ItemStack> contents) {
        Mode mode = contents.isEmpty() ? null : modeOf(level, contents.getFirst());
        List<ItemStack> out = new ArrayList<>();
        if (mode == Mode.TAN) {
            int hides = count(contents, Mechanical.SCRAPED_HIDE.get()), bark = (int) contents.stream().filter(s -> s.is(Mechanical.BARK_TAG)).count();
            int tanned = Math.min(hides, bark);
            add(out, Items.LEATHER, tanned + tanned / 2); // bark-tanned hides come out fuller: 3 Leather from 2
            add(out, Mechanical.SCRAPED_HIDE.get(), hides - tanned); // no bark left for these
            return new Result(out, ItemStack.EMPTY, 0);
        }
        if (mode == Mode.RENDER) {
            add(out, Mechanical.TALLOW.get(), 2 * count(contents, Wildlife.LARD.get()) + 3 * count(contents, Wildlife.BEAR_FAT.get())
                    + count(contents, Items.BONE) / 2);
            add(out, HIDE_GLUE.get(), 3 * count(contents, Mechanical.RAW_HIDE.get()) + count(contents, Items.RABBIT_HIDE)
                    + count(contents, Mechanical.FISH_BONES.get()) / 2);
            return new Result(out, ItemStack.EMPTY, 0);
        }
        if (mode != Mode.FOOD) return new Result(out, ItemStack.EMPTY, 0);
        if (contents.stream().allMatch(Kettle::isPlucked)) {
            // whole birds: they come out boiled (their cooked meat) and leave a rich broth behind
            for (ItemStack bird : contents) out.add(cooked(level, bird.copyWithCount(1)));
            out.removeIf(ItemStack::isEmpty);
            return new Result(out, new ItemStack(BROTH.get()), Math.min(MAX_BROTH, BROTH_PER_BIRD * contents.size()));
        }
        int nutrition = 0;
        List<Item> items = new ArrayList<>();
        for (ItemStack s : contents) {
            FoodProperties f = cookedValue(level, s);
            if (f == null) continue;
            nutrition += f.nutrition();
            items.add(s.getItem());
        }
        if (items.isEmpty()) return new Result(out, ItemStack.EMPTY, 0);
        float bonus = Math.min(MAX_BONUS, BONUS + VARIETY_BONUS * (items.stream().distinct().count() - 1));
        float total = nutrition * bonus;
        int portions = Math.clamp(Math.round(total / PORTION_NUTRITION), 1, MAX_PORTIONS);
        ItemStack bowl = new ItemStack(STEW.get());
        // rounded down: dividing the pot never makes more food than went in (times the bonus); a meal fills you longest
        int n = Math.max(1, (int) (total / portions));
        bowl.set(DataComponents.FOOD, new FoodProperties(n, FoodValues.saturation(n, FoodValues.MEAL), false));
        bowl.set(INGREDIENTS.get(), List.copyOf(items));
        return new Result(out, bowl, portions);
    }

    private static int count(List<ItemStack> contents, Item item) {
        return (int) contents.stream().filter(s -> s.is(item)).count();
    }

    private static void add(List<ItemStack> out, Item item, int n) {
        while (n > 0) {
            int k = Math.min(n, item.getDefaultMaxStackSize());
            out.add(new ItemStack(item, k));
            n -= k;
        }
    }

    // ---- looks ----

    /** The colour of the liquid: water, a stew tinted by its ingredients, milky fat, or dark bark liquor. */
    public static int colour(Mode mode, List<ItemStack> contents) {
        if (contents.isEmpty() || mode == null) return 0x3F76E4;
        if (mode == Mode.RENDER) return 0xE6DDB8;
        if (mode == Mode.TAN) return 0x6B3A1C;
        int r = 0, g = 0, b = 0;
        for (ItemStack s : contents) {
            int c = tint(s);
            r += c >> 16 & 255;
            g += c >> 8 & 255;
            b += c & 255;
        }
        int n = contents.size(), base = 0xA8783A; // a brown broth, pulled toward the ingredients
        return (((base >> 16 & 255) + r / n) / 2) << 16 | (((base >> 8 & 255) + g / n) / 2) << 8 | ((base & 255) + b / n) / 2;
    }

    private static int tint(ItemStack s) {
        if (s.is(Preserving.DRYABLE_BERRIES) || s.is(Items.SWEET_BERRIES) || s.is(Items.BEETROOT)) return 0x8A1E3C;
        if (s.is(Items.CARROT) || s.is(Items.PUMPKIN_PIE)) return 0xE07A22;
        if (s.is(Preserving.SMOKABLE_FISH)) return 0xD8C8A0;
        if (s.is(Preserving.SMOKABLE_MEAT) || isPlucked(s)) return 0x7A4426;
        if (s.is(Preserving.DRYABLE_MUSHROOMS) || s.is(Items.BROWN_MUSHROOM) || s.is(Items.RED_MUSHROOM)) return 0x7E6650;
        if (s.is(Farming.GRAINS) || s.is(Farming.FLOUR.get()) || s.is(Items.POTATO)) return 0xE4D4A2;
        if (s.is(Mechanical.BOILED_EGG.get()) || s.is(Birds.GOOSE_EGG.get()) || s.is(Items.EGG)) return 0xF0D870;
        return 0x8A9A4A; // greens and the rest
    }

    /** Grants a code-driven advancement to this player. */
    public static void award(ServerPlayer player, Identifier id) {
        var holder = player.level().getServer().getAdvancements().get(id);
        if (holder != null) player.getAdvancements().award(holder, "done");
    }

    @EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    static final class Tooltip {
        @SubscribeEvent
        static void onTooltip(ItemTooltipEvent event) {
            ItemStack stack = event.getItemStack();
            List<Item> items = stack.get(INGREDIENTS.get());
            FoodProperties food = stack.get(DataComponents.FOOD);
            if (items == null || food == null || event.getToolTip().isEmpty()) return;
            Map<Item, Integer> counts = new LinkedHashMap<>();
            for (Item i : items) counts.merge(i, 1, Integer::sum);
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("tooltip.mechanical.stew.food", food.nutrition(), String.format("%.1f", food.saturation())).withStyle(ChatFormatting.GOLD));
            counts.forEach((item, n) -> lines.add((n > 1 ? Component.translatable("tooltip.mechanical.stew.many", Component.translatable(item.getDescriptionId()), n)
                    : Component.translatable(item.getDescriptionId())).withStyle(ChatFormatting.GRAY)));
            event.getToolTip().addAll(1, lines);
        }

        private Tooltip() {}
    }

    private Kettle() {}
}
