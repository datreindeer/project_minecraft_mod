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

    static {
        TEST_FUNCTIONS.register("wagon", () -> WagonTest::test);
        TEST_FUNCTIONS.register("livestock_cart", () -> WagonTest::livestock);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "wagon"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "wagon"),
                new FunctionGameTestInstance(TEST, new TestData<>(env, Identifier.withDefaultNamespace("empty"), 40, 0, true)));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "livestock_cart"),
                new FunctionGameTestInstance(LIVESTOCK, new TestData<>(env, Identifier.withDefaultNamespace("empty"), 100, 0, true)));
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
        cart.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO); // nothing led: this harnesses the player instead
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

    private static void run(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        WagonEntity wagon = Carting.WAGON.get().create(level, EntitySpawnReason.EVENT);
        Vec3 at = h.absoluteVec(new Vec3(2.5, 1, 2.5));
        wagon.setPos(at);
        wagon.setYRot(0); // facing +z: place 0 (the front) is towards +z
        level.addFreshEntity(wagon);
        h.assertTrue(wagon.capacity() == WagonEntity.CAPACITY && WagonEntity.CAPACITY == 12, "an empty bed holds 12 room");

        // ---- modules: right-click a place with the block
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        Vec3 front = new Vec3(0, 1, 1.1), back = new Vec3(0, 1, -1.1);
        use(wagon, player, Items.CHEST, front);
        h.assertTrue(wagon.module(0) == WagonEntity.CHEST, "a chest goes in the front place");
        use(wagon, player, Items.HAY_BLOCK, back);
        h.assertTrue(wagon.module(3) == WagonEntity.HAY, "a hay bale goes in the back place");
        use(wagon, player, Items.OAK_SLAB, front);
        h.assertTrue(wagon.module(0) == WagonEntity.CHEST, "a taken place keeps its module");
        h.assertTrue(wagon.capacity() == 6, "two modules leave 6 room, not " + wagon.capacity());

        // ---- pulling: one horse crawls with it empty, can't move it loaded; a pair pulls it freely
        AbstractHorse lead = horse(level, at.add(0, 0, 3)), second = horse(level, at.add(1.5, 0, 3));
        wagon.setHarness(lead);
        h.assertTrue(wagon.pullFactor() == WagonEntity.CRAWL, "one horse crawls with an empty wagon");
        wagon.chest(0).setItem(0, new ItemStack(Items.COBBLESTONE, 64));
        h.assertTrue(wagon.pullFactor() == 0, "one horse can't move a loaded wagon");
        wagon.setPartner(second);
        h.assertTrue(wagon.pullFactor() == 1.0, "a pair pulls it");
        wagon.setHarness(null);
        wagon.setPartner(null);

        // ---- punching a module takes it out, the chest spilling; then the wagon itself comes up with the rest
        Player puncher = h.makeMockPlayer(GameType.SURVIVAL);
        puncher.setPos(at.x, at.y + 3, at.z + 1.1);
        puncher.setXRot(90); // looking straight down onto the front place
        wagon.hurtServer(level, level.damageSources().playerAttack(puncher), 1);
        h.assertTrue(wagon.isAlive() && wagon.module(0) == WagonEntity.EMPTY, "the punch took the chest out, not the wagon");
        h.assertTrue(dropped(h, level, at, Items.CHEST) && dropped(h, level, at, Items.COBBLESTONE), "the chest and its goods fall out");
        wagon.hurtServer(level, level.damageSources().playerAttack(puncher), 1);
        h.assertTrue(!wagon.isAlive(), "punching an empty place picks the wagon up");
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
