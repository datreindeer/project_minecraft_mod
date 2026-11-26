package com.mechanical.client.quests;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.mechanical.quests.QuestData;
import com.mechanical.quests.QuestLayout;

import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** What the quest screen draws: the player's advancements as chapters of quests, each with its state and place. */
final class QuestBook {
    enum State { DONE, READY, LOCKED }

    /** One line of a quest's checklist. {@code icons} may be empty; a tag has several, shown in turn. */
    record Check(Component name, List<ItemStack> icons, boolean done) {}

    static final class Quest {
        final AdvancementNode node;
        final DisplayInfo display;
        final ItemStack icon;
        final Quest parent;
        final List<Quest> next = new ArrayList<>();
        AdvancementProgress progress;
        State state;
        /** Requirements met, of how many there are. */
        int met, total;
        QuestLayout.Point at;

        private Quest(AdvancementNode node, DisplayInfo display, Quest parent) {
            this.node = node;
            this.display = display;
            this.icon = display.icon().create();
            this.parent = parent;
        }

        Identifier id() {
            return node.holder().id();
        }

        Component title() {
            return display.title();
        }

        /** task, goal or challenge: the node's shape. */
        String shape() {
            AdvancementType type = display.type();
            return type.getSerializedName();
        }

        /** Begun but not finished: some of several requirements are met. */
        boolean partial() {
            return state != State.DONE && met > 0;
        }

        private boolean has(String criterion) {
            CriterionProgress c = progress == null ? null : progress.getCriterion(criterion);
            return c != null && c.isDone();
        }

        /** The criteria worth listing: those of a quest with more than one. */
        List<Check> checklist() {
            QuestData.Quest data = QuestData.QUESTS.get(id());
            List<Check> out = new ArrayList<>();
            if (data != null) data.criteria().forEach((name, spec) -> out.add(check(spec, has(name))));
            return out;
        }

        private Check check(String spec, boolean done) {
            if (spec.startsWith("@")) {
                String key = spec.substring(1);
                List<ItemStack> icons = List.of(icon);
                if (key.startsWith("entity.")) { // the animal's spawn egg
                    String[] part = key.split("\\.", 3);
                    icons = BuiltInRegistries.ITEM.getOptional(Identifier.fromNamespaceAndPath(part[1], part[2] + "_spawn_egg"))
                            .map(item -> List.of(new ItemStack(item))).orElse(List.of());
                }
                return new Check(Component.translatable(key), icons, done);
            }
            if (spec.startsWith("#")) {
                Identifier tag = Identifier.parse(spec.substring(1));
                List<ItemStack> icons = new ArrayList<>();
                BuiltInRegistries.ITEM.getTagOrEmpty(TagKey.create(Registries.ITEM, tag)).forEach(h -> icons.add(new ItemStack(h.value())));
                return new Check(Component.translatable("gui.mechanical.quests.any_tag", pretty(tag.getPath())), icons, done);
            }
            Identifier id = Identifier.parse(spec);
            Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
            if (item != null) {
                ItemStack stack = new ItemStack(item);
                return new Check(stack.getHoverName(), List.of(stack), done);
            }
            Block block = BuiltInRegistries.BLOCK.getValue(id); // a placed or stood-on block without its own item
            ItemStack as = new ItemStack(block.asItem());
            return new Check(block.getName(), as.isEmpty() ? List.of() : List.of(as), done);
        }

        private static String pretty(String path) {
            StringBuilder out = new StringBuilder();
            for (String word : path.substring(path.lastIndexOf('/') + 1).split("_"))
                if (!word.isEmpty()) out.append(out.isEmpty() ? "" : " ").append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            return out.toString();
        }
    }

    static final class Chapter {
        final Quest root;
        /** In reading order: the root, then branch by branch. */
        final List<Quest> quests = new ArrayList<>();
        final QuestLayout.Result<Quest> layout;
        int done, ready;

        private Chapter(Quest root) {
            this.root = root;
            walk(root, quests::add);
            layout = QuestLayout.layout(root, q -> q.next);
            quests.forEach(q -> q.at = layout.nodes().get(q));
        }

        Identifier id() {
            return root.id();
        }

        /** What can be done now: begun quests first, then those furthest left (earliest on their path). */
        List<Quest> readyNow() {
            return quests.stream().filter(q -> q.state == State.READY).sorted(Comparator.<Quest, Boolean>comparing(q -> !q.partial())
                    .thenComparingInt(q -> q.at.x()).thenComparingInt(q -> q.at.y())).toList();
        }
    }

    final List<Chapter> chapters = new ArrayList<>();
    final Map<Identifier, Quest> byId = new HashMap<>();
    int done, total;

    QuestBook(ClientAdvancements advancements) {
        List<AdvancementNode> roots = new ArrayList<>();
        advancements.tree().roots().forEach(root -> {
            if (root.advancement().display().isPresent()) roots.add(root);
        });
        roots.sort(Comparator.<AdvancementNode>comparingInt(n -> QuestData.chapterOrder(n.holder().id())).thenComparing(n -> n.holder().id().toString()));
        for (AdvancementNode root : roots) {
            Chapter chapter = new Chapter(quest(advancements, root, null));
            for (Quest q : chapter.quests) {
                if (q.state == State.DONE) chapter.done++;
                else if (q.state == State.READY) chapter.ready++;
            }
            done += chapter.done;
            total += chapter.quests.size();
            chapters.add(chapter);
        }
    }

    private Quest quest(ClientAdvancements advancements, AdvancementNode node, Quest parent) {
        Quest q = new Quest(node, node.advancement().display().orElseThrow(), parent);
        q.progress = advancements.progress().get(node.holder());
        var requirements = node.advancement().requirements();
        q.total = requirements.size();
        for (List<String> any : requirements.requirements())
            if (any.stream().anyMatch(q::has)) q.met++;
        q.state = q.progress != null && q.progress.isDone() ? State.DONE
                : parent == null || parent.state == State.DONE ? State.READY : State.LOCKED;
        byId.put(q.id(), q);
        List<AdvancementNode> children = new ArrayList<>();
        node.children().forEach(c -> {
            if (c.advancement().display().isPresent()) children.add(c);
        });
        // the client keeps children in no order: back to the generator's
        children.sort(Comparator.<AdvancementNode>comparingInt(n -> QuestData.order(n.holder().id())).thenComparing(n -> n.holder().id().toString()));
        for (AdvancementNode child : children) q.next.add(quest(advancements, child, q));
        return q;
    }

    private static void walk(Quest q, Consumer<Quest> each) {
        each.accept(q);
        q.next.forEach(c -> walk(c, each));
    }
}
