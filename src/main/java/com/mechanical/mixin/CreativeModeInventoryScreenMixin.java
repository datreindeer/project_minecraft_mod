package com.mechanical.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mechanical.equipment.Accessory;
import com.mechanical.equipment.Equipment;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;

/**
 * The accessory slots ({@link Equipment}) in the creative inventory tab: vanilla would lay them out as extra slots of the
 * main grid (over the hotbar); they go to the free space right of the armour and left of the off hand instead, with slot
 * frames copied from the tab's own texture.
 */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin extends AbstractContainerScreen<CreativeModeInventoryScreen.ItemPickerMenu> {
    private static final Identifier TAB_INVENTORY = Identifier.withDefaultNamespace("textures/gui/container/creative_inventory/tab_inventory.png");
    private static final String SLOT_WRAPPER = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen$SlotWrapper;<init>(Lnet/minecraft/world/inventory/Slot;III)V";

    @Shadow
    private static CreativeModeTab selectedTab;

    private CreativeModeInventoryScreenMixin(CreativeModeInventoryScreen.ItemPickerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    private static Accessory mechanical$accessory(int index) {
        int i = index - Equipment.MENU_START;
        return i >= 0 && i < Accessory.COUNT ? Accessory.values()[i] : null;
    }

    @ModifyArg(method = "selectTab", index = 2, at = @At(value = "INVOKE", target = SLOT_WRAPPER))
    private int mechanical$accessoryX(Slot target, int index, int x, int y) {
        Accessory accessory = mechanical$accessory(index);
        return accessory != null ? accessory.creativeX : x;
    }

    @ModifyArg(method = "selectTab", index = 3, at = @At(value = "INVOKE", target = SLOT_WRAPPER))
    private int mechanical$accessoryY(Slot target, int index, int x, int y) {
        Accessory accessory = mechanical$accessory(index);
        return accessory != null ? accessory.creativeY : y;
    }

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void mechanical$accessoryFrames(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (selectedTab.getType() != CreativeModeTab.Type.INVENTORY) return;
        for (Accessory slot : Accessory.values()) {
            // the first main-grid slot's frame (8,53) of the tab texture
            graphics.blit(RenderPipelines.GUI_TEXTURED, TAB_INVENTORY, leftPos + slot.creativeX - 1, topPos + slot.creativeY - 1, 8.0F, 53.0F, 18, 18, 256, 256);
        }
    }
}
