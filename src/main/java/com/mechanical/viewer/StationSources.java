package com.mechanical.viewer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.mechanical.ArmorStationRecipes;
import com.mechanical.BloomeryBlockEntity;
import com.mechanical.FletchingRecipes;
import com.mechanical.KilnBlockEntity;
import com.mechanical.LeatherworkingRecipes;
import com.mechanical.MoldBench;
import com.mechanical.PatternBenchBlock;
import com.mechanical.StationRecipe;
import com.mechanical.ToolStationRecipes;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

/** The mod's Java-coded recipes (stations without JSON), as {@link ViewerRecipe}s. */
@EventBusSubscriber(modid = com.mechanical.Mechanical.MODID)
public final class StationSources {
    private static volatile List<ViewerRecipe> all;

    private StationSources() {}

    /** Built on first use, when the registries are full. */
    public static List<ViewerRecipe> all() {
        List<ViewerRecipe> a = all;
        if (a == null) all = a = build(); // rebuilding twice on a race is harmless
        return a;
    }

    /** Tags and registries changed (reload, datapacks, new world): the tag and predicate snapshots are stale. */
    @SubscribeEvent
    static void onTagsUpdated(TagsUpdatedEvent event) {
        invalidate();
    }

    static void invalidate() {
        all = null;
    }

    private static List<ViewerRecipe> build() {
        List<ViewerRecipe> out = new ArrayList<>();
        for (ToolStationRecipes.Recipe r : ToolStationRecipes.ALL) {
            out.add(new ViewerRecipe(Station.TOOL_STATION, java.util.stream.IntStream.range(0, 3)
                    .mapToObj(i -> new Slot(first(r.parts().get(i), r.options(i)), 1, false)).toList(),
                    new ItemStack(r.result().get()), 0, UnlockKind.RECIPE_ID, r.id()));
        }
        station(out, Station.FLETCHING, FletchingRecipes.ALL);
        station(out, Station.ARMOR_STATION, ArmorStationRecipes.ALL);
        station(out, Station.LEATHERWORKING, LeatherworkingRecipes.ALL);
        station(out, Station.MOLD_BENCH, MoldBench.ALL);
        station(out, Station.PATTERN_BENCH, PatternBenchBlock.RECIPES);
        for (KilnBlockEntity.Recipe r : KilnBlockEntity.RECIPES) matching(out, Station.KILN, r.input(), r.count(), r.result());
        for (BloomeryBlockEntity.Recipe r : BloomeryBlockEntity.RECIPES) matching(out, Station.BLOOMERY, r.input(), r.count(), r.result());
        return List.copyOf(out);
    }

    private static List<Item> first(Item def, List<Item> all) {
        List<Item> out = new ArrayList<>(all);
        out.remove(def);
        out.add(0, def);
        return out;
    }

    private static void station(List<ViewerRecipe> out, Station station, List<StationRecipe> recipes) {
        for (StationRecipe r : recipes) {
            boolean open = r.unlockedBy() == null;
            out.add(new ViewerRecipe(station, r.parts().stream().map(StationSources::slot).toList(), r.assemble(), 0,
                    open ? UnlockKind.STATION_SEEN : UnlockKind.RECIPE_ID,
                    open ? BuiltInRegistries.ITEM.getKey(station.icon().getItem()).toString() : r.id()));
        }
    }

    private static Slot slot(StationRecipe.Part p) {
        List<Item> options = new ArrayList<>();
        if (p.tag() != null) BuiltInRegistries.ITEM.getTagOrEmpty(p.tag()).forEach(h -> options.add(h.value()));
        else {
            options.add(p.item().get());
            if (p.alt() != null) options.add(p.alt().get());
        }
        return new Slot(options, p.count(), p.tool());
    }

    private static void matching(List<ViewerRecipe> out, Station station, Predicate<ItemStack> input, int count, Supplier<Item> result) {
        List<Item> items = BuiltInRegistries.ITEM.stream().filter(i -> input.test(i.getDefaultInstance())).toList();
        if (items.isEmpty()) return;
        out.add(new ViewerRecipe(station, List.of(new Slot(items, count, false)), new ItemStack(result.get()), 0, UnlockKind.ANY_SEEN, ""));
    }
}
