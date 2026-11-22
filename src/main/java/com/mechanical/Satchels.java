package com.mechanical;

import static com.mechanical.Mechanical.ITEMS;
import static com.mechanical.Mechanical.MODID;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Bags of the end of the Iron Age (docs/living-world-plan.md F9, Steel): Tool Loops stacked onto a Leather Satchel in the
 * inventory sew {@link #TOOL_SLOTS} loops on it that hold only tools (item tag {@code mechanical:satchel_tools}: knives,
 * saws, tongs, needles, chisels, sickles, hammers ...); the Steel-Frame Pack is a 5x5 bag with the same loops built in.
 * Both are sewn at the Leatherworking Bench with Steel Plate (LeatherworkingRecipes). The contents live in the bag's
 * container component, the loops after the bag slots (equipment.PackMenu). Data: scripts/gen_p8smith.py.
 */
public final class Satchels {
    public static final int TOOL_SLOTS = 3, PACK_COLUMNS = 5;
    public static final TagKey<Item> TOOLS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "satchel_tools"));
    /** A Leather Satchel with Tool Loops sewn on. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> LOOPED = Casting.COMPONENTS.registerComponentType(
            "tool_loops", b -> b.persistent(Unit.CODEC).networkSynchronized(Unit.STREAM_CODEC));
    public static final DeferredItem<Item> TOOL_LOOPS = ITEMS.registerSimpleItem("tool_loops");
    public static final DeferredItem<LeatherSatchelItem> STEEL_FRAME_PACK = ITEMS.registerItem("steel_frame_pack",
            p -> new LeatherSatchelItem(p, PACK_COLUMNS, true), p -> p.stacksTo(1).component(DataComponents.CONTAINER, ItemContainerContents.EMPTY));

    /** Bag slots (rows = columns) of a satchel-like bag; 0 if it is none. */
    public static int columns(ItemStack bag) {
        return bag.getItem() instanceof LeatherSatchelItem s ? s.columns : 0;
    }

    /** Tool loops on it: built in (pack) or sewn on (satchel). */
    public static int toolSlots(ItemStack bag) {
        return bag.getItem() instanceof LeatherSatchelItem s && (s.builtInLoops || bag.has(LOOPED.get())) ? TOOL_SLOTS : 0;
    }

    public static boolean isTool(ItemStack stack) {
        return stack.is(TOOLS);
    }

    public static void init() {}

    private Satchels() {}
}
