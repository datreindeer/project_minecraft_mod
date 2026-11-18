package com.mechanical;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Farm animals no longer spawn in the wild (biome modifier); they live in villages. The first time a player comes near
 * a village its bell gets a pen nearby (7x7 fence ring with a gate) with a few animals; after that the village slowly
 * restocks the pen whenever few farm animals are left around it. The pen's position is remembered on the bell.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class VillageAnimals {
    private static final int CHECK_TICKS = 600, VILLAGE_RANGE = 48, STOCK_RANGE = 32, STOCK_TARGET = 5, PEN_RADIUS = 3;
    /** Not visited yet. */
    private static final long NONE = 0L;
    /** Visited, but no room for a pen: animals are put near the bell instead. */
    private static final long NO_PEN = 1L;

    /** On the bell block entity: the pen's center (BlockPos.asLong) or NONE / NO_PEN. */
    public static final Supplier<AttachmentType<Long>> PEN = Mechanical.ATTACHMENTS.register("village_pen", () -> AttachmentType
            .builder(() -> NONE)
            .serialize(Codec.LONG.fieldOf("value"))
            .build());

    private static final List<Supplier<EntityType<? extends Animal>>> STOCK = List.of(
            () -> EntityTypes.COW, () -> EntityTypes.COW, () -> EntityTypes.SHEEP, () -> EntityTypes.SHEEP,
            () -> EntityTypes.PIG, () -> EntityTypes.PIG, () -> EntityTypes.CHICKEN, () -> EntityTypes.CHICKEN);

    public static void init() {}

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD || level.getGameTime() % CHECK_TICKS != 0) return;
        for (ServerPlayer player : level.players()) {
            Optional<BlockPos> bell = level.getPoiManager().findClosest(p -> p.is(PoiTypes.MEETING), player.blockPosition(), VILLAGE_RANGE, PoiManager.Occupancy.ANY);
            bell.ifPresent(pos -> tend(level, pos));
        }
    }

    private static void tend(ServerLevel level, BlockPos bell) {
        BlockEntity holder = level.getBlockEntity(bell);
        if (holder == null) return;
        long pen = holder.getData(PEN);
        RandomSource random = level.getRandom();
        if (pen == NONE) {
            BlockPos center = findPenSpot(level, bell, random);
            if (center != null) buildPen(level, center, random);
            pen = center != null ? center.asLong() : NO_PEN;
            holder.setData(PEN, pen);
            holder.setChanged();
            if (center != null) {
                for (int i = 0; i < 3; i++) spawnAnimal(level, center, random);
                return;
            }
        }
        BlockPos home = pen == NO_PEN ? bell : BlockPos.of(pen);
        int animals = level.getEntitiesOfClass(Animal.class, new AABB(home).inflate(STOCK_RANGE), VillageAnimals::isFarmAnimal).size();
        if (animals < STOCK_TARGET) spawnAnimal(level, home, random);
    }

    private static boolean isFarmAnimal(Animal animal) {
        return STOCK.stream().anyMatch(t -> t.get() == animal.getType()) || animal.getType() == EntityTypes.HORSE;
    }

    /** A flat, dry, empty 7x7 patch of ground 8-20 blocks from the bell, off the paths. */
    private static BlockPos findPenSpot(ServerLevel level, BlockPos bell, RandomSource random) {
        for (int attempt = 0; attempt < 40; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            int dist = 8 + random.nextInt(13);
            BlockPos center = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bell.offset((int) (Math.cos(angle) * dist), 0, (int) (Math.sin(angle) * dist)));
            if (isFlatGround(level, center)) return center;
        }
        return null;
    }

    private static boolean isFlatGround(ServerLevel level, BlockPos center) {
        for (int dx = -PEN_RADIUS; dx <= PEN_RADIUS; dx++) {
            for (int dz = -PEN_RADIUS; dz <= PEN_RADIUS; dz++) {
                BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, center.offset(dx, 0, dz));
                if (top.getY() != center.getY()) return false;
                BlockState ground = level.getBlockState(top.below());
                if (!(ground.is(BlockTags.DIRT) || ground.is(BlockTags.SAND)) || ground.is(Blocks.DIRT_PATH)) return false;
                if (!level.getBlockState(top).isAir() && !level.getBlockState(top).canBeReplaced()) return false;
                if (!level.getFluidState(top).isEmpty()) return false;
            }
        }
        return true;
    }

    private static void buildPen(ServerLevel level, BlockPos center, RandomSource random) {
        Direction gateSide = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        BlockPos gate = center.relative(gateSide, PEN_RADIUS);
        for (int dx = -PEN_RADIUS; dx <= PEN_RADIUS; dx++) {
            for (int dz = -PEN_RADIUS; dz <= PEN_RADIUS; dz++) {
                BlockPos pos = center.offset(dx, 0, dz);
                boolean edge = Math.abs(dx) == PEN_RADIUS || Math.abs(dz) == PEN_RADIUS;
                if (!edge) {
                    if (!level.getBlockState(pos).isAir()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL); // clear grass
                    continue;
                }
                BlockState state = pos.equals(gate)
                        ? Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, gateSide.getClockWise())
                        : Blocks.OAK_FENCE.defaultBlockState();
                level.setBlock(pos, state, Block.UPDATE_ALL);
            }
        }
        // fences connect to each other only once all are placed
        for (int dx = -PEN_RADIUS; dx <= PEN_RADIUS; dx++) {
            for (int dz = -PEN_RADIUS; dz <= PEN_RADIUS; dz++) {
                if (Math.abs(dx) != PEN_RADIUS && Math.abs(dz) != PEN_RADIUS) continue;
                BlockPos pos = center.offset(dx, 0, dz);
                level.setBlock(pos, Block.updateFromNeighbourShapes(level.getBlockState(pos), level, pos), Block.UPDATE_ALL);
            }
        }
    }

    private static void spawnAnimal(ServerLevel level, BlockPos home, RandomSource random) {
        EntityType<? extends Animal> type = STOCK.get(random.nextInt(STOCK.size())).get();
        Animal animal = type.create(level, EntitySpawnReason.STRUCTURE);
        if (animal == null) return;
        int spread = PEN_RADIUS - 1;
        BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                home.offset(random.nextInt(spread * 2 + 1) - spread, 0, random.nextInt(spread * 2 + 1) - spread));
        animal.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360, 0);
        animal.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.STRUCTURE, null);
        level.addFreshEntity(animal);
    }

    private VillageAnimals() {}
}
