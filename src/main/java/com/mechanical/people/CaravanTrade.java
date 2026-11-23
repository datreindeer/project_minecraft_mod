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

    /** One deal: the caravan sells {@code goods} ({@code sale}) or buys it, for {@code price} copper, {@code stock} more times. */
    public record Deal(ItemStack goods, int price, int stock, boolean sale) {
        static final Codec<Deal> CODEC = RecordCodecBuilder.create(i -> i.group(
                ItemStack.CODEC.fieldOf("goods").forGetter(Deal::goods), Codec.INT.fieldOf("price").forGetter(Deal::price),
                Codec.INT.fieldOf("stock").forGetter(Deal::stock), Codec.BOOL.fieldOf("sale").forGetter(Deal::sale)).apply(i, Deal::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Deal> STREAM_CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC, Deal::goods, ByteBufCodecs.VAR_INT, Deal::price, ByteBufCodecs.VAR_INT, Deal::stock,
                ByteBufCodecs.BOOL, Deal::sale, Deal::new);

        Deal used() {
            return new Deal(goods, price, stock - 1, sale);
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

    /** May {@code player} trade with this leader now (close, not angry, the tribe deals with them)? */
    static boolean canTrade(Player player, Tribesperson leader) {
        return leader.isAlive() && leader.stock != null && leader.getTarget() == null && !leader.isAngryAt(player)
                && (leader.tribe() == null || leader.tribe().trades(player)) && leader.distanceToSqr(player) < REACH * REACH;
    }

    /** Sends the leader's goods and purse to the player (opens the screen, or refreshes it). */
    static void open(ServerPlayer player, Tribesperson leader) {
        if (!canTrade(player, leader)) return;
        Stock stock = leader.stock;
        Inventory inv = player.getInventory();
        List<Integer> have = stock.deals.stream().map(d -> d.sale() ? 0 : inv.countItem(d.goods().getItem())).toList();
        // the name says "<name> the Caravan Leader of the <tribe>": who and their group
        PacketDistributor.sendToPlayer(player, new Open(leader.getId(), leader.getDisplayName(),
                Component.translatable("trader_kind." + stock.kind.getNamespace() + "." + stock.kind.getPath()), stock.big, stock.purse,
                List.copyOf(stock.deals), Coins.worth(player), have));
    }

    /** Server, main thread: a click on a deal. Anything stale, forged or out of reach is ignored. */
    private static void onClick(Click click, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || player.isSpectator()) return;
        if (!(player.level().getEntity(click.entity()) instanceof Tribesperson leader) || leader.role() != TribeRole.TRADER) return;
        trade(player, leader, click.deal());
        open(player, leader);
    }

    /**
     * Carries out deal {@code index} once: buying takes the price (with change) and fills the purse; selling needs the goods
     * and enough in the purse. Returns whether it happened.
     */
    static boolean trade(Player player, Tribesperson leader, int index) {
        if (!canTrade(player, leader) || index < 0 || index >= leader.stock.deals.size()) return false;
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
