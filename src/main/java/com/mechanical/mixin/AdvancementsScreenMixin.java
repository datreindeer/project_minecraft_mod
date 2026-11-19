package com.mechanical.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.mechanical.client.AdvancementWindow;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.resources.Identifier;

/** Stretches the advancements window to fill the screen (see AdvancementWindow); the frame is drawn as a nine-slice. */
@Mixin(AdvancementsScreen.class)
public abstract class AdvancementsScreenMixin {
    private Screen mechanical$self() {
        return (Screen) (Object) this;
    }

    @ModifyConstant(method = {"init", "repositionElements", "mouseClicked"}, constant = @Constant(intValue = 252))
    private int mechanical$width(int vanilla) {
        return AdvancementWindow.width(mechanical$self().width);
    }

    @ModifyConstant(method = {"init", "repositionElements", "mouseClicked"}, constant = @Constant(intValue = 140))
    private int mechanical$height(int vanilla) {
        return AdvancementWindow.height(mechanical$self().height);
    }

    @ModifyConstant(method = "extractInside", constant = @Constant(intValue = 234))
    private int mechanical$insideWidth(int vanilla) {
        return AdvancementWindow.insideWidth(mechanical$self().width);
    }

    @ModifyConstant(method = "extractInside", constant = @Constant(intValue = 113))
    private int mechanical$insideHeight(int vanilla) {
        return AdvancementWindow.insideHeight(mechanical$self().height);
    }

    @ModifyConstant(method = "extractInside", constant = @Constant(intValue = 117))
    private int mechanical$insideMidX(int vanilla) {
        return AdvancementWindow.insideWidth(mechanical$self().width) / 2;
    }

    /** window.png as a nine-slice: corners kept, edges stretched (the inside is transparent). */
    @Redirect(method = "extractWindow", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"))
    private void mechanical$frame(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier texture, int x, int y,
            float u, float v, int w, int h, int texW, int texH) {
        int[] srcX = {0, 9, 243, 252}, srcY = {0, 18, 131, 140};
        int width = AdvancementWindow.width(mechanical$self().width), height = AdvancementWindow.height(mechanical$self().height);
        int[] dstX = {0, 9, width - 9, width}, dstY = {0, 18, height - 9, height};
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (i == 1 && j == 1) continue;
                graphics.blit(pipeline, texture, x + dstX[i], y + dstY[j], srcX[i], srcY[j], dstX[i + 1] - dstX[i], dstY[j + 1] - dstY[j],
                        srcX[i + 1] - srcX[i], srcY[j + 1] - srcY[j], texW, texH);
            }
        }
    }
}
