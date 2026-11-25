package com.mechanical;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
 * Game tests for vehicles and hauling: a hitched horse's turn and speed limits, two players dragging one body, which
 * bodies are too heavy to carry, wagon and cart chests (any hand, the one aimed at), hitching from the saddle.
 */
@EventBusSubscriber(modid = MODID)
public final class VehfixTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final List<String> NAMES = List.of("vehfix_turn", "vehfix_drag", "vehfix_heavy", "vehfix_chest", "vehfix_saddle");

    static {
        TEST_FUNCTIONS.register("vehfix_turn", () -> VehfixTest::turn);
        TEST_FUNCTIONS.register("vehfix_drag", () -> VehfixTest::drag);
        TEST_FUNCTIONS.register("vehfix_heavy", () -> VehfixTest::heavy);
        TEST_FUNCTIONS.register("vehfix_chest", () -> VehfixTest::chest);
        TEST_FUNCTIONS.register("vehfix_saddle", () -> VehfixTest::saddle);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "vehfix"), new TestEnvironmentDefinition.AllOf(List.of()));
        for (String name : NAMES) {
            Identifier id = Identifier.fromNamespaceAndPath(MODID, name);
            event.registerTest(id, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id), TestArea.of(env, 80, true)));
        }
    }

    /** A rider swinging round: the hitched horse comes about slowly (not at once) and no faster than the hitched speed. */
    private static void turn(GameTestHelper h) {
        floor(h);
        ServerLevel level = h.getLevel();
        Vec3 at = h.absoluteVec(new Vec3(8.5, 1, 5.5));
        WagonEntity wagon = wagon(level, at);
        AbstractHorse horse = horse(level, at.add(0, 0, WagonEntity.LEAD));
        horse.setNoAi(false);
        wagon.setHarness(horse);
        Player rider = h.makeMockPlayer(GameType.SURVIVAL);
        rider.setPos(horse.position());
        h.assertTrue(rider.startRiding(horse, true, true) && horse.getControllingPassenger() == rider, "the rider is up");
        rider.setYRot(180);
        h.runAfterDelay(10, () -> {
            float turned = Math.abs(Mth.wrapDegrees(horse.getYRot()));
            h.assertTrue(turned > 5 && turned < 45, "in 10 ticks the standing horse turns a little, not round at once: " + turned);
            h.assertTrue(HorseWork.riddenSpeed(horse, 0.5F) == HorseWork.HITCHED_SPEED, "hitched speed is capped");
            wagon.setHarness(null);
            wagon.discard();
            h.assertTrue(HorseWork.riddenSpeed(horse, 0.5F) == 0.5F, "a free horse is not");
        });
        h.runAfterDelay(13, () -> {
            h.assertTrue(Math.abs(Mth.wrapDegrees(horse.getYRot() - 180)) < 1, "free, it turns at once: " + horse.getYRot());
            rider.stopRiding();
            horse.discard();
            h.succeed();
        });
    }

    /** Two on one body: far ahead it never jumps; on opposite sides it doesn't spin; neither can walk off from it. */
    private static void drag(GameTestHelper h) {
        floor(h);
        ServerLevel level = h.getLevel();
        Vec3 at = h.absoluteVec(new Vec3(8.5, 1, 8.5));
        CarcassEntity body = body(level, net.minecraft.world.entity.EntityTypes.HORSE, at);
        Player a = h.makeMockPlayer(GameType.SURVIVAL), b = h.makeMockPlayer(GameType.SURVIVAL);
        for (Player p : List.of(a, b)) {
            p.setPos(at.add(1, 0, 0));
            CarcassHauling.pickUpOrLetGo(p, body);
        }
        h.assertTrue(body.haulerCount() == 2, "both hold it");
        // opposite sides, shuffling a little: the body settles instead of spinning
        float turned = 0;
        double fastest = 0, off = 0;
        for (int t = 0; t < 200; t++) {
            double wobble = (t / 3) % 2 == 0 ? 0.4 : -0.4;
            a.setPos(at.add(3.5, 0, wobble));
            b.setPos(at.add(-3.5, 0, 0));
            Vec3 was = body.position();
            float yaw = body.getYRot();
            CarcassHauling.follow(body);
            fastest = Math.max(fastest, body.position().subtract(was).horizontalDistance());
            if (t >= 40) turned += Math.abs(Mth.wrapDegrees(body.getYRot() - yaw));
            off = Math.max(off, body.position().subtract(at).horizontalDistance());
        }
        h.assertTrue(turned < 45, "pulled both ways it doesn't spin (" + turned + " degrees in 160 ticks)");
        h.assertTrue(off < 3, "it stays between them (" + off + ")");
        // both far off the same way: it comes after them at a capped speed
        a.setPos(at.add(8, 0, 0));
        b.setPos(at.add(8, 0, 1));
        for (int t = 0; t < 10; t++) {
            Vec3 was = body.position();
            CarcassHauling.follow(body);
            fastest = Math.max(fastest, body.position().subtract(was).horizontalDistance());
        }
        h.assertTrue(fastest <= 0.26, "capped speed: " + fastest);
        // tethered, though two are enough to move it
        Vec3 grip = body.gripPoint(body.slotOf(a), 1.0F);
        a.setPos(grip.x + 8, a.getY(), grip.z);
        a.xo = grip.x + 2;
        a.zo = grip.z;
        CarcassHauling.tether(a, body);
        double d = a.position().subtract(grip).horizontalDistance();
        h.assertTrue(d <= CarcassHauling.GRIP_REACH + CarcassHauling.TETHER_SLACK + 0.01, "held to the body: " + d);
        body.discard();
        h.succeed();
    }

    /** Pig, cow, boar, red deer and up are dragged and never hung on a horse; sheep, goat, mouflon, roe deer are carried. */
    private static void heavy(GameTestHelper h) {
        floor(h);
        ServerLevel level = h.getLevel();
        Vec3 at = h.absoluteVec(new Vec3(8.5, 1, 8.5));
        AbstractHorse horse = horse(level, at.add(3, 0, 0));
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(at);
        List<String> carried = List.of("minecraft:sheep", "minecraft:goat", "mechanical:mouflon", "mechanical:roe_deer", "minecraft:rabbit");
        for (String id : List.of("minecraft:pig", "minecraft:cow", "mechanical:wild_boar", "mechanical:red_deer", "mechanical:reindeer",
                "mechanical:aurochs", "minecraft:sheep", "minecraft:goat", "mechanical:mouflon", "mechanical:roe_deer", "minecraft:rabbit")) {
            boolean drag = !carried.contains(id);
            CarcassEntity body = body(level, BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(id)), at);
            h.assertTrue(body.isDragged() == drag, id + (drag ? " can only be dragged" : " can be carried"));
            body.addHauler(player);
            h.assertTrue(HorseWork.sling(player, body, horse) != drag, id + (drag ? " is not hung on a horse" : " hangs on a horse"));
            body.discard();
        }
        horse.discard();
        h.succeed();
    }

    /** A chest opens whatever is in the hand, and it is the one looked at, not the one the click entered the wagon's box over. */
    private static void chest(GameTestHelper h) {
        floor(h);
        ServerLevel level = h.getLevel();
        Vec3 at = h.absoluteVec(new Vec3(8.5, 1, 8.5));
        WagonEntity wagon = wagon(level, at);
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(at.add(5, 0, 0));
        for (int i = 0; i < WagonEntity.SLOTS; i++) {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CHEST));
            wagon.interact(player, InteractionHand.MAIN_HAND, new Vec3(0, 1, 0));
        }
        // facing +z the wagon's right is -x: aim from high on one side, over the near chest, at each far chest
        for (int cell = 0; cell < WagonEntity.SLOTS; cell++) {
            double side = cell % 2 == 0 ? -1 : 1;
            Vec3 target = at.add(-0.3 * side, WagonEntity.BED + 0.6, WagonEntity.slotAlong(cell));
            Vec3 eye = at.add(2.5 * side, WagonEntity.BED + 1.8, WagonEntity.slotAlong(cell));
            player.setPos(eye.subtract(0, player.getEyeHeight(), 0));
            player.lookAt(EntityAnchorArgument.Anchor.EYES, target);
            h.assertTrue(wagon.slotLookedAt(player) == cell, "aimed at cell " + cell + ", got " + wagon.slotLookedAt(player));
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        h.assertTrue(wagon.interact(player, InteractionHand.MAIN_HAND, new Vec3(0, 1, 0)) == InteractionResult.SUCCESS, "a wagon chest opens with a stick in hand");
        CartEntity cart = Carting.CART.get().create(level, EntitySpawnReason.EVENT);
        cart.setPos(at.add(0, 0, -5));
        level.addFreshEntity(cart);
        h.assertTrue(cart.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO) == InteractionResult.PASS, "a cart without a chest leaves the stick alone");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CHEST));
        cart.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
        h.assertTrue(cart.hasChest(), "a chest is mounted");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        h.assertTrue(cart.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO) == InteractionResult.SUCCESS, "a cart chest opens with a stick in hand");
        wagon.discard();
        cart.discard();
        h.succeed();
    }

    /** The grab key from the saddle hitches the ridden horse to the wagon, cart or plough beside it, and nothing to a travois. */
    private static void saddle(GameTestHelper h) {
        floor(h);
        ServerLevel level = h.getLevel();
        for (EntityType<? extends TravoisEntity> type : List.<EntityType<? extends TravoisEntity>>of(Carting.WAGON.get(), Carting.CART.get(),
                Carting.PLOUGH.get(), CarcassHauling.TRAVOIS.get())) {
            Vec3 at = h.absoluteVec(new Vec3(8.5, 1, 6.5));
            TravoisEntity vehicle = type.create(level, EntitySpawnReason.EVENT);
            vehicle.setPos(at);
            level.addFreshEntity(vehicle);
            AbstractHorse horse = horse(level, at.add(1.8, 0, 3)); // ridden up beside its front, looking away from it
            Player rider = h.makeMockPlayer(GameType.SURVIVAL);
            rider.setPos(horse.position());
            rider.startRiding(horse, true, true);
            rider.setXRot(-30);
            TravoisEntity.grabKey(rider);
            boolean takes = type != CarcassHauling.TRAVOIS.get();
            h.assertTrue((vehicle.harness() == horse) == takes && vehicle.harness() != rider, type + ": hitched from the saddle (" + vehicle.harness() + ")");
            rider.stopRiding();
            horse.discard();
            vehicle.discard();
        }
        h.succeed();
    }

    private static void floor(GameTestHelper h) {
        for (int x = 0; x <= 16; x++) for (int z = 0; z <= 16; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
    }

    private static WagonEntity wagon(ServerLevel level, Vec3 at) {
        WagonEntity wagon = Carting.WAGON.get().create(level, EntitySpawnReason.EVENT);
        wagon.setPos(at);
        wagon.setYRot(0);
        level.addFreshEntity(wagon);
        return wagon;
    }

    private static AbstractHorse horse(ServerLevel level, Vec3 at) {
        AbstractHorse horse = com.mechanical.wildlife.Wildlife.TAME_HORSE.get().create(level, EntitySpawnReason.EVENT);
        horse.setPos(at);
        horse.setYRot(0);
        horse.setNoAi(true);
        horse.setTamed(true);
        level.addFreshEntity(horse);
        return horse;
    }

    private static CarcassEntity body(ServerLevel level, EntityType<?> type, Vec3 at) {
        var dead = (LivingEntity) type.create(level, EntitySpawnReason.EVENT);
        dead.setPos(at);
        CarcassEntity body = CarcassEntity.of(level, dead);
        body.setYRot(0);
        level.addFreshEntity(body);
        return body;
    }
}
