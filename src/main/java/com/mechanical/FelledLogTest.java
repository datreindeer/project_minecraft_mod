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
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "felled_log"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "felled_log"),
                new FunctionGameTestInstance(TEST, TestArea.of(env, 260, true)));
    }

    private static List<FelledLogEntity> logs(GameTestHelper h) {
        return h.getLevel().getEntitiesOfClass(FelledLogEntity.class, new AABB(h.absolutePos(BlockPos.ZERO)).expandTowards(16, 16, 16).inflate(1));
    }

    /** Does anything an entity collides with (the log's per-block shape) overlap {@code box}? */
    private static boolean collides(ServerLevel level, AABB box) {
        var b = Shapes.create(box);
        return level.getEntityCollisions(null, box).stream().anyMatch(v -> Shapes.joinIsNotEmpty(v, b, BooleanOp.AND));
    }

    private static void test(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        for (FelledLogEntity l : logs(h)) l.discard(); // left over from an earlier run (saved entities load after the area is cleared)
        for (int x = 0; x <= 16; x++) for (int z = 0; z <= 16; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        // a small natural oak: 5 logs and a few leaves round the top
        for (int y = 1; y <= 5; y++) h.setBlock(new BlockPos(3, y, 3), Blocks.OAK_LOG);
        var leaf = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE, 1).setValue(LeavesBlock.PERSISTENT, false);
        for (BlockPos p : List.of(new BlockPos(2, 5, 3), new BlockPos(4, 5, 3), new BlockPos(3, 5, 2), new BlockPos(3, 5, 4), new BlockPos(3, 6, 3)))
            h.setBlock(p, leaf);
        BlockPos cut = h.absolutePos(new BlockPos(3, 1, 3));
        TreeFelling.Tree tree = TreeFelling.find(level, cut);
        h.assertTrue(tree != null && tree.logs().size() == 5, "a natural 5-log tree (" + tree + ")");
        Player player = h.makeMockPlayer(GameType.SURVIVAL), second = h.makeMockPlayer(GameType.SURVIVAL);
        TreeFelling.fell(level, cut, tree, Direction.SOUTH, player, new ItemStack(Mechanical.FLINT_AXE.get()));

        // all callbacks are registered here: adding them from inside one makes the test runner run that one twice
        int a = FallingTreeEntity.FALL_TICKS + 15, b = a + 60, c = b + 60;
        double[] start = new double[1];
        FelledLogEntity[] held = new FelledLogEntity[2];
        BlockState oak = Blocks.OAK_LOG.defaultBlockState();

        // A: the tree lands as one solid log; hewing the middle splits it; one player lifts the root piece by its end and
        // walks off with it; a real right-click loads it on a wagon, sneak + right-click unloads it
        h.runAfterDelay(a, () -> {
            List<FelledLogEntity> landed = logs(h).stream().filter(l -> l.tickCount < 40).toList();
            h.assertTrue(landed.size() == 1 && landed.get(0).size() == 5, "the tree lies as one log of 5 (" + landed.stream().map(FelledLogEntity::size).toList() + ")");
            FelledLogEntity log = landed.get(0);
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

        // B: two players carry an 8-block log between them, one at each end
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
            // log blocks lie as Fallen Logs (hewing stages and the saw kerf show); a saw across a seam parts it there, no block lost
            FelledLogEntity sawLog = FelledLogEntity.spawnStraight(level, Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(12, 1, 3))), 0, 3, oak);
            h.assertTrue(sawLog.states().get(0).getBlock() instanceof FallenLogBlock, "log blocks lie as Fallen Logs");
            for (int k = 0; k < FallenLogBlock.SAW_STROKES; k++) sawLog.sawStroke(level, player, InteractionHand.MAIN_HAND, -2, new ItemStack(Items.STICK), k % 2 == 0);
            List<Integer> sawnSizes = logs(h).stream().filter(l -> l != sawLog && l.position().distanceTo(sawLog.position()) < 3).map(FelledLogEntity::size).sorted().toList();
            h.assertTrue(!sawLog.isAlive() && sawnSizes.equals(List.of(1, 2)), "sawn at the seam behind the root block into 1 + 2 (" + sawnSizes + ")");
            for (FelledLogEntity l : logs(h)) if (l.position().distanceTo(sawLog.position()) < 3) l.discard();
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
                bed.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
                if (k == 3) {
                    h.assertTrue(four.getVehicle() == null, "a wagon takes no fourth log");
                    four.putDown(player);
                    four.discard();
                }
            }
            h.assertTrue(bed.carriedLogs().size() == 3, "a wagon takes three 4-block logs (" + bed.carriedLogs().size() + ")");
            bed.positionRider(bed.carriedLogs().get(2));
            h.assertTrue(bed.carriedLogs().get(2).getY() > bed.carriedLogs().get(0).getY() + 0.5, "the third lies on top");
            bed.ejectPassengers();
            for (FelledLogEntity l : logs(h)) if (l.position().distanceTo(bed.position()) < 6) l.discard();
            bed.discard();
            FelledLogEntity log = FelledLogEntity.spawnStraight(level, h.absoluteVec(new Vec3(2.5, 1, 6.5)), -90, 8, oak); // root at x 2.5, crown at 9.5
            held[0] = log;
            player.setPos(log.endPoint(true).add(-0.8, 0, 0));
            second.setPos(log.endPoint(false).add(0.8, 0, 0));
            log.lift(level, player, 0);
            log.lift(level, second, 7);
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
            h.succeed();
        });
    }
}
