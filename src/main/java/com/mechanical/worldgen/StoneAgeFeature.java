package com.mechanical.worldgen;

import java.util.function.Consumer;

import com.mechanical.Mechanical;
import com.mechanical.RockBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;

/** Shared helpers for the procedural Stone Age structures. Each structure stays within about 12x12 around its origin. */
abstract class StoneAgeFeature implements Feature {
    static final BlockState AIR = Blocks.AIR.defaultBlockState();

    static void set(WorldGenLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
    }

    /** First free block above solid ground at x,z (water counts as free, so a column under water has fluid here). */
    static BlockPos surface(WorldGenLevel level, int x, int z) {
        return new BlockPos(x, level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z), z);
    }

    static boolean natural(BlockState s) {
        return s.is(BlockTags.SUBSTRATE_OVERWORLD) || s.is(BlockTags.SAND) || s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(Blocks.GRAVEL)
                || s.is(BlockTags.TERRACOTTA); // badlands ground
    }

    /** Every column within r of c has dry, natural ground within tol blocks of c's height. */
    static boolean flat(WorldGenLevel level, BlockPos c, int r, int tol) {
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                BlockPos top = surface(level, c.getX() + dx, c.getZ() + dz);
                if (Math.abs(top.getY() - c.getY()) > tol || !natural(level.getBlockState(top.below()))
                        || !level.getFluidState(top).isEmpty() || !level.getFluidState(top.below()).isEmpty()) return false;
            }
        }
        return true;
    }

    /** Levels the square within r of c to c's height: clears h blocks above, fills dips with the ground block. */
    static void levelGround(WorldGenLevel level, BlockPos c, int r, int h) {
        BlockState ground = level.getBlockState(c.below());
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                BlockPos p = c.offset(dx, 0, dz);
                for (int y = 0; y < h; y++) if (!level.getBlockState(p.above(y)).isAir()) set(level, p.above(y), AIR);
                foundation(level, p.below(), ground);
            }
        }
    }

    /** Sets state at p and fills dirt below until solid ground, so nothing floats. */
    static void foundation(WorldGenLevel level, BlockPos p, BlockState state) {
        set(level, p, state);
        BlockPos q = p.below();
        for (int i = 0; i < 8 && !level.getBlockState(q).isSolid(); i++, q = q.below()) set(level, q, Blocks.DIRT.defaultBlockState());
    }

    static void pot(WorldGenLevel level, RandomSource random, BlockPos pos, String loot) {
        set(level, pos, Blocks.DECORATED_POT.defaultBlockState());
        RandomizableContainer.setBlockEntityLootTable(level, random, pos,
                ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(Mechanical.MODID, "chests/" + loot)));
    }

    /** A loose rock (or flint) of random size, only on sturdy ground. */
    static void rock(WorldGenLevel level, RandomSource random, BlockPos pos, Block rock) {
        if (level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP))
            set(level, pos, rock.defaultBlockState().setValue(RockBlock.SIZE, 1 + random.nextInt(3)));
    }

    /** Only on sturdy ground (bone piles, rugs and racks would pop off otherwise). */
    static void onGround(WorldGenLevel level, BlockPos pos, BlockState state) {
        if (level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) set(level, pos, state);
    }

    /** setup runs before the mob is added: in worldgen the chunk saves the entity right away, later changes are lost. */
    static <T extends Mob> void spawn(WorldGenLevel level, RandomSource random, EntityType<T> type, BlockPos pos, Consumer<T> setup) {
        T mob = type.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
        if (mob == null) return;
        mob.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360, 0);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.STRUCTURE, null);
        mob.setPersistenceRequired();
        setup.accept(mob);
        level.addFreshEntityWithPassengers(mob);
    }

    static Direction randomSide(RandomSource random) {
        return Direction.Plane.HORIZONTAL.getRandomDirection(random);
    }
}
