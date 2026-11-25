package com.mechanical.people;

import java.util.ArrayList;
import java.util.List;

import com.mechanical.Coins;
import com.mechanical.Mechanical;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * A Trader Caravan's own trade screen (docs/traders-plan.md D): the leader's {@link Stock} (what the caravan sells and buys,
 * each deal with a stock that runs out, and the coins in its purse) is sent to the player as {@link Open}; the client shows
 * client.people.CaravanTradeScreen, and a click comes back as {@link Click}, which the server checks and carries out
 * ({@link #trade}), then sends the new state. Prices are in copper; the player pays with any coins and gets change (Coins).
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class CaravanTrade {
    static final double REACH = 8;

    /**
     * One deal: the caravan sells {@code goods} ({@code sale}) or buys it, for {@code price} copper, {@code stock} more times.
     * {@code cost}: goods the player hands over besides the coins (a tribe's barter, {@link #barterDeal}); empty for caravans.
     */
    public record Deal(ItemStack goods, int price, int stock, boolean sale, List<ItemStack> cost) {
        public Deal(ItemStack goods, int price, int stock, boolean sale) {
            this(goods, price, stock, sale, List.of());
        }

        static final Codec<Deal> CODEC = RecordCodecBuilder.create(i -> i.group(
                ItemStack.CODEC.fieldOf("goods").forGetter(Deal::goods), Codec.INT.fieldOf("price").forGetter(Deal::price),
                Codec.INT.fieldOf("stock").forGetter(Deal::stock), Codec.BOOL.fieldOf("sale").forGetter(Deal::sale),
                ItemStack.CODEC.listOf().optionalFieldOf("cost", List.of()).forGetter(Deal::cost)).apply(i, Deal::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Deal> STREAM_CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC, Deal::goods, ByteBufCodecs.VAR_INT, Deal::price, ByteBufCodecs.VAR_INT, Deal::stock,
                ByteBufCodecs.BOOL, Deal::sale, ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list(4)), Deal::cost, Deal::new);

        Deal used() {
            return new Deal(goods, price, stock - 1, sale, cost);
        }
    }

    /** The leader's goods and purse, saved with him. {@code kind}: a TraderKinds id; {@code big}: a wagon caravan. */
    public static final class Stock {
        static final Codec<Stock> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.fieldOf("kind").forGetter(s -> s.kind), Codec.BOOL.fieldOf("big").forGetter(s -> s.big),
                Codec.INT.fieldOf("purse").forGetter(s -> s.purse), Deal.CODEC.listOf().fieldOf("deals").forGetter(s -> s.deals)).apply(i, Stock::new));
        public final Identifier kind;
        public final boolean big;
        public int purse;
        public final List<Deal> deals;

        public Stock(Identifier kind, boolean big, int purse, List<Deal> deals) {
            this.kind = kind;
            this.big = big;
            this.purse = purse;
            this.deals = new ArrayList<>(deals);
        }
    }

    public record Open(int entity, Component name, Component kind, boolean big, int purse, List<Deal> deals, int coins,
            List<Integer> have) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "caravan_trade"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Open> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Open::entity, ComponentSerialization.TRUSTED_STREAM_CODEC, Open::name,
                ComponentSerialization.TRUSTED_STREAM_CODEC, Open::kind,
                ByteBufCodecs.BOOL, Open::big, ByteBufCodecs.VAR_INT, Open::purse, Deal.STREAM_CODEC.apply(ByteBufCodecs.list(64)), Open::deals,
                ByteBufCodecs.VAR_INT, Open::coins, ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(64)), Open::have, Open::new);

        @Override
        public Type<Open> type() {
            return TYPE;
        }
    }

    public record Click(int entity, int deal) implements CustomPacketPayload {
        public static final Type<Click> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "caravan_click"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Click> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Click::entity, ByteBufCodecs.VAR_INT, Click::deal, Click::new);

        @Override
        public Type<Click> type() {
            return TYPE;
        }
    }

    @SubscribeEvent
    static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(Open.TYPE, Open.CODEC).playToServer(Click.TYPE, Click.CODEC, CaravanTrade::onClick);
    }

    /**
     * May {@code player} trade with this leader now (close, not angry, the tribe deals with them)? A caravan leader trades
     * his {@link Stock}; an elder, the Miner or an envoy their barter offers ({@link Tribesperson#getOffers}).
     */
    static boolean canTrade(Player player, Tribesperson leader) {
        return leader.isAlive() && (leader.stock != null || leader.role() != TribeRole.TRADER && (leader.barters() || leader.isEnvoy()))
                && leader.getTarget() == null && !leader.isAngryAt(player)
                && (leader.tribe() == null || leader.tribe().trades(player)) && leader.distanceToSqr(player) < REACH * REACH;
    }

    /** Sends the leader's goods and purse to the player (opens the screen, or refreshes it). */
    static void open(ServerPlayer player, Tribesperson leader) {
        Open open = payload(player, leader);
        if (open == null) return;
        if (leader.stock == null) leader.trading(player); // a tribesperson stands still while you barter
        PacketDistributor.sendToPlayer(player, open);
    }

    /**
     * What the screen shows {@code player}, or null if they can't trade now. A tribe without a caravan has no purse
     * ({@code purse} -1: they always pay) and its deals come from its barter offers, one deal per offer, same order.
     */
    static Open payload(Player player, Tribesperson leader) {
        if (!canTrade(player, leader)) return null;
        Inventory inv = player.getInventory();
        if (leader.stock == null) {
            List<Deal> deals = barterDeals(player, leader);
            List<Integer> have = deals.stream().map(d -> d.sale() ? times(inv, d.cost()) : inv.countItem(d.goods().getItem())).toList();
            Component group = Component.translatable("entity.mechanical.tribesperson.role." + leader.role().id());
            return new Open(leader.getId(), leader.getDisplayName(), group, false, -1, deals, Coins.worth(player), have);
        }
        Stock stock = leader.stock;
        List<Integer> have = stock.deals.stream().map(d -> d.sale() ? 0 : inv.countItem(d.goods().getItem())).toList();
        // the name says "<name> the Caravan Leader of the <tribe>": who and their group
        return new Open(leader.getId(), leader.getDisplayName(),
                Component.translatable("trader_kind." + stock.kind.getNamespace() + "." + stock.kind.getPath()), stock.big, stock.purse,
                List.copyOf(stock.deals), Coins.worth(player), have);
    }

    /** The tribesperson's barter offers as deals (the packet holds 64), prices after the player's standing (Tribe.priceDiff). */
    static List<Deal> barterDeals(Player player, Tribesperson t) {
        List<Deal> deals = new ArrayList<>();
        for (MerchantOffer offer : t.getOffers()) if (deals.size() < 64) deals.add(barterDeal(player, t, offer));
        return deals;
    }

    /**
     * One offer as a deal: coins given for goods are a purchase ({@code sale} false, the goods are what they want); else a
     * sale of the result, coins in the cost becoming the copper price and other goods the barter {@code cost}.
     */
    static Deal barterDeal(Player player, Tribesperson t, MerchantOffer offer) {
        offer.setSpecialPriceDiff(t.tribe() != null && !Caravans.isExchange(offer) ? t.tribe().priceDiff(player, offer.getBaseCostA().getCount()) : 0);
        int stock = Math.max(0, offer.getMaxUses() - offer.getUses());
        ItemStack a = offer.getCostA(), b = offer.getCostB();
        if (Caravans.isCoin(offer.getResult()) && !Caravans.isCoin(a) && b.isEmpty())
            return new Deal(a.copy(), copper(offer.getResult()), stock, false);
        List<ItemStack> cost = new ArrayList<>();
        int price = 0;
        for (ItemStack c : new ItemStack[] {a, b}) {
            if (c.isEmpty()) continue;
            if (Caravans.isCoin(c)) price += copper(c);
            else cost.add(c.copy());
        }
        return new Deal(offer.getResult().copy(), price, stock, true, List.copyOf(cost));
    }

    /** A coin stack's worth in copper. */
    private static int copper(ItemStack coins) {
        return coins.getCount() * (coins.is(Coins.GOLD_COIN.get()) ? Coins.GOLD : coins.is(Coins.SILVER_COIN.get()) ? Coins.SILVER : 1);
    }

    /** How many times the inventory holds all of {@code cost} (999 when there is none). */
    private static int times(Inventory inv, List<ItemStack> cost) {
        int n = 999;
        for (ItemStack c : cost) n = Math.min(n, inv.countItem(c.getItem()) / Math.max(1, c.getCount()));
        return n;
    }

    /** Server, main thread: a click on a deal. Anything stale, forged or out of reach is ignored. */
    private static void onClick(Click click, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || player.isSpectator()) return;
        if (!(player.level().getEntity(click.entity()) instanceof Tribesperson leader)) return;
        trade(player, leader, click.deal());
        open(player, leader);
    }

    /**
     * Carries out deal {@code index} once: buying takes the price (with change) and fills the purse; selling needs the goods
     * and enough in the purse. Returns whether it happened.
     */
    static boolean trade(Player player, Tribesperson leader, int index) {
        if (!canTrade(player, leader)) return false;
        if (leader.stock == null) return barter(player, leader, index);
        if (index < 0 || index >= leader.stock.deals.size()) return false;
        Stock stock = leader.stock;
        Deal d = stock.deals.get(index);
        if (d.stock() <= 0) return false;
        if (d.sale()) {
            if (!Coins.pay(player, d.price())) return false;
            Coins.hand(player, d.goods().copy());
            stock.purse += d.price();
            if (d.stock() == 1 && player instanceof ServerPlayer sp) TribeVisits.award(sp, "caravan_sold_out");
        } else {
            Inventory inv = player.getInventory();
            if (stock.purse < d.price() || inv.countItem(d.goods().getItem()) < d.goods().getCount()) return false;
            take(inv, d.goods());
            Coins.give(player, d.price());
            stock.purse -= d.price();
            if (Caravans.isGem(d.goods()) && player instanceof ServerPlayer sp) TribeVisits.award(sp, "caravan_gem");
        }
        stock.deals.set(index, d.used());
        if (leader.tribe() != null) leader.tribe().addReputation(player, Tribe.TRADE);
        leader.playSound(TribeSounds.TRADE, 1.0F, leader.getVoicePitch());
        return true;
    }

    /**
     * A tribe's offer {@code index} once (already checked: {@link #canTrade}): a sale takes the coins (with change) and the
     * barter goods and hands over the result; a purchase takes the goods and pays the coins. No purse: tribes always pay.
     */
    private static boolean barter(Player player, Tribesperson t, int index) {
        if (index < 0 || index >= t.getOffers().size()) return false;
        MerchantOffer offer = t.getOffers().get(index);
        Deal d = barterDeal(player, t, offer);
        if (d.stock() <= 0) return false;
        Inventory inv = player.getInventory();
        if (d.sale()) {
            if (times(inv, d.cost()) < 1 || !Coins.pay(player, d.price())) return false;
            d.cost().forEach(c -> take(inv, c));
            Coins.hand(player, d.goods().copy());
        } else {
            if (inv.countItem(d.goods().getItem()) < d.goods().getCount()) return false;
            take(inv, d.goods());
            Coins.give(player, d.price());
        }
        t.traded(player, offer);
        t.playSound(TribeSounds.TRADE, 1.0F, t.getVoicePitch());
        return true;
    }

    private static void take(Inventory inv, ItemStack goods) {
        int left = goods.getCount();
        for (int i = 0; i < inv.getContainerSize() && left > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (!s.is(goods.getItem())) continue;
            int n = Math.min(left, s.getCount());
            s.shrink(n);
            left -= n;
        }
    }

    private CaravanTrade() {}
}
