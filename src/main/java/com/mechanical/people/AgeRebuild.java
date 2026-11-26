package com.mechanical.people;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.mechanical.Mechanical;
import com.mechanical.WorldAge;
import com.mechanical.people.Neighbours.Job;
import com.mechanical.people.Neighbours.Settlement;
import com.mechanical.worldgen.TribeVillageStructure;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * NPC tribes rebuild as they go up an age (WorldAge.npcAge): their old buildings come down and new ones go up in the new
 * age's style. Two kinds of settlement have buildings that are the tribe's:
 * <ul>
 * <li>tribe villages from worldgen: the layout follows from the village's seed and age
 * ({@link TribeVillageStructure#plan(long, BlockPos, com.mechanical.people.ClimateZone, int)}), so the rebuild is the
 * difference between the layout of the age it stands in and of the new one. Each village is noted here ({@link Village})
 * when the chunk it started in loads, with the age it is built in;</li>
 * <li>camps tribes built by a player camp (Settlements): their age and rebuild are saved with them; only blocks the camp built
 * ({@link Settlement#built}) are touched.</li>
 * </ul>
 * A rebuild is planned once per age step, when the whole settlement is loaded, as a list of {@link Job}s (tear-downs first,
 * roofs first, then the new buildings). With no player within {@link #NEAR} blocks the whole list is done at once; with one
 * near, a member of the tribe does it block by block (BuildCampGoal), until the player leaves. A job is done only while its
 * spot still holds the tribe's block or is free, so player blocks stay. Rebuilding costs a camp nothing.
 * Worldgen camps (TribeCamp), fishing camps and hunting bands' fires are not rebuilt: their blocks are not known.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class AgeRebuild {
    static final int CHECK = 100, NEAR = 64, SEEN = 40;
    private static final ResourceKey<net.minecraft.world.level.storage.loot.LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE,
            Identifier.fromNamespaceAndPath(Mechanical.MODID, "chests/tribe_village"));
    /** Villages whose starting chunk loaded (on any thread); taken into {@link Villages} on the next check. */
    private static final ConcurrentLinkedQueue<Village> FOUND = new ConcurrentLinkedQueue<>();

    /** A tribe village from worldgen: where, its layout seed and climate, the age it stands in, and what is left of its rebuild. */
    public static final class Village {
        public final BlockPos centre;
        public final long seed;
        public final ClimateZone climate;
        public final UUID tribe;
        public int age;
        public final List<Job> jobs;

        Village(BlockPos centre, long seed, int climate, int age, List<Job> jobs) {
            this.centre = centre;
            this.seed = seed;
            this.climate = ClimateZone.byId(climate);
            this.tribe = TribeVillageStructure.tribeOf(seed);
            this.age = age;
            this.jobs = new ArrayList<>(jobs);
        }

        static final Codec<Village> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockPos.CODEC.fieldOf("centre").forGetter(v -> v.centre),
                Codec.LONG.fieldOf("seed").forGetter(v -> v.seed),
                Codec.INT.fieldOf("climate").forGetter(v -> v.climate.ordinal()),
                Codec.INT.fieldOf("age").forGetter(v -> v.age),
                Job.CODEC.listOf().fieldOf("jobs").forGetter(v -> v.jobs)).apply(i, Village::new));
    }

    /** The villages known so far (overworld). */
    public static final class Villages extends SavedData {
        private static final Codec<Villages> CODEC = Village.CODEC.listOf().xmap(Villages::new, d -> d.villages);
        private static final SavedDataType<Villages> TYPE = new SavedDataType<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "village_ages"),
                () -> new Villages(List.of()), CODEC);
        public final List<Village> villages;

        private Villages(List<Village> villages) {
            this.villages = new ArrayList<>(villages);
        }

        public static Villages get(ServerLevel level) {
            return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
        }
    }

    @SubscribeEvent
    static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        var structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(TribeVillageStructure.ID);
        var start = structure == null ? null : event.getChunk().getStartForStructure(structure);
        if (start != null && start.isValid() && start.getPieces().getFirst() instanceof TribeVillageStructure.Piece piece)
            found(piece);
    }

    /** A village built just now or whose start chunk loaded; /structure hands its own in, as its chunk is loaded already. */
    public static void found(TribeVillageStructure.Piece piece) {
        FOUND.add(new Village(piece.centre(), piece.seed(), piece.climate().ordinal(), WorldAge.STONE, List.of())); // generated in the Stone Age
    }

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD || level.getGameTime() % CHECK != 0) return;
        Villages villages = Villages.get(level);
        for (Village v; (v = FOUND.poll()) != null;) {
            BlockPos c = v.centre;
            if (villages.villages.stream().noneMatch(o -> o.centre.equals(c))) {
                villages.villages.add(v);
                villages.setDirty();
            }
        }
        for (Village v : villages.villages) if (update(level, v)) villages.setDirty();
        Neighbours data = Neighbours.get(level);
        for (Settlement s : data.settlements) if (update(level, s)) data.setDirty();
    }

    /** Plans a village's rebuild when its tribe has gone up an age, and swaps it at once while no player is near. True if anything changed. */
    public static boolean update(ServerLevel level, Village v) {
        int r = TribeVillageStructure.HALF, x = v.centre.getX(), z = v.centre.getZ();
        if (!level.hasChunksAt(x - r, z - r, x + r, z + r)) return false; // only a whole village (its chunks all generated)
        boolean changed = false;
        int target = WorldAge.npcAge(level.getServer(), v.tribe);
        if (v.jobs.isEmpty() && v.age < target) {
            v.jobs.addAll(diff(TribeVillageStructure.plan(v.seed, v.centre, v.climate, v.age), TribeVillageStructure.plan(v.seed, v.centre, v.climate, target),
                    v.centre.getY()));
            v.age = target;
            changed = true;
        }
        changed |= swap(level, v.jobs, v.centre, v.centre.getY(), null);
        if (v.jobs.isEmpty()) seen(level, v.centre, v.age);
        return changed;
    }

    /** The same for a camp a tribe built (once its first stage stands and it is not halfway through a stage). */
    public static boolean update(ServerLevel level, Settlement s) {
        int r = Settlements.PALISADE_R + 2, x = s.centre.getX(), z = s.centre.getZ();
        if (!s.hasFire() || !level.hasChunksAt(x - r, z - r, x + r, z + r)) return false;
        boolean changed = false;
        int target = WorldAge.npcAge(level.getServer(), s.tribe);
        if (s.rebuild.isEmpty() && s.age < target && s.stage >= 1 && (!s.started || s.stage >= 3)) {
            s.rebuild.addAll(Settlements.rebuild(level, s, target));
            changed = true;
        }
        changed |= swap(level, s.rebuild, s.centre, Integer.MIN_VALUE, s);
        if (s.rebuild.isEmpty()) seen(level, s.centre, s.age);
        return changed;
    }

    /** Jobs that turn layout {@code from} into layout {@code to}: what goes (top first; ground below {@code floor} stays), then what is new or changed, in building order. */
    static List<Job> diff(Map<BlockPos, BlockState> from, Map<BlockPos, BlockState> to, int floor) {
        BlockState air = Blocks.AIR.defaultBlockState();
        List<Job> jobs = new ArrayList<>();
        from.entrySet().stream().filter(e -> !to.containsKey(e.getKey()) && e.getKey().getY() >= floor)
                .sorted(Comparator.comparingInt((Map.Entry<BlockPos, BlockState> e) -> e.getKey().getY()).reversed())
                .forEach(e -> jobs.add(new Job(e.getKey(), e.getValue(), air)));
        to.forEach((pos, state) -> {
            BlockState was = from.get(pos);
            if (!state.equals(was)) jobs.add(new Job(pos, was == null ? air : was, state));
        });
        return jobs;
    }

    private static boolean playerNear(ServerLevel level, BlockPos centre) {
        return level.players().stream().anyMatch(p -> !p.isSpectator() && Math.abs(p.getX() - centre.getX()) < NEAR && Math.abs(p.getZ() - centre.getZ()) < NEAR);
    }

    /** Nobody near: the whole rest of the rebuild at once (as worldgen places blocks: no drops, no neighbour updates). */
    private static boolean swap(ServerLevel level, List<Job> jobs, BlockPos centre, int floor, Settlement camp) {
        if (jobs.isEmpty() || playerNear(level, centre)) return false;
        for (Job j : jobs) apply(level, j, floor, camp, false);
        jobs.clear();
        return true;
    }

    /** The job can be done: its spot doesn't hold the new block yet, and holds the tribe's old block, or is free (or plain ground under the floor). */
    static boolean ready(ServerLevel level, Job j, int floor) {
        BlockState now = level.getBlockState(j.pos());
        if (now.equals(j.to())) return false;
        if (!j.from().isAir() && now.is(j.from().getBlock())) return true;
        if (j.to().isAir()) return false;
        return now.canBeReplaced() && level.getFluidState(j.pos()).isEmpty()
                || j.pos().getY() < floor && (now.is(BlockTags.SUBSTRATE_OVERWORLD) || now.is(Blocks.DIRT_PATH));
    }

    /** Does one job ({@code slow}: in sight, with sound and the usual block updates). True if it changed the world. */
    static boolean apply(ServerLevel level, Job j, int floor, Settlement camp, boolean slow) {
        if (!level.isLoaded(j.pos()) || !ready(level, j, floor)) return false;
        BlockPos pos = j.pos();
        if (j.to().isAir()) {
            if (slow) level.destroyBlock(pos, false);
            else level.setBlock(pos, j.to(), Block.UPDATE_CLIENTS);
        } else {
            level.setBlock(pos, j.to(), slow ? Block.UPDATE_ALL : Block.UPDATE_CLIENTS);
            if (slow) {
                var sound = j.to().getSoundType();
                level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1) / 2, sound.getPitch() * 0.8F);
            }
            if (camp == null && (j.to().is(Blocks.DECORATED_POT) || j.to().is(Blocks.BARREL)))
                RandomizableContainer.setBlockEntityLootTable(level, level.getRandom(), pos, LOOT); // a new store in a village
        }
        if (camp != null) {
            Long key = pos.asLong();
            camp.built.remove(key);
            if (!j.to().isAir()) camp.built.add(key);
        }
        return true;
    }

    // ---- block by block, while a player is near (BuildCampGoal) ----

    private record Site(List<Job> jobs, int floor, Settlement camp, SavedData data) {}

    private static Site site(ServerLevel level, UUID tribe, BlockPos near) {
        if (tribe == null) return null;
        Neighbours n = Neighbours.get(level);
        Settlement s = n.settlement(tribe);
        if (s != null && !s.rebuild.isEmpty() && s.centre.closerThan(near, 64)) return new Site(s.rebuild, Integer.MIN_VALUE, s, n);
        Villages d = Villages.get(level);
        for (Village v : d.villages) if (!v.jobs.isEmpty() && v.tribe.equals(tribe) && v.centre.closerThan(near, 64)) return new Site(v.jobs, v.centre.getY(), null, d);
        return null;
    }

    /** The next block of {@code mob}'s tribe's rebuild for it to do (one builder per tribe), skipping any a player has taken over; null if none. */
    static Job nextJob(ServerLevel level, Tribesperson mob) {
        Site site = site(level, mob.group(), mob.blockPosition());
        if (site == null || !Settlements.claim(mob.group(), mob, level.getGameTime())) return null;
        while (!site.jobs.isEmpty() && !ready(level, site.jobs.getFirst(), site.floor)) {
            site.jobs.removeFirst();
            site.data.setDirty();
        }
        return site.jobs.isEmpty() ? null : site.jobs.getFirst();
    }

    /** Still the next job (it may have been swapped meanwhile). */
    static boolean current(ServerLevel level, Tribesperson mob, Job job) {
        Site site = site(level, mob.group(), mob.blockPosition());
        return site != null && !site.jobs.isEmpty() && site.jobs.getFirst() == job;
    }

    static void finish(ServerLevel level, Tribesperson mob, Job job) {
        Site site = site(level, mob.group(), mob.blockPosition());
        if (site == null || site.jobs.isEmpty() || site.jobs.getFirst() != job) return;
        site.jobs.removeFirst();
        apply(level, job, site.floor, site.camp, true);
        site.data.setDirty();
    }

    // ---- advancements ----

    /** Players by a settlement that stands rebuilt in the Bronze (Iron) Age have seen it. */
    private static void seen(ServerLevel level, BlockPos centre, int age) {
        if (age < WorldAge.BRONZE) return;
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || !p.blockPosition().closerThan(centre, SEEN)) continue;
            for (int a = WorldAge.BRONZE; a <= age; a++) {
                var holder = level.getServer().getAdvancements().get(Identifier.fromNamespaceAndPath(Mechanical.MODID, "bronze_age/tribe_village_" + WorldAge.AGES[a]));
                if (holder != null) p.getAdvancements().award(holder, "done");
            }
        }
    }

    private AgeRebuild() {}
}
