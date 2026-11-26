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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.PortalProcessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Electrum ({@code electrum}: the 1:1 alloy, pure Gold and Silver stay pure on their own, its jewelry), the Magma Block
 * ({@code magma_heat}: warmth and snow; {@code magma_stations}: Baking Stone, Clay Kiln, Smelting Furnace, Crushed Magma
 * and the High Furnace) and the closed Nether and End ({@code dimension_lock}).
 */
@EventBusSubscriber(modid = MODID)
public final class MagmaTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final String[] TESTS = {"electrum", "magma_heat", "magma_stations", "dimension_lock"};

    static {
        TEST_FUNCTIONS.register("electrum", () -> MagmaTest::electrum);
        TEST_FUNCTIONS.register("magma_heat", () -> MagmaTest::heat);
        TEST_FUNCTIONS.register("magma_stations", () -> MagmaTest::stations);
        TEST_FUNCTIONS.register("dimension_lock", () -> MagmaTest::lock);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "magma"), new TestEnvironmentDefinition.AllOf(List.of()));
        for (String t : TESTS)
            event.registerTest(Identifier.fromNamespaceAndPath(MODID, t), new FunctionGameTestInstance(
                    ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, t)), TestArea.of(env, 100, true)));
    }

    private static void floor(GameTestHelper h) {
        for (int x = 0; x <= 16; x++) for (int z = 0; z <= 16; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
    }

    private static ItemStack craft(ServerLevel level, int w, ItemStack... items) {
        CraftingInput in = CraftingInput.of(w, items.length / w, List.of(items));
        return level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, in, level).map(r -> r.value().assemble(in)).orElse(ItemStack.EMPTY);
    }

    private static void electrum(GameTestHelper h) {
        floor(h);
        ServerLevel level = h.getLevel();
        MeltPool p = new MeltPool(8000);
        p.add(Molten.GOLD, 600);
        for (int t = 0; t < 100; t++) p.mix(1800);
        h.assertTrue(p.get(Molten.GOLD) == 600 && p.get(Molten.ELECTRUM) == 0, "gold alone stays pure gold: " + p.layers());
        p.add(Molten.SILVER, 400);
        for (int t = 0; t < 100; t++) p.mix(1099);
        h.assertTrue(p.get(Molten.ELECTRUM) == 0, "no electrum below 1100°");
        for (int t = 0; t < 100; t++) p.mix(1100);
        h.assertTrue(p.get(Molten.ELECTRUM) == 800 && p.get(Molten.GOLD) == 200 && p.get(Molten.SILVER) == 0 && p.total() == 1000,
                "1:1 electrum, the gold left over stays gold: " + p.layers());
        MeltPool s = new MeltPool(8000);
        s.add(Molten.SILVER, 600);
        for (int t = 0; t < 100; t++) s.mix(1800);
        h.assertTrue(s.get(Molten.SILVER) == 600 && s.total() == 600, "silver alone stays pure silver");
        List<Molten> order = Molten.values();
        h.assertTrue(order.indexOf(Molten.GOLD) < order.indexOf(Molten.ELECTRUM) && order.indexOf(Molten.ELECTRUM) < order.indexOf(Molten.SILVER),
                "it lies between gold and silver: " + order);

        // ingot, nuggets and block both ways, and it remelts as electrum
        ItemStack ingot = new ItemStack(IronOres.ELECTRUM_INGOT.get());
        ItemStack[] nine = new ItemStack[9];
        java.util.Arrays.fill(nine, new ItemStack(IronOres.ELECTRUM_NUGGET.get()));
        h.assertTrue(craft(level, 3, nine).is(IronOres.ELECTRUM_INGOT.get()) && craft(level, 1, ingot).getCount() == 9, "9 nuggets <-> 1 ingot");
        java.util.Arrays.fill(nine, ingot);
        h.assertTrue(craft(level, 3, nine).is(HighFurnace.ELECTRUM_BLOCK_ITEM.get())
                && craft(level, 1, new ItemStack(HighFurnace.ELECTRUM_BLOCK_ITEM.get())).getCount() == 9, "9 ingots <-> 1 block");
        h.assertTrue(HighFurnaceMelting.of(ingot).molten() == Molten.ELECTRUM && FoundryBasinBlock.block(Molten.ELECTRUM) == HighFurnace.ELECTRUM_BLOCK_ITEM.get()
                && Coins.minted(ingot).isEmpty() && !Molten.ELECTRUM.castsAnything(), "remelts, basin block, no coins, no tools");

        // jewelry: the casting table needs electrum's heat, and the gem's bonus lies between silver's and gold's
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        BlockState kiln = Mechanical.KILN.get().defaultBlockState();
        h.setBlock(new BlockPos(2, 1, 3), kiln.setValue(KilnBlock.HALF, DoubleBlockHalf.LOWER));
        h.setBlock(new BlockPos(2, 2, 3), kiln.setValue(KilnBlock.HALF, DoubleBlockHalf.UPPER));
        BlockPos tablePos = h.absolutePos(new BlockPos(3, 1, 3));
        h.setBlock(new BlockPos(3, 1, 3), Jewelry.STEEL_CASTING_TABLE.get().defaultBlockState());
        KilnBlockEntity k = (KilnBlockEntity) level.getBlockEntity(h.absolutePos(new BlockPos(2, 1, 3)));
        JewelryTableBlockEntity table = (JewelryTableBlockEntity) level.getBlockEntity(tablePos);
        table.use(level, player, InteractionHand.MAIN_HAND, new ItemStack(IronOres.ELECTRUM_INGOT.get()));
        h.assertTrue(table.castTemp() == 1100, "electrum melts at 1100°");
        for (int t = 0; t < Jewelry.CAST_TICKS + 5; t++) {
            k.setTemperatureForTest(1050);
            table.serverTick(level, tablePos);
        }
        h.assertTrue(table.items().get(1).isEmpty(), "1050° is too cold");
        for (int t = 0; t < Jewelry.CAST_TICKS; t++) {
            k.setTemperatureForTest(1150);
            table.serverTick(level, tablePos);
        }
        h.assertTrue(table.items().get(1).is(Jewelry.ELECTRUM_RING_BLANK.get()) && table.items().get(1).getCount() == 2, "two Electrum Ring Blanks");
        h.assertTrue(Jewelry.jewelFor(new ItemStack(Jewelry.ELECTRUM_RING_BLANK.get())) == Jewelry.ELECTRUM_RING.get()
                && Jewelry.jewelFor(new ItemStack(Jewelry.ELECTRUM_CHAIN.get())) == Jewelry.ELECTRUM_NECKLACE.get(), "the bench sets electrum blanks");
        ItemStack necklace = Jewelry.make(Jewelry.ELECTRUM_NECKLACE.get(), new Jewelry.Jewel("garnet", true));
        h.assertTrue(necklace.is(com.mechanical.equipment.Accessory.NECK.tag)
                && Jewelry.make(Jewelry.ELECTRUM_RING.get(), new Jewelry.Jewel("ruby", true)).is(com.mechanical.equipment.Accessory.RING_1.tag), "worn in the slots");
        h.assertTrue(Temperature.insulation(necklace) == (float) (Jewelry.WARM_FLAWLESS * 0.75)
                && Jewelry.SILVER_STRENGTH < Jewelry.strength(Jewelry.ELECTRUM_RING.get()) && Jewelry.strength(Jewelry.ELECTRUM_RING.get()) < 1, "0.75 of gold's bonus");
        h.succeed();
    }

    private static void heat(GameTestHelper h) {
        floor(h);
        ServerLevel level = h.getLevel();
        BlockPos magma = new BlockPos(8, 1, 8);
        BlockPos beside = h.absolutePos(magma.east());
        h.assertTrue(Magma.warmth(level, beside) == 0 && !Magma.meltsSnow(level, beside), "no magma, no warmth");
        BlockPos snow = h.absolutePos(magma.east(2));
        h.assertTrue(Thaw.pile(level, snow, 3), "a storm lays snow without magma");
        level.removeBlock(snow, false);
        h.setBlock(magma, Blocks.MAGMA_BLOCK);
        float near = Magma.warmth(level, beside), far = Magma.warmth(level, h.absolutePos(magma.east(4)));
        h.assertTrue(near > 6 && near < 10 && far > 0 && far < near && Magma.warmth(level, h.absolutePos(magma.east(6))) == 0,
                "warmth falls off with distance (weaker than a campfire's 10): " + near + ", " + far);
        h.assertTrue(!Thaw.pile(level, snow, 3) && level.getBlockState(snow).isAir(), "no snow piles by magma");
        h.assertTrue(Thaw.pile(level, h.absolutePos(magma.east(4)), 3), "4 blocks off the snow lies");
        h.succeed();
    }

    private static void stations(GameTestHelper h) {
        floor(h);
        ServerLevel level = h.getLevel();
        // Baking Stone: bakes on magma at half speed, at full speed by a fire
        BlockPos stone = new BlockPos(2, 2, 2), abs = h.absolutePos(stone);
        h.setBlock(stone.below(), Blocks.MAGMA_BLOCK);
        BlockState dough = Farming.BAKING_STONE.get().defaultBlockState().setValue(BakingStoneBlock.CONTENTS, BakingStoneBlock.Contents.DOUGH);
        h.setBlock(stone, dough);
        h.assertTrue(BakingStoneBlock.heated(level, abs) && BakingStoneBlock.slowness(level, abs) == 2, "magma heats it, at half speed");
        level.getBlockState(abs).tick(level, abs, level.getRandom());
        h.assertTrue(level.getBlockState(abs).getValue(BakingStoneBlock.CONTENTS) == BakingStoneBlock.Contents.BREAD, "bread on magma");
        h.setBlock(stone.east(), Blocks.CAMPFIRE);
        h.assertTrue(BakingStoneBlock.slowness(level, abs) == 1, "a campfire beside it is full speed");
        h.setBlock(stone.east(), Blocks.AIR);

        // Clay Kiln: never lit, no fuel burnt, 600°: bakes bricks, never melts gold
        BlockPos kilnPos = new BlockPos(6, 2, 2), kilnAbs = h.absolutePos(kilnPos);
        h.setBlock(kilnPos.below(), Blocks.MAGMA_BLOCK);
        BlockState kiln = Mechanical.KILN.get().defaultBlockState();
        h.setBlock(kilnPos, kiln.setValue(KilnBlock.HALF, DoubleBlockHalf.LOWER));
        h.setBlock(kilnPos.above(), kiln.setValue(KilnBlock.HALF, DoubleBlockHalf.UPPER));
        KilnBlockEntity k = (KilnBlockEntity) level.getBlockEntity(kilnAbs);
        k.setItem(KilnBlockEntity.INPUT, new ItemStack(Items.CLAY_BALL));
        k.setItem(KilnBlockEntity.FUEL, new ItemStack(Items.CHARCOAL, 3));
        k.setTemperatureForTest(Magma.HEAT - 1);
        for (int t = 0; t < 1000; t++) KilnBlockEntity.serverTick(level, kilnAbs, level.getBlockState(kilnAbs), k);
        h.assertTrue(k.getItem(KilnBlockEntity.OUTPUT).is(Items.BRICK) && k.getItem(KilnBlockEntity.FUEL).getCount() == 3 && !k.isLit(),
                "a brick without fire or fuel: " + k.getItem(KilnBlockEntity.OUTPUT) + ", " + k.temperature() + "°");
        h.assertTrue(k.temperature() == Magma.HEAT, "magma holds exactly 600°, got " + k.temperature());
        h.assertTrue(!k.pump(level), "no bellows without a fire");
        k.setItem(KilnBlockEntity.OUTPUT, ItemStack.EMPTY);
        k.setItem(KilnBlockEntity.INPUT, new ItemStack(Items.RAW_GOLD));
        for (int t = 0; t < 1000; t++) KilnBlockEntity.serverTick(level, kilnAbs, level.getBlockState(kilnAbs), k);
        h.assertTrue(k.getItem(KilnBlockEntity.OUTPUT).isEmpty(), "no gold melts on magma");

        // Smelting Furnace: magma under the middle of its floor
        BlockPos c = new BlockPos(11, 2, 11);
        h.setBlock(c.below(), Blocks.MAGMA_BLOCK);
        for (int dx = -1; dx <= 1; dx++) for (int dy = 0; dy <= 2; dy++) for (int dz = -1; dz <= 1; dz++)
            h.setBlock(c.offset(dx, dy, dz), dx == 0 && dz == 0 && dy > 0 ? Blocks.AIR : Smelting.FURNACE_BRICKS.get());
        h.setBlock(c.above(2), Smelting.FURNACE_CHUTE.get());
        BlockPos tapPos = c.north();
        h.setBlock(tapPos, Smelting.FURNACE_TAP.get().defaultBlockState().setValue(FurnaceTapBlock.FACING, Direction.NORTH));
        FurnaceTapBlockEntity tap = (FurnaceTapBlockEntity) level.getBlockEntity(h.absolutePos(tapPos));
        for (int t = 0; t < 130 * 20; t++) tap.serverTick(level, level.getBlockState(h.absolutePos(tapPos)));
        h.assertTrue(tap.problem() == null && Math.abs(tap.temperature() - Magma.HEAT) < 1, "the furnace sits at 600°: " + tap.temperature() + " " + tap.problem());

        // Crushed Magma: from the Stone Anvil, into the two refractory recipes
        ItemStack crushed = BronzeOres.crushed(new ItemStack(Items.MAGMA_BLOCK));
        h.assertTrue(crushed.is(Magma.CRUSHED_MAGMA.get()) && crushed.getCount() == 4, "a Magma Block crushes into 4");
        ItemStack grit = new ItemStack(Magma.CRUSHED_MAGMA.get()), clay = new ItemStack(Items.CLAY_BALL),
                quartzite = new ItemStack(HighFurnace.CRUSHED_QUARTZITE.get()), fireClay = new ItemStack(Ironworking.FIRE_CLAY.get());
        ItemStack made = craft(level, 2, clay, clay, grit, grit), brick = craft(level, 2, quartzite, quartzite, fireClay, grit);
        h.assertTrue(made.is(Ironworking.FIRE_CLAY.get()) && made.getCount() == 4, "Fire Clay: " + made);
        h.assertTrue(brick.is(HighFurnace.UNFIRED_SILICA_BRICK.get()) && brick.getCount() == 4, "Unfired Silica Bricks: " + brick);

        // High Furnace: a handful lights the fuelled fireboxes every time, and is kept when they all burn
        HighFurnaceTest.Built built = HighFurnaceTest.build(h, 2, 8, 2, 2, 5, 1);
        HighFurnaceBlockEntity f = (HighFurnaceBlockEntity) level.getBlockEntity(h.absolutePos(built.front()));
        f.check(level);
        BlockPos box = h.absolutePos(built.fireboxes().get(0));
        f.addFuel(box, new ItemStack(Items.CHARCOAL, 8));
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        ItemStack held = new ItemStack(Magma.CRUSHED_MAGMA.get(), 3);
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(box), Direction.EAST, box, false);
        level.getBlockState(box).useItemOn(held, level, player, InteractionHand.MAIN_HAND, hit);
        h.assertTrue(f.lit() && held.getCount() == 2, "lit, one used up: " + held);
        level.getBlockState(box).useItemOn(held, level, player, InteractionHand.MAIN_HAND, hit);
        h.assertTrue(held.getCount() == 2, "nothing left to light: none used");
        h.succeed();
    }

    private static void lock(GameTestHelper h) {
        floor(h);
        ServerLevel level = h.getLevel();
        h.assertTrue(DimensionLock.closed(), "closed by default");
        // the rule: into the Nether and the End no, home yes
        h.assertTrue(DimensionLock.blocks(Level.OVERWORLD, Level.NETHER) && DimensionLock.blocks(Level.OVERWORLD, Level.END)
                && DimensionLock.blocks(Level.END, Level.NETHER), "no way in");
        h.assertTrue(!DimensionLock.blocks(Level.NETHER, Level.OVERWORLD) && !DimensionLock.blocks(Level.END, Level.OVERWORLD)
                && !DimensionLock.blocks(Level.NETHER, Level.NETHER) && !DimensionLock.blocks(Level.END, Level.END), "the way home and moving about are open");
        Entity pig = h.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(2, 1, 2));
        h.assertTrue(!CommonHooks.onTravelToDimension(pig, Level.NETHER) && !CommonHooks.onTravelToDimension(pig, Level.END)
                && CommonHooks.onTravelToDimension(pig, Level.OVERWORLD), "the travel event");
        // portals give no destination (so no exit portal is built over there)
        BlockPos at = h.absolutePos(new BlockPos(2, 1, 2));
        for (var portal : new net.minecraft.world.level.block.Block[] {Blocks.NETHER_PORTAL, Blocks.END_PORTAL})
            h.assertTrue(new PortalProcessor((Portal) portal, at).getPortalDestination(level, pig) == null, portal + " leads nowhere");

        // a frame lit with fire stays fire; with the switch off it becomes a portal
        h.assertTrue(!lightFrame(h), "no portal in a closed world");
        ServerConfig.NETHER_AND_END_CLOSED.set(false);
        try {
            h.assertTrue(lightFrame(h), "the switch opens it again");
            h.assertTrue(CommonHooks.onTravelToDimension(pig, Level.NETHER), "and travel");
        } finally {
            ServerConfig.NETHER_AND_END_CLOSED.set(true);
        }

        // no Eye of Ender into an End Portal Frame
        BlockPos frame = new BlockPos(13, 1, 13), frameAbs = h.absolutePos(frame);
        h.setBlock(frame, Blocks.END_PORTAL_FRAME);
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.ENDER_EYE));
        var event = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, frameAbs,
                new BlockHitResult(Vec3.atCenterOf(frameAbs), Direction.UP, frameAbs, false));
        NeoForge.EVENT_BUS.post(event);
        h.assertTrue(event.isCanceled(), "the eye stays out");
        h.succeed();
    }

    /** Builds an obsidian frame, puts fire in it and says whether a Nether Portal came of it; then clears it away. */
    private static boolean lightFrame(GameTestHelper h) {
        for (int x = 6; x <= 9; x++) for (int y = 1; y <= 5; y++) h.setBlock(new BlockPos(x, y, 6), x == 6 || x == 9 || y == 1 || y == 5 ? Blocks.OBSIDIAN : Blocks.AIR);
        h.setBlock(new BlockPos(7, 2, 6), Blocks.FIRE);
        boolean portal = h.getBlockState(new BlockPos(7, 2, 6)).is(Blocks.NETHER_PORTAL);
        for (int x = 6; x <= 9; x++) for (int y = 1; y <= 5; y++) h.setBlock(new BlockPos(x, y, 6), Blocks.AIR);
        return portal;
    }

    private MagmaTest() {}
}
