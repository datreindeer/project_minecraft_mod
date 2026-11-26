package com.mechanical.quests;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.resources.Identifier;

/**
 * assets/mechanical/quests.json (scripts/gen_advancements.py): what the quest screen can't learn from the advancements the
 * server sends. Chapter order, quest order (siblings are drawn in it) and what each criterion of a many-criteria quest is
 * (an item id, "#item_tag" or "@translation.key"); the client is only sent criterion names.
 */
public final class QuestData {
    /** @param order place in its chapter; @param criteria name to spec, in the generator's order; empty for one criterion. */
    public record Quest(int order, Map<String, String> criteria) {}

    public static final Map<Identifier, Quest> QUESTS = new LinkedHashMap<>();
    /** Chapter ids (stone_age, ...) in sidebar order. */
    public static final List<String> CHAPTERS = new ArrayList<>();

    static {
        try (InputStream in = QuestData.class.getResourceAsStream("/assets/mechanical/quests.json");
                Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            for (JsonElement chapter : JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("chapters")) {
                JsonObject c = chapter.getAsJsonObject();
                CHAPTERS.add(c.get("id").getAsString());
                int order = 0;
                for (JsonElement quest : c.getAsJsonArray("quests")) {
                    Map<String, String> criteria = new LinkedHashMap<>();
                    quest.getAsJsonArray().get(1).getAsJsonObject().entrySet().forEach(e -> criteria.put(e.getKey(), e.getValue().getAsString()));
                    QUESTS.put(Identifier.parse(quest.getAsJsonArray().get(0).getAsString()), new Quest(order++, criteria));
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("assets/mechanical/quests.json", e);
        }
    }

    /** Sort key for siblings and chapters; unknown advancements (another datapack's) go last. */
    public static int order(Identifier id) {
        Quest q = QUESTS.get(id);
        return q == null ? Integer.MAX_VALUE : q.order();
    }

    /** The chapter's place in the sidebar, by its root's id (mechanical:stone_age/root); unknown ones go last. */
    public static int chapterOrder(Identifier root) {
        int at = CHAPTERS.indexOf(root.getPath().split("/")[0]);
        return at < 0 ? CHAPTERS.size() : at;
    }

    private QuestData() {}
}
