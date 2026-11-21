package com.mechanical.equipment;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;

import com.mechanical.Mechanical;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingGetProjectileEvent;

/**
 * The worn {@link QuiverItem} (Hip slot):
 * <ul>
 * <li>Bows and crossbows shoot from it before the inventory (an arrow held in the other hand still comes first, as in
 * vanilla). The weapon gets a copy of the quiver's first fitting stack; what the shot uses of it is then taken from the
 * quiver (ProjectileWeaponItemMixin -> {@link #afterUseAmmo}).</li>
 * <li>Arrows picked up (lying items and arrows stuck in the ground) go into it first, the rest into the inventory
 * (ItemEntityMixin, AbstractArrowMixin -> {@link #pickUp}).</li>
 * </ul>
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class Quiver {
    public static final int SIZE = 4;
    /** What fits: anything tagged #minecraft:arrows (vanilla and the mod's arrows), and crossbow bolts. */
    public static final Predicate<ItemStack> ARROWS = s -> s.is(ItemTags.ARROWS) || s.is(com.mechanical.Smithing.BOLTS);

    /** Arrow stacks handed to a weapon from a quiver, with where they came from and their size at the time. */
    private static final Map<ItemStack, Drawn> DRAWN = Collections.synchronizedMap(new WeakHashMap<>());

    private record Drawn(Player player, ItemStack quiver, ItemStack kind, int count) {}

    /** The quiver this player wears, or EMPTY. */
    public static ItemStack worn(Player player) {
        ItemStack stack = Equipment.get(player, Accessory.HIP);
        return stack.getItem() instanceof QuiverItem ? stack : ItemStack.EMPTY;
    }

    public static NonNullList<ItemStack> contents(ItemStack quiver) {
        NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        quiver.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
        return items;
    }

    static void setContents(ItemStack quiver, List<ItemStack> items) {
        quiver.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
    }

    /** Slots of the quiver holding arrows (0..4), for the worn model. */
    public static int filledSlots(ItemStack quiver) {
        int n = 0;
        for (ItemStack s : contents(quiver)) if (!s.isEmpty()) n++;
        return n;
    }

    @SubscribeEvent
    static void onGetProjectile(LivingGetProjectileEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack weaponStack = event.getProjectileWeaponItemStack();
        if (!(weaponStack.getItem() instanceof ProjectileWeaponItem weapon)) return;
        // an arrow in the off hand (or main hand) is picked on purpose, as in vanilla
        if (!ProjectileWeaponItem.getHeldProjectile(player, weapon.getSupportedHeldProjectiles(weaponStack)).isEmpty()) return;
        ItemStack quiver = worn(player);
        if (quiver.isEmpty()) return;
        Predicate<ItemStack> supported = weapon.getAllSupportedProjectiles(weaponStack);
        for (ItemStack arrows : contents(quiver)) {
            if (arrows.isEmpty() || !supported.test(arrows)) continue;
            ItemStack ammo = arrows.copy();
            DRAWN.put(ammo, new Drawn(player, quiver, arrows.copyWithCount(1), arrows.getCount()));
            event.setProjectileItemStack(ammo);
            return;
        }
    }

    /** After ProjectileWeaponItem#useAmmo: what the shot took from a stack drawn from a quiver comes out of that quiver. */
    public static void afterUseAmmo(ItemStack projectile) {
        Drawn drawn = DRAWN.get(projectile);
        if (drawn == null) return;
        int used = drawn.count() - projectile.getCount();
        if (used <= 0) return;
        DRAWN.put(projectile, new Drawn(drawn.player(), drawn.quiver(), drawn.kind(), projectile.getCount()));
        take(drawn.quiver(), drawn.kind(), used);
        Equipment.of(drawn.player()).setChanged();
    }

    /** Takes {@code amount} arrows like {@code kind} out of the quiver (from whichever of its slots hold them). */
    static void take(ItemStack quiver, ItemStack kind, int amount) {
        NonNullList<ItemStack> items = contents(quiver);
        for (int i = 0; i < SIZE && amount > 0; i++) {
            ItemStack s = items.get(i);
            if (!ItemStack.isSameItemSameComponents(s, kind)) continue;
            int n = Math.min(amount, s.getCount());
            s.shrink(n);
            amount -= n;
            if (s.isEmpty()) items.set(i, ItemStack.EMPTY);
        }
        setContents(quiver, items);
    }

    /**
     * Puts as much of {@code stack} as fits into the worn quiver (topping up stacks of the same arrow first), shrinking
     * it. True if anything went in.
     */
    public static boolean insert(Player player, ItemStack stack) {
        if (stack.isEmpty() || !ARROWS.test(stack)) return false;
        ItemStack quiver = worn(player);
        if (quiver.isEmpty()) return false;
        NonNullList<ItemStack> items = contents(quiver);
        int before = stack.getCount();
        for (int pass = 0; pass < 2 && !stack.isEmpty(); pass++) {
            for (int i = 0; i < SIZE && !stack.isEmpty(); i++) {
                ItemStack s = items.get(i);
                if (pass == 0 && !s.isEmpty() && ItemStack.isSameItemSameComponents(s, stack)) {
                    int n = Math.min(stack.getCount(), s.getMaxStackSize() - s.getCount());
                    s.grow(n);
                    stack.shrink(n);
                } else if (pass == 1 && s.isEmpty()) {
                    items.set(i, stack.split(stack.getMaxStackSize()));
                }
            }
        }
        if (stack.getCount() == before) return false;
        setContents(quiver, items);
        Equipment.of(player).setChanged();
        return true;
    }

    /**
     * Wraps the {@code player.getInventory().add(stack)} of a pickup (ItemEntityMixin, AbstractArrowMixin): arrows go into
     * the worn quiver first, what doesn't fit into the inventory. The pickup counts as done if either took some.
     */
    public static boolean pickUp(Player player, ItemStack stack, Predicate<ItemStack> addToInventory) {
        boolean quivered = insert(player, stack);
        if (stack.isEmpty()) return true;
        return addToInventory.test(stack) || quivered;
    }

    private Quiver() {}
}
