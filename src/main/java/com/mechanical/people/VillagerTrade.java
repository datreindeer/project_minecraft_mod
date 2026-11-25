package com.mechanical.people;

import com.mechanical.Mechanical;
import com.mechanical.mixin.MerchantMenuAccessor;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * "Trade offer {@code offer} of my open merchant menu", from client.people.VillagerTradeScreen. The server does what a
 * player does in vanilla's screen: pick the offer (the menu moves the price into its payment slots), shift-click the
 * result once, take the rest of the payment back. So stock, uses, the villager's experience and level-ups, demand,
 * reputation prices, statistics and events are all vanilla's own, and nothing from the client is trusted but the number.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public record VillagerTrade(int containerId, int offer, boolean all) implements CustomPacketPayload {
    private static final int RESULT = 2;
    /** Shift-click: at most this many trades (an offer's stock runs out far sooner). */
    private static final int MOST = 64;
    public static final Type<VillagerTrade> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "villager_trade"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VillagerTrade> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, VillagerTrade::containerId, ByteBufCodecs.VAR_INT, VillagerTrade::offer,
            ByteBufCodecs.BOOL, VillagerTrade::all, VillagerTrade::new);

    @Override
    public Type<VillagerTrade> type() {
        return TYPE;
    }

    @SubscribeEvent
    static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToServer(TYPE, CODEC, (packet, context) -> {
            if (context.player() instanceof ServerPlayer player && !player.isSpectator()) trade(player, packet);
        });
    }

    /** Carries out the packet (server thread) and sends the new offers. Returns how many trades happened. */
    static int trade(Player player, VillagerTrade packet) {
        if (!(player.containerMenu instanceof MerchantMenu menu) || menu.containerId != packet.containerId || !menu.stillValid(player)) return 0;
        int done = 0;
        while (done < (packet.all ? MOST : 1) && once(player, menu, packet.offer)) done++;
        // whatever is left of the payment goes back to the inventory (what doesn't fit stays until the menu closes)
        menu.quickMoveStack(player, 0);
        menu.quickMoveStack(player, 1);
        menu.broadcastChanges();
        // vanilla only resends offers on a level-up (its own client counts along); ours shows the server's numbers
        if (player instanceof ServerPlayer sp && ((MerchantMenuAccessor) menu).mechanical$trader() instanceof AbstractVillager trader)
            sp.sendMerchantOffers(menu.containerId, trader.getOffers(), trader instanceof Villager v ? v.getVillagerData().level() : 1,
                    trader.getVillagerXp(), trader.showProgressBar(), trader.canRestock());
        return done;
    }

    private static boolean once(Player player, MerchantMenu menu, int index) {
        if (index < 0 || index >= menu.getOffers().size()) return false;
        MerchantOffer offer = menu.getOffers().get(index);
        // vanilla would throw away what doesn't fit of a result: nothing moves without room for all of it, counted before
        // the payment is taken out, so the payment's change always has its place back too
        if (offer.isOutOfStock() || !fits(player.getInventory(), offer.getResult())) return false;
        menu.setSelectionHint(index);
        menu.tryMoveItems(index);
        ItemStack a = menu.getSlot(0).getItem(), b = menu.getSlot(1).getItem(), result = menu.getSlot(RESULT).getItem();
        // the menu may offer another trade that the same payment satisfies: only the one asked for counts
        if (result.isEmpty() || !(offer.satisfiedBy(a, b) || offer.satisfiedBy(b, a)) || !ItemStack.matches(result, offer.getResult())) return false;
        int uses = offer.getUses();
        menu.quickMoveStack(player, RESULT);
        return offer.getUses() > uses;
    }

    private static boolean fits(Inventory inv, ItemStack stack) {
        int room = 0;
        for (int i = 0; i < Inventory.INVENTORY_SIZE && room < stack.getCount(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) room += stack.getMaxStackSize();
            else if (ItemStack.isSameItemSameComponents(s, stack)) room += s.getMaxStackSize() - s.getCount();
        }
        return room >= stack.getCount();
    }
}
