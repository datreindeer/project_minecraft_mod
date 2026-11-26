package com.mechanical.dashboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.mechanical.Mechanical;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * The vanilla villages the dashboard map shows as named lands (clan.Lands announces them by the same {@link #name}). Found from the structure starts of chunks as they
 * load, so nothing is ever loaded or scanned for them; recorded whether or not the dashboard runs, so the map knows old villages too.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class Villages extends SavedData {
    static final String[] KINDS = {"plains", "desert", "savanna", "taiga", "snowy"};

    /** One village: its start chunk (the key), kind, name and the chunks its pieces cover (packed, x/z only). */
    public record Village(long start, String kind, String name, Set<Long> chunks) {
        static final Codec<Village> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.fieldOf("start").forGetter(Village::start),
                Codec.STRING.optionalFieldOf("kind", "plains").forGetter(Village::kind),
                Codec.STRING.optionalFieldOf("name", "Village").forGetter(Village::name),
                Codec.LONG.listOf().<Set<Long>>xmap(Set::copyOf, List::copyOf).optionalFieldOf("chunks", Set.of()).forGetter(Village::chunks)).apply(i, Village::new));
    }

    private static final Codec<Villages> CODEC = Village.CODEC.listOf().xmap(Villages::new, d -> List.copyOf(d.villages.values()));
    private static final SavedDataType<Villages> TYPE = new SavedDataType<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "dashboard_villages"),
            () -> new Villages(List.of()), CODEC);
    /** Villages whose start chunk loaded (chunk loads may come off the server thread); taken into the save on the next overworld tick. */
    private static final ConcurrentLinkedQueue<Village> FOUND = new ConcurrentLinkedQueue<>();

    private final Map<Long, Village> villages = new LinkedHashMap<>();

    private Villages(List<Village> list) {
        for (Village v : list) villages.put(v.start, v);
    }

    public static Villages get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public Collection<Village> all() {
        return villages.values();
    }

    /** Records a village once (by start chunk); false if it was known already. */
    boolean add(Village v) {
        if (villages.putIfAbsent(v.start, v) != null) return false;
        setDirty();
        return true;
    }

    void remove(long start) {
        if (villages.remove(start) != null) setDirty();
    }

    /** The chunks the boxes touch, x/z only. */
    static Set<Long> covered(List<BoundingBox> boxes) {
        Set<Long> out = new HashSet<>();
        for (BoundingBox b : boxes)
            for (int x = b.minX() >> 4; x <= b.maxX() >> 4; x++) for (int z = b.minZ() >> 4; z <= b.maxZ() >> 4; z++) out.add(ChunkPos.pack(x, z));
        return out;
    }

    static Village of(long seed, ChunkPos start, String kind, List<BoundingBox> boxes) {
        return new Village(start.pack(), kind, name(seed, start, kind), Set.copyOf(covered(boxes)));
    }

    /** The kind from the structure's id (village_desert, ...), plains for anything else (a datapack village). */
    static String kind(String path) {
        for (String k : KINDS) if (path.contains(k)) return k;
        return "plains";
    }

    private static final String[] FIRST = {"Mill", "Ash", "Oak", "Elm", "Thorn", "Hazel", "Bram", "Alder", "Holly", "Wil", "Hart", "Fox", "Wolf", "Barrow",
            "Kings", "Old", "Bright", "Black", "Red", "Stone"};
    private static final String[] LAST = {"brook", "ford", "ton", "by", "wick", "ham", "stead", "field", "dale", "holm", "worth", "ley", "thorpe", "bury",
            "mere", "cross", "hollow", "well"};
    private static final Map<String, String[]> KIND_FIRST = Map.of(
            "plains", new String[] {"Wheat", "Barley", "Meadow", "Hay", "Clover", "Green", "Bee"},
            "desert", new String[] {"Sand", "Dune", "Amber", "Sun", "Dust", "Ochre", "Palm"},
            "savanna", new String[] {"Acacia", "Lion", "Gold", "Dry", "Tall", "Ember", "Baobab"},
            "taiga", new String[] {"Pine", "Fir", "Spruce", "Moss", "Fern", "Raven", "Elk"},
            "snowy", new String[] {"Frost", "Rime", "Snow", "Winter", "Hail", "Ice", "White"});
    private static final Map<String, String[]> KIND_LAST = Map.of(
            "plains", new String[] {"field", "meadow", "acre", "ley", "ford"},
            "desert", new String[] {"well", "wash", "spring", "rock", "gate"},
            "savanna", new String[] {"veld", "rock", "spring", "hill", "wash"},
            "taiga", new String[] {"wood", "hurst", "shaw", "fell", "glen"},
            "snowy", new String[] {"fell", "holm", "mere", "drift", "peak"});

    /**
     * A village-like English name, the same every time for this world seed and start chunk (no lang file: a dedicated server has none of
     * the mod's). Two parts, leaning on the kind (Dunewell, Frostholm, Millbrook); a repeat now and then is fine.
     */
    static String name(long seed, ChunkPos start, String kind) {
        Random r = new Random(seed ^ start.pack() * 0x9E3779B97F4A7C15L);
        String[] kf = KIND_FIRST.getOrDefault(kind, FIRST), kl = KIND_LAST.getOrDefault(kind, LAST);
        String a = r.nextInt(3) < 2 ? kf[r.nextInt(kf.length)] : FIRST[r.nextInt(FIRST.length)];
        String b = r.nextBoolean() ? kl[r.nextInt(kl.length)] : LAST[r.nextInt(LAST.length)];
        return Character.toLowerCase(a.charAt(a.length() - 1)) == b.charAt(0) ? a + b.substring(1) : a + b; // Mill+ley = Milley, not Millley
    }

    /** The name of the village grown from {@code start}: the one the map shows. */
    public static String name(ServerLevel level, StructureStart start) {
        Identifier id = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getKey(start.getStructure());
        return name(level.getSeed(), start.getChunkPos(), kind(id == null ? "" : id.getPath()));
    }

    /** Only reads the loaded chunk's own structure starts (a map lookup), never another chunk. */
    @SubscribeEvent
    static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        Dashboard.guard("villages", () -> {
            var registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
            event.getChunk().getAllStarts().forEach((structure, start) -> {
                if (!start.isValid() || !registry.wrapAsHolder(structure).is(StructureTags.VILLAGE)) return;
                Identifier id = registry.getKey(structure);
                List<BoundingBox> boxes = new ArrayList<>();
                for (StructurePiece p : start.getPieces()) boxes.add(p.getBoundingBox());
                FOUND.add(of(level.getSeed(), start.getChunkPos(), kind(id == null ? "" : id.getPath()), boxes));
            });
        });
    }

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (FOUND.isEmpty() || !(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        Villages data = get(level);
        for (Village v; (v = FOUND.poll()) != null;) data.add(v);
    }
}
