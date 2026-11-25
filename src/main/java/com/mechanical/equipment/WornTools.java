package com.mechanical.equipment;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import com.mechanical.LeatherPouchItem;
import com.mechanical.LeatherSatchelItem;
import com.mechanical.Satchels;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * Tools used straight out of a worn bag: right-clicking with an EMPTY main hand uses the first fitting tool in what the
 * player wears, as if it were in the hand, and its wear goes to that stored stack. Every empty-hand tool use routes
 * through {@link #forEmptyHand}, so the rule is the same everywhere:
 * <ul>
 * <li>only the main hand, only while it is empty, and only when the off hand doesn't hold a fitting tool itself (that
 * one is used, as in vanilla);</li>
 * <li>bags in a fixed order: the Back slot (Leather Satchel, Steel-Frame Pack), then the first Hip slot, then the second
 * (Leather Pouches; a quiver holds only arrows); inside a bag slot by slot, the grid first, then its tool loops;</li>
 * <li>the first match is used.</li>
 * </ul>
 * Works on both sides (bag contents sync with the worn items), so the client predicts the swing. Uses that take holding
 * the button (butchering, sawing) count how long the click has been held on the same thing with {@link #held}.
 */
public final class WornTools {
    /** A tool found in a worn bag: the bag, the tool's slot in it, and the tool (a copy; {@link #hurt} writes it back). */
    public record Found(ItemStack bag, int slot, ItemStack tool) {
        /** Wears the stored tool by {@code amount} (server only) and writes it back into the bag; a broken one is gone. */
        public void hurt(int amount, Player player) {
            if (!(player instanceof ServerPlayer sp) || !(player.level() instanceof ServerLevel level) || !tool.isDamageableItem()) return;
            ItemStack worn = tool.copy();
            worn.hurtAndBreak(amount, level, sp, broken -> level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ITEM_BREAK.value(), player.getSoundSource(), 0.8F, 0.8F + level.getRandom().nextFloat() * 0.4F));
            NonNullList<ItemStack> items = contents(bag);
            if (slot >= items.size() || !ItemStack.isSameItem(items.get(slot), tool)) return; // the bag changed meanwhile
            items.set(slot, worn.isEmpty() ? ItemStack.EMPTY : worn);
            bag.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
            Equipment.of(player).setChanged();
        }
    }

    /** The accessory slots searched, in order. */
    private static final Accessory[] BAG_SLOTS = {Accessory.BACK, Accessory.HIP, Accessory.HIP_2};

    /**
     * The tool to use for a click with {@code hand}: the first {@code fits} tool in a worn bag, when the click is with the
     * empty main hand and the off hand holds no fitting tool; else null.
     */
    public static @Nullable Found forEmptyHand(Player player, InteractionHand hand, Predicate<ItemStack> fits) {
        if (hand != InteractionHand.MAIN_HAND || !player.getMainHandItem().isEmpty() || player.isSpectator() || fits.test(player.getOffhandItem())) return null;
        return find(player, fits);
    }

    /** The first {@code fits} tool in the player's worn bags (in the order above), or null. */
    public static @Nullable Found find(Player player, Predicate<ItemStack> fits) {
        for (Accessory slot : BAG_SLOTS) {
            ItemStack bag = Equipment.get(player, slot);
            if (size(bag) == 0) continue;
            NonNullList<ItemStack> items = contents(bag);
            for (int i = 0; i < items.size(); i++) {
                ItemStack s = items.get(i);
                if (!s.isEmpty() && fits.test(s)) return new Found(bag, i, s);
            }
        }
        return null;
    }

    /** Slots of a bag that can hold tools: a satchel's grid and loops, a pouch's 4; 0 for anything else. */
    private static int size(ItemStack bag) {
        if (bag.getItem() instanceof LeatherSatchelItem) return Satchels.columns(bag) * Satchels.columns(bag) + Satchels.toolSlots(bag);
        if (bag.getItem() instanceof LeatherPouchItem) return Quiver.SIZE;
        return 0;
    }

    private static NonNullList<ItemStack> contents(ItemStack bag) {
        NonNullList<ItemStack> items = NonNullList.withSize(size(bag), ItemStack.EMPTY);
        bag.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
        return items;
    }

    // ---------------------------------------------------------------- held clicks

    /** How long a player has been holding the use button on the same target, from repeated clicks (the client repeats every 4 ticks). */
    public record Hold(int total, int delta) {}

    private record Holding(Object target, long start, long last) {}

    private static final Map<Player, Holding> HOLDING = new WeakHashMap<>();
    /** A gap longer than this between two clicks (the client repeats every 4 ticks) starts over. */
    private static final int MAX_GAP = 6;

    /**
     * Server: a click on {@code target} at game time {@code now}. Returns how many ticks the button has been held on it
     * ({@code total}, 0 on the first click) and how many since the last click ({@code delta}).
     */
    public static Hold held(Player player, Object target, long now) {
        Holding h = HOLDING.get(player);
        if (h == null || !h.target().equals(target) || now - h.last() > MAX_GAP || now < h.last()) {
            HOLDING.put(player, new Holding(target, now, now));
            return new Hold(0, 0);
        }
        HOLDING.put(player, new Holding(target, h.start(), now));
        return new Hold((int) (now - h.start()), (int) (now - h.last()));
    }

    private WornTools() {}
}
