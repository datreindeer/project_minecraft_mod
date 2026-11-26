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
            placedOreXp(h, p);
            cookedXp(h, p);
            extraOre(h);
            noDupeUnstackable(h);
            perkUnlock(h);
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

    private static void placedOreXp(GameTestHelper h, ServerPlayer p) {
        var level = h.getLevel();
        net.minecraft.core.BlockPos pos = h.absolutePos(new net.minecraft.core.BlockPos(6, 1, 6)), below = pos.below();
        level.setBlockAndUpdate(below, net.minecraft.world.level.block.Blocks.COBBLESTONE.defaultBlockState());
        var ore = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_ORE);
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ore);
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(below), net.minecraft.core.Direction.UP, below, false);
        ore.useOn(new net.minecraft.world.item.context.UseOnContext(level, p, net.minecraft.world.InteractionHand.MAIN_HAND, ore, hit));
        h.assertTrue(level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.IRON_ORE), "player placed the ore");
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE));
        int before = Skills.xp(p, Skill.MINING);
        p.gameMode.destroyBlock(pos);
        h.assertTrue(Skills.xp(p, Skill.MINING) == before, "a placed ore gives no Mining XP");
        level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.IRON_ORE.defaultBlockState());
        p.gameMode.destroyBlock(pos);
        h.assertTrue(Skills.xp(p, Skill.MINING) == before + 10, "a set (generated) ore still gives 10");
    }

    private static void cookedXp(GameTestHelper h, ServerPlayer p) {
        int before = Skills.xp(p, Skill.COOKING);
        Skills.cooked(p, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ROTTEN_FLESH, 3));
        Skills.cooked(p, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK, 3));
        h.assertTrue(Skills.xp(p, Skill.COOKING) == before, "rotten flesh and non-food pay nothing");
        Skills.cooked(p, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BREAD, 2));
        h.assertTrue(Skills.xp(p, Skill.COOKING) == before + 10, "bread pays 5 each");
    }

    private static ServerPlayer fresh(GameTestHelper h, String name) {
        return net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), name));
    }

    private static void extraOre(GameTestHelper h) {
        ServerPlayer q = fresh(h, "miner");
        Skills.addXp(q, Skill.MINING, Skills.xpForLevel(100));
        var level = h.getLevel();
        net.minecraft.core.BlockPos pos = h.absolutePos(new net.minecraft.core.BlockPos(10, 1, 10));
        for (int i = 0; i < 200; i++) {
            level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.IRON_ORE.defaultBlockState());
            q.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE));
            q.gameMode.destroyBlock(pos);
        }
        var items = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(3));
        int total = items.stream().mapToInt(e -> e.getItem().getCount()).sum();
        items.forEach(net.minecraft.world.entity.Entity::discard);
        h.assertTrue(total > 240, "Mining 100 gives extra ore: " + total + " drops from 200 ores");
    }

    private static void noDupeUnstackable(GameTestHelper h) {
        var pick = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE);
        pick.setDamageValue(5);
        h.assertTrue(SkillPerks.extra(List.of(pick)).isEmpty(), "never an extra tool");
        var ore = SkillPerks.extra(List.of(net.minecraft.world.item.ItemStack.EMPTY, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.RAW_IRON, 3)));
        h.assertTrue(ore.is(net.minecraft.world.item.Items.RAW_IRON) && ore.getCount() == 1, "extra is one of the first stackable drop");
    }

    private static void perkUnlock(GameTestHelper h) {
        ServerPlayer q = fresh(h, "novice");
        Skills.addXp(q, Skill.MINING, Skills.xpForLevel(9));
        Skills.Perk ore = Skills.perks(Skill.MINING).get(0);
        h.assertTrue(ore.unlockLevel() == 10 && ore.langKey().equals("skill.mechanical.perk.mining.1"), "first Mining perk: " + ore);
        h.assertTrue(!ore.unlocked(q) && ore.value(q) == 0, "locked at level 9");
        for (int i = 0; i < 300; i++) h.assertTrue(!ore.roll(q), "level 9 never rolls the level 10 perk");
        Skills.addXp(q, Skill.MINING, Skills.xpForLevel(10) - Skills.xpForLevel(9));
        h.assertTrue(ore.unlocked(q) && Math.abs(ore.value(q) - 0.04) < 1e-9, "unlocked at level 10 with 4 %");
        for (Skill s : Skill.values()) h.assertTrue(!Skills.perks(s).isEmpty(), "perks for " + s);
    }
}
