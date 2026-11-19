package com.mechanical.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import com.mechanical.client.AdvancementWindow;

import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;

/** The tab's view area follows the stretched window (AdvancementWindow): clipping, background tiles, scrolling, centring. */
@Mixin(AdvancementTab.class)
public abstract class AdvancementTabMixin {
    @Shadow @Final private AdvancementsScreen screen;

    @ModifyConstant(method = {"tick", "extractContents", "extractTooltips", "scroll", "canScrollHorizontally"}, constant = @Constant(intValue = 234))
    private int mechanical$insideWidth(int vanilla) {
        return AdvancementWindow.insideWidth(screen.width);
    }

    @ModifyConstant(method = {"tick", "extractContents", "extractTooltips", "scroll", "canScrollVertically"}, constant = @Constant(intValue = 113))
    private int mechanical$insideHeight(int vanilla) {
        return AdvancementWindow.insideHeight(screen.height);
    }

    @ModifyConstant(method = "extractContents", constant = @Constant(intValue = 117))
    private int mechanical$midX(int vanilla) {
        return AdvancementWindow.insideWidth(screen.width) / 2;
    }

    @ModifyConstant(method = "extractContents", constant = @Constant(intValue = 56))
    private int mechanical$midY(int vanilla) {
        return AdvancementWindow.insideHeight(screen.height) / 2;
    }

    /** Background tile columns ({@code x <= 15}). */
    @ModifyConstant(method = "extractContents", constant = @Constant(intValue = 15))
    private int mechanical$tilesX(int vanilla) {
        return AdvancementWindow.insideWidth(screen.width) / 16 + 1;
    }

    /** Background tile rows ({@code y <= 8}). */
    @ModifyConstant(method = "extractContents", constant = @Constant(intValue = 8))
    private int mechanical$tilesY(int vanilla) {
        return AdvancementWindow.insideHeight(screen.height) / 16 + 1;
    }
}
