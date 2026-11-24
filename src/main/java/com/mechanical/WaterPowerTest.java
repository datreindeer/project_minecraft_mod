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
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Game tests for water power and the Sawmill (docs/flora-and-food-plan.md phases 5a/5b): waterpower_wheel (still water
 * gives nothing; water falling past one side turns the wheel, the turn goes down a shaft, through a gearbox and round
 * the corner the other way; a block in the wheel's way stops it), waterpower_two_wheels (two wheels on one axle add
 * their power, and it climbs an upright shaft through two gearboxes to a loft), waterpower_body (the wheel's solid body
 * stands in its cells, keeps blocks out and goes with the hub), sawmill_barked (a barked log floats to the saw and
 * comes out as 2 Rough Logs, 2 Firewood, 2 Bark, 2 Sawdust, the blade worn by one), sawmill_stripped (6 Planks, 2 Sticks,
 * 1 Sawdust), sawmill_no_power (4 power: the log waits at the blade), sawmill_wrong_flow (water let in at the out end:
 * the log doesn't move).
 */
@EventBusSubscriber(modid = MODID)
public final class WaterPowerTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final String[] TESTS = {"waterpower_wheel", "waterpower_two_wheels", "waterpower_body", "waterpower_shared", "waterpower_shared_bellows", "sawmill_barked", "sawmill_stripped", "sawmill_no_power", "sawmill_wrong_flow"};

    static {
        TEST_FUNCTIONS.register("waterpower_wheel", () -> WaterPowerTest::wheel);
        TEST_FUNCTIONS.register("waterpower_two_wheels", () -> WaterPowerTest::twoWheels);
        TEST_FUNCTIONS.register("waterpower_body", () -> WaterPowerTest::body);
        TEST_FUNCTIONS.register("waterpower_shared", () -> h -> shared(h, Sawmill.SAW_BENCH.get().defaultBlockState().setValue(SawBenchBlock.FACING, Direction.NORTH)));
        TEST_FUNCTIONS.register("waterpower_shared_bellows", () -> h -> shared(h, HighFurnace.GREAT_BELLOWS.get().defaultBlockState().setValue(GreatBellowsBlock.FACING, Direction.NORTH)));
        TEST_FUNCTIONS.register("sawmill_barked", () -> h -> mill(h, new ItemStack(Items.OAK_LOG), 12, true, List.of(
                new ItemStack(Mechanical.ROUGH_LOGS.get("oak").get(), 2), new ItemStack(Mechanical.FIREWOOD.get("oak").get(), 2),
                new ItemStack(Mechanical.BARKS.get("oak").get(), 2), new ItemStack(Sawmill.SAWDUST.get(), 2))));
        TEST_FUNCTIONS.register("sawmill_stripped", () -> h -> mill(h, new ItemStack(Mechanical.ROUGH_LOGS.get("birch").get()), 12, true, List.of(
                new ItemStack(Items.BIRCH_PLANKS, 6), new ItemStack(Items.STICK, 2), new ItemStack(Sawmill.SAWDUST.get(), 1))));
        TEST_FUNCTIONS.register("sawmill_no_power", () -> h -> mill(h, new ItemStack(Items.OAK_LOG), 4, true, List.of()));
        TEST_FUNCTIONS.register("sawmill_wrong_flow", () -> h -> mill(h, new ItemStack(Items.OAK_LOG), 12, false, List.of()));
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "waterpower"), new TestEnvironmentDefinition.AllOf(List.of()));
        for (String t : TESTS) {
            event.registerTest(Identifier.fromNamespaceAndPath(MODID, t), new FunctionGameTestInstance(
                    ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, t)), TestArea.of(env, 260, true)));
        }
    }

    private static void floor(GameTestHelper h) {
        for (int x = 0; x <= 14; x++) for (int z = 0; z <= 14; z++) {
            h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            for (int y = 1; y <= 10; y++) h.setBlock(new BlockPos(x, y, z), Blocks.AIR);
        }
    }

    private static void wheel(GameTestHelper h) {
        floor(h);
        BlockPos hub = new BlockPos(6, 5, 6); // the wheel's plane: x 4..8, y 3..7 at z 6
        h.setBlock(hub, WaterPower.WATER_WHEEL.get().defaultBlockState().setValue(WaterWheelBlock.AXIS, Direction.Axis.Z));
        WaterWheelBlockEntity wheel = (WaterWheelBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(hub));
        // still water round the bottom paddles turns nothing
        for (int x = 5; x <= 7; x++) h.setBlock(new BlockPos(x, 3, 6), Blocks.WATER);
        h.assertTrue(wheel.measure(h.getLevel()) == 0, "still water must not turn the wheel");
        for (int x = 5; x <= 7; x++) h.setBlock(new BlockPos(x, 3, 6), Blocks.AIR);
        // a line: shaft, gearbox, shaft round the corner
        h.setBlock(new BlockPos(6, 5, 7), WaterPower.WOODEN_SHAFT.get().defaultBlockState().setValue(ShaftBlock.AXIS, Direction.Axis.Z));
        h.setBlock(new BlockPos(6, 5, 8), WaterPower.GEARBOX.get());
        h.setBlock(new BlockPos(7, 5, 8), WaterPower.WOODEN_SHAFT.get().defaultBlockState().setValue(ShaftBlock.AXIS, Direction.Axis.X));
        // overshot: a spring above the right side falls down past the paddles
        h.setBlock(new BlockPos(8, 8, 6), Blocks.WATER);
        h.runAfterDelay(80, () -> {
            float sum = wheel.measure(h.getLevel());
            h.assertTrue(Math.abs(sum) >= 6, "water falling past one side gives at least 6 power, got " + sum);
            h.assertTrue(wheel.power() >= 6 && wheel.speed() != 0, "the wheel turns, power " + wheel.power());
            RotorBlockEntity shaft = (RotorBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(6, 5, 7)));
            RotorBlockEntity corner = (RotorBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(7, 5, 8)));
            h.assertTrue(shaft.power() == wheel.power() && shaft.speed() == wheel.speed(), "the shaft turns with the wheel");
            h.assertTrue(corner.power() == wheel.power() && corner.speed() == -wheel.speed(), "round the gearbox the shaft turns the other way");
            // something in the way: it stops
            h.setBlock(new BlockPos(4, 5, 6), Blocks.COBBLESTONE);
            h.assertTrue(wheel.measure(h.getLevel()) == 0 && wheel.blocked != null, "a stone in the wheel stops it");
            h.succeed();
        });
    }

    private static BlockState shaft(Direction.Axis axis) {
        return WaterPower.WOODEN_SHAFT.get().defaultBlockState().setValue(ShaftBlock.AXIS, axis);
    }

    private static RotorBlockEntity rotor(GameTestHelper h, int x, int y, int z) {
        return (RotorBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(x, y, z)));
    }

    /**
     * Two wheels (planes z 4 and z 8, hubs at y 5) on one Z axle with shafts between, a little water falling past the same
     * side of each; the axle runs on through a gearbox at z 10, up two upright shafts to a gearbox at y 8 and out along X.
     */
    private static void twoWheels(GameTestHelper h) {
        floor(h);
        for (int z : new int[] {4, 8}) {
            h.setBlock(new BlockPos(6, 5, z), WaterPower.WATER_WHEEL.get().defaultBlockState().setValue(WaterWheelBlock.AXIS, Direction.Axis.Z));
            h.setBlock(new BlockPos(8, 6, z), Blocks.WATER); // a short fall down the lower side: well under full power each
        }
        for (int z : new int[] {5, 6, 7, 9}) h.setBlock(new BlockPos(6, 5, z), shaft(Direction.Axis.Z));
        h.setBlock(new BlockPos(6, 5, 10), WaterPower.GEARBOX.get());
        h.setBlock(new BlockPos(6, 6, 10), shaft(Direction.Axis.Y));
        h.setBlock(new BlockPos(6, 7, 10), shaft(Direction.Axis.Y));
        h.setBlock(new BlockPos(6, 8, 10), WaterPower.GEARBOX.get());
        h.setBlock(new BlockPos(7, 8, 10), shaft(Direction.Axis.X));
        h.runAfterDelay(80, () -> {
            WaterWheelBlockEntity a = (WaterWheelBlockEntity) rotor(h, 6, 5, 4), b = (WaterWheelBlockEntity) rotor(h, 6, 5, 8);
            float sa = a.measure(h.getLevel()), sb = b.measure(h.getLevel());
            int own = Math.max(Math.round(Math.abs(sa)), Math.round(Math.abs(sb))), both = Math.min(WaterPower.MAX_POWER, Math.round(Math.abs(sa + sb)));
            h.assertTrue(Math.abs(sa) >= 3 && Math.abs(sb) >= 3 && Math.signum(sa) == Math.signum(sb), "both wheels turn the same way, " + sa + " and " + sb);
            h.assertTrue(own < WaterPower.MAX_POWER && both > own, "two wheels give more than one: " + sa + " + " + sb + " gives " + both);
            for (RotorBlockEntity r : new RotorBlockEntity[] {a, b, rotor(h, 6, 5, 6), rotor(h, 6, 5, 9)})
                h.assertTrue(r.power() == both && r.speed() == a.speed() && r.speed() != 0, "the axle turns as one at " + both + ": " + r.getBlockPos() + " has " + r.power());
            RotorBlockEntity up = rotor(h, 6, 7, 10), loft = rotor(h, 6, 8, 10), out = rotor(h, 7, 8, 10);
            h.assertTrue(up.power() == both && up.speed() == a.speed(), "the upright shaft turns, " + up.power() + " speed " + up.speed());
            h.assertTrue(loft.power() == both && out.power() == both && out.speed() == -a.speed(), "power reaches the loft, " + out.power() + " speed " + out.speed());
            // one wheel jammed stops the whole axle
            h.setBlock(new BlockPos(4, 5, 8), Blocks.COBBLESTONE);
            h.runAfterDelay(25, () -> {
                h.assertTrue(a.power() == 0 && rotor(h, 7, 8, 10).power() == 0, "a jammed wheel stops the axle, " + a.power());
                h.succeed();
            });
        });
    }

    /** The wheel's body: two boxes in its cells, solid, keeping blocks out of the wheel, gone with the hub. */
    /**
     * Machines on one axle share its power (user decision 2026-11-24): a wheel's shaft into a gearbox drives two machines
     * (Saw Benches, or Great Bellows), one each way; each gets half (rounded down). With one bench gone the other gets it all, and the shafts
     * always carry the whole line.
     */
    private static void shared(GameTestHelper h, BlockState saw) {
        floor(h);
        h.setBlock(new BlockPos(6, 5, 6), WaterPower.WATER_WHEEL.get().defaultBlockState().setValue(WaterWheelBlock.AXIS, Direction.Axis.Z));
        h.setBlock(new BlockPos(8, 8, 6), Blocks.WATER);
        h.setBlock(new BlockPos(6, 5, 7), shaft(Direction.Axis.Z));
        h.setBlock(new BlockPos(6, 5, 8), WaterPower.GEARBOX.get());
        for (int x : new int[] {5, 7}) h.setBlock(new BlockPos(x, 5, 8), shaft(Direction.Axis.X));
        for (int x : new int[] {4, 8}) h.setBlock(new BlockPos(x, 5, 8), saw);
        h.runAfterDelay(70, () -> {
            RotorBlockEntity wheel = rotor(h, 6, 5, 6), a = rotor(h, 4, 5, 8), b = rotor(h, 8, 5, 8), s = rotor(h, 7, 5, 8);
            h.assertTrue(wheel.power() >= 6, "the wheel turns, power " + wheel.power());
            h.assertTrue(a.power() == wheel.power() / 2 && b.power() == wheel.power() / 2,
                    "two saws share " + wheel.power() + ": " + a.power() + " and " + b.power());
            h.assertTrue(s.power() == wheel.power(), "the shaft carries the whole line, " + s.power());
            h.setBlock(new BlockPos(4, 5, 8), Blocks.AIR);
            h.runAfterDelay(30, () -> {
                h.assertTrue(b.power() == wheel.power(), "a saw alone on the line gets it all, " + b.power() + " of " + wheel.power());
                h.succeed();
            });
        });
    }

    private static void body(GameTestHelper h) {
        floor(h);
        BlockPos hub = new BlockPos(6, 5, 6);
        h.setBlock(hub, WaterPower.WATER_WHEEL.get().defaultBlockState().setValue(WaterWheelBlock.AXIS, Direction.Axis.X));
        net.minecraft.world.phys.AABB around = new net.minecraft.world.phys.AABB(h.absolutePos(hub)).inflate(3);
        h.runAfterDelay(25, () -> {
            h.assertTrue(h.getLevel().getEntities(WaterPower.WHEEL_HITBOX.get(), around, e -> true).size() == 2, "the wheel has its body");
            for (BlockPos cell : new BlockPos[] {new BlockPos(6, 5, 8), new BlockPos(6, 7, 6), new BlockPos(6, 6, 7)}) {
                BlockPos at = h.absolutePos(cell);
                h.assertFalse(h.getLevel().getEntityCollisions(null, new net.minecraft.world.phys.AABB(at)).isEmpty(), "the wheel is solid at " + cell);
                h.assertFalse(h.getLevel().isUnobstructed(Blocks.STONE.defaultBlockState(), at, net.minecraft.world.phys.shapes.CollisionContext.empty()),
                        "no block can be put in the wheel at " + cell);
            }
            h.assertTrue(h.getLevel().getEntityCollisions(null, new net.minecraft.world.phys.AABB(h.absolutePos(new BlockPos(5, 5, 6)))).isEmpty(),
                    "beside the wheel (along its axle) is free");
            h.setBlock(hub, Blocks.AIR);
            h.runAfterDelay(25, () -> {
                h.assertTrue(h.getLevel().getEntities(WaterPower.WHEEL_HITBOX.get(), around, e -> true).isEmpty(), "the body goes with the hub");
                h.succeed();
            });
        });
    }

    /**
     * A mill along X at y 1: flumes x 1..3, the saw at x 4 facing west (logs come from the west), flumes x 5..7. Water at
     * x 0 (right) or x 8 (wrong way). The saw is fed {@code power} every tick, as a wheel's shaft would.
     */
    private static void mill(GameTestHelper h, ItemStack log, int power, boolean rightWay, List<ItemStack> expected) {
        floor(h);
        int z = 6;
        BlockState flume = Sawmill.LOG_FLUME.get().defaultBlockState().setValue(LogFlumeBlock.AXIS, Direction.Axis.X);
        for (int x = 1; x <= 7; x++)
            h.setBlock(new BlockPos(x, 1, z), x == 4 ? Sawmill.SAW_BENCH.get().defaultBlockState().setValue(SawBenchBlock.FACING, Direction.WEST) : flume);
        h.setBlock(new BlockPos(rightWay ? 0 : 8, 1, z), Blocks.WATER);
        SawBenchBlockEntity saw = (SawBenchBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(4, 1, z)));
        saw.setBladeForTest(Sawmill.BLADE_USES);
        saw.layForTest(log, -2);
        for (int t = 1; t < 240; t++) h.runAtTickTime(t, () -> saw.feed(power * WaterPower.DEG_PER_POWER, 0, 0, power));
        h.runAfterDelay(5, () -> h.assertTrue(saw.problem() == (rightWay ? (power >= Sawmill.NEED_POWER ? SawBenchBlockEntity.Problem.NONE
                : SawBenchBlockEntity.Problem.LOW_POWER) : SawBenchBlockEntity.Problem.WRONG_WAY), "problem: " + saw.problem()));
        h.runAfterDelay(200, () -> {
            List<ItemEntity> drops = h.getLevel().getEntities(EntityTypes.ITEM, new net.minecraft.world.phys.AABB(
                    net.minecraft.world.phys.Vec3.atLowerCornerOf(h.absolutePos(BlockPos.ZERO)), net.minecraft.world.phys.Vec3.atLowerCornerOf(h.absolutePos(new BlockPos(15, 10, 15)))), e -> true);
            if (expected.isEmpty()) {
                h.assertTrue(drops.isEmpty(), "nothing is sawn, got " + drops.size() + " items");
                h.assertTrue(saw.logs().size() == 1, "the log is still in the flume");
                float s = saw.logs().get(0).s;
                if (rightWay) h.assertTrue(Math.abs(s + SawBenchBlockEntity.HALF) < 0.01, "the log waits at the blade, s " + s);
                else h.assertTrue(s == -2, "the log doesn't move when the water runs the wrong way, s " + s);
                h.succeed();
                return;
            }
            h.assertTrue(saw.logs().isEmpty(), "the log has been sawn");
            for (ItemStack want : expected) {
                Item item = want.getItem();
                int got = drops.stream().filter(e -> e.getItem().is(item)).mapToInt(e -> e.getItem().getCount()).sum();
                h.assertTrue(got == want.getCount(), want.getHoverName().getString() + ": " + got + ", wanted " + want.getCount());
            }
            h.assertTrue(saw.blade() == Sawmill.BLADE_USES - 1, "the blade wore by one");
            h.succeed();
        });
    }

    private WaterPowerTest() {}
}
