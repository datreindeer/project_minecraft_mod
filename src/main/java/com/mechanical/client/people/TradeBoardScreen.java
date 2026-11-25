package com.mechanical.client.people;

import java.util.ArrayList;
import java.util.List;

import com.mechanical.Coins;
import com.mechanical.client.ui.HideUi;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.component.DataComponents;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * The mod's trade screen on the hide panel, for caravans and tribes ({@link CaravanTradeScreen}) and villagers
 * ({@link VillagerTradeScreen}): the trader's portrait and name at the top, two columns (what they sell, what they buy),
 * each row the goods in a pocket, how many are left and the price (goods as small icons, coins as coins, a changed
 * price with the old one struck through); a click trades once, the row flashes and the bottom line says what happened.
 * Rows that can't trade now are dull, the reason in the tooltip. Subclasses give the rows and carry out the click.
 */
public abstract class TradeBoardScreen extends Screen {
    static final int W = 400, PAD = 10, ROW = 24, COL_GAP = 8, HEADER = 62, COL_HEAD = 16, FOOTER = 30, PORTRAIT_W = 40, PORTRAIT_H = 48;
    private static final int ICON = 12, FLASH_MS = 1600;

    /**
     * One row. {@code price}: goods shown as small icons. {@code usual}: when the price has changed, the normal count of
     * what changed (a sale's first price icon, a purchase's goods), else 0. {@code coins}: a price in copper drawn as coins, -1 for none. For a sale the price is what the
     * player pays for {@code goods}, else what the trader pays for them. {@code blocked}: why not now, or null.
     */
    protected record Row(ItemStack goods, List<ItemStack> price, int usual, int coins, boolean sale, int stock, Component status,
            @Nullable Component blocked) {}

    private int left, top, rows, scrollSale, scrollBuy;
    /** The clicked row and its stock then: a lower stock means the trade happened. */
    private int pending = -1, pendingStock, flashRow = -1;
    private long flashSince;
    private Component flashText = Component.empty();

    protected TradeBoardScreen(Component title) {
        super(title);
    }

    // ---- what the subclass gives

    protected abstract List<Row> rows();

    /** Under the name: the trader's kind or profession. */
    protected abstract Component badge();

    /** Right of the header and under the badge: a purse, a level bar. */
    protected void extractHeaderExtra(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY) {}

    /** Left of the footer. */
    protected abstract void extractWallet(GuiGraphicsExtractor g, int x, int y);

    protected abstract Component hint();

    /** The trader, for the portrait; null if not known. */
    protected abstract @Nullable Entity trader();

    /** Send the trade of row {@code index}; {@code all}: as many times as possible, where the trader allows it. */
    protected abstract void send(int index, boolean all);

    // ---- layout

    private int colWidth() {
        return (W - PAD * 2 - COL_GAP) / 2;
    }

    private int panelHeight() {
        return HEADER + COL_HEAD + rows * ROW + FOOTER;
    }

    /** How many rows the longer column shows: as many as it has, between 4 and what fits the window. */
    private void layout() {
        List<Row> all = rows();
        int sales = (int) all.stream().filter(Row::sale).count(), longest = Math.max(sales, all.size() - sales);
        int fits = Math.clamp((height - HEADER - COL_HEAD - FOOTER - 12) / ROW, 3, 8);
        rows = Math.clamp(longest, Math.min(4, fits), fits);
        left = (width - W) / 2;
        top = (height - panelHeight()) / 2;
        scrollSale = Math.clamp(scrollSale, 0, Math.max(0, sales - rows));
        scrollBuy = Math.clamp(scrollBuy, 0, Math.max(0, all.size() - sales - rows));
    }

