package com.mechanical;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Game test for the {@link WagonEntity}: room per empty place, modules put in by right-clicking a place and punched out
 * again (a chest spills), the wagon picked up with everything falling out, and how far it follows one horse or a pair.
 */
@EventBusSubscriber(modid = MODID)
public final class WagonTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> TEST = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "wagon"));

    private static final ResourceKey<Consumer<GameTestHelper>> LIVESTOCK = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "livestock_cart"));
    private static final ResourceKey<Consumer<GameTestHelper>> TRAILER = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "wagon_trailer"));
    private static final ResourceKey<Consumer<GameTestHelper>> CELLS = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "wagon_cells"));

    static {
        TEST_FUNCTIONS.register("wagon", () -> WagonTest::test);
        TEST_FUNCTIONS.register("livestock_cart", () -> WagonTest::livestock);
        TEST_FUNCTIONS.register("wagon_trailer", () -> WagonTest::trailer);
        TEST_FUNCTIONS.register("wagon_cells", () -> WagonTest::cells);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "wagon"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "wagon"),
                new FunctionGameTestInstance(TEST, TestArea.of(env, 40, true)));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "livestock_cart"),
                new FunctionGameTestInstance(LIVESTOCK, TestArea.of(env, 100, true)));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "wagon_trailer"),
                new FunctionGameTestInstance(TRAILER, TestArea.of(env, 100, true)));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "wagon_cells"),
                new FunctionGameTestInstance(CELLS, TestArea.of(env, 60, true)));
    }

    /**
     * A cart right-clicked behind a wagon is hitched as its trailer and follows the moving team; a second cart is refused;
     * a led horse can't take the towed cart; a loaded trailer stops a single horse; right-clicking again unhitches.
     */
    private static void trailer(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        for (int x = 0; x < 7; x++) for (int z = 0; z < 16; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        Vec3 at = h.absoluteVec(new Vec3(3.5, 1, 6.5));
        WagonEntity wagon = Carting.WAGON.get().create(level, EntitySpawnReason.EVENT);
        wagon.setPos(at);
        wagon.setYRot(0); // facing +z
        level.addFreshEntity(wagon);
        AbstractHorse lead = horse(level, at.add(-WagonEntity.PAIR / 2, 0, WagonEntity.LEAD)), second = horse(level, at.add(WagonEntity.PAIR / 2, 0, WagonEntity.LEAD));
        lead.setYRot(0);
        lead.setNoAi(true); // moved by hand below: wandering horses would leave the test's floor and disturb other tests
        second.setNoAi(true);
        wagon.setHarness(lead);
        wagon.setPartner(second);
        CartEntity cart = cart(level, wagon.towPoint().add(0, 0, -CartEntity.LEAD_TOWED));
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(at.add(2, 0, -3));
        cart.grab(player);
        h.assertTrue(cart.puller() == wagon && cart.harness() == null, "the cart is hitched behind the wagon, not to the player");
        h.assertTrue(cart.pullsOther(player) == false && TravoisEntity.pulling(wagon) == cart, "the wagon tows it");
        CartEntity other = cart(level, wagon.towPoint().add(1, 0, -1.5));
        other.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
        h.assertTrue(other.puller() == null, "a wagon takes one trailer only (the second cart: " + other.puller() + ")");
        other.discard();
        double startZ = wagon.getZ();
        for (int t = 1; t <= 6; t++) h.runAfterDelay(t, () -> { // the team walks on, 3 blocks
            lead.setPos(lead.position().add(0, 0, 0.5));
            second.setPos(second.position().add(0, 0, 0.5));
        });
        h.runAfterDelay(12, () -> {
            h.assertTrue(wagon.getZ() > startZ + 2, "the pair drew the wagon on (" + (wagon.getZ() - startZ) + "; wagon " + wagon.position() + " lead " + lead.position()
                    + " harness " + wagon.harness() + " partner " + wagon.partner() + ")");
            double gap = cart.position().subtract(wagon.towPoint()).horizontalDistance();
            h.assertTrue(cart.puller() == wagon && gap < CartEntity.LEAD_TOWED + 0.5, "the trailer followed the wagon (gap " + gap + ")");
            // a led horse can't take the towed cart: the click unhitches it instead
            AbstractHorse third = horse(level, cart.position().add(2, 0, 0));
            third.setLeashedTo(player, true);
            player.setPos(cart.position().add(1.5, 0, 0));
            cart.grab(player);
            h.assertTrue(cart.puller() == null && cart.harness() != third, "the click unhitched the trailer, the led horse stayed out");
            third.dropLeash();
            third.discard();
            cart.grab(player);
            h.assertTrue(cart.puller() == wagon, "hitched again behind the wagon");
            // load: an empty trailer leaves one horse crawling; a cow on it stops the lone horse, the pair still pulls
            wagon.setPartner(null);
            h.assertTrue(wagon.pullFactor() == WagonEntity.CRAWL, "one horse crawls with an empty wagon and empty trailer");
            var cow = net.minecraft.world.entity.EntityTypes.COW.create(level, EntitySpawnReason.EVENT);
            cow.setPos(cart.position().add(1.5, 0, 0));
            level.addFreshEntity(cow);
            cow.setLeashedTo(player, true);
            cart.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
            h.assertTrue(cow.getVehicle() == cart && cart.puller() == wagon, "the cow climbs on, the trailer stays hitched");
            h.assertTrue(wagon.loadFraction() > 0 && wagon.pullFactor() == 0, "one horse can't move a wagon with a loaded trailer");
            wagon.setPartner(second);
            h.assertTrue(wagon.pullFactor() == 1.0, "the pair pulls it");
            cart.grab(player);
            h.assertTrue(cart.puller() == null && wagon.loadFraction() == 0, "unhitched: the trailer's load is off the team");
            for (var e : List.of(cow, cart, wagon, lead, second)) e.discard(); // nothing left to stray into later tests
            h.succeed();
        });
    }

    private static CartEntity cart(ServerLevel level, Vec3 at) {
        CartEntity cart = Carting.CART.get().create(level, EntitySpawnReason.EVENT);
        cart.setPos(at);
        level.addFreshEntity(cart);
        return cart;
    }

    /** Led farm animals climb onto a cart, tied to it; a full cart refuses more; a cut lead lets one off; untying leads it off. */
    private static void livestock(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        Vec3 at = h.absoluteVec(new Vec3(2.5, 1, 2.5));
        CartEntity cart = Carting.CART.get().create(level, EntitySpawnReason.EVENT);
        cart.setPos(at);
        level.addFreshEntity(cart);
        var cow = net.minecraft.world.entity.EntityTypes.COW.create(level, EntitySpawnReason.EVENT);
        var sheep = net.minecraft.world.entity.EntityTypes.SHEEP.create(level, EntitySpawnReason.EVENT);
        var chicken = net.minecraft.world.entity.EntityTypes.CHICKEN.create(level, EntitySpawnReason.EVENT);
        for (var a : List.of(cow, sheep, chicken)) {
            a.setPos(at.add(1.5, 0, 0));
            level.addFreshEntity(a);
        }
        h.assertTrue(TravoisEntity.room(cow) == 3 && TravoisEntity.room(sheep) == 2 && TravoisEntity.room(chicken) == 1, "a cow takes 3 room, a sheep 2, a hen 1");
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(at.add(-1.5, 0, 0));
        cart.grab(player); // nothing led: this harnesses the player instead
        h.assertTrue(cow.getVehicle() == null, "a loose cow stays off");
        cart.setHarness(null);
        TravoisEntity travois = CarcassHauling.TRAVOIS.get().create(level, EntitySpawnReason.EVENT);
        h.assertTrue(!travois.canAddPassenger(cow), "the travois takes no livestock");
        cow.setLeashedTo(player, true);
        sheep.setLeashedTo(player, true);
        h.assertTrue(cow.getLeashHolder() == player, "the cow is led (holder " + cow.getLeashHolder() + ")");
        cart.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
        h.assertTrue(cow.getVehicle() == cart && sheep.getVehicle() == cart && cart.load() == 5, "led cow and sheep climb on: 5 room");
        h.assertTrue(cow.getLeashHolder() == cart && sheep.getLeashHolder() == cart, "their leads are tied to the cart");
        chicken.setLeashedTo(player, true);
        cart.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
        h.assertTrue(chicken.getVehicle() == cart && cart.load() == 6, "a hen fills the last room");
        var hen = net.minecraft.world.entity.EntityTypes.CHICKEN.create(level, EntitySpawnReason.EVENT);
        hen.setPos(at.add(1.5, 0, 0));
        level.addFreshEntity(hen);
        hen.setLeashedTo(player, true);
        cart.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
        h.assertTrue(hen.getVehicle() == null && hen.getLeashHolder() == player, "a full cart takes no more: the hen stays on the lead");
        h.runAfterDelay(40, () -> {
            h.assertTrue(cow.getVehicle() == cart && cow.getLeashHolder() == cart, "the tied cow still rides after 2 s");
            sheep.dropLeash();
            h.runAfterDelay(2, () -> {
                h.assertTrue(sheep.getVehicle() == null, "a cut lead lets the sheep off");
                h.assertTrue(TravoisEntity.untie(cow, player) && cow.getVehicle() == null && cow.getLeashHolder() == player,
                        "untying the cow leads it off on the player's lead");
                for (var e : List.of(cow, sheep, chicken, hen, cart, travois)) e.discard();
                h.succeed();
            });
        });
    }

    private static void test(GameTestHelper h) {
        try {
            run(h);
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw h.assertionException(e + " at " + java.util.Arrays.toString(java.util.Arrays.copyOf(e.getStackTrace(), 6)));
        }
    }

    /**
     * The 2 x 3 bed: six chests at most (the screen holds all of them), four chests leave the last row for a medium body,
     * hay and a seat, four large bodies fill an empty bed, a saved wagon with 4 places still loads.
     */
    private static void cells(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        for (int x = 0; x < 7; x++) for (int z = 0; z < 7; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        Vec3 at = h.absoluteVec(new Vec3(3.5, 1, 3.5));
        WagonEntity wagon = Carting.WAGON.get().create(level, EntitySpawnReason.EVENT);
        wagon.setPos(at);
        wagon.setYRot(0);
        level.addFreshEntity(wagon);
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(at.add(3, 0, 0));
        Vec3 any = new Vec3(0, 1, 0);
        h.assertTrue(WagonEntity.SLOTS == 6 && WagonEntity.ROOM_PER_SLOT == 2 && WagonEntity.CAPACITY == 12, "6 cells of 2 room");

        // four large bodies fill an empty bed
        java.util.List<net.minecraft.world.entity.Mob> cows = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) {
            var cow = net.minecraft.world.entity.EntityTypes.COW.create(level, EntitySpawnReason.EVENT);
            cow.setPos(at.add(1.5, 0, 0));
            level.addFreshEntity(cow);
            cow.setLeashedTo(player, true);
            wagon.interact(player, InteractionHand.MAIN_HAND, any);
            cows.add(cow);
        }
        h.assertTrue(wagon.load() == 12 && cows.stream().filter(c -> c.getVehicle() == wagon).count() == 4, "four cows (3 room each) fill the bed, the fifth stays off (load " + wagon.load() + ")");
        for (var cow : cows) {
            cow.dropLeash();
            cow.discard();
        }

        // 4 chests: a free last row for a medium body, hay and a seat
        for (int i = 0; i < 4; i++) use(wagon, player, Items.CHEST, any);
        h.assertTrue(wagon.capacity() == 4, "four chests leave 2 cells, 4 room");
        var sheep = net.minecraft.world.entity.EntityTypes.SHEEP.create(level, EntitySpawnReason.EVENT);
        sheep.setPos(at.add(1.5, 0, 0));
        level.addFreshEntity(sheep);
        sheep.setLeashedTo(player, true);
        wagon.interact(player, InteractionHand.MAIN_HAND, new Vec3(0, 1, -1.0)); // aimed at the free back row: a click on a chest opens it
        h.assertTrue(sheep.getVehicle() == wagon, "a medium animal fits in the free last row");
        use(wagon, player, Items.HAY_BLOCK, any);
        h.assertTrue(wagon.module(5) == WagonEntity.HAY && sheep.getVehicle() == wagon, "a hay bale takes the back right cell beside the animal");
        use(wagon, player, Items.OAK_SLAB, any);
        h.assertTrue(wagon.module(4) == WagonEntity.EMPTY, "the animal's room is not given to a seat");
        sheep.dropLeash();
        sheep.discard();
        h.assertTrue(wagon.load() == 0, "the animal is off");
        use(wagon, player, Items.OAK_SLAB, any);
        h.assertTrue(wagon.module(4) == WagonEntity.SEAT, "with the animal gone the seat goes in the cell beside the hay");

        // the fill fraction counts every chest's slots
        use(wagon, player, Items.CHEST, any);
        h.assertTrue(wagon.module(4) == WagonEntity.SEAT, "no free cell: a chest doesn't take the seat's");
        for (int i = 0; i < 27; i++) wagon.chest(0).setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        h.assertTrue(Math.abs(wagon.loadFraction() - 27.0 / (4 * 27)) < 1.0E-6, "fill fraction over the 4 chests: " + wagon.loadFraction());
        // a pair (cells 0 and 1) opens as a double chest, left cell first; a lone chest as 27 slots
        wagon.chest(1).setItem(0, new ItemStack(Items.IRON_INGOT, 2));
        h.assertTrue(wagon.paired(0) && wagon.paired(1) && wagon.paired(2) && wagon.paired(3), "two chests in a row are a pair");
        // a plain right-click with an empty hand on a chest opens it (no sneaking); on an empty cell it grabs nothing
        player.setShiftKeyDown(false);
        // (the mock player is no ServerPlayer, so the screen itself is not opened here: chestMenu below is what opens)
        h.assertTrue(wagon.interact(player, InteractionHand.MAIN_HAND, new Vec3(0.3, 1, 1.0)) == net.minecraft.world.InteractionResult.SUCCESS,
                "a plain right-click on a chest is taken (it opens the chest)");
        h.assertTrue(wagon.interact(player, InteractionHand.MAIN_HAND, new Vec3(0, 1, -1.0)) == net.minecraft.world.InteractionResult.SUCCESS,
                "an empty-hand click on an empty cell only hints at the grab key");
        h.assertTrue(wagon.harness() == null, "an empty-hand click harnesses nothing");
        var menu = wagon.chestMenu(1, player.getInventory(), 1); // opened from the right cell: the same double chest
        h.assertTrue(menu.slots.size() == 54 + 36, "a pair opens 54 slots and the player's 36");
        h.assertTrue(menu.getSlot(0).getItem() == wagon.chest(0).getItem(0) && menu.getSlot(27).getItem() == wagon.chest(1).getItem(0),
                "the left cell's chest comes first, the right cell's after");
        player.getInventory().setItem(0, new ItemStack(Items.DIRT, 5));
        menu.quickMoveStack(player, 54 + 27); // hotbar slot 0 into the chests
        h.assertTrue(player.getInventory().getItem(0).isEmpty() && wagon.chest(1).countItem(Items.DIRT) == 5, "shift-click moved the dirt in (chest 0 is full)");
        menu.quickMoveStack(player, 0); // chest slot 0 (cobblestone) out to the inventory
        h.assertTrue(player.getInventory().countItem(Items.COBBLESTONE) == 64, "shift-click moved a stack out");
        h.assertTrue(menu.stillValid(player), "the menu is valid while the pair stays");
        Player puncher = h.makeMockPlayer(GameType.SURVIVAL);
        puncher.setPos(at.x + 0.3, at.y + 3, at.z + 1.1);
        puncher.setXRot(90);
        wagon.hurtServer(level, level.damageSources().playerAttack(puncher), 1);
        h.assertTrue(!menu.stillValid(player), "a chest taken out closes the open double chest");
        h.assertTrue(!wagon.paired(1) && wagon.chestMenu(2, player.getInventory(), 1).slots.size() == 27 + 36, "the chest left alone opens 27 slots");
        var lone = wagon.chestMenu(3, player.getInventory(), 1);
        h.assertTrue(lone.stillValid(player), "a lone chest's menu is valid");
        player.setPos(at.add(20, 0, 0));
        h.assertTrue(!lone.stillValid(player), "too far (8 blocks) closes it");
        player.setPos(at);

        // 6 chests
        WagonEntity full = Carting.WAGON.get().create(level, EntitySpawnReason.EVENT);
        full.setPos(at.add(0, 0, 3));
        level.addFreshEntity(full);
        for (int i = 0; i < 7; i++) use(full, player, Items.CHEST, any);
        h.assertTrue(full.capacity() == 0 && full.module(5) == WagonEntity.CHEST && full.paired(4), "six chests at most, no carcass room left");
        var gone = full.chestMenu(4, player.getInventory(), 0);
        full.discard();
        h.assertTrue(!gone.stillValid(player), "a wagon that is gone closes the screen");

        // a wagon saved with 4 places: chest, hay (5 bites), empty, seat
        var old = new net.minecraft.nbt.CompoundTag();
        old.putInt("modules", 1 | 2 << 2 | 3 << 6);
        old.putIntArray("hay", new int[] {0, 5, 0, 0});
        wagon.discard();
        WagonEntity loaded = Carting.WAGON.get().create(level, EntitySpawnReason.EVENT);
        net.minecraft.core.NonNullList<ItemStack> goods = net.minecraft.core.NonNullList.withSize(27, ItemStack.EMPTY);
        goods.set(0, new ItemStack(Items.IRON_INGOT, 3));
        var out = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess());
        net.minecraft.world.ContainerHelper.saveAllItems(out.child("chest0"), goods);
        old.put("chest0", out.buildResult().getCompoundOrEmpty("chest0"));
        loaded.readAdditionalSaveData(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(), old));
        h.assertTrue(loaded.module(0) == WagonEntity.CHEST && loaded.module(1) == WagonEntity.HAY && loaded.module(2) == WagonEntity.EMPTY
                && loaded.module(3) == WagonEntity.SEAT && loaded.module(4) == WagonEntity.EMPTY, "old places 0-3 become cells 0-3");
        h.assertTrue(loaded.chest(0).countItem(Items.IRON_INGOT) == 3, "the old chest keeps its goods");
        loaded.discard();
        h.succeed();
    }

    private static void run(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        WagonEntity wagon = Carting.WAGON.get().create(level, EntitySpawnReason.EVENT);
        Vec3 at = h.absoluteVec(new Vec3(2.5, 1, 2.5));
        wagon.setPos(at);
        wagon.setYRot(0); // facing +z: row 0 (the front) is towards +z
        level.addFreshEntity(wagon);
        h.assertTrue(wagon.capacity() == WagonEntity.CAPACITY && WagonEntity.CAPACITY == 12, "an empty bed holds 12 room");

        // ---- modules: right-click the wagon with the block; chests from the front, hay and seats from the back
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        Vec3 any = new Vec3(0, 1, 0);
        use(wagon, player, Items.CHEST, any);
        use(wagon, player, Items.CHEST, any);
        use(wagon, player, Items.CHEST, any);
        h.assertTrue(wagon.module(0) == WagonEntity.CHEST && wagon.module(1) == WagonEntity.CHEST && wagon.module(2) == WagonEntity.CHEST
                && wagon.module(3) == WagonEntity.EMPTY, "chests fill front left, front right, then the row behind");
        use(wagon, player, Items.HAY_BLOCK, any);
        use(wagon, player, Items.OAK_SLAB, any);
        h.assertTrue(wagon.module(5) == WagonEntity.HAY && wagon.module(4) == WagonEntity.SEAT, "hay and a seat fill from the back");
        h.assertTrue(wagon.capacity() == 2, "five modules leave one cell, 2 room, not " + wagon.capacity());

        // ---- pulling: one horse crawls with it empty, can't move it loaded; a pair pulls it freely
        AbstractHorse lead = horse(level, at.add(0, 0, 3)), second = horse(level, at.add(1.5, 0, 3));
        wagon.setHarness(lead);
        h.assertTrue(wagon.pullFactor() == WagonEntity.CRAWL, "one horse crawls with an empty wagon");
        wagon.chest(0).setItem(0, new ItemStack(Items.COBBLESTONE, 64));
        h.assertTrue(wagon.pullFactor() == 0, "one horse can't move a loaded wagon");
        wagon.setPartner(second);
        h.assertTrue(wagon.pullFactor() == 1.0, "a pair pulls it");
        CartEntity cart = Carting.CART.get().create(level, EntitySpawnReason.EVENT);
        cart.setPos(at.add(0, 0, -3));
        level.addFreshEntity(cart);
        h.assertTrue(cart.pullsOther(lead) && cart.pullsOther(second) && !wagon.pullsOther(lead), "a horse in the wagon's team can't pull a cart too");
        cart.discard();
        wagon.setHarness(null);
        wagon.setPartner(null);

        // ---- punching a module takes it out, the chest spilling; then the wagon itself comes up with the rest
        Player puncher = h.makeMockPlayer(GameType.SURVIVAL);
        puncher.setPos(at.x + 0.3, at.y + 3, at.z + 1.1); // facing +z the wagon's left is +x
        puncher.setXRot(90); // looking straight down onto the front left cell
        wagon.hurtServer(level, level.damageSources().playerAttack(puncher), 1);
        h.assertTrue(wagon.isAlive() && wagon.module(0) == WagonEntity.EMPTY && wagon.module(1) == WagonEntity.CHEST,
                "the punch took the front left chest out, not the wagon or the chest beside it");
        h.assertTrue(dropped(h, level, at, Items.CHEST) && dropped(h, level, at, Items.COBBLESTONE), "the chest and its goods fall out");
        wagon.hurtServer(level, level.damageSources().playerAttack(puncher), 1);
        h.assertTrue(!wagon.isAlive(), "punching an empty cell picks the wagon up");
        h.assertTrue(dropped(h, level, at, Carting.WAGON_ITEM.get()) && dropped(h, level, at, Items.HAY_BLOCK),
                "the wagon and its untouched hay bale drop");
        h.succeed();
    }

    private static void use(WagonEntity wagon, Player player, Item item, Vec3 local) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
        wagon.interact(player, InteractionHand.MAIN_HAND, local);
    }

    private static AbstractHorse horse(ServerLevel level, Vec3 at) {
        AbstractHorse horse = com.mechanical.wildlife.Wildlife.TAME_HORSE.get().create(level, EntitySpawnReason.EVENT);
        horse.setPos(at);
        horse.setTamed(true);
        level.addFreshEntity(horse);
        return horse;
    }

    private static boolean dropped(GameTestHelper h, ServerLevel level, Vec3 at, Item item) {
        return !level.getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(4), e -> e.getItem().is(item)).isEmpty();
    }
}
