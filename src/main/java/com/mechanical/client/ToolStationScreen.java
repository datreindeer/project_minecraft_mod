package com.mechanical.client;

import static com.mechanical.client.ui.HideUi.BAD;
import static com.mechanical.client.ui.HideUi.DIM;
import static com.mechanical.client.ui.HideUi.EDGE;
import static com.mechanical.client.ui.HideUi.GOLD;
import static com.mechanical.client.ui.HideUi.GOOD;
import static com.mechanical.client.ui.HideUi.LABEL;
import static com.mechanical.client.ui.HideUi.NAME;
import static com.mechanical.client.ui.HideUi.SLOT_FILL;
import static com.mechanical.client.ui.HideUi.TEXT;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.mechanical.Bronze;
import com.mechanical.Kettle;
import com.mechanical.RecipeStationMenu;
import com.mechanical.ToolStationMenu;
import com.mechanical.ToolStationRecipes;
import com.mechanical.ToolStationRecipes.Recipe;
import com.mechanical.client.ui.HideUi;
import com.mechanical.skills.Skill;
import com.mechanical.skills.Skills;
import com.mechanical.tools.ToolParts;
import com.mechanical.tools.ToolStats;
import com.mechanical.viewer.SeenItems;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Left: the tool types you know (one pocket each) and under them the recipes of the picked type, one per head material.
 * Right: head, binding and rod pockets with a faded ghost of what goes there and which parts would do, the result, and
 * a ledger of what each part does to durability, speed and damage. Slot positions are ToolStationMenu's.
 */
public class ToolStationScreen extends AbstractContainerScreen<ToolStationMenu> {
    /** Type keys in list order; bows, fishing rods, the broom and buckets are here so they stay craftable. */
    private static final String[] TYPES = {"pickaxe", "axe", "shovel", "hoe", "sickle", "knife", "saw", "chisel", "smithing_hammer",
            "quarry_hammer", "spear", "sword", "tongs", "ard", "bow", "fishing_rod", "broom", "bucket"};
    private static final String[] PARTS = {"head", "binding", "rod"};
    private static final int W = 392, H = 226;
    private static final int TYPE_X = 7, TYPE_Y = 25, LIST_X = 7, LIST_Y = 64, LIST_W = 162, LIST_H = 62, ROW_W = 78, ROW_H = 18;
    private static final int PART_X = 178, PART_Y = 26, PART_STEP = 22, OUT_X = 358, OUT_Y = 44, BOX_X = 178, BOX_Y = 94, BOX_W = 206, BOX_H = 125;
    /** The pocket's own colour laid over an item: what's left is a faint ghost. */
    private static final int GHOST = (0xA8 << 24) | (SLOT_FILL & 0xFFFFFF);
    /** The picked type, kept for the next time the station is opened. */
    private static String last;
    private String type;
    private Set<String> seen = Set.of();

