package com.mechanical.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mechanical.client.DynamicLights;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Dynamic lights for entities (mobs, players, items) standing near a held torch. */
@Mixin(EntityRenderer.class)
public abstract class EntityLightMixin {
    @Inject(method = "getPackedLightCoords(Lnet/minecraft/world/entity/Entity;F)I", at = @At("RETURN"), cancellable = true)
    private void mechanical$dynamicLight(Entity entity, float partialTicks, CallbackInfoReturnable<Integer> cir) {
        Vec3 p = entity.getLightProbePosition(partialTicks);
        int dynamic = DynamicLights.levelAt(net.minecraft.util.Mth.floor(p.x), net.minecraft.util.Mth.floor(p.y), net.minecraft.util.Mth.floor(p.z));
        int packed = cir.getReturnValueI();
        if (dynamic > LightCoordsUtil.block(packed)) cir.setReturnValue(LightCoordsUtil.withBlock(packed, dynamic));
    }
}
