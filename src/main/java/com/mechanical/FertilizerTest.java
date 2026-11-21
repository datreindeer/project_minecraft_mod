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
import net.minecraft.world.item.Items;
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

        // soot on bare farmland: +1 as well
        h.setBlock(new BlockPos(6, 1, 1), farmland);
        ItemStack soot = new ItemStack(Mechanical.SOOT_ITEM.get(), 2);
        click(h, p, new BlockPos(6, 1, 1), soot);
        int sooted = Fertility.get(h.getBlockState(new BlockPos(6, 1, 1)));
        if (sooted != 1 || soot.getCount() != 1) throw h.assertionException("soot on farmland: fertility " + sooted + ", soot left " + soot.getCount());

        // writing (gen_writing.py): Paper needs a metal knife, which stays; Book and Quill takes Soot Ink
        var level = h.getLevel();
        var cane = new ItemStack(Items.SUGAR_CANE);
        var paperIn = net.minecraft.world.item.crafting.CraftingInput.of(2, 2, java.util.List.of(cane, cane.copy(), cane.copy(), new ItemStack(Bronze.BRONZE_KNIFE.get())));
        var paper = level.recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, paperIn, level)
                .orElseThrow(() -> h.assertionException("no Paper from Sugar Cane and a Bronze Knife"));
        if (!paper.value().assemble(paperIn).is(Items.PAPER) || !paper.value().getRemainingItems(paperIn).get(3).is(Bronze.BRONZE_KNIFE.get()))
            throw h.assertionException("Paper, and the knife stays in the grid");
        var noKnife = net.minecraft.world.item.crafting.CraftingInput.of(3, 1, java.util.List.of(cane, cane.copy(), cane.copy()));
        if (level.recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, noKnife, level).isPresent())
            throw h.assertionException("Paper without a knife");
        var bookIn = net.minecraft.world.item.crafting.CraftingInput.of(3, 1, java.util.List.of(new ItemStack(Items.BOOK),
                new ItemStack(Mechanical.SOOT_INK.get()), new ItemStack(Items.FEATHER)));
        if (level.recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, bookIn, level)
                .map(r -> r.value().assemble(bookIn)).filter(s -> s.is(Items.WRITABLE_BOOK)).isEmpty())
            throw h.assertionException("no Book and Quill from Book + Soot Ink + Feather");

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
        // a sickle on a staked ripe crop: replanted too, and the sickle wears 1
        h.setBlock(new BlockPos(4, 1, 1), farmland.setValue(Fertility.FERTILITY, 2).setValue(Fertility.STAKED, true));
        h.setBlock(new BlockPos(4, 2, 1), wheat.getStateForAge(wheat.getMaxAge()));
        ItemStack sickle = new ItemStack(Farming.FLINT_SICKLE.get());
        click(h, p, new BlockPos(4, 2, 1), sickle);
        if (wheat.getAge(h.getBlockState(new BlockPos(4, 2, 1))) != 0) throw h.assertionException("sickle staked harvest left " + h.getBlockState(new BlockPos(4, 2, 1)));
        if (sickle.getDamageValue() != 1) throw h.assertionException("sickle wear " + sickle.getDamageValue() + ", expected 1");
        h.succeed();
    }

    private FertilizerTest() {}
}
