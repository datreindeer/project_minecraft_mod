package com.mechanical.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.google.common.collect.ImmutableList;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mechanical.FelledLogEntity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.EntityGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A felled log collides block by block ({@link FelledLogEntity#collisionShape}), not as its whole bounding box; and only
 * shapes that reach into the box asked about are returned. The server's move check (isPlayerCollidingWithAnythingNew)
 * counts every returned shape the player wasn't already in as a new collision and quietly puts the player back: a log
 * whose box (not its blocks) reached the player would freeze them where they stood.
 */
@Mixin(EntityGetter.class)
public interface EntityGetterMixin {
    @WrapOperation(method = "getEntityCollisions", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/phys/shapes/Shapes;create(Lnet/minecraft/world/phys/AABB;)Lnet/minecraft/world/phys/shapes/VoxelShape;"))
    private VoxelShape mechanical$logBlocks(AABB box, Operation<VoxelShape> original, @Local(ordinal = 1) Entity entity) {
        return entity instanceof FelledLogEntity log ? log.collisionShape() : original.call(box);
    }

    @ModifyReturnValue(method = "getEntityCollisions", at = @At("RETURN"))
    private List<VoxelShape> mechanical$onlyReaching(List<VoxelShape> shapes, Entity source, AABB box) {
        if (shapes.isEmpty()) return shapes;
        VoxelShape query = Shapes.create(box.inflate(1.0E-7));
        boolean all = true;
        for (VoxelShape s : shapes) if (s.isEmpty() || !Shapes.joinIsNotEmpty(s, query, BooleanOp.AND)) all = false;
        if (all) return shapes;
        ImmutableList.Builder<VoxelShape> out = ImmutableList.builder();
        for (VoxelShape s : shapes) if (!s.isEmpty() && Shapes.joinIsNotEmpty(s, query, BooleanOp.AND)) out.add(s);
        return out.build();
    }
}
