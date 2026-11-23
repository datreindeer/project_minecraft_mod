package com.mechanical.wildlife.temperate;

import java.util.Comparator;
import java.util.EnumSet;

import com.mechanical.AnimalFear;
import com.mechanical.Calendar;
import com.mechanical.Mechanical;
import com.mechanical.wildlife.BrownBear;
import com.mechanical.wildlife.river.Otter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.fish.Salmon;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * The salmon run: all through Leaffall (the first month of autumn, {@link #RUN_MONTH}) the rivers fill with salmon
 * coming up from the sea. Near a player in a river biome, schools of salmon keep turning up ({@link #MAX_SALMON} within
 * {@link #CROWD_RANGE}); they leap out of the water now and then. Brown bears and otters come to the river for them and
 * take salmon out of the water ({@link FishSalmonGoal}).
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class SalmonRun {
    public static final int RUN_MONTH = 6;
    static final int MAX_SALMON = 14, CROWD_RANGE = 32, SPAWN_RANGE = 20, LEAP_CHANCE = 300;
    static final double BEAR_SEARCH = 32, OTTER_SEARCH = 16;

    private SalmonRun() {}

    public static boolean isRun(int month) {
        return month == RUN_MONTH;
    }

    public static boolean isRun(Level level) {
        return isRun(Calendar.month(level.getOverworldClockTime()));
    }

    /** Every 10 s near each player in a river biome during the run: tops the salmon up with a school of 3-5. */
    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD || level.getGameTime() % 200 != 50
                || !isRun(level)) return;
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || !level.getBiome(player.blockPosition()).is(BiomeTags.IS_RIVER)) continue;
            if (level.getEntitiesOfClass(Salmon.class, player.getBoundingBox().inflate(CROWD_RANGE)).size() >= MAX_SALMON) continue;
            spawnSchool(level, player.blockPosition(), 3 + level.getRandom().nextInt(3));
        }
    }

    /** Puts {@code count} salmon into one patch of river water near {@code around}; false if no water was found. */
    static boolean spawnSchool(ServerLevel level, BlockPos around, int count) {
        for (int tries = 0; tries < 10; tries++) {
            int x = around.getX() + level.getRandom().nextInt(SPAWN_RANGE * 2 + 1) - SPAWN_RANGE;
            int z = around.getZ() + level.getRandom().nextInt(SPAWN_RANGE * 2 + 1) - SPAWN_RANGE;
            BlockPos top = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, new BlockPos(x, 0, z)).below();
            if (!level.getFluidState(top).is(FluidTags.WATER) || !level.getFluidState(top.below()).is(FluidTags.WATER)) continue;
            for (int i = 0; i < count; i++) {
                Salmon salmon = EntityTypes.SALMON.create(level, EntitySpawnReason.NATURAL);
                if (salmon == null) return false;
                salmon.snapTo(x + level.getRandom().nextDouble(), top.getY() - 0.5, z + level.getRandom().nextDouble(), level.getRandom().nextFloat() * 360, 0);
                level.addFreshEntity(salmon);
            }
            return true;
        }
        return false;
    }

    /** Running salmon leap out of the water now and then. */
    @SubscribeEvent
    static void onSalmonTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Salmon salmon) || !(salmon.level() instanceof ServerLevel level)
                || (salmon.tickCount + salmon.getId()) % 20 != 0 || !salmon.isInWater() || level.getRandom().nextInt(LEAP_CHANCE / 20) != 0
                || !isRun(level) || level.getFluidState(salmon.blockPosition().above()).is(FluidTags.WATER)) return;
        Vec3 fwd = Vec3.directionFromRotation(0, salmon.getYRot()).scale(0.25);
        salmon.setDeltaMovement(fwd.x, 0.55, fwd.z);
        level.sendParticles(ParticleTypes.SPLASH, salmon.getX(), salmon.getY() + 0.3, salmon.getZ(), 6, 0.2, 0.05, 0.2, 0.05);
        salmon.playSound(SoundEvents.SALMON_FLOP, 0.6F, 1.0F);
    }

    @SubscribeEvent
    static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof BrownBear bear) bear.goalSelector.addGoal(4, new FishSalmonGoal(bear, BEAR_SEARCH, 2.2));
        else if (event.getEntity() instanceof Otter otter) otter.goalSelector.addGoal(4, new FishSalmonGoal(otter, OTTER_SEARCH, 1.6));
    }

    /** During the run: goes down to the river, wades or swims to a salmon and takes it (the fish is eaten on the spot). */
    public static class FishSalmonGoal extends Goal {
        private final PathfinderMob hunter;
        private final double search, reach;
        private Salmon fish;
        private int ticks, cooldown, repath;

        FishSalmonGoal(PathfinderMob hunter, double search, double reach) {
            this.hunter = hunter;
            this.search = search;
            this.reach = reach;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (--cooldown > 0 || (hunter.tickCount + hunter.getId()) % 20 != 0 || hunter.isBaby() || hunter.getTarget() != null
                    || AnimalFear.isPanicking(hunter) || hunter instanceof com.mechanical.wildlife.WildAnimal w && w.isDozing() || !isRun(hunter.level())) return false;
            fish = hunter.level().getEntitiesOfClass(Salmon.class, hunter.getBoundingBox().inflate(search, 8, search), s -> s.isAlive() && s.isInWater())
                    .stream().min(Comparator.comparingDouble(hunter::distanceToSqr)).orElse(null);
            return fish != null;
        }

        @Override
        public void start() {
            ticks = 0;
        }

        /** Goes for this salmon straight away (tests). */
        void forceTarget(Salmon salmon) {
            fish = salmon;
            start();
        }

        @Override
        public boolean canContinueToUse() {
            return ++ticks < 600 && fish.isAlive() && hunter.getTarget() == null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            hunter.getLookControl().setLookAt(fish);
            if (hunter.distanceTo(fish) < reach) {
                catchIt();
                return;
            }
            if (++repath % 20 == 1 || hunter.getNavigation().isDone()) hunter.getNavigation().moveTo(fish, 1.1);
        }

        /** The salmon is taken (and eaten): it is gone, the hunter heals a little. */
        void catchIt() {
            if (hunter.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.SPLASH, fish.getX(), fish.getY() + 0.3, fish.getZ(), 12, 0.3, 0.1, 0.3, 0.1);
            }
            hunter.playSound(SoundEvents.GENERIC_EAT.value(), 0.8F, 0.9F);
            fish.discard();
            hunter.heal(4.0F);
            cooldown = 400;
        }
    }
}
