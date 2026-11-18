package com.mechanical.worldgen;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.mechanical.AncientOak;
import com.mechanical.OfferingAltarBlockEntity;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * The Ancient Oak: a giant, unbreakable oak. A 7-wide flared trunk (hollow at the bottom: a room with an opening between two
 * root buttresses, the Offering Altar in the middle and a chimney above), thick roots running over and into the ground, and a
 * wide crown of curved limbs that split into ever thinner branches and twigs with leaf clusters at their ends.
 * Placed after the vanilla trees; trees standing in its footprint are removed first. Every write stays inside the 3x3 chunks
 * a feature may touch (wood within 13 blocks of the trunk, leaves within 16).
 */
final class AncientOakFeature extends StoneAgeFeature {
    static final MapCodec<AncientOakFeature> CODEC = MapCodec.unit(AncientOakFeature::new);
    private static final int TWIG = 0, BRANCH = 1, WOOD = 2;
    private static final double WOOD_REACH = 13, LEAF_REACH = 16;

    @Override
    public MapCodec<AncientOakFeature> codec() {
        return CODEC;
    }

    // per-placement state (features are singletons, but place() runs on one thread per call; keep it all local via this holder)
    private static final class Tree {
        final WorldGenLevel level;
        final int x0, z0, g, minX, minZ;
        final Map<BlockPos, Integer> wood = new HashMap<>(); // limbs, roots: thickness level
        final Map<BlockPos, Integer> links = new HashMap<>(); // branch connections, bit per Direction
        final Set<BlockPos> trunk = new HashSet<>();
        final Set<BlockPos> leaves = new HashSet<>();
        final Map<Long, Integer> groundCache = new HashMap<>();

        Tree(WorldGenLevel level, int x0, int z0, int g) {
            this.level = level;
            this.x0 = x0;
            this.z0 = z0;
            this.g = g;
            minX = (x0 >> 4 << 4) - 16;
            minZ = (z0 >> 4 << 4) - 16;
        }

        boolean safe(BlockPos p) {
            return p.getX() >= minX && p.getX() < minX + 48 && p.getZ() >= minZ && p.getZ() < minZ + 48
                    && !level.isOutsideBuildHeight(p.getY());
        }

        void set(BlockPos p, BlockState s) {
            if (safe(p)) StoneAgeFeature.set(level, p, s);
        }

        int ground(int x, int z) {
            return groundCache.computeIfAbsent(BlockPos.asLong(x, 0, z), k -> AncientOakFeature.ground(level, x, z));
        }

        boolean isTree(BlockPos p) {
            return trunk.contains(p) || wood.containsKey(p);
        }

        void wood(BlockPos p, int lvl) {
            if (Math.hypot(p.getX() - x0, p.getZ() - z0) > WOOD_REACH + 0.5) return;
            wood.merge(p, lvl, Math::max);
        }

        void link(BlockPos a, BlockPos b) {
            for (Direction d : Direction.values()) {
                if (a.relative(d).equals(b)) {
                    links.merge(a, 1 << d.ordinal(), (x, y) -> x | y);
                    links.merge(b, 1 << d.getOpposite().ordinal(), (x, y) -> x | y);
                }
            }
        }
    }

