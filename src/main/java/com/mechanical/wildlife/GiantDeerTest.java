package com.mechanical.wildlife;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.pig.Pig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Game test for the Giant Deer's antler sweep: two attackers in front of it are hit and thrown outward; one behind it,
 * a fawn in front and a fellow giant deer in front are left alone.
 */
@EventBusSubscriber(modid = MODID)
public final class GiantDeerTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> TEST = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "giant_deer_sweep"));

    static {
        TEST_FUNCTIONS.register("giant_deer_sweep", () -> GiantDeerTest::test);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "giant_deer_sweep"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "giant_deer_sweep"),
                new FunctionGameTestInstance(TEST, com.mechanical.TestArea.of(env, 20, true)));
    }

    private static <T extends Mob> T spawn(GameTestHelper h, EntityType<T> type, double x, double z, GiantDeer deer) {
        T mob = h.spawn(type, new net.minecraft.world.phys.Vec3(x, 1, z));
        mob.setNoAi(true);
        mob.setTarget(deer);
        return mob;
    }

    private static void test(GameTestHelper h) {
        // deer at the middle of the floor, facing +z (yaw 0)
        double cx = 7.5, cz = 7.5;
        GiantDeer deer = h.spawn(Wildlife.GIANT_DEER.get(), new net.minecraft.world.phys.Vec3(cx, 1, cz));
        deer.setNoAi(true);
        deer.setYRot(0);
        Pig left = spawn(h, EntityTypes.PIG, cx - 2, cz + 2.5, deer);
        Pig right = spawn(h, EntityTypes.PIG, cx + 2, cz + 2.5, deer);
        Pig behind = spawn(h, EntityTypes.PIG, cx, cz - 3, deer);
        RedDeer fawn = spawn(h, Wildlife.RED_DEER.get(), cx, cz + 3, deer);
        fawn.setBaby(true);
        GiantDeer herd = spawn(h, Wildlife.GIANT_DEER.get(), cx + 3.5, cz + 1, deer);
        Pig bystander = h.spawn(EntityTypes.PIG, new net.minecraft.world.phys.Vec3(cx - 3, 1, cz + 1)); // not after the deer
        bystander.setNoAi(true);

        int hit = deer.sweep(h.getLevel());
        h.assertTrue(hit == 2, "the sweep hits exactly the two attackers in front (hit " + hit + ")");
        for (Pig p : new Pig[] {left, right}) {
            h.assertTrue(p.getHealth() <= p.getMaxHealth() - GiantDeer.SWEEP_DAMAGE + 0.01F, "an attacker in front is hurt (" + p.getHealth() + ")");
            var v = p.getDeltaMovement();
            double outward = (p.getX() - deer.getX()) * v.x + (p.getZ() - deer.getZ()) * v.z;
            h.assertTrue(outward > 0.5 && v.horizontalDistance() > 0.8, "an attacker in front is thrown outward (" + v + ")");
        }
        h.assertTrue(behind.getHealth() == behind.getMaxHealth(), "the attacker behind is not hit");
        h.assertTrue(fawn.getHealth() == fawn.getMaxHealth(), "a fawn is never hit");
        h.assertTrue(herd.getHealth() == herd.getMaxHealth(), "another giant deer is never hit");
        h.assertTrue(bystander.getHealth() == bystander.getMaxHealth(), "a mob that isn't fighting it is not hit");

        // the rear-up stomp catches the one behind it
        int stomped = deer.stomp(h.getLevel());
        h.assertTrue(stomped >= 1 && behind.getHealth() < behind.getMaxHealth(), "the stomp hits the attacker behind");
        h.succeed();
    }

    private GiantDeerTest() {}
}
