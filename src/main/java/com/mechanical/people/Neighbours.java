package com.mechanical.people;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.mechanical.Mechanical;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Saved state of the tribes that come to the players' camps (overworld): per player camp when the next band is due and
 * which tribes came before ({@link CampInfo}), the bands visiting right now ({@link Visit}), and the tribes that settled
 * nearby ({@link Settlement}: site, build stage, plan, stockpile). The people themselves are ordinary saved
 * {@link Tribesperson} entities (group = tribe id); the player's standing with each tribe is {@link Tribe#REPUTATION}.
 * Logic: {@link TribeVisits} and {@link Settlements}.
 */
public final class Neighbours extends SavedData {
    /** A player camp (CampAge centre) that bands visit. */
    public static final class CampInfo {
        public final BlockPos camp;
        public long nextVisit;
        /** Tribes that have visited here (they may come back). */
        public final List<UUID> known;
        /** When a tribe last settled near this camp (game time), so they come one at a time. */
        public long lastSettled;
        /** Bands that stayed the night here since a tribe last settled by this camp (the 4th settles for sure, TribeVisits). */
        public int stays;

        CampInfo(BlockPos camp, long nextVisit, List<UUID> known, long lastSettled, int stays) {
            this.camp = camp;
            this.nextVisit = nextVisit;
            this.known = new ArrayList<>(known);
            this.lastSettled = lastSettled;
            this.stays = stays;
        }

