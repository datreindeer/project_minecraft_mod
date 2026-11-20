package com.mechanical.people;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

import com.mechanical.BloodMoon;
import com.mechanical.CampAge;
import com.mechanical.Mechanical;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Night raids on old camps (CampAge). At dusk each camp that has burned for {@link #RAID_AGE} or more, with a player
 * by it, may be picked (one raid a night, never on a blood moon). The raiders are the worst-disposed tribe the players
 * there know of (or that lives within {@link #TRIBE_RANGE}): hostile tribes come often, unfriendly ones less, neutral
 * wary/raider tribes rarely, friendly tribes never; with no such tribe a wandering band may still come, rarely.
 * Drums are heard from their direction for a while, then 3-7 armed raiders with torches come out of the dark (RaidGoal).
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class Raids {
    public static final long RAID_AGE = 5L * CampAge.DAY;
    static final int TRIBE_RANGE = 160, PLAYER_RANGE = 48, SPAWN_DIST = 40, MIN = 3, MAX = 7, DEFENCE_REWARD = 5;
    private static final long DUSK = 12500, DUSK_END = 13500;
    public static final SoundEvent DRUMS = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(Mechanical.MODID, "raid.drums"));

    record Pending(BlockPos camp, UUID tribe, boolean band, int hostility, double angle, long spawnAt, int size) {}

    private static final Map<Level, Pending> PENDING = new WeakHashMap<>();
    private static final Map<Level, Long> ROLLED = new WeakHashMap<>();
    /** Raids already counted as beaten off (so the reward is given once). */
    private static final Set<UUID> DEFENDED = new HashSet<>();

    /** Chance per night that a camp of {@code campTicks} is raided by a tribe at standing {@code rep} (or a wandering band). */
    public static float chance(long campTicks, int rep, boolean raiderTribe, boolean band) {
        if (campTicks < RAID_AGE) return 0;
        float days = (campTicks - RAID_AGE) / (float) CampAge.DAY;
        float base = Math.min(0.25F, 0.05F + 0.02F * days);
        if (band) return base * 0.3F;
        float m = rep <= Tribe.HOSTILE ? 3F : rep < 0 ? 1.5F : 0.75F;
        if (raiderTribe) m *= 1.5F;
        return Math.min(0.7F, base * m);
    }

    record Choice(UUID tribe, int rep, boolean band) {}

    /** A Fire Pit burns in the camp: raids come more often and bigger (FirePit.RAID_CHANCE, FirePit.RAID_EXTRA). */
    static boolean firePitAt(ServerLevel level, BlockPos camp) {
        return com.mechanical.NightDangers.litFires(level).stream().anyMatch(f -> f.closerThan(camp, CampAge.RADIUS) && com.mechanical.NightDangers.reach(level, f) > 1);
    }

    /** The worst-disposed tribe that would raid (not friendly; neutral only if wary or raiders), else a wandering band. */
    static Choice chooseTribe(ServerLevel level, BlockPos camp, List<ServerPlayer> players) {
        Set<UUID> ids = new HashSet<>();
        for (ServerPlayer p : players) {
            for (String key : p.getData(Tribe.REPUTATION).keySet()) {
                try { ids.add(UUID.fromString(key)); } catch (IllegalArgumentException ignored) {}
            }
        }
        for (Tribesperson t : level.getEntitiesOfClass(Tribesperson.class, new AABB(camp).inflate(TRIBE_RANGE), t -> t.group() != null && t.raid == null)) {
            ids.add(t.group());
        }
        for (Neighbours.Settlement s : Neighbours.get(level).settlementsNear(camp, TRIBE_RANGE)) ids.add(s.tribe); // settled neighbours, loaded or not
        Choice best = null;
        int bestScore = Integer.MAX_VALUE;
        for (UUID id : ids) {
            Tribe tribe = Tribe.of(id);
            int rep = players.stream().mapToInt(tribe::reputation).min().orElse(tribe.temperament.start);
            boolean keen = tribe.temperament == Tribe.Temperament.WARY || tribe.temperament == Tribe.Temperament.RAIDERS;
            if (rep >= Tribe.FRIENDLY || rep >= 0 && !keen) continue;
            int score = rep - (tribe.temperament == Tribe.Temperament.RAIDERS ? 20 : 0);
            if (score < bestScore) { bestScore = score; best = new Choice(id, rep, false); }
        }
        if (best != null) return best;
        UUID band;
        do band = UUID.randomUUID(); while (Tribe.of(band).temperament != Tribe.Temperament.RAIDERS);
        return new Choice(band, 0, true);
    }

    private static List<ServerPlayer> playersAt(ServerLevel level, BlockPos camp) {
        return level.players().stream().filter(p -> !p.isSpectator() && !p.isCreative() && p.blockPosition().closerThan(camp, PLAYER_RANGE)).toList();
    }

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD || level.getGameTime() % 20 != 0) return;
        long time = level.getOverworldClockTime(), night = time / 24000, dayTime = time % 24000;
        Pending pending = PENDING.get(level);
        if (pending != null) {
            if (level.getGameTime() >= pending.spawnAt) {
                PENDING.remove(level);
                spawn(level, pending);
            } else if (level.getGameTime() % 80 != 60) { // boom, boom, boom, rest
                drums(level, pending);
            }
            return;
        }
        if (dayTime < DUSK || dayTime >= DUSK_END || Long.valueOf(night).equals(ROLLED.get(level))) return;
        ROLLED.put(level, night);
        if (BloodMoon.isBloodNight(level, night)) return; // the dead walk tonight; the tribes stay home
        RandomSource random = level.getRandom();
        for (CampAge.Camp camp : CampAge.get(level).camps()) {
            if (camp.ticks < RAID_AGE || camp.raidedNight == night) continue;
            List<ServerPlayer> players = playersAt(level, camp.centre);
            if (players.isEmpty()) continue;
            Choice c = chooseTribe(level, camp.centre, players);
            float pit = firePitAt(level, camp.centre) ? com.mechanical.FirePit.RAID_CHANCE : 1; // its smoke column is seen from afar
            if (random.nextFloat() < pit * chance(camp.ticks, c.rep, Tribe.of(c.tribe).temperament == Tribe.Temperament.RAIDERS, c.band)) {
                start(level, camp, c, level.getGameTime() + 1200 + random.nextInt(2400));
                return;
            }
        }
    }

    /** Drums from the raiders' side, and word to the players; they come at {@code spawnAt}. */
    static void start(ServerLevel level, CampAge.Camp camp, Choice c, long spawnAt) {
        RandomSource random = level.getRandom();
        long days = camp.ticks / CampAge.DAY;
        int size = c.band ? MIN + random.nextInt(2)
                : Math.min(MAX, MIN + random.nextInt(3) + (c.rep <= Tribe.HOSTILE ? 1 : 0) + (int) (days / 10));
        if (firePitAt(level, camp.centre)) size += com.mechanical.FirePit.RAID_EXTRA; // a big fire draws a bigger war party
        Pending p = new Pending(camp.centre, c.tribe, c.band, c.rep, random.nextDouble() * Math.PI * 2, spawnAt, size);
        PENDING.put(level, p);
        CampAge.get(level).markRaided(camp, level.getOverworldClockTime() / 24000);
        for (ServerPlayer player : playersAt(level, camp.centre)) {
            player.sendSystemMessage(Component.translatable("message.mechanical.raid.drums", direction(p)).withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        }
        drums(level, p);
    }

    private static Component direction(Pending p) {
        return Component.translatable("message.mechanical.raid.dir." + Direction.getApproximateNearest(Math.cos(p.angle), 0, Math.sin(p.angle)).getName());
    }

    /** A few beats, heard by each player from the raiders' direction. */
    private static void drums(ServerLevel level, Pending p) {
        for (ServerPlayer player : playersAt(level, p.camp)) {
            double x = player.getX() + Math.cos(p.angle) * 20, z = player.getZ() + Math.sin(p.angle) * 20;
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                    net.minecraft.core.Holder.direct(DRUMS), SoundSource.HOSTILE, x, player.getY(), z, 2.0F, 0.9F + level.getRandom().nextFloat() * 0.2F, level.getRandom().nextLong()));
        }
    }

    private static void spawn(ServerLevel level, Pending p) {
        RandomSource random = level.getRandom();
        BlockPos from = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                p.camp.offset((int) (Math.cos(p.angle) * SPAWN_DIST), 0, (int) (Math.sin(p.angle) * SPAWN_DIST)));
        UUID raid = UUID.randomUUID();
        Tribe tribe = Tribe.of(p.tribe);
        long now = level.getGameTime();
        int spawned = 0;
        for (int i = 0; i < p.size; i++) {
            BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, from.offset(random.nextInt(7) - 3, 0, random.nextInt(7) - 3));
            Tribesperson t = People.TRIBESPERSON.get().create(level, EntitySpawnReason.EVENT);
            if (t == null) continue;
            boolean leader = i == 0;
            t.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            t.setup(leader || random.nextBoolean() ? TribeRole.HUNTER : TribeRole.WOODCUTTER, null, p.tribe, leader);
            t.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.EVENT, null);
            t.makeRaider(new RaidGoal.State(raid, p.camp, from, p.size, now, leader ? 2 : 1));
            level.addFreshEntity(t);
            spawned++;
        }
        if (spawned == 0) return;
        Component who = p.band ? Component.translatable("message.mechanical.raid.band") : tribe.name();
        for (ServerPlayer player : playersAt(level, p.camp)) {
            player.sendSystemMessage(Component.translatable("message.mechanical.raid.coming", who, direction(p)).withStyle(ChatFormatting.RED));
            level.playSound(null, player.getX() + Math.cos(p.angle) * 16, player.getY(), player.getZ() + Math.sin(p.angle) * 16,
                    SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 4.0F, 0.7F);
        }
    }

    /** Half the party fell: the players at the camp beat them off. The tribe respects that a little. */
    static void defended(ServerLevel level, RaidGoal.State r, Tribe tribe) {
        if (!DEFENDED.add(r.id)) return;
        for (ServerPlayer player : playersAt(level, r.camp)) {
            if (tribe != null) tribe.addReputation(player, DEFENCE_REWARD);
            player.sendSystemMessage(Component.translatable("message.mechanical.raid.defended").withStyle(ChatFormatting.GREEN));
        }
    }

    /** Action bar note to the players at the camp. */
    static void tell(ServerLevel level, BlockPos camp, Component message) {
        for (ServerPlayer player : playersAt(level, camp)) player.sendOverlayMessage(message.copy().withStyle(ChatFormatting.RED));
    }

    @SubscribeEvent
    static void register(RegisterEvent event) {
        event.register(Registries.SOUND_EVENT, DRUMS.location(), () -> DRUMS);
    }

    /** /raid: a raid on the camp where you stand (or right here), coming in a few seconds (ops, for testing). */
    @SubscribeEvent
    static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("raid").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(ctx -> {
            ServerLevel level = ctx.getSource().getLevel();
            BlockPos pos = BlockPos.containing(ctx.getSource().getPosition());
            CampAge data = CampAge.get(level);
            CampAge.Camp camp = data.find(pos);
            if (camp == null) camp = data.burn(pos, 0);
            Choice c = chooseTribe(level, camp.centre, playersAt(level, camp.centre));
            start(level, camp, c, level.getGameTime() + 100);
            Component who = c.band ? Component.translatable("message.mechanical.raid.band") : Tribe.of(c.tribe).name();
            ctx.getSource().sendSuccess(() -> Component.literal("Raid on camp at " + PENDING.get(level).camp.toShortString() + " by ").append(who), true);
            return 1;
        }));
    }

    private Raids() {}
}
