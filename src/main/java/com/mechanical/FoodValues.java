package com.mechanical;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.food.FoodConstants;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

/**
 * How long food keeps you full (docs/seasons-and-tribes.md, "Mat och bunkring"). A day is an hour, so you eat several
 * times a day: raw food barely fills you, cooked and preserved food does, and prepared meals the longest. Each food keeps
 * its nutrition (its size); its saturation is nutrition x 2 x the modifier of the first rule its id matches. New foods
 * slot in by name (cooked_*, smoked_*, *_stew ...); add a rule here for anything else.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class FoodValues {
    public static final float RAW = 0.1F, COOKED = 0.8F, DELICACY = 1.0F, MEAL = 1.2F;

    /** Item id regex -> saturation modifier (null: leave as it is). First match wins. */
    static final Map<Pattern, Float> RULES = new LinkedHashMap<>();
    static {
        rule("minecraft:(golden_.*|enchanted_golden_apple|honey_bottle|chorus_fruit|rotten_flesh|spider_eye|poisonous_potato"
                + "|pufferfish|.*_bucket|dried_kelp|cookie)", null); // special or snack foods: vanilla values
        rule("mechanical:(cooked_bison_tongue|roasted_beaver_tail|cooked_loin|sturgeon_roe)", DELICACY);
        rule("(minecraft|mechanical):(.*_stew|.*_soup|stew|broth|porridge|bread|pumpkin_pie)", MEAL); // Kettle stews: Kettle.finish
        // Kitchen dishes (flora and food plan, section 6): meals, and keeping sweets and preserves
        rule("mechanical:(.*_pottage|berry_mash|spiced_porridge|flatbread|sausage|fried_eggs_and_onion|herb_roasted_fish)", MEAL);
        rule("mechanical:(honey_cake|elderberry_syrup|sloe_jam|cheese|sauerkraut|herbal_tea|tea)", COOKED);
        rule("(minecraft|mechanical):(cooked_.*|roasted_.*|baked_.*|smoked_.*|dried_.*|.*jerky|stockfish|peeled_egg)", COOKED);
        rule("(minecraft|mechanical):.*", RAW); // raw meat and fish, berries, nuts, mushrooms, roots, fruit
    }

    private static void rule(String regex, Float modifier) {
        RULES.put(Pattern.compile(regex), modifier);
    }

    /** The saturation modifier for this item id, or null to keep its own. */
    static Float modifier(String id) {
        for (var e : RULES.entrySet()) if (e.getKey().matcher(id).matches()) return e.getValue();
        return null;
    }

    public static float saturation(int nutrition, float modifier) {
        return FoodConstants.saturationByModifier(nutrition, modifier);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onModifyComponents(ModifyDefaultComponentsEvent event) {
        event.modifyMatching((item, components) -> components.get(DataComponents.FOOD) != null && modifier(id(item)) != null,
                (components, context, item) -> {
                    FoodProperties f = components.get(DataComponents.FOOD);
                    components.set(DataComponents.FOOD, new FoodProperties(f.nutrition(), saturation(f.nutrition(), modifier(id(item))), f.canAlwaysEat()));
                });
    }

    private static String id(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    private FoodValues() {}
}
