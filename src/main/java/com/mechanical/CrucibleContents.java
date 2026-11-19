package com.mechanical;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What a crucible holds: units per metal id ({@link Metal}). On a Crucible it is ore (by the metal it yields), on a
 * Crucible of Molten/Solid Metal it is the melted, alloyed metal. One unit = one Crushed Copper Ore.
 */
public record CrucibleContents(Map<String, Integer> units) {
    public static final int CAPACITY = 8;
    public static final CrucibleContents EMPTY = new CrucibleContents(Map.of());
    public static final Codec<CrucibleContents> CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT).xmap(CrucibleContents::new, CrucibleContents::units);
    public static final StreamCodec<ByteBuf, CrucibleContents> STREAM_CODEC = ByteBufCodecs.<ByteBuf, String, Integer, Map<String, Integer>>map(HashMap::new,
            ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT).map(CrucibleContents::new, CrucibleContents::units);

    public CrucibleContents {
        units = Map.copyOf(new TreeMap<>(units)); // sorted copy: equal contents compare (and stack) equal
    }

    public int total() {
        return units.values().stream().mapToInt(Integer::intValue).sum();
    }

    public boolean isEmpty() {
        return total() == 0;
    }

    public CrucibleContents add(Metal metal, int n) {
        Map<String, Integer> out = new HashMap<>(units);
        out.merge(metal.getSerializedName(), n, Integer::sum);
        out.values().removeIf(v -> v <= 0);
        return new CrucibleContents(out);
    }

    /**
     * The metal this melts into, or null if the mix makes nothing castable. Now only pure copper; bronze adds the
     * copper + tin rule here (about 9:1) without changing how contents are stored.
     */
    public Metal alloy() {
        if (units.size() != 1) return null;
        return Metal.byId(units.keySet().iterator().next());
    }

    /** The same metal units, all as the alloy (what the Crucible of Molten Metal holds). */
    public CrucibleContents melted() {
        Metal metal = alloy();
        return metal == null ? EMPTY : new CrucibleContents(Map.of(metal.getSerializedName(), total()));
    }
}
