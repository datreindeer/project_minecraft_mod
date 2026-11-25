package com.mechanical.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mechanical.equipment.Accessory;
import com.mechanical.equipment.AccessorySlot;
import com.mechanical.equipment.Equipment;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The accessory slots ({@link Equipment}) in the player's inventory menu: added after all vanilla slots (indices from
 * Equipment.MENU_START, so vanilla's slot numbers keep their meaning), with the main inventory moved down to make room
 * for the row under the player model. Shift-click puts a wearable item into its free accessory slot.
 */
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin extends AbstractCraftingMenu {
    private InventoryMenuMixin(MenuType<?> type, int containerId, int width, int height) {
        super(type, containerId, width, height);
    }

    @ModifyArg(method = "<init>", index = 2, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/inventory/InventoryMenu;addStandardInventorySlots(Lnet/minecraft/world/Container;II)V"))
    private int mechanical$inventoryLower(int top) {
        return top + Equipment.SCREEN_EXTRA_HEIGHT;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void mechanical$accessorySlots(Inventory inventory, boolean active, Player owner, CallbackInfo ci) {
        for (Accessory accessory : Accessory.values()) addSlot(new AccessorySlot(owner, accessory, accessory.x, accessory.y));
        // make room for the wider player box (InventoryScreenMixin): off hand in line with the accessory column, crafting aside
        mechanical$moveX(InventoryMenu.SHIELD_SLOT, 3);
        for (int i = InventoryMenu.CRAFT_SLOT_START; i < InventoryMenu.CRAFT_SLOT_END; i++) mechanical$moveX(i, 2);
        mechanical$moveX(InventoryMenu.RESULT_SLOT, 1);
    }

    private void mechanical$moveX(int index, int by) {
        Slot slot = slots.get(index);
        ((SlotAccessor) slot).mechanical$setX(slot.x + by);
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void mechanical$toAccessory(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (index < InventoryMenu.INV_SLOT_START || index >= InventoryMenu.USE_ROW_SLOT_END) return;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return;
        ItemStack stack = slot.getItem();
        Accessory target = Equipment.freeSlotFor(player, stack);
        if (target == null) return;
        int to = Equipment.MENU_START + target.ordinal();
        ItemStack copy = stack.copy();
        if (!moveItemStackTo(stack, to, to + 1, false)) return;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY, copy);
        else slot.setChanged();
        slot.onTake(player, stack);
        cir.setReturnValue(copy);
    }
}
