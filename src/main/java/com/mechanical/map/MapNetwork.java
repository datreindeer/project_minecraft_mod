package com.mechanical.map;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import com.mechanical.Mechanical;
import com.mechanical.clan.Clans;
import com.mechanical.dashboard.MapTiles;
import com.mechanical.dashboard.Snapshot;
import com.mojang.logging.LogUtils;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * The map screen's traffic (client: client.map.MapScreen). While open, the client asks ({@link Request}) for the map
 * regions in view with the stamp of what it already has. The server is the trust boundary: it answers with the positions
 * of the viewer's online tribe mates only ({@link People}) and, for each region that changed, its picture with every
 * chunk the viewer's group hasn't explored made transparent ({@link Tile}). One request per player at a time, at most
 * {@link #MAX_WANTS} regions every {@link #MIN_TICKS} ticks; regions nobody in the group explored cost no disk read,
 * and all PNG work happens on {@link MapTiles}' worker thread.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class MapNetwork {
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();
    public static final int MAX_WANTS = 48, MIN_TICKS = 10;
    /** The world border (30 million blocks) in 512-block regions. */
    static final int MAX_REGION = 58_594;
    /** Under the 1 MiB custom payload limit; a region of map colours is far smaller. */
    static final int MAX_PNG = 1_000_000;
    /** Characters; at up to 3 bytes each still under the payload limit (it is all but ASCII). */
    static final int MAX_FEATURES = 340_000;
    static final Identifier ADVANCEMENT = Identifier.fromNamespaceAndPath(Mechanical.MODID, "stone_age/world_map");

    /** A region in view; {@code stamp} is the {@link Tile#stamp} the client holds for it, 0 for none. */
    public record Want(int x, int z, int stamp) {
        static final StreamCodec<RegistryFriendlyByteBuf, Want> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Want::x, ByteBufCodecs.VAR_INT, Want::z, ByteBufCodecs.INT, Want::stamp, Want::new);
    }

    public record Request(List<Want> wants) implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "map_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC =
                Want.CODEC.apply(ByteBufCodecs.list(MAX_WANTS)).map(Request::new, Request::wants);

        @Override
        public Type<Request> type() {
            return TYPE;
        }
    }

    /** One region. Stamp 0: forget it (no longer yours to see). An empty png: nothing to show there yet. */
    public record Tile(int x, int z, int stamp, byte[] png) implements CustomPacketPayload {
        public static final Type<Tile> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "map_tile"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Tile> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Tile::x, ByteBufCodecs.VAR_INT, Tile::z, ByteBufCodecs.INT, Tile::stamp,
                ByteBufCodecs.byteArray(MAX_PNG), Tile::png, Tile::new);

        @Override
        public Type<Tile> type() {
            return TYPE;
        }
    }

    /** Lands, villages, fires and places the viewer may know of: the dashboard's /api/map JSON, filtered. */
    public record Features(String json) implements CustomPacketPayload {
        public static final Type<Features> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "map_features"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Features> CODEC =
                ByteBufCodecs.stringUtf8(MAX_FEATURES).<RegistryFriendlyByteBuf>cast().map(Features::new, Features::json);

        @Override
        public Type<Features> type() {
            return TYPE;
        }
    }

    public record Person(String name, int x, int z, int yaw) {
        static final StreamCodec<RegistryFriendlyByteBuf, Person> CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(64), Person::name, ByteBufCodecs.VAR_INT, Person::x, ByteBufCodecs.VAR_INT, Person::z, ByteBufCodecs.VAR_INT, Person::yaw, Person::new);
    }

    /** The viewer's tribe mates in the overworld; {@code colour} the tribe's DyeColor id, -1 without a tribe. */
    public record People(int colour, List<Person> mates) implements CustomPacketPayload {
        public static final Type<People> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "map_people"));
        public static final StreamCodec<RegistryFriendlyByteBuf, People> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, People::colour, Person.CODEC.apply(ByteBufCodecs.list()), People::mates, People::new);

        @Override
        public Type<People> type() {
            return TYPE;
        }
    }

    /** Server thread only: one request at a time per player, {@link #MIN_TICKS} apart. */
    static final class Limiter {
        private final Map<UUID, Integer> last = new HashMap<>();
        private final Set<UUID> busy = new HashSet<>();

        boolean accept(UUID player, int tick) {
            Integer before = last.get(player);
            if (busy.contains(player) || before != null && tick - before < MIN_TICKS) return false;
            last.put(player, tick);
            busy.add(player);
            return true;
        }

        void done(UUID player) {
            busy.remove(player);
        }

        void clear() {
            last.clear();
            busy.clear();
        }
    }

    private static final Limiter LIMITER = new Limiter();

    private MapNetwork() {}

    @SubscribeEvent
    static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional()
                .playToServer(Request.TYPE, Request.CODEC, (request, ctx) -> {
                    if (ctx.player() instanceof ServerPlayer player) onRequest(player, request);
                })
                .playToClient(People.TYPE, People.CODEC)
                .playToClient(Tile.TYPE, Tile.CODEC)
                .playToClient(Features.TYPE, Features.CODEC);
    }

    /** Tick counts start over with the next world, and a stopping worker may never release its players. */
    @SubscribeEvent
    static void onStopping(ServerStoppingEvent event) {
        LIMITER.clear();
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LIMITER.done(event.getEntity().getUUID());
    }

    /** At most {@link #MAX_WANTS} regions inside the world, each once. */
    static List<Want> valid(List<Want> wants) {
        Set<Long> seen = new HashSet<>();
        List<Want> out = new ArrayList<>();
        for (Want w : wants) {
            if (out.size() >= MAX_WANTS) break;
            if (w.x < -MAX_REGION || w.x > MAX_REGION || w.z < -MAX_REGION || w.z > MAX_REGION) continue;
            if (seen.add(ChunkPos.pack(w.x, w.z))) out.add(w);
        }
        return out;
    }

    /** The tribe mates among {@code online} the viewer may see: same tribe, in the overworld, not the viewer. */
    static People people(ServerPlayer viewer, Collection<ServerPlayer> online) {
        MinecraftServer server = viewer.level().getServer();
        Clans.Clan clan = Clans.get(server).clanOf(viewer.getUUID());
        if (clan == null) return new People(-1, List.of());
        Set<UUID> group = ExploredMap.group(server, viewer.getUUID());
        List<Person> mates = new ArrayList<>();
        for (ServerPlayer p : online) {
            if (p.getUUID().equals(viewer.getUUID()) || !group.contains(p.getUUID()) || p.level().dimension() != Level.OVERWORLD) continue;
            mates.add(new Person(p.getGameProfile().name(), p.getBlockX(), p.getBlockZ(), Math.round(p.getYRot())));
        }
        return new People(clan.colour(), mates);
    }

    static String features(MinecraftServer server, UUID viewer) {
        Clans.Clan clan = Clans.get(server).clanOf(viewer);
        return Snapshot.map(server, new Snapshot.Seen(ExploredMap.get(server).seen(ExploredMap.group(server, viewer)), clan == null ? null : clan.id()));
    }

    /** A wanted region the viewer has explored part of, with what they may see of it. */
    record Job(Want want, long[] mask) {}

    /** Server thread: what to draw for these wants; anything not explored but still held by the client goes to {@code forget}. */
    static List<Job> jobs(MinecraftServer server, UUID viewer, List<Want> wants, List<Tile> forget) {
        ExploredMap explored = ExploredMap.get(server);
        Set<UUID> group = ExploredMap.group(server, viewer);
        List<Job> out = new ArrayList<>();
        for (Want w : valid(wants)) {
            long[] mask = explored.mask(group, w.x, w.z);
            if (mask != null) out.add(new Job(w, mask));
            else if (w.stamp != 0) forget.add(new Tile(w.x, w.z, 0, new byte[0]));
        }
        return out;
    }

    private static void onRequest(ServerPlayer player, Request request) {
        MinecraftServer server = player.level().getServer();
        UUID id = player.getUUID();
        if (player.level().dimension() != Level.OVERWORLD || !LIMITER.accept(id, server.getTickCount())) return;
        var advancement = server.getAdvancements().get(ADVANCEMENT);
        if (advancement != null) player.getAdvancements().award(advancement, "done");
        PacketDistributor.sendToPlayer(player, people(player, server.getPlayerList().getPlayers()));
        // shortcut: sent whole with every request (about once a second); give it a stamp like the tiles if it grows heavy
        String features = features(server, id);
        if (features.length() <= MAX_FEATURES) PacketDistributor.sendToPlayer(player, new Features(features));
        else LOGGER.warn("Mechanical map: too much to mark for {} ({} characters)", player.getGameProfile().name(), features.length());
        List<Tile> forget = new ArrayList<>();
        List<Job> jobs = jobs(server, id, request.wants(), forget);
        forget.forEach(t -> PacketDistributor.sendToPlayer(player, t));
        boolean queued = !jobs.isEmpty() && MapTiles.onWorker(() -> {
            List<Tile> tiles = new ArrayList<>();
            for (Job job : jobs) {
                try {
                    Tile tile = draw(job);
                    if (tile != null) tiles.add(tile);
                } catch (IOException | RuntimeException e) {
                    LOGGER.warn("Mechanical map: could not send region {} {}", job.want.x, job.want.z, e);
                }
            }
            server.execute(() -> {
                LIMITER.done(id);
                ServerPlayer now = server.getPlayerList().getPlayer(id);
                if (now != null) tiles.forEach(t -> PacketDistributor.sendToPlayer(now, t));
            });
        });
        if (!queued) LIMITER.done(id);
    }

    /** Worker thread: the region as the viewer may see it, or null when the client's copy is up to date. */
    private static Tile draw(Job job) throws IOException {
        Want w = job.want;
        int stamp = 31 * Arrays.hashCode(job.mask) + MapTiles.version(w.x, w.z);
        if (stamp == 0) stamp = 1;
        if (stamp == w.stamp) return null;
        BufferedImage region = MapTiles.region(w.x, w.z);
        BufferedImage seen = region == null ? null : masked(region, job.mask);
        if (seen == null) return new Tile(w.x, w.z, stamp, new byte[0]);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (var out = new MemoryCacheImageOutputStream(bytes)) {
            ImageIO.write(seen, "png", out);
        }
        if (bytes.size() > MAX_PNG) throw new IOException("region picture too big to send: " + bytes.size() + " bytes");
        return new Tile(w.x, w.z, stamp, bytes.toByteArray());
    }

    /** A copy of a 512x512 region with every chunk outside the mask fully transparent (no colour left), or null when nothing remains. */
    static BufferedImage masked(BufferedImage region, long[] mask) {
        int[] pixels = new int[512 * 512];
        boolean any = false;
        for (int cz = 0; cz < 32; cz++)
            for (int cx = 0; cx < 32; cx++) {
                if (!ExploredMap.has(mask, cx, cz)) continue;
                region.getRGB(cx * 16, cz * 16, 16, 16, pixels, cz * 16 * 512 + cx * 16, 512);
                for (int row = 0; !any && row < 16; row++)
                    for (int x = 0; x < 16; x++) any |= pixels[(cz * 16 + row) * 512 + cx * 16 + x] >>> 24 != 0;
            }
        if (!any) return null;
        BufferedImage out = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
        out.setRGB(0, 0, 512, 512, pixels, 0, 512);
        return out;
    }
}
