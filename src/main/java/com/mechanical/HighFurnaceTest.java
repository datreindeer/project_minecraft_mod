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
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
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
 * High Furnace game tests (docs/steel-furnace-plan.md, phases 1-2 "klart när"):
 * <ul>
 * <li>{@code high_furnace_shape}: 2x2x5, 2x3x6 and 3x3x8 form (with or without corners); 1 wide, 4 wide, 4 layers and 9
 * layers don't; a missing wall block, a block inside, a wrong floor and too few Fireboxes say so; an unloaded block pauses.</li>
 * <li>{@code high_furnace_heat}: the smallest furnace with B 16 heads for 1600°, with 13 for 1450°; the biggest with
 * three bellows at 16 for 1467°, with four for 1600°; a new furnace holds 600° while it dries; the wheel stopping
 * lets it fall; the Front's sight hatch glows when hot.</li>
 * </ul>
 * Two machines on one wheel get half each: WaterPowerTest {@code waterpower_shared_bellows}.
 */
@EventBusSubscriber(modid = MODID)
public final class HighFurnaceTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final String[] TESTS = {"high_furnace_shape", "high_furnace_heat"};

    static {
        TEST_FUNCTIONS.register("high_furnace_shape", () -> HighFurnaceTest::shape);
        TEST_FUNCTIONS.register("high_furnace_heat", () -> HighFurnaceTest::heat);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "high_furnace"), new TestEnvironmentDefinition.AllOf(List.of()));
        for (String t : TESTS)
            event.registerTest(Identifier.fromNamespaceAndPath(MODID, t), new FunctionGameTestInstance(
                    ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, t)), TestArea.of(env, 400, true)));
    }

    /** Where a test furnace stands: its Front's position (relative), facing north; the inside starts right south of it. */
    record Built(BlockPos front, List<BlockPos> fireboxes) {}

    /**
     * Builds a furnace: the inside x {@code x0}..x0+wide-1, z {@code z0}..z0+deep-1, the Silica floor at y 1 and
     * {@code layers} wall layers from y 2; the Front in the north wall at x0; Fireboxes in the east wall, then the west.
     * No corners. Returns where the Front and Fireboxes are (relative).
     */
    static Built build(GameTestHelper h, int x0, int z0, int wide, int deep, int layers, int fireboxes) {
        for (int x = x0 - 1; x <= x0 + wide; x++)
            for (int z = z0 - 1; z <= z0 + deep; z++)
                for (int y = 1; y <= layers + 2; y++) h.setBlock(new BlockPos(x, y, z), Blocks.AIR);
        for (int x = x0; x < x0 + wide; x++) for (int z = z0; z < z0 + deep; z++) h.setBlock(new BlockPos(x, 1, z), HighFurnace.SILICA_BRICKS.get());
        for (int y = 2; y < 2 + layers; y++) {
            for (int x = x0; x < x0 + wide; x++) {
                h.setBlock(new BlockPos(x, y, z0 - 1), HighFurnace.BANDED_FURNACE_CASING.get());
                h.setBlock(new BlockPos(x, y, z0 + deep), HighFurnace.BANDED_FURNACE_CASING.get());
            }
            for (int z = z0; z < z0 + deep; z++) {
                h.setBlock(new BlockPos(x0 - 1, y, z), HighFurnace.BANDED_FURNACE_CASING.get());
                h.setBlock(new BlockPos(x0 + wide, y, z), HighFurnace.BANDED_FURNACE_CASING.get());
            }
        }
        BlockPos front = new BlockPos(x0, 2, z0 - 1);
        h.setBlock(front, HighFurnace.HIGH_FURNACE_FRONT.get().defaultBlockState().setValue(HighFurnaceFrontBlock.FACING, Direction.NORTH));
        List<BlockPos> boxes = new java.util.ArrayList<>();
        for (int i = 0; i < fireboxes; i++) {
            boolean east = i < deep;
            BlockPos p = new BlockPos(east ? x0 + wide : x0 - 1, 2, z0 + (east ? i : i - deep));
            h.setBlock(p, HighFurnace.FIREBOX.get().defaultBlockState().setValue(HighFurnaceWallBlock.Facing.FACING, east ? Direction.EAST : Direction.WEST));
            boxes.add(p);
        }
        return new Built(front, boxes);
    }

    private static HighFurnaceShape.Result find(GameTestHelper h, Built b) {
        return HighFurnaceShape.find(h.getLevel(), h.absolutePos(b.front()), Direction.NORTH, p -> true);
    }

    private static String key(HighFurnaceShape.Result r) {
        if (r.paused()) return "paused";
        Component c = r.problem();
        return c == null ? "formed" : c.getContents() instanceof TranslatableContents t ? t.getKey().substring("message.mechanical.high_furnace.".length()) : c.getString();
    }

    private static void expect(GameTestHelper h, HighFurnaceShape.Result r, String want, String what) {
        h.assertTrue(key(r).equals(want), what + ": wanted " + want + ", got " + key(r) + (r.problem() != null ? " (" + r.problem().getString() + ")" : ""));
    }

    private static void shape(GameTestHelper h) {
        int x0 = 4, z0 = 4;
        // the three sizes of the plan form (the minimum fireboxes each)
        Built b = build(h, x0, z0, 2, 2, 5, 1);
        HighFurnaceShape.Result r = find(h, b);
        expect(h, r, "formed", "2x2x5");
        h.assertTrue(r.shape().volume() == 20 && r.shape().fireboxes().size() == 1, "2x2x5: 20 blocks, 1 firebox");
        expect(h, find(h, build(h, x0, z0, 2, 3, 6, 2)), "formed", "2x3x6");
        r = find(h, build(h, x0, z0, 3, 3, 8, 3));
        expect(h, r, "formed", "3x3x8");
        h.assertTrue(r.shape().volume() == 72, "3x3x8 holds 72 blocks, got " + r.shape().volume());
        // corners are optional: pillars there change nothing
        for (int y = 2; y < 10; y++) for (int[] c : new int[][] {{x0 - 1, z0 - 1}, {x0 + 3, z0 - 1}, {x0 - 1, z0 + 3}, {x0 + 3, z0 + 3}})
            h.setBlock(new BlockPos(c[0], y, c[1]), HighFurnace.BANDED_FURNACE_CASING.get());
        expect(h, find(h, b), "formed", "3x3x8 with corner pillars");
        // an unloaded block pauses the furnace instead of breaking it
        BlockPos far = h.absolutePos(new BlockPos(x0 + 3, 6, z0 + 1));
        expect(h, HighFurnaceShape.find(h.getLevel(), h.absolutePos(b.front()), Direction.NORTH, p -> !p.equals(far)), "paused", "unloaded wall");
        // too small and too big
        expect(h, find(h, build(h, x0, z0, 1, 2, 5, 1)), "too_narrow", "1 wide");
        expect(h, find(h, build(h, x0, z0, 4, 2, 5, 2)), "too_wide", "4 wide");
        expect(h, find(h, build(h, x0, z0, 2, 2, 4, 1)), "too_low", "4 layers");
        expect(h, find(h, build(h, x0, z0, 2, 2, 9, 1)), "too_tall", "9 layers");
        // what is wrong is said
        b = build(h, x0, z0, 2, 2, 5, 1);
        h.setBlock(new BlockPos(x0 - 1, 4, z0 + 1), Blocks.AIR);
        expect(h, find(h, b), "missing", "a missing wall block");
        b = build(h, x0, z0, 2, 2, 5, 1);
        h.setBlock(new BlockPos(x0 + 1, 4, z0 + 1), Blocks.COBBLESTONE);
        expect(h, find(h, b), "blocked", "a block inside");
        b = build(h, x0, z0, 2, 2, 5, 1);
        h.setBlock(new BlockPos(x0 + 1, 1, z0 + 1), Blocks.STONE);
        expect(h, find(h, b), "floor", "a stone in the floor");
        expect(h, find(h, build(h, x0, z0, 3, 3, 5, 1)), "fireboxes", "45 blocks with one firebox");
        // and the Front itself forms it
        b = build(h, x0, z0, 2, 2, 5, 1);
        HighFurnaceBlockEntity f = (HighFurnaceBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(b.front()));
        f.check(h.getLevel());
        h.assertTrue(f.formed() && f.problem() == null, "the Front forms the furnace: " + f.problem());
        h.succeed();
    }

    /** Feeds a bellows {@code power} as a wheel's shaft would (every tick, from the test). */
    private static GreatBellowsBlockEntity bellows(GameTestHelper h, BlockPos firebox, Direction out) {
        BlockPos at = firebox.relative(out);
        h.setBlock(at, HighFurnace.GREAT_BELLOWS.get().defaultBlockState().setValue(GreatBellowsBlock.FACING, out.getOpposite()));
        return (GreatBellowsBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(at));
    }

    private static HighFurnaceBlockEntity furnace(GameTestHelper h, Built b) {
        HighFurnaceBlockEntity f = (HighFurnaceBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(b.front()));
        f.check(h.getLevel());
        h.assertTrue(f.formed(), "formed: " + f.problem());
        for (BlockPos p : b.fireboxes()) f.addFuel(h.absolutePos(p), new ItemStack(Items.CHARCOAL, 32));
        f.light();
        return f;
    }

    private static void heat(GameTestHelper h) {
        Built small = build(h, 1, 3, 2, 2, 5, 1), big = build(h, 8, 3, 3, 3, 8, 4);
        GreatBellowsBlockEntity sb = bellows(h, small.fireboxes().get(0), Direction.EAST);
        List<GreatBellowsBlockEntity> bb = new java.util.ArrayList<>();
        for (BlockPos p : big.fireboxes()) bb.add(bellows(h, p, p.getX() > 8 ? Direction.EAST : Direction.WEST));
        int[] smallPower = {16}, bigFed = {3};
        boolean[] stopped = {false};
        for (int t = 1; t < 380; t++) h.runAtTickTime(t, () -> {
            if (!stopped[0]) sb.feed(smallPower[0] * WaterPower.DEG_PER_POWER, 0, 0, smallPower[0]);
            for (int i = 0; i < bb.size(); i++) if (i < bigFed[0]) bb.get(i).feed(4, 0, 0, 16);
        });
        h.runAfterDelay(2, () -> {
            HighFurnaceBlockEntity s = furnace(h, small), g = furnace(h, big);
            h.assertTrue(s.lit() && g.lit(), "both lit");
            h.assertTrue(Math.round(s.target()) == HighFurnaceBlockEntity.DRY_MAX, "a new furnace holds 600° while it dries, target " + s.target());
            s.setDriedForTest();
            g.setDriedForTest();
            h.runAfterDelay(3, () -> {
                h.assertTrue(Math.round(s.target()) == 1600, "smallest, B 16: 1600°, got " + s.target());
                h.assertTrue(Math.round(g.target()) == 1467, "biggest, three bellows at 16: 1467°, got " + g.target());
                smallPower[0] = 13;
                bigFed[0] = 4;
                h.runAfterDelay(3, () -> {
                    h.assertTrue(Math.round(s.target()) == 1450, "smallest, B 13: 1450°, got " + s.target());
                    h.assertTrue(Math.round(g.target()) == 1600, "biggest, four bellows: 1600°, got " + g.target());
                    // the sight hatch glows when it is hot
                    s.setTemperatureForTest(1500);
                    h.runAfterDelay(2, () -> {
                        BlockState front = h.getBlockState(small.front());
                        h.assertTrue(front.getValue(HighFurnace.HEAT) == 3, "the hatch glows white-hot, heat " + front.getValue(HighFurnace.HEAT));
                        h.assertTrue(h.getBlockState(small.fireboxes().get(0)).getValue(HighFurnace.HEAT) == 3, "the firebox glows");
                        // the wheel stops: the bellows lapse, the heat heads back to charcoal's 800° and falls
                        stopped[0] = true;
                        h.runAfterDelay(WaterPower.LAPSE_TICKS + 30, () -> {
                            h.assertTrue(sb.power() == 0, "the bellows stopped, power " + sb.power());
                            h.assertTrue(Math.round(s.target()) == 800, "no blast: 800°, got " + s.target());
                            h.assertTrue(s.temperature() < 1500, "the heat falls, " + s.temperature());
                            h.succeed();
                        });
                    });
                });
            });
        });
    }

    private HighFurnaceTest() {}
}
