package com.mechanical.wildlife;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.mechanical.Mechanical;
import com.mechanical.NightDangers;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Game comes back: animals are only placed when a chunk is made, so hunted-out land would stay empty. Every 30 s, for each
 * player, a spot 48-96 blocks away is looked at; if its 128x128 area hasn't been restocked for {@link #RESTOCK_DAYS} days
 * and few animals live there, one group of whatever the biome spawns (its creature list) walks in, out of sight.
 * The time each area was last stocked is saved with the world; an area seen for the first time counts as stocked now.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class WildlifeRestock extends SavedData {
    static final int RESTOCK_DAYS = 3, CHECK_TICKS = 600, SPARSE = 3, AREA = 128;
    private static final Codec<WildlifeRestock> CODEC = Codec.unboundedMap(Codec.STRING, Codec.LONG)
            .xmap(m -> new WildlifeRestock(new HashMap<>(m)), d -> d.stocked);
    private static final SavedDataType<WildlifeRestock> TYPE = new SavedDataType<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "wildlife_restock"),
            () -> new WildlifeRestock(new HashMap<>()), CODEC);

    /** Area key "x,z" (in AREA-block cells) -> game time it was last stocked. */
    private final Map<String, Long> stocked;

    private WildlifeRestock(Map<String, Long> stocked) {
        this.stocked = stocked;
    }

    /** Is the area due (and mark it stocked now if so, or if it's new)? */
    boolean due(BlockPos pos, long now) {
        String key = Math.floorDiv(pos.getX(), AREA) + "," + Math.floorDiv(pos.getZ(), AREA);
        Long last = stocked.get(key);
        if (last != null && now - last < RESTOCK_DAYS * 24000L) return false;
        stocked.put(key, now);
        setDirty();
        return last != null;
    }

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD || level.getGameTime() % CHECK_TICKS != 0) return;
        WildlifeRestock data = level.getDataStorage().computeIfAbsent(TYPE);
        RandomSource random = level.getRandom();
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) continue;
            double angle = random.nextDouble() * Math.PI * 2, dist = 48 + random.nextInt(48);
            BlockPos at = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    BlockPos.containing(player.getX() + Math.cos(angle) * dist, 0, player.getZ() + Math.sin(angle) * dist));
            if (!level.isLoaded(at) || level.getEntitiesOfClass(Animal.class, new AABB(at).inflate(48)).size() >= SPARSE) continue;
            if (data.due(at, level.getGameTime())) spawnGroup(level, at, random);
        }
    }

    /** One group from the biome's creature list, spread around {@code at}, away from players and fires. */
    static void spawnGroup(ServerLevel level, BlockPos at, RandomSource random) {
        MobSpawnSettings settings = level.environmentAttributes().getValue(EnvironmentAttributes.NATURAL_MOB_SPAWNS, at);
        Optional<MobSpawnSettings.SpawnerData> pick = settings.getMobsToSpawn(MobCategory.CREATURE).getRandom(random);
        if (pick.isEmpty() || NightDangers.nearFire(level, Vec3.atCenterOf(at), 32)) return;
        MobSpawnSettings.SpawnerData spawn = pick.get();
        SpawnGroupData group = null;
        int count = spawn.count().sample(random);
        for (int i = 0; i < count; i++) {
            BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.offset(random.nextInt(9) - 4, 0, random.nextInt(9) - 4));
            if (level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 32, false) != null
                    || !SpawnPlacements.isSpawnPositionOk(spawn.type(), level, pos)
                    || !level.noCollision(spawn.type().getSpawnAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5))) continue;
            if (!(spawn.type().create(level, EntitySpawnReason.NATURAL) instanceof Mob mob)) continue;
            mob.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360, 0);
            group = mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.NATURAL, group);
            level.addFreshEntity(mob);
        }
    }
}
