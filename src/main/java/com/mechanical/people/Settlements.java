package com.mechanical.people;

import com.mechanical.Calendar;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.mechanical.CampAge;
import com.mechanical.DryingRackBlock;
import com.mechanical.HangingRackBlock;
import com.mechanical.KilnBlock;
import com.mechanical.WorldAge;
import com.mechanical.Mechanical;
import com.mechanical.RockBlock;
import com.mechanical.TrampledPathBlock;
import com.mechanical.people.Neighbours.CampInfo;
import com.mechanical.people.Neighbours.Placement;
import com.mechanical.people.Neighbours.Settlement;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

/**
 * Tribes that settle near a player camp: after a good night by the fire (TribeVisits) a band may pick a fairly flat,
 * untouched spot {@link #MIN_DIST}-{@link #MAX_DIST} blocks away and build a camp there, block by block, in three stages:
 * <ol>
 * <li>a fire with a ring of stones, two hide tents, a hanging rack, the banner on a pole and a log to sit on (the band
 * carries what this takes);</li>
 * <li>two more tents, drying racks, a barrel of stores and a woodpile;</li>
 * <li>a palisade of sharpened logs (radius {@link #PALISADE_R}) with a gate toward the player camp.</li>
 * </ol>
 * The climate at the site ({@link ClimateZone}) picks the materials: spruce in the cold, oak or spruce in temperate lands,
 * acacia and hide shades where it is warm, jungle wood, bamboo palisades and leaf lean-tos in the jungle.
 * Each stage starts once the stockpile (wood, stone, hides) covers all its blocks; one member at a time builds
 * (BuildCampGoal), paying for each block as it goes down. Woodcutters bring wood (felling trees only with mobGriefing),
 * gatherers stone (FetchStoneGoal), hunters hides (ButcherCarcassGoal; kills hung on the camp's rack first). On top of
 * that a coarse income ({@link #INCOME_TICKS}) stands in for work done while nobody is near: stockpiles grow even when
 * unloaded, but blocks only go down while the camp is loaded. Up to {@link #MAX_NEAR} camps settle by one player camp,
 * one at a time. Members walk to the player camp to trade and to the other camps (ErrandGoal), wearing paths between them.
 */
public final class Settlements {
    public static final int MAX_NEAR = 3, AREA = 150, MIN_DIST = 50, MAX_DIST = 150, CAMP_GAP = 45, SETTLEMENT_GAP = 60, PALISADE_R = 10;
    public static final int WOOD_PER_TRIP = 4, HIDES_PER_KILL = 2, NEWCOMERS = 2, HIDE_WANT = 60;
    static final long SETTLE_GAP = 2L * CampAge.DAY, ABANDON = 3L * CampAge.DAY, ERRAND_MIN = 8000, ERRAND_SPREAD = 8000;
    static final int INCOME_TICKS = 1000;
    static final int[] CAP = {400, 120, 120}, START_EXTRA = {4, 2, 2};
    /** Loose stones round the fire, as SetUpCampGoal lays them. */
    private static final int[][] RING = {{2, 0}, {-2, 0}, {0, 2}, {0, -2}, {2, 1}, {-1, 2}, {-2, -1}, {1, -2}};

    /** Who is building at each camp right now (tribe -> entity id, last game time), so only one does. Not saved. */
    private static final Map<UUID, long[]> BUILDER = new HashMap<>();
    private static final Map<UUID, Integer> EMPTY_CHECKS = new HashMap<>();

    public static Settlement of(Tribesperson t) {
        return t.level() instanceof ServerLevel level && t.group() != null ? Neighbours.get(level).settlement(t.group()) : null;
    }

    static boolean canSettleNear(Neighbours data, CampInfo info, long now) {
        return data.settlementsNear(info.camp, AREA).size() < MAX_NEAR && (info.lastSettled < 0 || now - info.lastSettled > SETTLE_GAP);
    }

    // ---- the site ----

    /** Ground a camp may stand on (anything else on top is someone's build, and the spot is left alone). */
    private static boolean natural(BlockState ground) {
        return ground.is(BlockTags.SUBSTRATE_OVERWORLD) || ground.is(BlockTags.SAND) || ground.is(Blocks.GRAVEL)
                || ground.is(BlockTags.BASE_STONE_OVERWORLD) || ground.getBlock() instanceof TrampledPathBlock || ground.is(Blocks.SNOW_BLOCK);
    }

    static BlockPos ground(ServerLevel level, BlockPos column) {
        return level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
    }

