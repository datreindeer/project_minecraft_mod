package com.mechanical.equipment;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.function.Consumer;

import com.mechanical.CarcassEntity;
import com.mechanical.Mechanical;
import com.mechanical.Smithing;
import com.mechanical.TestArea;
import com.mechanical.wildlife.Wildlife;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Game tests for the gear batch (v0.211.0): the two Hip slots (two pouches, never two quivers; old saves keep their single
 * Hip item; B shows both) and tools used from a worn bag with an empty hand ({@link WornTools}: a knife in the satchel
 * butchers, first match by bag order, wear on the stored knife). Silver jewellery: P8SmithTest p8_silver_jewelry.
 */
@EventBusSubscriber(modid = MODID)
public final class GearTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final List<String> TESTS = List.of("gear_two_hips", "gear_worn_knife");

    static {
        TEST_FUNCTIONS.register("gear_two_hips", () -> GearTest::twoHips);
        TEST_FUNCTIONS.register("gear_worn_knife", () -> GearTest::wornKnife);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "gear"), new TestEnvironmentDefinition.AllOf(List.of()));
        for (String t : TESTS) {
            event.registerTest(Identifier.fromNamespaceAndPath(MODID, t), new FunctionGameTestInstance(
                    ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, t)), TestArea.of(env, t.equals("gear_worn_knife") ? 1200 : 40, true)));
        }
    }

    private static void floor(GameTestHelper h) {
        for (int x = 0; x <= 10; x++) for (int z = 0; z <= 10; z++) {
            h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            for (int y = 1; y <= 3; y++) h.setBlock(new BlockPos(x, y, z), Blocks.AIR);
        }
    }

    private static ItemStack bagWith(net.minecraft.world.item.Item bag, int size, int[] slots, ItemStack... items) {
        ItemStack stack = new ItemStack(bag);
        NonNullList<ItemStack> list = NonNullList.withSize(size, ItemStack.EMPTY);
        for (int i = 0; i < slots.length; i++) list.set(slots[i], items[i]);
        stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(list));
        return stack;
    }

    private static ItemStack slot(ItemStack bag, int size, int index) {
        NonNullList<ItemStack> list = NonNullList.withSize(size, ItemStack.EMPTY);
        bag.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(list);
        return list.get(index);
    }

    // ---------------------------------------------------------------- two Hip slots

    private static void twoHips(GameTestHelper h) {
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        InventoryMenu menu = p.inventoryMenu;
        var hip = menu.slots.get(Equipment.MENU_START + Accessory.HIP.ordinal());
        var hip2 = menu.slots.get(Equipment.MENU_START + Accessory.HIP_2.ordinal());
        h.assertTrue(hip2 instanceof AccessorySlot a && a.accessory == Accessory.HIP_2 && hip2.y == hip.y && hip2.x == hip.x + 18,
                "the second Hip slot sits right of the first");
        ItemStack quiver = new ItemStack(Mechanical.QUIVER.get()), pouch = new ItemStack(Mechanical.LEATHER_POUCH.get());
        EquipmentInventory worn = Equipment.of(p);

        // two pouches
        h.assertTrue(hip.mayPlace(pouch) && hip2.mayPlace(pouch), "both Hip slots take a pouch");
        worn.set(Accessory.HIP, pouch.copy());
        h.assertTrue(Equipment.freeSlotFor(p, pouch) == Accessory.HIP_2, "a second pouch goes to the second Hip slot");
        h.assertTrue(hip2.mayPlace(quiver), "a quiver beside a pouch");
        // never two quivers
        worn.set(Accessory.HIP, quiver.copy());
        h.assertTrue(!hip2.mayPlace(quiver), "no second quiver in the second Hip slot");
        h.assertTrue(hip.mayPlace(quiver), "but the worn quiver can be swapped for another");
        h.assertTrue(Equipment.freeSlotFor(p, quiver) == null, "shift-click finds no slot for a second quiver");
        ItemStack held = quiver.copy();
        p.setItemInHand(InteractionHand.MAIN_HAND, held);
        Equipment.equipFromHand(p, held, InteractionHand.MAIN_HAND);
        h.assertTrue(worn.get(Accessory.HIP_2).isEmpty(), "right-clicking a second quiver never puts it in the second Hip slot");
        h.assertTrue(Equipment.canWear(p, Accessory.HIP_2, pouch), "a pouch still fits beside the quiver");
        // the quiver works from the second slot too
        worn.set(Accessory.HIP, pouch.copy());
        worn.set(Accessory.HIP_2, quiver.copy());
        h.assertTrue(!Quiver.worn(p).isEmpty() && !hip.mayPlace(quiver), "a quiver in the second slot is worn, and blocks a quiver in the first");
        ItemStack arrows = new ItemStack(Items.ARROW, 10);
        h.assertTrue(Quiver.insert(p, arrows) && arrows.isEmpty(), "arrows go into the quiver in the second Hip slot");

        // B shows both: arrows into the quiver's 2x2, other things into the pouch's
        p.getInventory().setItem(9, new ItemStack(Items.ARROW, 5));
        p.getInventory().setItem(10, new ItemStack(Items.FLINT, 3));
        PackMenu pack = PackMenu.wornForTest(p);
        h.assertTrue(pack != null && pack.hipCount == 2 && !pack.hasSatchel, "B opens both hip bags");
        int inv = pack.invStart();
        pack.quickMoveStack(p, inv); // inventory slot 9 is the first of the menu's inventory rows
        pack.quickMoveStack(p, inv + 1);
        h.assertTrue(pack.slots.get(pack.hipStart(1)).getItem().is(Items.ARROW), "shift-clicked arrows go into the quiver (second bag)");
        h.assertTrue(pack.slots.get(pack.hipStart(0)).getItem().is(Items.FLINT), "flint goes into the pouch (first bag)");
        h.assertTrue(slot(worn.get(Accessory.HIP), 4, 0).is(Items.FLINT), "and is saved in the pouch item");
        int[] a = PackMenu.hipPosition(false, 3, 0, 2, 0), b = PackMenu.hipPosition(false, 3, 0, 2, 1);
        h.assertTrue(b[0] >= a[0] + 36 + 4, "the two 2x2s side by side don't overlap");
        int[] pa = PackMenu.hipPosition(true, 5, 3, 2, 0), pb = PackMenu.hipPosition(true, 5, 3, 2, 1);
        h.assertTrue(pb[1] >= pa[1] + 36 + 4 && pb[1] + 36 <= 17 + 90, "beside the 5x5 pack they stack inside its height");

        // an old save (7 slots, one Hip at index 5) keeps its Hip item, and the second Hip slot starts empty
        NonNullList<ItemStack> old = NonNullList.withSize(7, ItemStack.EMPTY);
        old.set(5, pouch.copy());
        old.set(6, new ItemStack(com.mechanical.Flora.FERN_FLOWER_CHARM.get()));
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, h.getLevel().registryAccess());
        ContainerHelper.saveAllItems(out, old);
        EquipmentInventory loaded = new EquipmentInventory();
        loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(), out.buildResult()));
        h.assertTrue(loaded.get(Accessory.HIP).is(Mechanical.LEATHER_POUCH.get()) && loaded.get(Accessory.CHARM).is(com.mechanical.Flora.FERN_FLOWER_CHARM.get())
                && loaded.get(Accessory.HIP_2).isEmpty(), "an old save keeps its Hip and Charm items; the new Hip slot is empty");
        h.succeed();
    }

    // ---------------------------------------------------------------- a knife from the satchel, empty hand

    private static void wornKnife(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        floor(h);
        ServerPlayer p = net.neoforged.neoforge.common.util.FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "gear_knife"));
        Vec3 stand = h.absoluteVec(new Vec3(3.5, 1, 5.5));
        p.snapTo(stand.x, stand.y, stand.z, 0, 0);
        // the satchel: an iron knife in slot 2, a steel knife in slot 5; a pouch at the hip with a flint knife (searched after the back)
        ItemStack satchel = bagWith(Mechanical.LEATHER_SATCHEL.get(), 9, new int[] {2, 5}, new ItemStack(Mechanical.IRON_KNIFE.get()), new ItemStack(Smithing.STEEL_KNIFE.get()));
        ItemStack pouch = bagWith(Mechanical.LEATHER_POUCH.get(), 4, new int[] {0}, new ItemStack(Mechanical.FLINT_KNIFE.get()));
        Equipment.of(p).set(Accessory.BACK, satchel);
        Equipment.of(p).set(Accessory.HIP, pouch);
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        p.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);

        var knife = WornTools.forEmptyHand(p, InteractionHand.MAIN_HAND, s -> s.getItem() instanceof com.mechanical.KnifeItem);
        h.assertTrue(knife != null && knife.bag() == satchel && knife.slot() == 2 && knife.tool().is(Mechanical.IRON_KNIFE.get()),
                "first match by bag order: the satchel's first knife");
        h.assertTrue(WornTools.forEmptyHand(p, InteractionHand.OFF_HAND, s -> s.getItem() instanceof com.mechanical.KnifeItem) == null, "only the main hand");
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        h.assertTrue(WornTools.forEmptyHand(p, InteractionHand.MAIN_HAND, s -> s.getItem() instanceof com.mechanical.KnifeItem) == null, "only an empty hand");
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Mechanical.FLINT_KNIFE.get()));
        h.assertTrue(WornTools.forEmptyHand(p, InteractionHand.MAIN_HAND, s -> s.getItem() instanceof com.mechanical.KnifeItem) == null,
                "a knife in the off hand is used as it is, not the bag's");
        p.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);

        var hare = Wildlife.HARE.get().create(level, EntitySpawnReason.COMMAND);
        BlockPos at = h.absolutePos(new BlockPos(5, 1, 5));
        hare.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        CarcassEntity carcass = CarcassEntity.of(level, hare);
        level.addFreshEntity(carcass);
        // holding the button on the body: the client repeats the click every 4 ticks
        long start = level.getGameTime();
        h.onEachTick(() -> {
            if (!carcass.isRemoved() && (level.getGameTime() - start) % 4 == 0) carcass.interact(p, InteractionHand.MAIN_HAND, Vec3.ZERO);
        });
        h.succeedWhen(() -> {
            h.assertTrue(carcass.isRemoved(), "the hare is cut up with the knife from the satchel");
            ItemStack used = slot(Equipment.get(p, Accessory.BACK), 9, 2);
            h.assertTrue(used.is(Mechanical.IRON_KNIFE.get()) && used.getDamageValue() > 0, "the stored iron knife took the wear: " + used.getDamageValue());
            h.assertTrue(slot(Equipment.get(p, Accessory.BACK), 9, 5).getDamageValue() == 0, "the second knife is untouched");
            h.assertTrue(slot(Equipment.get(p, Accessory.HIP), 4, 0).getDamageValue() == 0, "the pouch's knife is untouched");
            h.assertTrue(p.getMainHandItem().isEmpty(), "the hand stayed empty");
        });
    }

    private GearTest() {}
}
