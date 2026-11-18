package com.mechanical.worldgen;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
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
final class HermitsHut extends StoneAgeFeature {
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
        });
        return true;
    }
}
