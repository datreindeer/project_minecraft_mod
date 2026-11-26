package com.mechanical.people;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import com.mechanical.FirePit;
import com.mechanical.FirePitBlock;
import com.mechanical.TestArea;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Game tests for life round the fire (docs/living-world-plan.md C, CampLife).
 * <ul>
 * <li>tribe_fire_path: the path across a field with a Fire Pit and an unlit campfire in the straight line goes round both,
 * and walking it the person never steps onto either.</li>
 * <li>tribe_visit_sit: a band staying at a player's fire sits down round it (seated, within 3.6 blocks), not milling about.</li>
 * <li>tribe_night_camp: at night camp folk lay out Straw Beds by their fire and sleep in them while the hunter keeps watch
 * awake further out; in the morning they are up and the beds are gone.</li>
 * <li>tribe_night_home: at night one shut in a plank pen far from the fire, who cannot walk home, is moved to the fire.</li>
 * <li>tribe_dig_out: one at the bottom of a dirt pit 3 deep digs a way up and out.</li>
 * <li>tribe_new_leader: when the leader dies the elder leads and wears the banner; when a camp's elder dies, flagged as
 * leader or not, another becomes the Elder (role, staff); a camp found without one names one too.</li>
 * </ul>
 */
@EventBusSubscriber(modid = MODID)
public final class TribeAiTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final String[] NAMES = {"tribe_fire_path", "tribe_visit_sit", "tribe_night_camp", "tribe_night_home", "tribe_dig_out", "tribe_new_leader"};

    static {
        TEST_FUNCTIONS.register(NAMES[0], () -> TribeAiTest::firePath);
        TEST_FUNCTIONS.register(NAMES[1], () -> TribeAiTest::visitSit);
        TEST_FUNCTIONS.register(NAMES[2], () -> TribeAiTest::nightCamp);
        TEST_FUNCTIONS.register(NAMES[3], () -> TribeAiTest::nightHome);
        TEST_FUNCTIONS.register(NAMES[4], () -> TribeAiTest::digOut);
        TEST_FUNCTIONS.register(NAMES[5], () -> TribeAiTest::newLeader);
    }

    static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "tribe_ai"), new TestEnvironmentDefinition.AllOf(List.of()));
        int[] ticks = {400, 600, 1200, 900, 600, 100};
        for (int i = 0; i < NAMES.length; i++) {
            ResourceKey<Consumer<GameTestHelper>> key = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, NAMES[i]));
            event.registerTest(Identifier.fromNamespaceAndPath(MODID, NAMES[i]), new FunctionGameTestInstance(key, TestArea.of(env, ticks[i], true)));
        }
    }

    private static void floor(GameTestHelper h) {
        for (int x = 0; x <= 16; x++)
            for (int z = 0; z <= 16; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.GRASS_BLOCK.defaultBlockState());
    }

    private static Tribesperson person(GameTestHelper h, TribeRole role, BlockPos at, BlockPos home, UUID group, boolean leader) {
        Tribesperson t = h.spawn(People.TRIBESPERSON.get(), at);
        t.setup(role, home, group, leader);
        return t;
    }

    private static boolean fire(BlockState s) {
        return s.getBlock() instanceof FirePitBlock || s.getBlock() instanceof CampfireBlock;
    }

    private static void firePath(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        floor(h);
        for (int part = 0; part < 4; part++)
            h.setBlock(new BlockPos(7 + (part & 1), 1, 8 + (part >> 1)), FirePit.FIRE_PIT.get().defaultBlockState().setValue(FirePitBlock.PART, part));
        h.setBlock(new BlockPos(11, 1, 8), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        h.setBlock(new BlockPos(4, 1, 8), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        Tribesperson t = person(h, TribeRole.GATHERER, new BlockPos(1, 1, 8), null, UUID.randomUUID(), true);
        BlockPos goal = h.absolutePos(new BlockPos(15, 1, 8));
        h.onEachTick(() -> {
            if (t.isAlive()) h.assertTrue(!fire(level.getBlockState(t.blockPosition())) && !fire(level.getBlockState(t.getBlockPosBelowThatAffectsMyMovement())),
                    "he never steps onto a fire (at " + t.blockPosition() + ")");
        });
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(t.onGround(), "on the ground")) // no path is planned before
                .thenExecute(() -> {
                    Path path = t.getNavigation().createPath(goal, 0);
                    h.assertTrue(path != null && path.canReach(), "a path round the fires (" + path + ", from " + t.blockPosition() + ")");
                    for (int i = 0; i < path.getNodeCount(); i++) {
                        BlockPos n = path.getNodePos(i);
                        h.assertTrue(!fire(level.getBlockState(n)) && !fire(level.getBlockState(n.below())), "the path never crosses a fire (node " + n + ")");
                    }
                    t.getNavigation().moveTo(path, 0.8);
                })
                .thenWaitUntil(() -> {
                    if (t.getNavigation().isDone() && t.distanceToSqr(Vec3.atBottomCenterOf(goal)) > 2 * 2) t.getNavigation().moveTo(goal.getX() + 0.5, goal.getY(), goal.getZ() + 0.5, 0.8);
                    h.assertTrue(t.distanceToSqr(Vec3.atBottomCenterOf(goal)) < 2 * 2, "he gets across (at " + t.position() + ")");
                })
                .thenExecute(t::discard)
                .thenSucceed();
    }

    private static void visitSit(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        floor(h);
        BlockPos fire = h.absolutePos(new BlockPos(8, 1, 8));
        h.setBlock(new BlockPos(8, 1, 8), Blocks.CAMPFIRE.defaultBlockState());
        UUID band = UUID.randomUUID();
        long day = level.getOverworldClockTime() / com.mechanical.Calendar.CLOCK_DAY + 1; // no dawn during the test
        Neighbours data = Neighbours.get(level);
        Neighbours.Visit v = new Neighbours.Visit(UUID.randomUUID(), band, fire, fire, fire, Neighbours.Visit.STAYING, level.getGameTime(), day, true, Optional.empty(), 0);
        data.visits.add(v);
        List<Tribesperson> folk = List.of(person(h, TribeRole.GATHERER, new BlockPos(2, 1, 3), null, band, true),
                person(h, TribeRole.WOODCUTTER, new BlockPos(14, 1, 4), null, band, false),
                person(h, TribeRole.ELDER, new BlockPos(3, 1, 14), null, band, false));
        for (Tribesperson t : folk) {
            t.visit = v.id;
            t.life.forcedPhase = CampLife.DAY;
        }
        h.succeedWhen(() -> {
            for (Tribesperson t : folk) {
                double dx = t.getX() - fire.getX() - 0.5, dz = t.getZ() - fire.getZ() - 0.5;
                h.assertTrue((t.idle() & CampLife.SITTING) != 0 && dx * dx + dz * dz < 3.6 * 3.6, t.tribeName() + " sits by the fire (idle " + t.idle() + " at " + t.position() + ", nav " + t.getNavigation().isInProgress() + ", goals " + running(t) + ")");
            }
            data.visits.remove(v);
            folk.forEach(Tribesperson::discard);
        });
    }

    private static void nightCamp(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        floor(h);
        BlockPos fire = h.absolutePos(new BlockPos(8, 1, 8));
        h.setBlock(new BlockPos(8, 1, 8), Blocks.CAMPFIRE.defaultBlockState());
        UUID camp = UUID.randomUUID();
        Tribesperson a = person(h, TribeRole.GATHERER, new BlockPos(6, 1, 6), fire, camp, true);
        Tribesperson b = person(h, TribeRole.WOODCUTTER, new BlockPos(10, 1, 10), fire, camp, false);
        Tribesperson hunter = person(h, TribeRole.HUNTER, new BlockPos(10, 1, 6), fire, camp, false);
        List<Tribesperson> folk = List.of(a, b, hunter);
        folk.forEach(t -> t.life.forcedPhase = CampLife.NIGHT);
        h.startSequence()
                .thenWaitUntil(() -> {
                    for (Tribesperson t : List.of(a, b)) {
                        h.assertTrue(t.isSleeping(), t.tribeName() + " lies down (at " + t.position() + ", bed " + t.life.bed + ", goals " + running(t) + ")");
                        h.assertTrue(level.getBlockState(t.getSleepingPos().get()).is(Blocks.STRAW_BED), "on a Straw Bed laid out by the fire");
                    }
                    double d = Math.sqrt(hunter.distanceToSqr(Vec3.atBottomCenterOf(fire)));
                    h.assertTrue(Math.abs(d - CampLife.GUARD_R) < 1.6, "the hunter on watch six blocks out (at " + d + ")");
                })
                .thenExecute(() -> {
                    h.assertTrue(!hunter.isSleeping(), "the hunter stays awake");
                    folk.forEach(t -> t.life.forcedPhase = CampLife.MORNING);
                })
                .thenWaitUntil(() -> {
                    for (Tribesperson t : List.of(a, b)) h.assertTrue(!t.isSleeping(), t.tribeName() + " is up in the morning");
                    for (BlockPos p : BlockPos.betweenClosed(fire.offset(-6, 0, -6), fire.offset(6, 0, 6)))
                        h.assertTrue(!level.getBlockState(p).is(Blocks.STRAW_BED), "the beds are taken up (one at " + p + ")");
                })
                .thenExecute(() -> folk.forEach(Tribesperson::discard))
                .thenSucceed();
    }

    private static void nightHome(GameTestHelper h) {
        floor(h);
        BlockPos fire = h.absolutePos(new BlockPos(0, 1, 0));
        h.setBlock(new BlockPos(0, 1, 0), Blocks.CAMPFIRE.defaultBlockState());
        // a pen of planks (a build: not to be dug through) in the far corner
        for (int x = 13; x <= 16; x++)
            for (int z = 13; z <= 16; z++)
                for (int y = 1; y <= 3; y++)
                    if (x == 13 || x == 16 || z == 13 || z == 16) h.setBlock(new BlockPos(x, y, z), Blocks.OAK_PLANKS.defaultBlockState());
        Tribesperson t = person(h, TribeRole.GATHERER, new BlockPos(15, 1, 15), fire, UUID.randomUUID(), true);
        t.life.forcedPhase = CampLife.NIGHT;
        h.succeedWhen(() -> {
            h.assertTrue(t.distanceToSqr(Vec3.atBottomCenterOf(fire)) < 13 * 13, "brought home (at " + t.position() + ", goals " + running(t) + ")");
            h.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(13, 1, 15));
            t.discard();
        });
    }

    private static void digOut(GameTestHelper h) {
        floor(h);
        for (int x = 4; x <= 12; x++)
            for (int z = 4; z <= 12; z++)
                for (int y = 1; y <= 3; y++)
                    if (x != 8 || z != 8) h.setBlock(new BlockPos(x, y, z), Blocks.DIRT.defaultBlockState());
        Tribesperson t = person(h, TribeRole.GATHERER, new BlockPos(8, 1, 8), null, UUID.randomUUID(), true);
        double start = t.getY();
        h.assertTrue(DigOut.enclosed(h.getLevel(), h.absolutePos(new BlockPos(8, 1, 8))), "shut in the pit");
        h.succeedWhen(() -> {
            h.assertTrue(t.getY() >= start + 1.9 && !DigOut.enclosed(h.getLevel(), t.blockPosition()), "a way up out of the pit (at " + t.position() + ", trapped " + t.trapped + ")");
            t.discard();
        });
    }

    private static void newLeader(GameTestHelper h) {
        floor(h);
        UUID group = UUID.randomUUID();
        Tribesperson old = person(h, TribeRole.HUNTER, new BlockPos(4, 1, 4), null, group, true);
        Tribesperson near = person(h, TribeRole.GATHERER, new BlockPos(5, 1, 4), null, group, false);
        Tribesperson elder = person(h, TribeRole.ELDER, new BlockPos(12, 1, 12), null, group, false);
        old.kill(h.getLevel());
        h.assertTrue(elder.isLeader() && !near.isLeader(), "the elder leads now");
        h.assertTrue(!elder.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).isEmpty(), "with the banner");
        near.discard();
        elder.discard();
        // a worldgen camp's elder carries no leader flag
        UUID camp = UUID.randomUUID();
        BlockPos fire = h.absolutePos(new BlockPos(8, 1, 8));
        Tribesperson chief = person(h, TribeRole.ELDER, new BlockPos(6, 1, 6), fire, camp, false);
        Tribesperson heir = person(h, TribeRole.GATHERER, new BlockPos(7, 1, 6), fire, camp, false);
        Tribesperson other = person(h, TribeRole.WOODCUTTER, new BlockPos(11, 1, 11), fire, camp, false);
        chief.kill(h.getLevel());
        isElder(h, heir, "after the elder's death");
        h.assertTrue(other.role() == TribeRole.WOODCUTTER, "only one is named");
        heir.discard(); // gone without dying
        Tribesperson third = person(h, TribeRole.FISHER, new BlockPos(9, 1, 9), fire, camp, false);
        for (int i = 1; i < Tribesperson.ELDERLESS_CHECKS; i++) other.checkElder(h.getLevel());
        h.assertTrue(other.role() == TribeRole.WOODCUTTER, "not at once");
        other.checkElder(h.getLevel());
        isElder(h, other, "in a camp left without one");
        third.checkElder(h.getLevel());
        h.assertTrue(third.role() == TribeRole.FISHER, "no second elder");
        other.discard();
        third.discard();
        h.succeed();
    }

    private static void isElder(GameTestHelper h, Tribesperson t, String when) {
        h.assertTrue(t.role() == TribeRole.ELDER && t.isLeader() && t.getMainHandItem().is(com.mechanical.Mechanical.ELDERS_STAFF.get())
                && t.getCustomName().getString().contains("Elder"), "a new Elder " + when + ", got " + t.role() + " " + t.getCustomName().getString());
    }

    private static String running(Tribesperson t) {
        return t.goalSelector.getAvailableGoals().stream().filter(g -> g.isRunning()).map(g -> g.getGoal().getClass().getSimpleName()).toList().toString();
    }

    private TribeAiTest() {}
}
