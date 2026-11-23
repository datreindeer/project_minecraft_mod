package com.mechanical.people;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.function.Consumer;

import com.mechanical.CartEntity;
import com.mechanical.Coins;
import com.mechanical.TestArea;
import com.mechanical.WagonEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Trader Caravans (Caravans, docs/traders-plan.md):
 * <ul>
 * <li>caravan_kinds: every kind (TraderKinds) comes as 4-5 of one trading tribe: a leader with a staff and a Coin Purse and
 * goods of his kind, a handler holding the animals (camels for desert kinds), 2-3 guards armed and armoured (no leather,
 * no helmet); a small one with a cart showing its cargo, a big one with a Wagon, a pair, rare goods and a fuller purse.</li>
 * <li>caravan_still: once there, the handler and the animals stay put, the leader keeps near the handler and the guards
 * round the leader.</li>
 * <li>caravan_guards: hitting a caravan's animal or its cart angers the caravan, and the guards go for the player.</li>
 * <li>caravan_purse: selling to the leader stops when his purse is empty and goes on once buying from him has filled it;
 * buying stops when the stock runs out; coins are paid with change.</li>
 * <li>caravan_arrive: a caravan set out down the road walks in as a column, the animals and the cart following the
 * handler, and stops at its stop: then it is stopped, the cart still hitched behind the horse.</li>
 * </ul>
 */
