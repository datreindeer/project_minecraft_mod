package com.mechanical;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Game test for {@link FelledLogEntity}: a felled 5-log tree lies as one solid log of 5 blocks (no wall beside it);
 * cutting out the middle block splits it into two logs of 2 (with a hewn log's drops); one player lifts the root piece
 * by hand and carries it, then loads it on a Wagon with the click a real right-click makes, and unloads it; two players
 * carry an 8-block log between them; a 14-block log can't be lifted, one Lead can't move it and two can.
 */
@EventBusSubscriber(modid = MODID)
public final class FelledLogTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> TEST = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "felled_log"));

    static {
        TEST_FUNCTIONS.register("felled_log", () -> FelledLogTest::test);
        TEST_FUNCTIONS.register("felled_log_rest", () -> FelledLogTest::rest);
        TEST_FUNCTIONS.register("felled_log_slide", () -> FelledLogTest::slide);
        TEST_FUNCTIONS.register("felled_log_old_world", () -> FelledLogTest::oldWorld);
        TEST_FUNCTIONS.register("felled_log_drag", () -> FelledLogTest::drag);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "felled_log"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "felled_log"),
                new FunctionGameTestInstance(TEST, TestArea.of(env, 520, true)));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "felled_log_rest"),
                new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "felled_log_rest")), TestArea.of(env, 160, true)));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "felled_log_slide"),
                new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "felled_log_slide")), TestArea.of(env, 220, true)));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "felled_log_old_world"),
                new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "felled_log_old_world")), TestArea.of(env, 100, true)));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "felled_log_drag"),
                new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "felled_log_drag")), TestArea.of(env, 120, true)));
    }

    private static List<FelledLogEntity> logs(GameTestHelper h) {
        return h.getLevel().getEntitiesOfClass(FelledLogEntity.class, new AABB(h.absolutePos(BlockPos.ZERO)).expandTowards(16, 16, 16).inflate(1));
    }

    /** Does anything an entity collides with (the log's per-block shape) overlap {@code box}? */
    private static boolean collides(ServerLevel level, AABB box) {
        var b = Shapes.create(box);
        return level.getEntityCollisions(null, box).stream().anyMatch(v -> Shapes.joinIsNotEmpty(v, b, BooleanOp.AND));
    }

    /**
     * Does any point inside the log (a grid through each block, its skin left out) lie in a block's collision shape? Checked
     * point by point, apart from the log's own collision code.
     */
    private static String pokes(ServerLevel level, FelledLogEntity log) {
        for (int i = 0; i < log.size(); i++) {
            Vec3 c = log.localCell(i);
            double bottom = log.getY() + c.y + log.liftOffset(i);
            for (double x = -0.42; x <= 0.43; x += 0.21)
                for (double z = -0.42; z <= 0.43; z += 0.21)
                    for (double y = 0.06; y < 0.95; y += 0.22) {
                        double up = log.lifterCount() == 0 ? log.tilt() * z : 0; // tilted: the bottom rises along it
                        Vec3 w = log.toWorld(new Vec3(c.x + x, 0, c.z + z)).add(0, bottom + up + y, 0);
                        BlockPos p = BlockPos.containing(w);
                        for (AABB b : level.getBlockState(p).getCollisionShape(level, p).toAabbs())
                            if (b.move(p).contains(w)) return "block " + i + " in " + level.getBlockState(p) + " at " + w;
                    }
        }
        return null;
    }

    /**
     * A log saved in an old world, from before felled trees came down as straight logs: one entity in the tree's shape (a
     * trunk listed crown first, a branch beside it). Loaded, it falls apart into straight logs, one per row, which rest by
     * today's rule: on top of the slabs under them, not sunk into them as the old block-centre test let them.
     */
    private static void oldWorld(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        for (int x = 0; x <= 16; x++) for (int z = 0; z <= 16; z++) {
            h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            h.setBlock(new BlockPos(x, 1, z), Blocks.SMOOTH_STONE_SLAB);
            for (int y = 2; y <= 8; y++) h.setBlock(new BlockPos(x, y, z), Blocks.AIR);
        }
        FelledLogEntity made = FelledLogEntity.spawnStraight(level, h.absoluteVec(new Vec3(8.5, 4, 4.5)), 0, 5, Blocks.OAK_LOG.defaultBlockState());
        var out = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess());
        made.saveWithoutId(out);
        made.discard();
        net.minecraft.nbt.CompoundTag tag = out.buildResult();
        tag.putIntArray("cells", new int[] {0, 0, 3, 0, 0, 0, 0, 0, 1, 0, 0, 2, 1, 0, 2}); // the old shape: trunk crown first, a branch beside block 2
        tag.remove("UUID");
        FelledLogEntity old = new FelledLogEntity(Mechanical.FELLED_LOG.get(), level);
        old.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(), tag));
        h.assertTrue(old.size() == 5 && !old.straight(), "loaded in its old shape");
        level.addFreshEntity(old);
        h.runAfterDelay(60, () -> {
            List<FelledLogEntity> now = logs(h);
            h.assertTrue(old.isRemoved() && now.size() == 2, "it fell apart into the trunk and the branch (" + now.size() + " logs)");
            h.assertTrue(now.stream().allMatch(FelledLogEntity::straight) && now.stream().mapToInt(FelledLogEntity::size).sorted().boxed().toList().equals(List.of(1, 4)),
                    "straight logs of 4 and 1, got " + now.stream().map(FelledLogEntity::size).toList());
            double slabTop = h.absoluteVec(new Vec3(0, 1.5, 0)).y;
            for (FelledLogEntity l : now) {
                h.assertTrue(Math.abs(l.getY() - slabTop) < 0.01, "it rests on the slabs (y " + l.getY() + ", want " + slabTop + ")");
                String p = pokes(level, l);
                h.assertTrue(p == null, "nothing of it in a slab: " + p);
                l.discard();
            }
            h.succeed();
        });
    }

    /**
     * The biggest log one player drags by an end (12 blocks) goes at a slow walk, not a crawl: he walks at least 1.2 m/s
     * under it (sneaking is 1.3) and the log keeps up with him.
     */
    private static void drag(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        for (int x = 0; x <= 30; x++) for (int z = 0; z <= 16; z++) {
            h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            for (int y = 1; y <= 4; y++) h.setBlock(new BlockPos(x, y, z), Blocks.AIR);
        }
        FelledLogEntity log = FelledLogEntity.spawnStraight(level, h.absoluteVec(new Vec3(16.5, 1, 8.5)), 90, FelledLogEntity.HAND_MAX, Blocks.OAK_LOG.defaultBlockState()); // crown towards -x
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        double[] start = new double[2];
        h.runAfterDelay(10, () -> {
            player.setPos(log.endPoint(true).add(0.8, 0, 0));
            log.lift(level, player, 0);
            h.assertTrue(log.rootLifted(), "lifted by the root end");
            start[0] = log.endPoint(true).x;
            start[1] = player.getX();
        });
        // he walks east away from it at his burdened walking pace (4.317 m/s at the default speed 0.1)
        for (int t = 11; t <= 70; t++) h.runAfterDelay(t, () -> player.setPos(player.position().add(0.21585 * player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) / 0.1, 0, 0)));
        h.runAfterDelay(75, () -> {
            double pace = player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) / 0.1 * 4.317, walked = player.getX() - start[1], moved = log.endPoint(true).x - start[0];
            h.assertTrue(pace >= 1.2, "a slow walk under the log, not a crawl (" + String.format("%.2f", pace) + " m/s)");
            h.assertTrue(moved >= 3.0 && moved >= walked - 1.0, "the log keeps up with him: " + String.format("%.2f of %.2f", moved, walked) + " blocks in 3 s (a crawl was 0.2)");
            log.putDown(player);
            log.discard();
            h.succeed();
        });
    }

    /**
     * Heavy logs and friction: a 5-block log lying over a one-block step stays put, and so does a 2-block one tipped over
     * it; on a staircase rising a block every block a 4-block log slides down, but slowly (never faster than MAX_SLIDE a
     * tick, gathering speed from rest), until level ground stops it.
     */
    private static void slide(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockState oak = Blocks.OAK_LOG.defaultBlockState();
        for (int x = 0; x <= 15; x++) for (int z = 0; z <= 15; z++) {
            h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            for (int y = 1; y <= 12; y++) h.setBlock(new BlockPos(x, y, z), Blocks.AIR);
        }
        for (int z = 3; z <= 14; z++) for (int y = 1; y <= Math.min(z - 2, 8); y++) h.setBlock(new BlockPos(3, y, z), Blocks.STONE); // the staircase
        for (int z = 6; z <= 14; z++) {
            h.setBlock(new BlockPos(8, 1, z), Blocks.STONE); // the steps
            h.setBlock(new BlockPos(12, 1, z), Blocks.STONE);
        }
        FelledLogEntity steep = FelledLogEntity.spawnStraight(level, h.absoluteVec(new Vec3(3.5, 10, 7.5)), 0, 4, oak);
        FelledLogEntity step = FelledLogEntity.spawnStraight(level, h.absoluteVec(new Vec3(8.5, 2, 4.5)), 0, 5, oak); // root at z 4: blocks 4, 5 over the floor, 6..8 on the step
        FelledLogEntity tipped = FelledLogEntity.spawnStraight(level, h.absoluteVec(new Vec3(12.5, 2, 5.5)), 0, 2, oak); // blocks 5 (over the floor) and 6 (on the step)
        double[] at = new double[4];
        h.runAfterDelay(40, () -> { // landed
            at[0] = step.getZ();
            at[1] = step.getY();
            at[2] = tipped.getZ();
        });
        for (int t = 41; t <= 200; t++) h.runAfterDelay(t, () -> {
            double moved = at[3] == 0 ? 0 : Math.abs(steep.getZ() - at[3]);
            h.assertTrue(moved <= FelledLogEntity.MAX_SLIDE + 1.0E-6, "it slides no faster than " + FelledLogEntity.MAX_SLIDE + " a tick (" + moved + ")");
            at[3] = steep.getZ();
        });
        h.runAfterDelay(200, () -> {
            h.assertTrue(Math.abs(step.getZ() - at[0]) < 0.01 && Math.abs(step.getY() - at[1]) < 0.01, "a log over a one-block step stays put (" + (step.getZ() - at[0]) + ", tilt " + step.tilt() + ")");
            h.assertTrue(Math.abs(tipped.getZ() - at[2]) < 0.01, "a short one tipped over the step too (" + (tipped.getZ() - at[2]) + ", tilt " + tipped.tilt() + ")");
            h.assertTrue(7.5 - h.relativeVec(steep.position()).z > 2, "the log on the staircase slid down it (" + h.relativeVec(steep.position()) + ", tilt " + steep.tilt() + ")");
            for (FelledLogEntity l : logs(h)) l.discard();
            h.succeed();
        });
    }

    /**
     * Logs rest on the real block shapes: a turned log dropped on slabs lies on their tops, one reaching half over a slab
     * never sinks into it, and a lifted log carried into a wall at a slant stops at it without a corner going in.
     */
    private static void rest(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        for (FelledLogEntity l : logs(h)) l.discard();
        for (int x = 0; x <= 15; x++) for (int z = 0; z <= 15; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        BlockState slab = Blocks.SMOOTH_STONE_SLAB.defaultBlockState();
        for (int x = 0; x <= 6; x++) for (int z = 1; z <= 7; z++) h.setBlock(new BlockPos(x, 1, z), slab);
        for (int x = 8; x <= 11; x++) for (int z = 1; z <= 2; z++) h.setBlock(new BlockPos(x, 1, z), slab);
        for (int z = 0; z <= 15; z++) for (int y = 1; y <= 2; y++) h.setBlock(new BlockPos(15, y, z), Blocks.DIRT);
        BlockState oak = Blocks.OAK_LOG.defaultBlockState();
        FelledLogEntity onSlabs = FelledLogEntity.spawnStraight(level, h.absoluteVec(new Vec3(3.0, 3.2, 2.6)), 30, 4, oak);
        FelledLogEntity half = FelledLogEntity.spawnStraight(level, h.absoluteVec(new Vec3(9.7, 3.2, 1.5)), 10, 3, oak); // root end over the slabs, crown on the floor
        FelledLogEntity carried = FelledLogEntity.spawnStraight(level, h.absoluteVec(new Vec3(10.5, 1, 9.5)), -70, 3, oak); // crown towards the wall, slanting
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        h.runAfterDelay(40, () -> {
            double slabTop = h.absoluteVec(new Vec3(0, 1.5, 0)).y;
            h.assertTrue(Math.abs(onSlabs.getY() - slabTop) < 0.01 && onSlabs.tilt() == 0, "a turned log on slabs lies on their tops (y " + onSlabs.getY() + ", want " + slabTop + ")");
            String p = pokes(level, onSlabs);
            h.assertTrue(p == null, "nothing of it in a slab: " + p);
            p = pokes(level, half);
            h.assertTrue(p == null, "a log half over a slab doesn't sink into it: " + p + " (y " + half.getY() + ", tilt " + half.tilt() + ")");
            h.assertTrue(half.getY() > h.absoluteVec(new Vec3(0, 1.0, 0)).y + 0.01, "it lies on the slab's edge, above the floor (y " + half.getY() + ")");
            player.setPos(carried.endPoint(true).add(Vec3.directionFromRotation(0, carried.getYRot()).scale(-0.5)));
            carried.lift(level, player, 0);
            h.assertTrue(carried.rootLifted(), "lifted by the root end");
        });
        // the lifter walks east into the wall, pushing the log's crown end into it at a slant
        for (int t = 41; t <= 85; t++) h.runAfterDelay(t, () -> player.setPos(player.position().add(0.12, 0, 0.02)));
        h.runAfterDelay(105, () -> {
            String p = pokes(level, carried);
            h.assertTrue(p == null, "carried into a wall at a slant, no corner goes in: " + p);
            Vec3 tip = carried.endPoint(false);
            h.assertTrue(tip.x < h.absoluteVec(new Vec3(15, 0, 0)).x + 0.01 && carried.endPoint(true).distanceTo(h.absoluteVec(new Vec3(10.5, 1, 9.5))) > 1,
                    "it moved and stopped at the wall (crown end at " + tip + ")");
            carried.putDown(player);
        });
        h.runAfterDelay(130, () -> {
            String p = pokes(level, carried);
            h.assertTrue(p == null, "put down by the wall, it lies clear of it: " + p);
            for (FelledLogEntity l : logs(h)) l.discard();
            h.succeed();
        });
    }

    private static void test(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        for (FelledLogEntity l : logs(h)) l.discard(); // left over from an earlier run (saved entities load after the area is cleared)
        for (int x = 0; x <= 16; x++) for (int z = 0; z <= 16; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        // a small natural oak: 5 logs and a few leaves round the top
        for (int y = 1; y <= 5; y++) h.setBlock(new BlockPos(3, y, 3), Blocks.OAK_LOG);
        h.setBlock(new BlockPos(4, 3, 3), Blocks.OAK_LOG.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, Direction.Axis.X)); // a branch out east
        var leaf = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE, 1).setValue(LeavesBlock.PERSISTENT, false);
        for (BlockPos p : List.of(new BlockPos(2, 5, 3), new BlockPos(4, 5, 3), new BlockPos(3, 5, 2), new BlockPos(3, 5, 4), new BlockPos(3, 6, 3)))
            h.setBlock(p, leaf);
        BlockPos cut = h.absolutePos(new BlockPos(3, 1, 3));
        TreeFelling.Tree tree = TreeFelling.find(level, cut);
        h.assertTrue(tree != null && tree.logs().size() == 6, "a natural 5-log tree with a branch (" + tree + ")");
        Player player = h.makeMockPlayer(GameType.SURVIVAL), second = h.makeMockPlayer(GameType.SURVIVAL);
        TreeFelling.fell(level, cut, tree, Direction.SOUTH, player, new ItemStack(Mechanical.FLINT_AXE.get()));

        // all callbacks are registered here: adding them from inside one makes the test runner run that one twice
        int a = FallingTreeEntity.FALL_TICKS + 15, b = a + 60, c = b + 60;
        double[] start = new double[2];
        FelledLogEntity[] overLedge = new FelledLogEntity[1];
        FelledLogEntity[] held = new FelledLogEntity[2];
        BlockState oak = Blocks.OAK_LOG.defaultBlockState();

        // A: the tree lands as one solid log; hewing the middle splits it; one player lifts the root piece by its end and
        // walks off with it; a real right-click loads it on a wagon, sneak + right-click unloads it
        h.runAfterDelay(a, () -> {
            List<FelledLogEntity> landed = logs(h).stream().filter(l -> l.tickCount < 40).toList();
            List<Integer> landedSizes = landed.stream().map(FelledLogEntity::size).sorted().toList();
            h.assertTrue(landedSizes.equals(List.of(1, 5)), "trunk and branch land as two logs, 5 and 1: blocks hold only end to end (" + landedSizes + ")");
            FelledLogEntity log = landed.stream().filter(l -> l.size() == 5).findFirst().orElseThrow();
            for (FelledLogEntity l : landed) if (l != log) l.discard();
            h.assertTrue(log.straight(), "a trunk with no branches lies straight");
            BlockPos root = log.cellPos(0);
            h.assertTrue(root.equals(cut.south()) && log.cellPos(4).equals(cut.south(5)), "it lies south of the stump, root end first (" + root + " .. " + log.cellPos(4) + ")");
            Vec3 m = log.world(2);
            h.assertTrue(collides(level, new AABB(m.x - 0.2, m.y + 0.5, m.z - 0.2, m.x + 0.2, m.y + 0.9, m.z + 0.2)), "a log block is solid");
            h.assertTrue(!collides(level, new AABB(m.x - 0.2, m.y + 1.0, m.z - 0.2, m.x + 0.2, m.y + 1.1, m.z + 0.2)), "nothing above it: it can be stood on");
            h.assertTrue(!collides(level, new AABB(m.x + 0.55, m.y + 0.5, m.z - 0.2, m.x + 0.65, m.y + 0.9, m.z + 0.2)), "no invisible wall beside it");
            BlockPos middle = log.cellPos(2);
            TreeChopping.hewWhole(level, middle, "oak", new ItemStack(Mechanical.FLINT_AXE.get()), player);
            log.removeBlock(level, 2);
            List<FelledLogEntity> pieces = logs(h);
            h.assertTrue(pieces.size() == 2 && pieces.stream().allMatch(l -> l.size() == 2), "split in two logs of 2 (" + pieces.stream().map(FelledLogEntity::size).toList() + ")");
            h.assertItemEntityPresent(Mechanical.BARKS.get("oak").get(), h.relativePos(middle), 2);
            FelledLogEntity rootPiece = pieces.stream().filter(l -> l.cellPos(0).equals(root)).findFirst().orElseThrow(() -> h.assertionException("the root piece"));
            player.setPos(Vec3.atBottomCenterOf(root).add(1.2, 0, 0));
            rootPiece.tie(level, player, 0, new ItemStack(Items.LEAD));
            h.assertTrue(!rootPiece.isPulled(), "a small log takes no lead");
            rootPiece.lift(level, player, 0);
            h.assertTrue(FelledLogEntity.liftedBy(player) == rootPiece && rootPiece.rootLifted(), "the root end is lifted by hand");
            h.assertTrue(CarcassHauling.blocksSprint(player), "no sprinting with a log in your hands");
            start[0] = rootPiece.world(0).x;
        });
        for (int t = 1; t <= 40; t++) h.runAfterDelay(a + t, () -> player.setPos(player.position().add(0.15, 0, 0)));
        h.runAfterDelay(a + 50, () -> {
            FelledLogEntity log = FelledLogEntity.liftedBy(player);
            h.assertTrue(log != null, "still lifting");
            double moved = log.world(0).x - start[0];
            h.assertTrue(moved > 3, "the log was carried east (" + moved + ")");
            WagonEntity wagon = Carting.WAGON.get().create(level, EntitySpawnReason.EVENT);
            wagon.setPos(h.absoluteVec(new Vec3(8.5, 1, 12.5)));
            wagon.setYRot(0);
            level.addFreshEntity(wagon);
            player.interactOn(wagon, InteractionHand.MAIN_HAND, Vec3.ZERO); // the path a real right-click takes
            h.assertTrue(wagon.harness() == null && TravoisEntity.harnessedTo(player) == null, "the click loaded the log, it didn't harness the wagon");
            FelledLogEntity onBed = wagon.carriedLog();
            h.assertTrue(onBed != null && onBed.size() == 2 && onBed.lifterCount() == 0, "the lifted log is loaded on the wagon and put down");
            h.assertTrue(FelledLogEntity.liftedBy(player) == null, "hands free again");
            player.setShiftKeyDown(true);
            player.interactOn(wagon, InteractionHand.MAIN_HAND, Vec3.ZERO);
            player.setShiftKeyDown(false);
            h.assertTrue(!onBed.isPassenger() && wagon.carriedLog() == null, "sneak + right-click unloads it");
            h.assertTrue(onBed.position().z < wagon.getZ() - WagonEntity.REAR, "it lies behind the wagon (" + onBed.position() + ")");
            wagon.discard();
            for (FelledLogEntity l : logs(h)) l.discard();
        });

        // B: two players carry a 5-block log between them, one at each end
        h.runAfterDelay(b, () -> {
            // collision exactly on the blocks' cells, for a log along z and one along x (and clicks hit the same boxes)
            for (float yaw : new float[] {0, -90}) {
                BlockPos rootCell = h.absolutePos(new BlockPos(12, 1, 3));
                FelledLogEntity probe = FelledLogEntity.spawnStraight(level, Vec3.atBottomCenterOf(rootCell), yaw, 3, oak);
                BlockPos far = yaw == 0 ? rootCell.south(2) : rootCell.east(2);
                AABB want = new AABB(Vec3.atLowerCornerOf(rootCell), Vec3.atLowerCornerOf(far).add(1, 1, 1));
                AABB got = probe.collisionShape().bounds();
                h.assertTrue(got.minX == want.minX && got.maxX == want.maxX && got.minZ == want.minZ && got.maxZ == want.maxZ && got.minY == want.minY && got.maxY == want.maxY,
                        "collision on the cells (yaw " + yaw + "): " + got + " vs " + want);
                for (int i = 0; i < 3; i++) {
                    BlockPos cell = yaw == 0 ? rootCell.south(i) : rootCell.east(i);
                    h.assertTrue(probe.cellPos(i).equals(cell), "block " + i + " in its cell (yaw " + yaw + ")");
                }
                probe.discard();
            }
            // log blocks lie as Fallen Logs (hewing stages and the kerf show); a saw through the middle block cuts it out and parts the log
            BlockPos sawAt = h.absolutePos(new BlockPos(12, 1, 3));
            FelledLogEntity sawLog = FelledLogEntity.spawnStraight(level, Vec3.atBottomCenterOf(sawAt), 0, 3, oak);
            h.assertTrue(sawLog.states().get(0).getBlock() instanceof FallenLogBlock, "log blocks lie as Fallen Logs");
            for (int k = 0; k < FallenLogBlock.SAW_STROKES - 1; k++) sawLog.sawStroke(level, player, InteractionHand.MAIN_HAND, 1, new ItemStack(Items.STICK), k % 2 == 0);
            h.assertTrue(sawLog.isAlive() && sawLog.states().get(1).getValue(FallenLogBlock.SAWN) == FallenLogBlock.SAW_STROKES - 1, "the kerf deepens in the middle block");
            sawLog.sawStroke(level, player, InteractionHand.MAIN_HAND, 1, new ItemStack(Items.STICK), true);
            List<Integer> sawnSizes = logs(h).stream().filter(l -> l.position().distanceTo(Vec3.atBottomCenterOf(sawAt.south())) < 2.5).map(FelledLogEntity::size).sorted().toList();
            h.assertTrue(!sawLog.isAlive() && sawnSizes.equals(List.of(1, 1)), "sawn through, the log parts into 1 + 1 (" + sawnSizes + ")");
            h.assertItemEntityPresent(Mechanical.ROUGH_LOGS.get("oak").get(), h.relativePos(sawAt.south()), 2);
            for (FelledLogEntity l : logs(h)) if (l.position().distanceTo(Vec3.atBottomCenterOf(sawAt.south())) < 2.5) l.discard();
            // turned 45° (LogCollision): a mob walking at it stops at its side, exactly; one dropped on it stands on its top
            FelledLogEntity diag = FelledLogEntity.spawnStraight(level, h.absoluteVec(new Vec3(13.5, 1, 10.5)), 45, 3, oak);
            Vec3 dm = diag.world(1), perp = new Vec3(1, 0, 0).yRot(-45 * Mth.DEG_TO_RAD);
            var pig = net.minecraft.world.entity.EntityTypes.PIG.create(level, EntitySpawnReason.EVENT);
            pig.setNoAi(true);
            pig.setPos(dm.add(perp.scale(2.0)));
            level.addFreshEntity(pig);
            for (int k = 0; k < 12; k++) pig.move(net.minecraft.world.entity.MoverType.SELF, perp.scale(-0.25));
            double apart = pig.position().subtract(dm).dot(perp), want = 0.5 + pig.getBbWidth() / 2;
            h.assertTrue(Math.abs(apart - want) < 0.02, "a mob stops at a turned log's side (" + apart + " from its line, want " + want + ")");
            // the server's move check counts every listed shape as a collision: none may be listed for a log that doesn't reach
            h.assertTrue(pig.getBoundingBox().intersects(diag.getBoundingBox()) && level.getEntityCollisions(pig, pig.getBoundingBox()).isEmpty(),
                    "no collision shapes listed next to a turned log (inside its box)");
            pig.move(net.minecraft.world.entity.MoverType.SELF, new Vec3(-perp.z, 0, perp.x).scale(0.8)); // along it: slides freely
            double stillApart = pig.position().subtract(dm).dot(perp);
            h.assertTrue(Math.abs(stillApart - want) < 0.02, "and slides along it (" + stillApart + ")");
            pig.setPos(dm.add(0, 2.5, 0));
            for (int k = 0; k < 10; k++) pig.move(net.minecraft.world.entity.MoverType.SELF, new Vec3(0, -0.4, 0));
            h.assertTrue(Math.abs(pig.getY() - (dm.y + 1)) < 0.01, "a mob stands on a turned log's top (y " + pig.getY() + ", top " + (dm.y + 1) + ")");
            pig.discard();
            h.assertTrue(!diag.onGrid(), "a turned log stays turned");
            diag.discard();
            // a Cart takes one log of up to 2 blocks; a Wagon three of up to 4 (two below, one on top), not a fourth
            BlockPos bedAt = h.absolutePos(new BlockPos(13, 1, 3));
            CartEntity cart = Carting.CART.get().create(level, EntitySpawnReason.EVENT);
            cart.setPos(Vec3.atBottomCenterOf(bedAt));
            level.addFreshEntity(cart);
            FelledLogEntity three = FelledLogEntity.spawnStraight(level, Vec3.atBottomCenterOf(bedAt.east(2)), 0, 3, oak);
            player.setPos(three.endPoint(true).add(0, 0, -0.6));
            three.lift(level, player, 0);
            player.interactOn(cart, InteractionHand.MAIN_HAND, Vec3.ZERO);
            h.assertTrue(cart.carriedLog() == null && FelledLogEntity.liftedBy(player) == three, "a cart refuses a 3-block log");
            three.putDown(player);
            three.discard();
            FelledLogEntity two = FelledLogEntity.spawnStraight(level, Vec3.atBottomCenterOf(bedAt.east(2)), 0, 2, oak);
            player.setPos(two.endPoint(true).add(0, 0, -0.6));
            two.lift(level, player, 0);
            player.interactOn(cart, InteractionHand.MAIN_HAND, Vec3.ZERO);
            h.assertTrue(cart.carriedLog() == two && cart.harness() == null, "a cart takes a 2-block log");
            cart.ejectPassengers();
            two.discard();
            cart.discard();
            WagonEntity bed = Carting.WAGON.get().create(level, EntitySpawnReason.EVENT);
            bed.setPos(Vec3.atBottomCenterOf(bedAt));
            level.addFreshEntity(bed);
            for (int k = 0; k < 4; k++) {
                FelledLogEntity four = FelledLogEntity.spawnStraight(level, Vec3.atBottomCenterOf(bedAt.east(3)), 0, 4, oak);
                player.setPos(four.endPoint(true).add(0, 0, -0.6));
                four.lift(level, player, 0);
                // the first log is clicked straight on the wagon; the next ones by clicking a log already on it (as the
                // crosshair often lands on one): that loads the new log, it doesn't roll the old one off
                if (k == 0) bed.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
                else player.interactOn(bed.carriedLogs().getFirst(), InteractionHand.MAIN_HAND, Vec3.ZERO);
                if (k == 3) {
                    h.assertTrue(four.getVehicle() == null, "a wagon takes no fourth log");
                    four.putDown(player);
                    four.discard();
                }
            }
            h.assertTrue(bed.carriedLogs().size() == 3, "a wagon takes three 4-block logs (" + bed.carriedLogs().size() + ")");
            bed.positionRider(bed.carriedLogs().get(2));
            h.assertTrue(bed.carriedLogs().get(2).getY() > bed.carriedLogs().get(0).getY() + 0.5, "the third lies on top");
            FelledLogEntity topLog = bed.carriedLogs().get(2);
            player.setPos(bed.position().add(1.5, 0, 0));
            topLog.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
            h.assertTrue(topLog.isPassenger() && bed.carriedLogs().size() == 3, "a plain empty-hand click on a log on the bed leaves it there");
            player.setShiftKeyDown(true);
            topLog.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
            player.setShiftKeyDown(false);
            h.assertTrue(!topLog.isPassenger() && bed.carriedLogs().size() == 2, "sneak + right-click with an empty hand rolls that log off");
            topLog.discard();
            bed.ejectPassengers();
            for (FelledLogEntity l : logs(h)) if (l.position().distanceTo(bed.position()) < 6) l.discard();
            bed.discard();
            FelledLogEntity log = FelledLogEntity.spawnStraight(level, h.absoluteVec(new Vec3(2.5, 1, 6.5)), -90, 5, oak); // root at x 2.5, crown at 6.5: two carry 5 blocks at full strength
            held[0] = log;
            player.setPos(log.endPoint(true).add(-0.8, 0, 0));
            second.setPos(log.endPoint(false).add(0.8, 0, 0));
            log.lift(level, player, 0);
            log.lift(level, second, 4);
            h.assertTrue(log.rootLifted() && log.tailLifted() && log.liftLoad() <= 1, "both ends up (load " + log.liftLoad() + ")");
            start[0] = log.getZ();
        });
        for (int t = 1; t <= 40; t++) h.runAfterDelay(b + t, () -> {
            player.setPos(player.position().add(0, 0, 0.12));
            second.setPos(second.position().add(0, 0, 0.12));
        });
        h.runAfterDelay(b + 50, () -> {
            FelledLogEntity log = held[0];
            h.assertTrue(log.isAlive() && log.lifterCount() == 2, "still carried by both");
            h.assertTrue(log.getZ() - start[0] > 3, "carried along by both (" + (log.getZ() - start[0]) + ")");
            h.assertTrue(Math.abs(Mth.wrapDegrees(log.getYRot() + 90)) < 20, "still lying between them (yaw " + log.getYRot() + ")");
            log.putDown(player);
            log.putDown(second);
            h.assertTrue(log.lifterCount() == 0 && FelledLogEntity.liftedBy(second) == null, "both put it down");
            log.discard();
        });

        // C: a 14-block log is too big to lift; one Lead can't move it, a second one can
        h.runAfterDelay(c, () -> {
            FelledLogEntity log = FelledLogEntity.spawnStraight(level, h.absoluteVec(new Vec3(13.5, 1, 14.5)), 180, 14, oak); // crown towards -z
            held[0] = log;
            player.setPos(log.world(0).add(-2, 0, 0));
            log.lift(level, player, 0);
            h.assertTrue(log.lifterCount() == 0, "too big to lift by hand");
            log.tie(level, player, 0, new ItemStack(Items.LEAD));
            h.assertTrue(log.pulledBy(player), "a big log takes a lead");
            start[0] = log.world(0).x;
        });
        for (int t = 1; t <= 20; t++) h.runAfterDelay(c + t, () -> player.setPos(player.position().add(-0.1, 0, 0)));
        h.runAfterDelay(c + 25, () -> {
            FelledLogEntity log = held[0];
            h.assertTrue(log.isAlive() && Math.abs(log.world(0).x - start[0]) < 0.01, "one player can't move 14 blocks (" + (log.world(0).x - start[0]) + ")");
            second.setPos(player.position());
            log.tie(level, second, 0, new ItemStack(Items.LEAD));
        });
        for (int t = 1; t <= 40; t++) h.runAfterDelay(c + 25 + t, () -> {
            player.setPos(player.position().add(-0.1, 0, 0));
            second.setPos(second.position().add(-0.1, 0, 0));
        });
        h.runAfterDelay(c + 75, () -> {
            FelledLogEntity log = logs(h).stream().filter(l -> l.pulledBy(player)).findFirst().orElseThrow(() -> h.assertionException("the pulled log"));
            h.assertTrue(start[0] - log.world(0).x > 1.5, "two leads drag it (" + (start[0] - log.world(0).x) + ")");
            for (FelledLogEntity l : logs(h)) l.discard();
            // D: a slope (rising 1 block every 2 towards +z): a log laid along it lies tilted and stays put (heavy, rough bark; felled_log_slide has the steep one)
            for (int z = 2; z <= 14; z++) for (int y = 1; y <= (z - 2) / 2; y++) h.setBlock(new BlockPos(3, y, z), Blocks.STONE);
            FelledLogEntity onSlope = FelledLogEntity.spawnStraight(level, h.absoluteVec(new Vec3(3.5, 6, 9.5)), 0, 4, oak);
            held[0] = onSlope;
            start[0] = onSlope.getZ();
            // and a gap 1 block wide and 2 deep in a raised floor: a 3-block log across it bridges it, level and still
            for (int z = 2; z <= 8; z++) for (int y = 1; y <= 2; y++) if (z != 5) h.setBlock(new BlockPos(9, y, z), Blocks.STONE);
            held[1] = FelledLogEntity.spawnStraight(level, h.absoluteVec(new Vec3(9.5, 4, 4.5)), 0, 3, oak); // blocks at z 4, 5, 6
            // a ledge (2 high, ending at z 5) with a 4-block log reaching past it, its middle over the drop: it goes over and lies on the floor
            for (int z = 2; z <= 5; z++) for (int y = 1; y <= 2; y++) h.setBlock(new BlockPos(12, y, z), Blocks.STONE);
            overLedge[0] = FelledLogEntity.spawnStraight(level, h.absoluteVec(new Vec3(12.5, 3, 4.5)), 0, 4, oak); // blocks at z 4..7
        });
        h.runAfterDelay(c + 95, () -> {
            FelledLogEntity log = held[0];
            h.assertTrue(log.isAlive() && log.tilt() > 0.3F, "on the slope it lies tilted (" + log.tilt() + ")");
            FelledLogEntity bridge = held[1];
            start[1] = bridge.getY();
            h.assertTrue(bridge.tilt() == 0 && Math.abs(bridge.getY() - h.absoluteVec(new Vec3(0, 3, 0)).y) < 0.01, "a log bridges a narrow gap, level on its edges (y " + bridge.getY() + ", tilt " + bridge.tilt() + ")");
        });
        h.runAfterDelay(c + 200, () -> {
            FelledLogEntity log = held[0];
            h.assertTrue(Math.abs(start[0] - log.getZ()) < 0.6, "and doesn't slide down a 1 in 2 slope (" + (start[0] - log.getZ()) + " blocks)");
            FelledLogEntity fell = overLedge[0];
            h.assertTrue(fell.isAlive() && fell.getY() < h.absoluteVec(new Vec3(0, 2.6, 0)).y, "a log with its middle past a ledge doesn't hang there: it went over (y " + fell.getY() + ", tilt " + fell.tilt() + ")");
            h.assertTrue(Math.abs(held[1].getY() - start[1]) < 0.01 && Math.abs(held[1].getZ() - h.absoluteVec(new Vec3(0, 0, 5.5)).z) < 0.01, "and stays put over it (" + held[1].position() + ")");
            for (FelledLogEntity l : logs(h)) l.discard();
            h.succeed();
        });
    }
}
