package com.mechanical.wildlife.river;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.function.Consumer;

import com.mechanical.TestArea;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * A10: a beaver with building stuff in its mouth packs a Beaver Dam block into a narrow stream by its home, and a Beaver
 * Dam breaks into Sticks and clay (the mud).
 */
@EventBusSubscriber(modid = MODID)
public final class BeaverTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> TEST = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "beaver_dam"));

    static {
        TEST_FUNCTIONS.register("beaver_dam", () -> BeaverTest::test);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "beaver_dam"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "beaver_dam"), new FunctionGameTestInstance(TEST, TestArea.of(env, 800, true)));
    }

    private static void test(GameTestHelper h) {
        var level = h.getLevel();
        // stone ground with a stream 2 wide running north-south (x 5..6)
        for (int x = 0; x <= 12; x++) {
            for (int z = 0; z <= 12; z++) {
                h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                h.setBlock(new BlockPos(x, 1, z), x == 5 || x == 6 ? Blocks.WATER : Blocks.STONE);
            }
        }
        List<ItemStack> drops = Block.getDrops(RiverFauna.BEAVER_DAM.get().defaultBlockState(), level, h.absolutePos(new BlockPos(0, 2, 0)), null);
        h.assertTrue(drops.stream().anyMatch(s -> s.is(Items.STICK)), "a Beaver Dam drops sticks, got " + drops);
        h.assertTrue(drops.stream().allMatch(s -> s.is(Items.STICK) || s.is(Items.CLAY_BALL)), "and clay, nothing else, got " + drops);

        Beaver b = h.spawn(RiverFauna.BEAVER.get(), new BlockPos(6, 1, 8)); // in the water: it won't doze off there
        b.setHomeTo(h.absolutePos(new BlockPos(5, 1, 6)), Beaver.HOME_RADIUS);
        b.material = 6;
        h.succeedWhen(() -> {
            boolean dam = false;
            for (int z = 0; z <= 12 && !dam; z++) dam = h.getBlockState(new BlockPos(5, 1, z)).is(RiverFauna.BEAVER_DAM.get()) || h.getBlockState(new BlockPos(6, 1, z)).is(RiverFauna.BEAVER_DAM.get());
            h.assertTrue(dam, "the beaver packs a dam block into the stream");
        });
    }

    private BeaverTest() {}
}