        static final Codec<CampInfo> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockPos.CODEC.fieldOf("camp").forGetter(c -> c.camp),
                Codec.LONG.fieldOf("next_visit").forGetter(c -> c.nextVisit),
                UUIDUtil.STRING_CODEC.listOf().fieldOf("known").forGetter(c -> c.known),
                Codec.LONG.optionalFieldOf("last_settled", -1L).forGetter(c -> c.lastSettled),
                Codec.INT.optionalFieldOf("stays", 0).forGetter(c -> c.stays)).apply(i, CampInfo::new));
    }

    /** A band visiting a player camp; SEEKING: it means to settle and roams round the camp looking for land (never given up). */
    public static final class Visit {
        public static final int ARRIVING = 0, AT_CAMP = 1, STAYING = 2, LEAVING = 3, SEEKING = 4;
        public final UUID id, tribe;
        public final BlockPos camp, from;
        /** The fire they make for (the camp's fire nearest its centre when they came); while SEEKING the spot they walk to next. */
        public BlockPos fire;
        public int state;
        /** Game time of the last change of state; while SEEKING when the band was last seen. */
        public long since;
        /** Day number (overworld clock / 24000) they came; they leave at the next dawn. */
        public final long day;
        public boolean asked;
        /** The player who let them stay. */
        public Optional<UUID> host;
        /** While SEEKING: spots walked to so far (they range wider the longer they look). */
        public int tries;

        Visit(UUID id, UUID tribe, BlockPos camp, BlockPos from, BlockPos fire, int state, long since, long day, boolean asked, Optional<UUID> host, int tries) {
            this.id = id;
            this.tribe = tribe;
            this.camp = camp;
            this.from = from;
            this.fire = fire;
            this.state = state;
            this.since = since;
            this.day = day;
            this.asked = asked;
            this.host = host;
            this.tries = tries;
        }

        static final Codec<Visit> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.STRING_CODEC.fieldOf("id").forGetter(v -> v.id),
                UUIDUtil.STRING_CODEC.fieldOf("tribe").forGetter(v -> v.tribe),
                BlockPos.CODEC.fieldOf("camp").forGetter(v -> v.camp),
                BlockPos.CODEC.fieldOf("from").forGetter(v -> v.from),
                BlockPos.CODEC.fieldOf("fire").forGetter(v -> v.fire),
                Codec.INT.fieldOf("state").forGetter(v -> v.state),
                Codec.LONG.fieldOf("since").forGetter(v -> v.since),
                Codec.LONG.fieldOf("day").forGetter(v -> v.day),
                Codec.BOOL.fieldOf("asked").forGetter(v -> v.asked),
                UUIDUtil.STRING_CODEC.optionalFieldOf("host").forGetter(v -> v.host),
                Codec.INT.optionalFieldOf("tries", 0).forGetter(v -> v.tries)).apply(i, Visit::new));
    }

    /** One block of a camp's build plan. */
    public record Placement(BlockPos pos, BlockState state) {
        static final Codec<Placement> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(Placement::pos),
                BlockState.CODEC.fieldOf("state").forGetter(Placement::state)).apply(i, Placement::new));
    }

    /**
     * One block of a rebuild as a tribe goes up an age (AgeRebuild): {@code from} is the tribe's block standing there (air for
     * an empty spot), {@code to} what goes there now (air to tear it down). Done only while the spot still holds {@code from}
     * or is free, so whatever a player put there stays.
     */
    public record Job(BlockPos pos, BlockState from, BlockState to) {
        static final Codec<Job> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(Job::pos),
                BlockState.CODEC.fieldOf("from").forGetter(Job::from),
                BlockState.CODEC.fieldOf("to").forGetter(Job::to)).apply(i, Job::new));
    }

    /** A tribe that settled near a player camp. */
    public static final class Settlement {
        public static final int WOOD = 0, STONE = 1, HIDES = 2;
        public final UUID tribe;
        /** The player camp it settled by, and its own centre (the campfire). */
        public final BlockPos anchor, centre;
        /** Toward the player camp: the camp is laid out around it and the palisade gate faces it. */
        public final Direction gate;
        /** Stages finished (0-3); {@link #plan} is the next stage, built from {@link #step} once {@link #started}. */
        public int stage, step;
        public boolean started;
        public List<Placement> plan;
        /** Stockpile: wood, stone, hides. */
        public final int[] stock;
        public final long founded;
        public long nextErrand;
        /** Blocks they built that still stand (breaking one in sight of them is theft). */
        public final List<Long> built;
        public Optional<BlockPos> rack;
        /** The age (WorldAge) its buildings are in, and what is left of the rebuild into it (AgeRebuild). */
        public int age;
        public List<Job> rebuild;
        /** While a rebuild is planned: the built blocks it may take down, which count as free ground. Not saved. */
        java.util.Set<Long> clearing;

        Settlement(UUID tribe, BlockPos anchor, BlockPos centre, Direction gate, int stage, int step, boolean started, List<Placement> plan,
                List<Integer> stock, long founded, long nextErrand, List<Long> built, Optional<BlockPos> rack, int age, List<Job> rebuild) {
            this.tribe = tribe;
            this.anchor = anchor;
            this.centre = centre;
            this.gate = gate;
            this.stage = stage;
            this.step = step;
            this.started = started;
            this.plan = new ArrayList<>(plan);
            this.stock = new int[3];
            for (int k = 0; k < 3 && k < stock.size(); k++) this.stock[k] = stock.get(k);
            this.founded = founded;
            this.nextErrand = nextErrand;
            this.built = new ArrayList<>(built);
            this.rack = rack;
            this.age = age;
            this.rebuild = new ArrayList<>(rebuild);
        }

        /** The camp's fire stands (the first block of the first stage is the fire). */
        public boolean hasFire() {
            return stage > 0 || step > 0;
        }

        public Tribe tribeInfo() {
            return Tribe.of(tribe);
        }

        static final Codec<Settlement> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.STRING_CODEC.fieldOf("tribe").forGetter(s -> s.tribe),
                BlockPos.CODEC.fieldOf("anchor").forGetter(s -> s.anchor),
                BlockPos.CODEC.fieldOf("centre").forGetter(s -> s.centre),
                Direction.CODEC.fieldOf("gate").forGetter(s -> s.gate),
                Codec.INT.fieldOf("stage").forGetter(s -> s.stage),
                Codec.INT.fieldOf("step").forGetter(s -> s.step),
                Codec.BOOL.fieldOf("started").forGetter(s -> s.started),
                Placement.CODEC.listOf().fieldOf("plan").forGetter(s -> s.plan),
                Codec.INT.listOf().fieldOf("stock").forGetter(s -> List.of(s.stock[0], s.stock[1], s.stock[2])),
                Codec.LONG.fieldOf("founded").forGetter(s -> s.founded),
                Codec.LONG.fieldOf("next_errand").forGetter(s -> s.nextErrand),
                Codec.LONG.listOf().fieldOf("built").forGetter(s -> s.built),
                BlockPos.CODEC.optionalFieldOf("rack").forGetter(s -> s.rack),
                Codec.INT.optionalFieldOf("age", 0).forGetter(s -> s.age),
                Job.CODEC.listOf().optionalFieldOf("rebuild", List.of()).forGetter(s -> s.rebuild)).apply(i, Settlement::new));
    }

    private static final Codec<Neighbours> CODEC = RecordCodecBuilder.create(i -> i.group(
            CampInfo.CODEC.listOf().fieldOf("camps").forGetter(n -> n.camps),
            Visit.CODEC.listOf().fieldOf("visits").forGetter(n -> n.visits),
            Settlement.CODEC.listOf().fieldOf("settlements").forGetter(n -> n.settlements)).apply(i, Neighbours::new));
    private static final SavedDataType<Neighbours> TYPE = new SavedDataType<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "neighbours"),
            () -> new Neighbours(List.of(), List.of(), List.of()), CODEC);

    public final List<CampInfo> camps;
    public final List<Visit> visits;
    public final List<Settlement> settlements;

    private Neighbours(List<CampInfo> camps, List<Visit> visits, List<Settlement> settlements) {
        this.camps = new ArrayList<>(camps);
        this.visits = new ArrayList<>(visits);
        this.settlements = new ArrayList<>(settlements);
    }

    /** Always the overworld's data (bands and camps live there). */
    public static Neighbours get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public CampInfo camp(BlockPos centre) {
        for (CampInfo c : camps) if (c.camp.equals(centre)) return c;
        return null;
    }

    public Visit visit(UUID id) {
        if (id == null) return null;
        for (Visit v : visits) if (v.id.equals(id)) return v;
        return null;
    }

    /** The band visiting this camp now (one looking for land nearby does not count: others may still come). */
    public Visit visitAt(BlockPos camp) {
        for (Visit v : visits) if (v.camp.equals(camp) && v.state != Visit.SEEKING) return v;
        return null;
    }

    /** How many bands visit this camp now (not counting ones looking for land). */
    public int visitsAt(BlockPos camp) {
        int n = 0;
        for (Visit v : visits) if (v.camp.equals(camp) && v.state != Visit.SEEKING) n++;
        return n;
    }

    public Settlement settlement(UUID tribe) {
        if (tribe == null) return null;
        for (Settlement s : settlements) if (s.tribe.equals(tribe)) return s;
        return null;
    }

    /** Settlements whose centre is within {@code range} of {@code pos}. */
    public List<Settlement> settlementsNear(BlockPos pos, double range) {
        return settlements.stream().filter(s -> s.centre.closerThan(pos, range)).toList();
    }

    /** The settlement that built the block at {@code pos}, or null. */
    public Settlement builtBy(BlockPos pos) {
        long key = pos.asLong();
        for (Settlement s : settlements) if (s.centre.closerThan(pos, 16) && s.built.contains(key)) return s;
        return null;
    }
}
