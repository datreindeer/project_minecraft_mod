package com.mechanical;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.function.Consumer;

import com.mechanical.equipment.Accessory;
import com.mechanical.equipment.Equipment;
import com.mechanical.equipment.PackMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Game tests for the smithing and fire pit phase (docs/living-world-plan.md F12, F9, F10): p8_big_fire_pit (a Steel Hearth
 * Ring can't grow a hemmed-in pit and names what is in the way, grows toward the one free side, moving the food and
 * firewood to the new corner), p8_roast (an unskinned carcass is refused, a skinned cow roasts into 2x its most meat,
 * cooked), p8_satchel (Tool Loops sew three tool-only slots on a satchel, shift-click puts a knife there; the 5x5 pack),
 * p8_jewelry (the casting table needs a hot kiln beside it, casts ring blanks; the Jeweler's Bench sets a Flawless Ruby;
 * worn, it adds 4 health; a garnet keeps out the cold).
 */
@EventBusSubscriber(modid = MODID)
public final class P8SmithTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final String[] TESTS = {"p8_big_fire_pit", "p8_roast", "p8_satchel", "p8_jewelry"};

    static {
        TEST_FUNCTIONS.register("p8_big_fire_pit", () -> P8SmithTest::bigFirePit);
        TEST_FUNCTIONS.register("p8_roast", () -> P8SmithTest::roast);
        TEST_FUNCTIONS.register("p8_satchel", () -> P8SmithTest::satchel);
        TEST_FUNCTIONS.register("p8_jewelry", () -> P8SmithTest::jewelry);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "p8smith"), new TestEnvironmentDefinition.AllOf(List.of()));
        for (String t : TESTS) {
            event.registerTest(Identifier.fromNamespaceAndPath(MODID, t), new FunctionGameTestInstance(
                    ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, t)), TestArea.of(env, 100, true)));
        }
    }

    private static void floor(GameTestHelper h) {
        for (int x = 0; x <= 12; x++) for (int z = 0; z <= 12; z++) {
            h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            for (int y = 1; y <= 3; y++) h.setBlock(new BlockPos(x, y, z), Blocks.AIR);
        }
    }

    /** A small Fire Pit with its north-west corner at {@code at}. */
    private static void smallPit(GameTestHelper h, BlockPos at) {
        BlockState s = FirePit.FIRE_PIT.get().defaultBlockState();
        for (int part = 0; part < 4; part++) h.setBlock(at.offset(part & 1, 0, part >> 1), s.setValue(FirePitBlock.PART, part));
    }

    private static void bigFirePit(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        floor(h);
        BlockPos c = new BlockPos(3, 1, 3);
        smallPit(h, c);
        // hemmed in: each of the four 3x3s round the 2x2 takes one of these corners
        for (BlockPos p : List.of(new BlockPos(2, 1, 2), new BlockPos(5, 1, 2), new BlockPos(2, 1, 5), new BlockPos(5, 1, 5))) h.setBlock(p, Blocks.COBBLESTONE);
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(9.5, 1, 9.5)));
        BlockPos ac = h.absolutePos(c);
        h.assertFalse(FirePitBlock.upgrade(level, ac, player), "a hemmed-in pit can't grow");
        h.assertTrue(FirePitBlock.blockers(level, h.absolutePos(new BlockPos(2, 1, 2)), ac).contains(h.absolutePos(new BlockPos(2, 1, 2))),
                "the cobblestone is named as in the way");
        h.assertFalse(FirePit.isBig(level.getBlockState(ac)), "still small after a blocked try");
        // food and firewood on it, then free the north-west side only: it grows that way and the corner moves
        FirePitBlockEntity pit = FirePit.entity(level, ac, level.getBlockState(ac));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BEEF, 2));
        h.assertTrue(pit.placeFood(level, player, player.getMainHandItem()), "beef on the spit");
        pit.feed(level, player, new ItemStack(Mechanical.FIREWOOD.values().iterator().next().get()));
        h.setBlock(new BlockPos(2, 1, 2), Blocks.AIR);
        h.assertTrue(FirePitBlock.upgrade(level, ac, player), "grows when one side is free");
        BlockPos nc = h.absolutePos(new BlockPos(2, 1, 2));
        for (int x = 2; x <= 4; x++) for (int z = 2; z <= 4; z++)
            h.assertTrue(FirePit.isBig(level.getBlockState(h.absolutePos(new BlockPos(x, 1, z)))), "big pit part at " + x + "," + z);
        h.assertTrue(level.getBlockState(nc).getValue(FirePitBlock.PART) == 0 && FirePitBlock.controller(level.getBlockState(ac), ac).equals(nc),
                "the north-west corner is the new controller");
        h.assertTrue(level.getBlockEntity(ac) == null, "no block entity left at the old corner");
        FirePitBlockEntity big = FirePit.entity(level, ac, level.getBlockState(ac));
        h.assertTrue(big != null && big.getBlockPos().equals(nc) && big.items().get(0).is(Items.BEEF) && big.stock() == 1, "food and firewood moved along");
        h.assertTrue(big.stockMax() == FirePit.BIG_STOCK_MAX, "a big pit holds more firewood");
        h.assertTrue(level.getBlockState(h.absolutePos(new BlockPos(5, 1, 5))).is(Blocks.COBBLESTONE), "the blocks round it are left alone");
        h.succeed();
    }

    private static void roast(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        floor(h);
        BlockPos c = new BlockPos(3, 1, 3);
        smallPit(h, c);
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(9.5, 1, 9.5)));
        BlockPos ac = h.absolutePos(c);
        var cow = EntityTypes.COW.create(level, EntitySpawnReason.LOAD);
        cow.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(8, 1, 8)));
        CarcassEntity carcass = CarcassEntity.of(level, cow);
        level.addFreshEntity(carcass);
        FirePitBlockEntity small = FirePit.entity(level, ac, level.getBlockState(ac));
        h.assertFalse(small.hangCarcass(level, player, carcass), "a small pit can't take a whole carcass");
        h.assertTrue(FirePitBlock.upgrade(level, ac, player), "grows");
        FirePitBlockEntity pit = FirePit.entity(level, ac, level.getBlockState(ac));
        h.assertFalse(pit.hangCarcass(level, player, carcass), "the hide must come off first");
        carcass.setSkinned();
        h.assertTrue(pit.hangCarcass(level, player, carcass) && carcass.isRemoved(), "a skinned cow hangs over the big pit");
        h.assertFalse(pit.takeRoast(player), "not done yet");
        for (int i = 0; i < 8; i++) pit.feed(level, player, new ItemStack(Mechanical.FIREWOOD.values().iterator().next().get()));
        pit.light();
        BlockState lit = level.getBlockState(pit.getBlockPos());
        for (int t = 0; t < FirePit.ROAST_TICKS; t++) pit.serverTick(level, pit.getBlockPos(), level.getBlockState(pit.getBlockPos()));
        h.assertTrue(lit.getValue(FirePitBlock.LIT) && pit.roastDone(), "roasted after " + FirePit.ROAST_TICKS + " lit ticks");
        int want = FirePit.ROAST_MEAT * Butchery.yieldFor(EntityTypes.COW).maxMeat();
        ItemStack cooked = pit.roast().copy();
        h.assertTrue(pit.takeRoast(player), "taken off");
        h.assertTrue(cooked.getCount() == want && player.getInventory().countItem(cooked.getItem()) == want,
                "a pile of cooked meat: " + want + " " + cooked.getItem() + ", got " + player.getInventory().countItem(cooked.getItem()));
        h.assertFalse(cooked.is(Butchery.yieldFor(EntityTypes.COW).meat().get()), "cooked, not raw");
        h.succeed();
    }

    private static void satchel(GameTestHelper h) {
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        ItemStack satchel = new ItemStack(Mechanical.LEATHER_SATCHEL.get());
        h.assertTrue(Satchels.toolSlots(satchel) == 0, "a plain satchel has no tool loops");
        ItemStack loops = new ItemStack(Satchels.TOOL_LOOPS.get(), 2);
        Slot slot = new Slot(new SimpleContainer(satchel), 0, 0, 0);
        h.assertTrue(satchel.getItem().overrideOtherStackedOnMe(satchel, loops, slot, ClickAction.PRIMARY, player, SlotAccess.of(() -> loops, s -> {})),
                "tool loops dropped on the satchel");
        h.assertTrue(Satchels.toolSlots(satchel) == Satchels.TOOL_SLOTS && loops.getCount() == 1, "three tool loops sewn on, one used");
        h.assertFalse(satchel.getItem().overrideOtherStackedOnMe(satchel, loops, slot, ClickAction.PRIMARY, player, SlotAccess.of(() -> loops, s -> {})),
                "only once");
        PackMenu menu = PackMenu.forTest(player.getInventory(), satchel);
        int tools = menu.toolStart();
        h.assertTrue(tools == 9 && menu.invStart() == 12, "9 bag slots then 3 tool slots");
        ItemStack knife = new ItemStack(Smithing.STEEL_KNIFE.get());
        h.assertTrue(menu.slots.get(tools).mayPlace(knife) && menu.slots.get(tools).mayPlace(new ItemStack(Smithing.STEEL_SAW.get()))
                && menu.slots.get(tools).mayPlace(new ItemStack(Mechanical.BONE_NEEDLE.get())), "tool slots take knives, saws, needles");
        h.assertFalse(menu.slots.get(tools).mayPlace(new ItemStack(Items.DIRT)) || menu.slots.get(tools).mayPlace(new ItemStack(Items.BREAD)),
                "but not dirt or bread");
        h.assertTrue(menu.slots.get(0).mayPlace(new ItemStack(Items.DIRT)), "the bag slots still take anything");
        player.getInventory().setItem(9, knife);
        int knifeSlot = menu.invStart(); // player inventory index 9 is the first menu slot of the inventory rows
        menu.quickMoveStack(player, knifeSlot);
        var contents = net.minecraft.core.NonNullList.withSize(12, ItemStack.EMPTY);
        satchel.get(DataComponents.CONTAINER).copyInto(contents);
        h.assertTrue(contents.size() > 9 && contents.get(9).is(Smithing.STEEL_KNIFE.get()), "shift-click put the knife in the first tool loop");
        // the 5x5 Steel-Frame Pack: 25 bag slots, tool loops built in, the inventory two rows lower
        ItemStack pack = new ItemStack(Satchels.STEEL_FRAME_PACK.get());
        PackMenu big = PackMenu.forTest(player.getInventory(), pack);
        h.assertTrue(big.toolStart() == 25 && big.invStart() == 28 && big.extraHeight == 36, "5x5 + 3 tool loops");
        h.assertTrue(big.slots.get(big.invStart()).y == 84 + 36, "inventory below the taller grid");
        big.slots.get(24).set(new ItemStack(Items.COBBLESTONE, 64));
        var packed = net.minecraft.core.NonNullList.withSize(28, ItemStack.EMPTY);
        pack.get(DataComponents.CONTAINER).copyInto(packed);
        h.assertTrue(packed.get(24).is(Items.COBBLESTONE), "the 25th slot is saved in the pack");
        h.assertTrue(pack.is(com.mechanical.equipment.Accessory.BACK.tag), "worn on the back");
        h.succeed();
    }

    private static void jewelry(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        floor(h);
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        // a Clay Kiln (both halves) with the casting table beside its lower half
        BlockState kiln = Mechanical.KILN.get().defaultBlockState();
        h.setBlock(new BlockPos(2, 1, 3), kiln.setValue(KilnBlock.HALF, DoubleBlockHalf.LOWER));
        h.setBlock(new BlockPos(2, 2, 3), kiln.setValue(KilnBlock.HALF, DoubleBlockHalf.UPPER));
        BlockPos tablePos = h.absolutePos(new BlockPos(3, 1, 3));
        h.setBlock(new BlockPos(3, 1, 3), Jewelry.STEEL_CASTING_TABLE.get().defaultBlockState());
        KilnBlockEntity k = (KilnBlockEntity) level.getBlockEntity(h.absolutePos(new BlockPos(2, 1, 3)));
        JewelryTableBlockEntity table = (JewelryTableBlockEntity) level.getBlockEntity(tablePos);
        ItemStack gold = new ItemStack(Items.GOLD_INGOT, 2);
        table.use(level, player, InteractionHand.MAIN_HAND, gold);
        h.assertTrue(table.items().get(0).is(Items.GOLD_INGOT) && gold.getCount() == 1, "a gold ingot on the table");
        k.setTemperatureForTest(300);
        for (int t = 0; t < Jewelry.CAST_TICKS + 5; t++) table.serverTick(level, tablePos);
        h.assertTrue(table.items().get(1).isEmpty(), "a cold kiln melts nothing");
        for (int t = 0; t < Jewelry.CAST_TICKS; t++) {
            k.setTemperatureForTest(IronOres.GOLD_TEMP + 50);
            table.serverTick(level, tablePos);
        }
        h.assertTrue(table.items().get(1).is(Jewelry.GOLD_RING_BLANK.get()) && table.items().get(1).getCount() == 2 && table.items().get(0).isEmpty(),
                "one ingot cast into two ring blanks beside a kiln at 1100°+");
        // the Jeweler's Bench
        BlockPos benchPos = h.absolutePos(new BlockPos(6, 1, 3));
        h.setBlock(new BlockPos(6, 1, 3), Jewelry.JEWELERS_BENCH.get().defaultBlockState());
        JewelryTableBlockEntity bench = (JewelryTableBlockEntity) level.getBlockEntity(benchPos);
        bench.use(level, player, InteractionHand.MAIN_HAND, new ItemStack(Jewelry.GOLD_RING_BLANK.get()));
        ItemStack ruby = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(MODID, "flawless_ruby")));
        h.assertTrue(Jewelry.gemOf(ruby) != null && Jewelry.gemOf(new ItemStack(Items.DIRT)) == null, "a Flawless Ruby is a gem, dirt isn't");
        bench.use(level, player, InteractionHand.MAIN_HAND, ruby);
        ItemStack chisel = new ItemStack(Smithing.STEEL_CHISEL.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, chisel);
        bench.use(level, player, InteractionHand.MAIN_HAND, chisel);
        ItemStack ring = ItemStack.EMPTY;
        for (ItemStack s : player.getInventory().getNonEquipmentItems()) if (s.is(Jewelry.GOLD_RING.get())) ring = s;
        h.assertTrue(!ring.isEmpty() && ring.get(Jewelry.JEWEL.get()).equals(new Jewelry.Jewel("ruby", true)), "a Gold Ring with a Flawless Ruby");
        h.assertTrue(bench.items().get(0).isEmpty() && bench.items().get(1).isEmpty(), "the bench is cleared");
        // worn: +4 max health; taken off again: back to 20
        double before = player.getAttribute(Attributes.MAX_HEALTH).getValue();
        Equipment.of(player).set(Accessory.RING_1, ring.copy());
        Equipment.update(player);
        double worn = player.getAttribute(Attributes.MAX_HEALTH).getValue();
        h.assertTrue(worn == before + 4, "a Flawless Ruby ring adds 4 health: " + before + " -> " + worn);
        Equipment.of(player).set(Accessory.RING_1, ItemStack.EMPTY);
        Equipment.update(player);
        h.assertTrue(player.getAttribute(Attributes.MAX_HEALTH).getValue() == before, "taken off, the bonus is gone");
        // a plain garnet necklace keeps a little cold out
        ItemStack necklace = Jewelry.make(Jewelry.GOLD_NECKLACE.get(), new Jewelry.Jewel("garnet", false));
        h.assertTrue(necklace.is(Accessory.NECK.tag) && Temperature.insulation(necklace) == Jewelry.WARM_NORMAL, "garnet: cold insulation");
        Equipment.of(player).set(Accessory.NECK, necklace);
        h.assertTrue(Temperature.wornForWarmth(player).contains(necklace), "the necklace counts toward warmth when worn");
        h.succeed();
    }

    private P8SmithTest() {}
}
