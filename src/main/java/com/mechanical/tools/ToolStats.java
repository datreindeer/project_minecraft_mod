package com.mechanical.tools;

import com.mechanical.BoneHookRodItem;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;

/** What a tool's stats are and what each part does to them, for the Tool Station and the recipe viewer. */
public final class ToolStats {
    /**
     * Durability (0: never wears), mining speed (0: not a mining tool), attack damage as the tooltip shows it (0: none).
     * A bow: speed = seconds to a full draw, damage = the full draw's arrow damage multiplier. A fishing rod: speed = seconds bites come sooner.
     */
    public record Stats(int durability, float speed, float damage) {}

    /**
     * One part against the recipe's default part in that slot: factors on durability and speed, attack damage added.
     * A bow: speed = draw time factor (below 1 is quicker), damage = arrow speed factor. A fishing rod: speed = seconds sooner, damage 0.
     */
    public record Share(Item part, boolean isDefault, float durability, float speed, float damage) {}

    public static Stats of(ItemStack tool) {
        int durability = tool.isDamageableItem() ? tool.getMaxDamage() : 0;
        if (tool.getItem() instanceof BowItem) return new Stats(durability, drawTicks(tool) / 20.0F, bowDamage(tool));
        if (tool.getItem() instanceof BoneHookRodItem rod) return new Stats(durability, rod.lureTicks(tool) / 20.0F, 0);
        Tool miner = tool.get(DataComponents.TOOL);
        float speed = 0;
        // the rule for the tool's own mineable/* tag; without one (swords: cobwebs at 15, then sword_efficient) the last rule with a speed
        if (miner != null) for (Tool.Rule rule : miner.rules()) {
            if (rule.speed().isEmpty()) continue;
            if (rule.blocks().unwrapKey().filter(tag -> tag.location().getPath().startsWith("mineable/")).isPresent()) {
                speed = rule.speed().get();
                break;
            }
            speed = rule.speed().get();
        }
        ItemAttributeModifiers attributes = tool.get(DataComponents.ATTRIBUTE_MODIFIERS);
        float damage = 0;
        if (attributes != null) for (ItemAttributeModifiers.Entry e : attributes.modifiers())
            if (e.matches(Attributes.ATTACK_DAMAGE, Item.BASE_ATTACK_DAMAGE_ID)) damage = (float) (1 + e.modifier().amount()); // + the hand's 1
        return new Stats(durability, speed, damage);
    }

    /**
     * Arrow damage is its base damage times its speed, so a full draw's speed is the multiplier: vanilla bow 3.0
     * (BowItem: power 1 x 3), crossbow 3.15 (CrossbowItem.ARROW_POWER, private); the mod's bows scale the bow's, their parts scale theirs.
     */
    public static float bowDamage(ItemStack stack) {
        if (stack.getItem() instanceof net.minecraft.world.item.CrossbowItem) return 3.15F;
        float bow = 3.0F * ToolParts.ranged(stack).power();
        if (stack.getItem() instanceof com.mechanical.CrudeBowItem) return bow * (float) com.mechanical.CrudeBowItem.DAMAGE_FACTOR;
        if (stack.getItem() instanceof com.mechanical.wildlife.mountain.HornBowItem) return bow * com.mechanical.wildlife.mountain.HornBowItem.SPEED_FACTOR;
        if (stack.getItem() instanceof com.mechanical.YewLongbowItem) return bow * com.mechanical.YewLongbowItem.SPEED_FACTOR;
        return bow;
    }

    /** Ticks to a full draw (the crossbow's 1.25 s without a holder to ask about Quick Charge). */
    public static int drawTicks(ItemStack stack) {
        if (stack.getItem() instanceof net.minecraft.world.item.CrossbowItem) return 25;
        return ToolParts.ranged(stack).drawTicks(stack.getItem() instanceof com.mechanical.YewLongbowItem ? com.mechanical.YewLongbowItem.DRAW_TICKS : BowItem.MAX_DRAW_DURATION);
    }

    public static Share binding(Item defaultBinding, Item binding) {
        return share(ToolParts.binding(defaultBinding), ToolParts.binding(binding), defaultBinding, binding);
    }

    public static Share rod(Item defaultRod, Item rod) {
        return share(ToolParts.rod(defaultRod), ToolParts.rod(rod), defaultRod, rod);
    }

    /** The part in slot 0-2 of a recipe of this kind against the recipe's own. */
    public static Share share(ToolParts.Kind kind, int slot, Item defaultItem, Item item) {
        ToolParts.Part def = ToolParts.part(kind, slot, defaultItem), chosen = ToolParts.part(kind, slot, item);
        if (def == null || chosen == null || kind == ToolParts.Kind.TOOL) return share(def, chosen, defaultItem, item);
        float durability = chosen.durability() / def.durability();
        if (kind == ToolParts.Kind.BOW) return new Share(item, item == defaultItem, durability, def.speed() / chosen.speed(), chosen.power() / def.power());
        return new Share(item, item == defaultItem, durability, (chosen.lure() - def.lure()) / 20.0F, 0);
    }

    private static Share share(ToolParts.Part def, ToolParts.Part chosen, Item defaultItem, Item item) {
        if (def == null || chosen == null) return new Share(item, item == defaultItem, 1, 1, 0);
        return new Share(item, item == defaultItem, chosen.durability() / def.durability(), chosen.speed() / def.speed(), chosen.damage() - def.damage());
    }

    /** The durability bonus a Smithing level gives, as a fraction (40: 0.2). */
    public static float smithing(int level) {
        return ToolParts.SMITHING_BONUS_PER_LEVEL * level;
    }

    private ToolStats() {}
}
