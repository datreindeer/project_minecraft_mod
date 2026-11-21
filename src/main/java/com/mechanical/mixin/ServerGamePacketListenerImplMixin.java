package com.mechanical.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mechanical.Chemistry;
import com.mechanical.equipment.Accessory;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.FilteredText;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

/**
 * Creative mode sets inventory slots by number, and vanilla only accepts 1..45: also accept the accessory slots after
 * them (InventoryMenuMixin), or changes made to them in the creative inventory would not reach the server.
 * The Slate Tablet (Chemistry) uses the book screen: it keeps only its one page, and signing it just saves that page.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
    @Shadow
    public ServerPlayer player;

    @Shadow
    private void updateBookContents(List<FilteredText> contents, int slot) {
        throw new AssertionError();
    }

    @ModifyConstant(method = "handleSetCreativeModeSlot", constant = @Constant(intValue = 45))
    private int mechanical$accessorySlots(int lastSlot) {
        return lastSlot + Accessory.COUNT;
    }

    @Inject(method = "updateBookContents", at = @At("HEAD"), cancellable = true)
    private void mechanical$tabletPage(List<FilteredText> contents, int slot, CallbackInfo ci) {
        if (contents.size() > Chemistry.SlateTabletItem.PAGES && player.getInventory().getItem(slot).is(Chemistry.SLATE_TABLET.get())) {
            updateBookContents(contents.subList(0, Chemistry.SlateTabletItem.PAGES), slot);
            ci.cancel();
        }
    }

    @Inject(method = "signBook", at = @At("HEAD"), cancellable = true)
    private void mechanical$tabletSign(FilteredText title, List<FilteredText> contents, int slot, CallbackInfo ci) {
        if (player.getInventory().getItem(slot).is(Chemistry.SLATE_TABLET.get())) {
            updateBookContents(contents, slot);
            ci.cancel();
        }
    }
}