    public ToolStationScreen(ToolStationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, W, H);
        inventoryLabelY = 133;
    }

    // ------------------------------------------------------------------ model

    /** A recipe's type key, from its result's registry path. */
    static String type(Recipe r) {
        String p = BuiltInRegistries.ITEM.getKey(r.result().get()).getPath();
        for (String t : new String[] {"pickaxe", "quarry_hammer", "smithing_hammer", "fishing_rod", "shovel", "sickle", "chisel", "spear", "sword",
                "tongs", "bucket", "broom", "saw", "axe", "hoe", "knife"})
            if (p.contains(t)) return t;
        if (p.contains("dagger")) return "knife";
        if (p.endsWith("_ard")) return "ard";
        return p.contains("bow") ? "bow" : p;
    }

    private List<Integer> recipes(String t) {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < ToolStationRecipes.ALL.size(); i++) {
            Recipe r = ToolStationRecipes.ALL.get(i);
            if (type(r).equals(t) && ToolStationRecipes.isUnlocked(minecraft.player, r)) out.add(i);
        }
        return out;
    }

    private List<String> types() {
        List<String> out = new ArrayList<>();
        for (String t : TYPES) if (!recipes(t).isEmpty()) out.add(t);
        return out;
    }

    /** The picked type: the selected recipe's, else the last one, else the first known. */
    private @Nullable String type() {
        List<String> known = types();
        if (type == null || !known.contains(type)) {
            int s = menu.selected();
            type = s >= 0 ? type(ToolStationRecipes.ALL.get(s)) : last;
            if (!known.contains(type)) type = known.isEmpty() ? null : known.get(0);
        }
        return type;
    }

    /** The recipe the pockets are about: the one being built, else the selected one, else the first that takes the placed head. */
    private @Nullable Recipe active() {
        ItemStack out = menu.getSlot(ToolStationMenu.RESULT).getItem(), head = menu.getSlot(ToolStationMenu.HEAD).getItem();
        if (!out.isEmpty()) for (Recipe r : ToolStationRecipes.ALL) if (out.is(r.result().get()) && head.is(r.head().get())) return r;
        int s = menu.selected();
        if (s >= 0 && (head.isEmpty() || head.is(ToolStationRecipes.ALL.get(s).head().get()))) return ToolStationRecipes.ALL.get(s);
        if (head.isEmpty()) return null;
        for (Recipe r : ToolStationRecipes.ALL) if (head.is(r.head().get()) && ToolStationRecipes.isUnlocked(minecraft.player, r)) return r;
        return null;
    }

    private boolean seen(Item item) {
        return seen.contains(BuiltInRegistries.ITEM.getKey(item).toString());
    }

    /** The parts that would do in binding (1) or rod (2) slot, the recipe's own first, only ones the player has seen. */
    private List<Item> options(Recipe r, int part) {
        Item own = part == 1 ? r.binding().get() : r.rod().get();
        List<Item> out = new ArrayList<>(List.of(own));
        if (r.dynamic()) {
            for (Item i : part == 1 ? r.bindings() : r.rods()) if (i != own && seen(i)) out.add(i);
        } else if (part == 1 && r.gluable() && seen(Kettle.HIDE_GLUE.get())) {
            out.add(Kettle.HIDE_GLUE.get());
        }
        return out;
    }

    // ------------------------------------------------------------------ input

    private int typeAt(double mx, double my, int count) {
        int c = (int) Math.floor((mx - leftPos - TYPE_X) / 18), r = (int) Math.floor((my - topPos - TYPE_Y) / 18);
        return c >= 0 && c < 9 && r >= 0 && r < 2 && r * 9 + c < count ? r * 9 + c : -1;
    }

    /** Row {@code i} of the recipe well: two columns, three rows. */
    private int[] rowPos(int i) {
        return new int[] {leftPos + LIST_X + 2 + i % 2 * (ROW_W + 2), topPos + LIST_Y + 2 + i / 2 * (ROW_H + 1)};
    }

    private int rowAt(double mx, double my, int count) {
        for (int i = 0; i < count; i++) {
            int[] p = rowPos(i);
            if (mx >= p[0] && mx < p[0] + ROW_W && my >= p[1] && my < p[1] + ROW_H) return i;
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            List<String> known = types();
            int t = typeAt(event.x(), event.y(), known.size());
            if (t >= 0) {
                type = last = known.get(t);
                click();
                return true;
            }
            String picked = type();
            List<Integer> list = picked == null ? List.of() : recipes(picked);
            int row = rowAt(event.x(), event.y(), Math.min(6, list.size()));
            if (row >= 0) {
                // shift-click: fill as many sets as the inventory has
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, list.get(row) + (event.hasShiftDown() ? RecipeStationMenu.FILL_ALL : 0));
                click();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void click() {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        seen = SeenItems.seen(minecraft.player);
        int x0 = leftPos, y0 = topPos;
        HideUi.panel(g, x0, y0, W, H);
        HideUi.seam(g, x0 + 7, y0 + 20, W - 14);
        HideUi.seam(g, x0 + 7, y0 + 129, LIST_W);
        // a stitched line down between the lists and the bench
        for (int k = 0; k < H - 30; k += 6) {
            g.fill(x0 + 172, y0 + 25 + k, x0 + 173, y0 + 25 + Math.min(H - 30, k + 3), HideUi.STITCH);
            g.fill(x0 + 173, y0 + 25 + k, x0 + 174, y0 + 25 + Math.min(H - 30, k + 3), HideUi.DARK);
        }
        for (Slot s : menu.slots) if (s.index > ToolStationMenu.RESULT) HideUi.slot(g, x0 + s.x - 1, y0 + s.y - 1);
        typeGrid(g, mouseX, mouseY);
        recipeWell(g, mouseX, mouseY);
        Recipe r = active();
        bench(g, r);
        ledger(g, r);
    }

    private void typeGrid(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        List<String> known = types();
        String picked = type();
        int hover = typeAt(mouseX, mouseY, known.size());
        for (int i = 0; i < 18; i++) {
            int x = leftPos + TYPE_X + i % 9 * 18, y = topPos + TYPE_Y + i / 9 * 18;
            HideUi.slot(g, x, y);
            if (i >= known.size()) continue;
            List<Integer> list = recipes(known.get(i));
            g.item(new ItemStack(ToolStationRecipes.ALL.get(list.get(list.size() - 1)).result().get()), x + 1, y + 1); // the best one known
            if (i == hover) g.fill(x + 1, y + 1, x + 17, y + 17, 0x40F0D8A8);
        }
        int at = known.indexOf(picked);
        if (at >= 0) {
            int x = leftPos + TYPE_X + at % 9 * 18, y = topPos + TYPE_Y + at / 9 * 18;
            g.outline(x - 1, y - 1, 20, 20, GOLD);
            g.fill(x + 7, y + 19, x + 11, y + 20, GOLD); // a tab hanging down to the list below
        }
    }

    private void recipeWell(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        HideUi.inset(g, leftPos + LIST_X, topPos + LIST_Y, LIST_W, LIST_H);
        String picked = type();
        if (picked == null) {
            wrapped(g, Component.translatable("gui.mechanical.tool_station.none_known"), leftPos + LIST_X + 6, topPos + LIST_Y + 18, LIST_W - 12, DIM);
            return;
        }
        // shortcut: six recipes fit (no type has more today); add scrolling when one gets a seventh
        List<Integer> list = recipes(picked);
        int hover = rowAt(mouseX, mouseY, Math.min(6, list.size()));
        for (int i = 0; i < Math.min(6, list.size()); i++) {
            Recipe r = ToolStationRecipes.ALL.get(list.get(i));
            int[] p = rowPos(i);
            boolean selected = list.get(i) == menu.selected(), has = ToolStationRecipes.hasParts(minecraft.player, r);
            HideUi.row(g, p[0], p[1], ROW_W, ROW_H, selected || i == hover, has || selected);
            g.item(new ItemStack(r.result().get()), p[0] + 1, p[1] + 1);
            if (!has) g.fill(p[0] + 1, p[1] + 1, p[0] + 17, p[1] + 17, 0x60160F09);
            g.text(font, cut(material(r, picked), ROW_W - 22), p[0] + 19, p[1] + 5, selected ? NAME : has ? TEXT : DIM, selected || has);
            if (selected) g.outline(p[0], p[1], ROW_W, ROW_H, GOLD);
        }
    }

    /** The recipe's name without its type ("Bronze" for Bronze Pickaxe in the pickaxes); the full name when it doesn't end in it. */
    private static Component material(Recipe r, String type) {
        String name = new ItemStack(r.result().get()).getHoverName().getString(), t = Component.translatable("gui.mechanical.tool_station.type." + type).getString();
        return Component.literal(name.endsWith(" " + t) ? name.substring(0, name.length() - t.length() - 1) : name);
    }

    /** The three part pockets with their ghosts and hints, the lashing into the result, and the result pocket. */
    private void bench(GuiGraphicsExtractor g, @Nullable Recipe r) {
        ItemStack out = menu.getSlot(ToolStationMenu.RESULT).getItem();
        for (int i = 0; i < 3; i++) {
            int x = leftPos + PART_X, y = topPos + PART_Y + i * PART_STEP;
            HideUi.slot(g, x, y);
            ItemStack placed = menu.getSlot(i).getItem();
            if (placed.isEmpty() && r != null) ghost(g, new ItemStack(r.parts().get(i)), x + 1, y + 1);
            g.text(font, Component.translatable("gui.mechanical.tool_station." + PARTS[i]), x + 22, y + 1, LABEL, false);
            Hint hint = hint(r, i, placed);
            if (hint != null) g.text(font, cut(hint.text, leftPos + OUT_X - 34 - (x + 22)), x + 22, y + 10, hint.colour, false);
        }
        // the lashing: a cord from each pocket gathered into the arrow, gold once the parts make a tool
        int lit = out.isEmpty() ? EDGE : GOLD, bx = leftPos + OUT_X - 30, top = topPos + PART_Y + 9, mid = topPos + OUT_Y + 13;
        for (int i = 0; i < 3; i++) {
            int y = top + i * PART_STEP;
            if (i != 1) g.fill(bx - 4, y, bx, y + 1, lit);
        }
        g.fill(bx, top, bx + 1, top + 2 * PART_STEP + 1, lit);
        HideUi.arrow(g, bx + 2, mid - 7);
        if (!out.isEmpty()) g.fill(bx + 2, mid, bx + 14, mid + 1, GOLD);
        HideUi.pocket(g, leftPos + OUT_X, topPos + OUT_Y, 26, 26);
        if (!out.isEmpty()) g.outline(leftPos + OUT_X - 1, topPos + OUT_Y - 1, 28, 28, GOLD);
        else if (r != null) ghost(g, new ItemStack(r.result().get()), leftPos + OUT_X + 5, topPos + OUT_Y + 5);
    }

    private void ghost(GuiGraphicsExtractor g, ItemStack stack, int x, int y) {
        g.item(stack, x, y);
        g.fill(x, y, x + 16, y + 16, GHOST);
    }

    private record Hint(Component text, int colour, List<Component> tip) {}

    /** What the pocket wants: the head's name, the parts that would do, or why the placed one doesn't. */
    private @Nullable Hint hint(@Nullable Recipe r, int part, ItemStack placed) {
        if (r == null) {
            if (part == 0 && !placed.isEmpty()) return new Hint(Component.translatable("gui.mechanical.tool_station.no_tool"), BAD, List.of());
            return null;
        }
        if (part == 0) return new Hint(new ItemStack(r.head().get()).getHoverName(), TEXT, List.of());
        if (!placed.isEmpty()) {
            ToolParts.Part p = part == 1 ? ToolParts.binding(placed.getItem()) : ToolParts.rod(placed.getItem());
            boolean ok = options(r, part).contains(placed.getItem()) || r.dynamic() && p != null && p.tier() <= ToolParts.tier(r.head().get());
            if (!ok && r.dynamic() && p != null)
                return new Hint(Component.translatable("gui.mechanical.tool_station.needs_tier",
                        Component.translatable("gui.mechanical.tool_station.tier." + p.tier())), BAD, List.of());
            if (!ok) return new Hint(Component.translatable("gui.mechanical.tool_station.wrong_" + PARTS[part]), BAD, List.of());
        }
        List<Item> options = options(r, part);
        List<Component> tip = new ArrayList<>();
        for (Item i : options) tip.add(new ItemStack(i).getHoverName().copy().withColor(TEXT));
        if (options.size() == 1) return new Hint(tip.get(0), DIM, List.of());
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < options.size(); i++) names.append(i > 0 ? ", " : "").append(tip.get(i).getString());
        return new Hint(Component.translatable("gui.mechanical.tool_station.any", names.toString()), DIM, tip);
    }

    // ------------------------------------------------------------------ the ledger

    /** The stats box: the tool's base stats, a row per part with what it does to each, and the result. */
    private void ledger(GuiGraphicsExtractor g, @Nullable Recipe r) {
        int bx = leftPos + BOX_X, by = topPos + BOX_Y;
        HideUi.inset(g, bx, by, BOX_W, BOX_H);
        int x = bx + 6, w = BOX_W - 12, y = by + 6;
        if (r == null) {
            List<FormattedCharSequence> pick = font.split(Component.translatable("gui.mechanical.tool_station.pick"), w),
                    how = font.split(Component.translatable("gui.mechanical.tool_station.how", 0.5), w);
            int ty = by + (BOX_H - (pick.size() + how.size()) * 10 - 9) / 2;
            for (FormattedCharSequence line : pick) {
                g.text(font, line, x, ty, TEXT, false);
                ty += 10;
            }
            HideUi.seam(g, bx + 3, ty + 3, BOX_W - 6);
            ty += 9;
            for (FormattedCharSequence line : how) {
                g.text(font, line, x, ty, DIM, false);
                ty += 10;
            }
            return;
        }
        ItemStack out = menu.getSlot(ToolStationMenu.RESULT).getItem();
        ItemStack plain = new ItemStack(r.result().get());
        g.text(font, cut((out.isEmpty() ? plain : out).getHoverName().copy().withStyle(ChatFormatting.BOLD), w), x, y, out.isEmpty() ? DIM : NAME, true);
        y += 13;
        int[] col = {x + w - 68, x + w - 40, x + w - 12}; // centres of the three number columns
        String[] heads = {"durability", "speed", "damage"};
        for (int c = 0; c < 3; c++) {
            Component h = Component.translatable("gui.mechanical.tool_station.col." + heads[c]);
            g.text(font, h, col[c] - font.width(h) / 2, y, DIM, false);
        }
        y += 11;
        ToolStats.Stats base = ToolStats.of(plain);
        g.text(font, Component.translatable("gui.mechanical.tool_station.base"), x, y, LABEL, false);
        numbers(g, col, y, new String[] {num(base.durability()), dec(base.speed()), dec(base.damage())}, new int[] {TEXT, TEXT, TEXT});
        y += 11;
        HideUi.seam(g, bx + 3, y, BOX_W - 6);
        y += 5;
        if (out.isEmpty()) {
            boolean full = !menu.getSlot(0).getItem().isEmpty() && !menu.getSlot(1).getItem().isEmpty() && !menu.getSlot(2).getItem().isEmpty();
            String why = full ? "no_fit" : r.dynamic() ? "lay_parts" : "fixed";
            wrapped(g, Component.translatable("gui.mechanical.tool_station." + why), x, y + 4, w, full ? BAD : DIM);
            return;
        }
        if (r.dynamic()) {
            ToolParts.Parts parts = out.get(ToolParts.TOOL_PARTS.get());
            if (parts != null) {
                y = share(g, col, x, y, ToolStats.binding(r.binding().get(), parts.binding()));
                y = share(g, col, x, y, ToolStats.rod(r.rod().get(), parts.rod()));
                if (parts.smithing() > 0) {
                    icon(g, Skill.SMITHING.icon(), x, y);
                    g.text(font, Component.translatable("gui.mechanical.tool_station.smithing", parts.smithing()), x + 18, y + 4, TEXT, false);
                    numbers(g, col, y + 4, new String[] {"+" + Math.round(ToolStats.smithing(parts.smithing()) * 100) + "%", "", ""}, new int[] {GOOD, 0, 0});
                    y += 16;
                }
            }
        } else {
            wrapped(g, Component.translatable("gui.mechanical.tool_station.fixed"), x, y + 1, w, DIM);
            y += 22;
        }
        if (Bronze.isPoor(out)) {
            icon(g, menu.getSlot(ToolStationMenu.HEAD).getItem(), x, y);
            g.text(font, Component.translatable("gui.mechanical.tool_station.poor"), x + 18, y + 4, BAD, false);
            numbers(g, col, y + 4, new String[] {"×" + factor(Bronze.POOR_DURABILITY), "", ""}, new int[] {BAD, 0, 0});
            y += 16;
        }
        y = Math.max(y, by + BOX_H - 18);
        HideUi.seam(g, bx + 3, y, BOX_W - 6);
        y += 5;
        ToolStats.Stats now = ToolStats.of(out);
        g.text(font, Component.translatable("gui.mechanical.tool_station.result"), x, y, NAME, true);
        numbers(g, col, y, new String[] {num(now.durability()), dec(now.speed()), dec(now.damage())},
                new int[] {change(now.durability(), base.durability()), change(now.speed(), base.speed()), change(now.damage(), base.damage())});
    }

    /** One part's row: its icon and name, and its factors against the recipe's own part ("–" where it changes nothing). */
    private int share(GuiGraphicsExtractor g, int[] col, int x, int y, ToolStats.Share s) {
        icon(g, new ItemStack(s.part()), x, y);
        g.text(font, cut(new ItemStack(s.part()).getHoverName(), col[0] - 16 - (x + 18)), x + 18, y + 4, s.isDefault() ? DIM : TEXT, false);
        numbers(g, col, y + 4, new String[] {times(s.durability()), times(s.speed()), plus(s.damage())},
                new int[] {change(s.durability(), 1), change(s.speed(), 1), change(s.damage(), 0)});
        return y + 16;
    }

    private void icon(GuiGraphicsExtractor g, ItemStack stack, int x, int y) {
        g.item(stack, x, y);
    }

    private void numbers(GuiGraphicsExtractor g, int[] col, int y, String[] values, int[] colours) {
        for (int c = 0; c < 3; c++) {
            if (values[c].isEmpty()) continue;
            boolean none = values[c].equals("–");
            g.text(font, values[c], col[c] - font.width(values[c]) / 2, y, none ? DIM : colours[c], false);
        }
    }

    private static int change(double now, double was) {
        return Math.abs(now - was) < 0.005 ? TEXT : now > was ? GOOD : BAD;
    }

    private static String num(int n) {
        return n <= 0 ? "–" : String.valueOf(n);
    }

    private static String dec(float v) {
        return v <= 0 ? "–" : String.format(Locale.ROOT, "%.1f", v);
    }

    private static String times(float f) {
        return Math.abs(f - 1) < 0.005 ? "–" : "×" + factor(f);
    }

    private static String plus(float d) {
        return Math.abs(d) < 0.005 ? "–" : (d > 0 ? "+" : "") + factor(d);
    }

    /** Up to two decimals, no trailing zeros: 1.35, 1.1, 0.7. */
    private static String factor(float f) {
        String s = String.format(Locale.ROOT, "%.2f", f);
        return s.replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, title.copy().withStyle(ChatFormatting.BOLD), 8, 8, NAME, true);
        String lv = String.valueOf(Skills.level(minecraft.player, Skill.SMITHING));
        int nx = W - 8 - font.width(lv);
        g.text(font, lv, nx, 8, NAME, true);
        Component label = Component.translatable(Skill.SMITHING.langKey());
        g.text(font, label, nx - 4 - font.width(label), 8, DIM, false);
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, LABEL, false);
    }

    private void wrapped(GuiGraphicsExtractor g, Component text, int x, int y, int w, int colour) {
        for (FormattedCharSequence line : font.split(text, w)) {
            g.text(font, line, x, y, colour, false);
            y += 10;
        }
    }

    /** One line of {@code text} no wider than {@code w}, cut with an ellipsis. */
    private FormattedCharSequence cut(Component text, int w) {
        if (font.width(text) <= w) return text.getVisualOrderText();
        return net.minecraft.locale.Language.getInstance().getVisualOrder(
                FormattedText.composite(font.substrByWidth(text, w - font.width("...")), FormattedText.of("...")));
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
        if (hoveredSlot != null && hoveredSlot.hasItem()) return;
        List<String> known = types();
        int t = typeAt(mouseX, mouseY, known.size());
        if (t >= 0) {
            g.setComponentTooltipForNextFrame(font, List.of(Component.translatable("gui.mechanical.tool_station.type." + known.get(t)),
                    Component.translatable("gui.mechanical.tool_station.known", recipes(known.get(t)).size()).withColor(DIM)), mouseX, mouseY);
            return;
        }
        String picked = type();
        List<Integer> list = picked == null ? List.of() : recipes(picked);
        int row = rowAt(mouseX, mouseY, Math.min(6, list.size()));
        if (row >= 0) {
            Recipe r = ToolStationRecipes.ALL.get(list.get(row));
            List<Component> lines = new ArrayList<>();
            lines.add(new ItemStack(r.result().get()).getHoverName());
            List<Item> parts = r.parts(minecraft.player.getInventory());
            for (int i = 0; i < 3; i++) {
                boolean has = minecraft.player.getInventory().countItem(parts.get(i)) > 0;
                lines.add(Component.translatable("gui.mechanical.tool_station." + PARTS[i]).append(": ").append(new ItemStack(parts.get(i)).getHoverName())
                        .withStyle(has ? ChatFormatting.GREEN : ChatFormatting.RED));
            }
            g.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
            return;
        }
        // the hint line of a pocket: every part that would do
        Recipe r = active();
        for (int i = 1; i < 3 && r != null; i++) {
            int x = leftPos + PART_X + 22, y = topPos + PART_Y + i * PART_STEP + 9;
            if (mouseX < x || mouseX >= leftPos + OUT_X - 30 || mouseY < y || mouseY >= y + 10) continue;
            Hint hint = hint(r, i, menu.getSlot(i).getItem());
            if (hint == null || hint.tip.isEmpty()) return;
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("gui.mechanical.tool_station.would_do." + PARTS[i]));
            lines.addAll(hint.tip);
            g.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
        }
    }
}
