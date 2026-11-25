package com.mechanical.client.people;

import java.util.ArrayList;
import java.util.List;

import com.mechanical.Coins;
import com.mechanical.people.CaravanTrade;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * A Trader Caravan's trade screen (people.CaravanTrade, docs/traders-plan.md D), on the hide-coloured panel of the tribe
 * dialog: the leader's name, his tribe and what the caravan deals in at the top, the coins in his purse at the top right;
 * two columns, what they sell and what they buy, each row the goods, how many are left (or how many more they want, and how
 * many you carry) and the price in gold, silver and copper coins; your coins at the bottom. A click on a row trades once
 * (the server checks and answers with the new state, {@link #update}); rows that can't trade now are dimmed, the reason in
 * the tooltip. Scrolls when a column is longer than the screen.
 * <p>
 * Tribes (elders, the Miner, envoys) use it too: no purse (purse -1, hidden), and a deal may ask for goods in barter
 * ({@code Deal.cost}), drawn as small item icons before the coins.
 */
public class CaravanTradeScreen extends Screen {
    private static final int W = 372, PAD = 10, ROW = 22, COL_GAP = 8, HEADER = 38, COL_HEAD = 14, FOOTER = 26;
    private static final int PANEL = 0xF0302418, BORDER = 0xFF8A6A44, BORDER_DARK = 0xFF1E160E, NAME = 0xFFF0D8A8, TEXT = 0xFFE8E0D0,
            DIM = 0xFF9A8C78, ROW_BG = 0x40000000, ROW_HOVER = 0x40F0D8A8, SLOT = 0xFF1E160E, SLOT_EDGE = 0xFF5A4630, GOLD = 0xFFE8C060,
            GOOD = 0xFF9ED07A, BAD = 0xFFE08060, KIND_BG = 0xFF4A3420;
    private CaravanTrade.Open open;
    private final List<Integer> sales = new ArrayList<>(), purchases = new ArrayList<>();
    private int left, top, rows, scrollSale, scrollBuy;

    public CaravanTradeScreen(CaravanTrade.Open open) {
        super(open.name());
        update(open);
    }

    public int entity() {
        return open.entity();
    }

    /** New state from the server (after a trade): same screen, new numbers. */
    public void update(CaravanTrade.Open open) {
        this.open = open;
        sales.clear();
        purchases.clear();
        for (int i = 0; i < open.deals().size(); i++) (open.deals().get(i).sale() ? sales : purchases).add(i);
    }

    private int colWidth() {
        return (W - PAD * 2 - COL_GAP) / 2;
    }

    private int height() {
        return HEADER + COL_HEAD + rows * ROW + FOOTER + PAD;
    }

    @Override
    protected void init() {
        rows = Math.clamp((this.height - HEADER - COL_HEAD - FOOTER - PAD - 16) / ROW, 3, 8);
        left = (width - W) / 2;
        top = (this.height - height()) / 2;
    }

    // ---- what a deal can do now

    /** Why deal {@code i} can't happen now, or null if it can. */
    private Component blocked(int i) {
        CaravanTrade.Deal d = open.deals().get(i);
        if (d.stock() <= 0) return Component.translatable(d.sale() ? "gui.mechanical.caravan.sold_out" : "gui.mechanical.caravan.enough");
        boolean creative = minecraft != null && minecraft.player != null && minecraft.player.isCreative();
        if (d.sale() && open.have().get(i) < 1) return Component.translatable("gui.mechanical.caravan.no_goods");
        if (d.sale()) return open.coins() >= d.price() || creative ? null : Component.translatable("gui.mechanical.caravan.too_dear");
        if (open.purse() >= 0 && open.purse() < d.price()) return Component.translatable("gui.mechanical.caravan.purse_empty");
        return open.have().get(i) >= d.goods().getCount() ? null : Component.translatable("gui.mechanical.caravan.none_to_sell");
    }

    private int listTop() {
        return top + HEADER + COL_HEAD;
    }

    /** The deal index under the mouse, or -1. */
    private int dealAt(double mx, double my) {
        if (my < listTop() || my >= listTop() + rows * ROW) return -1;
        int row = (int) ((my - listTop()) / ROW);
        int cw = colWidth();
        if (mx >= left + PAD && mx < left + PAD + cw) return row + scrollSale < sales.size() ? sales.get(row + scrollSale) : -1;
        int bx = left + PAD + cw + COL_GAP;
        if (mx >= bx && mx < bx + cw) return row + scrollBuy < purchases.size() ? purchases.get(row + scrollBuy) : -1;
        return -1;
    }

    // ---- drawing

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        int h = height();
        g.fill(left - 2, top - 2, left + W + 2, top + h + 2, BORDER_DARK);
        g.fill(left - 1, top - 1, left + W + 1, top + h + 1, BORDER);
        g.fill(left, top, left + W, top + h, PANEL);
        // stitched seam under the header and over the footer, like the edge of a hide
        for (int x = left + 6; x < left + W - 6; x += 4) {
            g.fill(x, top + HEADER - 4, x + 2, top + HEADER - 3, BORDER);
            g.fill(x, top + h - FOOTER + 2, x + 2, top + h - FOOTER + 3, BORDER);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        // header: who, of which tribe, and what they deal in
        // the name tag already says "of the X tribe": the name on one line, cut to leave room for the purse
        int nameRoom = open.purse() < 0 ? W - PAD * 2
                : W - PAD * 3 - Math.max(font.width(Component.translatable("gui.mechanical.caravan.purse")), coinsWidth(open.purse()));
        g.text(font, font.plainSubstrByWidth(open.name().getString(), nameRoom), left + PAD, top + PAD, NAME);
        Component kind = open.big() ? Component.translatable("gui.mechanical.caravan.big", open.kind()) : open.kind();
        int kw = font.width(kind) + 10;
        g.fill(left + PAD, top + PAD + 13, left + PAD + kw, top + PAD + 25, KIND_BG);
        g.outline(left + PAD, top + PAD + 13, kw, 12, BORDER);
        g.text(font, kind, left + PAD + 5, top + PAD + 15, GOLD);
        // the purse, top right (a tribe has none: it always pays)
        if (open.purse() >= 0) {
            Component purse = Component.translatable("gui.mechanical.caravan.purse");
            int pw = coinsWidth(open.purse());
            g.text(font, purse, left + W - PAD - font.width(purse), top + PAD, DIM);
            coins(g, open.purse(), left + W - PAD - pw, top + PAD + 12, open.purse() > 0 ? TEXT : BAD);
        }

        int cw = colWidth(), sx = left + PAD, bx = sx + cw + COL_GAP;
        g.text(font, Component.translatable("gui.mechanical.caravan.for_sale"), sx + 2, top + HEADER + 2, NAME);
        g.text(font, Component.translatable("gui.mechanical.caravan.they_buy"), bx + 2, top + HEADER + 2, NAME);
        int hovered = dealAt(mouseX, mouseY);
        column(g, sales, scrollSale, sx, cw, hovered);
        column(g, purchases, scrollBuy, bx, cw, hovered);

        // footer: your coins, and how it works
        int fy = top + height() - FOOTER + 8;
        Component yours = Component.translatable("gui.mechanical.caravan.your_coins");
        g.text(font, yours, left + PAD, fy + 4, DIM);
        coins(g, open.coins(), left + PAD + font.width(yours) + 6, fy, TEXT);
        Component hint = Component.translatable(open.purse() < 0 ? "gui.mechanical.caravan.barter_hint" : "gui.mechanical.caravan.hint");
        g.text(font, hint, left + W - PAD - font.width(hint), fy + 4, DIM);

        if (hovered >= 0) tooltip(g, hovered, mouseX, mouseY);
    }

    private void column(GuiGraphicsExtractor g, List<Integer> deals, int scroll, int x, int cw, int hovered) {
        int y = listTop();
        if (deals.isEmpty()) g.text(font, Component.translatable("gui.mechanical.caravan.nothing"), x + 4, y + 7, DIM);
        for (int r = 0; r < rows && r + scroll < deals.size(); r++, y += ROW) {
            int i = deals.get(r + scroll);
            CaravanTrade.Deal d = open.deals().get(i);
            boolean ok = blocked(i) == null;
            g.fill(x, y, x + cw, y + ROW - 2, i == hovered && ok ? ROW_HOVER : ROW_BG);
            // the goods in an inset slot, with the count
            g.fill(x + 2, y + 1, x + 20, y + 19, SLOT_EDGE);
            g.fill(x + 3, y + 2, x + 19, y + 18, SLOT);
            g.item(d.goods(), x + 3, y + 2);
            g.itemDecorations(font, d.goods(), x + 3, y + 2);
            int pw = priceWidth(d);
            String name = font.plainSubstrByWidth(d.goods().getHoverName().getString(), cw - 24 - pw - 4);
            g.text(font, name, x + 23, y + 2, ok ? TEXT : DIM);
            Component count = d.sale() ? Component.translatable("gui.mechanical.caravan.left", d.stock())
                    : Component.translatable("gui.mechanical.caravan.wants", d.stock(), open.have().get(i));
            g.pose().pushMatrix();
            g.pose().translate(x + 23, y + 12);
            g.pose().scale(0.75F, 0.75F);
            g.text(font, count, 0, 0, d.stock() > 0 ? (d.sale() ? GOOD : DIM) : BAD);
            g.pose().popMatrix();
            price(g, d, x + cw - 3 - pw, y + 3, ok ? GOLD : DIM);
            if (!ok) g.fill(x, y, x + cw, y + ROW - 2, 0x50000000);
        }
        // a scroll bar when the column is longer than the screen
        if (deals.size() > rows) {
            int track = rows * ROW - 2, bar = Math.max(8, track * rows / deals.size());
            int by = listTop() + (track - bar) * scroll / (deals.size() - rows);
            g.fill(x + cw + 1, listTop(), x + cw + 3, listTop() + track, ROW_BG);
            g.fill(x + cw + 1, by, x + cw + 3, by + bar, BORDER);
        }
    }

    private static final int COIN = 12;

    /** Width of a deal's price: the barter goods ({@link #price}), then the coins if any. */
    private int priceWidth(CaravanTrade.Deal d) {
        int w = 0;
        for (ItemStack c : d.cost()) w += COIN + 1 + font.width(String.valueOf(c.getCount())) + 4;
        return d.cost().isEmpty() || d.price() > 0 ? w + coinsWidth(d.price()) : w - 4;
    }

    /** A deal's price: each barter good as a small icon with its count, then the coins (left out when it is barter only). */
    private void price(GuiGraphicsExtractor g, CaravanTrade.Deal d, int x, int y, int colour) {
        for (ItemStack c : d.cost()) {
            g.pose().pushMatrix();
            g.pose().translate(x, y);
            g.pose().scale(COIN / 16.0F, COIN / 16.0F);
            g.item(c, 0, 0);
            g.pose().popMatrix();
            String n = String.valueOf(c.getCount());
            g.text(font, n, x + COIN + 1, y + 2, colour);
            x += COIN + 1 + font.width(n) + 4;
        }
        if (d.cost().isEmpty() || d.price() > 0) coins(g, d.price(), x, y, colour);
    }

    private Item[] coinItems() {
        return new Item[] {Coins.GOLD_COIN.get(), Coins.SILVER_COIN.get(), Coins.COPPER_COIN.get()};
    }

    /** Width of {@code copper} drawn as coins ({@link #coins}). */
    private int coinsWidth(int copper) {
        int[] split = Coins.split(copper);
        int w = 0;
        for (int c = 0; c < 3; c++) if (split[c] > 0 || copper == 0 && c == 2) w += COIN + 1 + font.width(String.valueOf(split[c])) + 4;
        return Math.max(0, w - 4);
    }

    /** {@code copper} as small gold, silver and copper coin icons, each with its count. */
    private void coins(GuiGraphicsExtractor g, int copper, int x, int y, int colour) {
        int[] split = Coins.split(copper);
        Item[] items = coinItems();
        for (int c = 0; c < 3; c++) {
            if (split[c] == 0 && !(copper == 0 && c == 2)) continue;
            g.pose().pushMatrix();
            g.pose().translate(x, y);
            g.pose().scale(COIN / 16.0F, COIN / 16.0F);
            g.item(new ItemStack(items[c]), 0, 0);
            g.pose().popMatrix();
            String n = String.valueOf(split[c]);
            g.text(font, n, x + COIN + 1, y + 2, colour);
            x += COIN + 1 + font.width(n) + 4;
        }
    }

    private void tooltip(GuiGraphicsExtractor g, int i, int mouseX, int mouseY) {
        CaravanTrade.Deal d = open.deals().get(i);
        List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(minecraft, d.goods()));
        int[] split = Coins.split(d.price());
        for (ItemStack c : d.cost())
            lines.add(Component.translatable("gui.mechanical.caravan.barter", c.getCount() + " " + c.getHoverName().getString()).withStyle(ChatFormatting.GOLD));
        if (d.cost().isEmpty() || d.price() > 0)
            lines.add(Component.translatable(d.sale() ? "gui.mechanical.caravan.buy_for" : "gui.mechanical.caravan.sell_for", split[0], split[1], split[2])
                    .withStyle(ChatFormatting.GOLD));
        Component why = blocked(i);
        lines.add(why != null ? why.copy().withStyle(ChatFormatting.RED)
                : Component.translatable(d.sale() ? "gui.mechanical.caravan.click_buy" : "gui.mechanical.caravan.click_sell").withStyle(ChatFormatting.GREEN));
        g.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
    }

    // ---- input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int i = dealAt(event.x(), event.y());
        if (i >= 0 && event.button() == 0) {
            if (blocked(i) == null) {
                ClientPacketDistributor.sendToServer(new CaravanTrade.Click(open.entity(), i));
                minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        int step = (int) -Math.signum(scrollY);
        if (x < left + PAD + colWidth() + COL_GAP / 2.0) scrollSale = Math.clamp(scrollSale + step, 0, Math.max(0, sales.size() - rows));
        else scrollBuy = Math.clamp(scrollBuy + step, 0, Math.max(0, purchases.size() - rows));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