    private List<Integer> column(List<Row> all, boolean sale) {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < all.size(); i++) if (all.get(i).sale() == sale) out.add(i);
        return out;
    }

    private int columnX(boolean sale) {
        return left + PAD + (sale ? 0 : colWidth() + COL_GAP);
    }

    private int listTop() {
        return top + HEADER + COL_HEAD;
    }

    /** Where row {@code index} is drawn, as {x, y}, or null when scrolled out of sight. */
    public int @Nullable [] rowAt(int index) {
        layout();
        List<Row> all = rows();
        if (index < 0 || index >= all.size()) return null;
        boolean sale = all.get(index).sale();
        int line = column(all, sale).indexOf(index) - (sale ? scrollSale : scrollBuy);
        return line < 0 || line >= rows ? null : new int[] {columnX(sale), listTop() + line * ROW};
    }

    /** The row index under the mouse, or -1. */
    private int dealAt(List<Row> all, double mx, double my) {
        if (my < listTop() || my >= listTop() + rows * ROW) return -1;
        int line = (int) ((my - listTop()) / ROW);
        for (boolean sale : new boolean[] {true, false}) {
            int x = columnX(sale);
            if (mx < x || mx >= x + colWidth()) continue;
            List<Integer> col = column(all, sale);
            int at = line + (sale ? scrollSale : scrollBuy);
            return at < col.size() ? col.get(at) : -1;
        }
        return -1;
    }

    // ---- drawing

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        layout();
        int h = panelHeight();
        HideUi.panel(g, left, top, W, h);
        HideUi.seam(g, left + 7, top + HEADER - 4, W - 14);
        HideUi.seam(g, left + 7, top + h - FOOTER + 2, W - 14);
        HideUi.inset(g, left + PAD, top + 7, PORTRAIT_W, PORTRAIT_H);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        layout();
        List<Row> all = rows();
        checkFlash(all);

        // header: portrait, name, badge
        if (trader() instanceof LivingEntity living && living.isAlive())
            portrait(g, left + PAD + 1, top + 8, left + PAD + PORTRAIT_W - 1, top + 7 + PORTRAIT_H - 1, living);
        else g.item(new ItemStack(Coins.GOLD_COIN.get()), left + PAD + 12, top + 23);
        int hx = left + PAD + PORTRAIT_W + 8, hw = W - (hx - left) - PAD;
        Component badge = badge();
        int bw = font.width(badge) + 10;
        HideUi.row(g, hx, top + 21, bw, 14, false, true);
        g.text(font, badge, hx + 5, top + 24, HideUi.GOLD);
        extractHeaderExtra(g, hx, top + 39, hw, mouseX, mouseY);
        // the name tag already says "of the X tribe": one line, cut to the header's room
        g.text(font, font.plainSubstrByWidth(title(), headerNameRoom(hw)), hx, top + 10, HideUi.NAME);

        // columns
        int hovered = dealAt(all, mouseX, mouseY);
        for (boolean sale : new boolean[] {true, false}) {
            int x = columnX(sale);
            Component heading = Component.translatable(sale ? "gui.mechanical.caravan.for_sale" : "gui.mechanical.caravan.they_buy");
            g.text(font, heading, x + 3, top + HEADER + 3, HideUi.NAME);
            g.fill(x + 3, top + HEADER + 13, x + 3 + font.width(heading), top + HEADER + 14, HideUi.EDGE);
            drawColumn(g, all, column(all, sale), sale ? scrollSale : scrollBuy, x, hovered);
        }

        // footer: your coins, and what just happened or how it works
        int fy = top + panelHeight() - FOOTER + 10;
        extractWallet(g, left + PAD + 2, fy);
        long since = Util.getMillis() - flashSince;
        Component right = since < FLASH_MS ? flashText : hint();
        g.text(font, right, left + W - PAD - 2 - font.width(right), fy + 3, since < FLASH_MS ? HideUi.GOOD : HideUi.DIM);

        if (hovered >= 0) tooltip(g, all.get(hovered), mouseX, mouseY);
    }

    /** Head and shoulders, turned a little to the side, without the name tag (vanilla's inventory figure, held still). */
    private void portrait(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, LivingEntity entity) {
        EntityRenderer<? super LivingEntity, ?> renderer = minecraft.getEntityRenderDispatcher().getRenderer(entity);
        EntityRenderState state = renderer.createRenderState(entity, 1.0F);
        state.shadowPieces.clear();
        state.outlineColor = 0;
        state.nameTag = null;
        if (state instanceof LivingEntityRenderState living) {
            living.bodyRot = 200.0F;
            living.yRot = 6.0F;
            living.xRot = 0.0F;
            living.boundingBoxWidth /= living.scale;
            living.boundingBoxHeight /= living.scale;
            living.scale = 1.0F;
        }
        // the picture's middle is at 0.8 of the height: head, hat and shoulders fit
        g.entity(state, 30, new Vector3f(0.0F, state.boundingBoxHeight * 0.8F, 0.0F), new Quaternionf().rotateZ((float) Math.PI),
                new Quaternionf(), x0, y0, x1, y1);
    }

    /** Room for the trader's name in the header; a subclass with something at the top right leaves less. */
    protected int headerNameRoom(int headerWidth) {
        return headerWidth;
    }

    private String title() {
        return getTitle().getString();
    }

    private void drawColumn(GuiGraphicsExtractor g, List<Row> all, List<Integer> col, int scroll, int x, int hovered) {
        int y = listTop(), cw = colWidth();
        if (col.isEmpty()) g.text(font, Component.translatable("gui.mechanical.caravan.nothing"), x + 5, y + 8, HideUi.DIM);
        for (int r = 0; r < rows && r + scroll < col.size(); r++, y += ROW) {
            int i = col.get(r + scroll);
            Row row = all.get(i);
            boolean ok = row.blocked() == null;
            HideUi.row(g, x, y, cw, ROW - 2, i == hovered && ok, ok);
            HideUi.slot(g, x + 2, y + 2);
            g.item(row.goods(), x + 3, y + 3);
            g.itemDecorations(font, row.goods(), x + 3, y + 3);
            int pw = priceWidth(row);
            int room = cw - 24 - pw - 8;
            Component name = shortName(row.goods());
            FormattedCharSequence cut = font.width(name) <= room ? name.getVisualOrderText() : Language.getInstance()
                    .getVisualOrder(FormattedText.composite(font.substrByWidth(name, room - font.width("...")), FormattedText.of("...")));
            g.text(font, cut, x + 24, y + 3, ok ? HideUi.TEXT : HideUi.DIM);
            small(g, row.status(), x + 24, y + 13, row.stock() > 0 ? (row.sale() ? HideUi.GOOD : HideUi.DIM) : HideUi.BAD);
            if (row.stock() > 0) drawPrice(g, row, x + cw - 4 - pw, y + 5, ok ? HideUi.GOLD : HideUi.DIM);
            else {
                // sold out: a red cross over the price
                for (int k = 0; k < 8; k++) {
                    int cx = x + cw - 14 + k;
                    g.fill(cx, y + 7 + k, cx + 2, y + 8 + k, HideUi.BAD);
                    g.fill(cx, y + 14 - k, cx + 2, y + 15 - k, HideUi.BAD);
                }
            }
            long since = Util.getMillis() - flashSince;
            if (i == flashRow && since < FLASH_MS) {
                int alpha = (int) (150 * (1 - since / (float) FLASH_MS));
                g.fill(x + 1, y + 1, x + cw - 1, y + ROW - 3, alpha << 24 | (HideUi.GOLD & 0xFFFFFF));
            }
        }
        // a scroll bar when the column is longer than the screen
        if (col.size() > rows) {
            int track = rows * ROW - 2, bar = Math.max(8, track * rows / col.size());
            int by = listTop() + (track - bar) * scroll / (col.size() - rows);
            int bx = x + cw + 1;
            g.fill(bx, listTop(), bx + 3, listTop() + track, HideUi.DARK);
            g.fill(bx, by, bx + 3, by + bar, HideUi.EDGE);
        }
    }

    /** An enchanted book is named after its first enchantment; everything else by its own name. */
    private Component shortName(ItemStack goods) {
        ItemEnchantments stored = goods.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (var e : stored.entrySet()) return Component.literal(Enchantment.getFullname(e.getKey(), e.getIntValue()).getString());
        return goods.getHoverName();
    }

    private void small(GuiGraphicsExtractor g, Component text, int x, int y, int colour) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(0.75F, 0.75F);
        g.text(font, text, 0, 0, colour);
        g.pose().popMatrix();
    }

    private int iconWidth(int count) {
        return ICON + 1 + font.width(String.valueOf(count));
    }

    private int priceWidth(Row row) {
        int w = 0, n = 0;
        for (ItemStack c : row.price()) {
            w += (n++ > 0 ? 8 : 0) + iconWidth(c.getCount());
        }
        if (row.sale() && row.usual() > 0) w += font.width(String.valueOf(row.usual())) + 3;
        if (row.coins() >= 0) w += (w > 0 ? 4 : 0) + coinsWidth(row.coins());
        return w;
    }

    private void drawPrice(GuiGraphicsExtractor g, Row row, int x, int y, int colour) {
        int n = 0;
        for (ItemStack c : row.price()) {
            if (n > 0) {
                g.text(font, "+", x + 1, y + 2, HideUi.DIM);
                x += 8;
            }
            if (n++ == 0 && row.sale() && row.usual() > 0) {
                // the usual price struck through, the price now beside it: green when cheaper, red when dearer
                String old = String.valueOf(row.usual());
                g.text(font, old, x, y + 2, HideUi.DIM);
                g.fill(x - 1, y + 5, x + font.width(old) + 1, y + 6, HideUi.BAD);
                x += font.width(old) + 3;
                colour = c.getCount() < row.usual() ? HideUi.GOOD : HideUi.BAD;
                icon(g, c, x, y, colour);
                colour = row.blocked() == null ? HideUi.GOLD : HideUi.DIM;
            } else {
                icon(g, c, x, y, colour);
            }
            x += iconWidth(c.getCount());
        }
        if (row.coins() >= 0) coins(g, row.coins(), x + (n > 0 ? 4 : 0), y, colour);
    }

    private void icon(GuiGraphicsExtractor g, ItemStack stack, int x, int y, int colour) {
        icon(g, stack, stack.getCount(), x, y, colour);
    }

    private void icon(GuiGraphicsExtractor g, ItemStack stack, int count, int x, int y, int colour) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(ICON / 16.0F, ICON / 16.0F);
        g.item(stack, 0, 0);
        g.pose().popMatrix();
        g.text(font, String.valueOf(count), x + ICON + 1, y + 2, colour);
    }

    private static List<ItemStack> coinStacks(int copper) {
        int[] split = Coins.split(copper);
        net.minecraft.world.item.Item[] items = {Coins.GOLD_COIN.get(), Coins.SILVER_COIN.get(), Coins.COPPER_COIN.get()};
        List<ItemStack> out = new ArrayList<>();
        for (int c = 0; c < 3; c++) if (split[c] > 0) out.add(new ItemStack(items[c], split[c]));
        return out;
    }

    /** Width of {@code copper} drawn as coins ({@link #coins}). */
    protected int coinsWidth(int copper) {
        if (copper <= 0) return iconWidth(0);
        int w = -4;
        for (ItemStack c : coinStacks(copper)) w += iconWidth(c.getCount()) + 4;
        return w;
    }

    /** {@code copper} as small gold, silver and copper coin icons, each with its count. */
    protected void coins(GuiGraphicsExtractor g, int copper, int x, int y, int colour) {
        if (copper <= 0) {
            icon(g, new ItemStack(Coins.COPPER_COIN.get()), 0, x, y, colour);
            return;
        }
        for (ItemStack c : coinStacks(copper)) {
            icon(g, c, x, y, colour);
            x += iconWidth(c.getCount()) + 4;
        }
    }

    private void tooltip(GuiGraphicsExtractor g, Row row, int mouseX, int mouseY) {
        List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(minecraft, row.goods()));
        List<String> parts = new ArrayList<>();
        for (ItemStack c : row.price()) parts.add(c.getCount() + "× " + c.getHoverName().getString());
        if (row.coins() >= 0) {
            for (ItemStack c : coinStacks(row.coins())) parts.add(c.getCount() + "× " + c.getHoverName().getString());
        }
        lines.add(Component.empty());
        lines.add(Component.translatable(row.sale() ? "gui.mechanical.trade.price" : "gui.mechanical.trade.pays", String.join(" + ", parts))
                .withStyle(ChatFormatting.GOLD));
        if (row.usual() > 0) {
            boolean cheaper = (row.sale() ? row.price().get(0) : row.goods()).getCount() < row.usual();
            lines.add(Component.translatable(cheaper ? "gui.mechanical.trade.cheaper" : "gui.mechanical.trade.dearer", row.usual())
                    .withStyle(cheaper ? ChatFormatting.GREEN : ChatFormatting.RED));
        }
        lines.add(row.blocked() != null ? row.blocked().copy().withStyle(ChatFormatting.RED) : clickLine(row).copy().withStyle(ChatFormatting.GREEN));
        g.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY, row.goods());
    }

    protected Component clickLine(Row row) {
        return Component.translatable(row.sale() ? "gui.mechanical.caravan.click_buy" : "gui.mechanical.caravan.click_sell");
    }

    // ---- feedback

    private void checkFlash(List<Row> all) {
        if (pending < 0 || pending >= all.size() || all.get(pending).stock() >= pendingStock) return;
        Row row = all.get(pending);
        flashRow = pending;
        flashSince = Util.getMillis();
        flashText = Component.translatable(row.sale() ? "gui.mechanical.trade.bought" : "gui.mechanical.trade.sold", shortName(row.goods()));
        pending = -1;
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ITEM_PICKUP, 1.2F));
    }

    // ---- input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        List<Row> all = rows();
        int i = dealAt(all, event.x(), event.y());
        if (i >= 0 && event.button() == 0) {
            if (all.get(i).blocked() == null) {
                pending = i;
                pendingStock = all.get(i).stock();
                send(i, event.hasShiftDown());
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        int step = (int) -Math.signum(scrollY);
        if (x < left + W / 2.0) scrollSale += step;
        else scrollBuy += step;
        layout();
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
