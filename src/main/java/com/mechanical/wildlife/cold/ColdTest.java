package com.mechanical.wildlife.cold;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.mechanical.CarcassEntity;
import com.mechanical.TestArea;
import com.mechanical.wildlife.coast.CoastFauna;
import com.mechanical.wildlife.coast.Seal;
import com.mechanical.wildlife.mountain.Ibex;
import com.mechanical.wildlife.mountain.MountainFauna;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Game tests for the cold fauna: spawn lists per biome (and vanilla's polar bear gone, replaced on joining); a polar bear
 * picks a seal on the ice as quarry; an arctic fox catches up with a polar bear, and eats a carcass the bear left; the
 * fox's coat by the calendar; a snow leopard picks an ibex as quarry.
 */
@EventBusSubscriber(modid = MODID)
public final class ColdTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final String[] TESTS = {"cold_spawns", "cold_bear_hunts_seal", "cold_fox_follows_bear", "cold_fox_eats_leftovers",
            "cold_fox_coat", "cold_leopard_hunts_ibex", "cold_pika_hay", "cold_lammergeier_bone", "cold_spring_mother"};
    private static final int[] TICKS = {100, 300, 600, 900, 150, 300, 40, 40, 40};

    static {
        TEST_FUNCTIONS.register(TESTS[0], () -> ColdTest::spawns);
        TEST_FUNCTIONS.register(TESTS[1], () -> ColdTest::bearHuntsSeal);
        TEST_FUNCTIONS.register(TESTS[2], () -> ColdTest::foxFollowsBear);
        TEST_FUNCTIONS.register(TESTS[3], () -> ColdTest::foxEatsLeftovers);
        TEST_FUNCTIONS.register(TESTS[4], () -> ColdTest::foxCoat);
        TEST_FUNCTIONS.register(TESTS[5], () -> ColdTest::leopardHuntsIbex);
        TEST_FUNCTIONS.register(TESTS[6], () -> ColdTest::pikaHay);
        TEST_FUNCTIONS.register(TESTS[7], () -> ColdTest::lammergeierBone);
        TEST_FUNCTIONS.register(TESTS[8], () -> ColdTest::springMother);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        for (int i = 0; i < TESTS.length; i++) {
            var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, TESTS[i]), new TestEnvironmentDefinition.AllOf(List.of()));
            event.registerTest(Identifier.fromNamespaceAndPath(MODID, TESTS[i]), new FunctionGameTestInstance(
                    ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, TESTS[i])), TestArea.of(env, TICKS[i], true)));
        }
    }

    /** x/z 0..16 of {@code floor} with air above. */
    static void floor(GameTestHelper h, net.minecraft.world.level.block.Block floor) {
        for (int x = 0; x <= 16; x++) for (int z = 0; z <= 16; z++) {
            h.setBlock(x, 0, z, floor);
            for (int y = 1; y <= 4; y++) h.setBlock(x, y, z, Blocks.AIR);
        }
    }

    static AABB area(GameTestHelper h) {
        return new AABB(h.absolutePos(BlockPos.ZERO)).expandTowards(16, 6, 16).inflate(1);
    }

    /** Entity ids in a biome's natural creature spawns (biome modifiers applied). */
    static Set<String> creatures(GameTestHelper h, ResourceKey<Biome> key) {
        Biome biome = h.getLevel().registryAccess().lookupOrThrow(Registries.BIOME).getValueOrThrow(key);
        MobSpawnSettings s = biome.getAttributes().applyModifier(EnvironmentAttributes.NATURAL_MOB_SPAWNS, MobSpawnSettings.EMPTY);
        return s.getMobsToSpawn(MobCategory.CREATURE).unwrap().stream().map(w -> BuiltInRegistries.ENTITY_TYPE.getKey(w.value().type()).toString())
                .collect(Collectors.toSet());
    }

    private static void spawns(GameTestHelper h) {
        String bear = "mechanical:polar_bear", fox = "mechanical:arctic_fox", leopard = "mechanical:snow_leopard", vanilla = "minecraft:polar_bear";
        for (var b : List.of(Biomes.SNOWY_PLAINS, Biomes.ICE_SPIKES)) {
            Set<String> c = creatures(h, b);
            h.assertTrue(c.contains(bear) && c.contains(fox) && !c.contains(vanilla) && !c.contains(leopard), b.identifier() + ": " + c);
        }
        for (var b : List.of(Biomes.FROZEN_OCEAN, Biomes.DEEP_FROZEN_OCEAN, Biomes.FROZEN_RIVER, Biomes.SNOWY_BEACH))
            h.assertFalse(creatures(h, b).contains(vanilla), "no vanilla polar bear in " + b.identifier());
        for (var b : List.of(Biomes.FROZEN_PEAKS, Biomes.JAGGED_PEAKS, Biomes.STONY_PEAKS)) {
            Set<String> c = creatures(h, b);
            h.assertTrue(c.contains(leopard) && !c.contains(bear), b.identifier() + ": " + c);
        }
        for (var b : List.of(Biomes.PLAINS, Biomes.DESERT, Biomes.FOREST, Biomes.TAIGA)) {
            Set<String> c = creatures(h, b);
            h.assertTrue(!c.contains(bear) && !c.contains(fox) && !c.contains(leopard) && !c.contains("mechanical:siberian_tiger"), b.identifier() + ": " + c);
        }
        h.assertTrue(creatures(h, Biomes.SNOWY_TAIGA).contains("mechanical:siberian_tiger"), "tiger in snowy taiga");
        h.assertTrue(creatures(h, Biomes.SNOWY_PLAINS).contains("mechanical:snowy_owl") && !creatures(h, Biomes.FOREST).contains("mechanical:snowy_owl"), "snowy owl");
        h.assertTrue(creatures(h, Biomes.TAIGA).contains("mechanical:black_grouse") && !creatures(h, Biomes.DESERT).contains("mechanical:black_grouse"), "black grouse");
        h.assertTrue(creatures(h, Biomes.JAGGED_PEAKS).contains("mechanical:lammergeier") && !creatures(h, Biomes.PLAINS).contains("mechanical:lammergeier"), "lammergeier");
        h.assertTrue(creatures(h, Biomes.STONY_PEAKS).contains("mechanical:pika") && !creatures(h, Biomes.PLAINS).contains("mechanical:pika"), "pika");
        // a vanilla polar bear that joins the world is swapped for ours
        floor(h, Blocks.SNOW_BLOCK);
        h.spawn(EntityTypes.POLAR_BEAR, 8, 1, 8);
        h.runAfterDelay(5, () -> {
            h.assertTrue(h.getLevel().getEntities(EntityTypes.POLAR_BEAR, area(h), e -> true).isEmpty(), "vanilla polar bear kept out");
            h.assertTrue(!h.getLevel().getEntitiesOfClass(PolarBear.class, area(h)).isEmpty(), "ours in its place");
            h.succeed();
        });
    }

    private static void bearHuntsSeal(GameTestHelper h) {
        floor(h, Blocks.PACKED_ICE);
        for (int z = 0; z <= 16; z++) for (int x = 13; x <= 16; x++) h.setBlock(x, 0, z, Blocks.WATER); // open water at the ice edge
        PolarBear bear = h.spawn(ColdFauna.POLAR_BEAR.get(), 2, 1, 8);
        Seal seal = h.spawn(CoastFauna.SEAL.get(), 11, 1, 8);
        h.succeedWhen(() -> h.assertTrue(bear.getTarget() == seal, "bear stalks the seal (target " + bear.getTarget() + ")"));
    }

    private static void foxFollowsBear(GameTestHelper h) {
        floor(h, Blocks.SNOW_BLOCK);
        PolarBear bear = h.spawn(ColdFauna.POLAR_BEAR.get(), 15, 1, 15);
        bear.setNoAi(true);
        ArcticFox fox = h.spawn(ColdFauna.ARCTIC_FOX.get(), 1, 1, 1);
        h.succeedWhen(() -> h.assertTrue(fox.distanceTo(bear) < ArcticFox.BEAR_KEEP, "fox keeps near the bear (" + fox.distanceTo(bear) + ")"));
    }

    private static void foxEatsLeftovers(GameTestHelper h) {
        floor(h, Blocks.SNOW_BLOCK);
        PolarBear bear = h.spawn(ColdFauna.POLAR_BEAR.get(), 15, 1, 15);
        bear.setNoAi(true);
        Seal dead = h.spawn(CoastFauna.SEAL.get(), 8, 1, 8);
        CarcassEntity carcass = CarcassEntity.of(h.getLevel(), dead);
        dead.discard();
        carcass.addTag(PolarBear.LEFTOVERS);
        h.getLevel().addFreshEntity(carcass);
        h.spawn(ColdFauna.ARCTIC_FOX.get(), 2, 1, 2);
        h.succeedWhen(() -> h.assertTrue(carcass.isRemoved(), "the fox ate the bear's leftovers"));
    }

    private static void foxCoat(GameTestHelper h) {
        int month = com.mechanical.Calendar.MONTH_DAYS, season = com.mechanical.Calendar.SEASON_DAYS;
        h.assertTrue(ArcticFox.whiteCoat(3 * season + 10) && ArcticFox.whiteCoat(0) && ArcticFox.whiteCoat(month - 1)
                && ArcticFox.whiteCoat(2 * season + 2 * month), "white in winter, late autumn and the first month of spring");
        h.assertTrue(!ArcticFox.whiteCoat(month) && !ArcticFox.whiteCoat(season + 20) && !ArcticFox.whiteCoat(2 * season + 2 * month - 1),
                "brown from the second month of spring through summer to mid autumn");
        floor(h, Blocks.SNOW_BLOCK);
        ArcticFox fox = h.spawn(ColdFauna.ARCTIC_FOX.get(), 8, 1, 8);
        boolean expected = ArcticFox.whiteCoat(com.mechanical.Calendar.dayOfYear(h.getLevel().getOverworldClockTime()));
        h.succeedWhen(() -> h.assertTrue(fox.isWhite() == expected, "the live fox wears today's coat"));
    }

    /** Hay-making season; a pile grows layer by layer to 4; the next mouthful goes to a pile not yet full; no more than 3 piles. */
    private static void pikaHay(GameTestHelper h) {
        int season = com.mechanical.Calendar.SEASON_DAYS;
        long day = com.mechanical.Calendar.CLOCK_DAY;
        long spring = (0 - com.mechanical.Calendar.START_DAY + 10L + 3L * com.mechanical.Calendar.YEAR_DAYS) * day;
        h.assertTrue(!Pika.haySeason(spring) && Pika.haySeason(spring + season * day) && Pika.haySeason(spring + 2L * season * day)
                && !Pika.haySeason(spring + 3L * season * day), "hay in summer and autumn only");
        floor(h, Blocks.STONE);
        var level = h.getLevel();
        BlockPos at = h.absolutePos(new BlockPos(8, 1, 8));
        h.assertTrue(at.equals(Pika.nextPile(level, at)), "a new pile where it stands");
        for (int i = 0; i < 4; i++) Pika.stack(level, at);
        h.assertTrue(level.getBlockState(at).getValue(com.mechanical.wildlife.GuanoPileBlock.LAYERS) == 4, "four layers");
        BlockPos near = at.east(2);
        h.assertTrue(near.equals(Pika.nextPile(level, near)), "a full pile: a new one beside it");
        Pika.stack(level, near);
        h.assertTrue(near.equals(Pika.nextPile(level, at.west(3))), "the next mouthful goes to the pile not yet full");
        Pika.stack(level, at.south(2));
        for (int i = 0; i < 4; i++) {
            Pika.stack(level, near);
            Pika.stack(level, at.south(2));
        }
        h.assertTrue(Pika.nextPile(level, at.north(3)) == null, "three full piles: no more");
        h.succeed();
    }

    /** A Bone Pile taken whole (two bones), each dropped from high up cracking into Bone Marrow. */
    private static void lammergeierBone(GameTestHelper h) {
        floor(h, Blocks.STONE);
        var level = h.getLevel();
        BlockPos pile = h.absolutePos(new BlockPos(8, 1, 8));
        level.setBlockAndUpdate(pile, com.mechanical.Mechanical.BONE_PILE.get().defaultBlockState());
        Lammergeier bird = h.spawn(ColdFauna.LAMMERGEIER.get(), 8, 3, 8);
        bird.setNoAi(true);
        bird.bone = bird.findBone();
        h.assertTrue(bird.bone != null && net.minecraft.core.BlockPos.containing(bird.bone).equals(pile), "it spots the bone pile (" + bird.bone + ")");
        bird.take();
        h.assertTrue(bird.bones() == 2 && level.getBlockState(pile).isAir(), "took the pile: two bones");
        bird.dropBone(level, pile.getY());
        bird.dropBone(level, pile.getY());
        h.assertTrue(bird.bones() == 0, "both dropped");
        h.runAfterDelay(20, () -> {
            int marrow = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, area(h), e -> e.getItem().is(ColdFauna.BONE_MARROW.get()))
                    .stream().mapToInt(e -> e.getItem().getCount()).sum();
            h.assertTrue(marrow == 2, "two Bone Marrow on the ground (" + marrow + ")");
            h.succeed();
        });
    }

    /** A grown bear with a cub near is a guarding mother - in spring only; a lone bear never. */
    private static void springMother(GameTestHelper h) {
        floor(h, Blocks.GRASS_BLOCK);
        var mother = h.spawn(com.mechanical.wildlife.Wildlife.BROWN_BEAR.get(), 4, 1, 4);
        var cub = h.spawn(com.mechanical.wildlife.Wildlife.BROWN_BEAR.get(), 6, 1, 4);
        cub.setBaby(true);
        h.assertTrue(mother.guardsCubs(0) && !mother.guardsCubs(1) && !mother.guardsCubs(3), "mother guards in spring only");
        h.assertTrue(!cub.guardsCubs(0), "a cub guards nothing");
        h.succeed();
    }

    private static void leopardHuntsIbex(GameTestHelper h) {
        floor(h, Blocks.STONE);
        SnowLeopard leopard = h.spawn(ColdFauna.SNOW_LEOPARD.get(), 2, 1, 8);
        Ibex ibex = h.spawn(MountainFauna.IBEX.get(), 12, 1, 8);
        h.succeedWhen(() -> h.assertTrue(leopard.getTarget() == ibex, "snow leopard stalks the ibex (target " + leopard.getTarget() + ")"));
    }
}
