package com.mechanical.tools;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.google.common.base.Suppliers;
import com.mechanical.Bronze;
import com.mechanical.BronzeTools;
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
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * The parts a dynamic tool was built from and what they do to its stats (docs/superpowers/specs/2026-11-26-dynamic-tools-design.md).
 * Stats are relative to the recipe's default parts, so a tool built with them keeps exactly the item's own stats.
 * Leather in the rod slot is the grip of a sword, knife or tongs: a rod with factors 1.0, so the rod-table rods replace it as grips.
 * Bows (string, grip, stave in the head, binding and rod slots) and fishing rods (hook, line, rod) use their own tables ({@link Kind}).
 */
public final class ToolParts {
    /** What a Tool Station recipe makes, and so which table each slot reads (none: the slot takes only the recipe's own item). */
    public enum Kind {
        TOOL, BOW, FISHING_ROD, FIXED;

        private Map<Item, Part> table(int slot) {
            return switch (this) {
                case TOOL -> slot == 1 ? BINDINGS.get() : slot == 2 ? RODS.get() : null;
                case BOW -> slot == 0 ? STRINGS.get() : slot == 1 ? GRIPS.get() : STAVES.get();
                case FISHING_ROD -> slot == 1 ? LINES.get() : slot == 2 ? FISHING_RODS.get() : null;
                case FIXED -> null;
            };
        }
    }

    /** head: the bowstring, the one head that is a choice; air on everything else (there the head is the item). */
    public record Parts(Item binding, Item rod, int smithing, Item head) {
        public static final Codec<Parts> CODEC = RecordCodecBuilder.create(i -> i.group(
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("binding").forGetter(Parts::binding),
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("rod").forGetter(Parts::rod),
                Codec.INT.fieldOf("smithing").forGetter(Parts::smithing),
                BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("head", Items.AIR).forGetter(Parts::head)).apply(i, Parts::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Parts> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.registry(Registries.ITEM), Parts::binding, ByteBufCodecs.registry(Registries.ITEM), Parts::rod,
                ByteBufCodecs.VAR_INT, Parts::smithing, ByteBufCodecs.registry(Registries.ITEM), Parts::head, Parts::new);

        public Parts(Item binding, Item rod, int smithing) {
            this(binding, rod, smithing, Items.AIR);
        }

        public Item slot(int slot) {
            return slot == 0 ? head : slot == 1 ? binding : rod;
        }
    }

    /**
     * One row of a part table: the highest-tier head it may go on is its own tier and up. Tools use speed (mining) and damage (added);
     * bows speed (draw: the draw time is divided by it) and power (the arrow's speed, so its damage); fishing rods lure (ticks sooner).
     */
    public record Part(int tier, float durability, float speed, float damage, float power, int lure) {
        public Part(int tier, float durability, float speed, float damage) {
            this(tier, durability, speed, damage, 1, 0);
        }
    }

    /** A bow's or fishing rod's parts against the recipe's own: arrow speed and draw time as factors, bites this many ticks sooner. */
    public record Ranged(float power, float draw, int lure) {
        public static final Ranged NONE = new Ranged(1, 1, 0);
        public static final Codec<Ranged> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.FLOAT.fieldOf("power").forGetter(Ranged::power), Codec.FLOAT.fieldOf("draw").forGetter(Ranged::draw),
                Codec.INT.fieldOf("lure").forGetter(Ranged::lure)).apply(i, Ranged::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Ranged> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.FLOAT, Ranged::power, ByteBufCodecs.FLOAT, Ranged::draw, ByteBufCodecs.VAR_INT, Ranged::lure, Ranged::new);

        /**
         * Ticks to a full draw for a bow whose own full draw takes {@code base}.
         * shortcut: the pulling frames (use_duration scale in the item model) keep the bow's own draw time; add a client range property if the factors grow past ~10 %.
         */
        public int drawTicks(int base) {
            return Math.max(1, Math.round(base * draw));
        }

