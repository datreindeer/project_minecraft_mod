package com.mechanical.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import com.mechanical.equipment.Accessory;

import net.minecraft.server.network.ServerGamePacketListenerImpl;

/**
 * Creative mode sets inventory slots by number, and vanilla only accepts 1..45: also accept the accessory slots after
 * them (InventoryMenuMixin), or changes made to them in the creative inventory would not reach the server.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
    @ModifyConstant(method = "handleSetCreativeModeSlot", constant = @Constant(intValue = 45))
    private int mechanical$accessorySlots(int lastSlot) {
        return lastSlot + Accessory.COUNT;
    }
}
