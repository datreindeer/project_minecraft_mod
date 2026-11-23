package com.mechanical.worldgen;

import static com.mechanical.Mechanical.MODID;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.mechanical.wildlife.WildlifeRestock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Game tests for retrogen (fresh chunks marked current; an old chunk gets features and is re-marked; a chunk by a chest or
 * long lived in is skipped) and for the wildlife restock preferring a species missing from the area.
 */
@EventBusSubscriber(modid = MODID)
public final class RetrogenTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> RETROGEN = key("retrogen"), RESTOCK = key("restock_missing_species"), TRAIL_STEPS = key("trail_steps"),
            SMOOTH = key("smooth_ground"), GRASS_STAIRS = key("grass_stairs_creep"),
            WALK_UP = key("smooth_ground_walk_up"), TRAIL_BRIDGES = key("trail_bridge_height"), TRAIL_WALLS = key("trail_round_palisade");
    /** Far from the test grid, so features placed here can't land in another test. */
    private static final int FAR = 400;

    static {
        TEST_FUNCTIONS.register("retrogen", () -> RetrogenTest::retrogen);
        TEST_FUNCTIONS.register("restock_missing_species", () -> RetrogenTest::restock);
        TEST_FUNCTIONS.register("trail_steps", () -> RetrogenTest::trailSteps);
        TEST_FUNCTIONS.register("trail_bridge_height", () -> RetrogenTest::trailBridges);
        TEST_FUNCTIONS.register("trail_round_palisade", () -> RetrogenTest::trailWalls);
        TEST_FUNCTIONS.register("smooth_ground", () -> RetrogenTest::smoothGround);
        TEST_FUNCTIONS.register("grass_stairs_creep", () -> RetrogenTest::grassStairsCreep);
        TEST_FUNCTIONS.register("smooth_ground_walk_up", () -> RetrogenTest::walkUp);
    }

    private static ResourceKey<Consumer<GameTestHelper>> key(String name) {
        return ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, name));
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
        NatureDecorTest.init(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "retrogen"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "retrogen"),
                new FunctionGameTestInstance(RETROGEN, com.mechanical.TestArea.of(env, 20, true)));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "restock_missing_species"),
                new FunctionGameTestInstance(RESTOCK, com.mechanical.TestArea.of(env, 20, true)));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "trail_steps"),
                new FunctionGameTestInstance(TRAIL_STEPS, com.mechanical.TestArea.of(env, 20, true)));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "trail_bridge_height"),
                new FunctionGameTestInstance(TRAIL_BRIDGES, com.mechanical.TestArea.of(env, 20, true)));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "trail_round_palisade"),
                new FunctionGameTestInstance(TRAIL_WALLS, com.mechanical.TestArea.of(env, 20, true)));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "smooth_ground"),
                new FunctionGameTestInstance(SMOOTH, com.mechanical.TestArea.of(env, 20, true)));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "grass_stairs_creep"),
                new FunctionGameTestInstance(GRASS_STAIRS, com.mechanical.TestArea.of(env, 100, true)));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "smooth_ground_walk_up"),
                new FunctionGameTestInstance(WALK_UP, com.mechanical.TestArea.of(env, 400, true)));
    }

    /** Terraces 1 block apart, smoothed: a cow sent up them walks the half steps and never jumps. */
    private static void walkUp(GameTestHelper h) {
        for (int x = 0; x <= 15; x++)
            for (int z = 0; z <= 15; z++) {
                int top = 1 + x / 4;
                for (int y = 0; y < top; y++) h.setBlock(new BlockPos(x, y, z), Blocks.DIRT);
                h.setBlock(new BlockPos(x, top, z), Blocks.GRASS_BLOCK);
            }
        BlockPos a = h.absolutePos(new BlockPos(2, 0, 2));
        SmoothGround.smooth(h.getLevel(), a.getX(), a.getZ(), 12);
        for (int x : new int[] {3, 7, 11}) h.assertTrue(h.getBlockState(new BlockPos(x, 2 + x / 4, 8)).is(com.mechanical.EarthBlocks.GRASS_SLAB), "slab at x " + x);
        var cow = h.spawn(net.minecraft.world.entity.EntityTypes.COW, new BlockPos(1, 2, 8));
        cow.goalSelector.removeAllGoals(g -> true);
        BlockPos goal = h.absolutePos(new BlockPos(14, 5, 8));
        boolean[] jumped = {false};
        h.onEachTick(() -> {
            if (cow.getDeltaMovement().y > 0.3) jumped[0] = true; // a jump starts at 0.42; stepping up moves without speed
            if (cow.getNavigation().isDone()) cow.getNavigation().moveTo(goal.getX() + 0.5, goal.getY(), goal.getZ() + 0.5, 1.0);
        });
        h.succeedWhen(() -> {
            h.assertFalse(jumped[0], "the cow jumped");
            h.assertTrue(cow.getX() > goal.getX() - 2 && cow.getY() >= goal.getY() - 0.01, "cow at " + cow.position() + ", goal " + goal); // up on the top terrace
        });
    }

    /**
     * SmoothGround on a built step: grass 1 higher from x 9 gives a Grass Slab on x 8; stone 1 higher along a straight edge
     * gives a Stone Stairs facing the rise; a 2-block step stays a cliff. Glass walls (not ground) keep the strips apart.
     */
    private static void smoothGround(GameTestHelper h) {
        BlockState stone = Blocks.STONE.defaultBlockState(), grass = Blocks.GRASS_BLOCK.defaultBlockState(), dirt = Blocks.DIRT.defaultBlockState();
        for (int x = 0; x <= 15; x++) {
            for (int z = 0; z <= 15; z++) {
                h.setBlock(new BlockPos(x, 0, z), stone);
                if (z == 7) for (int y = 1; y <= 4; y++) h.setBlock(new BlockPos(x, y, z), Blocks.GLASS);
                else if (z < 7) { // grass, 1 higher from x 9
                    h.setBlock(new BlockPos(x, 1, z), x >= 9 ? dirt : grass);
                    if (x >= 9) h.setBlock(new BlockPos(x, 2, z), grass);
                } else { // stone: 2 high from x 9, 4 high from x 13 (a 2-block cliff)
                    for (int y = 1; y <= (x >= 13 ? 4 : x >= 9 ? 2 : 1); y++) h.setBlock(new BlockPos(x, y, z), stone);
                }
            }
        }
        // a flower and a tall plant at the foot of the grass step: replaced by slabs, never a hole
        h.setBlock(new BlockPos(8, 2, 3), Blocks.OXEYE_DAISY);
        h.setBlock(new BlockPos(8, 2, 5), Blocks.TALL_GRASS.defaultBlockState().setValue(
                net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER));
        h.setBlock(new BlockPos(8, 3, 5), Blocks.TALL_GRASS.defaultBlockState().setValue(
                net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
        BlockPos a = h.absolutePos(new BlockPos(2, 0, 2));
        int laid = SmoothGround.smooth(h.getLevel(), a.getX(), a.getZ(), 12);
        BlockState slab = h.getBlockState(new BlockPos(8, 2, 4)), stair = h.getBlockState(new BlockPos(8, 2, 10));
        h.assertTrue(slab.is(com.mechanical.EarthBlocks.GRASS_SLAB) && slab.getValue(net.minecraft.world.level.block.SlabBlock.TYPE)
                == net.minecraft.world.level.block.state.properties.SlabType.BOTTOM, "grass slab below the grass step, got " + slab);
        h.assertTrue(h.getBlockState(new BlockPos(8, 2, 3)).is(com.mechanical.EarthBlocks.GRASS_SLAB), "the flower's spot gets a slab, got " + h.getBlockState(new BlockPos(8, 2, 3)));
        h.assertTrue(h.getBlockState(new BlockPos(8, 2, 5)).is(com.mechanical.EarthBlocks.GRASS_SLAB) && h.getBlockState(new BlockPos(8, 3, 5)).isAir(),
                "the tall grass gives way to a slab, both halves");
        h.assertTrue(h.getBlockState(new BlockPos(8, 1, 4)).is(Blocks.DIRT), "the grass under the slab turns to dirt (one fringe per step)");
        h.assertTrue(stair.is(Blocks.STONE_STAIRS) && stair.getValue(net.minecraft.world.level.block.StairBlock.FACING) == net.minecraft.core.Direction.EAST
                && stair.getValue(net.minecraft.world.level.block.StairBlock.SHAPE) == net.minecraft.world.level.block.state.properties.StairsShape.STRAIGHT,
                "stone stairs facing the stone step, got " + stair);
        h.assertBlockPresent(Blocks.AIR, new BlockPos(12, 3, 10)); // 2 blocks below the cliff top: left alone
        h.assertBlockPresent(Blocks.AIR, new BlockPos(5, 2, 4)); // flat ground: nothing
        h.assertTrue(laid > 0, "laid " + laid);
        h.succeed();
    }

    /** Dirt Stairs take grass from a Grass Slab beside them (Grass Stairs, facing kept); under stone they die back and drop Dirt Stairs. */
    private static void grassStairsCreep(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        for (int x = 0; x <= 8; x++) for (int z = 0; z <= 8; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        BlockPos at = new BlockPos(4, 1, 4);
        h.setBlock(new BlockPos(5, 1, 4), com.mechanical.EarthBlocks.GRASS_SLAB.get());
        h.setBlock(new BlockPos(6, 2, 6), Blocks.GLOWSTONE);
        h.setBlock(at, com.mechanical.EarthBlocks.DIRT_STAIRS.get().defaultBlockState().setValue(net.minecraft.world.level.block.StairBlock.FACING, net.minecraft.core.Direction.WEST));
        h.succeedWhen(() -> { // retried until the glowstone's light is worked out
            BlockPos abs = h.absolutePos(at);
            for (int i = 0; i < 400 && level.getBlockState(abs).is(com.mechanical.EarthBlocks.DIRT_STAIRS); i++) level.getBlockState(abs).randomTick(level, abs, level.getRandom());
            BlockState s = level.getBlockState(abs);
            h.assertTrue(s.is(com.mechanical.EarthBlocks.GRASS_STAIRS) && s.getValue(net.minecraft.world.level.block.StairBlock.FACING) == net.minecraft.core.Direction.WEST,
                    "grass creeps onto the dirt stairs: " + s);
            var drops = net.minecraft.world.level.block.Block.getDrops(s, level, abs, null);
            h.assertTrue(drops.size() == 1 && drops.get(0).is(com.mechanical.EarthBlocks.DIRT_STAIRS.asItem()), "grass stairs drop dirt stairs: " + drops);
            h.setBlock(at.above(), Blocks.STONE);
            level.getBlockState(abs).randomTick(level, abs, level.getRandom());
            h.assertTrue(level.getBlockState(abs).is(com.mechanical.EarthBlocks.DIRT_STAIRS), "under stone it dies back: " + level.getBlockState(abs));
        });
    }

    /** A7 on worldgen trails (Trails.step): a stair on the column below a 2-block rise along the trail, none on flat ground. */
    private static void trailSteps(GameTestHelper h) {
        for (int x = 0; x <= 16; x++)
            for (int z = 0; z <= 16; z++) {
                h.setBlock(new BlockPos(x, 0, z), Blocks.GRASS_BLOCK.defaultBlockState());
                if (x >= 8) {
                    h.setBlock(new BlockPos(x, 1, z), Blocks.DIRT.defaultBlockState());
                    h.setBlock(new BlockPos(x, 2, z), Blocks.GRASS_BLOCK.defaultBlockState());
                }
            }
        BlockPos a = h.absolutePos(new BlockPos(0, 0, 8));
        Trails.Route r = new Trails.Route(new Trails.Node(1, a.getX(), a.getZ()), new Trails.Node(2, a.getX() + 40, a.getZ()));
        ServerLevel level = h.getLevel();
        Trails.step(level, h.absolutePos(new BlockPos(3, 0, 8)), r.alongX(0.1));
        h.assertBlockPresent(Blocks.AIR, new BlockPos(3, 1, 8));
        Trails.step(level, h.absolutePos(new BlockPos(7, 0, 8)), r.alongX(0.2));
        var s = level.getBlockState(h.absolutePos(new BlockPos(7, 1, 8)));
        h.assertTrue(s.getBlock() instanceof net.minecraft.world.level.block.StairBlock
                && s.getValue(net.minecraft.world.level.block.StairBlock.FACING) == net.minecraft.core.Direction.EAST, "stair facing up the rise, got " + s);
        h.succeed();
    }

    /** A height profile for Trails.highBridges: ground heights per block, water (surface 62) where the ground is below 62. */
    private static int[][] profile(int... ground) {
        int[] surface = new int[ground.length];
        for (int i = 0; i < ground.length; i++) surface[i] = Math.max(ground[i], 62);
        return new int[][] {ground, surface};
    }

    private static String bridges(int[][] p) {
        StringBuilder b = new StringBuilder();
        for (int[] s : Trails.highBridges(p[0], p[1])) b.append(java.util.Arrays.toString(s));
        return b.toString();
    }

    /**
     * Trails.highBridges: over a river in a cut the deck spans at the lower bank's height (ground 1 below the deck at both
     * ends), at most 12 above the water; low banks and a gentle valley keep the low deck (no high bridge).
     */
    private static void trailBridges(GameTestHelper h) {
        // a beach then a cliff up to 72 on the left, a cliff straight up to 70 on the right: deck at 71, from the left cliff top's foot of 70 to the right bank
        int[][] ravine = profile(72, 72, 72, 70, 66, 63, 63, 58, 58, 58, 58, 58, 58, 64, 68, 70, 70, 70);
        h.assertValueEqual(bridges(ravine), "[3, 15, 71]", "ravine bridge");
        // banks 30 above the water: the deck stops 12 above it, where the banks reach 73
        int[] deep = new int[40];
        for (int i = 0; i < 40; i++) deep[i] = i < 12 || i >= 28 ? 92 : i < 14 || i >= 26 ? 73 : 58;
        h.assertValueEqual(bridges(profile(deep)), "[13, 26, 74]", "deep cut");
        // banks just above the water (a deck on them would stand only 2 above it): the low deck stays
        h.assertValueEqual(bridges(profile(64, 64, 63, 63, 58, 58, 58, 58, 63, 63, 63, 63)), "", "low banks");
        // banks 2 above the water on both sides: a deck 3 above it, level with the banks
        h.assertValueEqual(bridges(profile(64, 64, 63, 63, 58, 58, 58, 58, 63, 63, 64, 64)), "[1, 10, 65]", "banks 2 up");
        // a wide, gentle valley (1 up in 4): the trail walks down to the water
        int[] gentle = new int[60];
        for (int i = 0; i < 60; i++) gentle[i] = Math.abs(i - 30) <= 3 ? 58 : 62 + (Math.abs(i - 30) - 3) / 4;
        h.assertValueEqual(bridges(profile(gentle)), "", "gentle valley");
        // a lake wider than RIVER_MAX keeps the low deck however high its shores
        int[] lake = new int[60];
        for (int i = 0; i < 60; i++) lake[i] = i < 10 || i >= 50 ? 75 : 55;
        h.assertValueEqual(bridges(profile(lake)), "", "lake");
        h.succeed();
    }

    /**
     * Trails.Route round a palisade: a trail from straight behind a walled village to its gate goes round the outside of the
     * wall (never closer than WALL_R + 3 to the centre), stays one continuous line, keeps its far end and ends on the gate's
     * line just outside the start of the village path.
     */
    private static void trailWalls(GameTestHelper h) {
        int wall = TribeVillageStructure.WALL_R;
        Trails.Node gate = new Trails.Node(2, wall + 3, 0, 0, 0, wall); // the village at 0,0, its gate (and village path start) to the east
        for (int[] from : new int[][] {{-200, 0}, {-200, 7}, {-150, -90}, {200, 3}}) {
            Trails.Node far = new Trails.Node(1, from[0], from[1]);
            Trails.Route r = new Trails.Route(far, gate, java.util.List.of(gate));
            int n = (int) Math.ceil(r.len * 2);
            double px = r.x(0), pz = r.z(0), min = Double.MAX_VALUE;
            for (int i = 1; i <= n; i++) {
                double x = r.x(i / (double) n), z = r.z(i / (double) n);
                h.assertTrue(Math.hypot(x - px, z - pz) < 1.0, "gap in the trail from " + from[0] + "," + from[1] + " at " + x + "," + z);
                min = Math.min(min, Math.hypot(x - 0.5, z - 0.5));
                px = x;
                pz = z;
            }
            h.assertTrue(min >= wall + 3, "trail from " + from[0] + "," + from[1] + " comes " + min + " from the village centre");
            h.assertTrue(Math.hypot(r.x(0) - from[0] - 0.5, r.z(0) - from[1] - 0.5) < 0.01, "far end moved");
            h.assertTrue(Math.abs(r.z(1) - 0.5) < 0.01 && r.x(1) > wall + 3 && r.x(1) < wall + 6, "gate end at " + r.x(1) + "," + r.z(1));
        }
        h.succeed();
    }

    /** Generates (or loads) the 3x3 around a chunk and returns its middle. */
    private static LevelChunk load(ServerLevel level, int cx, int cz) {
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) level.getChunk(cx + dx, cz + dz);
        return level.getChunk(cx, cz);
    }

    private static void retrogen(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        int current = Retrogen.sets(level).current();

        // freshly generated chunks already have every feature
        LevelChunk fresh = load(level, FAR, FAR);
        h.assertTrue(fresh.getExistingDataOrNull(Retrogen.MARKER) instanceof Integer m && m == current, "a freshly generated chunk is marked current");
        h.assertTrue(Retrogen.process(level, FAR, FAR) == Retrogen.Result.CURRENT, "a current chunk is left alone");

        // old chunks (no marker) get features and are re-marked. The test world is flat sandstone desert: a stone floor lets
        // loose stones (every overworld biome, about every other chunk) place, so over 12 chunks something must
        int placedBefore = Retrogen.placedFeatures;
        for (int i = 0; i < 12; i++) {
            int cx = FAR + 3 + 3 * i;
            LevelChunk old = load(level, cx, FAR);
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int bx = cx * 16 + x, bz = FAR * 16 + z;
                    level.setBlock(new BlockPos(bx, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz) - 1, bz), Blocks.STONE.defaultBlockState(), 2);
                }
            }
            old.removeData(Retrogen.MARKER);
            h.assertTrue(Retrogen.process(level, cx, FAR) == Retrogen.Result.UPDATED, "an old chunk is retrogenned");
            h.assertTrue(old.getExistingDataOrNull(Retrogen.MARKER) instanceof Integer m && m == current, "a retrogenned chunk is re-marked");
        }
        h.assertTrue(Retrogen.placedFeatures > placedBefore, "retrogen placed features in old chunks");

        // a player's chest in a neighbouring chunk: skipped (but marked, so it isn't tried again)
        LevelChunk byChest = load(level, FAR, FAR + 6);
        byChest.removeData(Retrogen.MARKER);
        int x = (FAR + 1) * 16 + 4, z = (FAR + 6) * 16 + 4;
        level.setBlock(new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z), Blocks.CHEST.defaultBlockState(), 3);
        h.assertTrue(Retrogen.process(level, FAR, FAR + 6) == Retrogen.Result.SKIPPED, "a chunk next to a chest is skipped");
        h.assertTrue(byChest.getExistingDataOrNull(Retrogen.MARKER) instanceof Integer m && m == current, "a skipped chunk is marked");

        // long lived in: skipped
        LevelChunk lived = load(level, FAR, FAR + 9);
        lived.removeData(Retrogen.MARKER);
        lived.setInhabitedTime(Retrogen.MAX_INHABITED * 10);
        h.assertTrue(Retrogen.process(level, FAR, FAR + 9) == Retrogen.Result.SKIPPED, "a chunk players lived in is skipped");
        h.succeed();
    }

    private static void restock(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos at = h.absolutePos(new BlockPos(7, 1, 7));
        AABB area = new AABB(at).inflate(64); // the area pick() looks at
        List<EntityType<?>> types = WildlifeRestock.candidates(level, at).stream().<EntityType<?>>map(w -> w.value().type()).distinct().toList();
        h.assertTrue(types.size() >= 2, "the test biome has at least two species (" + types + ")");
        EntityType<?> missing = types.stream().filter(t -> level.getEntities(t, area, Entity::isAlive).isEmpty()).reduce((a, b) -> b).orElse(null);
        h.assertTrue(missing != null, "some species is not around yet");
        // every other species gets a living member right here
        List<Entity> added = new ArrayList<>();
        for (EntityType<?> type : types) {
            if (type == missing || !level.getEntities(type, area, Entity::isAlive).isEmpty()) continue;
            Entity e = type.create(level, EntitySpawnReason.COMMAND);
            if (e == null) continue;
            if (e instanceof Mob mob) mob.setNoAi(true);
            e.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
            level.addFreshEntity(e);
            added.add(e);
        }
        for (int i = 0; i < 20; i++) {
            EntityType<?> picked = WildlifeRestock.pick(level, at, level.getRandom()).map(d -> d.type()).orElse(null);
            h.assertTrue(picked == missing, "restock picks the missing species " + missing + ", not " + picked);
        }
        added.forEach(Entity::discard);
        h.succeed();
    }
}
