package com.mechanical.dashboard;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.mechanical.Calendar;
import com.mechanical.CampAge;
import com.mechanical.FirePit;
import com.mechanical.Mechanical;
import com.mechanical.WorldAge;
import com.mechanical.clan.Clans;
import com.mechanical.clan.Territory;
import com.mechanical.people.AgeRebuild;
import com.mechanical.people.Neighbours;
import com.mechanical.people.Settlements;
import com.mechanical.people.Tribe;
import com.mechanical.worldgen.TribeVillageStructure;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.fml.ModList;

/** The dashboard's JSON, built on the server thread (Dashboard rebuilds it every second) so the HTTP threads only hand out finished strings. */
public final class Snapshot {
    /** One rebuild: the strings /api/status|players|tribes|map serve. */
    public record Json(String status, String players, String tribes, String map) {}

    private static volatile long start = System.currentTimeMillis();

    private Snapshot() {}

    /** Uptime counts from here (ServerStartedEvent), not from class load. */
    static void started() {
        start = System.currentTimeMillis();
    }

    public static Json build(MinecraftServer server) {
        return new Json(status(server), players(server), tribes(server), map(server));
    }

    public static String status(MinecraftServer server) {
        JsonObject o = new JsonObject();
        var info = ModList.get().getModContainerById(Mechanical.MODID).orElseThrow().getModInfo();
        o.addProperty("name", info.getDisplayName());
        o.addProperty("version", info.getVersion().toString());
        o.addProperty("uptimeSec", (System.currentTimeMillis() - start) / 1000);
        float mspt = server.getAverageTickTimeNanos() / 1_000_000f;
        o.addProperty("tps", Math.round(Math.min(server.tickRateManager().tickrate(), 1000f / Math.max(mspt, 0.001f)) * 10) / 10f);
        o.addProperty("mspt", Math.round(mspt * 10) / 10f);
        o.addProperty("online", server.getPlayerList().getPlayerCount());
        o.addProperty("max", server.getMaxPlayers());
        Runtime rt = Runtime.getRuntime();
        o.addProperty("memUsedMb", (rt.totalMemory() - rt.freeMemory()) >> 20);
        o.addProperty("memMaxMb", rt.maxMemory() >> 20);
        int chunks = 0, entities = 0;
        for (ServerLevel level : server.getAllLevels()) {
            chunks += level.getChunkSource().getLoadedChunksCount();
            entities += (int) java.util.stream.StreamSupport.stream(level.getAllEntities().spliterator(), false).count();
        }
        o.addProperty("loadedChunks", chunks);
        o.addProperty("entities", entities);
        long clock = Calendar.overworldClock(server).map(c -> server.clockManager().getInstance(c).totalTicks()).orElse(0L);
        o.addProperty("day", Calendar.day(clock));
        o.addProperty("time", String.format("%02d:%02d", Calendar.hour(clock), Calendar.minute(clock)));
        o.addProperty("season", Calendar.SEASONS[Calendar.season(clock)]);
        o.addProperty("month", Calendar.MONTHS[Calendar.month(clock)]);
        o.addProperty("dayOfMonth", Calendar.dayOfMonth(clock));
        o.addProperty("year", Calendar.year(clock));
        o.addProperty("worldAge", WorldAge.AGES[WorldAge.of(server)]);
        return o.toString();
    }

