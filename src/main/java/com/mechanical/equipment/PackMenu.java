package com.mechanical.equipment;

import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.mechanical.BarkBasketMenu;
import com.mechanical.LeatherSatchelItem;
import com.mechanical.Mechanical;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;

/**
 * The Leather Satchel's 3x3 and the Quiver's 2x2 side by side (or whichever of them is there), over the player
 * inventory. Opened with B for what is worn in the Back and Hip slots ({@link #openWorn}), or by right-clicking one in
 * hand ({@link #openHeld}; that inventory slot is locked while open). The contents are read from and written to the
 * items themselves ({@link ItemContents}). Layout shared with client.PackScreen.
 */
public class PackMenu extends AbstractContainerMenu {
    public static final int SATCHEL_SIZE = BarkBasketMenu.SIZE;
    public static final Supplier<MenuType<PackMenu>> TYPE = Mechanical.MENUS.register("pack", () -> IMenuTypeExtension.create(PackMenu::client));

    static void init() {}

    /** Where a bag comes from: worn in an accessory slot, or at an inventory index (40 = off hand). */
    private record Source(ItemStack stack, @Nullable Accessory worn, int inventorySlot) {}

    public final boolean hasSatchel, hasQuiver;
    public final Component satchelName, quiverName;
    private final int quiverStart, invStart, invEnd;
    private final @Nullable Source satchel, quiver;
    private final int lockedSlot;

    /** Client side: the layout comes in the open packet. */
    private static PackMenu client(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        boolean hasSatchel = buf.readBoolean(), hasQuiver = buf.readBoolean();
        Component satchelName = ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buf);
        Component quiverName = ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buf);
        return new PackMenu(id, inventory, hasSatchel ? new SimpleContainer(SATCHEL_SIZE) : null, hasQuiver ? new SimpleContainer(Quiver.SIZE) : null,
                null, null, satchelName, quiverName);
    }

    private PackMenu(int id, Inventory inventory, @Nullable Container satchelContents, @Nullable Container quiverContents,
            @Nullable Source satchel, @Nullable Source quiver, Component satchelName, Component quiverName) {
        super(TYPE.get(), id);
        this.hasSatchel = satchelContents != null;
        this.hasQuiver = quiverContents != null;
        this.satchel = satchel;
        this.quiver = quiver;
        this.satchelName = satchelName;
        this.quiverName = quiverName;
        int locked = -1;
        if (satchel != null && satchel.worn() == null) locked = satchel.inventorySlot();
        if (quiver != null && quiver.worn() == null) locked = quiver.inventorySlot();
        this.lockedSlot = locked;

        if (satchelContents != null) {
            int left = hasQuiver ? 30 : 62;
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) addSlot(bagSlot(satchelContents, col + row * 3, left + col * 18, 17 + row * 18, BarkBasketMenu.SATCHEL));
            }
        }
        quiverStart = slots.size();
        if (quiverContents != null) {
            int left = hasSatchel ? 116 : 71;
            for (int row = 0; row < 2; row++) {
                for (int col = 0; col < 2; col++) addSlot(bagSlot(quiverContents, col + row * 2, left + col * 18, 26 + row * 18, Quiver.ARROWS));
            }
        }
        invStart = slots.size();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(playerSlot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(playerSlot(inventory, col, 8 + col * 18, 142));
        invEnd = slots.size();
    }

    private static Slot bagSlot(Container container, int index, int x, int y, Predicate<ItemStack> allowed) {
        return new Slot(container, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return allowed.test(stack);
            }
        };
    }

    private Slot playerSlot(Inventory inventory, int index, int x, int y) {
        return new Slot(inventory, index, x, y) {
            @Override
            public boolean mayPickup(Player player) {
                return index != lockedSlot;
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return index != lockedSlot;
            }
        };
    }

    // ---------------------------------------------------------------- opening (server)

    /** B: the satchel worn on the back and the quiver at the hip. Says so if neither is worn. */
    public static void openWorn(Player player) {
        ItemStack back = Equipment.get(player, Accessory.BACK), hip = Equipment.get(player, Accessory.HIP);
        Source satchel = back.getItem() instanceof LeatherSatchelItem ? new Source(back, Accessory.BACK, -1) : null;
        Source quiver = hip.getItem() instanceof QuiverItem ? new Source(hip, Accessory.HIP, -1) : null;
        if (satchel == null && quiver == null) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.nothing_to_open"));
            return;
        }
        open(player, satchel, quiver);
    }

    /** Right-click with a satchel or quiver in hand. */
    public static void openHeld(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        int slot = hand == InteractionHand.MAIN_HAND ? player.getInventory().getSelectedSlot() : Inventory.SLOT_OFFHAND;
        Source source = new Source(stack, null, slot);
        if (stack.getItem() instanceof QuiverItem) open(player, null, source);
        else if (stack.getItem() instanceof LeatherSatchelItem) open(player, source, null);
    }

    private static void open(Player player, @Nullable Source satchel, @Nullable Source quiver) {
        Component satchelName = satchel != null ? satchel.stack().getHoverName() : Component.empty();
        Component quiverName = quiver != null ? quiver.stack().getHoverName() : Component.empty();
        Component title = satchel != null ? satchelName : quiverName;
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new PackMenu(id, inventory,
                satchel != null ? new ItemContents(satchel.stack(), SATCHEL_SIZE, BarkBasketMenu.SATCHEL) : null,
                quiver != null ? new ItemContents(quiver.stack(), Quiver.SIZE, Quiver.ARROWS) : null,
                satchel, quiver, satchelName, quiverName), title), buf -> {
                    buf.writeBoolean(satchel != null);
                    buf.writeBoolean(quiver != null);
                    ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, satchelName);
                    ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, quiverName);
                });
    }

    // ---------------------------------------------------------------- menu

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < invStart) {
            if (!moveItemStackTo(stack, invStart, invEnd, true)) return ItemStack.EMPTY;
        } else {
            // arrows into the quiver first, the rest (and arrows that don't fit) into the satchel
            boolean moved = hasQuiver && Quiver.ARROWS.test(stack) && moveItemStackTo(stack, quiverStart, invStart, false);
            if (!stack.isEmpty() && hasSatchel && BarkBasketMenu.SATCHEL.test(stack)) moved |= moveItemStackTo(stack, 0, quiverStart, false);
            if (!moved) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillThere(player, satchel) && stillThere(player, quiver);
    }

    /** Server: the bag is still where it was opened from (the very same stack). The client has no sources. */
    private static boolean stillThere(Player player, @Nullable Source source) {
        if (source == null) return true;
        if (source.worn() != null) return Equipment.get(player, source.worn()) == source.stack();
        return player.getInventory().getItem(source.inventorySlot()) == source.stack();
    }
}
