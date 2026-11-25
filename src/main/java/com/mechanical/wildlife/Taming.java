package com.mechanical.wildlife;

import java.util.List;
import java.util.Map;

import com.mechanical.Mechanical;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

/**
 * The farm-friendly wild animals that can be tamed: each has a favourite food (an item tag, written by
 * scripts/gen_husbandry.py) and a number of feedings. The taming itself and the tamed animal's behaviour are in
 * {@link WildAnimal}; a tamed one is livestock ({@link com.mechanical.Husbandry}). Horses, onagers, camels and fennecs
 * have their own taming; predators can't be tamed.
 */
public final class Taming {
    /** A species' favourite food and how many feedings tame it. */
    public record Diet(TagKey<Item> food, int feeds) {}

    /** Ticks it chews before the next feeding counts. */
    public static final int FEED_COOLDOWN = 100;
    /** Everything any of them likes: what a Feeding Trough takes besides vanilla animals' food. */
    public static final TagKey<Item> ANY_FEED = tag("taming_feed");
    private static final TagKey<Item> BOAR = tag("taming_feed/boar"), CATTLE = tag("taming_feed/cattle"), SHEEP = tag("taming_feed/sheep"),
            DEER = tag("taming_feed/deer"), REINDEER = tag("taming_feed/reindeer"), MOOSE = tag("taming_feed/moose"),
            ANTELOPE = tag("taming_feed/antelope"), HARE = tag("taming_feed/hare");

    /** Entity id (path) to diet. */
    private static final Map<String, Diet> SPECIES = Map.ofEntries(
            Map.entry("wild_boar", new Diet(BOAR, 5)),
            Map.entry("mouflon", new Diet(SHEEP, 4)),
            Map.entry("ibex", new Diet(SHEEP, 5)),
            Map.entry("chamois", new Diet(SHEEP, 4)),
            Map.entry("bighorn_sheep", new Diet(SHEEP, 5)),
            Map.entry("aurochs", new Diet(CATTLE, 6)),
            Map.entry("steppe_bison", new Diet(CATTLE, 6)),
            Map.entry("water_buffalo", new Diet(CATTLE, 6)),
            Map.entry("musk_ox", new Diet(CATTLE, 6)),
            Map.entry("roe_deer", new Diet(DEER, 4)),
            Map.entry("red_deer", new Diet(DEER, 5)),
            Map.entry("giant_deer", new Diet(DEER, 8)),
            Map.entry("reindeer", new Diet(REINDEER, 5)),
            Map.entry("moose", new Diet(MOOSE, 6)),
            Map.entry("gazelle", new Diet(ANTELOPE, 4)),
            Map.entry("saiga", new Diet(ANTELOPE, 4)),
            Map.entry("hare", new Diet(HARE, 3)),
            Map.entry("desert_hare", new Diet(HARE, 3)));

    private static TagKey<Item> tag(String path) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mechanical.MODID, path));
    }

    /** The species' diet, or null if it can't be tamed. */
    public static Diet diet(EntityType<?> type) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return id.getNamespace().equals(Mechanical.MODID) ? SPECIES.get(id.getPath()) : null;
    }

    /** The ids of the tameable species (tests). */
    public static List<String> species() {
        return SPECIES.keySet().stream().sorted().toList();
    }

    private Taming() {}
}
