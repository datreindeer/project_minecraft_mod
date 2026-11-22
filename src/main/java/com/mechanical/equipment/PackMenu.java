package com.mechanical.equipment;

import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.mechanical.BarkBasketMenu;
import com.mechanical.LeatherPouchItem;
import com.mechanical.LeatherSatchelItem;
import com.mechanical.Mechanical;
import com.mechanical.Satchels;

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
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;

/**
 * The Leather Satchel's 3x3 and the 2x2 of what hangs at the hip (the Quiver: arrows only; or the Leather Pouch: what a
 * satchel takes) side by side, or whichever of them is there, over the player inventory. Opened with B for what is worn in the Back and Hip slots ({@link #openWorn}), or by right-clicking one in
 * hand ({@link #openHeld}; that inventory slot is locked while open). The contents are read from and written to the
 * items themselves ({@link ItemContents}). Layout shared with client.PackScreen.
 * <p>
 * A bag with tool loops (com.mechanical.Satchels) gets a column of {@link Satchels#TOOL_SLOTS} tool-only slots right of its
 * grid (stored after the grid in the item); the Steel-Frame Pack is a 5x5 grid, which makes the screen two rows taller.
 */
public class PackMenu extends AbstractContainerMenu {
    public static final int SATCHEL_SIZE = BarkBasketMenu.SIZE;
    public static final Supplier<MenuType<PackMenu>> TYPE = Mechanical.MENUS.register("pack", () -> IMenuTypeExtension.create(PackMenu::client));

    /**
     * What the player is rummaging in while a bag's menu is open, synced so everyone sees it (client/HoldPoses): 0 nothing,
     * {@link #WORN} the satchel or pouch they wear (B), {@link #HELD} a bag or Bark Basket in hand. Not saved.
     */
    public static final Supplier<AttachmentType<Byte>> RUMMAGING = Mechanical.ATTACHMENTS.register("rummaging",
            () -> AttachmentType.builder(() -> (byte) 0).sync(ByteBufCodecs.BYTE).build());
    public static final byte WORN = 1, HELD = 2;

    static void init() {
        NeoForge.EVENT_BUS.addListener((PlayerContainerEvent.Open e) -> {
            byte how = e.getContainer() instanceof PackMenu pack ? (pack.worn() ? WORN : HELD) : e.getContainer() instanceof BarkBasketMenu ? HELD : 0;
            if (how != 0) e.getEntity().setData(RUMMAGING, how);
        });
        NeoForge.EVENT_BUS.addListener((PlayerContainerEvent.Close e) -> {
            if (e.getEntity().getData(RUMMAGING) != 0) e.getEntity().setData(RUMMAGING, (byte) 0);
        });
    }

    /** Server: opened from what is worn (B) rather than from a hand. */
    private boolean worn() {
        return satchel != null && satchel.worn() != null || hip != null && hip.worn() != null;
    }

    /** Where a bag comes from: worn in an accessory slot, or at an inventory index (40 = off hand). */
    record Source(ItemStack stack, @Nullable Accessory worn, int inventorySlot) {}

    public final boolean hasSatchel, hasHip;
    public final Component satchelName, hipName;
    /** The bag's grid width (= height), its tool loops, and how much taller than the 3x3 screen the menu is. */
    public final int columns, tools, extraHeight;
    private final int toolStart, hipStart, invStart, invEnd;
    private final @Nullable Source satchel, hip;
    /** What the hip slots take: arrows (quiver) or a satchel's things (pouch). */
    private final Predicate<ItemStack> hipAllowed;
    private final int lockedSlot;

