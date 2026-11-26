package com.mechanical.skills;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Game test for the skill core: the XP curve, fresh players, multi-level gains. */
@EventBusSubscriber(modid = MODID)
public final class SkillsTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> TEST = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "skills"));

    static {
        TEST_FUNCTIONS.register("skills", () -> SkillsTest::test);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "skills"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "skills"),
                new FunctionGameTestInstance(TEST, com.mechanical.TestArea.of(env, 20, true)));
    }

    private static void test(GameTestHelper h) {
        try {
            curve(h);
            ServerPlayer p = net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),
                    new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "skills"));
            freshPlayer(h, p);
            multiLevel(h, p);
            h.succeed();
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw h.assertionException(e + " at " + java.util.Arrays.toString(java.util.Arrays.copyOf(e.getStackTrace(), 6)));
        }
    }

    private static void curve(GameTestHelper h) {
        int[][] pts = {{1, 0}, {2, 100}, {10, 2700}, {50, 63700}, {100, 252450}};
        for (int[] pt : pts) h.assertTrue(Skills.xpForLevel(pt[0]) == pt[1], "xpForLevel(" + pt[0] + ")=" + Skills.xpForLevel(pt[0]));
        h.assertTrue(Skills.level(99) == 1 && Skills.level(100) == 2, "level 99/100");
        h.assertTrue(Skills.level(252450) == 100 && Skills.level(10_000_000) == 100, "level capped at 100");
    }

    private static void freshPlayer(GameTestHelper h, ServerPlayer p) {
        for (Skill s : Skill.values()) {
            h.assertTrue(Skills.level(p, s) == 1 && Skills.xp(p, s) == 0, "fresh " + s);
            h.assertTrue(!s.icon().isEmpty(), "icon " + s);
        }
    }

    private static void multiLevel(GameTestHelper h, ServerPlayer p) {
        Skills.addXp(p, Skill.MINING, 2700);
        h.assertTrue(Skills.level(p, Skill.MINING) == 10, "2700 xp is level 10");
        h.assertTrue(Skills.level(p, Skill.FARMING) == 1, "other skills untouched");
        for (int i = 0; i < 200; i++) h.assertTrue(!Skills.roll(p, Skill.MINING, 11, 1.0), "roll below unlock is false");
        h.assertTrue(Skills.roll(p, Skill.MINING, 10, 1.0), "chance 10 x 1.0 always hits at unlock");
    }
}
