package com.mechanical.worldgen;

import com.mechanical.CavePaintingBlock;
import com.mechanical.CavePaintingBlock.Colour;
import com.mechanical.CavePaintingBlock.Motif;
import com.mechanical.Mechanical;
import com.mechanical.Pigments;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.chunk.ChunkGenerator;

/**
 * Small finds in the cave biomes (docs/caves.md), on a dark cave floor near the origin: {@link #PAINTINGS} paints 2-5
 * old motifs in red, yellow and black on the walls around it (Ochre Caves), {@link #TOOL_CACHE} leaves a chest of
 * chests/tool_cache, Stone Age tools someone stashed and never came back for (Soapstone Grottos). Placement:
 * scripts/gen_caves.py.
 */
final class CaveFinds extends StoneAgeFeature {
    enum Kind { PAINTINGS, TOOL_CACHE }

    static final CaveFinds PAINTINGS = new CaveFinds(Kind.PAINTINGS), TOOL_CACHE = new CaveFinds(Kind.TOOL_CACHE);
    private static final Motif[] MOTIFS = {Motif.AUROCHS, Motif.HORSE, Motif.DEER, Motif.BEAR, Motif.MAMMOTH, Motif.IBEX, Motif.HUNTER,
            Motif.SUN, Motif.SPIRAL, Motif.DOTS, Motif.HAND};
    private static final Colour[] COLOURS = {Colour.RED, Colour.RED, Colour.YELLOW, Colour.BLACK};

    private final Kind kind;
    private final MapCodec<CaveFinds> codec;

    private CaveFinds(Kind kind) {
        this.kind = kind;
        this.codec = MapCodec.unit(this);
    }

    @Override
    public MapCodec<CaveFinds> codec() {
        return codec;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockPos c = floor(level, origin);
        if (c == null) return false;
        if (kind == Kind.TOOL_CACHE) {
            set(level, c, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, randomSide(random)));
            RandomizableContainer.setBlockEntityLootTable(level, random, c,
                    ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(Mechanical.MODID, "chests/tool_cache")));
            return true;
        }
        int painted = 0;
        for (int i = 0; i < 24 && painted < 2 + random.nextInt(4); i++) {
            BlockPos p = c.offset(random.nextInt(9) - 4, random.nextInt(4), random.nextInt(9) - 4);
            Direction facing = randomSide(random);
            BlockPos wall = p.relative(facing.getOpposite());
            if (!level.getBlockState(p).isAir() || !level.getBlockState(wall).isFaceSturdy(level, wall, facing)) continue;
            set(level, p, Pigments.CAVE_PAINTING.get().defaultBlockState().setValue(CavePaintingBlock.FACING, facing)
                    .setValue(CavePaintingBlock.COLOUR, COLOURS[random.nextInt(COLOURS.length)])
                    .setValue(CavePaintingBlock.MOTIF, MOTIFS[random.nextInt(MOTIFS.length)]));
            painted++;
        }
        return painted > 0;
    }

    /** Air over a sturdy floor, out of the sky, within 12 blocks above or below origin. */
    private static BlockPos floor(WorldGenLevel level, BlockPos origin) {
        for (int i = 0; i < 24; i++) {
            BlockPos p = origin.above(i % 2 == 0 ? i / 2 : -(i / 2 + 1));
            if (level.getBlockState(p).isAir() && level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP) && !level.canSeeSky(p))
                return p;
        }
        return null;
    }
}
