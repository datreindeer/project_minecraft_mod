package com.mechanical.dashboard;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.mechanical.Calendar;
import com.mechanical.Mechanical;
import com.mechanical.WorldAge;
import com.mechanical.clan.Clans;
import com.mechanical.people.Tribe;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
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
        return new Json(status(server), players(server), tribes(server), "{}");
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
}
