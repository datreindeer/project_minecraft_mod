package com.mechanical.client.viewer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mechanical.Mechanical;
import com.mechanical.viewer.CatalogPayload;
import com.mechanical.viewer.SeenItems;
import com.mechanical.viewer.StationSources;
import com.mechanical.viewer.ViewerRecipe;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

/** The client's recipe catalog: datapack recipes from the server plus the Java-coded stations, filtered by what this player has unlocked. */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class ClientCatalog {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientCatalog.class);

    private static volatile List<ViewerRecipe> json = List.of();
    // The filtered views are rebuilt only when one of these inputs is a different object (the attachments' lists are replaced on every change).
    private static List<?> keySeen, keyTools, keyJson, keyStations;
    private static final Map<Item, List<ViewerRecipe>> MAKES = new HashMap<>();
    private static final Map<Item, List<ViewerRecipe>> USES = new HashMap<>();
    private static List<Item> searchable = List.of();

    private ClientCatalog() {}

    public static void set(List<ViewerRecipe> recipes) {
        json = recipes;
        LOGGER.debug("Recipe catalog: {} datapack recipes", recipes.size());
    }

    public static List<ViewerRecipe> recipesFor(Item i) {
        refresh();
        return MAKES.getOrDefault(i, List.of());
    }

    public static List<ViewerRecipe> usagesOf(Item i) {
        refresh();
        return USES.getOrDefault(i, List.of());
    }

    /** Items seen plus outputs of unlocked recipes, in registry order. */
    public static List<Item> searchable() {
        refresh();
        return searchable;
    }

    private static void refresh() {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        List<String> seenList = player.getData(SeenItems.SEEN.get());
        List<String> tools = player.getData(Mechanical.UNLOCKED_TOOLS);
        List<ViewerRecipe> js = json;
        List<ViewerRecipe> stations = StationSources.all();
        if (seenList == keySeen && tools == keyTools && js == keyJson && stations == keyStations) return;
        keySeen = seenList;
        keyTools = tools;
        keyJson = js;
        keyStations = stations;

        Set<String> seen = new HashSet<>(seenList);
        List<ViewerRecipe> unlocked = new ArrayList<>();
        for (List<ViewerRecipe> src : List.of(js, stations)) {
            for (ViewerRecipe r : src) if (r.unlocked(seen, tools)) unlocked.add(r);
        }
        unlocked.sort(Comparator.comparing((ViewerRecipe r) -> r.station()).thenComparing(r -> r.output().getHoverName().getString()));

        MAKES.clear();
        USES.clear();
        Set<Item> outputs = new HashSet<>();
        for (ViewerRecipe r : unlocked) {
            MAKES.computeIfAbsent(r.output().getItem(), k -> new ArrayList<>()).add(r);
            outputs.add(r.output().getItem());
            Set<Item> used = new LinkedHashSet<>();
            r.inputs().forEach(s -> used.addAll(s.options()));
            for (Item it : used) USES.computeIfAbsent(it, k -> new ArrayList<>()).add(r);
        }
        List<Item> found = new ArrayList<>();
        for (Item it : BuiltInRegistries.ITEM) {
            if (outputs.contains(it) || seen.contains(BuiltInRegistries.ITEM.getKey(it).toString())) found.add(it);
        }
        searchable = List.copyOf(found);
    }

    @SubscribeEvent
    static void registerHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(CatalogPayload.TYPE, (payload, context) -> set(payload.recipes()));
    }

    @SubscribeEvent
    static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        json = List.of();
    }
}
