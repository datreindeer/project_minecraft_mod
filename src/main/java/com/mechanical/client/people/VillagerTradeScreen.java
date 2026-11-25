package com.mechanical.client.people;

import java.util.ArrayList;
import java.util.List;

import com.mechanical.Coins;
import com.mechanical.Mechanical;
import com.mechanical.client.ui.HideUi;
import com.mechanical.people.VillagerTrade;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

/**
 * Villagers (and the Wandering Trader) trade on the {@link TradeBoardScreen} instead of vanilla's merchant screen. The
 * menu stays vanilla's {@link MerchantMenu}, so offers, stock, prices and levels arrive as always; a click asks the
 * server ({@link VillagerTrade}) to trade through that menu. Under the name: the profession level, its five marks and
 * the experience toward the next one.
 */
public class VillagerTradeScreen extends TradeBoardScreen implements MenuAccess<MerchantMenu> {
    private final MerchantMenu menu;
    /** The one looked at when the screen opened (the menu doesn't say which villager it is), or null. */
    private final @Nullable AbstractVillager villager;

    public VillagerTradeScreen(MerchantMenu menu, Component title, @Nullable AbstractVillager villager) {
        super(title);
        this.menu = menu;
        this.villager = villager;
    }

    /** Vanilla's merchant screen never opens: ours takes its place. */
    @EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
    public static final class Swap {
        @SubscribeEvent
        static void onOpen(ScreenEvent.Opening event) {
            if (!(event.getNewScreen() instanceof MerchantScreen vanilla)) return;
            Entity aimed = Minecraft.getInstance().crosshairPickEntity;
            event.setNewScreen(new VillagerTradeScreen(vanilla.getMenu(), vanilla.getTitle(), aimed instanceof AbstractVillager v ? v : null));
        }
    }

    @Override
    public MerchantMenu getMenu() {
        return menu;
    }

    private static boolean isCoin(ItemStack stack) {
        return stack.is(Coins.COPPER_COIN.get()) || stack.is(Coins.SILVER_COIN.get()) || stack.is(Coins.GOLD_COIN.get());
    }

    @Override
    protected List<Row> rows() {
        List<Row> out = new ArrayList<>(menu.getOffers().size());
        Inventory inv = minecraft.player.getInventory();
        for (MerchantOffer offer : menu.getOffers()) {
            ItemStack a = offer.getCostA(), b = offer.getCostB(), result = offer.getResult();
            // coins for goods: the villager buys (the goods are the row, the coins what it pays)
            boolean sale = !(isCoin(result) && !isCoin(a) && b.isEmpty());
            int stock = offer.isOutOfStock() ? 0 : offer.getMaxUses() - offer.getUses();
            int base = offer.getBaseCostA().getCount();
            // a changed price: on the price of a sale; for a purchase it is the amount of goods wanted
            int usual = a.getCount() != base ? base : 0;
            Component status = stock <= 0 ? Component.translatable("gui.mechanical.trade.out_of_stock")
                    : sale ? Component.translatable("gui.mechanical.caravan.left", stock)
                    : Component.translatable("gui.mechanical.caravan.wants", stock, count(inv, offer.getItemCostA()));
            Component blocked = stock <= 0 ? Component.translatable("gui.mechanical.trade.restocks")
                    : affordable(inv, offer) ? null : Component.translatable(sale ? "gui.mechanical.caravan.no_goods" : "gui.mechanical.caravan.none_to_sell");
            List<ItemStack> price = new ArrayList<>(2);
            if (sale) {
                price.add(a);
                if (!b.isEmpty()) price.add(b);
            } else {
                price.add(result);
            }
            out.add(new Row(sale ? result : a, price, usual, -1, sale, stock, status, blocked));
        }
        return out;
    }

