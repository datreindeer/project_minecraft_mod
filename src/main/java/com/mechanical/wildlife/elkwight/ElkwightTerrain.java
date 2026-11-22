package com.mechanical.wildlife.elkwight;

import java.util.HashSet;
import java.util.Set;

import com.mechanical.TreeFelling;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * What the Elkwight does to the ground: smashing through natural blocks when he has no path, and the Ground Wave.
 * <p>
 * Natural means: in the block tag mechanical:elkwight_breakable (stone, deepslate, earth, sand, gravel, snow, leaves ...;
 * the Scorched Barrens' charred trunks belong there too), leaves only when not player-placed (vanilla's PERSISTENT flag),
 * and logs only as part of a natural tree ({@link TreeFelling#find}: a tree with its own, non-persistent leaves).
 * shortcut: the mod keeps no record of player-placed blocks, so raw stone or dirt a player built with counts as natural;
 * add a placed-block tracker if bases of raw stone/dirt get smashed.
 */
public final class ElkwightTerrain {
    /** A Ground Wave: its front, heading (flat, unit) and blocks left to run; the entities it already threw. */
    public static final class Wave {
        Vec3 at;
        final Vec3 dir;
        int left;
        final Set<Integer> hit = new HashSet<>();

        public Wave(Vec3 at, Vec3 dir, int range) {
            this.at = at;
            this.dir = dir;
            this.left = range;
        }
    }

    static final float WAVE_DAMAGE = 12;
    static final double WAVE_LIFT = 0.95;

    /** May he smash this block? See the class comment. */
    public static boolean canBreak(Level level, BlockPos pos, BlockState state) {
        if (state.isAir() || state.getDestroySpeed(level, pos) < 0) return false;
        if (state.getBlock() instanceof LeavesBlock) return !state.getValue(LeavesBlock.PERSISTENT);
        if (state.is(BlockTags.LOGS)) return state.is(Elkwights.BREAKABLE) || TreeFelling.find(level, pos) != null;
        return state.is(Elkwights.BREAKABLE);
    }

    /** The box he would smash moving a block towards {@code to}: ahead, and below him too when it is lower. */
    private static AABB smashBox(LivingEntity e, Vec3 to) {
        Vec3 d = to.subtract(e.position());
        Vec3 flat = new Vec3(d.x, 0, d.z);
        AABB box = e.getBoundingBox();
        AABB ahead = flat.lengthSqr() > 0.25 ? box.move(flat.normalize()) : box;
        if (d.y < -1.5 && flat.length() < 2.5) ahead = ahead.minmax(box.move(0, -1, 0)); // right over it: dig down
        else if (d.y > 1.5) ahead = ahead.minmax(box.move(0, 1, 0));
        return ahead.deflate(0.05);
    }

    /** Is there something natural in his way towards {@code to} that he could smash (and nothing built he'd have to)? */
    static boolean breakableToward(ServerLevel level, LivingEntity e, Vec3 to) {
        boolean any = false;
        for (BlockPos p : BlockPos.betweenClosed(BlockPos.containing(smashBox(e, to).getMinPosition()), BlockPos.containing(smashBox(e, to).getMaxPosition()))) {
            BlockState s = level.getBlockState(p);
            if (s.getCollisionShape(level, p).isEmpty()) continue;
            if (!canBreak(level, p, s)) return false;
            any = true;
        }
        return any;
    }

    /** Smashes the natural blocks in his way towards {@code to} (no drops, so a fight leaves no piles of loot). */
    static void smashToward(ServerLevel level, LivingEntity e, Vec3 to) {
        AABB box = smashBox(e, to);
        for (BlockPos p : BlockPos.betweenClosed(BlockPos.containing(box.getMinPosition()), BlockPos.containing(box.getMaxPosition()))) {
            BlockState s = level.getBlockState(p);
            if (canBreak(level, p, s)) level.destroyBlock(p, false, e);
        }
    }

    /** Charging into leaves, snow and the like: they give way. False if anything solid that isn't soft is in {@code box}. */
    static boolean smashSoft(ServerLevel level, LivingEntity e, AABB box) {
        boolean any = false;
        for (BlockPos p : BlockPos.betweenClosed(BlockPos.containing(box.getMinPosition()), BlockPos.containing(box.getMaxPosition()))) {
            BlockState s = level.getBlockState(p);
            if (s.getCollisionShape(level, p).isEmpty()) continue;
            boolean soft = canBreak(level, p, s) && (s.getBlock() instanceof LeavesBlock || s.getDestroySpeed(level, p) <= 0.5F);
            if (!soft) return false;
            any = true;
        }
        if (any) for (BlockPos p : BlockPos.betweenClosed(BlockPos.containing(box.getMinPosition()), BlockPos.containing(box.getMaxPosition())))
            if (canBreak(level, p, level.getBlockState(p))) level.destroyBlock(p, false, e);
        return any;
    }

    /** Room for his body where he'd land beside {@code at}. */
    static boolean landingFree(ServerLevel level, LivingEntity e, Vec3 at) {
        for (int i = 0; i < 9; i++) {
            Vec3 spot = at.add(i % 3 - 1, 0.1, i / 3 - 1);
            if (level.noCollision(e, e.getDimensions(e.getPose()).makeBoundingBox(spot))) return true;
        }
        return false;
    }

    /**
     * Velocity that carries him from {@code from} onto {@code to} (gravity 0.08/tick, air drag 0.98 vertical and 0.91
     * horizontal per tick): an arc topping out two blocks above the higher end.
     */
    static Vec3 leapVelocity(Vec3 from, Vec3 to) {
        double dy = to.y - from.y, apex = Math.max(dy, 0) + 2.0;
        double vy = Math.sqrt(2 * 0.08 * apex) * 1.12; // a little over, for the drag
        double up = vy / 0.08, down = Math.sqrt(2 * Math.max(0.5, apex - dy) / 0.08);
        double ticks = up + down;
        Vec3 flat = new Vec3(to.x - from.x, 0, to.z - from.z);
        double v = flat.length() * 0.09 / (1 - Math.pow(0.91, ticks));
        Vec3 h = flat.lengthSqr() > 1e-4 ? flat.normalize().scale(Math.min(v, 1.6)) : Vec3.ZERO;
        return new Vec3(h.x, vy, h.z);
    }

    /** One tick of a Ground Wave: a block further, cracking the ground and throwing up who stands there. False when spent. */
    static boolean tickWave(ServerLevel level, Elkwight owner, Wave w) {
        if (w.left-- <= 0) return false;
        w.at = w.at.add(w.dir);
        // find the ground near the wave's height (it follows slopes a little, and dies at a cliff)
        BlockPos ground = null;
        BlockPos top = BlockPos.containing(w.at.x, w.at.y + 2, w.at.z);
        for (int dy = 0; dy <= 5; dy++) {
            BlockPos p = top.below(dy);
            if (!level.getBlockState(p).getCollisionShape(level, p).isEmpty() && level.getBlockState(p.above()).getCollisionShape(level, p.above()).isEmpty()) {
                ground = p;
                break;
            }
        }
        if (ground == null) return false;
        w.at = new Vec3(w.at.x, ground.getY() + 1, w.at.z);
        BlockState g = level.getBlockState(ground);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, g), w.at.x, w.at.y + 0.1, w.at.z, 14, 0.6, 0.2, 0.6, 0.2);
        if (w.left % 3 == 0) level.playSound(null, ground, SoundEvents.DEEPSLATE_BREAK, SoundSource.HOSTILE, 1.5F, 0.6F);
        Vec3 side = new Vec3(-w.dir.z, 0, w.dir.x);
        Direction fall = Direction.getApproximateNearest(w.dir.x, 0, w.dir.z);
        for (int s = -1; s <= 1; s++) {
            BlockPos p = BlockPos.containing(w.at.x + side.x * s, ground.getY(), w.at.z + side.z * s);
            crack(level, p);
            for (int up = 1; up <= 2; up++) { // a natural tree in the way is felled away from him
                BlockPos log = p.above(up);
                if (level.getBlockState(log).is(BlockTags.LOGS)) {
                    TreeFelling.Tree tree = TreeFelling.find(level, log);
                    if (tree != null) TreeFelling.fell(level, log, tree, fall, owner, ItemStack.EMPTY);
                    break;
                }
            }
        }
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(w.at, w.at).inflate(1.6, 2.5, 1.6), e -> e != owner && e.isAlive())) {
            if (!w.hit.add(e.getId())) continue;
            owner.strike(level, e, WAVE_DAMAGE, 0.3, WAVE_LIFT);
        }
        return true;
    }

    /** Natural stone cracks under the wave: to cobblestone, or gravel one time in three. Nothing else changes. */
    static void crack(ServerLevel level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        if (!s.is(BlockTags.BASE_STONE_OVERWORLD) || !canBreak(level, p, s)) return;
        level.setBlockAndUpdate(p, level.getRandom().nextInt(3) == 0 ? Blocks.GRAVEL.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState());
    }

    private ElkwightTerrain() {}
}
