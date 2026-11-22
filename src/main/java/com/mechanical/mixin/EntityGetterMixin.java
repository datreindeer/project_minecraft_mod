package com.mechanical.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mechanical.FelledLogEntity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.EntityGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A felled log collides block by block ({@link FelledLogEntity#collisionShape}), not as its whole bounding box. */
@Mixin(EntityGetter.class)
public interface EntityGetterMixin {
    @WrapOperation(method = "getEntityCollisions", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/phys/shapes/Shapes;create(Lnet/minecraft/world/phys/AABB;)Lnet/minecraft/world/phys/shapes/VoxelShape;"))
    private VoxelShape mechanical$logBlocks(AABB box, Operation<VoxelShape> original, @Local(ordinal = 1) Entity entity) {
        return entity instanceof FelledLogEntity log ? log.collisionShape() : original.call(box);
    }
}
