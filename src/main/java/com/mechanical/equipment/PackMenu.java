package com.mechanical.equipment;

import java.util.ArrayList;
import java.util.List;
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
 * The Leather Satchel's 3x3 and the 2x2s of what hangs at the hip (the Quiver: arrows only; or a Leather Pouch: what a
 * satchel takes; one per Hip slot) side by side, or whichever of them is there, over the player inventory. Opened with B for what is worn in the Back and both Hip slots ({@link #openWorn}), or by right-clicking one in
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
        if (satchel != null && satchel.worn() != null) return true;
        for (Hip h : hips) if (h.source() != null && h.source().worn() != null) return true;
        return false;
    }

    /** Where a bag comes from: worn in an accessory slot, or at an inventory index (40 = off hand). */
    record Source(ItemStack stack, @Nullable Accessory worn, int inventorySlot) {}

    /** One 2x2 bag at the hip in this menu: its contents, where it comes from (null on the client), its name, pouch or quiver. */
    record Hip(Container contents, @Nullable Source source, Component name, boolean pouch) {
        Predicate<ItemStack> allowed() {
            return allowed(pouch);
        }

        static Predicate<ItemStack> allowed(boolean pouch) {
            return pouch ? BarkBasketMenu.SATCHEL : Quiver.ARROWS;
        }
    }

    public final boolean hasSatchel, hasHip;
    /** How many hip bags are shown: 0, 1, or 2 (both Hip slots, from B). */
    public final int hipCount;
    public final Component satchelName;
    /** The hip bags' names, first Hip slot first. */
    public final List<Component> hipNames;
    /** The bag's grid width (= height), its tool loops, and how much taller than the 3x3 screen the menu is. */
    public final int columns, tools, extraHeight;
    private final int toolStart, hipStart, invStart, invEnd;
    private final @Nullable Source satchel;
    private final List<Hip> hips;
    private final int lockedSlot;

    /** Client side: the layout comes in the open packet. */
    private static PackMenu client(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        boolean hasSatchel = buf.readBoolean();
        int columns = buf.readVarInt(), tools = buf.readVarInt();
        Component satchelName = ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buf);
        int hipCount = buf.readVarInt();
        List<Hip> hips = new ArrayList<>();
        for (int i = 0; i < hipCount; i++) {
            boolean pouch = buf.readBoolean();
            hips.add(new Hip(new SimpleContainer(Quiver.SIZE), null, ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buf), pouch));
        }
        return new PackMenu(id, inventory, hasSatchel ? new SimpleContainer(columns * columns + tools) : null, null, satchelName, hips, columns, tools);
    }

    PackMenu(int id, Inventory inventory, @Nullable Container satchelContents, @Nullable Source satchel, Component satchelName,
            List<Hip> hips, int columns, int tools) {
        super(TYPE.get(), id);
        this.columns = satchelContents != null ? columns : 3;
        this.tools = satchelContents != null ? tools : 0;
        this.extraHeight = (this.columns - 3) * 18;
        this.hasSatchel = satchelContents != null;
        this.hips = List.copyOf(hips);
        this.hipCount = hips.size();
        this.hasHip = hipCount > 0;
        this.satchel = satchel;
        this.satchelName = satchelName;
        this.hipNames = hips.stream().map(Hip::name).toList();
        int locked = -1;
        if (satchel != null && satchel.worn() == null) locked = satchel.inventorySlot();
        for (Hip h : hips) if (h.source() != null && h.source().worn() == null) locked = h.source().inventorySlot();
        this.lockedSlot = locked;

        int left = satchelLeft(hipCount, this.columns, this.tools);
        if (satchelContents != null) {
            for (int row = 0; row < columns; row++) {
                for (int col = 0; col < columns; col++) addSlot(bagSlot(satchelContents, col + row * columns, left + col * 18, 17 + row * 18, BarkBasketMenu.SATCHEL));
            }
        }
        toolStart = slots.size();
        for (int i = 0; i < this.tools; i++) addSlot(bagSlot(satchelContents, this.columns * this.columns + i, left + this.columns * 18 + 4, 17 + i * 18, Satchels::isTool));
        hipStart = slots.size();
        for (int n = 0; n < hipCount; n++) {
            int[] at = hipPosition(hasSatchel, this.columns, this.tools, hipCount, n);
            Hip h = hips.get(n);
            for (int row = 0; row < 2; row++) {
                for (int col = 0; col < 2; col++) addSlot(bagSlot(h.contents(), col + row * 2, at[0] + col * 18, at[1] + row * 18, h.allowed()));
            }
        }
        invStart = slots.size();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(playerSlot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + extraHeight + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(playerSlot(inventory, col, 8 + col * 18, 142 + extraHeight));
        invEnd = slots.size();
    }

    /**
     * Left edge of the bag's grid: centred alone; at the left with one hip bag beside it (the 3x3 as it always was); with
     * two hip bags the 3x3 and both 2x2s centred in a row (from the left edge with tool loops). scripts/gen_equipment_textures.py
     * draws the backgrounds from the same numbers.
     */
    public static int satchelLeft(int hipCount, int columns, int tools) {
        if (columns == 3) return hipCount == 0 ? 62 : hipCount == 1 ? 30 : tools > 0 ? 8 : 18;
        return hipCount > 0 ? 8 : (176 - columns * 18 - (tools > 0 ? 22 : 0)) / 2;
    }

    /** Top-left slot (x, y) of hip bag {@code n} of {@code count}: beside the 3x3, stacked beside the 5x5, or centred alone. */
    public static int[] hipPosition(boolean hasSatchel, int columns, int tools, int count, int n) {
        if (!hasSatchel) return count == 1 ? new int[] {71, 26} : new int[] {47 + n * 46, 26};
        if (columns > 3) return count == 1 ? new int[] {134, 26} : new int[] {134, 17 + n * 46};
        if (count == 1) return new int[] {116, 26};
        return new int[] {(tools > 0 ? 90 : 82) + n * 42, 26};
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

    /** The bag worn on the back (a Leather Satchel or the Steel-Frame Pack), or null. */
    private static @Nullable Source wornSatchel(Player player) {
        ItemStack back = Equipment.get(player, Accessory.BACK);
        return back.getItem() instanceof LeatherSatchelItem ? new Source(back, Accessory.BACK, -1) : null;
    }

    /** The quiver or pouches worn in the two Hip slots, first slot first. */
    private static List<Source> wornHips(Player player) {
        List<Source> hips = new ArrayList<>();
        for (Accessory slot : new Accessory[] {Accessory.HIP, Accessory.HIP_2}) {
            ItemStack hip = Equipment.get(player, slot);
            if (isHipBag(hip)) hips.add(new Source(hip, slot, -1));
        }
        return hips;
    }

    /** B: the satchel worn on the back and the quiver or pouches in both Hip slots. Says so if none is worn. */
    public static void openWorn(Player player) {
        Source satchel = wornSatchel(player);
        List<Source> hips = wornHips(player);
        if (satchel == null && hips.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.nothing_to_open"));
            return;
        }
        open(player, satchel, hips);
    }

    /** Right-click with a satchel, pouch or quiver in hand. */
    public static void openHeld(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        int slot = hand == InteractionHand.MAIN_HAND ? player.getInventory().getSelectedSlot() : Inventory.SLOT_OFFHAND;
        Source source = new Source(stack, null, slot);
        if (isHipBag(stack)) open(player, null, List.of(source));
        else if (stack.getItem() instanceof LeatherSatchelItem) open(player, source, List.of());
    }

    private static void open(Player player, @Nullable Source satchel, List<Source> hipSources) {
        Component satchelName = satchel != null ? satchel.stack().getHoverName() : Component.empty();
        Component title = satchel != null ? satchelName : hipSources.get(0).stack().getHoverName();
        int columns = satchel != null ? Satchels.columns(satchel.stack()) : 3, tools = satchel != null ? Satchels.toolSlots(satchel.stack()) : 0;
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new PackMenu(id, inventory, satchelContents(satchel, columns, tools),
                satchel, satchelName, hipSources.stream().map(PackMenu::hip).toList(), columns, tools), title), buf -> {
                    buf.writeBoolean(satchel != null);
                    buf.writeVarInt(columns);
                    buf.writeVarInt(tools);
                    ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, satchelName);
                    buf.writeVarInt(hipSources.size());
                    for (Source s : hipSources) {
                        buf.writeBoolean(s.stack().getItem() instanceof LeatherPouchItem);
                        ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, s.stack().getHoverName());
                    }
                });
    }

    private static @Nullable Container satchelContents(@Nullable Source satchel, int columns, int tools) {
        return satchel != null ? new ItemContents(satchel.stack(), columns * columns + tools, BarkBasketMenu.SATCHEL) : null;
    }

    /** Server: a hip bag's live contents in the menu. */
    private static Hip hip(Source source) {
        boolean pouch = source.stack().getItem() instanceof LeatherPouchItem;
        return new Hip(new ItemContents(source.stack(), Quiver.SIZE, Hip.allowed(pouch)), source, source.stack().getHoverName(), pouch);
    }

    /** Tests: the menu as B would open it for {@code bag} worn on the back (server side, nothing sent). */
    public static PackMenu forTest(Inventory inventory, ItemStack bag) {
        int columns = Satchels.columns(bag), tools = Satchels.toolSlots(bag);
        return new PackMenu(0, inventory, new ItemContents(bag, columns * columns + tools, BarkBasketMenu.SATCHEL),
                new Source(bag, null, -1), Component.empty(), List.of(), columns, tools);
    }

    /** Tests: the menu B opens for what this player wears (server side, nothing sent), or null if nothing is worn. */
    public static @Nullable PackMenu wornForTest(Player player) {
        Source satchel = wornSatchel(player);
        List<Source> hips = wornHips(player);
        if (satchel == null && hips.isEmpty()) return null;
        int columns = satchel != null ? Satchels.columns(satchel.stack()) : 3, tools = satchel != null ? Satchels.toolSlots(satchel.stack()) : 0;
        return new PackMenu(0, player.getInventory(), satchelContents(satchel, columns, tools), satchel, Component.empty(),
                hips.stream().map(PackMenu::hip).toList(), columns, tools);
    }

    /** Menu index of the first slot of hip bag {@code n} ({@code n == hipCount}: the end of the last one). */
    public int hipStart(int n) {
        return hipStart + n * Quiver.SIZE;
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
            // arrows into the quiver first, tools into the tool loops, the rest (and arrows that don't fit) into the satchel, then the pouches
            boolean moved = false;
            for (int n = 0; n < hipCount && !stack.isEmpty(); n++) {
                if (!hips.get(n).pouch() && Quiver.ARROWS.test(stack)) moved |= moveItemStackTo(stack, hipStart(n), hipStart(n + 1), false);
            }
            if (!stack.isEmpty() && tools > 0 && Satchels.isTool(stack)) moved |= moveItemStackTo(stack, toolStart, hipStart, false);
            if (!stack.isEmpty() && hasSatchel && BarkBasketMenu.SATCHEL.test(stack)) moved |= moveItemStackTo(stack, 0, toolStart, false);
            for (int n = 0; n < hipCount && !stack.isEmpty(); n++) {
                if (hips.get(n).pouch() && hips.get(n).allowed().test(stack)) moved |= moveItemStackTo(stack, hipStart(n), hipStart(n + 1), false);
            }
            if (!moved) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        if (!stillThere(player, satchel)) return false;
        for (Hip h : hips) if (!stillThere(player, h.source())) return false;
        return true;
    }

    /** Server: the bag is still where it was opened from (the very same stack). The client has no sources. */
    private static boolean stillThere(Player player, @Nullable Source source) {
        if (source == null) return true;
        if (source.worn() != null) return Equipment.get(player, source.worn()) == source.stack();
        return player.getInventory().getItem(source.inventorySlot()) == source.stack();
    }
}
