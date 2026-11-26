package com.mechanical.client.viewer;

import static com.mechanical.client.ui.HideUi.DIM;
import static com.mechanical.client.ui.HideUi.GOOD;
import static com.mechanical.client.ui.HideUi.BAD;
import static com.mechanical.client.ui.HideUi.EDGE;
import static com.mechanical.client.ui.HideUi.EDGE_DARK;
import static com.mechanical.client.ui.HideUi.GOLD;
import static com.mechanical.client.ui.HideUi.LABEL;
import static com.mechanical.client.ui.HideUi.NAME;
import static com.mechanical.client.ui.HideUi.TEXT;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.mechanical.BloomeryBlockEntity;
import com.mechanical.KilnBlockEntity;
import com.mechanical.ToolStationRecipes;
import com.mechanical.tools.ToolParts;
import com.mechanical.tools.ToolStats;
import com.mechanical.client.ui.HideUi;
import com.mechanical.viewer.Note;
import com.mechanical.viewer.SeenItems;
import com.mechanical.viewer.Slot;
import com.mechanical.viewer.Station;
import com.mechanical.viewer.ViewerRecipe;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The recipe viewer's page for one item: its recipes (R) or what it is used in (U), a tab per station and one recipe per
 * page, plus the item's note ("How to get it"). Items on the page are links: left click shows their recipes, right click
 * their uses; Backspace and Esc walk back.
 */
public class RecipeViewScreen extends Screen {
    private static final int W = 236, H = 176, TAB_W = 24, TAB_H = 22, TAB_STEP = 26;
    /** The body (between the header seam and the footer). */
    private static final int BODY_Y = 38, BODY_H = H - 38 - 30, NOTE_H = H - 38 - 8;
    private static final int ARROW = HideUi.ARROW, ARROW_LIP = HideUi.ARROW_LIP;
    private static final String[] FLAME = {"......#.......", ".....##.......", ".....###......", "....####..#...", "..#.#####.#...",
            "..#.#####.##..", ".##.##########", ".#############", "##############", "######.#######", "#####...######", ".###.....####.",
            ".###.....###..", "...###..###..."};

    private record Step(Item item, boolean usage, int tab, int page) {}

    /** A station's recipes, or the note when {@code station} is null. */
    private record Tab(@Nullable Station station, List<ViewerRecipe> recipes) {}

    /** Something on the page under the mouse: an item (a link), or a tab. */
    private record Hit(int x, int y, int w, int h, @Nullable ItemStack stack, @Nullable Component tip, @Nullable Runnable click) {
        boolean has(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    /** A laid-out piece of note text; {@code item} set for a link that has been seen. */
    private record Run(Component text, @Nullable Item item, boolean unseen, int x, int w) {}

    private record Line(List<Run> runs, int h) {}

    private final @Nullable Screen parent;
    private final Deque<Step> history = new ArrayDeque<>();
    private Item item;
    private boolean usage;
    private int tab, page, ticks;
    private double noteScroll;
    private List<Tab> tabs = List.of();
    private List<Line> noteLines = List.of();
    private Set<String> seen = Set.of();
    private @Nullable List<ViewerRecipe> shownList;
    private @Nullable List<String> shownSeen;
    private final List<Hit> hits = new ArrayList<>();
    private int left, top;
    private Button prev, next;
    private @Nullable Item hovered;

    private RecipeViewScreen(@Nullable Screen parent, Item item, boolean usage) {
        super(Component.empty());
        this.parent = parent;
        this.item = item;
        this.usage = usage;
    }

    /** Shows {@code item}'s recipes (or uses) over the current screen; on this screen already, as a step forward. */
    public static RecipeViewScreen open(Item item, boolean usage) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() instanceof RecipeViewScreen open) {
            open.go(item, usage);
            return open;
        }
        RecipeViewScreen screen = new RecipeViewScreen(mc.gui.screen(), item, usage);
        mc.gui.setScreen(screen);
        return screen;
    }

