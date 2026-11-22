package com.mechanical.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mechanical.LogCollision;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Every entity's movement also collides with felled logs turned off the block grid ({@link LogCollision}). */
@Mixin(Entity.class)
public abstract class EntityLogCollisionMixin {
    @Inject(method = "collide", at = @At("RETURN"), cancellable = true)
    private void mechanical$turnedLogs(Vec3 movement, CallbackInfoReturnable<Vec3> cir) {
        cir.setReturnValue(LogCollision.collide((Entity) (Object) this, movement, cir.getReturnValue()));
    }
}
