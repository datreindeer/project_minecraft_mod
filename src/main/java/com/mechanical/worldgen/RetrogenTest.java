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
            WALK_UP = key("smooth_ground_walk_up");
    /** Far from the test grid, so features placed here can't land in another test. */
    private static final int FAR = 400;

    static {
        TEST_FUNCTIONS.register("retrogen", () -> RetrogenTest::retrogen);
        TEST_FUNCTIONS.register("restock_missing_species", () -> RetrogenTest::restock);
        TEST_FUNCTIONS.register("trail_steps", () -> RetrogenTest::trailSteps);
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
        BlockPos a = h.absolutePos(new BlockPos(2, 0, 2));
        int laid = SmoothGround.smooth(h.getLevel(), a.getX(), a.getZ(), 12);
        BlockState slab = h.getBlockState(new BlockPos(8, 2, 4)), stair = h.getBlockState(new BlockPos(8, 2, 10));
        h.assertTrue(slab.is(com.mechanical.EarthBlocks.GRASS_SLAB) && slab.getValue(net.minecraft.world.level.block.SlabBlock.TYPE)
                == net.minecraft.world.level.block.state.properties.SlabType.BOTTOM, "grass slab below the grass step, got " + slab);
        BlockPos under = h.absolutePos(new BlockPos(8, 1, 4));
        for (int i = 0; i < 20; i++) h.getLevel().getBlockState(under).randomTick(h.getLevel(), under, h.getLevel().getRandom());
        h.assertTrue(h.getBlockState(new BlockPos(8, 1, 4)).is(Blocks.GRASS_BLOCK), "the grass under the slab stays grass");
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
        Trails.step(level, h.absolutePos(new BlockPos(3, 0, 8)), r);
        h.assertBlockPresent(Blocks.AIR, new BlockPos(3, 1, 8));
        Trails.step(level, h.absolutePos(new BlockPos(7, 0, 8)), r);
        var s = level.getBlockState(h.absolutePos(new BlockPos(7, 1, 8)));
        h.assertTrue(s.getBlock() instanceof net.minecraft.world.level.block.StairBlock
                && s.getValue(net.minecraft.world.level.block.StairBlock.FACING) == net.minecraft.core.Direction.EAST, "stair facing up the rise, got " + s);
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
