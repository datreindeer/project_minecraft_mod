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
    private static final String[] TESTS = {"high_furnace_shape", "high_furnace_heat", "high_furnace_melt", "high_furnace_pour", "high_furnace_iron"};

    static {
        TEST_FUNCTIONS.register("high_furnace_shape", () -> HighFurnaceTest::shape);
        TEST_FUNCTIONS.register("high_furnace_heat", () -> HighFurnaceTest::heat);
        TEST_FUNCTIONS.register("high_furnace_melt", () -> HighFurnaceTest::melt);
        TEST_FUNCTIONS.register("high_furnace_pour", () -> HighFurnaceTest::pour);
        TEST_FUNCTIONS.register("high_furnace_iron", () -> HighFurnaceTest::iron);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "high_furnace"), new TestEnvironmentDefinition.AllOf(List.of()));
        for (String t : TESTS)
            event.registerTest(Identifier.fromNamespaceAndPath(MODID, t), new FunctionGameTestInstance(
                    ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, t)), TestArea.of(env, t.endsWith("melt") || t.endsWith("iron") ? 1000 : 400, true)));
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

    /**
     * Phase 3: the smallest furnace holds 8000 mB in 20 places. At 900° 9 Crushed Copper Ore wait in their places while the
     * Crushed Tin Ore (thrown in at the top) melts; at 1200° they melt and mix 9:1 into exactly 2000 mB Bronze; 9 Gold
     * Nuggets give 198 mB (22 each); 10 Copper Ingots + 1 Tin Ingot give 2000 more Bronze and leave a 200 mB Copper layer.
     */
    private static void melt(GameTestHelper h) {
        Built b = build(h, 4, 4, 2, 2, 5, 1);
        HighFurnaceBlockEntity f = (HighFurnaceBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(b.front()));
        f.check(h.getLevel());
        h.assertTrue(f.formed() && f.pool().capacity() == 8000 && f.slots().size() == 20, "8000 mB in 20 places: " + f.pool().capacity() + ", " + f.slots().size());
        float[] heat = {900};
        for (int t = 1; t < 980; t++) h.runAtTickTime(t, () -> f.setTemperatureForTest(heat[0]));
        for (int i = 0; i < 9; i++) f.take(new ItemStack(BronzeOres.CRUSHED_COPPER_ORE.get()));
        for (int i = 0; i < 9; i++) f.take(new ItemStack(net.minecraft.world.item.Items.GOLD_NUGGET));
        // the tin goes in the real way: thrown in at the open top
        net.minecraft.world.phys.Vec3 top = f.topForTest();
        h.getLevel().addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(h.getLevel(), top.x, top.y + 1, top.z, new ItemStack(BronzeOres.CRUSHED_TIN_ORE.get())));
        h.runAfterDelay(150, () -> {
            h.assertTrue(f.pool().get(Molten.TIN) == 200, "the tin melted at 900°: " + f.pool().layers());
            h.assertTrue(f.pool().get(Molten.COPPER) == 0 && f.slots().stream().filter(s -> s.is(BronzeOres.CRUSHED_COPPER_ORE.get())).count() == 9,
                    "the copper ore waits for 1085°: " + f.pool().layers());
            heat[0] = 1200;
            h.runAfterDelay(300, () -> {
                h.assertTrue(f.pool().get(Molten.BRONZE) == 2000 && f.pool().get(Molten.COPPER) == 0 && f.pool().get(Molten.TIN) == 0,
                        "9 + 1 Crushed Ore make 2000 mB Bronze: " + f.pool().layers());
                h.assertTrue(f.pool().get(Molten.GOLD) == 9 * Molten.NUGGET, "9 nuggets give 198 mB: " + f.pool().get(Molten.GOLD));
                for (int i = 0; i < 10; i++) f.take(new ItemStack(net.minecraft.world.item.Items.COPPER_INGOT));
                f.take(new ItemStack(Mechanical.TIN_INGOT.get()));
                h.runAfterDelay(300, () -> {
                    h.assertTrue(f.pool().get(Molten.BRONZE) == 4000 && f.pool().get(Molten.COPPER) == 200,
                            "10 Copper + 1 Tin Ingot: 2000 more Bronze and 200 Copper left: " + f.pool().layers());
                    h.assertTrue(f.pool().bottom() == Molten.GOLD && f.pouring() == Molten.GOLD, "gold lies at the bottom and pours first");
                    f.select(Molten.COPPER);
                    h.assertTrue(f.pouring() == Molten.COPPER, "a chosen layer pours instead");
                    h.succeed();
                });
            });
        });
    }

    /**
     * Phase 4: a Pouring Spout on the Taphole pours the chosen layer into what stands under its lip, and no mB is made or
     * lost on the way: 1000 mB Bronze fill a Clay Pickaxe Head Mold (600) and leave 400; a mold taken away mid-pour gives
     * the stream back to the pool; an Iron Ingot Mold takes 800 mB Gold and gives 4 Gold Ingots; a Clay Mold takes no Steel.
     */
    private static void pour(GameTestHelper h) {
        Built b = build(h, 4, 4, 2, 2, 5, 1);
        BlockPos tap = new BlockPos(4, 2, 6), spout = tap.south(), under = spout.below();
        h.setBlock(tap, HighFurnace.TAPHOLE.get().defaultBlockState().setValue(HighFurnaceWallBlock.Facing.FACING, Direction.SOUTH));
        h.setBlock(spout, HighFurnace.POURING_SPOUT.get().defaultBlockState().setValue(PouringSpoutBlock.FACING, Direction.SOUTH).setValue(PouringSpoutBlock.OPEN, true));
        h.setBlock(under, Casting.CLAY_PICKAXE_HEAD_MOLD.get());
        HighFurnaceBlockEntity f = (HighFurnaceBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(b.front()));
        f.check(h.getLevel());
        h.assertTrue(f.formed() && f.shape().tapholes().size() == 1, "formed with a taphole: " + f.problem());
        for (int t = 1; t < 380; t++) h.runAtTickTime(t, () -> f.setTemperatureForTest(1200));
        f.pool().add(Molten.BRONZE, 1000);
        PouringSpoutBlockEntity s = (PouringSpoutBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(spout));
        h.runAfterDelay(30, () -> {
            h.assertTrue(h.getBlockState(under).getValue(ClayMoldBlock.FILL) == ClayMoldBlock.Fill.MOLTEN
                    && h.getBlockState(under).getValue(ClayMoldBlock.METAL) == Metal.BRONZE, "the mold under the lip is full of bronze");
            h.assertTrue(f.pool().get(Molten.BRONZE) == 400 && s.bufferForTest() == 0, "exactly 600 mB went in: " + f.pool().get(Molten.BRONZE));
            // a fresh mold, taken away mid-pour: what was in the stream goes back
            h.setBlock(under, Casting.CLAY_PICKAXE_HEAD_MOLD.get());
            h.runAfterDelay(5, () -> {
                int inStream = s.bufferForTest();
                h.assertTrue(inStream > 0 && f.pool().get(Molten.BRONZE) + inStream == 400, "pouring: " + inStream + " in the stream");
                h.setBlock(under, Blocks.AIR);
                h.runAfterDelay(2, () -> {
                    h.assertTrue(f.pool().get(Molten.BRONZE) == 400 && s.bufferForTest() == 0, "the stream went back: " + f.pool().get(Molten.BRONZE));
                    // gold into an Iron Ingot Mold: 4 ingots
                    f.pool().add(Molten.GOLD, 800);
                    f.select(Molten.GOLD);
                    h.setBlock(under, HighFurnace.IRON_INGOT_MOLD.get());
                    h.runAfterDelay(30, () -> {
                        IngotMoldBlock.Entity mold = (IngotMoldBlock.Entity) h.getLevel().getBlockEntity(h.absolutePos(under));
                        h.assertTrue(mold.molten() == Molten.GOLD && f.pool().get(Molten.GOLD) == 0, "800 mB of gold in the iron mold: " + f.pool().layers());
                        // steel is too hot for clay: a Clay Mold takes none
                        f.pool().add(Molten.STEEL, 400);
                        f.select(Molten.STEEL);
                        h.setBlock(under, Casting.CLAY_PICKAXE_HEAD_MOLD.get());
                        for (int t = 0; t < 40; t++) h.runAtTickTime(h.getTick() + t + 1, () -> f.setTemperatureForTest(1600));
                        h.runAfterDelay(20, () -> {
                            h.assertTrue(h.getBlockState(under).getValue(ClayMoldBlock.FILL) == ClayMoldBlock.Fill.EMPTY && f.pool().get(Molten.STEEL) == 400,
                                    "a clay mold takes no steel");
                            h.succeed();
                        });
                    });
                });
            });
        });
    }

    /**
     * Phase 5: 4 Roasted Iron Ore + 1 Flux at 1450° give 1200 mB Pig Iron + 100 mB Slag; without flux 800 + 300; each ore
     * burns half a Charcoal from the Firebox; 1 Pig Iron + 2 Wrought Iron Ingots (wrought melts at 1550°) mix into 600 mB Steel (3 ingots).
     */
    private static void iron(GameTestHelper h) {
        Built b = build(h, 4, 4, 2, 2, 5, 1);
        HighFurnaceBlockEntity f = (HighFurnaceBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(b.front()));
        f.check(h.getLevel());
        f.addFuel(h.absolutePos(b.fireboxes().get(0)), new ItemStack(net.minecraft.world.item.Items.CHARCOAL, 10));
        float[] heat = {1460};
        for (int t = 1; t < 980; t++) h.runAtTickTime(t, () -> f.setTemperatureForTest(heat[0]));
        net.minecraft.world.phys.Vec3 top = f.topForTest();
        h.getLevel().addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(h.getLevel(), top.x, top.y + 1, top.z, new ItemStack(IronOres.FLUX.get())));
        h.runAfterDelay(40, () -> {
            h.assertTrue(f.flux() == HighFurnaceMelting.ORES_PER_FLUX, "one flux serves 4 ores: " + f.flux());
            for (int i = 0; i < 4; i++) f.take(new ItemStack(Ironworking.ROASTED_IRON_ORE.get()));
            h.runAfterDelay(200, () -> {
                h.assertTrue(f.pool().get(Molten.PIG_IRON) == 1200 && f.pool().get(Molten.SLAG) == 100, "with flux: 1200 + 100, got " + f.pool().layers());
                h.assertTrue(f.pool().layers().getLast().getKey() == Molten.SLAG, "slag floats on top");
                h.assertTrue(f.fireList().get(0).fuel == 8, "4 ores burnt 2 Charcoal: " + f.fireList().get(0).fuel);
                f.pool().drain(Molten.PIG_IRON, 1200);
                f.pool().drain(Molten.SLAG, 100);
                for (int i = 0; i < 4; i++) f.take(new ItemStack(Ironworking.ROASTED_IRON_ORE.get()));
                h.runAfterDelay(200, () -> {
                    h.assertTrue(f.pool().get(Molten.PIG_IRON) == 800 && f.pool().get(Molten.SLAG) == 300, "without flux: 800 + 300, got " + f.pool().layers());
                    f.pool().drain(Molten.PIG_IRON, 800);
                    f.pool().drain(Molten.SLAG, 300);
                    heat[0] = 1560; // wrought iron melts at 1550
                    f.take(new ItemStack(HighFurnace.PIG_IRON_INGOT.get()));
                    f.take(new ItemStack(Ironworking.WROUGHT_IRON_INGOT.get()));
                    f.take(new ItemStack(Ironworking.WROUGHT_IRON_INGOT.get()));
                    h.runAfterDelay(200, () -> {
                        h.assertTrue(f.pool().get(Molten.STEEL) == 600 && f.pool().total() == 600, "1 Pig + 2 Wrought: 600 mB Steel, got " + f.pool().layers());
                        h.succeed();
                    });
                });
            });
        });
    }

    private HighFurnaceTest() {}
}
