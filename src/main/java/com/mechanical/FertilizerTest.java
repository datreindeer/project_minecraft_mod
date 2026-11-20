package com.mechanical;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Game test, through the right-click path (event, then Item.useOn): fertiliser used on a crop feeds the
 * farmland under it, and an empty hand on a ripe crop over Crop Stakes harvests and replants it.
 */
@EventBusSubscriber(modid = MODID)
public final class FertilizerTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> TEST = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "fertilizer"));

    static {
        TEST_FUNCTIONS.register("fertilizer", () -> FertilizerTest::test);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "fertilizer"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "fertilizer"),
                new FunctionGameTestInstance(TEST, new TestData<>(env, Identifier.withDefaultNamespace("empty"), 20, 0, true)));
    }

    /** What ServerPlayerGameMode.useItemOn does, minus the network: the right-click event, then the item's useOn. */
    private static void click(GameTestHelper h, Player p, BlockPos rel, ItemStack held) {
        p.setItemInHand(InteractionHand.MAIN_HAND, held);
        BlockPos abs = h.absolutePos(rel);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
        if (NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(p, InteractionHand.MAIN_HAND, abs, hit)).isCanceled()) return;
        if (!held.isEmpty()) held.useOn(new UseOnContext(p, InteractionHand.MAIN_HAND, hit));
    }

    private static void test(GameTestHelper h) {
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        BlockState farmland = Blocks.FARMLAND.defaultBlockState().setValue(Fertility.FERTILITY, 0);
        CropBlock wheat = (CropBlock) Blocks.WHEAT;

        // compost on the young crop raises the farmland under it 0 -> 1
        h.setBlock(new BlockPos(1, 1, 1), farmland);
        h.setBlock(new BlockPos(1, 2, 1), wheat.getStateForAge(3));
        ItemStack compost = new ItemStack(Farming.COMPOST.get(), 4);
        click(h, p, new BlockPos(1, 2, 1), compost);
        int got = Fertility.get(h.getBlockState(new BlockPos(1, 1, 1)));
        if (got != 1) throw h.assertionException("compost on a crop: farmland fertility " + got + ", expected 1");
        if (compost.getCount() != 3) throw h.assertionException("compost not used up: " + compost.getCount());

        // rich soil (3/3): the compost stays in hand
        h.setBlock(new BlockPos(2, 1, 1), farmland.setValue(Fertility.FERTILITY, Fertility.MAX));
        h.setBlock(new BlockPos(2, 2, 1), wheat.getStateForAge(3));
        click(h, p, new BlockPos(2, 2, 1), compost);
        if (compost.getCount() != 3) throw h.assertionException("compost used on rich soil: " + compost.getCount());

        // empty hand on a ripe crop over staked farmland: replanted at age 0
        h.setBlock(new BlockPos(3, 1, 1), farmland.setValue(Fertility.FERTILITY, 2).setValue(Fertility.STAKED, true));
        h.setBlock(new BlockPos(3, 2, 1), wheat.getStateForAge(wheat.getMaxAge()));
        click(h, p, new BlockPos(3, 2, 1), ItemStack.EMPTY);
        BlockState after = h.getBlockState(new BlockPos(3, 2, 1));
        if (!after.is(Blocks.WHEAT) || wheat.getAge(after) != 0) throw h.assertionException("staked harvest left " + after);
        h.succeed();
    }

    private FertilizerTest() {}
}
