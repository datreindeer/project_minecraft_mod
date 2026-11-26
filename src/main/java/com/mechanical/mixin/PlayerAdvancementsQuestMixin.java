package com.mechanical.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mechanical.Mechanical;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.advancements.AdvancementVisibilityEvaluator;

/**
 * The quest screen shows what lies ahead, so every one of the mod's advancements (with a display, not hidden) is sent to
 * the player. Vanilla only sends those near one already earned, and nothing of an untouched tab.
 */
@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsQuestMixin {
    @Shadow
    private void markForVisibilityUpdate(AdvancementHolder advancement) {}

    @Inject(method = "load", at = @At("TAIL"))
    private void mechanical$sendEveryChapter(ServerAdvancementManager manager, CallbackInfo ci) {
        for (AdvancementHolder holder : manager.getAllAdvancements())
            if (holder.value().isRoot() && holder.id().getNamespace().equals(Mechanical.MODID)) markForVisibilityUpdate(holder);
    }

    @ModifyArg(method = "updateTreeVisibility", index = 2, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/advancements/AdvancementVisibilityEvaluator;evaluateVisibility(Lnet/minecraft/advancements/AdvancementNode;Ljava/util/function/Predicate;Lnet/minecraft/server/advancements/AdvancementVisibilityEvaluator$Output;)V"))
    private AdvancementVisibilityEvaluator.Output mechanical$showQuests(AdvancementVisibilityEvaluator.Output output) {
        return (AdvancementNode node, boolean visible) -> output.accept(node, visible
                || node.holder().id().getNamespace().equals(Mechanical.MODID)
                        && node.advancement().display().filter(d -> !d.hidden()).isPresent());
    }
}
