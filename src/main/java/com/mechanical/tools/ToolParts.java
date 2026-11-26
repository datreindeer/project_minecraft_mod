package com.mechanical.tools;

import java.util.Map;
import java.util.function.Supplier;

import com.google.common.base.Suppliers;
import com.mechanical.Bronze;
import com.mechanical.Casting;
import com.mechanical.Kettle;
import com.mechanical.Mechanical;
import com.mechanical.Smithing;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * The parts a dynamic tool was built from and what they do to its stats (docs/superpowers/specs/2026-11-26-dynamic-tools-design.md).
 * Stats are relative to the recipe's default parts, so a tool built with them keeps exactly the item's own stats.
 * Leather in the rod slot is the grip of a sword, knife or tongs: a rod with factors 1.0, so the rod-table rods replace it as grips.
 */
public final class ToolParts {
    public record Parts(Item binding, Item rod, int smithing) {
        public static final Codec<Parts> CODEC = RecordCodecBuilder.create(i -> i.group(
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("binding").forGetter(Parts::binding),
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("rod").forGetter(Parts::rod),
                Codec.INT.fieldOf("smithing").forGetter(Parts::smithing)).apply(i, Parts::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Parts> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.registry(Registries.ITEM), Parts::binding, ByteBufCodecs.registry(Registries.ITEM), Parts::rod,
                ByteBufCodecs.VAR_INT, Parts::smithing, Parts::new);
    }

    /** One row of the rod or binding table: the highest-tier head it may go on is its own tier and up. */
    public record Part(int tier, float durability, float speed, float damage) {}

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Parts>> TOOL_PARTS = Casting.COMPONENTS.registerComponentType(
            "tool_parts", b -> b.persistent(Parts.CODEC).networkSynchronized(Parts.STREAM_CODEC));

    public static final float SMITHING_BONUS_PER_LEVEL = 0.005F;

    private static final Supplier<Map<Item, Part>> RODS = Suppliers.memoize(() -> Map.of(
            Items.STICK, new Part(0, 0.8F, 1.0F, 0),
            Mechanical.TOOL_ROD.get(), new Part(0, 1.0F, 1.0F, 0),
            Items.LEATHER, new Part(0, 1.0F, 1.0F, 0),
            Smithing.BONE_TOOL_ROD.get(), new Part(1, 1.1F, 1.0F, 0),
            Smithing.COPPER_TOOL_ROD.get(), new Part(1, 1.2F, 0.98F, 0),
            Smithing.BRONZE_TOOL_ROD.get(), new Part(2, 1.35F, 0.98F, 0.5F),
            Smithing.IRON_TOOL_ROD.get(), new Part(3, 1.5F, 0.96F, 0.5F),
            Smithing.STEEL_TOOL_ROD.get(), new Part(4, 1.75F, 0.96F, 1.0F)));
    private static final Supplier<Map<Item, Part>> BINDINGS = Suppliers.memoize(() -> Map.of(
            Mechanical.TWINE.get(), new Part(0, 0.85F, 1.0F, 0),
            Mechanical.CORDAGE.get(), new Part(0, 1.0F, 1.0F, 0),
            Kettle.HIDE_GLUE.get(), new Part(0, 1.05F, 1.0F, 0),
            Items.LEATHER, new Part(0, 1.1F, 1.03F, 0),
            com.mechanical.wildlife.Wildlife.SINEW.get(), new Part(0, 1.1F, 1.0F, 0),
            Bronze.BRONZE_PLATE.get(), new Part(2, 1.25F, 0.95F, 0),
            Smithing.IRON_PLATE.get(), new Part(3, 1.25F, 0.95F, 0),
            Smithing.STEEL_PLATE.get(), new Part(4, 1.25F, 0.95F, 0),
            Smithing.IRON_TOOL_ROD.get(), new Part(3, 1.3F, 1.0F, 0)));

    public static Part rod(Item item) {
        return RODS.get().get(item);
    }

    public static Part binding(Item item) {
        return BINDINGS.get().get(item);
    }

