package com.mechanical.people;

import java.util.List;
import java.util.Map;

import com.mechanical.Mechanical;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

/**
 * The kinds of Trader Caravan (docs/traders-plan.md B), data driven: one {@code data/<ns>/trader_kind/<kind>.json} each,
 * written by scripts/gen_traders.py, read again on every datapack reload. A kind's id (its file name) picks its skins
 * (textures/entity/tribesperson/caravan/&lt;kind&gt;/, scripts/gen_tribe_skins.py) and its name on screen
 * (lang {@code trader_kind.<ns>.<kind>}). Prices are in copper (Coins).
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class TraderKinds {
    /** One good: {@code item} is an item id, or {@code #tag} (buys only: one deal per item in the tag). Age 0 Stone, 1 Bronze, 2 Iron. */
    public record Good(String item, int count, int price, int stock, int age, boolean rare) {
        static final Codec<Good> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("item").forGetter(Good::item),
                Codec.INT.optionalFieldOf("count", 1).forGetter(Good::count),
                Codec.INT.fieldOf("price").forGetter(Good::price),
                Codec.INT.optionalFieldOf("stock", 4).forGetter(Good::stock),
                Codec.INT.optionalFieldOf("age", 0).forGetter(Good::age),
                Codec.BOOL.optionalFieldOf("rare", false).forGetter(Good::rare)).apply(i, Good::new));
    }

    /**
     * {@code animal}: "horse" or "camel". {@code purse}: [min, max] copper of a small caravan. {@code weight}: how often
     * this kind comes compared with the others (default {@link #WEIGHT}; the Eastern Caravan is rare).
     */
    public record Kind(String animal, List<String> cargo, List<Integer> purse, List<Good> sells, List<Good> buys, int weight) {
        static final Codec<Kind> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.optionalFieldOf("animal", "horse").forGetter(Kind::animal),
                Codec.STRING.listOf().optionalFieldOf("cargo", List.of()).forGetter(Kind::cargo),
                Codec.INT.listOf(2, 2).fieldOf("purse").forGetter(Kind::purse),
                Good.CODEC.listOf().fieldOf("sells").forGetter(Kind::sells),
                Good.CODEC.listOf().optionalFieldOf("buys", List.of()).forGetter(Kind::buys),
                Codec.INT.optionalFieldOf("weight", WEIGHT).forGetter(Kind::weight)).apply(i, Kind::new));

        public boolean camels() {
            return animal.equals("camel");
        }
    }

    public static final int WEIGHT = 4;

    /** A kind picked at random, each by its weight; null if none are loaded. */
    public static Identifier pick(net.minecraft.util.RandomSource r) {
        int total = kinds.values().stream().mapToInt(k -> Math.max(0, k.weight())).sum();
        if (total <= 0) return null;
        int n = r.nextInt(total);
        for (var e : kinds.entrySet()) if ((n -= Math.max(0, e.getValue().weight())) < 0) return e.getKey();
        return null;
    }

    private static Map<Identifier, Kind> kinds = Map.of();

    /** All kinds loaded, by id (empty until the server's data has loaded). */
    public static Map<Identifier, Kind> all() {
        return kinds;
    }

    public static Kind get(Identifier id) {
        return id == null ? null : kinds.get(id);
    }

    @SubscribeEvent
    static void addListener(AddServerReloadListenersEvent event) {
        event.addListener(Identifier.fromNamespaceAndPath(Mechanical.MODID, "trader_kinds"),
                new SimpleJsonResourceReloadListener<>(Kind.CODEC, FileToIdConverter.json("trader_kind")) {
                    @Override
                    protected void apply(Map<Identifier, Kind> loaded, ResourceManager manager, ProfilerFiller profiler) {
                        kinds = java.util.Collections.unmodifiableMap(new java.util.TreeMap<>(loaded)); // sorted: the same order on every load
                    }
                });
    }

    private TraderKinds() {}
}