    private void go(Item to, boolean use) {
        if (to == item && use == usage) return;
        history.push(new Step(item, usage, tab, page));
        show(to, use, 0, 0);
    }

    private void show(Item to, boolean use, int toTab, int toPage) {
        item = to;
        usage = use;
        tab = toTab;
        page = toPage;
        noteScroll = 0;
        shownList = null;
        refresh();
    }

    private void back() {
        Step s = history.pop();
        show(s.item(), s.usage(), s.tab(), s.page());
    }

    /** Turns to tab {@code i}, counted from the end when negative (the dev shots use it). */
    public void showTab(int i) {
        refresh();
        if (tabs.isEmpty()) return;
        tab = Math.floorMod(i, tabs.size());
        page = 0;
        noteScroll = 0;
    }

    /** The item link under the mouse in the last frame, for the R and U keys. */
    public @Nullable Item hoveredItem() {
        return hovered;
    }

    // ------------------------------------------------------------------ data

    /** Rebuilds the tabs when the catalog or the seen items changed (a recipe can unlock while the page is open). */
    private void refresh() {
        if (minecraft == null || minecraft.player == null) return;
        List<ViewerRecipe> list = usage ? ClientCatalog.usagesOf(item) : ClientCatalog.recipesFor(item);
        List<String> seenList = minecraft.player.getData(SeenItems.SEEN.get());
        if (list == shownList && seenList == shownSeen) return;
        shownList = list;
        shownSeen = seenList;
        seen = new HashSet<>(seenList);
        List<Tab> out = new ArrayList<>();
        for (ViewerRecipe r : list) { // the catalog is sorted by station
            if (out.isEmpty() || out.get(out.size() - 1).station() != r.station()) out.add(new Tab(r.station(), new ArrayList<>()));
            out.get(out.size() - 1).recipes().add(r);
        }
        noteLines = List.of();
        if (!usage && ClientNotes.visible(item, seen)) {
            String text = ClientNotes.get(item).orElse(null);
            if (text != null) {
                out.add(new Tab(null, List.of()));
                noteLines = layout(Note.parse(text), W - 16 - 14);
            }
        }
        tabs = out;
        tab = Math.clamp(tab, 0, Math.max(0, tabs.size() - 1));
        page = Math.clamp(page, 0, Math.max(0, pages() - 1));
    }

    private @Nullable Tab current() {
        return tabs.isEmpty() ? null : tabs.get(tab);
    }

    private int pages() {
        Tab t = current();
        return t == null || t.station() == null ? 1 : t.recipes().size();
    }

    private void turn(int by) {
        int n = pages();
        if (n > 1) page = Math.floorMod(page + by, n);
    }

