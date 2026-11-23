package com.mechanical.folklore;

import java.util.ArrayList;
import java.util.List;

import com.mechanical.Mechanical;
import com.mechanical.wildlife.underground.Troll;
import com.mechanical.wildlife.underground.UndergroundFauna;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

/**
 * Bergatroll i skogen: in the taiga a troll sleeps as a mossy stone, two blocks of Mossy Troll Stone (worldgen
 * troll_stone_sleeper). Chop a log within {@link #WAKE_RANGE} blocks, or strike the stone itself, and it wakes: the stone is
 * gone and an ordinary Troll (its products and all) stands there, after whoever woke it. A forest troll goes back to
 * sleep as Mossy Troll Stone where it stands when it has nobody to chase, or in daylight after a while (Troll).
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class TrollStone {
    static final int WAKE_RANGE = 10;

    @SubscribeEvent
    static void onBreak(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getPlayer() instanceof ServerPlayer player)) return;
        if (event.getState().is(Beings.MOSSY_TROLL_STONE.get())) {
            event.setCanceled(true);
            wake(level, event.getPos(), player);
        } else if (event.getState().is(BlockTags.LOGS)) {
            wakeNear(level, event.getPos(), player);
        }
    }

    /** Every sleeping troll within {@link #WAKE_RANGE} of pos wakes. Returns the trolls. */
    public static List<Troll> wakeNear(ServerLevel level, BlockPos pos, Player player) {
        List<BlockPos> stones = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-WAKE_RANGE, -4, -WAKE_RANGE), pos.offset(WAKE_RANGE, 4, WAKE_RANGE)))
            if (level.getBlockState(p).is(Beings.MOSSY_TROLL_STONE.get())) stones.add(p.immutable());
        List<Troll> trolls = new ArrayList<>();
        for (BlockPos p : stones) {
            Troll t = wake(level, p, player);
            if (t != null) trolls.add(t);
        }
        return trolls;
    }

    /** The troll whose stone is at pos (either block) wakes; null if that stone is already gone. */
    static Troll wake(ServerLevel level, BlockPos pos, Player player) {
        BlockPos feet = pos;
        while (level.getBlockState(feet.below()).is(Beings.MOSSY_TROLL_STONE.get())) feet = feet.below();
        if (!level.getBlockState(feet).is(Beings.MOSSY_TROLL_STONE.get())) return null;
        var state = level.getBlockState(feet);
        for (BlockPos p = feet; level.getBlockState(p).is(Beings.MOSSY_TROLL_STONE.get()); p = p.above()) level.removeBlock(p, false);
        Troll troll = UndergroundFauna.TROLL.get().create(level, EntitySpawnReason.TRIGGERED);
        if (troll == null) return null;
        troll.snapTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, level.getRandom().nextFloat() * 360, 0);
        troll.addTag(Troll.FOREST_TAG);
        if (player != null && !player.isCreative()) troll.setTarget(player);
        level.addFreshEntity(troll);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), feet.getX() + 0.5, feet.getY() + 1, feet.getZ() + 0.5, 40, 0.5, 1.0, 0.5, 0.1);
        level.playSound(null, feet, SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.5F, 0.6F);
        if (player instanceof ServerPlayer sp) Faith.award(sp, "folklore/troll_in_the_woods");
        return troll;
    }

    private TrollStone() {}
}
