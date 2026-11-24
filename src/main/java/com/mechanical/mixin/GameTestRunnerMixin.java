package com.mechanical.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Game test server: before each batch every entity left by the batches before it goes (mobs, carcasses, caravans, items
 * that walked or were dragged out of their test's box). Batches run one after another, and a new test's force-loaded chunks
 * reach next to older tests, whose leftovers then tick again and wander in, or are smelled, hunted or eaten by the new
 * test's mobs. And every batch starts at noon of the same day: animals sleep by night and predators by day, so a test
 * that ran whenever the clock happened to stand failed now and then; a test that wants the night sets it. Only on the game
 * test server: /test in a real world keeps its entities and its time.
 */
@Mixin(GameTestRunner.class)
public abstract class GameTestRunnerMixin {
    @Shadow
    @Final
    private MinecraftServer server;

    @Inject(method = "runBatch", at = @At("HEAD"))
    private void mechanical$clearLeftovers(int batchIndex, CallbackInfo ci) {
        if (!(server instanceof GameTestServer)) return;
        for (ServerLevel level : server.getAllLevels()) {
            java.util.List<Entity> left = new java.util.ArrayList<>();
            level.getAllEntities().forEach(e -> {
                if (!(e instanceof Player)) left.add(e);
            });
            left.forEach(e -> e.discard());
        }
        com.mechanical.Calendar.overworldClock(server).ifPresent(c -> server.clockManager().setTotalTicks(c,
                Math.floorDiv(server.clockManager().getInstance(c).totalTicks(), 24000L) * 24000L + 6000L));
    }
}
