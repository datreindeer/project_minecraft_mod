package com.mechanical.dashboard;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.mechanical.Mechanical;
import com.mechanical.worldgen.AncientOakStructure;
import com.mechanical.worldgen.TribeVillageStructure;
import com.mechanical.worldgen.Waystones;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * The Ancient Oaks, waystones and tribe villages the dashboard map marks. Like {@link Villages} they are found as chunks load
 * (so only explored land shows), and recorded whether or not the dashboard runs. Whatever builds one in a chunk that is
 * loaded already (/structure, /regen, travel to an old chunk) calls {@link #add} itself.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class Landmarks extends SavedData {
    /** One place: kind (tribe_village, ancient_oak, waystone) and block x, z. */
    public record Mark(String kind, int x, int z) {
        static final Codec<Mark> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("kind").forGetter(Mark::kind),
                Codec.INT.fieldOf("x").forGetter(Mark::x),
                Codec.INT.fieldOf("z").forGetter(Mark::z)).apply(i, Mark::new));
    }

    private static final Codec<Landmarks> CODEC = Mark.CODEC.listOf().xmap(Landmarks::new, d -> List.copyOf(d.marks));
    private static final SavedDataType<Landmarks> TYPE = new SavedDataType<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "dashboard_landmarks"),
            () -> new Landmarks(List.of()), CODEC);
    /** Chunk loads may come off the server thread: what they find waits here for the next overworld tick. */
    private static final ConcurrentLinkedQueue<Mark> FOUND = new ConcurrentLinkedQueue<>();
    /** Loaded chunks to check for a waystone end, on the tick (the stone has to be looked for in the world). */
    private static final ConcurrentLinkedQueue<ChunkPos> CHECK = new ConcurrentLinkedQueue<>();

    private final Set<Mark> marks;

    private Landmarks(List<Mark> list) {
        marks = new LinkedHashSet<>(list);
    }

    public static Landmarks get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    Collection<Mark> all() {
        return marks;
    }

    /** Records a place once. */
    public static void add(ServerLevel level, String kind, int x, int z) {
        Landmarks data = get(level);
        if (data.marks.add(new Mark(kind, x, z))) data.setDirty();
    }

    @SubscribeEvent
    static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        Dashboard.guard("landmarks", () -> {
            event.getChunk().getAllStarts().forEach((structure, start) -> {
                if (!start.isValid()) return;
                StructurePiece piece = start.getPieces().getFirst();
                if (piece instanceof TribeVillageStructure.Piece v) FOUND.add(new Mark("tribe_village", v.centre().getX(), v.centre().getZ()));
                if (piece instanceof AncientOakStructure.Piece oak) FOUND.add(new Mark("ancient_oak", oak.foot().getX(), oak.foot().getZ()));
            });
            CHECK.add(event.getChunk().getPos());
        });
    }

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        Dashboard.guard("landmarks", () -> {
            for (Mark m; (m = FOUND.poll()) != null;) add(level, m.kind, m.x, m.z);
            // shortcut: 64 chunks a tick; partner() is cached lookups, the column scan runs only in a pair's own chunks
            ChunkPos p;
            for (int i = 0; i < 64 && (p = CHECK.poll()) != null; i++) {
                if (!level.hasChunk(p.x(), p.z()) || Waystones.partner(level, p.x(), p.z()) == null) continue;
                BlockPos stone = Waystones.findStone(level, p.getMiddleBlockX(), p.getMiddleBlockZ());
                if (stone != null) add(level, "waystone", stone.getX(), stone.getZ());
            }
        });
    }
}
