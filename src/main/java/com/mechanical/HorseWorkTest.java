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
    private static final String[] NAMES = {"horse_hitch", "horse_team_rigid", "horse_stamina", "horse_exhaustion", "horse_side_carcass", "horse_hitched_stand", "wagon_slabs_reverse"};

    static {
        TEST_FUNCTIONS.register("horse_hitch", () -> HorseWorkTest::hitch);
        TEST_FUNCTIONS.register("horse_team_rigid", () -> HorseWorkTest::rigid);
        TEST_FUNCTIONS.register("horse_stamina", () -> HorseWorkTest::stamina);
        TEST_FUNCTIONS.register("horse_exhaustion", () -> HorseWorkTest::exhaustion);
        TEST_FUNCTIONS.register("horse_side_carcass", () -> HorseWorkTest::sideCarcass);
        TEST_FUNCTIONS.register("horse_hitched_stand", () -> HorseWorkTest::hitchedStand);
        TEST_FUNCTIONS.register("wagon_slabs_reverse", () -> HorseWorkTest::slabsAndReverse);
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
        wagon.grab(player);
        h.assertTrue(wagon.harness() == a && a.getLeashHolder() == null, "the led horse is locked in the lead place, its lead off");
        h.assertTrue(wagon.partner() == null, "one horse: the second place is free");
        b.setLeashedTo(player, true);
        wagon.grab(player);
        h.assertTrue(wagon.harness() == a && wagon.partner() == b && b.getLeashHolder() == null, "the second led horse goes in beside it");
        wagon.grab(player);
        h.assertTrue(wagon.harness() == a && wagon.partner() == b, "grabbing again does not unhitch the team");
        c.setLeashedTo(player, true);
        wagon.grab(player);
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

    /**
     * Hitched horses nobody rides stand still, with their AI on and told to walk off: a wagon's pair and a cart's horse, and
     * the vehicles stay. A wagon saved and loaded finds its pair again, the second horse on the side it was yoked. Let out of
     * the traces, the same horse does walk off (the test is fair).
     */
    private static void hitchedStand(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        floor(h);
        Vec3 at = h.absoluteVec(new Vec3(5.5, 1, 3.5));
        WagonEntity wagon = wagon(level, at);
        AbstractHorse a = horse(level, at.add(WagonEntity.PAIR / 2, 0, WagonEntity.LEAD)), b = horse(level, at.add(-WagonEntity.PAIR / 2, 0, WagonEntity.LEAD));
        CartEntity cart = Carting.CART.get().create(level, net.minecraft.world.entity.EntitySpawnReason.EVENT);
        Vec3 cartAt = h.absoluteVec(new Vec3(11.5, 1, 3.5));
        cart.setPos(cartAt);
        level.addFreshEntity(cart);
        AbstractHorse c = horse(level, cartAt.add(0, 0, CartEntity.LEAD_HORSE));
        for (AbstractHorse horse : List.of(a, b, c)) {
            horse.setNoAi(false); // a real horse's wandering and walking
            horse.setYRot(0);
        }
        wagon.setHarness(a);
        wagon.setPartner(b); // b stands on a's right (facing +z, -x is right)
        cart.setHarness(c);
        Vec3 a0 = a.position(), b0 = b.position(), c0 = c.position(), w0 = wagon.position(), k0 = cart.position();
        for (int t = 2; t <= 40; t += 2) h.runAfterDelay(t, () -> { // something keeps telling them to walk off
            for (AbstractHorse horse : List.of(a, b, c)) horse.getNavigation().moveTo(horse.getX(), horse.getY(), horse.getZ() + 8, 1.5);
        });
        h.runAfterDelay(45, () -> {
            for (Vec3[] p : List.of(new Vec3[] {a0, a.position()}, new Vec3[] {b0, b.position()}, new Vec3[] {c0, c.position()},
                    new Vec3[] {w0, wagon.position()}, new Vec3[] {k0, cart.position()}))
                h.assertTrue(p[0].distanceTo(p[1]) < 0.3, "hitched and unridden, nothing walks off (" + p[0] + " -> " + p[1] + ")");
            h.assertTrue(wagon.harness() == a && wagon.partner() == b && cart.harness() == c, "still hitched");
            // saved and loaded: the team comes back by UUID, the second horse on the right still
            net.minecraft.world.level.storage.TagValueOutput out = net.minecraft.world.level.storage.TagValueOutput.createWithoutContext(net.minecraft.util.ProblemReporter.DISCARDING);
            wagon.saveWithoutId(out);
            WagonEntity copy = Carting.WAGON.get().create(level, net.minecraft.world.entity.EntitySpawnReason.EVENT);
            copy.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(), out.buildResult()));
            wagon.discard();
            level.addFreshEntity(copy);
            held[0] = copy;
            // out of the traces (sneak + right-click on it), the cart's horse may go where it is told
            Player player = h.makeMockPlayer(GameType.SURVIVAL);
            player.setShiftKeyDown(true);
            player.interactOn(c, InteractionHand.MAIN_HAND, Vec3.ZERO);
            h.assertTrue(cart.harness() == null, "sneak + right-click on the horse unhitched it from the cart");
        });
        for (int t = 46; t <= 70; t += 2) h.runAfterDelay(t, () -> c.getNavigation().moveTo(c.getX(), c.getY(), c.getZ() + 8, 1.5));
        h.runAfterDelay(72, () -> {
            WagonEntity copy = (WagonEntity) held[0];
            h.assertTrue(copy.harness() == a && copy.partner() == b, "the loaded wagon found its pair (" + copy.harness() + ", " + copy.partner() + ")");
            h.assertTrue(gap(a, b, true) < 0.05, "the second horse is still on the right (off by " + gap(a, b, true) + ")");
            h.assertTrue(c.position().distanceTo(c0) > 1, "unhitched, the horse walks where it is told (" + c.position().distanceTo(c0) + ")");
            discard(List.of(copy, cart, a, b, c));
            h.succeed();
        });
    }

    private static final Entity[] held = new Entity[1];

    /**
     * The pair walks up a slab step and a full block onto a raised floor: the wagon follows up both, still at its lead; then
     * the horses back up and push it back (the pole is rigid), and a turn swings the bed after them, never past MAX_STEER.
     */
    private static void slabsAndReverse(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        floor(h);
        for (int x = 3; x <= 13; x++) {
            for (int z = 8; z <= 9; z++) h.setBlock(new BlockPos(x, 1, z), Blocks.SMOOTH_STONE_SLAB);
            for (int z = 10; z <= 15; z++) h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
        }
        Vec3 at = h.absoluteVec(new Vec3(8.5, 1, 2.5));
        WagonEntity wagon = wagon(level, at);
        AbstractHorse a = horse(level, at.add(-WagonEntity.PAIR / 2, 0, WagonEntity.LEAD)), b = horse(level, at.add(WagonEntity.PAIR / 2, 0, WagonEntity.LEAD));
        a.setYRot(0);
        wagon.setHarness(a);
        wagon.setPartner(b);
        double floorY = at.y;
        for (int t = 1; t <= 20; t++) { // 10 blocks on: z 5.3 -> 15.3, stepping up 0.5 at z 8 and 1 at z 10
            h.runAfterDelay(t, () -> {
                Vec3 p = a.position().add(0, 0, 0.5);
                double y = floorY + (p.z - h.absoluteVec(Vec3.ZERO).z >= 10 ? 1 : p.z - h.absoluteVec(Vec3.ZERO).z >= 8 ? 0.5 : 0);
                a.setPos(p.x, y, p.z);
            });
        }
        double[] onTop = new double[1];
        h.runAfterDelay(40, () -> {
            h.assertTrue(Math.abs(wagon.getY() - (floorY + 1)) < 0.01, "the wagon came up the slab and the block (y " + wagon.getY() + ", want " + (floorY + 1) + ")");
            double d = wagon.position().subtract(a.position().add(b.position()).scale(0.5)).horizontalDistance();
            h.assertTrue(Math.abs(d - WagonEntity.LEAD) < 0.15 && wagon.harness() == a, "and is at its lead behind the pair (" + d + ")");
            onTop[0] = wagon.getZ();
        });
        for (int t = 41; t <= 44; t++) h.runAfterDelay(t, () -> a.setPos(a.position().add(0, 0, -0.25))); // backing up a block
        h.runAfterDelay(48, () -> {
            h.assertTrue(onTop[0] - wagon.getZ() > 0.9, "backing the horses pushes the wagon back (" + (onTop[0] - wagon.getZ()) + ")");
            a.setYRot(90); // a sharp turn on the spot
        });
        h.runAfterDelay(52, () -> {
            float steer = net.minecraft.util.Mth.wrapDegrees(a.getYRot() - wagon.getYRot());
            h.assertTrue(Math.abs(steer) <= WagonEntity.MAX_STEER + 0.5F, "the front axle turns at most " + WagonEntity.MAX_STEER + " degrees, the bed is dragged round (" + steer + ")");
            h.assertTrue(gap(a, b) < 0.02, "the second horse turned with the first");
            discard(List.of(wagon, a, b));
            h.succeed();
        });
    }

    private static double gap(AbstractHorse a, AbstractHorse b, boolean right) {
        return b.position().distanceTo(a.position().add(Vec3.directionFromRotation(0, a.getYRot() + (right ? 90 : -90)).scale(WagonEntity.PAIR)));
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
