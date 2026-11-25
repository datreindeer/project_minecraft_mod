package com.mechanical.worldgen;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkGenerator;

/** A log hut with a thatched (hay) roof deep in the woods, home to one hermit: a leatherworker with his cauldron. */
public final class HermitsHut extends StoneAgeFeature {
    /** On the hermit: the middle of his hut's floor (BlockPos.asLong), for "now entering" (clan.Lands). */
    private static final String HOME = "mechanical_hermit_hut";
    static final MapCodec<HermitsHut> CODEC = MapCodec.unit(HermitsHut::new);

    @Override
    public MapCodec<HermitsHut> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockPos c = surface(level, origin.getX(), origin.getZ());
        if (!flat(level, c, 3, 1)) return false;
        levelGround(level, c, 3, 6);
        var biome = level.getBiome(c);
        boolean dark = biome.is(Biomes.DARK_FOREST), taiga = biome.is(BiomeTags.IS_TAIGA);
        Block log = dark ? Blocks.DARK_OAK_LOG : taiga ? Blocks.SPRUCE_LOG : Blocks.OAK_LOG;
        Block door = dark ? Blocks.DARK_OAK_DOOR : taiga ? Blocks.SPRUCE_DOOR : Blocks.OAK_DOOR;
        BlockState hay = Blocks.HAY_BLOCK.defaultBlockState();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                int ax = Math.abs(dx), az = Math.abs(dz);
                if (ax <= 2 && az <= 2 && (ax == 2 || az == 2)) {
                    // corners stand upright, wall logs lie along their wall
                    BlockState wall = ax == 2 && az == 2 ? log.defaultBlockState()
                            : log.defaultBlockState().setValue(RotatedPillarBlock.AXIS, ax == 2 ? Direction.Axis.Z : Direction.Axis.X);
                    for (int y = 0; y <= 2; y++) set(level, c.offset(dx, y, dz), wall);
                }
                set(level, c.offset(dx, 3, dz), hay);
                if (ax <= 1 && az <= 1) set(level, c.offset(dx, 4, dz), hay);
            }
        }
        Direction out = randomSide(random), p = out.getClockWise();
        BlockPos doorAt = c.relative(out, 2);
        set(level, doorAt, door.defaultBlockState().setValue(DoorBlock.FACING, out.getOpposite()).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        set(level, doorAt.above(), door.defaultBlockState().setValue(DoorBlock.FACING, out.getOpposite()).setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        set(level, c.relative(p, 2).above(), AIR); // windows
        set(level, c.relative(p, -2).above(), AIR);
        BlockState bed = Blocks.BED.pick(net.minecraft.world.item.DyeColor.BROWN).defaultBlockState().setValue(BedBlock.FACING, out.getOpposite());
        set(level, c.relative(out, -1).relative(p, 1), bed.setValue(BedBlock.PART, BedPart.HEAD));
        set(level, c.relative(p, 1), bed.setValue(BedBlock.PART, BedPart.FOOT));
        set(level, c.relative(out, -1).relative(p, -1), Blocks.CAULDRON.defaultBlockState());
        set(level, c.relative(p, -1), Blocks.CRAFTING_TABLE.defaultBlockState());
        pot(level, random, c.relative(out, 1).relative(p, -1), "hermits_hut");
        spawn(level, random, EntityTypes.VILLAGER, c, hermit -> {
            hermit.setVillagerData(hermit.getVillagerData().withProfession(level.registryAccess(), VillagerProfession.LEATHERWORKER));
            hermit.setVillagerXp(1); // keeps the profession even if the cauldron is never claimed
            hermit.getPersistentData().putLong(HOME, c.asLong());
        });
        return true;
    }

    /**
     * Where {@code v}'s hut stands, or null if he is no hermit. Hermits from worlds made before they were marked are known
     * by the hut round them, and marked when found.
     * shortcut: a hut has a name only while its hermit lives near it; save hut positions in the level if that is missed.
     */
    public static BlockPos home(Villager v) {
        var marked = v.getPersistentData().getLong(HOME);
        if (marked.isPresent()) return BlockPos.of(marked.get());
        if (!v.getVillagerData().profession().is(VillagerProfession.LEATHERWORKER)) return null;
        BlockPos at = v.blockPosition();
        for (BlockPos c : BlockPos.betweenClosed(at.offset(-6, -2, -6), at.offset(6, 2, 6))) {
            if (!isHut(v.level(), c)) continue;
            v.getPersistentData().putLong(HOME, c.asLong());
            return c.immutable();
        }
        return null;
    }

    /** The hut's own shape round floor middle {@code c}: a 7x7 hay eave under a hay cap, on four upright log corners. */
    private static boolean isHut(Level level, BlockPos c) {
        if (!level.getBlockState(c.above(4)).is(Blocks.HAY_BLOCK)) return false;
        for (int dx : new int[] {-1, 1})
            for (int dz : new int[] {-1, 1}) {
                if (!level.getBlockState(c.offset(3 * dx, 3, 3 * dz)).is(Blocks.HAY_BLOCK)) return false;
                if (!level.getBlockState(c.offset(2 * dx, 1, 2 * dz)).is(BlockTags.LOGS)) return false;
            }
        return true;
    }
}
