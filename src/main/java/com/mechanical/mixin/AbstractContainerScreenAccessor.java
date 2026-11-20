package com.mechanical.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/** Lets InventoryScreenMixin make the survival inventory taller (the size is final, set by the constructor). */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Mutable
    @Accessor("imageHeight")
    void mechanical$setImageHeight(int imageHeight);
}
