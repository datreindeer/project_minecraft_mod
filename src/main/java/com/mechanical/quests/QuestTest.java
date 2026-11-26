package com.mechanical.quests;

import static com.mechanical.Mechanical.MODID;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import com.mechanical.TestArea;

import net.minecraft.advancements.AdvancementNode;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.locale.Language;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Game test for the quest screen's data: a fresh player is sent every quest (not only those near ones already earned),
 * quests.json knows each of them and every checklist line names a real thing, and in every chapter's layout no node
 * overlaps another, no line runs through a node, and every follow-up stands to the right of what leads to it.
 */
@EventBusSubscriber(modid = MODID)
public final class QuestTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> TEST = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "quests"));

    static {
        TEST_FUNCTIONS.register("quests", () -> QuestTest::test);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "quests"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "quests"), new FunctionGameTestInstance(TEST, TestArea.of(env, 20, true)));
    }

    private static List<AdvancementNode> children(AdvancementNode node) {
        List<AdvancementNode> out = new ArrayList<>();
        node.children().forEach(c -> {
            if (c.advancement().display().isPresent()) out.add(c);
        });
        out.sort(Comparator.comparingInt((AdvancementNode n) -> QuestData.order(n.holder().id())));
        return out;
    }

    private static void test(GameTestHelper h) {
        var server = h.getLevel().getServer();
        var tree = server.getAdvancements().tree();

        // data: quests.json has exactly the mod's displayed advancements, with a line for every criterion
        int shown = 0;
        for (var holder : server.getAdvancements().getAllAdvancements()) {
            if (!holder.id().getNamespace().equals(MODID) || holder.value().display().isEmpty()) continue;
            shown++;
            QuestData.Quest data = QuestData.QUESTS.get(holder.id());
            h.assertTrue(data != null, "not in quests.json: " + holder.id());
            int criteria = holder.value().criteria().size();
            h.assertTrue(criteria < 2 ? data.criteria().isEmpty() : data.criteria().keySet().equals(holder.value().criteria().keySet()),
                    "checklist and criteria differ: " + holder.id());
            for (String spec : data.criteria().values()) h.assertTrue(named(spec), holder.id() + ": checklist line names nothing: " + spec);
        }
        h.assertTrue(shown > 800 && shown == QuestData.QUESTS.size(), "quests.json has " + QuestData.QUESTS.size() + " of " + shown);

        // layout
        int chapters = 0;
        for (AdvancementNode root : tree.roots()) {
            if (!root.holder().id().getNamespace().equals(MODID) || root.advancement().display().isEmpty()) continue;
            chapters++;
            check(h, root.holder().id().toString(), QuestLayout.layout(root, QuestTest::children));
        }
        h.assertTrue(chapters == QuestData.CHAPTERS.size(), chapters + " chapters, quests.json has " + QuestData.CHAPTERS.size());

        // sync: a player with nothing earned is sent them all
        ServerPlayer player = (ServerPlayer) h.makeMockServerPlayer(GameType.SURVIVAL);
        var advancements = server.getPlayerList().getPlayerAdvancements(player);
        try {
            var flush = advancements.getClass().getDeclaredField("visible");
            flush.setAccessible(true);
            player.connection = net.neoforged.neoforge.common.util.FakePlayerFactory
                    .get(h.getLevel(), new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "quest-test-net")).connection;
            advancements.flushDirty(player, false);
            long sent = ((java.util.Set<?>) flush.get(advancements)).stream()
                    .filter(o -> ((net.minecraft.advancements.AdvancementHolder) o).id().getNamespace().equals(MODID)).count();
            h.assertTrue(sent == shown, "a new player is sent " + sent + " of " + shown + " quests");
        } catch (ReflectiveOperationException e) {
            throw h.assertionException(e.toString());
        }
        h.succeed();
    }

    private static boolean named(String spec) {
        if (spec.startsWith("@")) return Language.getInstance().has(spec.substring(1));
        if (spec.startsWith("#"))
            return BuiltInRegistries.ITEM.getTagOrEmpty(TagKey.create(Registries.ITEM, Identifier.parse(spec.substring(1)))).iterator().hasNext();
        Identifier id = Identifier.parse(spec);
        var item = BuiltInRegistries.ITEM.getOptional(id);
        if (item.isPresent()) return Language.getInstance().has(item.get().getDescriptionId());
        return BuiltInRegistries.BLOCK.getOptional(id).map(b -> Language.getInstance().has(b.getDescriptionId())).orElse(false);
    }

    private static void check(GameTestHelper h, String chapter, QuestLayout.Result<AdvancementNode> result) {
        var places = new ArrayList<>(result.nodes().entrySet());
        int size = QuestLayout.SIZE;
        for (int i = 0; i < places.size(); i++) {
            var a = places.get(i).getValue();
            for (int j = i + 1; j < places.size(); j++) {
                var b = places.get(j).getValue();
                // 6 px apart at least: room for the tick and padlock badges
                h.assertTrue(Math.abs(a.x() - b.x()) >= size + 6 || Math.abs(a.y() - b.y()) >= size + 6,
                        chapter + ": " + places.get(i).getKey() + " and " + places.get(j).getKey() + " crowd each other at " + a + " " + b);
            }
        }
        for (var line : result.lines()) {
            h.assertTrue(result.nodes().get(line.child()).x() > result.nodes().get(line.parent()).x(), chapter + ": line leads left to " + line.child());
            int[] xy = line.xy();
            for (int i = 0; i + 3 < xy.length; i += 2) {
                int lx = Math.min(xy[i], xy[i + 2]), hx = Math.max(xy[i], xy[i + 2]), ly = Math.min(xy[i + 1], xy[i + 3]), hy = Math.max(xy[i + 1], xy[i + 3]);
                for (var place : places) {
                    if (place.getKey() == line.parent() || place.getKey() == line.child()) continue;
                    var p = place.getValue();
                    h.assertTrue(hx < p.x() || lx >= p.x() + size || hy < p.y() || ly >= p.y() + size,
                            chapter + ": the line to " + line.child() + " runs through " + place.getKey());
                }
            }
        }
        h.assertTrue(result.nodes().values().stream().allMatch(p -> p.x() >= 0 && p.y() >= 0 && p.x() + size <= result.width()
                && p.y() + size <= result.height()), chapter + ": a node outside the chapter's bounds");
        com.mojang.logging.LogUtils.getLogger().info("quest layout {}: {} quests, {} x {}", chapter, places.size(), result.width(), result.height());
    }

    private QuestTest() {}
}