    /** Client side: the layout comes in the open packet. */
    private static PackMenu client(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        boolean hasSatchel = buf.readBoolean(), hasHip = buf.readBoolean(), pouch = buf.readBoolean();
        int columns = buf.readVarInt(), tools = buf.readVarInt();
        Component satchelName = ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buf);
        Component hipName = ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buf);
        return new PackMenu(id, inventory, hasSatchel ? new SimpleContainer(columns * columns + tools) : null, hasHip ? new SimpleContainer(Quiver.SIZE) : null,
                null, null, satchelName, hipName, pouch, columns, tools);
    }

    PackMenu(int id, Inventory inventory, @Nullable Container satchelContents, @Nullable Container hipContents,
            @Nullable Source satchel, @Nullable Source hip, Component satchelName, Component hipName, boolean pouch, int columns, int tools) {
        super(TYPE.get(), id);
        this.columns = satchelContents != null ? columns : 3;
        this.tools = satchelContents != null ? tools : 0;
        this.extraHeight = (this.columns - 3) * 18;
        this.hasSatchel = satchelContents != null;
        this.hasHip = hipContents != null;
        this.satchel = satchel;
        this.hip = hip;
        this.satchelName = satchelName;
        this.hipName = hipName;
        this.hipAllowed = pouch ? BarkBasketMenu.SATCHEL : Quiver.ARROWS;
        int locked = -1;
        if (satchel != null && satchel.worn() == null) locked = satchel.inventorySlot();
        if (hip != null && hip.worn() == null) locked = hip.inventorySlot();
        this.lockedSlot = locked;

        int left = satchelLeft(hasHip, this.columns, this.tools);
        if (satchelContents != null) {
            for (int row = 0; row < columns; row++) {
                for (int col = 0; col < columns; col++) addSlot(bagSlot(satchelContents, col + row * columns, left + col * 18, 17 + row * 18, BarkBasketMenu.SATCHEL));
            }
        }
        toolStart = slots.size();
        for (int i = 0; i < this.tools; i++) addSlot(bagSlot(satchelContents, this.columns * this.columns + i, left + this.columns * 18 + 4, 17 + i * 18, Satchels::isTool));
        hipStart = slots.size();
        if (hipContents != null) {
            int hipLeft = hasSatchel ? (this.columns == 3 ? 116 : 134) : 71;
            for (int row = 0; row < 2; row++) {
                for (int col = 0; col < 2; col++) addSlot(bagSlot(hipContents, col + row * 2, hipLeft + col * 18, 26 + row * 18, hipAllowed));
            }
        }
        invStart = slots.size();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(playerSlot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + extraHeight + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(playerSlot(inventory, col, 8 + col * 18, 142 + extraHeight));
        invEnd = slots.size();
    }

    /** Left edge of the bag's grid: centred, or at the left with the hip bag beside it (the 3x3 as it always was). */
    public static int satchelLeft(boolean hasHip, int columns, int tools) {
        if (columns == 3) return hasHip ? 30 : 62;
        return hasHip ? 8 : (176 - columns * 18 - (tools > 0 ? 22 : 0)) / 2;
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

    /** A 2x2 bag for the hip: the Quiver or the Leather Pouch. */
    private static boolean isHipBag(ItemStack stack) {
        return stack.getItem() instanceof QuiverItem || stack.getItem() instanceof LeatherPouchItem;
    }

    /** B: the satchel worn on the back and the quiver or pouch at the hip. Says so if neither is worn. */
    public static void openWorn(Player player) {
        ItemStack back = Equipment.get(player, Accessory.BACK), hip = Equipment.get(player, Accessory.HIP);
        Source satchel = back.getItem() instanceof LeatherSatchelItem ? new Source(back, Accessory.BACK, -1) : null; // the Steel-Frame Pack too
        Source hipBag = isHipBag(hip) ? new Source(hip, Accessory.HIP, -1) : null;
        if (satchel == null && hipBag == null) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.nothing_to_open"));
            return;
        }
        open(player, satchel, hipBag);
    }

    /** Right-click with a satchel, pouch or quiver in hand. */
    public static void openHeld(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        int slot = hand == InteractionHand.MAIN_HAND ? player.getInventory().getSelectedSlot() : Inventory.SLOT_OFFHAND;
        Source source = new Source(stack, null, slot);
        if (isHipBag(stack)) open(player, null, source);
        else if (stack.getItem() instanceof LeatherSatchelItem) open(player, source, null);
    }

    private static void open(Player player, @Nullable Source satchel, @Nullable Source hip) {
        Component satchelName = satchel != null ? satchel.stack().getHoverName() : Component.empty();
        Component hipName = hip != null ? hip.stack().getHoverName() : Component.empty();
        Component title = satchel != null ? satchelName : hipName;
        boolean pouch = hip != null && hip.stack().getItem() instanceof LeatherPouchItem;
        Predicate<ItemStack> hipAllowed = pouch ? BarkBasketMenu.SATCHEL : Quiver.ARROWS;
        int columns = satchel != null ? Satchels.columns(satchel.stack()) : 3, tools = satchel != null ? Satchels.toolSlots(satchel.stack()) : 0;
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new PackMenu(id, inventory,
                satchel != null ? new ItemContents(satchel.stack(), columns * columns + tools, BarkBasketMenu.SATCHEL) : null,
                hip != null ? new ItemContents(hip.stack(), Quiver.SIZE, hipAllowed) : null,
                satchel, hip, satchelName, hipName, pouch, columns, tools), title), buf -> {
                    buf.writeBoolean(satchel != null);
                    buf.writeBoolean(hip != null);
                    buf.writeBoolean(pouch);
                    buf.writeVarInt(columns);
                    buf.writeVarInt(tools);
                    ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, satchelName);
                    ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, hipName);
                });
    }

    /** Tests: the menu as B would open it for {@code bag} worn on the back (server side, nothing sent). */
    public static PackMenu forTest(Inventory inventory, ItemStack bag) {
        int columns = Satchels.columns(bag), tools = Satchels.toolSlots(bag);
        return new PackMenu(0, inventory, new ItemContents(bag, columns * columns + tools, BarkBasketMenu.SATCHEL), null,
                new Source(bag, null, -1), null, Component.empty(), Component.empty(), false, columns, tools);
    }

    public int toolStart() {
        return toolStart;
    }

    public int invStart() {
        return invStart;
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
            // arrows into the quiver first, the rest (and arrows that don't fit) into the satchel, then the pouch
            boolean quiver = hasHip && hipAllowed == Quiver.ARROWS;
            boolean moved = quiver && Quiver.ARROWS.test(stack) && moveItemStackTo(stack, hipStart, invStart, false);
            // tools into the tool loops first, then the bag
            if (!stack.isEmpty() && tools > 0 && Satchels.isTool(stack)) moved |= moveItemStackTo(stack, toolStart, hipStart, false);
            if (!stack.isEmpty() && hasSatchel && BarkBasketMenu.SATCHEL.test(stack)) moved |= moveItemStackTo(stack, 0, toolStart, false);
            if (!stack.isEmpty() && hasHip && !quiver && hipAllowed.test(stack)) moved |= moveItemStackTo(stack, hipStart, invStart, false);
            if (!moved) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillThere(player, satchel) && stillThere(player, hip);
    }

    /** Server: the bag is still where it was opened from (the very same stack). The client has no sources. */
    private static boolean stillThere(Player player, @Nullable Source source) {
        if (source == null) return true;
        if (source.worn() != null) return Equipment.get(player, source.worn()) == source.stack();
        return player.getInventory().getItem(source.inventorySlot()) == source.stack();
    }
}
