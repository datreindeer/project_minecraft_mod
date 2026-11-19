package com.mechanical.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.StrawBedBlock;

/**
 * Straw beds (plain and furred) keep the respawn point StrawBedRest sets: vanilla's straw bed rule says never, so a death
 * would send you to world spawn. Nothing else changes (you still can't sleep the night away in one).
 */
@Mixin(AbstractBedBlock.class)
public abstract class AbstractBedBlockMixin {
    @Inject(method = "getBedRule", at = @At("RETURN"), cancellable = true)
    private void mechanical$strawBedSpawn(Level level, BlockPos pos, CallbackInfoReturnable<BedRule> cir) {
        if (!((Object) this instanceof StrawBedBlock)) return;
        BedRule rule = cir.getReturnValue();
        cir.setReturnValue(new BedRule(rule.canSleep(), BedRule.Rule.ALWAYS, rule.destroyOnUse(), rule.destroyOnLeave(), rule.errorMessage()));
    }
}
