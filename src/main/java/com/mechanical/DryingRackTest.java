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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Game test for the Drying Rack: every {@link DryingRackBlockEntity#TABLE} row hangs a spot's worth, turns into its output
 * after exactly its seconds out in sun and wind (twice that sheltered) and comes off with an empty hand; hides and berries
 * still go into the blockstate and dry as before; a rack from before the block entity keeps what hangs on it; a spot with
 * goods is taken; breaking drops the goods. Also the recipes around it (Wet Adobe Brick, Adobe Bricks, Cut Peat, Reed Thatch)
 * and Dried Peat as kiln fuel.
 */
@EventBusSubscriber(modid = MODID)
public final class DryingRackTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> TEST = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "drying_rack"));

    static {
        TEST_FUNCTIONS.register("drying_rack", () -> DryingRackTest::test);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "drying_rack"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "drying_rack"),
                new FunctionGameTestInstance(TEST, new TestData<>(env, Identifier.withDefaultNamespace("empty"), 20, 0, true)));
    }

    private static BlockHitResult hit(BlockPos pos) {
        return new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false);
    }

    private static void use(ServerLevel level, BlockPos pos, Player player, ItemStack stack) {
        player.setItemInHand(InteractionHand.OFF_HAND, stack); // off hand: taken goods fill the main inventory, never this slot
        level.getBlockState(pos).useItemOn(stack, level, player, InteractionHand.OFF_HAND, hit(pos));
    }

    private static int count(Player player, Item item) {
        int n = 0;
        for (ItemStack s : player.getInventory()) if (s.is(item)) n += s.getCount();
        return n;
    }

    private static void test(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        for (int x = 0; x <= 4; x++) for (int z = 0; z <= 4; z++) {
            h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            for (int y = 1; y <= 3; y++) h.setBlock(new BlockPos(x, y, z), Blocks.AIR);
        }
        BlockPos pos = h.absolutePos(new BlockPos(1, 1, 1));
        BlockState empty = Mechanical.DRYING_RACK.get().defaultBlockState();

        // ---- every table row: a spot's worth goes up, dries in exactly its time, comes down with an empty hand
        for (DryingRackBlockEntity.Drying d : DryingRackBlockEntity.TABLE) {
            String name = d.input().get().toString();
            reset(level, pos, empty);
            Player player = h.makeMockPlayer(GameType.SURVIVAL);
            ItemStack stack = new ItemStack(d.input().get(), d.perSpot() + 2);
            use(level, pos, player, stack);
            h.assertTrue(stack.getCount() == 2, name + ": a spot takes " + d.perSpot());
            h.assertTrue(level.getBlockState(pos).getValue(DryingRackBlock.HIDE) == DryingRackBlock.EMPTY, name + ": the blockstate stays empty");
            DryingRackBlockEntity rack = (DryingRackBlockEntity) level.getBlockEntity(pos);
            h.assertTrue(rack != null && rack.item(0).is(d.input().get()) && rack.item(0).getCount() == d.perSpot(), name + " hangs in the block entity");
            use(level, pos, player, stack);
            h.assertTrue(stack.getCount() == 2, name + ": a single rack's one spot is full");
            for (int s = 1; s < d.seconds(); s++) rack.dry(2);
            h.assertTrue(rack.item(0).is(d.input().get()), name + " is not dry one second early");
            h.assertFalse(level.getBlockState(pos).useWithoutItem(level, player, hit(pos)).consumesAction(), name + ": nothing to take while drying");
            rack.dry(0); // night or rain
            h.assertTrue(rack.item(0).is(d.input().get()), name + ": night and rain pause");
            rack.dry(2);
            h.assertTrue(rack.item(0).is(d.output().get()) && rack.item(0).getCount() == d.perSpot(), name + " dries into " + d.output().get());
            level.getBlockState(pos).useWithoutItem(level, player, hit(pos));
            h.assertTrue(count(player, d.output().get()) == d.perSpot() && rack.item(0).isEmpty(), name + ": an empty hand takes the dried goods");
        }

        // ---- sheltered takes twice as long
        reset(level, pos, empty);
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        use(level, pos, player, new ItemStack(EarthBlocks.CUT_PEAT.get(), 4));
        DryingRackBlockEntity rack = (DryingRackBlockEntity) level.getBlockEntity(pos);
        for (int s = 1; s < 2 * 480; s++) rack.dry(1);
        h.assertTrue(rack.item(0).is(EarthBlocks.CUT_PEAT.get()), "sheltered peat is not dry after 2x its time minus a second");
        rack.dry(1);
        h.assertTrue(rack.item(0).is(EarthBlocks.DRIED_PEAT.get()), "sheltered peat is dry after 2x its time");

        // ---- goods block the spot for a hide, and drop when the rack breaks
        reset(level, pos, empty);
        use(level, pos, player, new ItemStack(EarthBlocks.REED.get(), 3));
        ItemStack hide = new ItemStack(Mechanical.SCRAPED_HIDE.get());
        use(level, pos, player, hide);
        h.assertTrue(hide.getCount() == 1 && level.getBlockState(pos).getValue(DryingRackBlock.HIDE) == DryingRackBlock.EMPTY, "a spot with reed takes no hide");
        level.destroyBlock(pos, true);
        h.assertTrue(level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2), e -> e.getItem().is(EarthBlocks.REED.get().asItem()) && e.getItem().getCount() == 3).size() == 1,
                "a broken rack drops the 3 reed");
        level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).forEach(ItemEntity::discard);

        // ---- the old kinds still live in the blockstate and dry as before
        reset(level, pos, empty);
        hide = new ItemStack(Mechanical.SCRAPED_HIDE.get());
        use(level, pos, player, hide);
        h.assertTrue(level.getBlockState(pos).getValue(DryingRackBlock.HIDE) == DryingRackBlock.HIDE_WET && DryingRackBlockEntity.empty(level, pos),
                "a Scraped Hide still goes into the blockstate");
        for (int i = 0; i < 300 && level.getBlockState(pos).getValue(DryingRackBlock.HIDE) == DryingRackBlock.HIDE_WET; i++)
            level.getBlockState(pos).randomTick(level, pos, level.getRandom());
        h.assertTrue(level.getBlockState(pos).getValue(DryingRackBlock.HIDE) == 2, "the hide dries by random ticks as before");
        level.getBlockState(pos).useWithoutItem(level, player, hit(pos));
        h.assertTrue(count(player, Items.LEATHER) == 1, "Leather off the rack");
        ItemStack berries = new ItemStack(Items.SWEET_BERRIES, 5);
        use(level, pos, player, berries);
        h.assertTrue(berries.getCount() == 1 && level.getBlockState(pos).getValue(DryingRackBlock.HIDE) == DryingRackBlock.BERRIES, "4 berries go into the blockstate");
        level.setBlock(pos, level.getBlockState(pos).setValue(DryingRackBlock.HIDE, DryingRackBlock.BERRIES + 1), 3);
        level.getBlockState(pos).useWithoutItem(level, player, hit(pos));
        h.assertTrue(count(player, Preserving.DRIED_BERRIES.get()) == DryingRackBlock.HANDFUL, "4 Dried Berries off the rack");

        // ---- a rack from before the block entity keeps its content
        level.setBlock(pos, empty.setValue(DryingRackBlock.HIDE, 2), 3);
        level.removeBlockEntity(pos);
        h.assertTrue(level.getBlockEntity(pos) instanceof DryingRackBlockEntity, "an old rack gets an empty block entity when touched");
        h.assertTrue(level.getBlockState(pos).getValue(DryingRackBlock.HIDE) == 2, "and keeps its leather");
        level.getBlockState(pos).useWithoutItem(level, player, hit(pos));
        h.assertTrue(count(player, Items.LEATHER) == 2, "which comes off as before (leather " + count(player, Items.LEATHER) + ", hide " + level.getBlockState(pos).getValue(DryingRackBlock.HIDE) + ")");

        // ---- recipes and fuel
        h.assertTrue(craft(h, level, List.of(new ItemStack(Items.CLAY_BALL), new ItemStack(Items.SAND), new ItemStack(Farming.STRAW.get())), 3, 1)
                .is(EarthBlocks.WET_ADOBE_BRICK.get()), "Clay Ball + Sand + Straw make Wet Adobe Bricks");
        ItemStack adobe = craft(h, level, List.of(new ItemStack(EarthBlocks.ADOBE_BRICK.get()), new ItemStack(EarthBlocks.ADOBE_BRICK.get()),
                new ItemStack(EarthBlocks.ADOBE_BRICK.get()), new ItemStack(EarthBlocks.ADOBE_BRICK.get())), 2, 2);
        h.assertTrue(adobe.getItem() == StoneFamilies.block("adobe_bricks").asItem() && adobe.getCount() == 4, "4 Adobe Brick make 4 Adobe Bricks");
        ItemStack cut = craft(h, level, List.of(new ItemStack(EarthBlocks.PEAT.get())), 1, 1);
        h.assertTrue(cut.is(EarthBlocks.CUT_PEAT.get()) && cut.getCount() == 4, "a Peat block cuts into 4 Cut Peat");
        ItemStack dried = new ItemStack(EarthBlocks.DRIED_REED.get());
        h.assertTrue(craft(h, level, List.of(dried, dried, dried, dried), 2, 2).is(EarthBlocks.REED_THATCH.get().asItem()), "4 Dried Reed make Reed Thatch");
        ItemStack fresh = new ItemStack(EarthBlocks.REED.get());
        h.assertTrue(level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(2, 2, List.of(fresh, fresh, fresh, fresh)), level).isEmpty(),
                "fresh Reed makes no thatch");
        h.assertTrue(KilnBlockEntity.FUELS.stream().anyMatch(f -> f.item().test(new ItemStack(EarthBlocks.DRIED_PEAT.get())) && f.ticks() * 4 > 3600),
                "Dried Peat burns in the kiln, 4 of them longer than the Peat block");
        h.assertTrue(EarthBlocks.peatCampfireTicks(new ItemStack(EarthBlocks.DRIED_PEAT.get())) * 4 > EarthBlocks.PEAT_CAMPFIRE_TICKS, "and on a campfire");
        h.succeed();
    }

    /** A fresh rack (setting the same state again would keep the old block entity). */
    private static void reset(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(pos, state, 3);
    }

    private static ItemStack craft(GameTestHelper h, ServerLevel level, List<ItemStack> items, int w, int hgt) {
        CraftingInput in = CraftingInput.of(w, hgt, items);
        return level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, in, level)
                .orElseThrow(() -> h.assertionException("no recipe for " + items)).value().assemble(in);
    }

    private DryingRackTest() {}
}
