package com.mechanical.worldgen;

import static com.mechanical.Mechanical.MODID;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.mechanical.Mechanical;
import com.mechanical.TestArea;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Game test for Elkwight phases 1-2: Scorched Barrens never within 2500 blocks of 0,0 (sampling the real overworld biome
 * source) but present beyond, about as rare as Mushroom Fields; the ruin's placement rule; and the arena drawn chunk by
 * chunk with its rune blocks and exactly one boss spawn call.
 */
@EventBusSubscriber(modid = MODID)
public final class ElkwightWorldTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> TEST = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "elkwight_world"));

    static {
        TEST_FUNCTIONS.register("elkwight_world", () -> ElkwightWorldTest::test);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "elkwight_world"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "elkwight_world"), new FunctionGameTestInstance(TEST, TestArea.of(env, 40, true)));
    }

    private static void check(GameTestHelper h, boolean ok, String what) {
        if (!ok) throw h.assertionException("elkwight_world: " + what);
    }

    private static void test(GameTestHelper h) {
        biomes(h);
        placement(h);
        trees(h);
        arena(h);
        h.succeed();
    }

    /** The overworld preset's biome source with this world's seed (the test level itself is flat). */
    private static void biomes(GameTestHelper h) {
        var access = h.getLevel().registryAccess();
        var source = MultiNoiseBiomeSource.createFromPreset(access.lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
                .getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD));
        check(h, source.possibleBiomes().stream().anyMatch(b -> b.is(ModBiomes.SCORCHED_BARRENS)), "Scorched Barrens is in the overworld preset");
        NoiseGeneratorSettings settings = access.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.OVERWORLD).value();
        int y = QuartPos.fromBlock(80), raw = 0, barrens = 0, mushroom = 0, total = 0;
        // a few seeds, so the raw climate map surely puts it inside 2500 somewhere and the rule has something to turn back
        for (long seed : new long[] {h.getLevel().getSeed(), 1, 2, 3, 4242}) {
            RandomState rs = RandomState.create(access.lookupOrThrow(Registries.NOISE), seed, settings);
            BiomeResolver resolver = source.createUncachedResolver(rs);
            Climate.Sampler sampler = rs.createClimateSampler(net.minecraft.world.level.levelgen.densityfunction.SamplerContext.EMPTY_UNCACHED);
            for (int x = -2560; x <= 2560; x += 32) {
                for (int z = -2560; z <= 2560; z += 32) {
                    if ((long) x * x + (long) z * z >= 2500L * 2500L) continue;
                    int qx = QuartPos.fromBlock(x), qz = QuartPos.fromBlock(z);
                    check(h, !resolver.getNoiseBiome(qx, y, qz).is(ModBiomes.SCORCHED_BARRENS), "Scorched Barrens at " + x + ", " + z + " (seed " + seed + ")");
                    if (source.getNoiseBiome(sampler.sample(qx, y, qz)).is(ModBiomes.SCORCHED_BARRENS)) raw++;
                }
            }
            for (int i = 0; i < 2000; i++) { // the rim
                double a = i * Math.PI * 2 / 2000;
                int qx = QuartPos.fromBlock((int) (Math.cos(a) * 2499)), qz = QuartPos.fromBlock((int) (Math.sin(a) * 2499));
                check(h, !resolver.getNoiseBiome(qx, y, qz).is(ModBiomes.SCORCHED_BARRENS), "Scorched Barrens on the 2500 rim");
            }
            // beyond: present, and rare
            for (int x = -12000; x <= 12000; x += 128) {
                for (int z = -12000; z <= 12000; z += 128) {
                    if ((long) x * x + (long) z * z < 2600L * 2600L) continue;
                    Holder<Biome> b = resolver.getNoiseBiome(QuartPos.fromBlock(x), y, QuartPos.fromBlock(z));
                    total++;
                    if (b.is(ModBiomes.SCORCHED_BARRENS)) barrens++;
                    if (b.is(Biomes.MUSHROOM_FIELDS)) mushroom++;
                }
            }
        }
        Mechanical.LOGGER.info("elkwight_world: beyond 2600 blocks {} of {} samples Scorched Barrens ({}%), {} Mushroom Fields ({}%); {} raw hits inside 2500 turned back",
                barrens, total, 100.0 * barrens / total, mushroom, 100.0 * mushroom / total, raw);
        check(h, raw > 0, "the raw climate map never put Scorched Barrens inside 2500 in five seeds: the rule went untested");
        check(h, barrens > 0, "Scorched Barrens beyond 2500 blocks (0 of " + total + ")");
        check(h, barrens < total / 200, "Scorched Barrens rare (" + barrens + " of " + total + ")");
    }

    /** Charred trees stand on Wood Ash (not in the substrate tags). Drawn at the far corner, then cleared for the arena. */
    private static void trees(GameTestHelper h) {
        for (int x = 0; x <= 16; x++) for (int z = 0; z <= 16; z++) h.setBlock(x, 1, z, ScorchedBarrens.WOOD_ASH.get());
        int placed = 0;
        var gen = h.getLevel().getChunkSource().getGenerator();
        for (int i = 0; i < 6; i++) if (new ScorchedBarrens.CharredTree().place(h.getLevel(), gen, h.getLevel().getRandom(), h.absolutePos(new BlockPos(2 + i * 2, 2, 3)))) placed++;
        int logs = 0;
        for (BlockPos p : BlockPos.betweenClosed(h.absolutePos(new BlockPos(0, 2, 0)), h.absolutePos(new BlockPos(16, 14, 16))))
            if (h.getLevel().getBlockState(p).is(ScorchedBarrens.CHARRED_LOG.get())) logs++;
        Mechanical.LOGGER.info("elkwight_world: {} of 6 charred trees placed, {} logs", placed, logs);
        check(h, placed == 6 && logs >= 6, "charred trees on Wood Ash: " + placed + " placed, " + logs + " logs");
    }

    private static void placement(GameTestHelper h) {
        check(h, !ElkwightRuinStructure.allowedAt(0, 0) && !ElkwightRuinStructure.allowedAt(2500, 0) && !ElkwightRuinStructure.allowedAt(-1700, 1700),
                "no ruin within 2500 blocks (its whole arena)");
        check(h, ElkwightRuinStructure.allowedAt(2600, 0) && ElkwightRuinStructure.allowedAt(-1900, -1900), "ruins allowed beyond");
        for (int i = 0; i < 360; i++) {
            double a = Math.toRadians(i);
            int x = (int) (Math.cos(a) * 2524), z = (int) (Math.sin(a) * 2524);
            check(h, !ElkwightRuinStructure.allowedAt(x, z), "ruin centre at " + x + ", " + z);
        }
    }

    /** The arena drawn in two slices (as two chunks would): rune blocks, and the boss spawn called once, at the centre. */
    private static void arena(GameTestHelper h) {
        BlockPos centre = h.absolutePos(new BlockPos(8, 2, 8));
        BlockPos lo = h.absolutePos(BlockPos.ZERO), hi = h.absolutePos(new BlockPos(16, 16, 16));
        List<BlockPos> calls = new ArrayList<>();
        BiConsumer<WorldGenLevel, BlockPos> real = ElkwightRuins.spawner;
        ElkwightRuins.spawner = (level, pos) -> calls.add(pos);
        try {
            long seed = 12345L;
            ElkwightRuinStructure.build(h.getLevel(), BoundingBox.fromCorners(lo, new BlockPos(centre.getX() - 1, hi.getY(), hi.getZ())), seed, centre,
                    h.getLevel().getRandom());
            check(h, calls.isEmpty(), "no boss from a slice without the centre");
            ElkwightRuinStructure.build(h.getLevel(), BoundingBox.fromCorners(new BlockPos(centre.getX(), lo.getY(), lo.getZ()), hi), seed, centre,
                    h.getLevel().getRandom());
        } finally {
            ElkwightRuins.spawner = real;
        }
        check(h, calls.size() == 1 && calls.get(0).equals(centre.above()), "one boss spawn at the centre, got " + calls);
        int runes = 0, blackstone = 0;
        for (BlockPos p : BlockPos.betweenClosed(lo, hi)) {
            var s = h.getLevel().getBlockState(p);
            if (s.is(ScorchedBarrens.RUNE_BLACKSTONE.get())) runes++;
            if (s.is(net.minecraft.world.level.block.Blocks.POLISHED_BLACKSTONE_BRICKS)) blackstone++;
        }
        check(h, runes >= 8, "rune blocks round the dais, got " + runes);
        check(h, blackstone > 50, "a blackstone floor, got " + blackstone);
        check(h, h.getLevel().getBlockState(centre).is(net.minecraft.world.level.block.Blocks.POLISHED_BLACKSTONE_BRICKS) && h.getLevel().getBlockState(centre.above()).isAir(),
                "the dais with room for the boss");
        check(h, ScorchedBarrens.RUNE_BLACKSTONE.get().defaultBlockState().getLightEmission() > 0, "runes glow");
    }
}