    /** First block above natural ground, looking through trees and plants. */
    static int ground(WorldGenLevel level, int x, int z) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
        for (int i = 0; i < 48 && p.getY() > level.getMinY(); i++) {
            BlockState below = level.getBlockState(p.below());
            boolean passable = below.isAir() || below.is(BlockTags.LOGS) || below.is(BlockTags.LEAVES)
                    || below.canBeReplaced() && below.getFluidState().isEmpty();
            if (!passable) break;
            p.move(Direction.DOWN);
        }
        return p.getY();
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        int x0 = origin.getX(), z0 = origin.getZ();
        int g = ground(level, x0, z0);
        // firm, dry, fairly level ground under the trunk
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                int y = ground(level, x0 + dx, z0 + dz);
                BlockPos top = new BlockPos(x0 + dx, y, z0 + dz);
                if (Math.abs(y - g) > 3 || !level.getFluidState(top).isEmpty() || !level.getFluidState(top.below()).isEmpty()
                        || !natural(level.getBlockState(top.below()))) return false;
            }
        }
        Tree t = new Tree(level, x0, z0, g);
        clearTrees(t);

        int roots = 5 + random.nextInt(2);
        double rootBase = random.nextDouble() * Math.PI * 2;
        double[] rootAngle = new double[roots];
        for (int i = 0; i < roots; i++) rootAngle[i] = rootBase + i * Math.PI * 2 / roots + (random.nextDouble() - 0.5) * 0.4;
        double door = (rootAngle[0] + rootAngle[1]) / 2; // the opening sits between two buttresses
        double ph1 = random.nextDouble() * 6.3, ph2 = random.nextDouble() * 6.3;
        int trunkTop = 13 + random.nextInt(3);

        // ---- trunk ----
        for (int y = 0; y <= trunkTop + 2; y++) {
            for (int dx = -7; dx <= 7; dx++) {
                for (int dz = -7; dz <= 7; dz++) {
                    double d = Math.hypot(dx, dz), th = Math.atan2(dz, dx);
                    if (d <= radius(y, th, trunkTop, rootAngle, ph1, ph2)) t.trunk.add(new BlockPos(x0 + dx, g + y, z0 + dz));
                }
            }
        }
        // ---- roots ----
        for (int i = 0; i < roots; i++) root(t, random, rootAngle[i], 7 + random.nextInt(3), 3.0, 1.6, true);
        // ---- crown: limbs out of the upper trunk, plus a leader going up ----
        int limbs = 5 + random.nextInt(2);
        double limbBase = random.nextDouble() * Math.PI * 2;
        for (int i = 0; i < limbs; i++) {
            double az = limbBase + i * Math.PI * 2 / limbs + (random.nextDouble() - 0.5) * 0.5;
            int ys = trunkTop - 4 + random.nextInt(5);
            double elev = Math.toRadians(18 + random.nextInt(20) + (ys - trunkTop + 4) * 3);
            limb(t, random, az, elev, g + ys, 8.5 + random.nextDouble() * 2.5);
        }
        limb(t, random, random.nextDouble() * Math.PI * 2, Math.toRadians(68 + random.nextInt(12)), g + trunkTop, 7 + random.nextDouble() * 2);

        build(t, random, trunkTop, door);
        hollow(t, random, door, trunkTop, rootAngle, ph1, ph2);
        return true;
    }

    /** Trunk radius at height y and angle th: tapering, lumpy, flaring into root buttresses near the ground. */
    private static double radius(int y, double th, int top, double[] roots, double ph1, double ph2) {
        double base = y <= top ? 3.4 - 1.1 * y / top : Math.max(1.0, 2.3 - (y - top) * 0.5);
        double r = base * (1 + 0.10 * Math.sin(3 * th + ph1) + 0.06 * Math.sin(5 * th + ph2));
        double flare = 0;
        for (double a : roots) flare = Math.max(flare, Math.pow(Math.max(0, Math.cos(th - a)), 10) * Math.max(0, 3.5 - y) * 0.75);
        return r + flare;
    }

    private static double innerRadius(int y) {
        return y < 3 ? 2.15 : y == 3 ? 1.5 : 1.0;
    }

    /** A root running out from the trunk over the ground, dipping into it at the end, with a side root. */
    private static void root(Tree t, RandomSource random, double az, double len, double start, double rise, boolean main) {
        BlockPos prev = null;
        double drift = (random.nextDouble() - 0.5) * 0.3;
        for (double s = 0; s <= len; s += 0.25) {
            double f = s / len, a = az + drift * f;
            double r = start + s;
            double x = t.x0 + 0.5 + Math.cos(a) * r, z = t.z0 + 0.5 + Math.sin(a) * r;
            int gx = Mth.floor(x), gz = Mth.floor(z);
            double y = t.ground(gx, gz) + rise * (1 - f) * (1 - f) * (1 - f) - 0.8 * f;
            BlockPos p = BlockPos.containing(x, y, z);
            boolean under = p.getY() < t.ground(gx, gz);
            int lvl = under ? WOOD : main ? (f < 0.55 ? WOOD : BRANCH) : (f < 0.5 ? BRANCH : TWIG);
            if (main && f < 0.25) stamp(t, new Vec3(x, y, z), 0.9);
            prev = step(t, prev, p, lvl);
            if (under && f > 0.6) break; // it has gone into the soil
            if (main && Math.abs(s - len * 0.5) < 0.13) root(t, random, a + (random.nextBoolean() ? 0.7 : -0.7), 3 + random.nextInt(2), r, 0.6, false);
        }
    }

    /** A main limb: thick wood curving out and up, thinning to a branch, splitting into side branches with leaf clusters. */
    private static void limb(Tree t, RandomSource random, double az, double elev, int ys, double len) {
        Vec3 dir = dir(az, elev);
        Vec3 start = new Vec3(t.x0 + 0.5 + Math.cos(az) * 1.2, ys + 0.5, t.z0 + 0.5 + Math.sin(az) * 1.2);
        Vec3 end = clampReach(t, start.add(dir.scale(len)), WOOD_REACH - 3);
        Vec3 mid = start.add(dir(az, elev * 0.55).scale(len * 0.55)); // flatter at first, then turning up
        BlockPos prev = null;
        int forks = 0;
        for (double s = 0; s <= 1.0001; s += 0.02) {
            Vec3 p = bezier(start, mid, end, s);
            if (s < 0.35) stamp(t, p, s < 0.15 ? 1.2 : 0.8);
            prev = step(t, prev, BlockPos.containing(p), s < 0.7 ? WOOD : BRANCH);
            if (forks < 3 && s >= 0.45 + forks * 0.2) {
                forks++;
                Vec3 tangent = bezier(start, mid, end, Math.min(1, s + 0.02)).subtract(p).normalize();
                double taz = Math.atan2(tangent.z, tangent.x);
                for (int k = 0; k < (forks == 3 ? 2 : 1); k++) {
                    double side = (random.nextBoolean() ? 1 : -1) * (0.5 + random.nextDouble() * 0.5);
                    branch(t, random, BlockPos.containing(p), p, taz + side, elev * 0.7 + random.nextDouble() * 0.35, 4.5 - forks * 0.6 + random.nextDouble());
                }
            }
        }
        // the limb's own end splits in two twiggy tips
        for (int k = -1; k <= 1; k += 2) branch(t, random, prev, end, az + k * 0.45, elev + 0.2, 3.0);
        cluster(t, random, end.add(0, 0.8, 0), 2.8);
    }

    /** A side branch: half branch, half twig, leaves at its end. */
    private static void branch(Tree t, RandomSource random, BlockPos from, Vec3 p0, double az, double elev, double len) {
        Vec3 end = clampReach(t, p0.add(dir(az, elev).scale(len)), WOOD_REACH);
        BlockPos prev = from;
        for (double s = 0.05; s <= 1.0001; s += 0.05) {
            Vec3 p = p0.lerp(end, s);
            prev = step(t, prev, BlockPos.containing(p), s < 0.5 ? BRANCH : TWIG);
        }
        cluster(t, random, end.add(0, 0.6, 0), 2.3 + random.nextDouble() * 0.9);
    }

    private static Vec3 dir(double az, double elev) {
        return new Vec3(Math.cos(az) * Math.cos(elev), Math.sin(elev), Math.sin(az) * Math.cos(elev));
    }

    private static Vec3 bezier(Vec3 a, Vec3 b, Vec3 c, double s) {
        return a.scale((1 - s) * (1 - s)).add(b.scale(2 * s * (1 - s))).add(c.scale(s * s));
    }

    /** Pulls a point in horizontally so it is at most max blocks from the trunk centre (lifting it a little instead). */
    private static Vec3 clampReach(Tree t, Vec3 p, double max) {
        double dx = p.x - t.x0 - 0.5, dz = p.z - t.z0 - 0.5, d = Math.hypot(dx, dz);
        if (d <= max) return p;
        double k = max / d;
        return new Vec3(t.x0 + 0.5 + dx * k, p.y + (d - max) * 0.4, t.z0 + 0.5 + dz * k);
    }

    /** Thick wood: every block within r of p. */
    private static void stamp(Tree t, Vec3 p, double r) {
        int ri = (int) Math.ceil(r);
        for (int dx = -ri; dx <= ri; dx++)
            for (int dy = -ri; dy <= ri; dy++)
                for (int dz = -ri; dz <= ri; dz++) {
                    BlockPos q = BlockPos.containing(p.x + dx, p.y + dy, p.z + dz);
                    if (Vec3.atCenterOf(q).distanceTo(p) <= r) t.wood(q, WOOD);
                }
    }

    /** Extends a path to p one face at a time (so connections are always face to face), recording the links. */
    private static BlockPos step(Tree t, BlockPos prev, BlockPos p, int lvl) {
        if (prev == null) {
            t.wood(p, lvl);
            return p;
        }
        BlockPos cur = prev;
        while (!cur.equals(p)) {
            BlockPos next = cur.getX() != p.getX() ? cur.offset(Integer.signum(p.getX() - cur.getX()), 0, 0)
                    : cur.getY() != p.getY() ? cur.offset(0, Integer.signum(p.getY() - cur.getY()), 0)
                    : cur.offset(0, 0, Integer.signum(p.getZ() - cur.getZ()));
            t.wood(next, lvl);
            t.link(cur, next);
            cur = next;
        }
        return cur;
    }

    /** A lumpy ellipsoid of leaves. */
    private static void cluster(Tree t, RandomSource random, Vec3 c, double r) {
        double ry = r * 0.7;
        int ri = (int) Math.ceil(r);
        for (int dx = -ri; dx <= ri; dx++)
            for (int dy = -ri; dy <= ri; dy++)
                for (int dz = -ri; dz <= ri; dz++) {
                    BlockPos q = BlockPos.containing(c.x + dx, c.y + dy, c.z + dz);
                    Vec3 o = Vec3.atCenterOf(q).subtract(c);
                    double v = (o.x * o.x + o.z * o.z) / (r * r) + o.y * o.y / (ry * ry);
                    if (v < 1 - 0.3 * noise(q) && Math.hypot(q.getX() - t.x0, q.getZ() - t.z0) <= LEAF_REACH) t.leaves.add(q);
                }
    }

    private static double noise(BlockPos p) {
        return (Mth.getSeed(p.getX(), p.getY(), p.getZ()) >>> 20 & 1023) / 1024.0;
    }

    /** Writes the trunk, limbs, roots and leaves. */
    private static void build(Tree t, RandomSource random, int trunkTop, double door) {
        WorldGenLevel level = t.level;
        BlockState log = AncientOak.LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState bark = AncientOak.WOOD.get().defaultBlockState();
        for (BlockPos p : t.trunk) {
            // bark on top where nothing continues above, so buttresses don't show end grain
            t.set(p, t.trunk.contains(p.above()) || t.wood.containsKey(p.above()) ? log : bark);
            if (p.getY() == t.g) { // root the trunk into the ground on slopes
                BlockPos q = p.below();
                for (int i = 0; i < 6 && t.safe(q) && !level.getBlockState(q).isSolid(); i++, q = q.below()) t.set(q, bark);
            }
        }
        for (Map.Entry<BlockPos, Integer> e : t.wood.entrySet()) {
            BlockPos p = e.getKey();
            if (t.trunk.contains(p)) continue;
            if (e.getValue() == WOOD) {
                t.set(p, bark);
                continue;
            }
            BlockState s = (e.getValue() == BRANCH ? AncientOak.BRANCH : AncientOak.TWIG).get().defaultBlockState();
            int mask = t.links.getOrDefault(p, 0);
            for (Direction d : Direction.values())
                if ((mask >> d.ordinal() & 1) != 0 && t.isTree(p.relative(d)) && t.safe(p.relative(d)))
                    s = s.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(d), true);
            t.set(p, s);
        }
        BlockState leaf = AncientOak.LEAVES.get().defaultBlockState();
        for (BlockPos p : t.leaves) {
            if (t.isTree(p) || !t.safe(p)) continue;
            BlockState old = level.getBlockState(p);
            if (old.isAir() || old.canBeReplaced() || old.is(BlockTags.LEAVES)) t.set(p, leaf);
        }
    }

    /** The room at the bottom of the trunk: opening, floor, chimney, altar, mushrooms, cobwebs and maybe a bat. */
    private static void hollow(Tree t, RandomSource random, double door, int trunkTop, double[] roots, double ph1, double ph2) {
        WorldGenLevel level = t.level;
        BlockPos c = new BlockPos(t.x0, t.g, t.z0);
        for (int y = 0; y <= 8; y++) {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    if (Math.hypot(dx, dz) > innerRadius(y)) continue;
                    t.set(c.offset(dx, y, dz), AIR);
                    if (y == 0) t.set(c.offset(dx, -1, dz), floor(random));
                }
            }
        }
        // the opening: 2 high (3 in the middle) through the wall, and a clear path out past the buttresses
        for (int dx = -7; dx <= 7; dx++) {
            for (int dz = -7; dz <= 7; dz++) {
                double d = Math.hypot(dx, dz), diff = Math.abs(Mth.wrapDegrees(Math.toDegrees(Math.atan2(dz, dx) - door)));
                if (d < 1.5 || d > radius(0, door, trunkTop, roots, ph1, ph2) + 2.5 || diff > 24) continue;
                for (int y = 0; y < (diff < 10 && d < 4 ? 3 : 2); y++) {
                    BlockPos p = c.offset(dx, y, dz);
                    t.set(p, AIR);
                    t.trunk.remove(p);
                }
                BlockPos f = c.offset(dx, -1, dz);
                if (!level.getBlockState(f).isSolid()) t.set(f, floor(random));
            }
        }
        BlockPos altar = c;
        t.set(altar, AncientOak.ALTAR.get().defaultBlockState());
        BlockPos entrance = BlockPos.containing(t.x0 + 0.5 + Math.cos(door) * 6, t.g, t.z0 + 0.5 + Math.sin(door) * 6);
        if (level.getBlockEntity(altar) instanceof OfferingAltarBlockEntity be) be.setEntrance(entrance);

        // round the wall of the room: mushrooms and moss on the floor, cobwebs up under the dome
        int webs = 0, shrooms = 0;
        for (int i = 0; i < 24; i++) {
            double a = i * Math.PI / 12;
            BlockPos p = BlockPos.containing(c.getX() + 0.5 + Math.cos(a) * 1.9, c.getY(), c.getZ() + 0.5 + Math.sin(a) * 1.9);
            if (p.equals(altar) || !level.getBlockState(p).isAir() || Math.abs(Mth.wrapDegrees(Math.toDegrees(a - door))) < 35) continue;
            int roll = random.nextInt(6);
            if (roll == 0 && shrooms < 5) {
                t.set(p, (random.nextInt(3) == 0 ? Blocks.RED_MUSHROOM : Blocks.BROWN_MUSHROOM).defaultBlockState());
                shrooms++;
            } else if (roll == 1) t.set(p, Blocks.MOSS_CARPET.defaultBlockState());
            if (random.nextInt(4) == 0 && webs < 4 && level.getBlockState(p.above(2)).isAir()) {
                t.set(p.above(2), Blocks.COBWEB.defaultBlockState());
                webs++;
            }
        }
        t.set(c.above(5), Blocks.COBWEB.defaultBlockState()); // up the chimney
        if (random.nextBoolean()) spawn(level, random, EntityTypes.BAT, c.above(4), bat -> bat.setResting(true));
    }

    private static BlockState floor(RandomSource random) {
        return switch (random.nextInt(5)) {
            case 0 -> Blocks.ROOTED_DIRT.defaultBlockState();
            case 1 -> Blocks.COARSE_DIRT.defaultBlockState();
            default -> Blocks.PODZOL.defaultBlockState();
        };
    }

    /** Removes the trees growing where the oak stands: whole trees whose trunk is within 13 blocks, with their own leaves. */
    private static void clearTrees(Tree t) {
        WorldGenLevel level = t.level;
        Set<BlockPos> logs = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        for (int dx = -13; dx <= 13; dx++) {
            for (int dz = -13; dz <= 13; dz++) {
                if (dx * dx + dz * dz > 169) continue;
                BlockPos base = new BlockPos(t.x0 + dx, t.ground(t.x0 + dx, t.z0 + dz), t.z0 + dz);
                if (level.getBlockState(base).is(BlockTags.LOGS) && logs.add(base)) queue.add(base);
            }
        }
        while (!queue.isEmpty() && logs.size() < 3000) {
            BlockPos p = queue.poll();
            for (BlockPos n : BlockPos.betweenClosed(p.offset(-1, -1, -1), p.offset(1, 1, 1))) {
                if (n.getY() >= t.g - 4 && t.safe(n) && !logs.contains(n) && level.getBlockState(n).is(BlockTags.LOGS)) {
                    BlockPos im = n.immutable();
                    logs.add(im);
                    queue.add(im);
                }
            }
        }
        // a leaf belongs to these trees if its DISTANCE leads back to them (as in TreeFelling)
        Set<BlockPos> leaves = new HashSet<>();
        ArrayDeque<BlockPos> frontier = new ArrayDeque<>(logs);
        for (int step = 1; step <= LeavesBlock.DECAY_DISTANCE && !frontier.isEmpty(); step++) {
            ArrayDeque<BlockPos> next = new ArrayDeque<>();
            for (BlockPos p : frontier) {
                for (Direction d : Direction.values()) {
                    BlockPos n = p.relative(d);
                    if (!t.safe(n) || logs.contains(n)) continue;
                    BlockState s = level.getBlockState(n);
                    if (s.getBlock() instanceof LeavesBlock && !s.getValue(LeavesBlock.PERSISTENT) && s.getValue(LeavesBlock.DISTANCE) == step && leaves.add(n)) next.add(n);
                    else if (s.is(Blocks.VINE) || s.is(Blocks.BEE_NEST) || s.is(Blocks.COCOA)) t.set(n, AIR);
                }
            }
            frontier = next;
        }
        for (BlockPos p : logs) t.set(p, AIR);
        for (BlockPos p : leaves) t.set(p, AIR);
    }
}
