package com.mechanical;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.pathfinder.Path;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * What makes the night dangerous, and fire the only real safety:
 * - monsters keep away from burning fires (campfires, lit kilns), won't target players close to one and don't spawn near one;
 * - zombie hordes roam on blood moon nights (two waves): they go for the lights first (put out fires, smash torches and lanterns), then the players;
 * - torches give no protection, instead they draw monsters in, which smash them;
 * - more monsters at night, some of which stalk the player (circling at a distance) before they attack;
 * - monsters take no knockback, they keep pushing on; and they work their way around obstacles instead of standing still.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class NightDangers {
    /** Monsters stay this far from a burning fire; skeleton range is 15, so a player within SAFE of the fire is out of reach. */
    private static final double AVOID_RADIUS = 20, SAFE_RADIUS = 6, NO_SPAWN_RADIUS = 28;
    private static final int FIRE_STALE_TICKS = 40;
    /** Hordes only come on blood moon nights (BloodMoon), in two waves. */
    private static final int HORDE_MIN = 8, HORDE_MAX = 12;
    private static final String HORDE_TAG = "mechanical.horde";
    /** Extra night spawns: chance per player every 5 s, while fewer than NEARBY_CAP monsters are within 48 blocks. */
    private static final float EXTRA_SPAWN_CHANCE = 0.45F, BLOOD_MOON_SPAWN_CHANCE = 0.9F;
    private static final int NEARBY_CAP = 12, BLOOD_MOON_CAP = 20;
    /** Some zombies and creepers stalk: they circle the player for a while (30-70 s) before attacking. */
    private static final float LURK_CHANCE = 0.4F;
    private static final String LURKER_TAG = "mechanical.lurker", ROLLED_TAG = "mechanical.rolled";
    private static final Map<Monster, Long> LURK_END = new WeakHashMap<>();

    // ---- registry of burning fires: fires report themselves every tick while lit ----

    private static final Map<Level, Map<BlockPos, Long>> FIRES = new WeakHashMap<>();

    /** Called every tick by a burning campfire or lit kiln. */
    public static void markLit(Level level, BlockPos pos) {
        FIRES.computeIfAbsent(level, l -> new HashMap<>()).put(pos.immutable(), level.getGameTime());
    }

    /** The nearest burning fire within {@code radius}, or null. */
    public static BlockPos nearestFire(Level level, Vec3 at, double radius) {
        Map<BlockPos, Long> fires = FIRES.get(level);
        if (fires == null) return null;
        long now = level.getGameTime();
        BlockPos best = null;
        double bestDist = radius * radius;
        for (Iterator<Map.Entry<BlockPos, Long>> it = fires.entrySet().iterator(); it.hasNext();) {
            Map.Entry<BlockPos, Long> e = it.next();
            if (now - e.getValue() > FIRE_STALE_TICKS) { it.remove(); continue; } // went out or was removed
            double d = Vec3.atCenterOf(e.getKey()).distanceToSqr(at);
            if (d < bestDist) { bestDist = d; best = e.getKey(); }
        }
        return best;
    }

    public static boolean nearFire(Level level, Vec3 at, double radius) {
        return nearestFire(level, at, radius) != null;
    }

    // ---- monsters ----

    @SubscribeEvent
    static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Monster monster)) return;
        boolean horde = monster.entityTags().contains(HORDE_TAG);
        if (horde) monster.goalSelector.addGoal(1, new PutOutLightsGoal(monster));
        else monster.goalSelector.addGoal(0, new AvoidFireGoal(monster));
        if (monster instanceof PathfinderMob) {
            monster.getNavigation().setMaxVisitedNodesMultiplier(4.0F); // finds longer ways round
            monster.goalSelector.addGoal(1, new UnstuckGoal(monster));
        }
        if (!monster.entityTags().contains(ROLLED_TAG)) {
            monster.addTag(ROLLED_TAG);
            if ((monster instanceof Zombie || monster instanceof Creeper) && monster.getRandom().nextFloat() < LURK_CHANCE) monster.addTag(LURKER_TAG);
        }
        if (monster.entityTags().contains(LURKER_TAG)) monster.goalSelector.addGoal(3, new LurkGoal(monster));
        monster.goalSelector.addGoal(4, new SmashTorchGoal(monster));
        if (horde) monster.goalSelector.addGoal(5, new FollowHordeGoal(monster));
        // blood moon: torches within reach get smashed even mid-chase, and melee walkers break down wooden doors
        monster.goalSelector.addGoal(2, new BloodMoonTorchesGoal(monster));
        if (monster.getNavigation() instanceof GroundPathNavigation && !(monster instanceof Creeper)
                && !(monster instanceof net.minecraft.world.entity.monster.skeleton.AbstractSkeleton) && !(monster instanceof net.minecraft.world.entity.monster.Enderman)) {
            monster.goalSelector.addGoal(1, new BloodMoonDoorGoal(monster));
        }
    }

    /** Monsters don't pick (or keep) a player standing by a fire (a horde puts the fire out first). Stalkers hold off until their time is up. */
    @SubscribeEvent
    static void onTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Monster monster) || !(event.getNewAboutToBeSetTarget() instanceof Player player)) return;
        if (nearFire(player.level(), player.position(), SAFE_RADIUS)) {
            event.setCanceled(true);
        } else if (monster.entityTags().contains(LURKER_TAG) && monster.getLastHurtByMob() != player && monster.distanceTo(player) > 5) {
            event.setCanceled(true);
        }
    }

    /** No knockback: monsters keep pushing towards you. */
    @SubscribeEvent
    static void onKnockback(LivingKnockBackEvent event) {
        if (event.getEntity() instanceof Monster) event.setCanceled(true);
    }

    /** No monsters spawn close to a burning fire. */
    @SubscribeEvent
    static void onSpawn(FinalizeSpawnEvent event) {
        if (event.getEntity() instanceof Monster && event.getSpawnType() == EntitySpawnReason.NATURAL
                && nearFire(event.getEntity().level(), new Vec3(event.getX(), event.getY(), event.getZ()), NO_SPAWN_RADIUS)) {
            event.setSpawnCancelled(true);
        }
    }

    /** Walks out of the fire's reach and drops a target that is sheltering by it. */
    static class AvoidFireGoal extends Goal {
        private final Monster mob;
        private BlockPos fire;

        AvoidFireGoal(Monster mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (mob.tickCount % 10 != 0) return false;
            fire = nearestFire(mob.level(), mob.position(), AVOID_RADIUS);
            if (fire == null) return false;
            if (mob.getTarget() != null && nearFire(mob.level(), mob.getTarget().position(), SAFE_RADIUS)) mob.setTarget(null);
            Vec3 away = DefaultRandomPos.getPosAway(mob, (int) AVOID_RADIUS + 4, 7, Vec3.atCenterOf(fire));
            return away != null && mob.getNavigation().moveTo(away.x, away.y, away.z, 1.2);
        }

        @Override
        public boolean canContinueToUse() {
            return !mob.getNavigation().isDone();
        }
    }

    /** With nothing to chase, a monster may wander to a torch nearby and smash it. */
    static class SmashTorchGoal extends Goal {
        private static final int SEARCH = 8;
        private static final float CHANCE = 0.3F;
        private final Monster mob;
        private BlockPos torch;

        SmashTorchGoal(Monster mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            // a blood moon makes them go for every torch around, often
            boolean bloodMoon = BloodMoon.isActive(mob.level());
            if (mob.getTarget() != null || mob.tickCount % (bloodMoon ? 20 : 100) != 0 || (!bloodMoon && mob.getRandom().nextFloat() > CHANCE)) return false;
            torch = findTorch();
            return torch != null && mob.getNavigation().moveTo(torch.getX() + 0.5, torch.getY(), torch.getZ() + 0.5, 1.0);
        }

        private BlockPos findTorch() {
            BlockPos origin = mob.blockPosition();
            for (BlockPos p : BlockPos.betweenClosed(origin.offset(-SEARCH, -4, -SEARCH), origin.offset(SEARCH, 4, SEARCH))) {
                var state = mob.level().getBlockState(p);
                if (state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH)) return p.immutable();
            }
            return null;
        }

        @Override
        public boolean canContinueToUse() {
            return mob.getTarget() == null && torch != null && mob.level().getBlockState(torch).getBlock() != Blocks.AIR && !mob.getNavigation().isDone();
        }

        @Override
        public void tick() {
            if (torch != null && mob.distanceToSqr(Vec3.atCenterOf(torch)) < 2.5 * 2.5) {
                mob.getLookControl().setLookAt(Vec3.atCenterOf(torch));
                mob.level().destroyBlock(torch, false, mob);
                torch = null;
            }
        }

        @Override
        public void stop() {
            if (torch != null && mob.distanceToSqr(Vec3.atCenterOf(torch)) < 2.5 * 2.5) tick();
        }
    }

    /** Blood moon: smashes any torch within arm's reach every half second, also while chasing (no flags, so it never blocks the chase). */
    static class BloodMoonTorchesGoal extends Goal {
        private final Monster mob;

        BloodMoonTorchesGoal(Monster mob) {
            this.mob = mob;
        }

        @Override
        public boolean canUse() {
            return mob.tickCount % 10 == 0 && BloodMoon.isActive(mob.level());
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            BlockPos origin = mob.blockPosition();
            for (BlockPos p : BlockPos.betweenClosed(origin.offset(-2, -1, -2), origin.offset(2, 2, 2))) {
                var state = mob.level().getBlockState(p);
                if (!(state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH)) || mob.distanceToSqr(Vec3.atCenterOf(p)) > 2.5 * 2.5) continue;
                mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT);
                mob.level().destroyBlock(p, false, mob);
                return;
            }
        }
    }

    /**
     * Blood moon: walks paths through wooden doors and breaks them down (vanilla's door breaking, which normally only
     * zombies on Hard do). Door paths are switched on only during the blood moon, so on other nights monsters don't
     * plan routes through doors they can't open.
     */
    static class BloodMoonDoorGoal extends net.minecraft.world.entity.ai.goal.BreakDoorGoal {
        BloodMoonDoorGoal(Monster mob) {
            super(mob, d -> true);
        }

        @Override
        public boolean canUse() {
            boolean active = BloodMoon.isActive(mob.level());
            // zombies that break doors on Hard anyway keep their own door paths
            boolean ownDoors = mob instanceof Zombie zombie && zombie.canBreakDoors();
            if (!ownDoors && mob.getNavigation() instanceof GroundPathNavigation nav && nav.getNodeEvaluator().canOpenDoors() != active) nav.setCanOpenDoors(active);
            return active && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return BloodMoon.isActive(mob.level()) && super.canContinueToUse();
        }
    }

    /**
     * Has a target but hasn't moved for ~2 s (no path, or blocked): try a full path, then a partial path towards it, and
     * finally walk straight at it, jumping when it bumps into something. The normal attack goal takes over again after.
     */
    static class UnstuckGoal extends Goal {
        private final Monster mob;
        private Vec3 lastPos = Vec3.ZERO;
        private int still, ticks;
        private boolean direct;

        UnstuckGoal(Monster mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = mob.getTarget();
            if (target == null || mob.tickCount % 20 != 0) return false;
            boolean stuck = mob.position().distanceToSqr(lastPos) < 0.5 && mob.distanceTo(target) > 3;
            lastPos = mob.position();
            still = stuck ? still + 1 : 0;
            return still >= 2;
        }

        @Override
        public void start() {
            ticks = 0;
            direct = false;
            LivingEntity target = mob.getTarget();
            Path path = mob.getNavigation().createPath(target, 1);
            if (path == null || !path.canReach()) path = mob.getNavigation().createPath(target.blockPosition(), 4);
            if (path != null && path.getNodeCount() > 1) mob.getNavigation().moveTo(path, 1.1);
            else direct = true;
        }

        @Override
        public boolean canContinueToUse() {
            return ticks < 60 && mob.getTarget() != null && mob.getTarget().isAlive();
        }

        @Override
        public void tick() {
            ticks++;
            LivingEntity target = mob.getTarget();
            if (target == null) return;
            mob.getLookControl().setLookAt(target);
            if (direct || mob.getNavigation().isDone()) {
                mob.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.0);
                if (mob.horizontalCollision && mob.onGround()) mob.getJumpControl().jump();
            }
        }

        @Override
        public void stop() {
            still = 0;
        }
    }

    /**
     * Stalker: with a player in range it keeps its distance (12-18 blocks), drifting round them and watching, and after a
     * while (or when hurt, or the player comes close) it attacks for real.
     */
    static class LurkGoal extends Goal {
        private final Monster mob;
        private Player player;

        LurkGoal(Monster mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (mob.getTarget() != null || mob.tickCount % 20 != 0) return false;
            player = mob.level().getNearestPlayer(mob, 40);
            if (player == null || player.isCreative() || player.isSpectator()) return false;
            long now = mob.level().getGameTime();
            long end = LURK_END.computeIfAbsent(mob, m -> now + 600 + m.getRandom().nextInt(800));
            if (now >= end) {
                // done stalking: attack
                mob.removeTag(LURKER_TAG);
                LURK_END.remove(mob);
                mob.setTarget(player);
                return false;
            }
            if (nearFire(mob.level(), player.position(), AVOID_RADIUS)) return false;
            double angle = Math.atan2(mob.getZ() - player.getZ(), mob.getX() - player.getX()) + (mob.getRandom().nextDouble() - 0.5) * 1.6;
            double radius = 12 + mob.getRandom().nextDouble() * 6;
            return mob.getNavigation().moveTo(player.getX() + Math.cos(angle) * radius, player.getY(), player.getZ() + Math.sin(angle) * radius, 0.8);
        }

        @Override
        public boolean canContinueToUse() {
            return mob.getTarget() == null && !mob.getNavigation().isDone();
        }

        @Override
        public void tick() {
            if (player != null) mob.getLookControl().setLookAt(player);
        }
    }

    // ---- extra night spawns ----

    private static void spawnExtra(ServerLevel level, ServerPlayer player) {
        RandomSource random = level.getRandom();
        if (level.getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(48)).size() >= (BloodMoon.isActive(level) ? BLOOD_MOON_CAP : NEARBY_CAP)) return;
        double angle = random.nextDouble() * Math.PI * 2;
        double dist = 24 + random.nextInt(16);
        BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                BlockPos.containing(player.getX() + Math.cos(angle) * dist, player.getY(), player.getZ() + Math.sin(angle) * dist));
        // surface only, near the player's height, dark, dry, away from fire
        if (Math.abs(pos.getY() - player.getY()) > 16 || level.getBrightness(LightLayer.BLOCK, pos) > 0
                || !level.getFluidState(pos).isEmpty() || !level.getFluidState(pos.below()).isEmpty()
                || nearFire(level, Vec3.atCenterOf(pos), NO_SPAWN_RADIUS)) return;
        int roll = random.nextInt(100);
        EntityType<? extends Monster> type = roll < 50 ? EntityTypes.ZOMBIE : roll < 72 ? EntityTypes.SKELETON : roll < 88 ? EntityTypes.SPIDER : EntityTypes.CREEPER;
        Monster monster = type.create(level, EntitySpawnReason.NATURAL);
        if (monster == null) return;
        monster.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        if (!level.noCollision(monster)) return;
        monster.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.NATURAL, null);
        level.addFreshEntity(monster);
    }

    /**
     * Horde: makes for the nearest light (burning campfire or kiln within 48 blocks, torch or lantern within 10) and puts it
     * out; only when there is none left nearby does the horde turn on the players.
     */
    static class PutOutLightsGoal extends Goal {
        private static final double FIRE_SEARCH = 48;
        private static final int LAMP_SEARCH = 10;
        private final Monster mob;
        private BlockPos light;

        PutOutLightsGoal(Monster mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (mob.tickCount % 20 != 0) return false;
            light = findLight();
            if (light == null) return false;
            mob.setTarget(null); // lights first
            return mob.getNavigation().moveTo(light.getX() + 0.5, light.getY(), light.getZ() + 0.5, 1.1);
        }

        private BlockPos findLight() {
            Level level = mob.level();
            BlockPos fire = nearestFire(level, mob.position(), FIRE_SEARCH);
            if (fire != null && isLight(level.getBlockState(fire))) return fire;
            BlockPos origin = mob.blockPosition();
            for (BlockPos p : BlockPos.betweenClosed(origin.offset(-LAMP_SEARCH, -4, -LAMP_SEARCH), origin.offset(LAMP_SEARCH, 4, LAMP_SEARCH))) {
                if (isLamp(level.getBlockState(p))) return p.immutable();
            }
            return null;
        }

        private static boolean isLamp(net.minecraft.world.level.block.state.BlockState state) {
            return state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH) || state.is(Blocks.LANTERN) || state.is(Blocks.SOUL_LANTERN)
                    || state.is(Blocks.SOUL_TORCH) || state.is(Blocks.SOUL_WALL_TORCH)
                    || (state.getBlock() instanceof net.minecraft.world.level.block.AbstractCandleBlock && state.getValue(net.minecraft.world.level.block.AbstractCandleBlock.LIT));
        }

        private static boolean isLight(net.minecraft.world.level.block.state.BlockState state) {
            if (state.getBlock() instanceof net.minecraft.world.level.block.CampfireBlock) return state.getValue(net.minecraft.world.level.block.CampfireBlock.LIT);
            if (state.getBlock() instanceof KilnBlock) return state.getValue(KilnBlock.LIT);
            return isLamp(state);
        }

        @Override
        public boolean canContinueToUse() {
            return light != null && isLight(mob.level().getBlockState(light)) && !mob.getNavigation().isDone();
        }

        @Override
        public void tick() {
            if (light == null) return;
            mob.getLookControl().setLookAt(Vec3.atCenterOf(light));
            if (mob.distanceToSqr(Vec3.atCenterOf(light)) < 2.5 * 2.5) {
                putOut(mob, light);
                light = null;
            }
        }

        @Override
        public void stop() {
            if (light != null && mob.distanceToSqr(Vec3.atCenterOf(light)) < 2.5 * 2.5) putOut(mob, light);
        }

        private static void putOut(Monster mob, BlockPos pos) {
            Level level = mob.level();
            var state = level.getBlockState(pos);
            if (state.getBlock() instanceof net.minecraft.world.level.block.CampfireBlock) {
                level.setBlock(pos, state.setValue(net.minecraft.world.level.block.CampfireBlock.LIT, false)
                        .setValue(Campfires.EMBERS, false), net.minecraft.world.level.block.Block.UPDATE_ALL);
                level.playSound(null, pos, net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 0.8F);
            } else if (state.getBlock() instanceof KilnBlock) {
                if (level.getBlockEntity(KilnBlock.lowerPos(state, pos)) instanceof KilnBlockEntity kiln) kiln.extinguish();
                level.playSound(null, pos, net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 0.8F);
            } else if (state.getBlock() instanceof net.minecraft.world.level.block.AbstractCandleBlock) {
                net.minecraft.world.level.block.AbstractCandleBlock.extinguish(null, state, level, pos); // snuffed, not smashed
            } else if (isLamp(state)) {
                level.destroyBlock(pos, false, mob);
            }
            FIRES.getOrDefault(level, Map.of()).remove(pos);
        }
    }

    // ---- zombie hordes ----

    /** Night index of the last horde roll per level, so each night is rolled once. */
    private static final Map<Level, Long> ROLLED_NIGHT = new WeakHashMap<>();
    private static final Map<Level, ArrayDeque<Long>> HORDE_AT = new WeakHashMap<>();

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD || level.getGameTime() % 100 != 0) return;
        long time = level.getOverworldClockTime();
        long night = time / 24000;
        long dayTime = time % 24000;
        if (dayTime < 13000 || dayTime > 22500) return; // night only
        RandomSource random = level.getRandom();
        for (ServerPlayer player : level.players()) {
            float chance = BloodMoon.isActive(level) ? BLOOD_MOON_SPAWN_CHANCE : EXTRA_SPAWN_CHANCE;
            if (!player.isSpectator() && !player.isCreative() && random.nextFloat() < chance) spawnExtra(level, player);
        }
        // re-decided when /bloodmoon turns tonight into a blood moon after it was rolled
        if (!Long.valueOf(night).equals(ROLLED_NIGHT.get(level)) || (!HORDE_AT.containsKey(level) && BloodMoon.isBloodNight(level, night))) {
            ROLLED_NIGHT.put(level, night);
            // decide once per night: on a blood moon two horde waves, one early and one after midnight
            HORDE_AT.remove(level);
            if (BloodMoon.isBloodNight(level, night)) HORDE_AT.put(level, new ArrayDeque<>(List.of(
                    night * 24000 + 13500 + random.nextInt(3000), night * 24000 + 18000 + random.nextInt(3000))));
        }
        ArrayDeque<Long> waves = HORDE_AT.get(level);
        if (waves != null && !waves.isEmpty() && time >= waves.peek()) {
            waves.poll();
            List<ServerPlayer> players = new ArrayList<>(level.players().stream().filter(p -> !p.isSpectator() && !p.isCreative()).toList());
            if (!players.isEmpty()) spawnHorde(level, players.get(random.nextInt(players.size())));
        }
    }

    private static void spawnHorde(ServerLevel level, ServerPlayer near) {
        RandomSource random = level.getRandom();
        double angle = random.nextDouble() * Math.PI * 2;
        double dist = 32 + random.nextInt(16);
        BlockPos center = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                BlockPos.containing(near.getX() + Math.cos(angle) * dist, near.getY(), near.getZ() + Math.sin(angle) * dist));
        if (nearFire(level, Vec3.atCenterOf(center), NO_SPAWN_RADIUS)) return;
        int count = HORDE_MIN + random.nextInt(HORDE_MAX - HORDE_MIN + 1);
        for (int i = 0; i < count; i++) {
            BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, center.offset(random.nextInt(7) - 3, 0, random.nextInt(7) - 3));
            Zombie zombie = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.EVENT);
            if (zombie == null) continue;
            zombie.addTag(HORDE_TAG);
            zombie.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            zombie.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.EVENT, null);
            level.addFreshEntity(zombie);
        }
    }

    /** Horde members stick together and drift towards the nearest player when they have nothing to chase. */
    static class FollowHordeGoal extends Goal {
        private final Monster mob;

        FollowHordeGoal(Monster mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (mob.getTarget() != null || mob.tickCount % 40 != 0) return false;
            List<Monster> horde = mob.level().getEntitiesOfClass(Monster.class, mob.getBoundingBox().inflate(24),
                    o -> o != mob && o.entityTags().contains(HORDE_TAG));
            Vec3 goal;
            if (!horde.isEmpty() && mob.distanceTo(horde.get(0)) > 6) {
                goal = horde.get(0).position(); // close ranks
            } else {
                Player player = mob.level().getNearestPlayer(mob, 64);
                if (player == null || player.isCreative() || player.isSpectator()) return false;
                goal = player.position();
            }
            return mob.getNavigation().moveTo(goal.x, goal.y, goal.z, 0.9);
        }

        @Override
        public boolean canContinueToUse() {
            return mob.getTarget() == null && !mob.getNavigation().isDone();
        }
    }

    private NightDangers() {}
}
