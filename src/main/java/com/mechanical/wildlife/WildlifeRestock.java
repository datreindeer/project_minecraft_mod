package com.mechanical.wildlife;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mechanical.Mechanical;
import com.mechanical.NightDangers;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
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
 * Game comes back: animals are only placed when a chunk is made, so hunted-out land would stay empty. Every 15 s, for each
 * player, a spot 48-96 blocks away is looked at; if its 128x128 area hasn't been restocked for {@link #RESTOCK_DAYS} days
 * (or never, so an old world's explored land fills up too) and few animals live there, one group walks in, out of sight.
 * The group is picked from the biome's spawn lists (creatures, plus our own water animals), preferring species with no living
 * member in the area, so species added in a newer version reach old worlds. When the mod's set of species changes, every
 * known area is due once more. Each member must pass its own spawn placement and spawn rules (water for fish, shore for seals).
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class WildlifeRestock extends SavedData {
    static final int RESTOCK_DAYS = 3, CHECK_TICKS = 300, SPARSE = 3, AREA = 128;
    /** Saved in the same map as the areas: a hash of the mod's species, so a changed set makes every area due once. */
    private static final String SPECIES_KEY = "#species";
    private static final List<MobCategory> CATEGORIES = List.of(MobCategory.CREATURE, MobCategory.WATER_CREATURE, MobCategory.WATER_AMBIENT);
    private static final Codec<WildlifeRestock> CODEC = Codec.unboundedMap(Codec.STRING, Codec.LONG)
            .xmap(m -> new WildlifeRestock(new HashMap<>(m)), d -> d.stocked);
    private static final SavedDataType<WildlifeRestock> TYPE = new SavedDataType<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "wildlife_restock"),
            () -> new WildlifeRestock(new HashMap<>()), CODEC);

    /** Area key "x,z" (in AREA-block cells) -> game time it was last stocked; plus {@link #SPECIES_KEY}. */
    private final Map<String, Long> stocked;

    private WildlifeRestock(Map<String, Long> stocked) {
        this.stocked = stocked;
    }

    /** Is the area due (never stocked, or not for RESTOCK_DAYS)? If so it counts as stocked now. */
    boolean due(BlockPos pos, long now) {
        String key = Math.floorDiv(pos.getX(), AREA) + "," + Math.floorDiv(pos.getZ(), AREA);
        Long last = stocked.get(key);
        if (last != null && now - last < RESTOCK_DAYS * 24000L) return false;
        stocked.put(key, now);
        setDirty();
        return true;
    }

    /** A new set of species (a mod update): forget when areas were stocked, so each is due once. */
    void checkSpecies(long fingerprint) {
        Long saved = stocked.get(SPECIES_KEY);
        if (saved != null && saved == fingerprint) return;
        stocked.clear();
        stocked.put(SPECIES_KEY, fingerprint);
        setDirty();
    }

    private static long fingerprint;

    /** Hash of our spawning entity ids (everything but MISC). */
    static long speciesFingerprint() {
        if (fingerprint == 0) {
            List<String> ids = new ArrayList<>();
            for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
                var id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
                if (id.getNamespace().equals(Mechanical.MODID) && type.getCategory() != MobCategory.MISC) ids.add(id.getPath());
            }
            ids.sort(null);
            fingerprint = String.join(",", ids).hashCode() | 1L << 40; // never 0
        }
        return fingerprint;
    }

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD || level.getGameTime() % CHECK_TICKS != 0) return;
        WildlifeRestock data = level.getDataStorage().computeIfAbsent(TYPE);
        data.checkSpecies(speciesFingerprint());
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

    /**
     * The spawn entry to restock with: weighted from the biome's lists, but only among species with no living member in the
     * area if there are any. Water lists only count our own animals (vanilla fish come back by themselves).
     */
    public static Optional<MobSpawnSettings.SpawnerData> pick(ServerLevel level, BlockPos at, RandomSource random) {
        AABB area = new AABB(at).inflate(AREA / 2.0);
        List<Weighted<MobSpawnSettings.SpawnerData>> all = candidates(level, at), missing = new ArrayList<>();
        for (Weighted<MobSpawnSettings.SpawnerData> entry : all) if (level.getEntities(entry.value().type(), area, Entity::isAlive).isEmpty()) missing.add(entry);
        return WeightedList.of(missing.isEmpty() ? all : missing).getRandom(random);
    }

    /** The biome's creature list plus the entries of its water lists that are ours. */
    public static List<Weighted<MobSpawnSettings.SpawnerData>> candidates(ServerLevel level, BlockPos at) {
        MobSpawnSettings settings = level.environmentAttributes().getValue(EnvironmentAttributes.NATURAL_MOB_SPAWNS, at);
        List<Weighted<MobSpawnSettings.SpawnerData>> all = new ArrayList<>();
        for (MobCategory category : CATEGORIES) {
            for (Weighted<MobSpawnSettings.SpawnerData> entry : settings.getMobsToSpawn(category).unwrap()) {
                if (category == MobCategory.CREATURE || BuiltInRegistries.ENTITY_TYPE.getKey(entry.value().type()).getNamespace().equals(Mechanical.MODID)) all.add(entry);
            }
        }
        return all;
    }

    /** One group spread around {@code at}, away from players and fires; each member at a spot its spawn rules accept. */
    static void spawnGroup(ServerLevel level, BlockPos at, RandomSource random) {
        if (NightDangers.nearFire(level, Vec3.atCenterOf(at), 32)) return;
        Optional<MobSpawnSettings.SpawnerData> pick = pick(level, at, random);
        if (pick.isEmpty()) return;
        EntityType<?> type = pick.get().type();
        SpawnGroupData group = null;
        int count = pick.get().count().sample(random);
        for (int i = 0; i < count; i++) {
            for (int tries = 0; tries < 3; tries++) {
                BlockPos pos = level.getHeightmapPos(SpawnPlacements.getHeightmapType(type), at.offset(random.nextInt(13) - 6, 0, random.nextInt(13) - 6));
                if (!canSpawnAt(level, type, pos, random)) continue;
                if (!(type.create(level, EntitySpawnReason.NATURAL) instanceof Mob mob)) return;
                mob.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360, 0);
                group = mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.NATURAL, group);
                level.addFreshEntity(mob);
                break;
            }
        }
    }

    private static boolean canSpawnAt(ServerLevel level, EntityType<?> type, BlockPos pos, RandomSource random) {
        return level.isLoaded(pos) && level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 32, false) == null
                && SpawnPlacements.isSpawnPositionOk(type, level, pos) && SpawnPlacements.checkSpawnRules(type, level, EntitySpawnReason.NATURAL, pos, random)
                && level.noCollision(type.getSpawnAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5));
    }
}
