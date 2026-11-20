package com.mechanical.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mechanical.Thaw;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;

/**
 * Vanilla's snow-and-ice tick (one random column per chunk now and then) also thaws: snow melts a layer and ice turns
 * back to water where it is warm enough (Thaw.tick). Seas only freeze where they are cold all year (Thaw.mayFreeze).
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelThawMixin {
    @Inject(method = "tickPrecipitation", at = @At("HEAD"))
    private void mechanical$thaw(BlockPos pos, CallbackInfo ci) {
        Thaw.tick((ServerLevel) (Object) this, pos);
    }

    @WrapOperation(method = "tickPrecipitation", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/biome/Biome;shouldFreeze(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z"))
    private boolean mechanical$seasonalSeaIce(Biome biome, LevelReader level, BlockPos pos, Operation<Boolean> original) {
        return Thaw.mayFreeze((ServerLevel) (Object) this, pos) && original.call(biome, level, pos);
    }
}
