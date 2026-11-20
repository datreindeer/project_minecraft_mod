package com.mechanical.wildlife;

import java.util.function.Supplier;

import com.mechanical.BarkBasketMenu;
import com.mechanical.Mechanical;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ArmorSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;

/**
 * The tamed horse's inventory in place of vanilla's (whose grid stops at 5 columns): the saddle (Hide Saddle Blanket)
 * and horse armour slots, then the left satchel's 3x3, the horse, and the right satchel's 3x3, over the player
 * inventory. The satchel slots are the horse's own inventory ({@link TameHorse#getInventoryColumns}: 3 columns per
 * satchel) and take what a satchel takes ({@link BarkBasketMenu#SATCHEL}). Layout shared with client.SaddlebagScreen.
 */
public class SaddlebagMenu extends AbstractContainerMenu {
    public static final Supplier<MenuType<SaddlebagMenu>> TYPE = Mechanical.MENUS.register("saddlebags", () -> IMenuTypeExtension.create(SaddlebagMenu::client));
    public static final int WIDTH = 190, LEFT_BAG_X = 30, RIGHT_BAG_X = 130, BAG_Y = 18, INV_X = 15;
    private static final Identifier SADDLE_SPRITE = Identifier.withDefaultNamespace("container/slot/saddle");
    private static final Identifier ARMOR_SPRITE = Identifier.withDefaultNamespace("container/slot/horse_armor");
    private static final int BAGS_START = 2;

    static void init() {}

    public final TameHorse horse;
    public final int satchels;
    private final Container bags;
    private final int invStart;

    /** Client side: the horse and its satchel count come in the open packet (it is within reach, so loaded). */
    private static SaddlebagMenu client(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        TameHorse horse = (TameHorse) inventory.player.level().getEntity(buf.readVarInt());
        int satchels = buf.readVarInt();
        return new SaddlebagMenu(id, inventory, horse, new SimpleContainer(satchels * 9), satchels);
    }

    private SaddlebagMenu(int id, Inventory inventory, TameHorse horse, Container bags, int satchels) {
        super(TYPE.get(), id);
        this.horse = horse;
        this.bags = bags;
        this.satchels = satchels;
        addSlot(new ArmorSlot(horse.createEquipmentSlotContainer(EquipmentSlot.SADDLE), horse, EquipmentSlot.SADDLE, 0, 8, 18, SADDLE_SPRITE));
        addSlot(new ArmorSlot(horse.createEquipmentSlotContainer(EquipmentSlot.BODY), horse, EquipmentSlot.BODY, 0, 8, 36, ARMOR_SPRITE));
        int cols = satchels * TameHorse.SATCHEL_COLUMNS;
        for (int side = 0; side < satchels; side++) {
            int left = side == 0 ? LEFT_BAG_X : RIGHT_BAG_X;
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    addSlot(new Slot(bags, side * 3 + col + row * cols, left + col * 18, BAG_Y + row * 18) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return BarkBasketMenu.SATCHEL.test(stack);
                        }
                    });
                }
            }
        }
        invStart = slots.size();
        addStandardInventorySlots(inventory, INV_X, 84);
    }

    /** Server: sneak-click, or E while riding ({@link TameHorse#openCustomInventoryScreen}). */
    static void open(Player player, TameHorse horse) {
        int satchels = horse.satchelCount();
        Container bags = horse.getInventory();
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new SaddlebagMenu(id, inventory, horse, bags, satchels), horse.getDisplayName()), buf -> {
            buf.writeVarInt(horse.getId());
            buf.writeVarInt(satchels);
        });
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < invStart) {
            if (!moveItemStackTo(stack, invStart, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            for (int i = 0; i < BAGS_START && !moved; i++) {
                if (!slots.get(i).hasItem() && slots.get(i).mayPlace(stack)) moved = moveItemStackTo(stack, i, i + 1, false);
            }
            if (!moved && invStart > BAGS_START && BarkBasketMenu.SATCHEL.test(stack)) moved = moveItemStackTo(stack, BAGS_START, invStart, false);
            if (!moved) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    /** Closes when the horse dies, walks off, or gains or loses a satchel (its inventory is then a new one). */
    @Override
    public boolean stillValid(Player player) {
        return horse.isAlive() && !horse.hasInventoryChanged(bags) && player.isWithinEntityInteractionRange(horse, 4.0);
    }
}
