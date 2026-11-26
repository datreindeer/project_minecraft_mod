package com.mechanical.client;

import java.util.ArrayList;
import java.util.List;

import com.mechanical.BoneHookRodItem;
import com.mechanical.BronzeTools;
import com.mechanical.Coins;
import com.mechanical.FallenLogBlock;
import com.mechanical.Farming;
import com.mechanical.FlailItem;
import com.mechanical.PanItem;
import com.mechanical.FlintPickaxeItem;
import com.mechanical.Ironworking;
import com.mechanical.SawItem;
import com.mechanical.Spinning;
import com.mechanical.Tinder;
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

        // hot metal's bar is its heat, not wear
        if (stack.isDamageableItem() && !com.mechanical.Ironworking.hotItem(stack)) {
            int max = stack.getMaxDamage(), left = max - stack.getDamageValue();
            float frac = (float) left / max;
            lines.add(stat("Durability", Component.literal(left + " / " + max)
                    .withStyle(frac > 0.5F ? GOOD : frac > 0.2F ? ChatFormatting.YELLOW : BAD)));
        }

        com.mechanical.tools.ToolParts.Parts parts = stack.get(com.mechanical.tools.ToolParts.TOOL_PARTS.get());
        if (parts != null) {
            String key = stack.getItem() instanceof BowItem ? "tooltip.mechanical.bow_parts" : stack.getItem() instanceof FishingRodItem ? "tooltip.mechanical.fishing_rod_parts"
                    : stack.getItem() instanceof net.minecraft.world.item.CrossbowItem ? "tooltip.mechanical.crossbow_parts" : "tooltip.mechanical.tool_parts";
            lines.add(Component.translatable(key, new ItemStack(parts.rod()).getHoverName(), new ItemStack(parts.binding()).getHoverName(),
                    new ItemStack(parts.head()).getHoverName()).withStyle(LABEL));
            if (parts.smithing() > 0) lines.add(Component.translatable("tooltip.mechanical.tool_smithing", parts.smithing(),
                    Math.round(com.mechanical.tools.ToolStats.smithing(parts.smithing()) * 100)).withStyle(GOOD));
        }

        // mining: pickaxes (stone), axes (logs), shovels (dirt)
        mining(lines, stack, ItemTags.PICKAXES, Blocks.STONE.defaultBlockState(), "Mining speed");
        mining(lines, stack, ItemTags.AXES, Blocks.OAK_LOG.defaultBlockState(), "Chopping speed");
        mining(lines, stack, ItemTags.SHOVELS, Blocks.DIRT.defaultBlockState(), "Digging speed");
        boolean quarry = BronzeTools.isQuarryHammer(stack);
        if (quarry) lines.add(stat("Mining speed", value(String.format("%.1f", stack.getDestroySpeed(Blocks.STONE.defaultBlockState())))));
        if (stack.is(ItemTags.PICKAXES) || quarry) {
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

        // bows and the crossbow: what a fully drawn shot deals per point of the arrow's damage, and how long the draw takes
        if (stack.getItem() instanceof BowItem || stack.getItem() instanceof net.minecraft.world.item.CrossbowItem) {
            lines.add(Component.translatable("tooltip.mechanical.bow.damage_multiplier", value(multiplier(com.mechanical.tools.ToolStats.bowDamage(stack)))).withStyle(LABEL));
            int draw = stack.getItem() instanceof net.minecraft.world.item.CrossbowItem && event.getEntity() != null
                    ? net.minecraft.world.item.CrossbowItem.getChargeDuration(stack, event.getEntity()) : com.mechanical.tools.ToolStats.drawTicks(stack);
            lines.add(Component.translatable("tooltip.mechanical.bow.draw_time", value(String.format(java.util.Locale.ROOT, "%.2f", draw / 20.0F).replaceAll("0$", "") + " s")).withStyle(LABEL));
            if (stack.getItem() instanceof net.minecraft.world.item.CrossbowItem) use(lines, Component.translatable(stack.is(com.mechanical.Smithing.STEEL_CROSSBOW.get())
                    ? "tooltip.mechanical.crossbow.all_bolts" : "tooltip.mechanical.crossbow.bolts_only").getString());
        }
        if (stack.getItem() instanceof SpecialArrowItem arrow) {
            lines.add(stat("Damage", value(String.format("%.1f", arrow.damage()) + " (arrow: 2.0)")));
            if (arrow.fire()) lines.add(Component.literal(" Sets the target on fire").withStyle(ChatFormatting.GOLD));
            if (stack.is(com.mechanical.Smithing.BOLTS)) {
                lines.add(stat("Armor ignored", value(Math.round(com.mechanical.Smithing.boltPierce(stack) * 100) + "%")));
                use(lines, stack.is(com.mechanical.Smithing.STEEL_BOLT.get()) ? "Steel Crossbow only" : "Crossbow only");
            }
        } else if (stack.is(Items.ARROW)) {
            lines.add(stat("Damage", value("2.0")));
        }
        if (stack.getItem() instanceof BoneHookRodItem rod) {
            int lure = rod.lureTicks(stack);
            lines.add(stat("Bites", lure > 0 ? Component.literal(String.format(java.util.Locale.ROOT, "%.1f", lure / 20.0F).replace(".0", "") + " s sooner").withStyle(GOOD) : value("normal")));
            if (rod.luck > 0) lines.add(stat("Luck of the Sea", Component.literal("+" + rod.luck).withStyle(GOOD)));
        }

        float insulation = Temperature.insulation(stack);
        if (insulation > 0) lines.add(stat("Cold insulation", Component.literal("+" + Math.round(insulation * 100) + "%").withStyle(ChatFormatting.AQUA)));
        if (stack.getItem() instanceof net.minecraft.world.item.BlockItem block) {
            var state = block.getBlock().defaultBlockState();
            float warmth = Temperature.warmth(state);
            // only blocks that are better or worse than plain stone get a line
            if (warmth != Temperature.STONE_WARMTH && (warmth > 0 || state.is(Temperature.INSULATION_POOR)))
                lines.add(stat("Keeps warmth in", Component.literal(warmth >= Temperature.HIGH_WARMTH ? "well" : warmth >= Temperature.MEDIUM_WARMTH ? "fairly" : "poorly")
                        .withStyle(warmth >= Temperature.MEDIUM_WARMTH ? ChatFormatting.AQUA : ChatFormatting.GRAY)));
        }
        if (stack.is(Mechanical.WOLF_HOOD.get())) lines.add(Component.literal(" Grey Wolves take you for one of the pack").withStyle(ChatFormatting.GRAY));

        uses(lines, stack);

        if (lines.isEmpty()) return;
        List<Component> tooltip = event.getToolTip();
        tooltip.addAll(Math.min(1, tooltip.size()), lines);
    }

    /** Numbers and what the tool is for, for the mod's own tools. */
    private static void uses(List<Component> lines, ItemStack stack) {
        if (stack.getItem() instanceof SawItem saw) {
            lines.add(stat("Strokes per log", value(FallenLogBlock.SAW_STROKES + " (" + String.format("%.1f", FallenLogBlock.SAW_STROKES * saw.strokeTicks / 20.0F) + " s)")));
            lines.add(stat("Rough Logs per log", value(String.valueOf(TreeChopping.SAWN_LOGS))));
            use(lines, "Hold right-click on a fallen log to saw it across");
            use(lines, "Log + Saw in the crafting grid: planks (1 durability)");
        } else if (stack.getItem() instanceof FlintPickaxeItem) {
            use(lines, "Knaps rocks into stone tool heads in the crafting grid (1 durability)");
        }
        if (stack.is(ItemTags.HOES)) use(lines, "Right-click dirt or grass: tills it into farmland");
        if (stack.is(Items.SHEARS) || stack.is(BronzeTools.BRONZE_SHEARS.get())) {
            lines.add(stat("Cutting speed", value(String.format("%.1f", stack.getDestroySpeed(Blocks.OAK_LEAVES.defaultBlockState())))));
            use(lines, "Shears sheep; cuts leaves, vines and cobwebs");
        }
        if (stack.is(BronzeTools.NEEDLES)) {
            lines.add(stat("Wear per piece", value("1")));
            use(lines, "Sews hides and pelts at the Leatherworking Bench");
        }
        if (stack.is(BronzeTools.TONGS)) {
            lines.add(stat("Burns prevented", Component.literal(String.format("%.1f", BronzeTools.BURN_DAMAGE / 2) + " heart each").withStyle(GOOD)));
            lines.add(stat("Wear per casting", value("1")));
            use(lines, "In either hand: carry molten crucibles and hot metal");
        }
        if (stack.getItem() instanceof BronzeTools.ChiselItem chisel) {
            lines.add(stat("Whetstone strokes", Component.literal(chisel.whetStrokes + " per stroke (instead of 1)").withStyle(GOOD)));
            use(lines, "Right-click stone: smooth → bricks → chiseled");
            use(lines, "In the off hand at a Whetstone it knocks the flash off");
        }
        if (BronzeTools.isQuarryHammer(stack)) {
            lines.add(stat("Area", value("3×3")));
            lines.add(stat("Wear per ore", Component.literal("+" + BronzeTools.ORE_WEAR + " (ore crumbles to rock)").withStyle(BAD)));
            lines.add(stat("Ore too hard", Component.literal(BronzeTools.TOO_HARD_WEAR + " wear, nothing breaks").withStyle(BAD)));
        }
        if (com.mechanical.Smithing.isHammer(stack)) {
            lines.add(stat("Blows per bloom", value(String.valueOf(Ironworking.HITS))));
            lines.add(stat("Blows per " + Coins.COINS_PER_INGOT + " coins", value(String.valueOf(Coins.MINT_HITS))));
            lines.add(stat("Blow every", value(com.mechanical.Smithing.cooldown(stack) / 20.0 + " s")));
            lines.add(stat("Wear per blow on iron", value(String.valueOf(com.mechanical.Smithing.wear(stack)))));
            if (com.mechanical.Smithing.worksSteel(stack)) use(lines, "Hard enough to work steel");
            use(lines, "Strike a hot Iron Bloom or a Copper Ingot on a Bronze Anvil");
        }
        if (stack.is(Mechanical.FLINT_AND_STEEL.get())) {
            lines.add(stat("Catch chance", percent(1.0F)));
            use(lines, "Lights campfires, Fire Pits, kilns, furnaces, bloomeries and candles");
            use(lines, "Lasts " + com.mechanical.FlintAndSteelItem.DURABILITY / 16 + " times as long as a Fire Drill");
        } else if (stack.is(Mechanical.FIRE_DRILL.get())) {
            lines.add(stat("Catch chance", percent(Tinder.CHANCE_WITHOUT)));
            lines.add(stat("With tinder", percent(Tinder.CHANCE_WITH)));
            use(lines, "Lights campfires, Fire Pits, kilns, furnaces and bloomeries");
            use(lines, "Tinder: Leaf Litter, Bark or Mushroom Flesh on the unlit fire");
        }
        if (stack.is(Farming.FLAIL.get())) {
            lines.add(stat("Sheaf per stroke", value("1 in " + FlailItem.STROKES)));
            lines.add(stat("Per sheaf", value("1-3 Grain + 1 Straw")));
            use(lines, "Sheaves in the other hand: beat them on a hard floor");
        }
        if (stack.is(Mechanical.BROOM.get())) {
            lines.add(stat("Area", value("3×3")));
            use(lines, "Sweeps soot and leaf litter one stage thinner");
        }
        if (stack.getItem() instanceof PanItem) {
            lines.add(stat("Wash time", value(PanItem.WASH_TICKS / 20 + " s")));
            use(lines, "Scoop gravel or sand, then wash it in water");
        }
        if (stack.is(Spinning.DROP_SPINDLE.get())) {
            lines.add(stat("Wear per thread", value("1")));
            use(lines, "Spins fibre or wool into thread in the crafting grid");
        }
    }

    private static void use(List<Component> lines, String text) {
        lines.add(Component.literal(" " + text).withStyle(ChatFormatting.GRAY));
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

    /** ×3.0, ×3.15: one decimal, two when it needs them. */
    private static String multiplier(float f) {
        return "×" + String.format(java.util.Locale.ROOT, "%.2f", f).replaceAll("0$", "");
    }

    private static Component times(float f) {
        return Component.literal(String.format("%.1f×", f)).withStyle(f > 1.0F ? GOOD : VALUE);
    }

    private StatTooltips() {}
}
