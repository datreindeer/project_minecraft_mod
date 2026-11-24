package com.mechanical.wildlife.night;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.function.Consumer;

import com.mechanical.Blood;
import com.mechanical.Butchery;
import com.mechanical.Mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Game tests for the night monsters: a creeper never enters the world and a Gloson or Mushroom Man comes in its place
 * (always the Mushroom Man out of the sky); a Mushroom Man's burst breaks no blocks and blinds and sickens what stands in
 * it; the Gloson's butchering yield; and (optional, it needs AI in fresh chunks) a Gloson walking away from a fire.
 */
@EventBusSubscriber(modid = MODID)
public final class NightTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final String[] TESTS = {"night_creeper_replaced", "night_spore_burst", "night_gloson_yields", "night_gloson_fears_fire"};

    static {
        TEST_FUNCTIONS.register(TESTS[0], () -> NightTest::creeperReplaced);
        TEST_FUNCTIONS.register(TESTS[1], () -> NightTest::sporeBurst);
        TEST_FUNCTIONS.register(TESTS[2], () -> NightTest::yields);
        TEST_FUNCTIONS.register(TESTS[3], () -> NightTest::fearsFire);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        for (String name : TESTS) {
            var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, name), new TestEnvironmentDefinition.AllOf(List.of()));
            // the fire test waits on mob AI in fresh forced chunks (as WildlifeSwimTest): long timeout
            boolean ai = name.equals(TESTS[3]);
            event.registerTest(Identifier.fromNamespaceAndPath(MODID, name), new FunctionGameTestInstance(
                    ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, name)),
                    new TestData<>(env, net.minecraft.world.level.Level.OVERWORLD, Identifier.withDefaultNamespace("empty"), ai ? 1600 : 200, 0, true,
                            net.minecraft.world.level.block.Rotation.NONE, false, 1, 1, false, ai ? 24 : 12)));
        }
    }

    /** Clears x/z 0..size, y 1..height to air over a stone floor (other tests leave blocks behind). */
    private static void clear(GameTestHelper h, int size, int height) {
        for (int x = 0; x <= size; x++) for (int z = 0; z <= size; z++) {
            h.setBlock(x, 0, z, Blocks.STONE);
            for (int y = 1; y <= height; y++) h.setBlock(x, y, z, Blocks.AIR);
        }
    }

    private static AABB area(GameTestHelper h, int size) {
        return new AABB(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(new BlockPos(-2, -1, -2))), net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(new BlockPos(size + 2, 6, size + 2))));
    }

    private static void creeperReplaced(GameTestHelper h) {
        clear(h, 6, 5);
        var level = h.getLevel();
        h.spawn(EntityTypes.CREEPER, 3, 1, 3);
        // out of the sky (a stone roof): always the Mushroom Man
        for (int x = 0; x <= 6; x++) for (int z = 0; z <= 6; z++) h.setBlock(x, 4, z, Blocks.STONE);
        BlockPos under = h.absolutePos(new BlockPos(1, 1, 1));
        h.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    AABB box = area(h, 6);
                    h.assertTrue(level.getEntitiesOfClass(Creeper.class, box).isEmpty(), "no creeper in the world");
                    int night = level.getEntitiesOfClass(Mob.class, box, m -> m instanceof Gloson || m instanceof MushroomMan).size();
                    h.assertTrue(night == 1, "one Gloson or Mushroom Man in its place (found " + night + ")");
                    level.getEntitiesOfClass(Mob.class, box, m -> m instanceof Gloson || m instanceof MushroomMan).forEach(m -> m.discard());
                })
                // the roof's shadow (sky light) takes a moment to spread
                .thenWaitUntil(() -> h.assertTrue(!level.canSeeSky(under), "the roof shuts out the sky"))
                .thenExecute(() -> {
                    for (int i = 0; i < 20; i++) {
                        Mob mob = NightFauna.create(level, under, EntitySpawnReason.NATURAL);
                        h.assertTrue(mob instanceof MushroomMan, "under a roof the creeper's place goes to the Mushroom Man (got " + mob + ")");
                    }
                })
                .thenSucceed();
    }

    private static void sporeBurst(GameTestHelper h) {
        clear(h, 6, 5);
        var level = h.getLevel();
        // blocks right round it: none may break
        BlockPos[] ring = {new BlockPos(2, 1, 3), new BlockPos(4, 1, 3), new BlockPos(3, 1, 2), new BlockPos(3, 1, 4), new BlockPos(2, 1, 2)};
        Block[] kinds = {Blocks.GLASS, Blocks.DIRT, Blocks.OAK_PLANKS, Blocks.SHORT_GRASS, Blocks.TORCH};
        for (int i = 0; i < ring.length; i++) {
            if (kinds[i] == Blocks.SHORT_GRASS || kinds[i] == Blocks.TORCH) h.setBlock(ring[i].below(), Blocks.DIRT);
            h.setBlock(ring[i], kinds[i]);
        }
        MushroomMan man = h.spawn(NightFauna.MUSHROOM_MAN.get(), 3, 1, 3);
        Gloson victim = h.spawn(NightFauna.GLOSON.get(), 4, 1, 5);
        man.setNoAi(true); // tick() still swells it
        victim.setNoAi(true);
        man.setSwellDir(1);
        // ticked by hand: entities in a test's fresh chunks may not tick on their own
        for (int i = 0; i <= MushroomMan.MAX_SWELL && !man.isRemoved(); i++) man.tick();
        h.assertTrue(man.isRemoved(), "the Mushroom Man bursts after swelling");
        var clouds = level.getEntitiesOfClass(net.minecraft.world.entity.AreaEffectCloud.class, area(h, 6));
        h.assertTrue(!clouds.isEmpty(), "it leaves a spore cloud");
        for (int i = 0; i < 10; i++) clouds.forEach(c -> c.tick());
        h.assertTrue(victim.hasEffect(MobEffects.BLINDNESS) && victim.hasEffect(MobEffects.NAUSEA), "the spores blind and sicken what stands in the cloud");
        for (int i = 0; i < ring.length; i++) h.assertBlockPresent(kinds[i], ring[i]);
        h.assertBlockPresent(Blocks.STONE, new BlockPos(3, 0, 3));
        h.assertTrue(!Blood.bleeds(man), "a Mushroom Man has no blood");
        victim.discard();
        clouds.forEach(c -> c.discard());
        h.succeed();
    }

    private static boolean has(List<Butchery.Extra> extras, Item item) {
        return extras.stream().anyMatch(e -> e.item().get() == item);
    }

    private static void yields(GameTestHelper h) {
        var g = Butchery.yieldFor(NightFauna.GLOSON.get());
        h.assertTrue(g.meat().get() == NightFauna.RAW_GLOSON.get(), "the Gloson gives its own meat");
        h.assertTrue(has(g.outer(), NightFauna.GLOSON_BRISTLES.get()) && has(g.outer(), Mechanical.RAW_HIDE.get()), "bristles and hide come off first");
        h.assertTrue(g.inner().stream().anyMatch(e -> e.item().get() == NightFauna.RAZOR_RIDGE.get() && e.chance() >= 1.0F) && has(g.inner(), Items.BONE),
                "then the Razor Ridge (always) and bones");
        h.succeed();
    }

    /** A Gloson put down by a burning campfire walks out of its reach. */
    private static void fearsFire(GameTestHelper h) {
        forceChunks(h, 16);
        clear(h, 16, 4); // floor all round: it can only walk off as far as there is ground (padding 24 keeps this off other tests)
        h.setBlock(4, 1, 8, Blocks.CAMPFIRE); // off to one side: 12 blocks of floor beyond the Gloson (from the middle the edge was only 8 out)
        h.setTime(Math.floorDiv(h.getLevel().getOverworldClockTime(), 24000L) * 24000L + 18000L); // midnight: by day it dozes where it is put
        Gloson g = h.spawn(NightFauna.GLOSON.get(), 6, 1, 8);
        BlockPos fire = h.absolutePos(new BlockPos(4, 1, 8));
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(com.mechanical.NightDangers.nearFire(h.getLevel(), net.minecraft.world.phys.Vec3.atCenterOf(fire), 1), "the campfire burns"))
                .thenWaitUntil(() -> h.assertTrue(g.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(fire)) > 8 * 8, "the Gloson keeps away from the fire (at "
                        + String.format("%.1f", Math.sqrt(g.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(fire)))) + ")"))
                .thenExecute(g::discard)
                .thenSucceed();
    }

    /** The area reaches past the test's own (1-block) structure: keep its chunks ticking so the creature acts. */
    private static void forceChunks(GameTestHelper h, int size) {
        var level = h.getLevel();
        BlockPos lo = h.absolutePos(BlockPos.ZERO), hi = h.absolutePos(new BlockPos(size, 0, size));
        List<net.minecraft.world.level.ChunkPos> forced = new java.util.ArrayList<>();
        for (int cx = Math.min(lo.getX(), hi.getX()) >> 4; cx <= Math.max(lo.getX(), hi.getX()) >> 4; cx++)
            for (int cz = Math.min(lo.getZ(), hi.getZ()) >> 4; cz <= Math.max(lo.getZ(), hi.getZ()) >> 4; cz++)
                if (level.setChunkForced(cx, cz, true)) forced.add(new net.minecraft.world.level.ChunkPos(cx, cz));
        h.runBeforeTestEnd(() -> forced.forEach(c -> level.setChunkForced(c.x(), c.z(), false)));
    }

    private NightTest() {}
}
