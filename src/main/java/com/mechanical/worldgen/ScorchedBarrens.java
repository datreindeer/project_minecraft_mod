package com.mechanical.worldgen;

import java.util.Set;

import com.mechanical.Mechanical;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ColorRGBA;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * Scorched Barrens (docs/elkwight-plan.md phase 1): a burnt forest only beyond {@link #MIN_DISTANCE} blocks from 0,0 and
 * always ringed by living forest (mixin.MultiNoiseBiomeSourceMixin turns it back into its source biome elsewhere, see
 * {@link #stays}). Its blocks, and the charred trees.
 * Data: scripts/gen_world_biomes.py (biome, ground, features), scripts/gen_elkwight.py (blocks, ruin).
 */
public final class ScorchedBarrens {
    /** Neither the biome nor the Elkwight ruins come closer to 0,0 than this. */
    public static final int MIN_DISTANCE = 2500;

    public static boolean farEnough(int x, int z) {
        return (long) x * x + (long) z * z >= (long) MIN_DISTANCE * MIN_DISTANCE;
    }

    /** Burnt trunks: standing, fallen and stumps. Drops charcoal (loot table). */
    public static final DeferredBlock<RotatedPillarBlock> CHARRED_LOG = Mechanical.BLOCKS.registerBlock("charred_log", RotatedPillarBlock::new,
            p -> p.mapColor(MapColor.COLOR_BLACK).strength(1.5F).sound(SoundType.WOOD));
    /** Grey wood ash lying over the burnt ground; falls like sand. */
    public static final DeferredBlock<ColoredFallingBlock> WOOD_ASH = Mechanical.BLOCKS.registerBlock("wood_ash",
            p -> new ColoredFallingBlock(new ColorRGBA(0xFF8A8580), p), p -> p.mapColor(MapColor.COLOR_LIGHT_GRAY).strength(0.5F).sound(SoundType.SAND));
    /** Blackstone carved with faintly glowing green runes, in the Elkwight ruins. */
    public static final DeferredBlock<RuneBlock> RUNE_BLACKSTONE = Mechanical.BLOCKS.registerBlock("rune_blackstone", RuneBlock::new,
            p -> p.mapColor(MapColor.COLOR_BLACK).requiresCorrectToolForDrops().strength(1.5F, 6.0F).sound(SoundType.STONE).lightLevel(s -> 4));

    /**
     * Biome check in quart coordinates (4 blocks): a few blocks of margin, since the biome zoom can pick a neighbouring
     * quart for a block, so no block within MIN_DISTANCE ever reads as Scorched Barrens.
     */
    public static boolean biomeAllowed(int quartX, int quartZ) {
        long x = quartX * 4L + 2, z = quartZ * 4L + 2, r = MIN_DISTANCE + 12;
        return x * x + z * z >= r * r;
    }

    /** Quarts (24 blocks) of living forest round every Scorched Barrens quart: it never touches plains, water, desert... */
    public static final int RING = 6;
    /** What counts as living forest round the barrens (and the barrens itself, so its inside is fine). */
    private static final Set<ResourceKey<Biome>> LIVING = Set.of(ModBiomes.SCORCHED_BARRENS, ModBiomes.DECIDUOUS_FOREST, Biomes.FOREST,
            Biomes.FLOWER_FOREST, Biomes.BIRCH_FOREST, Biomes.OLD_GROWTH_BIRCH_FOREST, Biomes.DARK_FOREST, Biomes.DAPPLED_FOREST, Biomes.TAIGA,
            Biomes.OLD_GROWTH_PINE_TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA);

    /**
     * Whether a raw Scorched Barrens column stays barrens (MultiNoiseBiomeSourceMixin; else it is Forest): far enough out,
     * and every raw surface biome within RING quarts is living forest or barrens. So the barrens that stays always has a
     * ring of at least RING quarts of forest round it. The grid must reach RING quarts past (quartX, quartZ).
     */
    public static boolean stays(SurfaceGrid grid, int quartX, int quartZ) {
        return biomeAllowed(quartX, quartZ) && grid.all(quartX, quartZ, RING, ScorchedBarrens::living);
    }

    public static boolean living(Holder<Biome> h) {
        return h instanceof Holder.Reference<Biome> r && LIVING.contains(r.key());
    }

    /**
     * The resolver MultiNoiseBiomeSourceMixin returns: raw barrens quarts that may not stay become {@code source}. Only
     * they pay for the neighbourhood check, with one surface grid per chunk (the chunk and RING quarts round it) and one
     * decision per column. sizeX 0: a resolver for anywhere (may be shared between threads), so no cache.
     */
    public static BiomeResolver resolver(BiomeResolver raw, MultiNoiseBiomeSource biomes, Climate.Sampler sampler, Holder<Biome> barrens,
            Holder<Biome> source, int minX, int minZ, int sizeX, int sizeZ) {
        return new BiomeResolver() {
            private SurfaceGrid grid;
            private final byte[] stays = new byte[sizeX * sizeZ]; // per column: 0 not yet decided, 1 stays, 2 forest

            @Override
            public Holder<Biome> getNoiseBiome(int x, int y, int z) {
                Holder<Biome> h = raw.getNoiseBiome(x, y, z);
                if (h != barrens) return h;
                if (!biomeAllowed(x, z)) return source;
                if (x < minX || z < minZ || x >= minX + sizeX || z >= minZ + sizeZ)
                    return stays(SurfaceGrid.around(biomes, sampler, x, z, RING), x, z) ? h : source;
                int i = x - minX + (z - minZ) * sizeX;
                if (stays[i] == 0) {
                    if (grid == null) grid = new SurfaceGrid(biomes, sampler, minX - RING, minZ - RING, sizeX + 2 * RING, sizeZ + 2 * RING, 1);
                    stays[i] = (byte) (stays(grid, x, z) ? 1 : 2);
                }
                return stays[i] == 1 ? h : source;
            }
        };
    }

    /** Every quart within radius blocks of (x, z) is Scorched Barrens after the rules (the Elkwight ruin sits well inside). */
    public static boolean deepInside(MultiNoiseBiomeSource source, Climate.Sampler sampler, int x, int z, int radius) {
        int qx = QuartPos.fromBlock(x), qz = QuartPos.fromBlock(z), r = (radius + 3) / 4;
        SurfaceGrid g = SurfaceGrid.around(source, sampler, qx, qz, r + RING);
        for (int dx = -r; dx <= r; dx++)
            for (int dz = -r; dz <= r; dz++)
                if (dx * dx + dz * dz <= r * r && !(g.is(qx + dx, qz + dz, b -> b.is(ModBiomes.SCORCHED_BARRENS)) && stays(g, qx + dx, qz + dz)))
                    return false;
        return true;
    }

    public static void init() {}

    private ScorchedBarrens() {}

    /** Now and then a green mote drifts off the runes. */
    public static final class RuneBlock extends Block {
        public RuneBlock(Properties p) {
            super(p);
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (random.nextInt(8) != 0) return;
            Direction d = Direction.getRandom(random);
            if (!level.isEmptyBlock(pos.relative(d))) return;
            level.addParticle(new DustParticleOptions(0x6BFF8A, 0.5F + random.nextFloat() * 0.3F),
                    pos.getX() + 0.5 + d.getStepX() * 0.6, pos.getY() + 0.5 + d.getStepY() * 0.6, pos.getZ() + 0.5 + d.getStepZ() * 0.6,
                    d.getStepX() * 0.01, 0.02, d.getStepZ() * 0.01);
        }
    }

    /** One burnt tree: a standing trunk (some with a stub of a branch), a fallen log, or a stump. */
    static final class CharredTree extends StoneAgeFeature {
        static final MapCodec<CharredTree> CODEC = MapCodec.unit(CharredTree::new);

        @Override
        public MapCodec<CharredTree> codec() {
            return CODEC;
        }

        @Override
        public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
            // from the placement's height to the ground (the _WG heightmaps are stale or absent outside early worldgen)
            BlockPos c = origin;
            for (int i = 0; i < 8 && !level.getBlockState(c).canBeReplaced(); i++) c = c.above();
            for (int i = 0; i < 16 && level.getBlockState(c.below()).canBeReplaced(); i++) c = c.below();
            // on any sturdy ground: Wood Ash, Blackstone and Basalt are not in the substrate tags
            if (!level.getBlockState(c.below()).isFaceSturdy(level, c.below(), Direction.UP) || !level.getFluidState(c).isEmpty() || !level.getBlockState(c).isAir())
                return false;
            BlockState log = CHARRED_LOG.get().defaultBlockState();
            int kind = random.nextInt(10);
            if (kind < 2) { // fallen: lies along the ground until a dip or an obstacle
                Direction d = randomSide(random);
                BlockState lying = log.setValue(RotatedPillarBlock.AXIS, d.getAxis());
                int len = 3 + random.nextInt(4), n = 0;
                for (BlockPos p = c; n < len && level.getBlockState(p).canBeReplaced() && level.getFluidState(p).isEmpty()
                        && level.getBlockState(p.below()).isSolid(); p = p.relative(d), n++) set(level, p, lying);
                if (n > 0) set(level, c.relative(d.getOpposite()), log); // the stump it broke from
                return n > 0;
            }
            int height = kind < 3 ? 1 + random.nextInt(2) : 4 + random.nextInt(7);
            for (int y = 0; y < height; y++) if (level.getBlockState(c.above(y)).canBeReplaced()) set(level, c.above(y), log);
            if (height >= 5 && random.nextBoolean()) { // a burnt stub of a branch
                Direction d = randomSide(random);
                BlockPos b = c.above(height - 1 - random.nextInt(2)).relative(d);
                if (level.getBlockState(b).isAir()) set(level, b, log.setValue(RotatedPillarBlock.AXIS, d.getAxis()));
            }
            return true;
        }
    }
}