    /** Every table binding / rod no higher than the head's tier. */
    public static java.util.List<Item> bindings(Item head) {
        return BINDINGS.get().entrySet().stream().filter(e -> e.getValue().tier() <= tier(head)).map(Map.Entry::getKey).toList();
    }

    public static java.util.List<Item> rods(Item head) {
        return RODS.get().entrySet().stream().filter(e -> e.getValue().tier() <= tier(head)).map(Map.Entry::getKey).toList();
    }

    /** Head tier from its material, the registry path's first word: flint, stone and tusk 0, copper 1, bronze 2, iron 3, steel 4. */
    public static int tier(Item head) {
        String path = BuiltInRegistries.ITEM.getKey(head).getPath();
        return path.startsWith("steel") ? 4 : path.startsWith("iron") ? 3 : path.startsWith("bronze") ? 2 : path.startsWith("copper") ? 1 : 0;
    }

    /** Known parts, neither above the head's tier. */
    public static boolean allowed(Item head, Item binding, Item rod) {
        Part b = binding(binding), r = rod(rod);
        return b != null && r != null && b.tier() <= tier(head) && r.tier() <= tier(head);
    }

    /**
     * Writes the parts and the stats they give onto the tool, recomputed from the item's own stats so it can be applied again.
     * A Poor Bronze tool keeps its 70 % (Bronze.markPoor on it afterwards is a no-op), rounded as markPoor rounds.
     * Throws IllegalArgumentException for a part outside the tables: callers check {@link #allowed} first.
     */
    public static void apply(ItemStack tool, Item defaultBinding, Item defaultRod, Parts parts) {
        Part db = known(binding(defaultBinding), defaultBinding), dr = known(rod(defaultRod), defaultRod),
                b = known(binding(parts.binding()), parts.binding()), r = known(rod(parts.rod()), parts.rod());
        ItemStack fresh = new ItemStack(tool.getItem());
        tool.set(TOOL_PARTS.get(), parts);
        if (fresh.isDamageableItem()) {
            int durability = Math.round(fresh.getMaxDamage() / (db.durability() * dr.durability()) * (b.durability() * r.durability())
                    * (1 + SMITHING_BONUS_PER_LEVEL * parts.smithing()));
            if (Bronze.isPoor(tool)) durability = Math.round(durability * Bronze.POOR_DURABILITY);
            tool.set(DataComponents.MAX_DAMAGE, Math.max(1, durability));
        }
        Tool miner = fresh.get(DataComponents.TOOL);
        if (miner != null) {
            float factor = (b.speed() * r.speed()) / (db.speed() * dr.speed());
            tool.set(DataComponents.TOOL, new Tool(miner.rules().stream()
                    .map(rule -> new Tool.Rule(rule.blocks(), rule.speed().map(s -> s * factor), rule.correctForDrops())).toList(),
                    miner.defaultMiningSpeed(), miner.damagePerBlock(), miner.canDestroyBlocksInCreative())); // default = bare-hand speed, left alone
        }
        ItemAttributeModifiers attributes = fresh.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (attributes != null) {
            // only items with a base_attack_damage modifier get the rod's bonus; tongs and chisels without one get none, deliberately
            double extra = r.damage() - dr.damage();
            tool.set(DataComponents.ATTRIBUTE_MODIFIERS, new ItemAttributeModifiers(attributes.modifiers().stream()
                    .map(e -> !e.matches(Attributes.ATTACK_DAMAGE, Item.BASE_ATTACK_DAMAGE_ID) ? e : new ItemAttributeModifiers.Entry(e.attribute(),
                            new AttributeModifier(e.modifier().id(), e.modifier().amount() + extra, e.modifier().operation()), e.slot(), e.display()))
                    .toList()));
        }
    }

    private static Part known(Part part, Item item) {
        if (part == null) throw new IllegalArgumentException("not a tool part in this slot: " + BuiltInRegistries.ITEM.getKey(item));
        return part;
    }

    public static void init() {}

    private ToolParts() {}
}
