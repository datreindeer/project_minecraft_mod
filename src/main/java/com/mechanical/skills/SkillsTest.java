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
            miningXp(h, p);
            stationXp(h, p);
            huntingXp(h, p);
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

    private static void miningXp(GameTestHelper h, ServerPlayer p) {
        var level = h.getLevel();
        net.minecraft.core.BlockPos ore = h.absolutePos(new net.minecraft.core.BlockPos(2, 1, 2)), stone = ore.east();
        level.setBlockAndUpdate(ore, net.minecraft.world.level.block.Blocks.IRON_ORE.defaultBlockState());
        level.setBlockAndUpdate(stone, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE));
        int before = Skills.xp(p, Skill.MINING);
        p.gameMode.destroyBlock(ore);
        h.assertTrue(Skills.xp(p, Skill.MINING) == before + 10, "ore gives 10 Mining, got " + (Skills.xp(p, Skill.MINING) - before));
        p.gameMode.destroyBlock(stone);
        h.assertTrue(Skills.xp(p, Skill.MINING) == before + 11, "stone gives 1 Mining, got " + (Skills.xp(p, Skill.MINING) - before));
    }

    private static void stationXp(GameTestHelper h, ServerPlayer p) {
        var menu = new com.mechanical.FletchingMenu(1, p.getInventory(), net.minecraft.world.inventory.ContainerLevelAccess.create(h.getLevel(), h.absolutePos(new net.minecraft.core.BlockPos(1, 1, 1))));
        var stick = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK);
        var result = menu.slots.stream().filter(s -> !(s.container instanceof net.minecraft.world.entity.player.Inventory) && !s.mayPlace(stick)).findFirst().orElseThrow();
        int before = Skills.xp(p, Skill.FLETCHING);
        result.onTake(p, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW));
        h.assertTrue(Skills.xp(p, Skill.FLETCHING) == before + 8, "fletching result gives 8, got " + (Skills.xp(p, Skill.FLETCHING) - before));
    }

    private static void huntingXp(GameTestHelper h, ServerPlayer p) {
        var level = h.getLevel();
        net.minecraft.world.entity.animal.Animal cow = h.spawn(net.minecraft.world.entity.EntityTypes.COW, new net.minecraft.core.BlockPos(4, 1, 4));
        int before = Skills.xp(p, Skill.HUNTING);
        cow.hurtServer(level, level.damageSources().playerAttack(p), 1000F);
        h.assertTrue(Skills.xp(p, Skill.HUNTING) == before + 25, "killing a cow gives 25 Hunting, got " + (Skills.xp(p, Skill.HUNTING) - before));
    }
}
