package com.mechanical.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Game tests: force-load every chunk of a test's padded area, not only the chunks of its structure. Vanilla forces the
 * structure's chunks alone, so with the 1-block "empty" structure a test's entities a few blocks away often stand in a chunk
 * that doesn't tick (depending on where the random test grid lands), and wagons, carts and mobs there never move.
 * Only test instance blocks run this, so it does nothing in a normal game.
 */
@Mixin(TestInstanceBlockEntity.class)
public abstract class TestInstanceBlockEntityMixin {
    @Redirect(method = "forceLoadChunks", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/TestInstanceBlockEntity;getStructureBoundingBox()Lnet/minecraft/world/level/levelgen/structure/BoundingBox;"))
    private BoundingBox mechanical$forcePaddedArea(TestInstanceBlockEntity self) {
        return self.getTestBoundingBox();
    }
}