    // ------------------------------------------------------------------ screen

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = Math.max(TAB_H + 2, (height - H + TAB_H) / 2);
        int cx = left + W / 2, by = top + H - 24;
        prev = addRenderableWidget(Button.builder(Component.literal("◀"), b -> turn(-1)).bounds(cx - 52, by, 16, 16).build());
        next = addRenderableWidget(Button.builder(Component.literal("▶"), b -> turn(1)).bounds(cx + 36, by, 16, 16).build());
        refresh();
    }

    @Override
    public void tick() {
        ticks++;
    }

    @Override
    public void onClose() {
        if (!history.isEmpty()) back();
        else minecraft.gui.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        refresh();
        int key = event.key();
        if (key == InputConstants.KEY_BACKSPACE) {
            if (!history.isEmpty()) back();
            return true;
        }
        if (key == InputConstants.KEY_LEFT || key == InputConstants.KEY_RIGHT) {
            turn(key == InputConstants.KEY_LEFT ? -1 : 1);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        int button = event.button();
        if (button != 0 && button != 1) return false;
        for (Hit hit : hits) {
            if (!hit.has(event.x(), event.y())) continue;
            if (hit.click != null) {
                if (button != 0) return false;
                click();
                hit.click.run();
                return true;
            }
            if (hit.stack != null) {
                click();
                go(hit.stack.getItem(), button == 1);
                return true;
            }
        }
        return false;
    }

    private void click() {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        Tab t = current();
        if (t != null && t.station() == null) {
            noteScroll = Math.clamp(noteScroll - scrollY * 12, 0, maxNoteScroll());
            return true;
        }
        if (scrollY != 0) turn(scrollY > 0 ? -1 : 1);
        return true;
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        refresh();
        hits.clear();
        tabs(g, mouseX, mouseY);
        HideUi.panel(g, left, top, W, H);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        header(g);
        Tab t = current();
        int bx = left + 8, by = top + BODY_Y, bw = W - 16;
        if (t == null) {
            g.centeredText(font, Component.translatable("viewer.mechanical.none"), left + W / 2, by + BODY_H / 2 - 4, DIM);
        } else if (t.station() == null) {
            note(g, bx, by, bw, NOTE_H, mouseX, mouseY);
        } else {
            recipe(g, t.recipes().get(page), bx, by, bw, BODY_H, mouseX, mouseY);
        }
        boolean paging = pages() > 1;
        prev.visible = next.visible = paging;
        if (t != null && t.station() != null) {
            int fy = top + H - 20;
            g.centeredText(font, Component.translatable("viewer.mechanical.page", page + 1, pages()), left + W / 2, fy, paging ? LABEL : DIM);
        }
        super.extractRenderState(g, mouseX, mouseY, a);

        hovered = null;
        for (Hit hit : hits)
            if (hit.has(mouseX, mouseY)) {
                if (hit.stack != null) {
                    hovered = hit.stack.getItem();
                    g.setTooltipForNextFrame(font, hit.stack, mouseX, mouseY);
                } else if (hit.tip != null) g.setTooltipForNextFrame(font, hit.tip, mouseX, mouseY);
                break;
            }
    }

    /** One tab per station above the panel (tucked under its edge), the chosen one raised and lit. */
    private void tabs(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int step = Math.min(TAB_STEP, tabs.isEmpty() ? TAB_STEP : (W - 12) / tabs.size());
        for (int i = 0; i < tabs.size(); i++) {
            Tab t = tabs.get(i);
            boolean chosen = i == tab;
            int x = left + 6 + i * step, y = top - TAB_H + (chosen ? 2 : 4);
            int index = i;
            Component name = t.station() == null ? Component.translatable("viewer.mechanical.how_to_get") : Component.translatable(t.station().langKey());
            Hit hit = new Hit(x, y, TAB_W, top - y, null, name, () -> {
                tab = index;
                page = 0;
                noteScroll = 0;
            });
            hits.add(hit);
            boolean hover = hit.has(mouseX, mouseY);
            HideUi.row(g, x, y, TAB_W, TAB_H + 2, chosen || hover, true);
            if (chosen) g.outline(x, y, TAB_W, TAB_H + 2, GOLD);
            g.item(t.station() == null ? new ItemStack(Items.WRITABLE_BOOK) : t.station().icon(), x + 4, y + 3);
        }
    }

    private void header(GuiGraphicsExtractor g) {
        int x = left + 8, y = top + 8;
        HideUi.slot(g, x, y);
        g.item(new ItemStack(item), x + 1, y + 1);
        Component name = new ItemStack(item).getHoverName();
        Component title = Component.translatable(usage ? "viewer.mechanical.usages" : "viewer.mechanical.recipes", name).withStyle(ChatFormatting.BOLD);
        int tw = W - 8 - (x + 24 - left);
        g.text(font, cut(title, tw), x + 24, y, NAME, true);
        Tab t = current();
        Component sub = t == null ? Component.empty() : t.station() == null ? Component.translatable("viewer.mechanical.how_to_get")
                : Component.translatable(t.station().langKey());
        g.text(font, cut(sub, tw), x + 24, y + 11, DIM, false);
        hits.add(new Hit(x, y, 18, 18, new ItemStack(item), null, null));
        HideUi.seam(g, left + 7, top + 31, W - 14);
        if (t == null || t.station() != null) HideUi.seam(g, left + 7, top + H - 28, W - 14); // the note's well reaches down instead
    }

    /** One line of {@code text} no wider than {@code w}, cut with an ellipsis. */
    private FormattedCharSequence cut(Component text, int w) {
        if (font.width(text) <= w) return text.getVisualOrderText();
        return net.minecraft.locale.Language.getInstance().getVisualOrder(
                FormattedText.composite(font.substrByWidth(text, w - font.width("...")), FormattedText.of("...")));
    }

    // ------------------------------------------------------------------ recipes

    private void recipe(GuiGraphicsExtractor g, ViewerRecipe r, int bx, int by, int bw, int bh, int mouseX, int mouseY) {
        int cy = by + bh / 2;
        switch (r.station()) {
            case CRAFTING_SHAPED, CRAFTING_SHAPELESS -> {
                int x = bx + (bw - 126) / 2, y = cy - 27;
                int w = r.station() == Station.CRAFTING_SHAPED && r.width() > 0 ? r.width() : 3;
                for (int i = 0; i < 9; i++) HideUi.slot(g, x + i % 3 * 18, y + i / 3 * 18);
                for (int i = 0; i < r.inputs().size(); i++) {
                    int col = i % w, row = i / w;
                    if (row < 3 && col < 3) input(g, r.inputs().get(i), x + col * 18, y + row * 18, mouseX, mouseY);
                }
                HideUi.arrow(g, x + 54 + 12, cy - 8);
                output(g, r, x + 100, cy - 13, mouseX, mouseY);
            }
            case TOOL_STATION, FLETCHING, ARMOR_STATION, LEATHERWORKING, MOLD_BENCH, PATTERN_BENCH -> {
                int n = r.inputs().size(), x = bx + 8, ox = bx + bw - 8 - 26, ax = ox - 8 - 22;
                int y0 = cy - (n * 22 - 4) / 2;
                Item[] picked = new Item[n];
                for (int i = 0; i < n; i++) {
                    Slot s = r.inputs().get(i);
                    int y = y0 + i * 22;
                    HideUi.slot(g, x, y);
                    Item shown = input(g, s, x, y, mouseX, mouseY);
                    picked[i] = shown;
                    List<Component> lines = new ArrayList<>();
                    if (r.station() == Station.TOOL_STATION && i < 3)
                        lines.add(Component.translatable("gui.mechanical.tool_station." + com.mechanical.client.ToolStationScreen.partKey(toolRecipe(r), i)).withColor(LABEL));
                    if (shown != null) lines.add(new ItemStack(shown).getHoverName().copy().withColor(TEXT));
                    if (s.tool()) lines.add(Component.literal("(").append(Component.translatable("viewer.mechanical.tool")).append(")").withColor(DIM));
                    int lw = ax - 6 - (x + 22), ly = y + 9 - lines.size() * 10 / 2 + 1;
                    for (Component line : lines) {
                        g.text(font, cut(line, lw), x + 22, ly, -1, false);
                        ly += 10;
                    }
                }
                HideUi.arrow(g, ax, cy - 8);
                output(g, r, ox, cy - 13, mouseX, mouseY);
                if (r.station() == Station.TOOL_STATION && n == 3) toolStats(g, r, picked, bx + bw / 2, cy + 38);
            }
            default -> { // one in, one out
                boolean fire = r.station() != Station.STONECUTTING;
                int x = bx + (bw - 18 - 14 - 22 - 14 - 26) / 2, iy = fire ? cy - 18 : cy - 9;
                HideUi.slot(g, x, iy);
                input(g, r.inputs().get(0), x, iy, mouseX, mouseY);
                if (fire) stamp(g, FLAME, x + 2, iy + 21);
                int ax = x + 18 + 14;
                HideUi.arrow(g, ax, cy - 8);
                output(g, r, ax + 22 + 14, cy - 13, mouseX, mouseY);
                String[] heat = heat(r);
                if (heat != null) {
                    g.centeredText(font, heat[0], ax + 11, cy + 12, GOLD);
                    g.centeredText(font, heat[1], ax + 11, cy + 23, DIM);
                }
            }
        }
    }

    private static ToolStationRecipes.@Nullable Recipe toolRecipe(ViewerRecipe r) {
        return ToolStationRecipes.ALL.stream().filter(t -> t.id().equals(r.unlockId())).findFirst().orElse(null);
    }

    /** The Tool Station's numbers for the parts shown (no Smithing), against the recipe's own parts, in the Tool Station's columns. */
    private void toolStats(GuiGraphicsExtractor g, ViewerRecipe r, @Nullable Item[] picked, int cx, int y) {
        ToolStationRecipes.Recipe tr = toolRecipe(r);
        if (tr == null || !tr.dynamic() || java.util.Arrays.asList(picked).contains(null)) return;
        ItemStack plain = new ItemStack(tr.result().get()), built = plain.copy();
        tr.apply(built, picked[0], picked[1], picked[2], 0);
        ToolStats.Stats base = ToolStats.of(plain), now = ToolStats.of(built);
        String[] keys = com.mechanical.client.ToolStationScreen.columns(tr.kind()), vals = com.mechanical.client.ToolStationScreen.values(tr.kind(), now);
        // a bow's draw time is better shorter
        double flip = tr.kind() == ToolParts.Kind.BOW ? -1 : 1;
        double[] a = {now.durability(), flip * now.speed(), now.damage()}, b = {base.durability(), flip * base.speed(), base.damage()};
        int total = 0, cols = keys[2].isEmpty() ? 2 : 3;
        for (int i = 0; i < cols; i++) total += font.width(Component.translatable("gui.mechanical.tool_station.col." + keys[i])) + 3 + font.width(vals[i]) + (i < cols - 1 ? 10 : 0);
        int x = cx - total / 2;
        for (int i = 0; i < cols; i++) {
            Component label = Component.translatable("gui.mechanical.tool_station.col." + keys[i]);
            g.text(font, label, x, y, DIM, false);
            x += font.width(label) + 3;
            g.text(font, vals[i], x, y, Math.abs(a[i] - b[i]) < 0.005 ? TEXT : a[i] > b[i] ? GOOD : BAD, false);
            x += font.width(vals[i]) + 10;
        }
    }

    /** Draws the slot's current seen option (they take turns every second); returns it. Unseen alternatives never show: they would spoil later ages. */
    private @Nullable Item input(GuiGraphicsExtractor g, Slot s, int x, int y, int mouseX, int mouseY) {
        if (s.options().isEmpty()) return null;
        List<Item> known = s.options().stream().filter(i -> seen.contains(BuiltInRegistries.ITEM.getKey(i).toString())).toList();
        if (known.isEmpty()) known = s.options().subList(0, 1); // the recipe is unlocked, so only a non-key slot gets here
        Item shown = known.get(ticks / 20 % known.size());
        ItemStack stack = new ItemStack(shown, Math.max(1, s.count()));
        g.item(stack, x + 1, y + 1);
        g.itemDecorations(font, stack, x + 1, y + 1);
        pocketHit(g, x, y, 18, stack, mouseX, mouseY);
        return shown;
    }

    private void output(GuiGraphicsExtractor g, ViewerRecipe r, int x, int y, int mouseX, int mouseY) {
        HideUi.pocket(g, x, y, 26, 26);
        g.item(r.output(), x + 5, y + 5);
        g.itemDecorations(font, r.output(), x + 5, y + 5);
        pocketHit(g, x, y, 26, r.output(), mouseX, mouseY);
    }

    private void pocketHit(GuiGraphicsExtractor g, int x, int y, int size, ItemStack stack, int mouseX, int mouseY) {
        Hit hit = new Hit(x, y, size, size, stack, null, null);
        hits.add(hit);
        if (hit.has(mouseX, mouseY)) g.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0x40F0D8A8);
    }

    /** The kiln's or bloomery's heat and time for this recipe (not in the synced recipe: looked up by its output and input). */
    private static String @Nullable [] heat(ViewerRecipe r) {
        ItemStack in = r.inputs().get(0).options().get(0).getDefaultInstance();
        if (r.station() == Station.KILN)
            for (KilnBlockEntity.Recipe k : KilnBlockEntity.RECIPES)
                if (r.output().is(k.result().get()) && k.input().test(in)) return new String[] {k.temp() + "°", time(k.ticks())};
        if (r.station() == Station.BLOOMERY)
            for (BloomeryBlockEntity.Recipe b : BloomeryBlockEntity.RECIPES)
                if (r.output().is(b.result().get()) && b.input().test(in)) return new String[] {b.temp() + "°", time(b.ticks())};
        return null;
    }

    private static String time(int ticks) {
        int s = ticks / 20;
        return s < 60 ? s + " s" : s / 60 + " min" + (s % 60 > 0 ? " " + s % 60 + " s" : "");
    }

    /** A shape pressed into the leather: dark, with a light lip along its lower edges. */
    private static void stamp(GuiGraphicsExtractor g, String[] rows, int x, int y) {
        for (int j = 0; j < rows.length; j++)
            for (int i = 0; i < rows[j].length(); i++) {
                if (rows[j].charAt(i) != '#') continue;
                if (j + 1 >= rows.length || rows[j + 1].charAt(i) != '#') g.fill(x + i, y + j + 1, x + i + 1, y + j + 2, ARROW_LIP);
                g.fill(x + i, y + j, x + i + 1, y + j + 1, ARROW);
            }
    }

    // ------------------------------------------------------------------ note

    /** Lays the note out in lines {@code w} wide: words wrap, links are an icon and a name (or ??? when not seen). */
    private List<Line> layout(List<Note.Piece> pieces, int w) {
        // first the words and links, each knowing whether a space comes before it (null: a line break)
        List<Run> words = new ArrayList<>();
        List<Boolean> gaps = new ArrayList<>();
        boolean gap = false;
        for (Note.Piece p : pieces) {
            if (p.itemId() != null) {
                Identifier id = Identifier.tryParse(p.itemId());
                Item link = id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
                boolean known = link != null && seen.contains(p.itemId());
                Component text = known ? new ItemStack(link).getHoverName().copy() : Component.literal("???");
                if (p.bold()) text = text.copy().withStyle(ChatFormatting.BOLD);
                words.add(new Run(text, known ? link : null, !known, 0, Math.min(w, (known ? 18 : 0) + font.width(text))));
                gaps.add(gap);
                gap = false;
                continue;
            }
            StringBuilder word = new StringBuilder();
            String s = p.text() + "\0"; // the sentinel flushes the last word
            for (int k = 0; k < s.length(); k++) {
                char c = s.charAt(k);
                if (c != ' ' && c != '\n' && c != '\0') {
                    word.append(c);
                    continue;
                }
                if (!word.isEmpty()) {
                    Component text = Component.literal(word.toString());
                    if (p.bold()) text = text.copy().withStyle(ChatFormatting.BOLD);
                    words.add(new Run(text, null, false, 0, font.width(text)));
                    gaps.add(gap);
                    word.setLength(0);
                    gap = false;
                }
                if (c == ' ') gap = true;
                if (c == '\n') {
                    words.add(null);
                    gaps.add(false);
                    gap = false;
                }
            }
        }
        // then the lines: what touches without a space ("(" + a link + ")") wraps as one
        List<Line> lines = new ArrayList<>();
        List<Run> runs = new ArrayList<>();
        int x = 0, space = font.width(" ");
        for (int i = 0; i < words.size(); i++) {
            if (words.get(i) == null) {
                lines.add(line(runs));
                runs = new ArrayList<>();
                x = 0;
                continue;
            }
            int end = i + 1, glued = words.get(i).w();
            while (end < words.size() && words.get(end) != null && !gaps.get(end)) glued += words.get(end++).w();
            int at = x > 0 && gaps.get(i) ? x + space : x;
            if (at + glued > w && x > 0) {
                lines.add(line(runs));
                runs = new ArrayList<>();
                at = 0;
            }
            for (int k = i; k < end; k++) {
                Run r = words.get(k);
                if (at + r.w() > w && at > 0) { // a glued run longer than a line breaks where it must
                    lines.add(line(runs));
                    runs = new ArrayList<>();
                    at = 0;
                }
                runs.add(new Run(r.text(), r.item(), r.unseen(), at, r.w()));
                at += r.w();
            }
            x = at;
            i = end - 1;
        }
        if (!runs.isEmpty()) lines.add(line(runs));
        return lines;
    }

    private static Line line(List<Run> runs) {
        return new Line(runs, runs.stream().anyMatch(r -> r.item() != null) ? 18 : 11);
    }

    private int noteHeight() {
        return noteLines.stream().mapToInt(Line::h).sum();
    }

    private double maxNoteScroll() {
        return Math.max(0, noteHeight() - (NOTE_H - 8));
    }

    private void note(GuiGraphicsExtractor g, int bx, int by, int bw, int bh, int mouseX, int mouseY) {
        HideUi.inset(g, bx, by, bw, bh);
        int x0 = bx + 5, top0 = by + 4, bottom = by + bh - 4;
        noteScroll = Math.clamp(noteScroll, 0, maxNoteScroll());
        g.enableScissor(bx + 2, by + 2, bx + bw - 2, by + bh - 2);
        int y = top0 - (int) noteScroll;
        for (Line line : noteLines) {
            if (y + line.h() > by && y < by + bh) {
                int ty = y + (line.h() - 8) / 2;
                for (Run r : line.runs()) {
                    if (r.item() == null) {
                        g.text(font, r.text(), x0 + r.x(), ty, r.unseen() ? DIM : TEXT, false);
                        continue;
                    }
                    ItemStack stack = new ItemStack(r.item());
                    Hit hit = new Hit(x0 + r.x(), y, r.w(), line.h(), stack, null, null);
                    boolean hover = hit.has(mouseX, mouseY) && mouseY >= by && mouseY < bottom + 4;
                    if (y >= top0 - 2 && y + line.h() <= bottom + 2) hits.add(hit);
                    g.item(stack, x0 + r.x(), y + 1);
                    Component name = hover ? r.text().copy().withStyle(ChatFormatting.UNDERLINE) : r.text();
                    g.text(font, cut(name.copy(), r.w() - 18), x0 + r.x() + 18, ty, hover ? NAME : GOLD, false);
                }
            }
            y += line.h();
        }
        g.disableScissor();
        double max = maxNoteScroll();
        if (max > 0) { // a thin scroll bar in the well's right edge
            int trackY = by + 3, trackH = bh - 6, barH = Math.max(12, trackH * (bh - 8) / noteHeight());
            int barY = trackY + (int) ((trackH - barH) * noteScroll / max);
            int sx = bx + bw - 6;
            g.fill(sx, trackY, sx + 3, trackY + trackH, EDGE_DARK & 0x80FFFFFF);
            g.fill(sx, barY, sx + 3, barY + barH, EDGE);
        }
    }
}
