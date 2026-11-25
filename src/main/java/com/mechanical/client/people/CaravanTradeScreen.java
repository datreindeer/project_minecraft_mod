package com.mechanical.client.people;

import java.util.ArrayList;
import java.util.List;

import com.mechanical.client.ui.HideUi;
import com.mechanical.people.CaravanTrade;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

/**
 * A Trader Caravan's trade screen (people.CaravanTrade, docs/traders-plan.md D) on the {@link TradeBoardScreen}: the
 * leader, what the caravan deals in and the coins in his purse at the top, your coins at the bottom. A click on a row
 * trades once (the server checks and answers with the new state, {@link #update}).
 * <p>
 * Tribes (elders, the Miner, envoys) use it too: no purse (purse -1, hidden), and a deal may ask for goods in barter
 * ({@code Deal.cost}), drawn as small item icons before the coins.
 */
public class CaravanTradeScreen extends TradeBoardScreen {
    private CaravanTrade.Open open;

    public CaravanTradeScreen(CaravanTrade.Open open) {
        super(open.name());
        this.open = open;
    }

    public int entity() {
        return open.entity();
    }

    /** New state from the server (after a trade): same screen, new numbers. */
    public void update(CaravanTrade.Open open) {
        this.open = open;
    }

    @Override
    protected List<Row> rows() {
        List<Row> out = new ArrayList<>(open.deals().size());
        for (int i = 0; i < open.deals().size(); i++) {
            CaravanTrade.Deal d = open.deals().get(i);
            Component status = d.stock() <= 0 ? Component.translatable(d.sale() ? "gui.mechanical.caravan.sold_out" : "gui.mechanical.caravan.enough")
                    : d.sale() ? Component.translatable("gui.mechanical.caravan.left", d.stock())
                    : Component.translatable("gui.mechanical.caravan.wants", d.stock(), open.have().get(i));
            int coins = d.cost().isEmpty() || d.price() > 0 ? d.price() : -1;
            out.add(new Row(d.goods(), d.cost(), 0, coins, d.sale(), d.stock(), status, blocked(i)));
        }
        return out;
    }

    /** Why deal {@code i} can't happen now, or null if it can. */
    private @Nullable Component blocked(int i) {
        CaravanTrade.Deal d = open.deals().get(i);
        if (d.stock() <= 0) return Component.translatable(d.sale() ? "gui.mechanical.caravan.sold_out" : "gui.mechanical.caravan.enough");
        boolean creative = minecraft.player != null && minecraft.player.isCreative();
        if (d.sale() && open.have().get(i) < 1) return Component.translatable("gui.mechanical.caravan.no_goods");
        if (d.sale()) return open.coins() >= d.price() || creative ? null : Component.translatable("gui.mechanical.caravan.too_dear");
        if (open.purse() >= 0 && open.purse() < d.price()) return Component.translatable("gui.mechanical.caravan.purse_empty");
        return open.have().get(i) >= d.goods().getCount() ? null : Component.translatable("gui.mechanical.caravan.none_to_sell");
    }

    @Override
    protected Component badge() {
        return open.big() ? Component.translatable("gui.mechanical.caravan.big", open.kind()) : open.kind();
    }

    @Override
    protected int headerNameRoom(int headerWidth) {
        return open.purse() < 0 ? headerWidth : headerWidth - 8 - purseWidth();
    }

    private int purseWidth() {
        return Math.max(font.width(Component.translatable("gui.mechanical.caravan.purse")), coinsWidth(open.purse()));
    }

    /** The purse, top right (a tribe has none: it always pays). */
    @Override
    protected void extractHeaderExtra(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY) {
        if (open.purse() < 0) return;
        Component purse = Component.translatable("gui.mechanical.caravan.purse");
        g.text(font, purse, x + w - font.width(purse), y - 29, HideUi.DIM);
        coins(g, open.purse(), x + w - coinsWidth(open.purse()), y - 17, open.purse() > 0 ? HideUi.TEXT : HideUi.BAD);
    }

    @Override
    protected void extractWallet(GuiGraphicsExtractor g, int x, int y) {
        Component yours = Component.translatable("gui.mechanical.caravan.your_coins");
        g.text(font, yours, x, y + 3, HideUi.DIM);
        coins(g, open.coins(), x + font.width(yours) + 6, y + 1, HideUi.TEXT);
    }

    @Override
    protected Component hint() {
        return Component.translatable(open.purse() < 0 ? "gui.mechanical.caravan.barter_hint" : "gui.mechanical.caravan.hint");
    }

    @Override
    protected @Nullable Entity trader() {
        return minecraft.level == null ? null : minecraft.level.getEntity(open.entity());
    }

    @Override
    protected void send(int index, boolean all) {
        ClientPacketDistributor.sendToServer(new CaravanTrade.Click(open.entity(), index));
    }
}
