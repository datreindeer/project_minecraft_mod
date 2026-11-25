package com.mechanical.mixin;

import com.mechanical.HorseWork;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A ridden horse snaps to its rider's yaw; hitched it comes about slowly and its speed is capped (HorseWork). Both sides. */
@Mixin(AbstractHorse.class)
public abstract class AbstractHorseMixin {
    @Inject(method = "getRiddenRotation", at = @At("RETURN"), cancellable = true)
    private void mechanical$hitchedTurn(LivingEntity controller, CallbackInfoReturnable<Vec2> cir) {
        Vec2 rotation = cir.getReturnValue();
        cir.setReturnValue(new Vec2(rotation.x, HorseWork.riddenYaw((AbstractHorse) (Object) this, rotation.y)));
    }

    // shortcut: the vanilla camel overrides getRiddenSpeed without super, so it isn't capped; mixin Camel too if that matters
    @Inject(method = "getRiddenSpeed", at = @At("RETURN"), cancellable = true)
    private void mechanical$hitchedSpeed(Player controller, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(HorseWork.riddenSpeed((AbstractHorse) (Object) this, cir.getReturnValue()));
    }
}
