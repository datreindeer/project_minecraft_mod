package com.mechanical.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mechanical.client.DynamicLights;

import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.block.state.BlockState;

/** Dynamic lights for terrain, fluids and block entities: raise block light where a held torch is near. */
@Mixin(LightCoordsUtil.class)
public abstract class LightCoordsMixin {
    @Inject(method = "getLightCoords(Lnet/minecraft/util/LightCoordsUtil$BrightnessGetter;Lnet/minecraft/world/level/BlockAndLightGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I",
            at = @At("RETURN"), cancellable = true)
    private static void mechanical$dynamicLight(LightCoordsUtil.BrightnessGetter getter, BlockAndLightGetter level, BlockState state, BlockPos pos,
            CallbackInfoReturnable<Integer> cir) {
        int packed = cir.getReturnValueI();
        int dynamic = DynamicLights.levelAt(pos.getX(), pos.getY(), pos.getZ());
        if (dynamic > LightCoordsUtil.block(packed)) cir.setReturnValue(LightCoordsUtil.withBlock(packed, dynamic));
    }
}
