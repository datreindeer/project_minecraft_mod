package com.mechanical;

import static com.mechanical.Mechanical.MODID;

import java.util.Comparator;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Game test for {@link FelledLogEntity}: a felled 5-log tree lies as one log entity of 5 blocks; cutting out the middle
 * block splits it into two logs of 2 (with a hewn log's drops); a lead on the root end drags the root piece after a
 * walking player; the dragged log goes on a Wagon and comes off again.
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
                new FunctionGameTestInstance(TEST, TestArea.of(env, 140, true)));
    }

    private static List<FelledLogEntity> logs(GameTestHelper h) {
        return h.getLevel().getEntitiesOfClass(FelledLogEntity.class, new AABB(h.absolutePos(BlockPos.ZERO)).expandTowards(16, 16, 16).inflate(1));
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
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        TreeFelling.fell(level, cut, tree, Direction.SOUTH, player, new ItemStack(Mechanical.FLINT_AXE.get()));

        // all callbacks are registered here: adding them from inside one makes the test runner run that one twice
        int landed0 = FallingTreeEntity.FALL_TICKS + 15;
        double[] startX = new double[1];
        for (int t = 1; t <= 40; t++) h.runAfterDelay(landed0 + t, () -> player.setPos(player.position().add(0.15, 0, 0)));
        h.runAfterDelay(landed0 + 50, () -> {
            FelledLogEntity dragged = logs(h).stream().filter(l -> l.pulledBy(player)).findFirst().orElseThrow(() -> h.assertionException("the dragged log"));
            double moved = dragged.world(0).x - startX[0];
            h.assertTrue(moved > 3, "the log was dragged east (" + moved + ")");
            h.assertTrue(dragged.world(0).distanceTo(player.position()) < FelledLogEntity.TETHER + 1, "it keeps up with the puller");
            // onto a wagon and off again
            WagonEntity wagon = Carting.WAGON.get().create(level, EntitySpawnReason.EVENT);
            wagon.setPos(h.absoluteVec(new Vec3(8.5, 1, 12.5)));
            wagon.setYRot(0);
            level.addFreshEntity(wagon);
            wagon.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
            FelledLogEntity onBed = wagon.carriedLog();
            h.assertTrue(onBed != null && onBed.size() == 2 && !onBed.isPulled(), "the dragged log is loaded on the wagon, the lead off");
            h.assertTrue(player.getInventory().countItem(Items.LEAD) == 1, "the lead is back");
            player.getInventory().clearContent(); // the lead came back to hand; unloading takes an empty one
            player.setShiftKeyDown(true);
            wagon.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
            h.assertTrue(!onBed.isPassenger() && wagon.carriedLog() == null, "sneak + right-click unloads it");
            h.assertTrue(onBed.position().z < wagon.getZ() - WagonEntity.REAR, "it lies behind the wagon (" + onBed.position() + ")");
            wagon.discard();
            for (FelledLogEntity l : logs(h)) l.discard();
            h.succeed();
        });
        h.runAfterDelay(landed0, () -> {
            List<FelledLogEntity> landed = logs(h).stream().filter(l -> l.tickCount < 40).toList();
            h.assertTrue(landed.size() == 1 && landed.get(0).size() == 5, "the tree lies as one log of 5 (" + landed.stream().map(FelledLogEntity::size).toList() + ")");
            FelledLogEntity log = landed.get(0);
            h.assertTrue(log.straight(), "a trunk with no branches lies straight");
            BlockPos root = log.cellPos(0);
            h.assertTrue(root.equals(cut.south()) && log.cellPos(4).equals(cut.south(5)), "it lies south of the stump, root end first (" + root + " .. " + log.cellPos(4) + ")");
            // the middle block is hewn out: two logs of 2, and the hewn block's bark
            BlockPos middle = log.cellPos(2);
            TreeChopping.hewWhole(level, middle, "oak", new ItemStack(Mechanical.FLINT_AXE.get()), player);
            log.removeBlock(level, 2);
            List<FelledLogEntity> pieces = logs(h);
            h.assertTrue(pieces.size() == 2 && pieces.stream().allMatch(l -> l.size() == 2), "split in two logs of 2 (" + pieces.stream().map(FelledLogEntity::size).toList() + ")");
            h.assertItemEntityPresent(Mechanical.BARKS.get("oak").get(), h.relativePos(middle), 2);
            FelledLogEntity rootPiece = pieces.stream().filter(l -> l.cellPos(0).equals(root)).findFirst().orElseThrow(() -> h.assertionException("the root piece"));
            // a lead on the root end; the player walks off east and the log follows
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.LEAD));
            player.setPos(Vec3.atBottomCenterOf(root).add(2, 0, 0));
            rootPiece.tie(level, player, 0, player.getMainHandItem());
            h.assertTrue(rootPiece.pulledBy(player) && player.getMainHandItem().isEmpty(), "the lead is tied on and used up");
            startX[0] = rootPiece.world(0).x;
        });
    }
}
