package com.mechanical.folklore;

import com.mechanical.Calendar;
import com.mechanical.wildlife.forestbirds.SoaringBird;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Mountain Watcher: a great silver raptor with very long, rune-marked wings that wheels over the peaks like the golden
 * eagle (SoaringBird), shedding a faint glimmer from its wingtips. Spawns only on open ground at y {@link #MIN_Y}+ in clear weather by day. Only players with sight
 * {@link Faith#WATCHER_SIGHT}+ see it (client.folklore.FolkloreClient.WatcherRenderer); it cannot be hurt, hit or pushed, makes
 * no sound (that would give it away) and leaves no carcass. Once a calendar year a player who can see it and comes within
 * {@link #FEATHER_RANGE} blocks gets a Watcher Feather floating down onto them (not from the bird: it could land anywhere on a peak), and it flies off.
 */
public class MountainWatcher extends SoaringBird {
    static final Kind KIND = new Kind("mountain_watcher", 0.45, 18, 36, 1200, 2400, 200, 600, false, false, false, 1.1, () -> null, 1.0F);
    public static final int MIN_Y = 150;
    static final double FEATHER_RANGE = 24, SEEN_RANGE = 64;
    /** Blocks from its middle out to a wingtip, as drawn. */
    static final double WINGTIP = 3.2;

    public MountainWatcher(EntityType<? extends MountainWatcher> type, Level level) {
        super(type, level, KIND);
        setSilent(true);
    }

    /** Open ground (air, solid non-leaf ground, open sky, daylight) at y 150+, no rain, by day. */
    static boolean canSpawn(LevelAccessor level, BlockPos pos) {
        var below = level.getBlockState(pos.below());
        if (pos.getY() < MIN_Y || !level.getBlockState(pos).isAir() || !below.isSolid() || below.is(BlockTags.LEAVES) || !level.canSeeSky(pos)
                || level.getRawBrightness(pos, 0) <= 8) return false;
        return !(level instanceof ServerLevelAccessor s) || !s.getLevel().isRaining() && !s.getLevel().isDarkOutside();
    }

    /** On the client of a player who can see it: now and then a mote of light left hanging behind one wingtip. */
    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide() || !isFlying() || random.nextInt(5) != 0 || Faith.localSight() < Faith.WATCHER_SIGHT) return;
        double side = random.nextBoolean() ? WINGTIP : -WINGTIP, yaw = Math.toRadians(yBodyRot);
        level().addParticle(ParticleTypes.END_ROD, getX() + Math.cos(yaw) * side + Math.sin(yaw) * 0.4, getY() + 0.7,
                getZ() + Math.sin(yaw) * side - Math.cos(yaw) * 0.4, 0, -0.01, 0);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeHitByProjectile() {
        return false;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if ((tickCount + getId()) % 20 != 0) return;
        for (ServerPlayer p : level.getPlayers(p -> p.distanceToSqr(this) < SEEN_RANGE * SEEN_RANGE && Faith.canSee(p, Faith.WATCHER_SIGHT))) {
            Faith.seen(p, "mountain_watcher", "folklore/above_the_clouds");
            if (distanceTo(p) < FEATHER_RANGE && Faith.takeFeather(p, Calendar.year(level.getOverworldClockTime()))) {
                level.addFreshEntity(new ItemEntity(level, p.getX(), p.getY() + 3, p.getZ(), Folklore.WATCHER_FEATHER.toStack(), 0, -0.05, 0));
                takeOff(true, p.position());
            }
        }
    }
}
