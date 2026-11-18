package com.mechanical.client;

import java.util.ArrayList;
import java.util.List;

import com.mechanical.BoneHookRodItem;
import com.mechanical.CrudeBowItem;
import com.mechanical.KnifeItem;
import com.mechanical.Mechanical;
import com.mechanical.SpecialArrowItem;
import com.mechanical.Temperature;
import com.mechanical.TreeChopping;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Stats in item tooltips, right under the name: durability (always, not only with F3+H), mining speed and what it can
 * mine, chopping (axes), butchering (knives), bow and arrow damage, fishing and cold insulation. Vanilla's attack lines
 * stay where they are.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class StatTooltips {
    private static final ChatFormatting LABEL = ChatFormatting.GRAY, VALUE = ChatFormatting.WHITE, GOOD = ChatFormatting.GREEN, BAD = ChatFormatting.RED;

    @SubscribeEvent
    static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> lines = new ArrayList<>();

        if (stack.isDamageableItem()) {
            int max = stack.getMaxDamage(), left = max - stack.getDamageValue();
            float frac = (float) left / max;
            lines.add(stat("Durability", Component.literal(left + " / " + max)
                    .withStyle(frac > 0.5F ? GOOD : frac > 0.2F ? ChatFormatting.YELLOW : BAD)));
        }

        // mining: pickaxes (stone), axes (logs), shovels (dirt)
        mining(lines, stack, ItemTags.PICKAXES, Blocks.STONE.defaultBlockState(), "Mining speed");
        mining(lines, stack, ItemTags.AXES, Blocks.OAK_LOG.defaultBlockState(), "Chopping speed");
        mining(lines, stack, ItemTags.SHOVELS, Blocks.DIRT.defaultBlockState(), "Digging speed");
        if (stack.is(ItemTags.PICKAXES)) {
            String tier = stack.isCorrectToolForDrops(Blocks.DIAMOND_ORE.defaultBlockState()) ? "Diamond"
                    : stack.isCorrectToolForDrops(Blocks.IRON_ORE.defaultBlockState()) ? "Iron ore"
                    : stack.isCorrectToolForDrops(Blocks.STONE.defaultBlockState()) ? "Stone" : "Nothing hard";
            lines.add(stat("Can mine", value(tier)));
        }
        if (stack.is(ItemTags.AXES)) {
            lines.add(stat("Chops per stage", value(String.valueOf(TreeChopping.chopsNeeded(stack)))));
            lines.add(stat("Clean cut chance", percent(TreeChopping.successChance(stack))));
            int bonus = TreeChopping.firewoodBonus(stack);
            if (bonus > 0) lines.add(stat("Firewood bonus", Component.literal("+" + bonus).withStyle(GOOD)));
        }

        if (stack.getItem() instanceof KnifeItem knife) {
            lines.add(stat("Butchering speed", times(knife.speed())));
            lines.add(stat("Meat yield", times(knife.yieldMultiplier())));
            if (knife.hideMultiplier() != knife.yieldMultiplier()) lines.add(stat("Hide & pelt yield", times(knife.hideMultiplier())));
            lines.add(stat("Tough hides (bear)", knife.tier() >= KnifeItem.TUSK
                    ? Component.literal("Yes").withStyle(GOOD) : Component.literal("No").withStyle(BAD)));
        }

        if (stack.getItem() instanceof BowItem) {
            lines.add(stat("Arrow damage", percent(stack.getItem() instanceof CrudeBowItem ? (float) CrudeBowItem.DAMAGE_FACTOR : 1.0F)));
        }
        if (stack.getItem() instanceof SpecialArrowItem arrow) {
            lines.add(stat("Damage", value(String.format("%.1f", arrow.damage()) + " (arrow: 2.0)")));
            if (arrow.fire()) lines.add(Component.literal(" Sets the target on fire").withStyle(ChatFormatting.GOLD));
        } else if (stack.is(Items.ARROW)) {
            lines.add(stat("Damage", value("2.0")));
        }
        if (stack.getItem() instanceof BoneHookRodItem) {
            lines.add(stat("Bites", Component.literal(BoneHookRodItem.LURE_TICKS / 20 + " s sooner").withStyle(GOOD)));
        } else if (stack.getItem() instanceof FishingRodItem && stack.is(Mechanical.CRUDE_FISHING_ROD.get())) {
            lines.add(stat("Bites", value("normal")));
        }

        float insulation = stack.is(Items.LEATHER_HELMET) || stack.is(Items.LEATHER_CHESTPLATE) || stack.is(Items.LEATHER_LEGGINGS)
                || stack.is(Items.LEATHER_BOOTS) ? Temperature.LEATHER_PER_PIECE
                : stack.is(Mechanical.FUR_HOOD.get()) || stack.is(Mechanical.FUR_COAT.get()) || stack.is(Mechanical.FUR_LEGGINGS.get())
                || stack.is(Mechanical.FUR_BOOTS.get()) ? Temperature.FUR_PER_PIECE : 0;
        if (insulation > 0) lines.add(stat("Cold insulation", Component.literal("+" + Math.round(insulation * 100) + "%").withStyle(ChatFormatting.AQUA)));

        if (lines.isEmpty()) return;
        List<Component> tooltip = event.getToolTip();
        tooltip.addAll(Math.min(1, tooltip.size()), lines);
    }

    private static void mining(List<Component> lines, ItemStack stack, net.minecraft.tags.TagKey<net.minecraft.world.item.Item> tag, BlockState probe, String label) {
        if (stack.is(tag)) lines.add(stat(label, value(String.format("%.1f", stack.getDestroySpeed(probe)))));
    }

    private static MutableComponent stat(String label, Component value) {
        return Component.literal(" " + label + ": ").withStyle(LABEL).append(value);
    }

    private static Component value(String text) {
        return Component.literal(text).withStyle(VALUE);
    }

    private static Component percent(float f) {
        return Component.literal(Math.round(f * 100) + "%").withStyle(f >= 0.75F ? GOOD : f >= 0.6F ? ChatFormatting.YELLOW : BAD);
    }

    private static Component times(float f) {
        return Component.literal(String.format("%.1f×", f)).withStyle(f > 1.0F ? GOOD : VALUE);
    }

    private StatTooltips() {}
}
