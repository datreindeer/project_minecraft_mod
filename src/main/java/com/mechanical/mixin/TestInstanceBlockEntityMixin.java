package com.mechanical.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Game tests: force-load every chunk of a test's padded area, not only the chunks of its structure. Vanilla forces the
 * structure's chunks alone, so a test's entities a few blocks away often stand in a chunk that doesn't tick (depending on
 * where the random test grid lands), and wagons, carts and mobs there never move. And a TestArea (mechanical:test_area,
 * the 17-block box the tests build in) gets no barrier cage: its walls and ceiling would stand in the tests' ground,
 * shade it and shut in what should walk out. Only test instance blocks run this, so it does nothing in a normal game.
 */
@Mixin(TestInstanceBlockEntity.class)
public abstract class TestInstanceBlockEntityMixin {
    @Shadow
    private Optional<Holder.Reference<GameTestInstance>> getTestHolder() {
        throw new AssertionError();
    }

    @Redirect(method = "forceLoadChunks", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/TestInstanceBlockEntity;getStructureBoundingBox()Lnet/minecraft/world/level/levelgen/structure/BoundingBox;"))
    private BoundingBox mechanical$forcePaddedArea(TestInstanceBlockEntity self) {
        return self.getTestBoundingBox();
    }

    @Inject(method = "encaseStructure", at = @At("HEAD"), cancellable = true)
    private void mechanical$noCage(CallbackInfo ci) {
        if (getTestHolder().map(h -> h.value().structure().equals(com.mechanical.TestArea.STRUCTURE)).orElse(false)) ci.cancel();
    }
}
