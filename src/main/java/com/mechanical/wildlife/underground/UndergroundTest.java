package com.mechanical.wildlife.underground;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.function.Consumer;

import com.mechanical.Butchery;
import com.mechanical.wildlife.mountain.MountainFauna;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Game tests for the underground fauna: the darkness rule (a sealed stone room spawns, the open air beside it and the
 * same room lit by a torch don't, nor a spot outside the band), the depth bands (hyena/bear above Y 0, centipede and vittra
 * below, trolls and the rare lindworm deepest), the butchering yields (each its own hide, meat and parts), light driving
 * off trolls and vittra, a troll turning to stone in daylight and the lindworm's hoard.
 */
@EventBusSubscriber(modid = MODID)
public final class UndergroundTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final String[] TESTS = {"underground_spawn_rule", "underground_yields", "underground_light_repels",
            "underground_troll_daylight", "underground_lindworm_hoard"};

    static {
        TEST_FUNCTIONS.register(TESTS[0], () -> UndergroundTest::spawnRule);
        TEST_FUNCTIONS.register(TESTS[1], () -> UndergroundTest::yields);
        TEST_FUNCTIONS.register(TESTS[2], () -> UndergroundTest::lightRepels);
        TEST_FUNCTIONS.register(TESTS[3], () -> UndergroundTest::trollDaylight);
        TEST_FUNCTIONS.register(TESTS[4], () -> UndergroundTest::lindwormHoard);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        for (String name : TESTS) {
            var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, name), new TestEnvironmentDefinition.AllOf(List.of()));
            // the creature tests build rooms and spawn big mobs: keep them apart (padding 12); the daylight one needs the sky.
            // The AI ones (light, daylight) wait on fresh forced chunks, which can take a few hundred ticks to tick entities
            // (as WildlifeSwimTest): long timeout, and not required - sometimes they never start ticking (flaky, not a logic failure)
            boolean ai = name.equals(TESTS[2]) || name.equals(TESTS[3]);
            event.registerTest(Identifier.fromNamespaceAndPath(MODID, name), new FunctionGameTestInstance(
                    ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, name)),
                    new TestData<>(env, net.minecraft.world.level.Level.OVERWORLD, Identifier.withDefaultNamespace("empty"), 1600, 0, !ai,
                            net.minecraft.world.level.block.Rotation.NONE, false, 1, 1, name.equals(TESTS[3]), 12)));
        }
    }

    private static void spawnRule(GameTestHelper h) {
        // a sealed stone room: walls x/z 2..6, floor y 1, roof y 5; inside x/z 3..5, y 2..4
        for (int x = 2; x <= 6; x++) for (int y = 1; y <= 5; y++) for (int z = 2; z <= 6; z++) {
            boolean inside = x > 2 && x < 6 && y > 1 && y < 5 && z > 2 && z < 6;
            h.setBlock(x, y, z, inside ? Blocks.AIR : Blocks.STONE);
        }
        h.setBlock(9, 0, 9, Blocks.STONE);
        BlockPos room = h.absolutePos(new BlockPos(4, 2, 4)), open = h.absolutePos(new BlockPos(9, 1, 9));
        int y = room.getY();
        var level = h.getLevel();
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(Underground.canSpawn(level, room, y, y + 1), "the dark sealed room is a spawn spot (block " + level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, room) + ", sky " + level.getBrightness(net.minecraft.world.level.LightLayer.SKY, room) + ", floor " + level.getBlockState(room.below()) + ", at " + level.getBlockState(room) + ")"))
                .thenExecute(() -> {
                    h.assertTrue(!Underground.canSpawn(level, room, y + 1, y + 10), "not outside the depth band");
                    h.assertTrue(!Underground.canSpawn(level, open, open.getY(), open.getY() + 1), "not under the open sky");
                    h.setBlock(3, 2, 3, Blocks.TORCH);
                })
                .thenWaitUntil(() -> h.assertTrue(!Underground.canSpawn(level, room, y, y + 1), "a torch in the room stops spawns"))
                .thenExecute(() -> bands(h))
                .thenSucceed();
    }

    /**
     * Hyena or bear in Y 0..39; centipede or vittra in -30..-1; only trolls in -45..-31; trolls and now and then a lindworm
     * below -45; nothing below -64 or above 39.
     */
    private static void bands(GameTestHelper h) {
        RandomSource random = RandomSource.create(7);
        boolean hyena = false, bear = false, centipede = false, vittra = false, lindworm = false;
        int trolls = 0;
        for (int i = 0; i < 2000; i++) {
            Underground.Band upper = Underground.pick(20, random), lower = Underground.pick(-10, random);
            Underground.Band deep = Underground.pick(-40, random), deepest = Underground.pick(-55, random);
            h.assertTrue(upper != null && lower != null && deep != null && deepest != null, "a band at Y 20, -10, -40 and -55");
            h.assertTrue(upper.type().get() != UndergroundFauna.GIANT_CENTIPEDE.get(), "no centipede at Y 20");
            hyena |= upper.type().get() == MountainFauna.CAVE_HYENA.get();
            bear |= upper.type().get() == MountainFauna.CAVE_BEAR.get();
            centipede |= lower.type().get() == UndergroundFauna.GIANT_CENTIPEDE.get();
            vittra |= lower.type().get() == UndergroundFauna.VITTRA.get();
            h.assertTrue(lower.type().get() == UndergroundFauna.GIANT_CENTIPEDE.get() || lower.type().get() == UndergroundFauna.VITTRA.get(),
                    "only centipedes and vittra at Y -10");
            h.assertTrue(deep.type().get() == UndergroundFauna.TROLL.get(), "only trolls at Y -40");
            if (deepest.type().get() == UndergroundFauna.LINDWORM.get()) lindworm = true;
            else if (deepest.type().get() == UndergroundFauna.TROLL.get()) trolls++;
            else h.assertTrue(false, "only trolls and the lindworm at Y -55");
        }
        h.assertTrue(hyena && bear, "both hyenas and cave bears come at Y 20");
        h.assertTrue(centipede && vittra, "both centipedes and vittra come at Y -10");
        h.assertTrue(lindworm && trolls > 1800, "the lindworm comes at Y -55, but rarely (" + (2000 - trolls) + " in 2000)");
        h.assertTrue(Underground.pick(-65, random) == null && Underground.pick(40, random) == null, "nothing outside the bands");
    }

    /** A sealed stone room, inside x/z 1..7, y 2..5 (floor y 1, roof y 6): dark, out of the sky. */
    private static void room(GameTestHelper h) {
        for (int x = 0; x <= 8; x++) for (int y = 1; y <= 6; y++) for (int z = 0; z <= 8; z++) {
            boolean inside = x > 0 && x < 8 && y > 1 && y < 6 && z > 0 && z < 8;
            h.setBlock(x, y, z, inside ? Blocks.AIR : Blocks.STONE);
        }
    }

    /** The area reaches past the test's own (1-block) structure: keep its chunks ticking so the creatures act. */
    private static void forceChunks(GameTestHelper h, int size) {
        var level = h.getLevel();
        BlockPos lo = h.absolutePos(BlockPos.ZERO), hi = h.absolutePos(new BlockPos(size, 0, size));
        List<net.minecraft.world.level.ChunkPos> forced = new java.util.ArrayList<>();
        for (int cx = Math.min(lo.getX(), hi.getX()) >> 4; cx <= Math.max(lo.getX(), hi.getX()) >> 4; cx++)
            for (int cz = Math.min(lo.getZ(), hi.getZ()) >> 4; cz <= Math.max(lo.getZ(), hi.getZ()) >> 4; cz++)
                if (level.setChunkForced(cx, cz, true)) forced.add(new net.minecraft.world.level.ChunkPos(cx, cz));
        h.runBeforeTestEnd(() -> forced.forEach(c -> level.setChunkForced(c.x(), c.z(), false)));
    }

    private static void targetBoth(net.minecraft.world.entity.Mob a, net.minecraft.world.entity.Mob b, net.minecraft.world.entity.LivingEntity t) {
        a.setTarget(t);
        b.setTarget(t);
    }

    /**
     * In the dark a troll and a vittra keep after a player; holding a torch, or standing in torchlight, he is let go
     * (Underground.inLight; both back off).
     */
    private static void lightRepels(GameTestHelper h) {
        forceChunks(h, 8);
        room(h);
        var level = h.getLevel();
        Troll troll = h.spawn(UndergroundFauna.TROLL.get(), 2, 2, 2);
        Vittra vittra = h.spawn(UndergroundFauna.VITTRA.get(), 6, 2, 6);
        BlockPos at = h.absolutePos(new BlockPos(4, 2, 4));
        var lit = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var dark = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        for (var p : List.of(lit, dark)) p.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        lit.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(Items.TORCH));
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, at) == 0, "the room is dark"))
                .thenExecute(() -> {
                    h.assertTrue(!Underground.inLight(dark) && Underground.inLight(lit), "a torch in hand is light");
                    targetBoth(troll, vittra, lit);
                })
                .thenWaitUntil(() -> h.assertTrue(troll.getTarget() != lit && vittra.getTarget() != lit, "both let go of the torch bearer")) // they may pick the dark one beside him
                .thenExecute(() -> targetBoth(troll, vittra, dark))
                .thenIdle(30)
                .thenExecute(() -> {
                    h.assertTrue(troll.getTarget() == dark && vittra.getTarget() == dark, "in the dark they keep after him");
                    h.setBlock(4, 2, 3, Blocks.TORCH);
                })
                .thenWaitUntil(() -> h.assertTrue(Underground.inLight(dark), "a torch beside him lights him up"))
                .thenWaitUntil(() -> h.assertTrue(troll.getTarget() == null && vittra.getTarget() == null, "both let go of him in torchlight"))
                .thenSucceed();
    }

    /** A troll under the open sky at noon turns into two blocks of Troll Stone and is gone. */
    private static void trollDaylight(GameTestHelper h) {
        var level = h.getLevel();
        var server = level.getServer();
        long saved = level.getOverworldClockTime();
        forceChunks(h, 4);
        for (int x = 0; x <= 4; x++) for (int z = 0; z <= 4; z++) h.setBlock(x, 0, z, Blocks.STONE);
        Troll troll = h.spawn(UndergroundFauna.TROLL.get(), 2, 1, 2);
        troll.setNoAi(true); // stands where it is put
        h.startSequence()
                .thenExecute(() -> com.mechanical.Calendar.overworldClock(server)
                        .ifPresent(c -> server.clockManager().setTotalTicks(c, Math.floorDiv(saved, 24000L) * 24000L + 6000L)))
                .thenWaitUntil(() -> h.assertTrue(troll.isRemoved(), "the troll is gone in daylight"))
                .thenExecute(() -> {
                    com.mechanical.Calendar.overworldClock(server).ifPresent(c -> server.clockManager().setTotalTicks(c, saved));
                    h.assertBlockPresent(UndergroundFauna.TROLL_STONE.get(), 2, 1, 2);
                    h.assertBlockPresent(UndergroundFauna.TROLL_STONE.get(), 2, 2, 2);
                })
                .thenSucceed();
    }

    /** The hoard: gold every time, 3-5 gems; killed, a lindworm spills it where it dies. */
    private static void lindwormHoard(GameTestHelper h) {
        var level = h.getLevel();
        java.util.Set<Item> gems = java.util.Set.of(Items.EMERALD, Items.DIAMOND);
        for (int i = 0; i < 30; i++) {
            var loot = Lindworm.rollHoard(level, net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(BlockPos.ZERO)));
            h.assertTrue(loot.stream().anyMatch(s -> s.is(Items.GOLD_INGOT)) && loot.stream().anyMatch(s -> s.is(com.mechanical.Coins.GOLD_COIN.get())),
                    "gold ingots and gold coins in every hoard");
            long stones = loot.stream().filter(s -> gems.contains(s.getItem())
                    || net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).getPath().matches("(flawless_)?(ruby|sapphire|garnet|topaz|amber|moonstone|diamond|emerald)")).count();
            h.assertTrue(stones >= 3, "3 or more gemstones in every hoard (got " + stones + ")");
        }
        for (int x = 0; x <= 6; x++) for (int z = 0; z <= 6; z++) h.setBlock(x, 0, z, Blocks.STONE);
        Lindworm worm = h.spawn(UndergroundFauna.LINDWORM.get(), 3, 1, 3);
        worm.kill(level);
        h.assertItemEntityPresent(Items.GOLD_INGOT, new BlockPos(3, 1, 3), 4);
        h.assertItemEntityPresent(com.mechanical.Coins.GOLD_COIN.get(), new BlockPos(3, 1, 3), 4);
        h.succeed();
    }

    private static boolean has(List<Butchery.Extra> extras, Item item) {
        return extras.stream().anyMatch(e -> e.item().get() == item);
    }

    private static void yields(GameTestHelper h) {
        var c = Butchery.yieldFor(UndergroundFauna.GIANT_CENTIPEDE.get());
        h.assertTrue(c.meat().get() == UndergroundFauna.RAW_CENTIPEDE.get(), "the centipede gives its own meat");
        h.assertTrue(has(c.outer(), UndergroundFauna.CENTIPEDE_CHITIN.get()), "chitin comes off first");
        h.assertTrue(has(c.inner(), UndergroundFauna.CENTIPEDE_FANG.get()) && has(c.inner(), UndergroundFauna.CENTIPEDE_VENOM_SAC.get()),
                "then fangs and the venom sac");
        var bear = Butchery.yieldFor(MountainFauna.CAVE_BEAR.get());
        h.assertTrue(bear.meat().get() == MountainFauna.CAVE_BEAR_MEAT.get() && has(bear.outer(), MountainFauna.RAW_CAVE_BEAR_PELT.get())
                && has(bear.inner(), Items.BONE), "the cave bear: its meat, pelt and bones");
        var hyena = Butchery.yieldFor(MountainFauna.CAVE_HYENA.get());
        h.assertTrue(hyena.meat().get() == MountainFauna.HYENA_MEAT.get() && has(hyena.outer(), MountainFauna.RAW_HYENA_PELT.get())
                && has(hyena.inner(), MountainFauna.HYENA_FANG.get()) && has(hyena.inner(), Items.BONE), "the cave hyena: meat, pelt, fangs, bones");
        var v = Butchery.yieldFor(UndergroundFauna.VITTRA.get());
        h.assertTrue(v.meat().get() == UndergroundFauna.VITTRA_MEAT.get() && has(v.outer(), UndergroundFauna.VITTRA_HIDE.get())
                && has(v.inner(), UndergroundFauna.VITTRA_EYE.get()) && has(v.inner(), Items.BONE), "the vittra: meat, hide, eyes, bones");
        var t = Butchery.yieldFor(UndergroundFauna.TROLL.get());
        h.assertTrue(t.meat().get() == UndergroundFauna.TROLL_MEAT.get() && has(t.outer(), UndergroundFauna.TROLL_HIDE.get())
                && has(t.outer(), UndergroundFauna.TROLL_HAIR.get()) && has(t.inner(), UndergroundFauna.TROLL_TUSK.get())
                && has(t.inner(), Items.BONE), "the troll: meat, hide, hair, tusks, bones");
        var l = Butchery.yieldFor(UndergroundFauna.LINDWORM.get());
        h.assertTrue(l.meat().get() == UndergroundFauna.LINDWORM_MEAT.get() && has(l.outer(), UndergroundFauna.LINDWORM_SCALE.get())
                && has(l.inner(), UndergroundFauna.LINDWORM_FANG.get()) && has(l.inner(), UndergroundFauna.LINDWORM_VENOM_GLAND.get())
                && has(l.inner(), UndergroundFauna.LINDWORM_HEART.get()) && has(l.inner(), Items.BONE), "the lindworm: meat, scales, fangs, venom, heart, bones");
        h.assertTrue(Butchery.requiredKnifeTier(UndergroundFauna.TROLL.get()) == com.mechanical.KnifeItem.TUSK
                && Butchery.requiredKnifeTier(UndergroundFauna.LINDWORM.get()) == com.mechanical.KnifeItem.TUSK, "troll and lindworm need a tusk-grade knife");
        h.succeed();
    }

    private UndergroundTest() {}
}