    public static String players(MinecraftServer server) {
        Clans clans = Clans.get(server);
        JsonArray out = new JsonArray();
        Set<UUID> online = new HashSet<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            online.add(p.getUUID());
            JsonObject o = new JsonObject();
            o.addProperty("uuid", p.getUUID().toString());
            o.addProperty("name", p.getGameProfile().name());
            o.addProperty("online", true);
            o.addProperty("x", Math.round(p.getX() * 10) / 10.0);
            o.addProperty("y", Math.round(p.getY() * 10) / 10.0);
            o.addProperty("z", Math.round(p.getZ() * 10) / 10.0);
            o.addProperty("yaw", Math.round(p.getYRot()));
            o.addProperty("dim", p.level().dimension().identifier().toString());
            o.addProperty("health", p.getHealth());
            o.addProperty("food", p.getFoodData().getFoodLevel());
            o.addProperty("ping", p.connection.latency());
            addTribe(o, clans.clanOf(p.getUUID()));
            out.add(o);
        }
        for (Clans.Clan c : clans.all())
            for (Clans.Member m : c.members()) {
                if (online.contains(m.id())) continue;
                JsonObject o = new JsonObject();
                o.addProperty("uuid", m.id().toString());
                o.addProperty("name", m.name());
                o.addProperty("online", false);
                o.addProperty("lastSeenDay", m.lastSeenDay());
                addTribe(o, c);
                out.add(o);
            }
        return out.toString();
    }

    private static void addTribe(JsonObject o, Clans.Clan c) {
        if (c == null) o.add("tribe", JsonNull.INSTANCE);
        else o.addProperty("tribe", c.name());
    }

    /** Readable tribe name without lang files (a dedicated server has none of the mod's): the totem animal, as the lang line "%s Tribe" makes it. */
    private static String tribeName(Tribe t) {
        return t.oak() ? "Children of the Oak" : Character.toUpperCase(t.totem.charAt(0)) + t.totem.substring(1) + " Tribe";
    }

    public static String tribes(MinecraftServer server) {
        Clans clans = Clans.get(server);
        JsonArray out = new JsonArray();
        for (Clans.Clan c : clans.all()) {
            JsonObject o = new JsonObject();
            o.addProperty("id", c.id().toString());
            o.addProperty("name", c.name());
            o.addProperty("colour", String.format("#%06x", DyeColor.byId(c.colour()).getTextureDiffuseColor() & 0xFFFFFF));
            o.addProperty("age", WorldAge.AGES[c.age()]);
            if (c.pit().isEmpty()) o.add("pit", JsonNull.INSTANCE);
            else {
                var pit = c.pit().get();
                JsonObject p = new JsonObject();
                p.addProperty("x", pit.pos().getX());
                p.addProperty("y", pit.pos().getY());
                p.addProperty("z", pit.pos().getZ());
                p.addProperty("dim", pit.dimension().identifier().toString());
                o.add("pit", p);
            }
            o.addProperty("big", c.isBigPit(server));
            o.addProperty("chunks", c.territory().size());
            JsonArray members = new JsonArray();
            for (Clans.Member m : c.members()) {
                JsonObject mo = new JsonObject();
                mo.addProperty("name", m.name());
                mo.addProperty("rank", Clans.RANKS[m.rank()]);
                mo.addProperty("online", server.getPlayerList().getPlayer(m.id()) != null);
                members.add(mo);
            }
            o.add("members", members);
            JsonObject rep = new JsonObject();
            c.reputation().forEach((k, v) -> {
                try {
                    rep.addProperty(tribeName(Tribe.of(UUID.fromString(k))), v);
                } catch (IllegalArgumentException e) {
                    // not a tribe id
                }
            });
            o.add("reputation", rep);
            out.add(o);
        }
        return out.toString();
    }

    /** A tribe's rings with the territory they were made from (size + hash), so they are only rebuilt when the land changes. */
    private record Cached(int size, int hash, List<int[]> rings) {}

    private static final Map<UUID, Cached> OUTLINES = new HashMap<>();

    private static List<int[]> rings(Clans.Clan c) {
        Set<Long> t = c.territory();
        Cached k = OUTLINES.get(c.id());
        if (k == null || k.size != t.size() || k.hash != t.hashCode()) OUTLINES.put(c.id(), k = new Cached(t.size(), t.hashCode(), Outline.of(t)));
        return k.rings;
    }

    /** Village rings by the village record: a village never changes once recorded, so each is outlined once. */
    private static final Map<Villages.Village, List<int[]>> VILLAGE_OUTLINES = new HashMap<>();

    private static final Map<BlockPos, List<int[]>> NPC_OUTLINES = new HashMap<>(), CAMP_OUTLINES = new HashMap<>();

    /** The chunks a circle of {@code r} blocks round {@code c} reaches into. */
    private static Set<Long> circle(BlockPos c, int r) {
        Set<Long> out = new HashSet<>();
        for (int cx = c.getX() - r >> 4; cx <= c.getX() + r >> 4; cx++)
            for (int cz = c.getZ() - r >> 4; cz <= c.getZ() + r >> 4; cz++) {
                int dx = c.getX() - Math.clamp(c.getX(), cx * 16, cx * 16 + 15), dz = c.getZ() - Math.clamp(c.getZ(), cz * 16, cz * 16 + 15);
                if (dx * dx + dz * dz <= r * r) out.add(ChunkPos.pack(cx, cz));
            }
        return out;
    }

    private static JsonArray json(List<int[]> rings) {
        JsonArray out = new JsonArray();
        for (int[] r : rings) {
            JsonArray a = new JsonArray();
            for (int v : r) a.add(v);
            out.add(a);
        }
        return out;
    }

    private static JsonObject point(int[] xz) {
        JsonObject p = new JsonObject();
        p.addProperty("x", xz[0]);
        p.addProperty("z", xz[1]);
        return p;
    }

    /** Centre (block coordinates) of the territory chunk nearest the territory's centroid, so the label always sits on the tribe's land. */
    static int[] label(Collection<Long> chunks) {
        double cx = 0, cz = 0;
        for (long c : chunks) {
            cx += ChunkPos.getX(c);
            cz += ChunkPos.getZ(c);
        }
        cx /= Math.max(chunks.size(), 1);
        cz /= Math.max(chunks.size(), 1);
        long best = 0;
        double bd = Double.MAX_VALUE;
        for (long c : chunks) {
            double d = Math.pow(ChunkPos.getX(c) - cx, 2) + Math.pow(ChunkPos.getZ(c) - cz, 2);
            if (d < bd) {
                bd = d;
                best = c;
            }
        }
        return new int[] {ChunkPos.getX(best) * 16 + 8, ChunkPos.getZ(best) * 16 + 8};
    }

    private static JsonObject fire(int x, int z, String kind, UUID tribe) {
        JsonObject f = new JsonObject();
        f.addProperty("x", x);
        f.addProperty("z", z);
        f.addProperty("kind", kind);
        if (tribe == null) f.add("tribe", JsonNull.INSTANCE);
        else f.addProperty("tribe", tribe.toString());
        return f;
    }

    /** /api/map: overworld players, fires (tribe pits and the mod's tracked camps, no world scan), tribe outlines with the chunks their pit keeps loaded, and vanilla villages. */
    public static String map(MinecraftServer server) {
        Clans clans = Clans.get(server);
        ServerLevel overworld = server.overworld();
        JsonObject o = new JsonObject();
        JsonArray players = new JsonArray();
        for (ServerPlayer p : overworld.players()) {
            JsonObject j = new JsonObject();
            j.addProperty("name", p.getGameProfile().name());
            j.addProperty("x", Math.round(p.getX() * 10) / 10.0);
            j.addProperty("z", Math.round(p.getZ() * 10) / 10.0);
            j.addProperty("yaw", Math.round(p.getYRot()));
            Clans.Clan c = clans.clanOf(p.getUUID());
            if (c == null) j.add("tribe", JsonNull.INSTANCE);
            else j.addProperty("tribe", c.id().toString());
            players.add(j);
        }
        o.add("players", players);
        JsonArray fires = new JsonArray(), tribes = new JsonArray();
        List<BlockPos> pits = new java.util.ArrayList<>();
        OUTLINES.keySet().removeIf(id -> clans.all().stream().noneMatch(c -> c.id().equals(id)));
        for (Clans.Clan c : clans.all()) {
            c.pit().filter(g -> g.dimension().equals(overworld.dimension())).ifPresent(g -> {
                pits.add(g.pos());
                fires.add(fire(g.pos().getX(), g.pos().getZ(), c.isBigPit(server) ? "big_fire_pit" : "fire_pit", c.id()));
            });
            if (c.territory().isEmpty() || c.pit().filter(g -> !g.dimension().equals(overworld.dimension())).isPresent()) continue;
            JsonObject t = new JsonObject();
            t.addProperty("id", c.id().toString());
            t.addProperty("name", c.name());
            t.addProperty("colour", String.format("#%06x", DyeColor.byId(c.colour()).getTextureDiffuseColor() & 0xFFFFFF));
            t.add("rings", json(rings(c)));
            // the pit is in the overworld here (other dimensions were skipped); its force-loaded square is a fixed 5x5, cheap to outline each time
            t.add("loaded", json(c.pit().map(g -> Outline.of(Territory.square(ChunkPos.containing(g.pos()), Clans.LOAD_RADIUS))).orElse(List.of())));
            t.add("label", point(label(c.territory())));
            tribes.add(t);
        }
        for (CampAge.Camp camp : CampAge.get(overworld).camps()) {
            BlockPos at = camp.centre;
            if (pits.stream().anyMatch(p -> Math.abs(p.getX() - at.getX()) <= 2 && Math.abs(p.getY() - at.getY()) <= 2 && Math.abs(p.getZ() - at.getZ()) <= 2)) continue;
            boolean pit = overworld.isLoaded(at) && FirePit.isPit(overworld.getBlockState(at));
            fires.add(fire(at.getX(), at.getZ(), pit ? "fire_pit" : "campfire", null));
        }
        o.add("fires", fires);
        o.add("tribes", tribes);
        JsonArray villages = new JsonArray();
        for (Villages.Village v : Villages.get(overworld).all()) {
            if (v.chunks().isEmpty()) continue;
            JsonObject j = new JsonObject();
            j.addProperty("name", v.name());
            j.addProperty("kind", v.kind());
            j.add("rings", json(VILLAGE_OUTLINES.computeIfAbsent(v, k -> Outline.of(k.chunks()))));
            j.add("label", point(label(v.chunks())));
            villages.add(j);
        }
        o.add("villages", villages);
        // NPC tribe villages, from the record AgeRebuild keeps of them: their land is what clan.Lands announces, in whole chunks
        JsonArray npc = new JsonArray();
        for (AgeRebuild.Village v : AgeRebuild.Villages.get(overworld).villages) {
            Tribe t = Tribe.of(v.tribe);
            JsonObject j = new JsonObject();
            j.addProperty("id", "npc:" + v.centre.asLong());
            j.addProperty("name", tribeName(t));
            j.addProperty("colour", String.format("#%06x", t.baseColour().getTextureDiffuseColor() & 0xFFFFFF));
            j.add("rings", json(NPC_OUTLINES.computeIfAbsent(v.centre, c -> Outline.of(circle(c, TribeVillageStructure.WALL_R + 2)))));
            j.add("label", point(new int[] {v.centre.getX(), v.centre.getZ()}));
            npc.add(j);
        }
        // a tribe's camp built by a player camp (Neighbours.Settlement): its palisade ring, in its tribe's colour
        for (Neighbours.Settlement s : Neighbours.get(overworld).settlements) {
            Tribe t = Tribe.of(s.tribe);
            JsonObject j = new JsonObject();
            j.addProperty("id", "camp:" + s.centre.asLong());
            j.addProperty("name", tribeName(t));
            j.addProperty("colour", String.format("#%06x", t.baseColour().getTextureDiffuseColor() & 0xFFFFFF));
            j.add("rings", json(CAMP_OUTLINES.computeIfAbsent(s.centre, c -> Outline.of(circle(c, Settlements.PALISADE_R + 2)))));
            j.add("label", point(new int[] {s.centre.getX(), s.centre.getZ()}));
            npc.add(j);
        }
        o.add("tribeVillages", npc);
        JsonArray landmarks = new JsonArray();
        for (Landmarks.Mark m : Landmarks.get(overworld).all()) {
            JsonObject j = new JsonObject();
            j.addProperty("kind", m.kind());
            j.addProperty("x", m.x());
            j.addProperty("z", m.z());
            landmarks.add(j);
        }
        o.add("landmarks", landmarks);
        return o.toString();
    }
}