    /** The flattest fit spot of 32 tries round {@code anchor}, or null. */
    static BlockPos findSite(ServerLevel level, Neighbours data, BlockPos anchor) {
        RandomSource r = level.getRandom();
        BlockPos best = null;
        int bestScore = Integer.MAX_VALUE;
        for (int i = 0; i < 32; i++) {
            double a = r.nextDouble() * Math.PI * 2, d = MIN_DIST + r.nextDouble() * (MAX_DIST - MIN_DIST);
            int x = anchor.getX() + (int) (Math.cos(a) * d), z = anchor.getZ() + (int) (Math.sin(a) * d);
            BlockPos c = new BlockPos(x, 0, z);
            if (CampAge.get(level).camps().stream().anyMatch(cp -> horizontal(cp.centre, c) < CAMP_GAP)
                    || data.settlements.stream().anyMatch(s -> horizontal(s.centre, c) < SETTLEMENT_GAP)) continue;
            int score = siteScore(level, x, z);
            if (score >= 0 && score < bestScore) {
                bestScore = score;
                best = ground(level, c);
            }
        }
        return best;
    }

    private static double horizontal(BlockPos a, BlockPos b) {
        double dx = a.getX() - b.getX(), dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** How uneven the ground is round x,z (lower is better), or -1 if it won't do: not loaded, water, builds, too steep. */
    private static int siteScore(ServerLevel level, int x, int z) {
        int r = PALISADE_R + 1;
        if (!level.isLoaded(new BlockPos(x - r, 0, z - r)) || !level.isLoaded(new BlockPos(x + r, 0, z + r))
                || !level.isLoaded(new BlockPos(x - r, 0, z + r)) || !level.isLoaded(new BlockPos(x + r, 0, z - r))) return -1;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), score = 0, trees = 0;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 > r * r) continue;
                BlockPos top = ground(level, new BlockPos(x + dx, 0, z + dz));
                BlockState below = level.getBlockState(top.below());
                if (!level.getFluidState(top).isEmpty() || !level.getFluidState(top.below()).isEmpty()) return -1;
                int diff = Math.abs(top.getY() - y);
                boolean core = d2 <= 8 * 8;
                if (below.is(BlockTags.LOGS) || diff > 3) {
                    if (core || ++trees > 14) return -1; // a few trees in the outer ring are fine (the palisade goes round them)
                    continue;
                }
                if (!natural(below) || core && diff > 1) return -1;
                score += diff;
            }
        }
        return score;
    }

    /** The band of a visit becomes the settlers of a new camp at {@code site}; they walk there and build (BuildCampGoal). */
    static Settlement found(ServerLevel level, Neighbours data, Neighbours.Visit v, BlockPos site, List<Tribesperson> members, long now) {
        Direction gate = Direction.getApproximateNearest(v.camp.getX() - site.getX(), 0, v.camp.getZ() - site.getZ());
        Settlement s = new Settlement(v.tribe, v.camp, site, gate, 0, 0, true, List.of(), List.of(), now, now + CampAge.DAY, List.of(), Optional.empty(),
                WorldAge.npcAge(level.getServer(), v.tribe), List.of()); // built in the tribe's age from the start
        s.plan = plan(level, s, 1);
        int[] cost = cost(s.plan, 0);
        for (int k = 0; k < 3; k++) s.stock[k] = cost[k] + START_EXTRA[k]; // what the band carries
        data.settlements.add(s);
        for (Tribesperson t : members) t.visit = null;
        data.setDirty();
        return s;
    }

    // ---- what each stage builds ----

    /** The world position of camp-local x (toward the player camp) and z (to the right of that). */
    private static BlockPos at(Settlement s, int x, int z) {
        return s.centre.relative(s.gate, x).relative(s.gate.getClockWise(), z);
    }

    static List<Placement> plan(ServerLevel level, Settlement s, int stage) {
        return plan(level, s, stage, ClimateZone.of(level.getBiome(s.centre)));
    }

    /** The blocks of a stage, built of {@code zone}'s materials. */
    static List<Placement> plan(ServerLevel level, Settlement s, int stage, ClimateZone zone) {
        List<Placement> out = new ArrayList<>();
        RandomSource r = RandomSource.create(s.tribe.getLeastSignificantBits() * 31 + stage);
        boolean taiga = level.getBiome(s.centre).is(BiomeTags.IS_TAIGA);
        BlockState log = zone.log(taiga);
        Direction right = s.gate.getClockWise();
        switch (stage) {
            case 1 -> {
                boolean fresh = s.clearing == null; // a rebuild (AgeRebuild) redoes only the shelters
                if (fresh) {
                    BlockState fire = Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true);
                    out.add(new Placement(s.centre, fire));
                    for (int[] o : RING) add(out, level, s, ground(level, s, at(s, o[0], o[1])),
                            Mechanical.LOOSE_STONE.get().defaultBlockState().setValue(RockBlock.SIZE, 1 + r.nextInt(3)));
                    add(out, level, s, ground(level, s, at(s, -4, 0)), com.mechanical.CarcassHauling.HANGING_RACK.get().defaultBlockState()
                            .setValue(HangingRackBlock.FACING, s.gate).setValue(HangingRackBlock.PART, HangingRackBlock.Part.LOWER));
                }
                tent(out, level, s, SHELTERS[0][0], SHELTERS[0][1], right.getOpposite(), zone);
                tent(out, level, s, SHELTERS[1][0], SHELTERS[1][1], right, zone);
                if (fresh) {
                    BlockPos pole = ground(level, s, at(s, 3, 3));
                    BlockState stripped = zone.stripped(taiga);
                    if (add(out, level, s, pole, stripped) && add(out, level, s, pole.above(), stripped))
                        add(out, level, s, pole.above(2), Blocks.BANNER.pick(s.tribeInfo().baseColour()).defaultBlockState().setValue(BannerBlock.ROTATION, r.nextInt(16)));
                    add(out, level, s, ground(level, s, at(s, -2, 3)), log.setValue(RotatedPillarBlock.AXIS, s.gate.getAxis()));
                }
            }
            case 2 -> {
                tent(out, level, s, SHELTERS[2][0], SHELTERS[2][1], s.gate, zone);
                tent(out, level, s, SHELTERS[3][0], SHELTERS[3][1], s.gate, zone);
                if (s.clearing == null) {
                    BlockState rack = Mechanical.DRYING_RACK.get().defaultBlockState().setValue(DryingRackBlock.HIDE, DryingRackBlock.HIDE_WET);
                    add(out, level, s, ground(level, s, at(s, 3, -3)), rack.setValue(DryingRackBlock.FACING, s.gate.getClockWise()));
                    add(out, level, s, ground(level, s, at(s, 2, -4)), rack.setValue(DryingRackBlock.FACING, s.gate.getClockWise()));
                    add(out, level, s, ground(level, s, at(s, -3, -3)), Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP));
                    BlockState pile = log.setValue(RotatedPillarBlock.AXIS, right.getAxis());
                    BlockPos a = ground(level, s, at(s, 4, 4)), b = ground(level, s, at(s, 5, 4));
                    if (add(out, level, s, a, pile) & add(out, level, s, b, pile)) add(out, level, s, a.above(), pile);
                }
                if (s.age >= WorldAge.BRONZE) {
                    // Bronze Age and later: a clay kiln and a quern by the fire
                    BlockState kiln = Mechanical.KILN.get().defaultBlockState().setValue(KilnBlock.FACING, right);
                    BlockPos k = ground(level, s, at(s, WORKSHOP[0][0], WORKSHOP[0][1]));
                    if (free(level, s, k.above()) && add(out, level, s, k, kiln.setValue(KilnBlock.HALF, DoubleBlockHalf.LOWER)))
                        out.add(new Placement(k.above().immutable(), kiln.setValue(KilnBlock.HALF, DoubleBlockHalf.UPPER)));
                    add(out, level, s, ground(level, s, at(s, WORKSHOP[1][0], WORKSHOP[1][1])), com.mechanical.Farming.QUERN.get().defaultBlockState());
                }
            }
            case 3 -> {
                if (s.clearing == null) palisade(out, level, s, zone.post(taiga), zone.stake(taiga));
            }
            default -> {}
        }
        return out;
    }

    /** Adds a block if its spot is free now (air, plants, snow; no water). */
    private static boolean add(List<Placement> out, ServerLevel level, Settlement s, BlockPos pos, BlockState state) {
        if (!level.isLoaded(pos) || !free(level, s, pos)) return false;
        out.add(new Placement(pos.immutable(), state));
        return true;
    }

    private static boolean free(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).canBeReplaced() && level.getFluidState(pos).isEmpty();
    }

    /** Free, or one of the camp's own blocks that the rebuild being planned takes down. */
    private static boolean free(ServerLevel level, Settlement s, BlockPos pos) {
        return s.clearing != null && s.clearing.contains(pos.asLong()) || free(level, pos);
    }

    /** The ground of a column, looking through the blocks a rebuild being planned takes down (and the air under them). */
    private static BlockPos ground(ServerLevel level, Settlement s, BlockPos column) {
        BlockPos g = ground(level, column);
        if (s.clearing == null) return g;
        boolean through = false;
        for (int i = 0; i < 8; i++) {
            BlockPos below = g.below();
            boolean ours = s.clearing.contains(below.asLong());
            if (!ours && !(through && free(level, below))) break;
            through |= ours;
            g = below;
        }
        return g;
    }

    /** Camp-local spots of the shelters (stages 1, 1, 2, 2) and of the Bronze Age kiln and quern (stage 2). */
    private static final int[][] SHELTERS = {{0, 5}, {0, -5}, {-5, 4}, {-5, -4}}, WORKSHOP = {{-1, -3}, {1, -3}};

    /**
     * Plans the camp's rebuild into {@code age} (AgeRebuild): the shelters (and kiln and quern) of the stages it has finished
     * come down and go up again in that age's style, on the same spots. Its fire, racks, stores, banner and palisade stay.
     * Only blocks the camp built are taken down. Sets the camp's age; the pending stage is planned anew in the new style.
     */
    static List<Neighbours.Job> rebuild(ServerLevel level, Settlement s, int age) {
        Direction right = s.gate.getClockWise();
        s.clearing = new java.util.HashSet<>();
        for (long key : s.built) {
            BlockPos p = BlockPos.of(key);
            int dx = p.getX() - s.centre.getX(), dz = p.getZ() - s.centre.getZ();
            int x = dx * s.gate.getStepX() + dz * s.gate.getStepZ(), z = dx * right.getStepX() + dz * right.getStepZ();
            boolean restyled = false;
            for (int i = 0; i < SHELTERS.length; i++) {
                if (s.stage >= (i < 2 ? 1 : 2) && Math.abs(x - SHELTERS[i][0]) <= 1 && Math.abs(z - SHELTERS[i][1]) <= 1) restyled = true;
            }
            for (int[] w : WORKSHOP) if (s.stage >= 2 && x == w[0] && z == w[1]) restyled = true;
            if (restyled && !level.getBlockState(p).is(Blocks.DIRT)) s.clearing.add(key); // the dirt made up under a shelter stays
        }
        s.age = age;
        ClimateZone zone = ClimateZone.of(level.getBiome(s.centre));
        List<Placement> plan = new ArrayList<>();
        for (int stage = 1; stage <= Math.min(s.stage, 2); stage++) plan.addAll(plan(level, s, stage, zone));
        java.util.Set<Long> planned = new java.util.HashSet<>();
        for (Placement p : plan) planned.add(p.pos().asLong());
        List<Neighbours.Job> jobs = new ArrayList<>();
        s.clearing.stream().filter(k -> !planned.contains(k)).map(BlockPos::of).sorted(Comparator.comparingInt((BlockPos p) -> p.getY()).reversed())
                .forEach(p -> jobs.add(new Neighbours.Job(p, level.getBlockState(p), Blocks.AIR.defaultBlockState()))); // roofs first
        for (Placement p : plan) {
            BlockState now = level.getBlockState(p.pos());
            if (!now.equals(p.state())) jobs.add(new Neighbours.Job(p.pos(), s.clearing.contains(p.pos().asLong()) ? now : Blocks.AIR.defaultBlockState(), p.state()));
        }
        s.clearing = null;
        if (s.stage < 3 && !s.started) {
            s.plan = plan(level, s, s.stage + 1);
            s.step = 0;
        }
        return jobs;
    }

    /**
     * A 3x3 A-frame hide tent centred on camp-local x,z, its open end facing {@code front}: slopes along both sides, the
     * ridge on top, the back closed with a wall of hide, a hide on the floor. Where it is warm a hide shade on four poles
     * instead, in the jungle a lean-to of bamboo and leaves (see {@link ClimateZone}). Uneven ground under it is made up
     * with dirt (one block at most); a tent that doesn't fit is left out.
     */
    private static void tent(List<Placement> out, ServerLevel level, Settlement s, int x, int z, Direction front, ClimateZone zone) {
        BlockPos c = at(s, x, z);
        Direction side = front.getClockWise();
        int y = Integer.MIN_VALUE;
        for (int a = -1; a <= 1; a++) for (int b = -1; b <= 1; b++) y = Math.max(y, ground(level, s, c.relative(front, a).relative(side, b)).getY());
        List<Placement> fill = new ArrayList<>(), tent = new ArrayList<>();
        boolean hut = s.age >= WorldAge.BRONZE;
        boolean roofed = hut || zone == ClimateZone.WARM || zone == ClimateZone.JUNGLE; // a flat roof 2 up on posts
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                BlockPos col = c.relative(front, a).relative(side, b);
                int g = ground(level, s, col).getY();
                BlockPos p = new BlockPos(col.getX(), y, col.getZ());
                if (y - g > 1 || !level.isLoaded(p) || !free(level, s, p) || !free(level, s, p.above())
                        || roofed && !free(level, s, p.above(2))) return;
                if (g < y) fill.add(new Placement(p.below(), Blocks.DIRT.defaultBlockState()));
            }
        }
        BlockPos base = new BlockPos(c.getX(), y, c.getZ());
        if (hut) {
            // Bronze Age and later: a small wattle hut, posts at the corners, daub (mud brick, bamboo) walls with a door toward
            // the fire's side, under a roof of thatch (bark in the cold, packed mud where it is warm, leaves in the jungle)
            BlockState post = zone == ClimateZone.JUNGLE ? Blocks.BAMBOO_BLOCK.defaultBlockState() : zone.stripped(false);
            BlockState wall = switch (zone) {
                case WARM -> Blocks.MUD_BRICKS.defaultBlockState();
                case JUNGLE -> Blocks.BAMBOO_BLOCK.defaultBlockState();
                default -> Blocks.PACKED_MUD.defaultBlockState();
            };
            BlockState top = switch (zone) {
                case TEMPERATE -> Blocks.HAY_BLOCK.defaultBlockState();
                case COLD -> zone.barkRoof(true).setValue(com.mechanical.BarkRoofBlock.FLAT, true);
                case WARM -> Blocks.PACKED_MUD.defaultBlockState();
                case JUNGLE -> Blocks.JUNGLE_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true);
            };
            for (int a = -1; a <= 1; a++) {
                for (int b = -1; b <= 1; b++) {
                    BlockPos p = base.relative(front, a).relative(side, b);
                    boolean corner = Math.abs(a) == 1 && Math.abs(b) == 1, open = b == 0 && a >= 0; // the middle and the door
                    if (corner || !open) {
                        tent.add(new Placement(p, corner ? post : wall));
                        tent.add(new Placement(p.above(), corner ? post : wall));
                    }
                }
            }
            for (int a = -1; a <= 1; a++) for (int b = -1; b <= 1; b++) tent.add(new Placement(base.relative(front, a).relative(side, b).above(2), top));
            tent.add(new Placement(base, Blocks.CARPET.pick(net.minecraft.world.item.DyeColor.BROWN).defaultBlockState()));
            out.addAll(fill);
            out.addAll(tent);
            return;
        }
        if (roofed) {
            BlockState pole = zone.fence(false);
            for (int a = -1; a <= 1; a++) {
                for (int b = -1; b <= 1; b++) {
                    BlockPos p = base.relative(front, a).relative(side, b);
                    if (zone == ClimateZone.JUNGLE && a == -1) {
                        tent.add(new Placement(p, Blocks.BAMBOO_BLOCK.defaultBlockState()));
                        tent.add(new Placement(p.above(), Blocks.BAMBOO_BLOCK.defaultBlockState()));
                    } else if (Math.abs(a) == 1 && Math.abs(b) == 1) {
                        tent.add(new Placement(p, pole));
                        tent.add(new Placement(p.above(), pole));
                    }
                }
            }
            // the roof goes on once its posts stand: a hide stretched flat, or leaves
            BlockState top = zone == ClimateZone.WARM ? Blocks.WOOL.pick(net.minecraft.world.item.DyeColor.ORANGE).defaultBlockState()
                    : Blocks.JUNGLE_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true);
            for (int a = -1; a <= 1; a++) for (int b = -1; b <= 1; b++) tent.add(new Placement(base.relative(front, a).relative(side, b).above(2), top));
            tent.add(new Placement(base, Blocks.CARPET.pick(net.minecraft.world.item.DyeColor.BROWN).defaultBlockState()));
            out.addAll(fill);
            out.addAll(tent);
            return;
        }
        tent.add(new Placement(base.relative(front, -1), People.HIDE_CANVAS.get().defaultBlockState()));
        for (int a = -1; a <= 1; a++) {
            HideTentBlock.End left = a == -1 ? HideTentBlock.End.RIGHT : HideTentBlock.End.NONE, right = a == -1 ? HideTentBlock.End.LEFT : HideTentBlock.End.NONE;
            // the slope on the left (-side) rises toward +side; its back wall is on its right, and the other way round
            tent.add(new Placement(base.relative(front, a).relative(side, -1), People.HIDE_TENT_SLOPE.get().defaultBlockState()
                    .setValue(HideTentBlock.FACING, side).setValue(HideTentBlock.END, left)));
            tent.add(new Placement(base.relative(front, a).relative(side, 1), People.HIDE_TENT_SLOPE.get().defaultBlockState()
                    .setValue(HideTentBlock.FACING, side.getOpposite()).setValue(HideTentBlock.END, right)));
        }
        for (int a = -1; a <= 1; a++) tent.add(new Placement(base.relative(front, a).above(), People.HIDE_TENT_RIDGE.get().defaultBlockState()
                .setValue(HideTentBlock.Ridge.FACING, front).setValue(HideTentBlock.Ridge.CLOSED, a == -1)));
        tent.add(new Placement(base, Blocks.CARPET.pick(net.minecraft.world.item.DyeColor.BROWN).defaultBlockState()));
        out.addAll(fill);
        out.addAll(tent);
    }

    /** A ring of logs, two high with a sharpened top, round the camp; a 3-wide gate toward the player camp with taller posts. */
    private static void palisade(List<Placement> out, ServerLevel level, Settlement s, BlockState log, BlockState sharp) {
        List<int[]> cols = new ArrayList<>();
        int r = PALISADE_R + 1;
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                double d = Math.sqrt(x * x + z * z);
                if (d >= PALISADE_R - 0.5 && d < PALISADE_R + 0.5 && !(x > 0 && Math.abs(z) <= 1)) cols.add(new int[] {x, z});
            }
        }
        // round from the gate, so the builder walks along the wall
        cols.sort(Comparator.comparingDouble(c -> Math.floorMod((int) Math.round(Math.toDegrees(Math.atan2(c[1], c[0])) * 10), 3600)));
        for (int[] c : cols) {
            BlockPos g = ground(level, at(s, c[0], c[1]));
            if (!level.isLoaded(g) || !natural(level.getBlockState(g.below()))) continue; // a tree or a build in the way
            int h = c[0] > 0 && Math.abs(c[1]) == 2 ? 3 : 2;
            boolean ok = true;
            for (int i = 0; i < h && ok; i++) ok = add(out, level, s, g.above(i), log.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y));
            if (ok) add(out, level, s, g.above(h), sharp);
        }
    }

    /** Wood, stone and hides a block costs. */
    static int[] cost(BlockState s) {
        Block b = s.getBlock();
        if (b instanceof RockBlock || s.is(Blocks.PACKED_MUD) || s.is(Blocks.MUD_BRICKS) || b instanceof KilnBlock || b instanceof com.mechanical.QuernBlock) return new int[] {0, 1, 0};
        if (s.is(Blocks.HAY_BLOCK) || b instanceof com.mechanical.BarkRoofBlock) return new int[] {1, 0, 0};
        if (s.is(Blocks.CAMPFIRE)) return new int[] {2, 0, 0};
        if (b instanceof HangingRackBlock) return new int[] {4, 0, 0};
        if (b instanceof DryingRackBlock) return new int[] {2, 0, 1};
        if (s.is(Blocks.BARREL)) return new int[] {3, 0, 0};
        if (b == People.HIDE_CANVAS.get() || b instanceof HideTentBlock || b instanceof HideTentBlock.Ridge || s.is(BlockTags.WOOL_CARPETS)
                || s.is(BlockTags.WOOL) || s.is(BlockTags.BANNERS)) return new int[] {0, 0, 1};
        if (s.is(BlockTags.LOGS) || s.is(BlockTags.BAMBOO_BLOCKS) || s.is(BlockTags.WOODEN_FENCES) || b instanceof SharpenedLogBlock) return new int[] {1, 0, 0};
        return new int[] {0, 0, 0};
    }

    /** What the rest of a plan (from {@code from}) costs in all. */
    static int[] cost(List<Placement> plan, int from) {
        int[] sum = new int[3];
        for (int i = from; i < plan.size(); i++) {
            int[] c = cost(plan.get(i).state());
            for (int k = 0; k < 3; k++) sum[k] += c[k];
        }
        return sum;
    }

    private static boolean covers(Settlement s, int[] cost) {
        return s.stock[0] >= cost[0] && s.stock[1] >= cost[1] && s.stock[2] >= cost[2];
    }

    // ---- building ----

    /** One builder at a time per camp: true if {@code mob} is (now) it. */
    static boolean claim(Settlement s, Tribesperson mob, long now) {
        return claim(s.tribe, mob, now);
    }

    /** One builder at a time per tribe (camp building and AgeRebuild): true if {@code mob} is (now) it. */
    static boolean claim(UUID tribe, Tribesperson mob, long now) {
        long[] cur = BUILDER.get(tribe);
        if (cur != null && cur[0] != mob.getId() && now - cur[1] < 200) return false;
        BUILDER.put(tribe, new long[] {mob.getId(), now});
        return true;
    }

    /** The next block to put down: the fire if it is gone, else the plan's next block once the stage has started. Null if none or not affordable. */
    static Placement nextJob(ServerLevel level, Settlement s) {
        if (s.hasFire() && level.isLoaded(s.centre) && !level.getBlockState(s.centre).is(Blocks.CAMPFIRE) && free(level, s.centre))
            return s.stock[0] >= 2 ? new Placement(s.centre, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true)) : null;
        if (!s.started || s.step >= s.plan.size()) return null;
        Placement p = s.plan.get(s.step);
        return covers(s, cost(p.state())) ? p : null;
    }

    /** {@code by} puts {@code p} down (or finds its spot taken and moves on). Pays for it; finishing a plan finishes the stage. */
    static void place(ServerLevel level, Settlement s, Placement p, Tribesperson by) {
        Neighbours data = Neighbours.get(level);
        boolean planStep = s.step < s.plan.size() && s.plan.get(s.step) == p;
        if (!level.isLoaded(p.pos())) return;
        BlockState state = p.state();
        boolean rack = state.getBlock() instanceof HangingRackBlock;
        if (!free(level, p.pos()) || rack && (!free(level, p.pos().above()) || !free(level, p.pos().above(2)))) {
            if (planStep) advance(level, data, s); // something is in the way now: leave it out
            return;
        }
        int[] c = cost(state);
        if (!covers(s, c)) return;
        for (int k = 0; k < 3; k++) s.stock[k] -= c[k];
        if (state.is(Blocks.CAMPFIRE) && state.hasProperty(com.mechanical.Campfires.EMBERS)) state = state.setValue(com.mechanical.Campfires.EMBERS, false);
        level.setBlock(p.pos(), state, Block.UPDATE_ALL);
        s.built.add(p.pos().asLong());
        if (rack) {
            level.setBlock(p.pos().above(), state.setValue(HangingRackBlock.PART, HangingRackBlock.Part.MIDDLE), Block.UPDATE_ALL);
            level.setBlock(p.pos().above(2), state.setValue(HangingRackBlock.PART, HangingRackBlock.Part.UPPER), Block.UPDATE_ALL);
            s.built.add(p.pos().above().asLong());
            s.built.add(p.pos().above(2).asLong());
            s.rack = Optional.of(p.pos().immutable());
        }
        if (level.getBlockEntity(p.pos()) instanceof BannerBlockEntity banner) banner.applyComponentsFromItemStack(s.tribeInfo().banner(level.registryAccess()));
        if (level.getBlockEntity(p.pos()) instanceof BarrelBlockEntity barrel) {
            barrel.setItem(0, new ItemStack(com.mechanical.wildlife.Wildlife.COOKED_VENISON_SCRAPS.get(), 6));
            barrel.setItem(4, new ItemStack(Items.FLINT, 5));
            barrel.setItem(9, new ItemStack(Mechanical.CORDAGE.get(), 3));
            barrel.setItem(13, new ItemStack(Mechanical.SCRAPED_HIDE.get(), 2));
        }
        var sound = state.getSoundType();
        level.playSound(null, p.pos(), sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1) / 2, sound.getPitch() * 0.8F);
        if (by != null) by.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT);
        if (planStep) advance(level, data, s);
        data.setDirty();
    }

    private static void advance(ServerLevel level, Neighbours data, Settlement s) {
        boolean firstFire = s.stage == 0 && s.step == 0;
        s.step++;
        data.setDirty();
        if (firstFire) settled(level, s);
        if (s.step >= s.plan.size()) stageDone(level, s);
    }

    /** The fire burns at the new camp: everyone in the group lives here now. */
    private static void settled(ServerLevel level, Settlement s) {
        for (Tribesperson t : level.getEntitiesOfClass(Tribesperson.class, new AABB(s.centre).inflate(96, 48, 96), t -> s.tribe.equals(t.group()))) t.settleAt(s.centre);
        Component dir = TribeVisits.direction(s.anchor, s.centre);
        for (ServerPlayer p : playersAround(level, s)) {
            p.sendSystemMessage(Component.translatable("message.mechanical.settlement.founded", s.tribeInfo().name(), dir).withStyle(ChatFormatting.GOLD));
            TribeVisits.award(p, "tribe_neighbours");
        }
    }

    private static List<ServerPlayer> playersAround(ServerLevel level, Settlement s) {
        return level.players().stream().filter(p -> !p.isSpectator() && (p.blockPosition().closerThan(s.centre, 160) || p.blockPosition().closerThan(s.anchor, 160))).toList();
    }

    private static void stageDone(ServerLevel level, Settlement s) {
        s.stage++;
        s.step = 0;
        s.started = false;
        s.plan = s.stage < 3 ? plan(level, s, s.stage + 1) : new ArrayList<>();
        if (s.stage >= 2) {
            for (ServerPlayer p : playersAround(level, s)) {
                p.sendSystemMessage(Component.translatable("message.mechanical.settlement.stage" + s.stage, s.tribeInfo().name()).withStyle(ChatFormatting.GOLD));
                if (s.stage == 3) TribeVisits.award(p, "tribe_palisade");
            }
            // word spreads: a few more of the tribe come to live here
            if (level.isPositionEntityTicking(s.centre)) {
                RandomSource r = level.getRandom();
                for (int i = 0; i < NEWCOMERS; i++) {
                    double a = r.nextDouble() * Math.PI * 2;
                    TribeRole role = switch (r.nextInt(3)) { case 0 -> TribeRole.GATHERER; case 1 -> TribeRole.WOODCUTTER; default -> TribeRole.HUNTER; };
                    TribeVisits.spawn(level, s.centre.offset((int) (Math.cos(a) * 18), 0, (int) (Math.sin(a) * 18)), role, s.centre, s.tribe, false);
                }
            }
        }
    }

    // ---- the stockpile ----

    /** A member brought something home. */
    static void deliver(Tribesperson t, int kind, int amount) {
        Settlement s = of(t);
        if (s == null || t.home() == null) return;
        s.stock[kind] = Math.min(CAP[kind], s.stock[kind] + amount);
        Neighbours.get((ServerLevel) t.level()).setDirty();
    }

    /** Hunters of a camp hunt only while it wants hides; others (camps from worldgen, bands) as before. */
    static boolean wantsGame(Tribesperson t) {
        Settlement s = of(t);
        return s == null || s.stock[Settlement.HIDES] < HIDE_WANT;
    }

    // ---- every few seconds (from TribeVisits) ----

    static void tick(ServerLevel level, Neighbours data, long now) {
        long dayTime = level.getOverworldClockTime() % Calendar.CLOCK_DAY;
        for (Settlement s : List.copyOf(data.settlements)) {
            if (s.hasFire() && now % INCOME_TICKS == 0) {
                // the work of a day nobody watched: a little of everything (blocks only go down while loaded)
                s.stock[0] = Math.min(CAP[0], s.stock[0] + 2);
                if (now % (2 * INCOME_TICKS) == 0) {
                    s.stock[1] = Math.min(CAP[1], s.stock[1] + 1);
                    s.stock[2] = Math.min(CAP[2], s.stock[2] + 1);
                }
                data.setDirty();
            }
            if (!s.started && s.stage < 3 && !s.plan.isEmpty() && covers(s, cost(s.plan, s.step))) {
                s.started = true;
                data.setDirty();
            }
            if (now % 600 != 0) continue;
            if (!s.hasFire() && now - s.founded > ABANDON) {
                remove(data, s); // the settlers never got there
                continue;
            }
            if (!level.isPositionEntityTicking(s.centre)) continue;
            List<Tribesperson> members = level.getEntitiesOfClass(Tribesperson.class, new AABB(s.centre).inflate(64, 32, 64), t -> s.tribe.equals(t.group()) && t.isAlive());
            if (members.isEmpty() && s.hasFire()) {
                if (EMPTY_CHECKS.merge(s.tribe, 1, Integer::sum) >= 3) remove(data, s); // all gone: the camp stands empty
                continue;
            }
            EMPTY_CHECKS.remove(s.tribe);
            if (s.hasFire() && now >= s.nextErrand && dayTime >= 2000 && dayTime < 7000) errand(level, data, s, members, now);
        }
    }

    private static void remove(Neighbours data, Settlement s) {
        data.settlements.remove(s);
        BUILDER.remove(s.tribe);
        EMPTY_CHECKS.remove(s.tribe);
        data.setDirty();
    }

    /** Someone walks over: a trader to the player camp, or a visit to another camp nearby. */
    private static void errand(ServerLevel level, Neighbours data, Settlement s, List<Tribesperson> members, long now) {
        RandomSource r = level.getRandom();
        s.nextErrand = now + ERRAND_MIN + r.nextInt((int) ERRAND_SPREAD);
        data.setDirty();
        Tribe tribe = s.tribeInfo();
        List<ServerPlayer> players = TribeVisits.playersNear(level, s.anchor, TribeVisits.PLAYER_RANGE);
        boolean trade = !players.isEmpty() && players.stream().noneMatch(tribe::hostileTo) && players.stream().anyMatch(tribe::trades);
        List<Settlement> others = data.settlementsNear(s.centre, 300).stream().filter(o -> o != s && o.hasFire()).toList();
        BlockPos target;
        if (trade && (others.isEmpty() || r.nextBoolean())) target = TribeVisits.campFire(level, s.anchor);
        else if (!others.isEmpty()) {
            target = others.get(r.nextInt(others.size())).centre;
            trade = false;
        } else return;
        boolean asTrader = trade;
        List<Tribesperson> free = members.stream().filter(t -> t.home() != null && t.errand == null && t.visit == null && t.getTarget() == null
                && (!asTrader || t.role() != TribeRole.HUNTER)).toList();
        if (free.isEmpty()) return;
        Tribesperson who = free.stream().filter(t -> asTrader && t.role() == TribeRole.ELDER).findFirst().orElse(free.get(r.nextInt(free.size())));
        who.errand = new ErrandGoal.Errand(target.immutable(), trade, now);
    }

    static Component describe(Settlement s) {
        int[] need = cost(s.plan, s.step);
        return s.tribeInfo().name().copy().append(String.format(" camp at %s: stage %d/3, %s %d/%d, stock wood %d stone %d hides %d, needs wood %d stone %d hides %d",
                s.centre.toShortString(), s.stage, s.stage >= 3 ? "complete" : s.started ? "building" : "saving for", s.step, s.plan.size(),
                s.stock[0], s.stock[1], s.stock[2], need[0], need[1], need[2]));
    }

    private Settlements() {}
}
