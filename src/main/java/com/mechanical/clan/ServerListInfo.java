package com.mechanical.clan;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.MapMaker;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mechanical.Mechanical;
import com.mechanical.WorldAge;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.Settings;
import net.minecraft.server.level.ServerPlayer;

/**
 * The Multiplayer screen's detail for servers running the mod (docs/seasons-and-tribes.md, the server list): the date,
 * the world's age, the tribes with their players online and the server's own name (server.properties {@code server-name}). The server adds it as a "mechanical" field to its status JSON
 * (mixin.MinecraftServerStatusMixin); vanilla clients skip unknown fields, so they and the MOTD are untouched. Our client
 * reads it while decoding the status (mixin.ClientboundStatusResponsePacketMixin) and keys it by the status's player count
 * object, which ServerData keeps as {@code players} (client.clan.ServerListDetails draws it).
 */
public record ServerListInfo(long clock, int age, int tribes, int wanderers, List<Tribe> shown, String name) {
    public static final String KEY = Mechanical.MODID;
    /** At most this many tribes are sent, most players online first; {@code tribes} counts them all. */
    public static final int MAX_SHOWN = 6;
    /** The server's own name for the list (a line in server.properties, not a vanilla key), shown instead of the typed-in one. */
    public static final String NAME_KEY = "server-name";
    private static final int MAX_NAME = 64;
    private static String serverName;

    /** {@code colour} a DyeColor id, {@code age} an index into Clans.AGES. */
    public record Tribe(String name, int colour, int age, int online, int members) {
        static final Codec<Tribe> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("n").forGetter(Tribe::name), Codec.INT.fieldOf("c").forGetter(Tribe::colour),
                Codec.INT.fieldOf("a").forGetter(Tribe::age), Codec.INT.fieldOf("o").forGetter(Tribe::online),
                Codec.INT.fieldOf("m").forGetter(Tribe::members)).apply(i, Tribe::new));
    }

    public static final Codec<ServerListInfo> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.fieldOf("d").forGetter(ServerListInfo::clock), Codec.INT.fieldOf("a").forGetter(ServerListInfo::age),
            Codec.INT.fieldOf("t").forGetter(ServerListInfo::tribes), Codec.INT.fieldOf("w").forGetter(ServerListInfo::wanderers),
            Tribe.CODEC.listOf(0, MAX_SHOWN).fieldOf("l").forGetter(ServerListInfo::shown),
            Codec.STRING.optionalFieldOf("s", "").forGetter(ServerListInfo::name)).apply(i, ServerListInfo::new));

    /** Received infos by the status's player count object (weak, identity keys: ServerData holds the object while it lists the server). */
    private static final Map<ServerStatus.Players, ServerListInfo> RECEIVED = new MapMaker().weakKeys().makeMap();

    /** The info that came with this player count, or null for a server without the mod. */
    public static ServerListInfo of(ServerStatus.Players players) {
        return players == null ? null : RECEIVED.get(players);
    }

    static ServerListInfo build(MinecraftServer server) {
        Clans data = Clans.get(server);
        List<Tribe> all = new ArrayList<>();
        for (Clans.Clan c : data.clans) {
            int online = 0;
            for (Clans.Member m : c.members) if (server.getPlayerList().getPlayer(m.id) != null) online++;
            all.add(new Tribe(c.name, c.colour, c.age(), online, c.members.size()));
        }
        all.sort(Comparator.comparingInt((Tribe t) -> -t.online).thenComparingInt(t -> -t.members).thenComparing(Tribe::name, String::compareToIgnoreCase));
        int wanderers = 0;
        for (ServerPlayer sp : server.getPlayerList().getPlayers()) if (data.clanOf(sp.getUUID()) == null) wanderers++;
        return new ServerListInfo(server.overworld().getOverworldClockTime(), WorldAge.of(server), all.size(), wanderers,
                List.copyOf(all.subList(0, Math.min(MAX_SHOWN, all.size()))), serverName(server));
    }

    /** {@link #NAME_KEY} from a dedicated server's server.properties, read once (a change needs a restart); "" if absent. */
    private static String serverName(MinecraftServer server) {
        if (serverName == null) {
            String name = server.isDedicatedServer()
                    ? Settings.loadFromFile(Path.of("server.properties")).getProperty(NAME_KEY, "").strip() : "";
            serverName = name.length() > MAX_NAME ? name.substring(0, MAX_NAME) : name;
        }
        return serverName;
    }

    /** Server: add the info to the status JSON about to be cached. Never lets a failure break the status. */
    public static void addTo(MinecraftServer server, JsonElement status) {
        if (!(status instanceof JsonObject json)) return;
        try {
            CODEC.encodeStart(JsonOps.INSTANCE, build(server)).result().ifPresent(info -> json.add(KEY, info));
        } catch (RuntimeException e) {
            Mechanical.LOGGER.debug("Server list info skipped", e);
        }
    }

    /** Client: the status codec, also picking up our field (if any) on the way. */
    public static Codec<ServerStatus> reading(Codec<ServerStatus> codec) {
        return new Codec<>() {
            @Override
            public <T> DataResult<Pair<ServerStatus, T>> decode(DynamicOps<T> ops, T input) {
                DataResult<Pair<ServerStatus, T>> out = codec.decode(ops, input);
                out.result().flatMap(p -> p.getFirst().players()).ifPresent(players -> ops.get(input, KEY)
                        .flatMap(t -> CODEC.parse(ops, t)).result().ifPresent(info -> RECEIVED.put(players, info)));
                return out;
            }

            @Override
            public <T> DataResult<T> encode(ServerStatus input, DynamicOps<T> ops, T prefix) {
                return codec.encode(input, ops, prefix);
            }
        };
    }
}