        /** Ticks held on this bow as ticks on vanilla's 20-tick power curve (BowItem.getPowerForTime is static). */
        public int onVanillaCurve(int held, int base) {
            return held * net.minecraft.world.item.BowItem.MAX_DRAW_DURATION / drawTicks(base);
        }
    }

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Parts>> TOOL_PARTS = Casting.COMPONENTS.registerComponentType(
            "tool_parts", b -> b.persistent(Parts.CODEC).networkSynchronized(Parts.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Ranged>> RANGED = Casting.COMPONENTS.registerComponentType(
            "ranged_parts", b -> b.persistent(Ranged.CODEC).networkSynchronized(Ranged.STREAM_CODEC));

    /** The stack's bow / fishing-rod part factors; none for one never built at the Tool Station. */
    public static Ranged ranged(ItemStack stack) {
        return stack.getOrDefault(RANGED.get(), Ranged.NONE);
    }

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
    // bows (spec addendum; Claude): a light string draws quick and shoots soft, sinew is the strong one
    private static final Supplier<Map<Item, Part>> STRINGS = Suppliers.memoize(() -> Map.of(
            Mechanical.TWINE.get(), new Part(0, 0.7F, 1.1F, 0, 0.85F, 0),
            Mechanical.CORDAGE.get(), new Part(0, 1.0F, 1.0F, 0, 1.0F, 0),
            Items.STRING, new Part(0, 0.9F, 1.05F, 0, 1.05F, 0),
            com.mechanical.wildlife.Wildlife.SINEW.get(), new Part(0, 1.2F, 0.95F, 0, 1.1F, 0)));
    /** The binding table's wraps that make a grip; Leather's quicker hand (speed 1.03) is a quicker draw. */
    private static final Supplier<Map<Item, Part>> GRIPS = Suppliers.memoize(() -> Map.of(
            Mechanical.TWINE.get(), BINDINGS.get().get(Mechanical.TWINE.get()),
            Mechanical.CORDAGE.get(), BINDINGS.get().get(Mechanical.CORDAGE.get()),
            Items.LEATHER, BINDINGS.get().get(Items.LEATHER),
            com.mechanical.wildlife.Wildlife.SINEW.get(), BINDINGS.get().get(com.mechanical.wildlife.Wildlife.SINEW.get())));
    /** A thin stick draws quick and shoots weak; the Yew Stave makes the Yew Longbow and nothing else ({@link #fits}). */
    private static final Supplier<Map<Item, Part>> STAVES = Suppliers.memoize(() -> Map.of(
            Items.STICK, new Part(0, 0.75F, 1.1F, 0, 0.85F, 0),
            Mechanical.TOOL_ROD.get(), new Part(0, 1.0F, 1.0F, 0, 1.0F, 0),
            BronzeTools.YEW_STAVE.get(), new Part(0, 1.0F, 1.0F, 0, 1.0F, 0)));
    // fishing rods: a thin line the fish don't see brings bites sooner (String 2 s, Sinew 1 s); the rod only lasts
    private static final Supplier<Map<Item, Part>> LINES = Suppliers.memoize(() -> Map.of(
            Mechanical.TWINE.get(), new Part(0, 0.85F, 1, 0, 1, 0),
            Mechanical.CORDAGE.get(), new Part(0, 1.0F, 1, 0, 1, 0),
            Items.STRING, new Part(0, 0.9F, 1, 0, 1, 40),
            com.mechanical.wildlife.Wildlife.SINEW.get(), new Part(0, 1.1F, 1, 0, 1, 20)));
    private static final Supplier<Map<Item, Part>> FISHING_RODS = Suppliers.memoize(() -> Map.of(
            Items.STICK, new Part(0, 0.8F, 1, 0),
            Mechanical.TOOL_ROD.get(), new Part(0, 1.0F, 1, 0),
            Items.BAMBOO, new Part(0, 1.15F, 1, 0)));

    public static Part rod(Item item) {
        return RODS.get().get(item);
    }

    public static Part binding(Item item) {
        return BINDINGS.get().get(item);
    }

    /** A part in this slot of this kind of recipe; null when the slot has no table or the item isn't in it. */
    public static Part part(Kind kind, int slot, Item item) {
        Map<Item, Part> table = kind.table(slot);
        return table == null ? null : table.get(item);
    }

    /** Whether the slot is a choice (false: it takes only the recipe's own item). */
    public static boolean variable(Kind kind, int slot) {
        return kind.table(slot) != null;
    }

    /** What this slot takes: the table's parts no higher than the head's tier that fit beside {@code own} (the recipe's part); else just own. */
    public static java.util.List<Item> options(Kind kind, int slot, Item head, Item own) {
        Map<Item, Part> table = kind.table(slot);
        if (table == null) return java.util.List.of(own);
        return ordered(table, head).stream().filter(i -> fits(kind, slot, i, own)).toList();
    }

    /** Every table binding / rod no higher than the head's tier. */
    public static java.util.List<Item> bindings(Item head) {
        return ordered(BINDINGS.get(), head);
    }

    /** Fixed order (tier, then registry id) so auto-fill never depends on Map.of's iteration order. */
    private static java.util.List<Item> ordered(Map<Item, Part> table, Item head) {
        return table.entrySet().stream().filter(e -> e.getValue().tier() <= tier(head))
                .sorted(java.util.Comparator.<Map.Entry<Item, Part>>comparingInt(e -> e.getValue().tier())
                        .thenComparing(e -> BuiltInRegistries.ITEM.getKey(e.getKey()).toString()))
                .map(Map.Entry::getKey).toList();
    }

    /** The rods for this head: Leather only where the recipe's own rod is the Leather grip (swords, knives, tongs), never on a haft. */
    public static java.util.List<Item> rods(Item head, Item defaultRod) {
        return options(Kind.TOOL, 2, head, defaultRod);
    }

    /** Leather is a rod only as a grip; a Yew Stave goes only on the Yew Longbow, and the Yew Longbow takes only one. */
    private static boolean fits(Kind kind, int slot, Item part, Item own) {
        if (slot != 2) return true;
        if (kind == Kind.TOOL) return part != Items.LEATHER || own == Items.LEATHER;
        if (kind == Kind.BOW) return (part == BronzeTools.YEW_STAVE.get()) == (own == BronzeTools.YEW_STAVE.get());
        return true;
    }

    /** Head tier from its material, the registry path's first word: flint, stone and tusk 0, copper 1, bronze 2, iron 3, steel 4. */
    public static int tier(Item head) {
        String path = BuiltInRegistries.ITEM.getKey(head).getPath();
        return path.startsWith("steel") ? 4 : path.startsWith("iron") ? 3 : path.startsWith("bronze") ? 2 : path.startsWith("copper") ? 1 : 0;
    }

    /** Known parts, neither above the head's tier, and a Leather rod only in place of a Leather grip ({@link #rods}). */
    public static boolean allowed(Item head, Item defaultRod, Item binding, Item rod) {
        return bindings(head).contains(binding) && rods(head, defaultRod).contains(rod);
    }

    /** A tool's apply: the head is the item, binding and rod from the tool tables. */
    public static void apply(ItemStack tool, Item defaultBinding, Item defaultRod, Parts parts) {
        apply(tool, Kind.TOOL, List.of(Items.AIR, defaultBinding, defaultRod), parts);
    }

    /**
     * Writes the parts and the stats they give onto the tool, recomputed from the item's own stats so it can be applied again.
     * {@code defaults}: the recipe's head, binding and rod. A Poor Bronze tool keeps its 70 % (Bronze.markPoor on it afterwards is a no-op),
     * rounded as markPoor rounds. Throws IllegalArgumentException for a part outside the tables: callers check the slot's options first.
     */
    public static void apply(ItemStack tool, Kind kind, List<Item> defaults, Parts parts) {
        if (!variable(kind, 0)) parts = new Parts(parts.binding(), parts.rod(), parts.smithing());
        float dDur = 1, cDur = 1, dSpeed = 1, cSpeed = 1, power = 1, damage = 0;
        int lure = 0;
        for (int slot = 0; slot < 3; slot++) {
            if (!variable(kind, slot)) continue;
            Part d = known(part(kind, slot, defaults.get(slot)), defaults.get(slot)), c = known(part(kind, slot, parts.slot(slot)), parts.slot(slot));
            dDur *= d.durability();
            cDur *= c.durability();
            dSpeed *= d.speed();
            cSpeed *= c.speed();
            power *= c.power() / d.power();
            damage += c.damage() - d.damage();
            lure += c.lure() - d.lure();
        }
        ItemStack fresh = new ItemStack(tool.getItem());
        tool.set(TOOL_PARTS.get(), parts);
        // the item model picks its rod, binding (and bowstring) layers by these (scripts/gen_tool_parts.py)
        List<String> names = new java.util.ArrayList<>(List.of(BuiltInRegistries.ITEM.getKey(parts.rod()).getPath(),
                BuiltInRegistries.ITEM.getKey(parts.binding()).getPath()));
        if (variable(kind, 0)) names.add(BuiltInRegistries.ITEM.getKey(parts.head()).getPath());
        tool.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.copyOf(names), List.of()));
        if (fresh.isDamageableItem()) {
            int durability = Math.round(fresh.getMaxDamage() / dDur * cDur * (1 + SMITHING_BONUS_PER_LEVEL * parts.smithing()));
            if (Bronze.isPoor(tool)) durability = Math.round(durability * Bronze.POOR_DURABILITY);
            tool.set(DataComponents.MAX_DAMAGE, Math.max(1, durability));
        }
        if (kind == Kind.BOW) tool.set(RANGED.get(), new Ranged(power, dSpeed / cSpeed, 0));
        if (kind == Kind.FISHING_ROD) tool.set(RANGED.get(), new Ranged(1, 1, lure));
        if (kind != Kind.TOOL) return;
        Tool miner = fresh.get(DataComponents.TOOL);
        if (miner != null) {
            float factor = cSpeed / dSpeed;
            tool.set(DataComponents.TOOL, new Tool(miner.rules().stream()
                    .map(rule -> new Tool.Rule(rule.blocks(), rule.speed().map(s -> s * factor), rule.correctForDrops())).toList(),
                    miner.defaultMiningSpeed(), miner.damagePerBlock(), miner.canDestroyBlocksInCreative())); // default = bare-hand speed, left alone
        }
        ItemAttributeModifiers attributes = fresh.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (attributes != null) {
            // only items with a base_attack_damage modifier get the rod's bonus; tongs and chisels without one get none, deliberately
            double extra = damage;
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
