package com.mechanical.wildlife.elkwight;

import static com.mechanical.Mechanical.MODID;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.mechanical.Butchery;
import com.mechanical.CarcassEntity;
import com.mechanical.CarcassHauling;
import com.mechanical.HeavyLoad;
import com.mechanical.TestArea;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Game tests for the Elkwight: he reaches a player on a tower of built blocks (leaping) and one hiding in a hole in
 * natural stone (smashing down); his health scales with the players; he eats a dead player for a quarter of his health,
 * and a hard hit mid-meal leaves him only part of it; the Ground Wave cracks natural stone and fells a natural tree but
 * leaves built blocks and a player's tree; his colossal carcass doesn't budge for one player, creeps for two and goes for
 * three, and needs a knife past steel (the ritual knife cuts it); the ruin's spawner makes him; his axe needs two pullers.
 */
@EventBusSubscriber(modid = MODID)
public final class ElkwightTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final String[] TESTS = {"elkwight_tower", "elkwight_hole", "elkwight_rules", "elkwight_eat", "elkwight_wave", "elkwight_pull"};
    private static final int[] TICKS = {900, 1200, 100, 400, 200, 120};

    static {
        TEST_FUNCTIONS.register("elkwight_tower", () -> ElkwightTest::tower);
        TEST_FUNCTIONS.register("elkwight_hole", () -> ElkwightTest::hole);
        TEST_FUNCTIONS.register("elkwight_rules", () -> ElkwightTest::rules);
        TEST_FUNCTIONS.register("elkwight_eat", () -> ElkwightTest::eat);
        TEST_FUNCTIONS.register("elkwight_wave", () -> ElkwightTest::wave);
        TEST_FUNCTIONS.register("elkwight_pull", () -> ElkwightTest::pull);
    }

    static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "elkwight"), new TestEnvironmentDefinition.AllOf(List.of()));
        for (int i = 0; i < TESTS.length; i++) {
            Identifier id = Identifier.fromNamespaceAndPath(MODID, TESTS[i]);
            event.registerTest(id, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id), TestArea.of(env, TICKS[i], true)));
        }
    }

    private static void floor(GameTestHelper h, int top, BlockState state) {
        for (int x = 0; x <= 16; x++) for (int z = 0; z <= 16; z++) for (int y = 0; y <= top; y++) h.setBlock(new BlockPos(x, y, z), state);
    }

    private static void clearOld(GameTestHelper h) {
        AABB area = new AABB(h.absolutePos(BlockPos.ZERO)).expandTowards(16, 16, 16).inflate(2);
        for (var e : h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.Entity.class, area, e -> !(e instanceof Player))) e.discard();
    }

    private static Elkwight boss(GameTestHelper h, Vec3 rel) {
        Elkwight e = Elkwights.ELKWIGHT.get().create(h.getLevel(), EntitySpawnReason.MOB_SUMMONED);
        e.setPos(h.absoluteVec(rel));
        h.getLevel().addFreshEntity(e);
        return e;
    }

    /**
     * A stand-in for a player at {@code rel}: a pig that doesn't move or die (a mock player isn't in the level, can't be
     * made unhurtable without the boss losing interest, and peaceful test servers refuse player targets).
     */
    private static net.minecraft.world.entity.LivingEntity dummy(GameTestHelper h, Vec3 rel) {
        var p = EntityTypes.PIG.create(h.getLevel(), EntitySpawnReason.MOB_SUMMONED);
        p.setNoAi(true);
        p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);
        p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        p.setHealth(1000);
        p.setPos(h.absoluteVec(rel));
        h.getLevel().addFreshEntity(p);
        return p;
    }

    private static boolean reached(Elkwight e, net.minecraft.world.entity.LivingEntity p) {
        return e.distanceTo(p) < Elkwight.CHOP_RANGE && Math.abs(e.getY() - p.getY()) < 2.5; // in reach of his axe
    }

    /** A 3x3 tower of cobblestone (built: he can't smash it), 5 high, a player on top: he leaps up to him. */
    private static void tower(GameTestHelper h) {
        clearOld(h);
        floor(h, 0, Blocks.STONE.defaultBlockState());
        for (int x = 10; x <= 12; x++) for (int z = 10; z <= 12; z++) for (int y = 1; y <= 5; y++) h.setBlock(new BlockPos(x, y, z), Blocks.COBBLESTONE);
        var p = dummy(h, new Vec3(11.5, 6, 11.5));
        Elkwight e = boss(h, new Vec3(3.5, 1, 3.5));
        e.wake();
        e.setTarget(p);
        h.succeedWhen(() -> {
            e.setTarget(p);
            h.assertTrue(reached(e, p), "the Elkwight reaches the player on the tower: " + e.debug());
            e.discard();
            p.discard();
        });
    }

    /** A player at the bottom of a 1x1 shaft 5 deep in natural stone: he smashes his way down. */
    private static void hole(GameTestHelper h) {
        clearOld(h);
        floor(h, 5, Blocks.STONE.defaultBlockState());
        for (int y = 1; y <= 5; y++) h.setBlock(new BlockPos(11, y, 11), Blocks.AIR);
        var p = dummy(h, new Vec3(11.5, 1, 11.5));
        Elkwight e = boss(h, new Vec3(4.5, 6, 4.5));
        e.wake();
        e.setTarget(p);
        h.succeedWhen(() -> {
            e.setTarget(p);
            h.assertTrue(reached(e, p), "the Elkwight digs down to the player in the hole: " + e.debug());
            e.discard();
            p.discard();
        });
    }

    /** Health scales with the players near; his carcass is colossal and needs a knife past steel. */
    private static void rules(GameTestHelper h) {
        clearOld(h);
        floor(h, 0, Blocks.STONE.defaultBlockState());
        Elkwight e = boss(h, new Vec3(8.5, 1, 8.5));
        e.scaleFor(1);
        h.assertTrue(Math.abs(e.getMaxHealth() - Elkwight.BASE_HEALTH) < 0.01 && e.getHealth() == e.getMaxHealth(), "one player: " + Elkwight.BASE_HEALTH + " health, got " + e.getMaxHealth());
        e.setHealth(e.getMaxHealth() / 2);
        e.scaleFor(3);
        double three = Elkwight.BASE_HEALTH * (1 + 2 * Elkwight.HEALTH_PER_PLAYER);
        h.assertTrue(Math.abs(e.getMaxHealth() - three) < 0.01, "three players: " + three + " health, got " + e.getMaxHealth());
        h.assertTrue(Math.abs(e.getHealth() - three / 2) < 0.5, "the share of damage is kept: " + e.getHealth());
        h.assertTrue(e.damageScale() > 1.19 && e.damageScale() < 1.21, "and he hits 20 % harder: " + e.damageScale());
        e.scaleFor(1);
        h.assertTrue(Math.abs(e.getMaxHealth() - Elkwight.BASE_HEALTH) < 0.01, "back to one player: no stacking");

        CarcassEntity body = CarcassEntity.of(h.getLevel(), e);
        h.assertTrue(body.isColossal() && body.fitsOnTravois() && body.loadUnits() == 3, "a colossal body that goes on a travois");
        h.assertTrue(Butchery.requiredKnifeTier(Elkwights.ELKWIGHT.get()) == Elkwights.KNIFE_TIER, "it needs a top-tier knife");
        body.setPos(h.absoluteVec(new Vec3(4.5, 1, 4.5)));
        h.getLevel().addFreshEntity(body);
        Player butcher = h.makeMockPlayer(GameType.SURVIVAL);
        butcher.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(com.mechanical.Smithing.STEEL_KNIFE.get()));
        InteractionResult r = body.interact(butcher, InteractionHand.MAIN_HAND, Vec3.ZERO);
        h.assertTrue(r == InteractionResult.FAIL && !butcher.isUsingItem(), "a steel knife is refused: " + r);
        Player priest = h.makeMockPlayer(GameType.SURVIVAL);
        priest.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(com.mechanical.people.OakFolk.ANCIENT_RITUAL_KNIFE.get()));
        InteractionResult ok = body.interact(priest, InteractionHand.MAIN_HAND, Vec3.ZERO);
        h.assertTrue(ok == InteractionResult.CONSUME, "the Ancient Ritual Knife cuts it: " + ok);
        body.discard();
        // the ruin's spawner (worldgen.ElkwightRuins) makes him: persistent, tagged with his ruin, asleep until someone comes
        BlockPos at = h.absolutePos(new BlockPos(12, 1, 12));
        com.mechanical.worldgen.ElkwightRuins.spawnBoss(h.getLevel(), at);
        var spawned = h.getLevel().getEntitiesOfClass(Elkwight.class, new AABB(at).inflate(1));
        h.assertTrue(spawned.size() == 1, "the ruin spawns one Elkwight: " + spawned.size());
        Elkwight ruinBoss = spawned.getFirst();
        h.assertTrue(ruinBoss.isPersistenceRequired() && !ruinBoss.isAwake() && ruinBoss.getPersistentData().contains("mechanical_ruin"),
                "persistent, asleep and tagged with his ruin");
        ruinBoss.discard();
        e.discard();
        h.succeed();
    }

    /** A player dies by him: he goes, kneels, eats and gets a quarter of his health back; a hard hit cuts the meal short. */
    private static void eat(GameTestHelper h) {
        clearOld(h);
        floor(h, 0, Blocks.STONE.defaultBlockState());
        Elkwight full = boss(h, new Vec3(3.5, 1, 4.5)), cut = boss(h, new Vec3(3.5, 1, 12.5));
        for (Elkwight e : List.of(full, cut)) {
            e.wake();
            e.setHealth(e.getMaxHealth() / 2);
            e.corpseAt(e.position().add(4, 0, 0));
        }
        int[] eating = {0};
        boolean[] hit = {false};
        h.onEachTick(() -> {
            if (cut.action() == Elkwight.EAT && ++eating[0] == 20 && !hit[0]) {
                hit[0] = true;
                cut.hurtServer(h.getLevel(), h.getLevel().damageSources().generic(), cut.getMaxHealth() * 0.06F);
            }
        });
        h.succeedWhen(() -> {
            h.assertTrue(hit[0] && full.mealHealed() > 0 && full.action() != Elkwight.EAT, "both ate (" + full.action() + ", " + cut.action() + ")");
            float quarter = full.getMaxHealth() * Elkwight.MEAL_SHARE;
            h.assertTrue(Math.abs(full.mealHealed() - quarter) < 1.0F && Math.abs(full.getHealth() - full.getMaxHealth() * 0.75F) < 1.0F,
                    "a whole meal heals a quarter: " + full.mealHealed() + " of " + quarter + ", health " + full.getHealth());
            h.assertTrue(cut.mealHealed() > 0 && cut.mealHealed() < quarter * 0.6F, "a meal cut short heals only part: " + cut.mealHealed());
            full.discard();
            cut.discard();
        });
    }

    /** The Ground Wave cracks natural stone and fells a natural tree; built blocks and a player's tree stay; it throws a pig. */
    private static void wave(GameTestHelper h) {
        clearOld(h);
        ServerLevel level = h.getLevel();
        floor(h, 0, Blocks.STONE.defaultBlockState());
        for (int z = 6; z <= 10; z++) h.setBlock(new BlockPos(8, 0, z), Blocks.STONE_BRICKS); // a built path across
        tree(h, new BlockPos(12, 1, 9), false);
        tree(h, new BlockPos(12, 1, 7), true);
        Elkwight e = boss(h, new Vec3(2.5, 1, 8.5));
        var pig = EntityTypes.PIG.create(level, EntitySpawnReason.MOB_SUMMONED);
        pig.setPos(h.absoluteVec(new Vec3(10.5, 1, 8.5)));
        level.addFreshEntity(pig);
        e.groundWave(h.absoluteVec(new Vec3(15.5, 1, 8.5)));
        h.runAfterDelay(30, () -> {
            for (int x = 5; x <= 7; x++) {
                BlockState s = h.getBlockState(new BlockPos(x, 0, 8));
                h.assertFalse(s.is(Blocks.STONE), "natural stone in the wave's path cracks (to cobblestone, or gravel that may fall): " + s + " at x " + x);
            }
            h.assertTrue(h.getBlockState(new BlockPos(8, 0, 8)).is(Blocks.STONE_BRICKS), "built stone bricks stay");
            h.assertTrue(h.getBlockState(new BlockPos(12, 0, 3)).is(Blocks.STONE), "stone off the path stays");
            h.assertFalse(h.getBlockState(new BlockPos(12, 2, 9)).is(Blocks.OAK_LOG), "the natural tree is felled");
            h.assertTrue(h.getBlockState(new BlockPos(12, 2, 7)).is(Blocks.OAK_LOG), "the tree with player-placed leaves stands");
            h.assertTrue(!pig.isAlive() || pig.getHealth() < pig.getMaxHealth(), "the pig in its way is hurt");
            for (var t : level.getEntitiesOfClass(com.mechanical.FallingTreeEntity.class, new AABB(h.absolutePos(BlockPos.ZERO)).expandTowards(16, 16, 16).inflate(8))) t.discard();
            pig.discard();
            e.discard();
            h.succeed();
        });
    }

    /** An oak 4 logs high with leaves round the top (player-placed ones keep their PERSISTENT flag). */
    private static void tree(GameTestHelper h, BlockPos base, boolean placed) {
        for (int y = 0; y < 4; y++) h.setBlock(base.above(y), Blocks.OAK_LOG);
        BlockState leaf = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE, 1).setValue(LeavesBlock.PERSISTENT, placed);
        BlockPos top = base.above(3);
        for (BlockPos p : List.of(top.above(), top.east(), top.west(), top.north(), top.south(), top.above().east())) {
            if (p.equals(top.above().east())) h.setBlock(p, leaf.setValue(LeavesBlock.DISTANCE, 2));
            else h.setBlock(p, leaf);
        }
    }

    /** One player can't budge the colossal carcass, two drag it extremely slowly, three at full pace; the axe needs two. */
    private static void pull(GameTestHelper h) {
        clearOld(h);
        floor(h, 0, Blocks.STONE.defaultBlockState());
        ServerLevel level = h.getLevel();
        Elkwight template = Elkwights.ELKWIGHT.get().create(level, EntitySpawnReason.MOB_SUMMONED);
        List<HeavyLoad> loads = new ArrayList<>();
        List<Vec3> start = new ArrayList<>();
        int[] pullers = {1, 2, 3, 1, 2};
        for (int i = 0; i < pullers.length; i++) {
            Vec3 at = h.absoluteVec(new Vec3(3.5, 1, 1.5 + i * 3.2));
            HeavyLoad load;
            if (i < 3) {
                template.setPos(at);
                load = CarcassEntity.of(level, template);
                load.setYRot(90); // head (the grips) towards -x
            } else {
                load = new ElkwightAxeEntity(Elkwights.AXE.get(), level);
                load.setYRot(90);
            }
            load.setPos(at);
            level.addFreshEntity(load);
            for (int n = 0; n < pullers[i]; n++) {
                Player p = h.makeMockPlayer(GameType.SURVIVAL);
                CarcassHauling.pickUpOrLetGo(p, load);
                int slot = load.slotOf(p);
                p.setPos(load.gripPoint(slot, 1.0F).add(-3.0, 0, 0)); // leaning on it, 3 blocks out
            }
            loads.add(load);
            start.add(at);
        }
        h.assertTrue(loads.get(0).haulerCount() == 1 && loads.get(2).haulerCount() == 3 && loads.get(4).haulerCount() == 2, "everyone took hold");
        h.runAfterDelay(8, () -> { // a dragged body's speed is capped: give it time to close the 3 blocks
            double[] moved = new double[loads.size()];
            for (int i = 0; i < moved.length; i++) moved[i] = loads.get(i).position().subtract(start.get(i)).horizontalDistance();
            String all = java.util.Arrays.toString(moved);
            h.assertTrue(moved[0] < 0.01, "one player can't budge the Elkwight's body " + all);
            h.assertTrue(moved[1] > 0.05 && moved[1] < 1.0, "two drag it extremely slowly " + all);
            h.assertTrue(moved[2] > 1.5, "three drag it at full pace " + all);
            h.assertTrue(moved[3] < 0.01, "one player can't budge the axe " + all);
            h.assertTrue(moved[4] > 1.5, "two drag the axe " + all);
            for (HeavyLoad l : loads) l.discard();
            h.succeed();
        });
    }

    private ElkwightTest() {}
}
