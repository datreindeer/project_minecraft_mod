package com.mechanical.client.viewer;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mechanical.Mechanical;
import com.mechanical.viewer.Note;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;

/** The item notes (assets/mechanical/recipe_notes.json, a copy of docs/registry_notes_en.json), keyed by full item id. */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class ClientNotes {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientNotes.class);
    private static final Identifier FILE = Identifier.fromNamespaceAndPath(Mechanical.MODID, "recipe_notes.json");

    private static volatile Map<String, String> notes = Map.of();

    private ClientNotes() {}

    public static Optional<String> get(Item i) {
        return Optional.ofNullable(notes.get(BuiltInRegistries.ITEM.getKey(i).toString()));
    }

    /** A note page is visible iff its item or any item it links to is seen. */
    public static boolean visible(Item i, Set<String> seen) {
        if (seen.contains(BuiltInRegistries.ITEM.getKey(i).toString())) return true;
        return get(i).map(t -> Note.links(t).stream().anyMatch(seen::contains)).orElse(false);
    }

    @SubscribeEvent
    static void addListener(AddClientReloadListenersEvent event) {
        event.addListener(Identifier.fromNamespaceAndPath(Mechanical.MODID, "recipe_notes"), new SimplePreparableReloadListener<Map<String, String>>() {
            @Override
            protected Map<String, String> prepare(ResourceManager manager, ProfilerFiller profiler) {
                Map<String, String> out = new HashMap<>();
                var res = manager.getResource(FILE);
                if (res.isEmpty()) return out;
                try (var in = new InputStreamReader(res.get().open(), StandardCharsets.UTF_8)) {
                    JsonObject root = JsonParser.parseReader(in).getAsJsonObject();
                    for (var e : root.entrySet()) {
                        if (e.getKey().startsWith("_") || !e.getValue().isJsonObject()) continue;
                        var text = e.getValue().getAsJsonObject().get("text");
                        if (text == null) continue;
                        String key = e.getKey();
                        out.put(key.indexOf(':') >= 0 ? key : Mechanical.MODID + ":" + key, text.getAsString());
                    }
                } catch (Exception ex) {
                    LOGGER.error("Could not read {}", FILE, ex);
                }
                return out;
            }

            @Override
            protected void apply(Map<String, String> loaded, ResourceManager manager, ProfilerFiller profiler) {
                notes = Map.copyOf(loaded);
            }
        });
    }
}
