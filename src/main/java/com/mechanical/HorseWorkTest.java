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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
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
 * Game tests for {@link HorseWork} and the fixed wagon team (living-world-plan D1-D4): hitching led horses locks them in
 * the team's places and sneak + right-click releases them; the pair and the wagon move as one rigid unit and nothing
 * tears loose; stamina drains faster with load and comes back with water; an exhausted horse throws its rider, won't
 * take one and gets up rested; bodies hang on a horse's two sides and come off again.
 */
@EventBusSubscriber(modid = MODID)
public final class HorseWorkTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final String[] NAMES = {"horse_hitch", "horse_team_rigid", "horse_stamina", "horse_exhaustion", "horse_side_carcass"};

    static {
        TEST_FUNCTIONS.register("horse_hitch", () -> HorseWorkTest::hitch);
        TEST_FUNCTIONS.register("horse_team_rigid", () -> HorseWorkTest::rigid);
        TEST_FUNCTIONS.register("horse_stamina", () -> HorseWorkTest::stamina);
        TEST_FUNCTIONS.register("horse_exhaustion", () -> HorseWorkTest::exhaustion);
        TEST_FUNCTIONS.register("horse_side_carcass", () -> HorseWorkTest::sideCarcass);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "horse_work"), new TestEnvironmentDefinition.AllOf(List.of()));
        for (String name : NAMES) {
            ResourceKey<Consumer<GameTestHelper>> key = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, name));
            event.registerTest(Identifier.fromNamespaceAndPath(MODID, name), new FunctionGameTestInstance(key, TestArea.of(env, 100, true)));
        }
    }

    private static void floor(GameTestHelper h) {
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
    }

    private static AbstractHorse horse(ServerLevel level, Vec3 at) {
        AbstractHorse horse = com.mechanical.wildlife.Wildlife.TAME_HORSE.get().create(level, EntitySpawnReason.EVENT);
        horse.setPos(at);
        horse.setTamed(true);
        horse.setNoAi(true); // moved by hand: wandering would leave the floor
        level.addFreshEntity(horse);
        return horse;
    }

    private static WagonEntity wagon(ServerLevel level, Vec3 at) {
        WagonEntity wagon = Carting.WAGON.get().create(level, EntitySpawnReason.EVENT);
        wagon.setPos(at);
        wagon.setYRot(0); // facing +z
        level.addFreshEntity(wagon);
        return wagon;
    }

    private static void discard(List<? extends Entity> entities) {
        for (Entity e : entities) e.discard();
    }

    /** Led horses go in the lead place, then beside it; a third stays out; sneak + right-click lets one go, the other moves up. */
    private static void hitch(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        floor(h);
        Vec3 at = h.absoluteVec(new Vec3(8.5, 1, 5.5));
        WagonEntity wagon = wagon(level, at);
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(at.add(2, 0, 0));
        AbstractHorse a = horse(level, at.add(-1, 0, 3)), b = horse(level, at.add(1, 0, 3)), c = horse(level, at.add(3, 0, 3));
        a.setLeashedTo(player, true);
        wagon.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
        h.assertTrue(wagon.harness() == a && a.getLeashHolder() == null, "the led horse is locked in the lead place, its lead off");
        h.assertTrue(wagon.partner() == null, "one horse: the second place is free");
        b.setLeashedTo(player, true);
        wagon.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
        h.assertTrue(wagon.harness() == a && wagon.partner() == b && b.getLeashHolder() == null, "the second led horse goes in beside it");
        wagon.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
        h.assertTrue(wagon.harness() == a && wagon.partner() == b, "a plain click doesn't unhitch the team any more");
        c.setLeashedTo(player, true);
        wagon.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
        h.assertTrue(!wagon.inTeam(c) && c.getLeashHolder() == player, "a full team takes no third horse");
        player.setShiftKeyDown(true);
        player.interactOn(a, InteractionHand.MAIN_HAND, Vec3.ZERO);
        h.assertTrue(wagon.harness() == b && wagon.partner() == null, "sneak + right-click released the lead horse; the second moved up");
        player.interactOn(b, InteractionHand.MAIN_HAND, Vec3.ZERO);
        h.assertTrue(wagon.harness() == null, "and the last one");
        c.dropLeash();
        discard(List.of(wagon, a, b, c));
        h.succeed();
    }

    /** The pair walks on and turns: the second horse stays exactly beside the first, the wagon at its lead; a jump doesn't tear it loose. */
    private static void rigid(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        floor(h);
        Vec3 at = h.absoluteVec(new Vec3(8.5, 1, 2.5));
        WagonEntity wagon = wagon(level, at);
        AbstractHorse a = horse(level, at.add(-WagonEntity.PAIR / 2, 0, WagonEntity.LEAD)), b = horse(level, at.add(WagonEntity.PAIR / 2, 0, WagonEntity.LEAD));
        a.setYRot(0);
        wagon.setHarness(a);
        wagon.setPartner(b);
        for (int t = 1; t <= 8; t++) h.runAfterDelay(t, () -> a.setPos(a.position().add(0, 0, 0.5)));
        h.runAfterDelay(10, () -> {
            h.assertTrue(gap(a, b) < 0.02, "the second horse is exactly beside the first (off by " + gap(a, b) + ")");
            double d = wagon.position().subtract(a.position().add(b.position()).scale(0.5)).horizontalDistance();
            h.assertTrue(Math.abs(d - WagonEntity.LEAD) < 0.15, "the wagon follows at its lead (" + d + ")");
            h.assertTrue(a.distanceTo(b) > a.getBbWidth(), "the horses don't overlap");
            a.setYRot(90); // turn
        });
        h.runAfterDelay(12, () -> {
            h.assertTrue(gap(a, b) < 0.02, "it turned with the lead horse (off by " + gap(a, b) + ")");
            a.setPos(a.position().add(0, 0, 4)); // far past where a lead would tear
        });
        h.runAfterDelay(15, () -> {
            h.assertTrue(wagon.harness() == a && wagon.partner() == b, "nothing tore loose");
            double d = wagon.position().subtract(a.position().add(b.position()).scale(0.5)).horizontalDistance();
            h.assertTrue(d < WagonEntity.LEAD + 0.5, "the wagon was pulled up behind the team (" + d + ")");
            discard(List.of(wagon, a, b));
            h.succeed();
        });
    }

    private static double gap(AbstractHorse a, AbstractHorse b) {
        return b.position().distanceTo(a.position().add(Vec3.directionFromRotation(0, a.getYRot() - 90).scale(WagonEntity.PAIR)));
    }

    /**
     * Walks {@code horse} {@code blocks} blocks along +x, a stamina check per block (the bodies on its sides moved along, as
     * their own ticks would). Call {@link HorseWork#tick} once before, so the first check doesn't count as standing.
     */
    private static void walk(AbstractHorse horse, int blocks) {
        List<CarcassEntity> sides = HorseWork.sideCarcasses(horse);
        for (int i = 0; i < blocks; i++) {
            horse.setPos(horse.position().add(1, 0, 0));
            for (CarcassEntity c : sides) c.setPos(CarcassEntity.sidePoint(horse, c.slungSide()));
            HorseWork.tick(horse);
        }
    }

    /** Two bodies on its sides make a horse tire faster; standing in shallow water and a Water Bucket fill it again. */
    private static void stamina(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        floor(h);
        AbstractHorse free = horse(level, h.absoluteVec(new Vec3(1.5, 1, 3.5))), packed = horse(level, h.absoluteVec(new Vec3(1.5, 1, 8.5)));
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        for (int side = 0; side < 2; side++) {
            CarcassEntity body = carcass(level, packed.position().add(0, 0, 2));
            HorseWork.sling(player, body, packed);
        }
        h.assertTrue(HorseWork.load(packed) > HorseWork.load(free), "the bodies count as load (" + HorseWork.load(packed) + ")");
        HorseWork.tick(free);
        HorseWork.tick(packed);
        walk(free, 12);
        walk(packed, 12);
        float lostFree = HorseWork.MAX - HorseWork.stamina(free), lostPacked = HorseWork.MAX - HorseWork.stamina(packed);
        h.assertTrue(lostFree > 0 && lostPacked > lostFree * 1.3F, "load drains it faster (" + lostFree + " / " + lostPacked + ")");
        // water: standing in it (one block deep) and a bucket
        HorseWork.setStamina(free, 10);
        BlockPos feet = free.blockPosition();
        level.setBlockAndUpdate(feet, Blocks.WATER.defaultBlockState());
        h.runAfterDelay(3, () -> {
            float before = HorseWork.stamina(free);
            HorseWork.tick(free); // standing: rests, and drinks
            h.assertTrue(HorseWork.stamina(free) - before >= HorseWork.REST + HorseWork.DRINK - 0.01F,
                    "it drinks standing in shallow water (" + before + " -> " + HorseWork.stamina(free) + ", in water " + free.isInWater() + ")");
            level.setBlockAndUpdate(feet, Blocks.AIR.defaultBlockState());
            float mid = HorseWork.stamina(free);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
            player.interactOn(free, InteractionHand.MAIN_HAND, Vec3.ZERO);
            h.assertTrue(HorseWork.stamina(free) >= mid + HorseWork.BUCKET - 0.01F && player.getMainHandItem().is(Items.BUCKET),
                    "a Water Bucket fills it (" + mid + " -> " + HorseWork.stamina(free) + "), the bucket comes back empty");
            discard(HorseWork.sideCarcasses(packed));
            discard(List.of(free, packed));
            h.succeed();
        });
    }

    /** Run to the end it throws its rider, stops and won't be mounted; rested to RISE it gets up. */
    private static void exhaustion(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        floor(h);
        AbstractHorse horse = horse(level, h.absoluteVec(new Vec3(1.5, 1, 8.5)));
        Player rider = h.makeMockPlayer(GameType.SURVIVAL);
        rider.setPos(horse.position());
        h.assertTrue(rider.startRiding(horse, true, true) && horse.getControllingPassenger() == rider, "the rider is up");
        HorseWork.tick(horse);
        HorseWork.setStamina(horse, 0.5F);
        walk(horse, 12);
        h.assertTrue(HorseWork.exhausted(horse), "it ran itself out (stamina " + HorseWork.stamina(horse) + ")");
        h.assertTrue(horse.getPassengers().isEmpty() && !rider.isPassenger(), "it threw its rider");
        h.assertTrue(horse.getAttributeValue(Attributes.MOVEMENT_SPEED) < 1e-6, "it doesn't move (" + horse.getAttributeValue(Attributes.MOVEMENT_SPEED) + ")");
        h.assertTrue(!rider.startRiding(horse, false, true), "it won't take a rider while it lies");
        HorseWork.setStamina(horse, HorseWork.RISE - HorseWork.REST);
        HorseWork.tick(horse); // standing still: rests up to RISE
        h.assertTrue(!HorseWork.exhausted(horse) && horse.getAttributeValue(Attributes.MOVEMENT_SPEED) > 0, "rested a bit, it gets up");
        discard(List.of(horse));
        h.succeed();
    }

    /** A body from the shoulders hangs on the left, the next on the right, a third is refused; sneak + right-click takes one back. */
    private static void sideCarcass(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        floor(h);
        AbstractHorse horse = horse(level, h.absoluteVec(new Vec3(8.5, 1, 8.5)));
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(horse.position().add(2, 0, 0));
        CarcassEntity[] bodies = new CarcassEntity[3];
        for (int i = 0; i < 3; i++) {
            bodies[i] = carcass(level, player.position().add(0, 1, 0));
            bodies[i].addHauler(player);
            h.assertTrue(CarcassHauling.hauled(player) == bodies[i], "carrying body " + i);
            player.interactOn(horse, InteractionHand.MAIN_HAND, Vec3.ZERO);
        }
        h.assertTrue(bodies[0].slungHorse() == horse && bodies[0].slungSide() == 0 && bodies[0].haulerId() < 0, "the first hangs on the left side, out of the hands");
        h.assertTrue(bodies[1].slungHorse() == horse && bodies[1].slungSide() == 1, "the second on the right");
        h.assertTrue(bodies[2].slungHorse() == null && CarcassHauling.hauled(player) == bodies[2], "a third doesn't fit: still on the shoulders");
        h.assertTrue(HorseWork.sideCarcasses(horse).size() == 2 && HorseWork.load(horse) >= 2 * HorseWork.SIDE_ROOM, "two bodies weigh on the horse");
        bodies[2].discard();
        h.runAfterDelay(2, () -> {
            Vec3 left = CarcassEntity.sidePoint(horse, 0);
            h.assertTrue(bodies[0].position().distanceTo(left) < 0.05, "it stays on the horse's flank (" + bodies[0].position() + " / " + left + ")");
            Player other = h.makeMockPlayer(GameType.SURVIVAL);
            other.setPos(horse.position().add(-2, 0, 0));
            other.setShiftKeyDown(true);
            bodies[0].interact(other, InteractionHand.MAIN_HAND, Vec3.ZERO);
            h.assertTrue(bodies[0].slungHorse() == null && CarcassHauling.hauled(other) == bodies[0], "sneak + right-click takes it onto the shoulders");
            h.assertTrue(HorseWork.sideCarcasses(horse).size() == 1, "one left on the horse");
            discard(List.of(bodies[0], bodies[1], horse));
            h.succeed();
        });
    }

    /** A hare's body (small: carried on the shoulders). */
    private static CarcassEntity carcass(ServerLevel level, Vec3 at) {
        var hare = EntityTypes.RABBIT.create(level, EntitySpawnReason.EVENT);
        hare.setPos(at);
        CarcassEntity body = CarcassEntity.of(level, hare);
        body.setPos(at);
        level.addFreshEntity(body);
        return body;
    }
}
