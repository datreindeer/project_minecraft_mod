package com.mechanical.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Game tests: a test starts only once every chunk of its padded area has its entities loaded and ticking (vanilla waits for
 * the structure's chunks alone). Pairs with TestInstanceBlockEntityMixin, which force-loads those chunks; without the wait
 * a test's first tick can spawn entities into chunks still loading, where lookups don't find them.
 */
@Mixin(GameTestInfo.class)
public abstract class GameTestInfoMixin {
    @Redirect(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/TestInstanceBlockEntity;getStructureBoundingBox()Lnet/minecraft/world/level/levelgen/structure/BoundingBox;"))
    private BoundingBox mechanical$waitForPaddedArea(TestInstanceBlockEntity test) {
        return test.getTestBoundingBox();
    }
}