    private static int count(Inventory inv, ItemCost cost) {
        int n = 0;
        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) if (cost.test(inv.getItem(i))) n += inv.getItem(i).getCount();
        return n;
    }

    /** As far as the client can tell; the server has the last word. */
    private static boolean affordable(Inventory inv, MerchantOffer offer) {
        int a = count(inv, offer.getItemCostA()), needA = offer.getCostA().getCount();
        if (offer.getItemCostB().isEmpty()) return a >= needA;
        ItemCost costB = offer.getItemCostB().get();
        boolean same = costB.item().equals(offer.getItemCostA().item());
        return same ? a >= needA + costB.count() : a >= needA && count(inv, costB) >= costB.count();
    }

    private boolean levels() {
        return menu.showProgressBar();
    }

    @Override
    protected Component badge() {
        if (!levels()) return Component.translatable("gui.mechanical.trade.travelling");
        Component level = Component.translatable("merchant.level." + Math.clamp(menu.getTraderLevel(), 1, 5));
        if (villager instanceof Villager v && v.hasCustomName())
            return Component.translatable("gui.mechanical.trade.profession_level", v.getVillagerData().profession().value().name(), level);
        return level;
    }

    /** Five marks for the levels, then the experience bar toward the next one. */
    @Override
    protected void extractHeaderExtra(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY) {
        if (!levels()) return;
        int level = Math.clamp(menu.getTraderLevel(), 1, 5), xp = menu.getTraderXp();
        for (int i = 0; i < 5; i++) diamond(g, x + 5 + i * 11, y + 6, i < level ? HideUi.GOLD : HideUi.DARK);
        int bx = x + 62, bw = w - 62;
        HideUi.inset(g, bx, y + 2, bw, 9);
        boolean master = !VillagerData.canLevelUp(level);
        int min = VillagerData.getMinXpPerLevel(level), max = master ? min + 1 : VillagerData.getMaxXpPerLevel(level);
        int filled = master ? bw - 2 : (int) Math.clamp((long) (bw - 2) * (xp - min) / (max - min), 0, bw - 2);
        g.fill(bx + 1, y + 3, bx + 1 + filled, y + 10, 0xFF6F9E3E);
        g.fill(bx + 1, y + 3, bx + 1 + filled, y + 4, 0xFFC4EC8E);
        if (mouseX >= x && mouseX < bx + bw && mouseY >= y && mouseY < y + 13) {
            g.setTooltipForNextFrame(font, master ? Component.translatable("gui.mechanical.trade.master")
                    : Component.translatable("gui.mechanical.trade.xp", xp, max, Component.translatable("merchant.level." + (level + 1))), mouseX, mouseY);
        }
    }

    private static void diamond(GuiGraphicsExtractor g, int cx, int cy, int colour) {
        for (int r = -5; r <= 5; r++) g.fill(cx - (5 - Math.abs(r)), cy + r, cx + (5 - Math.abs(r)) + 1, cy + r + 1, HideUi.EDGE);
        for (int r = -3; r <= 3; r++) g.fill(cx - (3 - Math.abs(r)), cy + r, cx + (3 - Math.abs(r)) + 1, cy + r + 1, colour);
    }

    @Override
    protected void extractWallet(GuiGraphicsExtractor g, int x, int y) {
        Component yours = Component.translatable("gui.mechanical.caravan.your_coins");
        g.text(font, yours, x, y + 3, HideUi.DIM);
        coins(g, Coins.worth(minecraft.player), x + font.width(yours) + 6, y + 1, HideUi.TEXT);
    }

    @Override
    protected Component hint() {
        return Component.translatable("gui.mechanical.trade.hint");
    }

    @Override
    protected Component clickLine(Row row) {
        return Component.translatable(row.sale() ? "gui.mechanical.trade.click_buy" : "gui.mechanical.trade.click_sell");
    }

    @Override
    protected @Nullable Entity trader() {
        return villager;
    }

    @Override
    protected void send(int index, boolean all) {
        ClientPacketDistributor.sendToServer(new VillagerTrade(menu.containerId, index, all));
    }

    // ---- as a container screen: closing it closes the menu

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (minecraft.options.keyInventory.matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        minecraft.player.closeContainer();
        super.onClose();
    }

    @Override
    public void removed() {
        if (minecraft.player != null) menu.removed(minecraft.player);
    }

    @Override
    public void tick() {
        super.tick();
        if (!minecraft.player.isAlive() || minecraft.player.isRemoved()) minecraft.player.closeContainer();
    }
}
