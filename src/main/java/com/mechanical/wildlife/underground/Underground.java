package com.mechanical.wildlife.underground;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.mechanical.Mechanical;
import com.mechanical.Temperature;
import com.mechanical.wildlife.Predator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * The darkness rule for everything that lives underground (docs/underground-plan.md step 3): a creature only spawns in a
 * cave at block light 0 with no sky light, standing room and a sturdy floor, and the depth band decides which. Light is the
 * player's defence: a torch or campfire nearby (any block light at the spot) stops spawns there.
 * <p>
 * To add a creature: {@link #addBand} from the feature's init (bands are {@code minY <= y < maxY}). {@link #canSpawn} alone
 * is the rule for one spot, for spawners of their own.
 * <p>
 * Spawner: every {@link #INTERVAL} ticks each player in a cave ({@link #inCave}) has a {@link #CHANCE} roll for one group
 * 16-40 blocks away (up to 12 up or down), out of the player's sight-line distance (no player within 12 blocks of it), if
 * fewer than the band's {@code maxGroup} of that creature are already within 64 blocks. Spawned creatures carry
 * {@link Predator#NIGHT_TAG} so they may despawn far from players.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class Underground {
    /** One creature's depth band and group size. */
    public record Band(Supplier<? extends EntityType<? extends Mob>> type, int minY, int maxY, int weight, int minGroup, int maxGroup) {
        public boolean contains(int y) {
            return y >= minY && y < maxY;
        }
    }

    static final int INTERVAL = 200;
    static final float CHANCE = 0.3F;
    private static final double CROWD = 64, PLAYER_CLEAR = 12;
    private static final List<Band> BANDS = new ArrayList<>();

    /** A creature of the dark that never panics when hurt or startled (AnimalFear leaves it be): it fights or backs off by its own goals. */
    public interface Fearless {}

    /** Block light at which the folk of the dark (Vittra, Troll) keep away: about 7 blocks from a torch. */
    public static final int LIT = 7;

    /** In light the creatures of the dark shun: holding a torch or the like, or standing in block light {@link #LIT} or more. */
    public static boolean inLight(net.minecraft.world.entity.LivingEntity e) {
        return GiantCentipede.holdsLight(e) || e.level().getBrightness(LightLayer.BLOCK, e.blockPosition()) >= LIT;
    }

    /** Walks {@code mob} off, away from {@code from} (light, or whoever carries it). */
    static void backOff(net.minecraft.world.entity.PathfinderMob mob, net.minecraft.world.phys.Vec3 from, double speed) {
        mob.setTarget(null);
        net.minecraft.world.phys.Vec3 away = net.minecraft.world.entity.ai.util.DefaultRandomPos.getPosAway(mob, 12, 5, from);
        if (away != null) mob.getNavigation().moveTo(away.x, away.y, away.z, speed);
    }

    /** Registers a creature for the spawner (call during mod construction). */
    public static void addBand(Supplier<? extends EntityType<? extends Mob>> type, int minY, int maxY, int weight, int minGroup, int maxGroup) {
        BANDS.add(new Band(type, minY, maxY, weight, minGroup, maxGroup));
    }

    /** In a cave: the overworld, below {@link Temperature#CAVE_TOP}, no sky light. Underground creatures treat it as night. */
    public static boolean inCave(Level level, BlockPos pos) {
        return level.dimension() == Level.OVERWORLD && pos.getY() < Temperature.CAVE_TOP && level.getBrightness(LightLayer.SKY, pos) == 0;
    }

    /**
     * The shared spawn rule for one spot (the feet): {@code minY <= y < maxY}, total darkness (block light 0, sky light 0),
     * two blocks of open air without fluid, and a sturdy floor.
     */
    public static boolean canSpawn(LevelReader level, BlockPos pos, int minY, int maxY) {
        if (pos.getY() < minY || pos.getY() >= maxY) return false;
        if (level.getBrightness(LightLayer.BLOCK, pos) > 0 || level.getBrightness(LightLayer.SKY, pos) > 0) return false;
        BlockPos below = pos.below();
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() && level.getFluidState(pos).isEmpty()
                && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty() && level.getFluidState(pos.above()).isEmpty()
                && level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    /** A band for depth {@code y}, weighted, or null if none lives there. */
    static Band pick(int y, RandomSource random) {
        int total = 0;
        for (Band b : BANDS) if (b.contains(y)) total += b.weight();
        if (total == 0) return null;
        int r = random.nextInt(total);
        for (Band b : BANDS) {
            if (!b.contains(y)) continue;
            if ((r -= b.weight()) < 0) return b;
        }
        return null;
    }

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD || level.getGameTime() % INTERVAL != 0
                || !level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS)) return;
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.isCreative() || !inCave(level, player.blockPosition())) continue;
            if (level.getRandom().nextFloat() < CHANCE) trySpawnNear(level, player.blockPosition());
        }
    }

    /** One attempt at a group near {@code at}; returns how many were placed. */
    static int trySpawnNear(ServerLevel level, BlockPos at) {
        RandomSource random = level.getRandom();
        double angle = random.nextDouble() * Math.PI * 2, dist = 16 + random.nextInt(25);
        BlockPos start = BlockPos.containing(at.getX() + Math.cos(angle) * dist, at.getY() + random.nextInt(25) - 12, at.getZ() + Math.sin(angle) * dist);
        if (!level.isLoaded(start)) return 0;
        // drop down to a floor (at most 12 blocks)
        BlockPos pos = null;
        for (int dy = 0; dy < 12; dy++) {
            BlockPos p = start.below(dy);
            if (canSpawn(level, p, Integer.MIN_VALUE, Integer.MAX_VALUE)) {
                pos = p;
                break;
            }
        }
        if (pos == null || level.getNearestPlayer(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, PLAYER_CLEAR, false) != null) return 0;
        Band band = pick(pos.getY(), random);
        if (band == null) return 0;
        EntityType<? extends Mob> type = band.type().get();
        int near = level.getEntities(type, new AABB(pos).inflate(CROWD), e -> e.isAlive()).size();
        int count = Math.min(band.minGroup() + random.nextInt(band.maxGroup() - band.minGroup() + 1), band.maxGroup() - near);
        int placed = 0;
        for (int i = 0; i < count * 4 && placed < count; i++) {
            BlockPos p = placed == 0 ? pos : pos.offset(random.nextInt(7) - 3, random.nextInt(3) - 1, random.nextInt(7) - 3);
            if (!canSpawn(level, p, band.minY(), band.maxY())) continue;
            Mob mob = type.create(level, EntitySpawnReason.NATURAL);
            if (mob == null) return placed;
            mob.snapTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, random.nextFloat() * 360, 0);
            if (!level.noCollision(mob)) continue;
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(p), EntitySpawnReason.NATURAL, null);
            mob.addTag(Predator.NIGHT_TAG);
            level.addFreshEntity(mob);
            placed++;
        }
        return placed;
    }

    private Underground() {}
}
