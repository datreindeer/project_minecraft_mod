package com.mechanical.map;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.LongPredicate;

import com.mechanical.Mechanical;
import com.mechanical.clan.Clans;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The overworld chunks each player has explored (within {@link #RADIUS} chunks of them), saved in the world by player id,
 * so it survives death and restarts and an offline tribe mate's knowledge still counts. A player sees the union over
 * their {@link #group}, worked out when asked: knowledge follows the player into and out of tribes. One bit per chunk,
 * 16 longs per 32x32 chunk map region. Starts empty: nothing explored before this existed is filled in.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class ExploredMap extends SavedData {
    public static final int RADIUS = 6;
    private static final int WORDS = 16, EVERY_TICKS = 10;

    private record Region(long pos, long[] bits) {
        static final Codec<Region> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.fieldOf("pos").forGetter(Region::pos),
                Codec.LONG_STREAM.xmap(s -> Arrays.copyOf(s.toArray(), WORDS), a -> Arrays.stream(a.clone())).fieldOf("bits").forGetter(Region::bits))
                .apply(i, Region::new));
    }

    static final Codec<ExploredMap> CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, Region.CODEC.listOf()).xmap(ExploredMap::load, ExploredMap::save);
    private static final SavedDataType<ExploredMap> TYPE = new SavedDataType<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "explored_map"),
            ExploredMap::new, CODEC);

    /** Player -> packed region position -> bits. */
    private final Map<UUID, Map<Long, long[]>> players = new HashMap<>();

    ExploredMap() {}

    private static ExploredMap load(Map<UUID, List<Region>> saved) {
        ExploredMap out = new ExploredMap();
        saved.forEach((id, regions) -> {
            Map<Long, long[]> map = out.players.computeIfAbsent(id, k -> new HashMap<>());
            for (Region r : regions) map.put(r.pos, r.bits);
        });
        return out;
    }

    private Map<UUID, List<Region>> save() {
        Map<UUID, List<Region>> out = new HashMap<>();
        players.forEach((id, regions) -> {
            List<Region> list = new ArrayList<>(regions.size());
            regions.forEach((pos, bits) -> list.add(new Region(pos, bits)));
            out.put(id, list);
        });
        return out;
    }

    public static ExploredMap get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** Marks every chunk within {@link #RADIUS} of this one as explored by the player. */
    public void explore(UUID player, int cx, int cz) {
        Map<Long, long[]> regions = players.computeIfAbsent(player, k -> new HashMap<>());
        for (int dx = -RADIUS; dx <= RADIUS; dx++)
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                if (dx * dx + dz * dz > RADIUS * RADIUS) continue;
                int x = cx + dx, z = cz + dz;
                long[] bits = regions.computeIfAbsent(ChunkPos.pack(x >> 5, z >> 5), k -> new long[WORDS]);
                int i = (z & 31) * 32 + (x & 31);
                if ((bits[i >> 6] & 1L << i) == 0) {
                    bits[i >> 6] |= 1L << i;
                    setDirty();
                }
            }
    }

    /** A fresh mask of the region's chunks any of {@code who} has explored, or null when none of it is. */
    long[] mask(Collection<UUID> who, int rx, int rz) {
        long[] out = null;
        long key = ChunkPos.pack(rx, rz);
        for (UUID id : who) {
            long[] bits = players.getOrDefault(id, Map.of()).get(key);
            if (bits == null) continue;
            if (out == null) out = new long[WORDS];
            for (int i = 0; i < WORDS; i++) out[i] |= bits[i];
        }
        return out;
    }

    /** Has any of {@code who} explored this packed chunk? Region masks are worked out once each. */
    LongPredicate seen(Collection<UUID> who) {
        Map<Long, long[]> masks = new HashMap<>();
        return chunk -> {
            int x = ChunkPos.getX(chunk), z = ChunkPos.getZ(chunk);
            return has(masks.computeIfAbsent(ChunkPos.pack(x >> 5, z >> 5), k -> mask(who, x >> 5, z >> 5)), x, z);
        };
    }

    /** Is the chunk (any coordinates, taken within its region) set in this mask? */
    static boolean has(long[] mask, int cx, int cz) {
        int i = (cz & 31) * 32 + (cx & 31);
        return mask != null && (mask[i >> 6] & 1L << i) != 0;
    }

    /** Whose exploring the viewer sees: their own and that of everyone now in their tribe, online or not. */
    static Set<UUID> group(MinecraftServer server, UUID viewer) {
        Set<UUID> out = new HashSet<>();
        out.add(viewer);
        Clans.Clan clan = Clans.get(server).clanOf(viewer);
        if (clan != null) for (Clans.Member m : clan.members()) out.add(m.id());
        return out;
    }

    @SubscribeEvent
    static void onTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % EVERY_TICKS != 0) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.level().dimension() != Level.OVERWORLD || p.isSpectator()) continue;
            get(server).explore(p.getUUID(), p.chunkPosition().x(), p.chunkPosition().z());
        }
    }
}
