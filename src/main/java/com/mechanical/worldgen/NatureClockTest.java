package com.mechanical.worldgen;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.mechanical.FallingTreeEntity;
import com.mechanical.TreeFelling;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The nature clock (terrain-plan phases 5-7) on hand-built ground, its days run by hand ({@link NatureClock#simulate}, only
 * inside the test's box): a fallen log left 10 days has rotted away into Moss Block and Podzol with a sapling beside it,
 * and a storm-felled tree comes to rest as a fallen log of log blocks. Phase 6: a burnt field grows back into a young
 * forest in 300 days, a forest edge creeps out into the meadow (not where cows graze), an old tree dies into a snag that
 * later falls as a Deadwood log. Phase 7: a rock high on a cliff tumbles down and lies at the foot with its talus, a spring
 * flood rises a block over a creek and its flat banks and goes down leaving nothing of its water, a drift piles up in the
 * lee of a boulder.
 */
@EventBusSubscriber(modid = MODID)
public final class NatureClockTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final List<String> TESTS = List.of("nature_rot", "storm_felling", "nature_regrowth", "forest_creep", "forest_creep_grazed", "old_tree_snag", "rockfall", "spring_flood", "snow_drift");
    /** A day in early summer of year 2 (calendar day numbers count from the start of year 1). */
    static final long SUMMER = 360 + 95;

    static {
        TEST_FUNCTIONS.register("nature_rot", () -> NatureClockTest::rot);
        TEST_FUNCTIONS.register("storm_felling", () -> NatureClockTest::storm);
        TEST_FUNCTIONS.register("nature_regrowth", () -> NatureClockTest::regrowth);
        TEST_FUNCTIONS.register("forest_creep", () -> h -> creep(h, false));
        TEST_FUNCTIONS.register("forest_creep_grazed", () -> h -> creep(h, true));
        TEST_FUNCTIONS.register("old_tree_snag", () -> NatureClockTest::snag);
        TEST_FUNCTIONS.register("rockfall", () -> NatureClockTest::rockfall);
        TEST_FUNCTIONS.register("spring_flood", () -> NatureClockTest::flood);
        TEST_FUNCTIONS.register("snow_drift", () -> NatureClockTest::drift);
    }

    static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "nature_clock"), new TestEnvironmentDefinition.AllOf(List.of()));
        for (String name : TESTS)
            event.registerTest(Identifier.fromNamespaceAndPath(MODID, name), new FunctionGameTestInstance(
                    ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, name)), com.mechanical.TestArea.of(env, 200, true)));
    }

    /** Stone, dirt and grass up to y top in every column of 0..16, air above. */
    static void ground(GameTestHelper h, int top) {
        for (int x = 0; x <= 16; x++)
            for (int z = 0; z <= 16; z++) {
                h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                for (int y = 1; y <= top; y++) h.setBlock(new BlockPos(x, y, z), y == top ? Blocks.GRASS_BLOCK : Blocks.DIRT);
                for (int y = top + 1; y <= top + 60; y++) h.setBlock(new BlockPos(x, y, z), Blocks.AIR); // open sky: the clock reads the heightmaps
            }
    }

    /** The test's own columns 0..16, every height: nature only changes blocks in here. */
    static BoundingBox box(GameTestHelper h) {
        BlockPos a = h.absolutePos(BlockPos.ZERO), b = h.absolutePos(new BlockPos(16, 0, 16));
        return new BoundingBox(Math.min(a.getX(), b.getX()), h.getLevel().getMinY(), Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()), h.getLevel().getMaxY(), Math.max(a.getZ(), b.getZ()));
    }

    private static void rot(GameTestHelper h) {
        ground(h, 3);
        for (int x = 4; x <= 8; x++) h.setBlock(new BlockPos(x, 4, 8), Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        NatureClock.simulate(h.getLevel(), box(h), SUMMER, 10, Set.of("rot"));
        int soil = 0;
        for (int x = 4; x <= 8; x++) {
            for (int y = 3; y <= 4; y++) {
                BlockState s = h.getBlockState(new BlockPos(x, y, 8));
                if (s.is(BlockTags.LOGS)) h.fail("log still at " + x + " " + y + " after 10 days: " + s);
                if (s.is(Blocks.MOSS_BLOCK) || s.is(Blocks.PODZOL)) soil++;
            }
        }
        h.assertTrue(soil >= 3, "the log sank into Moss Block and Podzol (" + soil + ")");
        boolean sapling = false;
        for (int x = 3; x <= 9; x++) for (int z = 7; z <= 9; z += 2) sapling |= h.getBlockState(new BlockPos(x, 4, z)).is(Blocks.OAK_SAPLING);
        h.assertTrue(sapling, "an oak sapling came up beside the log");
        h.succeed();
    }

    private static void storm(GameTestHelper h) {
        ground(h, 3);
        for (int y = 4; y <= 10; y++) h.setBlock(new BlockPos(4, y, 8), Blocks.OAK_LOG);
        for (int y = 9; y <= 11; y++)
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++) {
                    if (y <= 10 && dx == 0 && dz == 0) continue;
                    int dist = y == 11 ? (dx == 0 && dz == 0 ? 1 : 2) : Math.abs(dx) + Math.abs(dz);
                    h.setBlock(new BlockPos(4 + dx, y, 8 + dz), Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE, dist));
                }
        BlockPos base = h.absolutePos(new BlockPos(4, 4, 8));
        TreeFelling.Tree tree = TreeFelling.find(h.getLevel(), base);
        h.assertTrue(tree != null, "the tree is found");
        Direction east = Direction.EAST;
        StormFelling.fell(h.getLevel(), base, tree, east);
        h.runAfterDelay(FallingTreeEntity.FALL_TICKS + 10, () -> {
            int lying = 0;
            for (int x = 5; x <= 16; x++) {
                BlockState s = h.getBlockState(new BlockPos(x, 4, 8));
                if (s.is(Blocks.OAK_LOG) && s.getValue(RotatedPillarBlock.AXIS) == east.getAxis()) lying++;
            }
            h.assertTrue(lying >= 5, "the trunk lies as a fallen log of oak logs (" + lying + ")");
            h.assertTrue(!h.getBlockState(new BlockPos(4, 6, 8)).is(Blocks.OAK_LOG), "the tree is down");
            h.assertTrue(h.getLevel().getEntitiesOfClass(com.mechanical.FelledLogEntity.class, new net.minecraft.world.phys.AABB(base).inflate(20)).isEmpty(),
                    "no felled log entity: the storm leaves log blocks");
            h.succeed();
        });
    }

    /** An oak at x, z: a trunk of h on the ground at y top+1 and a crown with the right leaf distances (TreeFelling finds it). */
    static void oak(GameTestHelper h, int x, int z, int top, int height) {
        int t = top + height;
        for (int y = top + 1; y <= t; y++) h.setBlock(new BlockPos(x, y, z), Blocks.OAK_LOG);
        for (int y = t - 1; y <= t + 1; y++)
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++) {
                    if (y <= t && dx == 0 && dz == 0) continue;
                    int dist = y == t + 1 ? (dx == 0 && dz == 0 ? 1 : 2) : Math.abs(dx) + Math.abs(dz);
                    h.setBlock(new BlockPos(x + dx, y, z + dz), Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE, dist));
                }
    }

    private static void regrowth(GameTestHelper h) {
        ground(h, 3);
        oak(h, 5, 5, 3, 5);
        oak(h, 11, 9, 3, 6);
        Wildfire.burnField(h.getLevel(), h.absolutePos(new BlockPos(8, 3, 8)), 6, net.minecraft.util.RandomSource.create(5), null);
        h.assertTrue(h.getBlockState(new BlockPos(5, 5, 5)).is(ScorchedBarrens.CHARRED_LOG.get()), "the burnt oak stands as Charred Log");
        int ash = count(h, 3, s -> s.is(ScorchedBarrens.WOOD_ASH.get()) || s.is(ScorchedBarrens.BURNT_SOIL.get()));
        h.assertTrue(ash > 40, "the burnt field is Burnt Soil and Wood Ash (" + ash + ")");
        int changed = NatureClock.simulate(h.getLevel(), box(h), SUMMER, 300, Set.of("regrowth", "grow"));
        long burnDay = NatureClock.data(h.getLevel().getChunkAt(h.absolutePos(new BlockPos(8, 3, 8)))).burnDay;
        // not under a charred stump: its lowest block never crumbles, so the ground under it never sees the sky (was 2 of the 3 allowed)
        int left = 0;
        for (int x = 0; x <= 16; x++)
            for (int z = 0; z <= 16; z++) {
                BlockState s = h.getBlockState(new BlockPos(x, 3, z));
                if ((s.is(ScorchedBarrens.WOOD_ASH.get()) || s.is(ScorchedBarrens.BURNT_SOIL.get()) || s.is(Blocks.COARSE_DIRT))
                        && !h.getBlockState(new BlockPos(x, 4, z)).is(ScorchedBarrens.CHARRED_LOG.get())) left++;
            }
        h.assertTrue(left <= 3, "the ash has turned back to grass (" + left + " burnt blocks left: " + count(h, 3, s -> s.is(ScorchedBarrens.WOOD_ASH.get())) + " ash, "
                + count(h, 3, s -> s.is(ScorchedBarrens.BURNT_SOIL.get())) + " burnt soil, " + count(h, 4, s -> s.is(ScorchedBarrens.WOOD_ASH.get())) + " ash a block up, " + changed + " changes)");
        int trees = 0;
        for (int x = 0; x <= 16; x++)
            for (int z = 0; z <= 16; z++)
                if (h.getBlockState(new BlockPos(x, 4, z)).is(BlockTags.LOGS) && h.getBlockState(new BlockPos(x, 6, z)).is(BlockTags.LOGS)) trees++;
        h.assertTrue(trees >= 3, "young trees have grown on the burnt field (" + trees + ")");
        h.succeed();
    }

    private static int count(GameTestHelper h, int y, java.util.function.Predicate<BlockState> what) {
        int n = 0;
        for (int x = 0; x <= 16; x++) for (int z = 0; z <= 16; z++) if (what.test(h.getBlockState(new BlockPos(x, y, z)))) n++;
        return n;
    }

    private static void creep(GameTestHelper h, boolean grazed) {
        ground(h, 3);
        oak(h, 2, 8, 3, 5);
        if (grazed)
            for (int i = 0; i < 2; i++) {
                var cow = h.spawn(net.minecraft.world.entity.EntityTypes.COW, new BlockPos(9 + i * 3, 4, 8));
                cow.setNoAi(true);
            }
        int changed = NatureClock.simulate(h.getLevel(), box(h), SUMMER, 200, Set.of("creep"));
        // saplings that came up, or have grown into trees already (the clock grows saplings too)
        int saplings = count(h, 4, s -> s.is(Blocks.OAK_SAPLING) || s.is(Blocks.OAK_LOG)) - 1;
        if (grazed) h.assertTrue(saplings == 0, "grazed meadow stays open (" + saplings + " saplings)");
        else h.assertTrue(saplings >= 2, "oak saplings crept out from the edge (" + saplings + ", " + changed + " changes)");
        for (int x = 0; x <= 16; x++)
            for (int z = 0; z <= 16; z++)
                if (h.getBlockState(new BlockPos(x, 4, z)).is(Blocks.OAK_SAPLING) && (x != 2 || z != 8) && Math.max(Math.abs(x - 2), Math.abs(z - 8)) < ForestCreep.CREEP_NEAR)
                    h.fail("a sapling right under the tree at " + x + " " + z);
        h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.animal.cow.Cow.class, new net.minecraft.world.phys.AABB(h.absolutePos(BlockPos.ZERO)).inflate(24)).forEach(e -> e.discard());
        h.succeed();
    }

    private static void snag(GameTestHelper h) {
        ground(h, 3);
        oak(h, 8, 8, 3, 6);
        BlockPos base = h.absolutePos(new BlockPos(8, 4, 8));
        var day = new NatureClock.Day(h.getLevel(), h.getLevel().getChunkAt(base), SUMMER, box(h), true, null);
        h.assertTrue(ForestCreep.kill(day, base, net.minecraft.util.RandomSource.create(3)), "the old oak dies");
        h.assertTrue(h.getBlockState(new BlockPos(8, 4, 8)).is(Blocks.OAK_LOG), "bark at the foot");
        int top = -1;
        for (int y = 5; y <= 14; y++) if (h.getBlockState(new BlockPos(8, y, 8)).is(com.mechanical.Deadwood.BROKEN_DEADWOOD.get())) top = y;
        h.assertTrue(top >= 7, "a Deadwood trunk with a broken top (" + top + ")");
        h.assertTrue(count(h, 9, s -> s.is(Blocks.OAK_LEAVES)) == 0, "the crown is gone");
        h.assertTrue(ForestCreep.topple(day, h.absolutePos(new BlockPos(8, top, 8)), net.minecraft.util.RandomSource.create(4)), "the snag falls");
        h.assertTrue(!h.getBlockState(new BlockPos(8, 6, 8)).is(com.mechanical.Deadwood.DEADWOOD.get()), "the snag is down");
        h.assertTrue(count(h, 4, s -> s.is(com.mechanical.Deadwood.DEADWOOD.get())) >= 2, "it lies as a Deadwood log");
        h.succeed();
    }

    /** A day for each chunk the test's box touches. */
    private static List<NatureClock.Day> days(GameTestHelper h, long day) {
        BoundingBox b = box(h);
        List<NatureClock.Day> out = new java.util.ArrayList<>();
        for (int cx = b.minX() >> 4; cx <= b.maxX() >> 4; cx++)
            for (int cz = b.minZ() >> 4; cz <= b.maxZ() >> 4; cz++) out.add(new NatureClock.Day(h.getLevel(), h.getLevel().getChunk(cx, cz), day, b, true, null));
        return out;
    }

    private static NatureClock.Day day(GameTestHelper h, long day) {
        return new NatureClock.Day(h.getLevel(), h.getLevel().getChunkAt(h.absolutePos(new BlockPos(8, 0, 8))), day, box(h), true, null);
    }

    private static void rockfall(GameTestHelper h) {
        ground(h, 3);
        for (int x = 0; x <= 3; x++) for (int z = 0; z <= 16; z++) for (int y = 4; y <= 16; y++) h.setBlock(new BlockPos(x, y, z), Blocks.STONE);
        var d = day(h, SUMMER);
        BlockPos rock = h.absolutePos(new BlockPos(3, 16, 8));
        h.assertTrue(Rockfall.loose(d, rock), "the cliff's edge rock is loose");
        h.assertTrue(Rockfall.fall(d, rock, net.minecraft.util.RandomSource.create(2)), "it falls");
        h.assertTrue(h.getBlockState(new BlockPos(3, 16, 8)).isAir(), "gone from the cliff top");
        int talus = 0;
        for (int x = 4; x <= 10; x++)
            for (int z = 5; z <= 11; z++) {
                BlockState s = h.getBlockState(new BlockPos(x, 4, z)), g = h.getBlockState(new BlockPos(x, 3, z));
                if (s.is(Blocks.STONE) || s.is(Blocks.COBBLESTONE) || s.getBlock() instanceof com.mechanical.RockBlock || g.is(Blocks.GRAVEL)) talus++;
            }
        h.assertTrue(talus >= 2, "the rock and its talus lie at the foot (" + talus + ")");
        h.succeed();
    }

    private static void flood(GameTestHelper h) {
        ground(h, 3);
        for (int x = 0; x <= 16; x++)
            for (int z = 0; z <= 16; z++) {
                if (x >= 7 && x <= 9) h.setBlock(new BlockPos(x, 3, z), Blocks.WATER); // the creek, level with its banks
                if (x <= 1 || x >= 15) h.setBlock(new BlockPos(x, 4, z), Blocks.GRASS_BLOCK); // higher ground holds the flood in
            }
        var ds = days(h, SUMMER);
        h.assertTrue(ds.stream().mapToInt(SpringFlood::flood).sum() > 30, "the creek floods");
        h.assertTrue(h.getBlockState(new BlockPos(8, 4, 8)).is(Blocks.WATER), "the creek rose a block");
        h.assertTrue(h.getBlockState(new BlockPos(4, 4, 8)).is(Blocks.WATER) && h.getBlockState(new BlockPos(12, 4, 8)).is(Blocks.WATER), "the flat banks are under water");
        h.assertTrue(!h.getBlockState(new BlockPos(0, 5, 8)).is(Blocks.WATER), "not up on the higher ground");
        h.runAfterDelay(40, () -> {
            ds.forEach(d -> SpringFlood.recede(d, net.minecraft.util.RandomSource.create(1)));
            h.runAfterDelay(90, () -> { // water that spilled past the test's edge drains back first
                int left = 0;
                for (int x = 2; x <= 14; x++) for (int z = 0; z <= 16; z++) if (!h.getBlockState(new BlockPos(x, 4, z)).getFluidState().isEmpty()) left++;
                h.assertTrue(left == 0, "every block of flood water is gone (" + left + " left)");
                h.assertTrue(h.getBlockState(new BlockPos(8, 3, 8)).is(Blocks.WATER), "the creek itself is still there");
                h.assertTrue(ds.stream().allMatch(d -> NatureClock.data(d.chunk).flood.isEmpty()), "nothing left on the list");
                h.succeed();
            });
        });
    }

    private static void drift(GameTestHelper h) {
        ground(h, 3);
        h.setBlock(new BlockPos(5, 4, 8), Blocks.MOSSY_COBBLESTONE);
        h.setBlock(new BlockPos(5, 5, 8), Blocks.STONE);
        var ds = days(h, SUMMER);
        for (int i = 0; i < 6; i++) ds.forEach(d -> SnowDrift.drift(d, Direction.EAST));
        int[] layers = new int[6];
        for (int k = 1; k <= 5; k++) {
            BlockState s = h.getBlockState(new BlockPos(5 + k, 4, 8));
            layers[k] = s.is(Blocks.SNOW) ? s.getValue(net.minecraft.world.level.block.SnowLayerBlock.LAYERS) : 0;
        }
        h.assertTrue(layers[1] >= 3 && layers[1] >= layers[3] && layers[3] >= layers[5], "a drift deepest at the boulder, thinning downwind: " + java.util.Arrays.toString(layers));
        h.assertTrue(!h.getBlockState(new BlockPos(4, 4, 8)).is(Blocks.SNOW), "nothing on the windward side");
        h.succeed();
    }
}