@EventBusSubscriber(modid = MODID)
public final class CaravanTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final String[] TESTS = {"caravan_kinds", "caravan_still", "caravan_guards", "caravan_purse", "caravan_arrive"};

    static {
        TEST_FUNCTIONS.register("caravan_kinds", () -> CaravanTest::kinds);
        TEST_FUNCTIONS.register("caravan_still", () -> CaravanTest::still);
        TEST_FUNCTIONS.register("caravan_guards", () -> CaravanTest::guards);
        TEST_FUNCTIONS.register("caravan_purse", () -> CaravanTest::purse);
        TEST_FUNCTIONS.register("caravan_arrive", () -> CaravanTest::arrive);
    }

    static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "caravan"), new TestEnvironmentDefinition.AllOf(List.of()));
        for (String name : TESTS) {
            ResourceKey<Consumer<GameTestHelper>> key = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, name));
            event.registerTest(Identifier.fromNamespaceAndPath(MODID, name), new FunctionGameTestInstance(key, TestArea.of(env, name.equals("caravan_arrive") ? 1200 : 400, true)));
        }
    }

    private static void guarded(GameTestHelper h, Runnable test) {
        try {
            test.run();
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw h.assertionException(e + " at " + java.util.Arrays.toString(java.util.Arrays.copyOf(e.getStackTrace(), 6)));
        }
    }

    private static List<Tribesperson> members(ServerLevel level, Tribesperson leader) {
        return level.getEntitiesOfClass(Tribesperson.class, new AABB(leader.blockPosition()).inflate(24), leader::inGroup);
    }

    private static Tribesperson role(List<Tribesperson> people, TribeRole role) {
        return people.stream().filter(t -> t.role() == role).findFirst().orElse(null);
    }

    private static List<AbstractHorse> animals(ServerLevel level, Tribesperson handler) {
        return level.getEntitiesOfClass(AbstractHorse.class, new AABB(handler.blockPosition()).inflate(16), a -> a.getLeashHolder() == handler);
    }

    private static Tribesperson spawn(GameTestHelper h, String kind, boolean big) {
        Tribesperson leader = Caravans.spawn(h.getLevel(), h.absolutePos(new BlockPos(8, 1, 8)), Identifier.fromNamespaceAndPath(MODID, kind), big);
        h.assertTrue(leader != null, "no " + kind + " caravan was made");
        return leader;
    }

    private static void kinds(GameTestHelper h) {
        guarded(h, () -> {
            ServerLevel level = h.getLevel();
            h.assertTrue(TraderKinds.all().size() >= 6, "the six caravan kinds load, got " + TraderKinds.all().keySet());
            for (var entry : TraderKinds.all().entrySet()) {
                String kind = entry.getKey().getPath();
                for (boolean big : new boolean[] {false, true}) {
                    if (big && !kind.equals("mining")) continue; // one big one is enough
                    Tribesperson leader = spawn(h, kind, big);
                    List<Tribesperson> people = members(level, leader);
                    String what = kind + (big ? " (big)" : "");
                    h.assertTrue(people.size() >= 4 && people.size() <= 5, what + ": 4-5 people, got " + people.size());
                    h.assertTrue(leader.tribe().temperament == Tribe.Temperament.TRADERS, what + ": of a trading tribe");
                    h.assertTrue(leader.role() == TribeRole.TRADER && leader.getMainHandItem().is(com.mechanical.Mechanical.WALKING_STAFF.get())
                            && leader.getOffhandItem().is(Coins.COIN_PURSE.get()), what + ": the leader holds a staff and a Coin Purse");
                    h.assertTrue(people.stream().allMatch(t -> entry.getKey().equals(t.caravanKind())), what + ": all dressed for the kind");
                    h.assertTrue(people.stream().filter(t -> t.role() == TribeRole.HANDLER).count() == 1, what + ": one handler");
                    List<Tribesperson> guards = people.stream().filter(t -> t.role() == TribeRole.GUARD).toList();
                    h.assertTrue(guards.size() >= 2 && guards.size() <= 3, what + ": 2-3 guards, got " + guards.size());
                    for (Tribesperson g : guards) {
                        h.assertTrue(g.isArmed() && g.getAttributeValue(Attributes.ARMOR) >= Caravans.GUARD_ARMOR, what + ": guards armed and armoured");
                        h.assertTrue(g.getItemBySlot(EquipmentSlot.HEAD).isEmpty() && g.getItemBySlot(EquipmentSlot.CHEST).isEmpty(),
                                what + ": no helmet, no leather");
                    }
                    CaravanTrade.Stock stock = leader.stock;
                    h.assertTrue(stock != null && stock.kind.equals(entry.getKey()) && stock.big == big && stock.purse > 0, what + ": goods and a purse");
                    long sales = stock.deals.stream().filter(CaravanTrade.Deal::sale).count();
                    h.assertTrue(sales >= (big ? Caravans.BIG_SALES : Caravans.SALES), what + ": enough goods for sale, got " + sales);
                    Tribesperson handler = role(people, TribeRole.HANDLER);
                    List<AbstractHorse> animals = animals(level, handler);
                    h.assertTrue(animals.size() == (big ? 2 : 1), what + ": " + (big ? 2 : 1) + " animals led by the handler, got " + animals.size());
                    boolean camels = entry.getValue().camels();
                    h.assertTrue(animals.stream().allMatch(a -> (a instanceof com.mechanical.wildlife.desert.TameCamel) == camels),
                            what + ": " + (camels ? "camels" : "horses"));
                    if (big) {
                        List<WagonEntity> wagons = level.getEntitiesOfClass(WagonEntity.class, new AABB(handler.blockPosition()).inflate(12), w -> w.isCaravan());
                        h.assertTrue(wagons.size() == 1 && animals.stream().allMatch(wagons.get(0)::inTeam) && !wagons.get(0).cargo().isEmpty(),
                                what + ": a laden Wagon drawn by the pair");
                        h.assertTrue(stock.deals.stream().anyMatch(d -> d.sale() && d.goods().is(Items.RAW_GOLD)),
                                what + ": a big caravan has the rare goods");
                    } else {
                        List<CartEntity> carts = level.getEntitiesOfClass(CartEntity.class, new AABB(handler.blockPosition()).inflate(12), CartEntity::isCaravan);
                        h.assertTrue(carts.size() == 1 && carts.get(0).harness() == animals.get(0), what + ": a cart behind the animal");
                        h.assertTrue(!carts.get(0).cargo().isEmpty() || !carts.get(0).getPassengers().isEmpty(), what + ": the cart carries its cargo (or a trophy)");
                    }
                    people.forEach(Caravans::leave);
                    h.assertTrue(level.getEntitiesOfClass(com.mechanical.TravoisEntity.class, new AABB(handler.blockPosition()).inflate(16)).isEmpty()
                            && animals.stream().allMatch(AbstractHorse::isRemoved), what + ": animals and carts leave with the caravan");
                }
            }
            h.succeed();
        });
    }

    private static void still(GameTestHelper h) {
        guarded(h, () -> {
            ServerLevel level = h.getLevel();
            Tribesperson leader = spawn(h, "woodsman", false);
            List<Tribesperson> people = members(level, leader);
            Tribesperson handler = role(people, TribeRole.HANDLER);
            AbstractHorse horse = animals(level, handler).get(0);
            Vec3 stand = handler.position(), graze = horse.position();
            h.runAfterDelay(300, () -> guarded(h, () -> {
                h.assertTrue(handler.position().distanceTo(stand) < 1.5, "the handler stands still, moved " + handler.position().distanceTo(stand));
                h.assertTrue(horse.position().multiply(1, 0, 1).distanceTo(graze.multiply(1, 0, 1)) < 1.0 && horse.getLeashHolder() == handler,
                        "the horse grazes where it stands, moved " + horse.position().distanceTo(graze));
                h.assertTrue(leader.distanceTo(handler) <= CaravanGoal.LEADER_RANGE + 2, "the leader keeps near the handler, at " + leader.distanceTo(handler));
                for (Tribesperson g : people)
                    if (g.role() == TribeRole.GUARD) h.assertTrue(g.distanceTo(leader) <= CaravanGoal.GUARD_MAX + 4, "a guard keeps round the leader, at " + g.distanceTo(leader));
                people.forEach(Caravans::leave);
                h.succeed();
            }));
        });
    }

    private static void guards(GameTestHelper h) {
        guarded(h, () -> {
            ServerLevel level = h.getLevel();
            Tribesperson leader = spawn(h, "hunting", false);
            List<Tribesperson> people = members(level, leader);
            Tribesperson handler = role(people, TribeRole.HANDLER);
            AbstractHorse horse = animals(level, handler).get(0);
            net.minecraft.world.entity.player.Player thief = h.makeMockPlayer(GameType.SURVIVAL);
            thief.setPos(horse.position().add(1, 0, 0));
            h.assertFalse(leader.isAngryAt(thief), "a stranger is welcome at first");
            horse.hurtServer(level, level.damageSources().playerAttack(thief), 1.0F);
            h.assertTrue(leader.isAngryAt(thief), "hitting their horse angers the caravan");
            for (Tribesperson g : people)
                if (g.role() == TribeRole.GUARD) h.assertTrue(g.getTarget() == thief, "the guards go for whoever hit the horse");
            // the cart too
            net.minecraft.world.entity.player.Player other = h.makeMockPlayer(GameType.SURVIVAL);
            CartEntity cart = level.getEntitiesOfClass(CartEntity.class, new AABB(handler.blockPosition()).inflate(12), CartEntity::isCaravan).get(0);
            other.setPos(cart.position().add(1, 0, 0));
            cart.hurtServer(level, level.damageSources().playerAttack(other), 1.0F);
            h.assertTrue(leader.isAngryAt(other) && cart.isAlive(), "hitting their cart angers them, and it doesn't break");
            people.forEach(Caravans::leave);
            h.succeed();
        });
    }

    private static void purse(GameTestHelper h) {
        guarded(h, () -> {
            ServerLevel level = h.getLevel();
            Tribesperson leader = spawn(h, "mining", false);
            List<Tribesperson> people = members(level, leader);
            // a known purse and stock: torches for sale (3 copper, 2 of them), raw copper bought (6 copper, many)
            leader.stock = new CaravanTrade.Stock(leader.stock.kind, false, 10, List.of(new CaravanTrade.Deal(new ItemStack(Items.TORCH), 3, 2, true),
                    new CaravanTrade.Deal(new ItemStack(Items.RAW_COPPER), 6, 8, false)));
            net.minecraft.world.entity.player.Player p = h.makeMockPlayer(GameType.SURVIVAL);
            p.setPos(leader.position().add(1, 0, 0));
            p.getInventory().add(new ItemStack(Items.RAW_COPPER, 4));
            h.assertTrue(CaravanTrade.trade(p, leader, 1), "sells raw copper to the leader");
            h.assertTrue(leader.stock.purse == 4 && Coins.worth(p) == 6, "his purse paid 6: purse " + leader.stock.purse + ", player " + Coins.worth(p));
            h.assertFalse(CaravanTrade.trade(p, leader, 1), "his purse (4) can't pay 6 more");
            h.assertTrue(p.getInventory().countItem(Items.RAW_COPPER) == 3, "the refused sale took nothing");
            // pay with a silver coin: change in copper
            p.getInventory().add(new ItemStack(Coins.SILVER_COIN.get()));
            h.assertTrue(CaravanTrade.trade(p, leader, 0), "buys a torch");
            h.assertTrue(p.getInventory().countItem(Items.TORCH) == 1 && Coins.worth(p) == 13 && leader.stock.purse == 7,
                    "3 copper paid (worth now " + Coins.worth(p) + ", purse " + leader.stock.purse + ")");
            h.assertTrue(CaravanTrade.trade(p, leader, 1), "his purse, filled by the sale, buys again");
            h.assertTrue(CaravanTrade.trade(p, leader, 0), "the second torch");
            h.assertFalse(CaravanTrade.trade(p, leader, 0), "the torches ran out (stock 2)");
            h.assertTrue(leader.stock.deals.get(0).stock() == 0 && p.getInventory().countItem(Items.TORCH) == 2, "two torches, none left");
            p.getInventory().clearContent();
            h.assertFalse(CaravanTrade.trade(p, leader, 1), "nothing to sell");
            people.forEach(Caravans::leave);
            h.succeed();
        });
    }

    private static void arrive(GameTestHelper h) {
        guarded(h, () -> {
            ServerLevel level = h.getLevel();
            BlockPos stop = h.absolutePos(new BlockPos(4, 1, 8));
            Tribesperson leader = Caravans.arrive(level, stop, stop.offset(10, 0, 0), Identifier.fromNamespaceAndPath(MODID, "foraging"), false, 12);
            h.assertTrue(leader != null && leader.caravanPhase == Caravans.ARRIVING, "the caravan sets out");
            List<Tribesperson> people = members(level, leader);
            Tribesperson handler = role(people, TribeRole.HANDLER);
            AbstractHorse horse = animals(level, handler).get(0);
            h.assertTrue(handler.distanceToSqr(Vec3.atBottomCenterOf(stop)) > 8 * 8, "it starts down the road, at " + h.relativePos(handler.blockPosition()));
            h.succeedWhen(() -> guarded(h, () -> {
                h.assertTrue(handler.caravanPhase == Caravans.STOPPED, "still on the road, handler at " + handler.blockPosition());
                h.assertTrue(handler.position().multiply(1, 0, 1).distanceToSqr(Vec3.atBottomCenterOf(stop).multiply(1, 0, 1)) < 4 * 4, "stopped at its stop " + h.relativePos(stop) + ", handler at "
                        + h.relativePos(handler.blockPosition()) + ", camp " + h.relativePos(handler.caravanCamp));
                h.assertTrue(people.stream().allMatch(t -> t.caravanPhase == Caravans.STOPPED), "all of them stopped");
                h.assertTrue(horse.getLeashHolder() == handler && horse.distanceTo(handler) < 5, "the horse came with the handler");
                CartEntity cart = level.getEntitiesOfClass(CartEntity.class, horse.getBoundingBox().inflate(8), CartEntity::isCaravan).get(0);
                h.assertTrue(cart.harness() == horse, "the cart came behind the horse");
                people.forEach(Caravans::leave);
            }));
        });
    }

    private CaravanTest() {}
}
