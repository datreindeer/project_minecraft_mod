package com.mechanical;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Game test for the {@link PloughEntity}: a horse draws it over grass and dirt with the share down and a 5-wide strip
 * becomes farmland at fertility {@link PloughEntity#PLOUGHED}, leaving stone, a covered block and a sown field alone;
 * with the share raised it tills nothing. Right-click toggles the share, sneak + right-click unhitches.
 */
@EventBusSubscriber(modid = MODID)
public final class PloughTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> TEST = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "plough"));

    static {
        TEST_FUNCTIONS.register("plough", () -> PloughTest::test);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "plough"), new TestEnvironmentDefinition.AllOf(List.of()));
        // padded: the field is 11 x 20
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "plough"), new FunctionGameTestInstance(TEST,
                new TestData<>(env, Level.OVERWORLD, Identifier.withDefaultNamespace("empty"), 40, 0, true, Rotation.NONE, false, 1, 1, false, 24)));
    }

    private static void test(GameTestHelper h) {
        try {
            run(h);
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw h.assertionException(e + " at " + java.util.Arrays.toString(java.util.Arrays.copyOf(e.getStackTrace(), 6)));
        }
    }

    private static void run(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        for (int x = 0; x <= 10; x++)
            for (int z = 0; z <= 20; z++) {
                h.setBlock(new BlockPos(x, 0, z), z % 2 == 0 ? Blocks.GRASS_BLOCK : Blocks.DIRT);
                for (int y = 1; y <= 3; y++) h.setBlock(new BlockPos(x, y, z), Blocks.AIR);
            }
        BlockPos stone = new BlockPos(4, 0, 6), covered = new BlockPos(6, 0, 7), sown = new BlockPos(7, 0, 5);
        h.setBlock(stone, Blocks.STONE);
        h.setBlock(covered.above(), Blocks.COBBLESTONE);
        h.setBlock(sown, Blocks.FARMLAND.defaultBlockState().setValue(Fertility.FERTILITY, 0));
        h.setBlock(sown.above(), Blocks.WHEAT);

        PloughEntity plough = Carting.PLOUGH.get().create(level, EntitySpawnReason.EVENT);
        plough.setPos(h.absoluteVec(new Vec3(5.5, 1, 2.5)));
        plough.setYRot(0); // facing +z
        level.addFreshEntity(plough);
        AbstractHorse horse = com.mechanical.wildlife.Wildlife.TAME_HORSE.get().create(level, EntitySpawnReason.EVENT);
        horse.setPos(plough.position().add(0, 0, PloughEntity.LEAD));
        horse.setTamed(true);
        horse.setNoAi(true);
        level.addFreshEntity(horse);
        plough.setHarness(horse);
        h.assertTrue(!plough.isShareDown(), "a placed plough has its share up");

        // the grab key while hitched lowers the share
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        plough.grab(player);
        h.assertTrue(plough.isShareDown(), "the grab key lowers the share");

        draw(plough, horse, 20); // the share runs from z 2.4 to about 10.4
        for (int z = 4; z <= 9; z++) {
            for (int x = 3; x <= 7; x++) {
                BlockPos at = new BlockPos(x, 0, z);
                if (at.equals(stone) || at.equals(covered) || at.equals(sown)) continue;
                BlockState s = h.getBlockState(at);
                h.assertTrue(s.is(Blocks.FARMLAND) && Fertility.get(s) == Fertility.NEW_FIELD + 1,
                        "ploughed " + at + " is farmland at fertility 3, not " + s);
            }
            for (int x : new int[] {2, 8}) h.assertTrue(!h.getBlockState(new BlockPos(x, 0, z)).is(Blocks.FARMLAND), "the strip is 5 wide, x " + x + " untouched");
        }
        h.assertTrue(h.getBlockState(stone).is(Blocks.STONE), "stone isn't ploughed");
        h.assertTrue(h.getBlockState(covered).is(Blocks.DIRT) && h.getBlockState(covered.above()).is(Blocks.COBBLESTONE), "a covered block isn't ploughed");
        h.assertTrue(Fertility.get(h.getBlockState(sown)) == 0 && h.getBlockState(sown.above()).is(Blocks.WHEAT), "a sown field is left alone");

        // share up: drawn on, nothing more is tilled
        plough.grab(player);
        h.assertTrue(!plough.isShareDown(), "the grab key again raises the share");
        double from = plough.getZ();
        draw(plough, horse, 15);
        h.assertTrue(plough.getZ() - from > 4, "the plough followed the horse, moved " + (plough.getZ() - from));
        for (int z = 13; z <= 15; z++)
            for (int x = 3; x <= 7; x++)
                h.assertTrue(!h.getBlockState(new BlockPos(x, 0, z)).is(Blocks.FARMLAND), "a raised share tills nothing at " + x + "," + z);

        player.setShiftKeyDown(true);
        plough.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
        h.assertTrue(plough.harness() == null, "sneak + right-click unhitches");
        h.succeed();
    }

    /** Leads the horse 0.4 blocks forward each tick, ticking the plough after it. */
    private static void draw(PloughEntity plough, AbstractHorse horse, int ticks) {
        for (int i = 0; i < ticks; i++) {
            horse.setPos(horse.position().add(0, 0, 0.4));
            plough.tick();
        }
    }
}
