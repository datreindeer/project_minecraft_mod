package com.mechanical.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.mechanical.client.ui.HideUi;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The hide GUI is dark: vanilla's dark grey label colour (container titles, "Inventory", creative tab names, the
 * advancements window) is drawn in the hide's tan instead. Every text call ends in this method.
 */
@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiTextColorMixin {
    @ModifyVariable(method = "text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private int mechanical$readableOnHide(int color) {
        return color == HideUi.VANILLA_LABEL ? HideUi.LABEL : color;
    }
}
