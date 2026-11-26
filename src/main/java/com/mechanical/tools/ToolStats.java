package com.mechanical.tools;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;

/** What a tool's stats are and what each part does to them, for the Tool Station and the recipe viewer. */
public final class ToolStats {
    /** Durability (0: never wears), mining speed (0: not a mining tool), attack damage as the tooltip shows it (0: none). */
    public record Stats(int durability, float speed, float damage) {}

    /** One part against the recipe's default part in that slot: factors on durability and speed, attack damage added. */
    public record Share(Item part, boolean isDefault, float durability, float speed, float damage) {}

    public static Stats of(ItemStack tool) {
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
        return new Stats(tool.isDamageableItem() ? tool.getMaxDamage() : 0, speed, damage);
    }

    public static Share binding(Item defaultBinding, Item binding) {
        return share(ToolParts.binding(defaultBinding), ToolParts.binding(binding), defaultBinding, binding);
    }

    public static Share rod(Item defaultRod, Item rod) {
        return share(ToolParts.rod(defaultRod), ToolParts.rod(rod), defaultRod, rod);
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
