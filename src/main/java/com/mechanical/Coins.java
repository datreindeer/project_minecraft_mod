package com.mechanical;

import static com.mechanical.Mechanical.ITEMS;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Money replaces vanilla's emeralds. Copper Coin from the Bronze Age, Silver and Gold Coin from the Iron Age;
 * 1 Silver = 10 Copper, 1 Gold = 10 Silver. Every villager trade is goods for coins (docs/traders-plan.md A).
 * Prices in code are in copper; {@link #pay} and {@link #give} make change.
 */
public final class Coins {
    public static final int SILVER_IN_COPPER = 10, GOLD_IN_SILVER = 10;
    /** A Copper, Silver or Gold Ingot struck cold on the Bronze Anvil ({@link BronzeAnvilBlockEntity}) with the Bronze Smithing Hammer. */
    public static final int MINT_HITS = 4, COINS_PER_INGOT = 4;
    public static final DeferredItem<Item> COPPER_COIN = ITEMS.registerSimpleItem("copper_coin");
    public static final DeferredItem<Item> SILVER_COIN = ITEMS.registerSimpleItem("silver_coin");
    public static final DeferredItem<Item> GOLD_COIN = ITEMS.registerSimpleItem("gold_coin");
    /** A Trader Caravan leader's purse, held in his hand (people.Caravans); not dropped (his coins fall out instead). */
    public static final DeferredItem<Item> COIN_PURSE = ITEMS.registerSimpleItem("coin_purse", p -> p.stacksTo(1));

    public static void init() {}

    /** What the Bronze Anvil mints into coins: Copper, Silver (Iron Age, {@link IronOres}) and Gold Ingots. */
    public static boolean mintable(ItemStack stack) {
        return !minted(stack).isEmpty();
    }

    /** The coins one ingot mints into, or empty if it isn't a coin metal. */
    public static ItemStack minted(ItemStack ingot) {
        Item coin = ingot.is(Items.COPPER_INGOT) ? COPPER_COIN.get() : ingot.is(IronOres.SILVER_INGOT.get()) ? SILVER_COIN.get()
                : ingot.is(Items.GOLD_INGOT) ? GOLD_COIN.get() : null;
        return coin == null ? ItemStack.EMPTY : new ItemStack(coin, COINS_PER_INGOT);
    }

    /** Copper each coin is worth: 1, 10, 100. */
    public static final int SILVER = SILVER_IN_COPPER, GOLD = SILVER_IN_COPPER * GOLD_IN_SILVER;

    /** The coins' worth in copper of everything the player carries. */
    public static int worth(Player player) {
        Inventory inv = player.getInventory();
        return inv.countItem(COPPER_COIN.get()) + inv.countItem(SILVER_COIN.get()) * SILVER + inv.countItem(GOLD_COIN.get()) * GOLD;
    }

    /**
     * Takes {@code copper} worth of coins from the player, smallest coins first, and gives the change back in the biggest
     * coins. False (nothing taken) if they don't have enough; creative players pay nothing.
     */
    public static boolean pay(Player player, int copper) {
        if (copper <= 0 || player.isCreative()) return true;
        if (worth(player) < copper) return false;
        int taken = 0;
        int[] values = {1, SILVER, GOLD};
        Item[] coins = {COPPER_COIN.get(), SILVER_COIN.get(), GOLD_COIN.get()};
        Inventory inv = player.getInventory();
        for (int c = 0; c < 3 && taken < copper; c++) {
            for (int i = 0; i < inv.getContainerSize() && taken < copper; i++) {
                ItemStack st = inv.getItem(i);
                if (!st.is(coins[c])) continue;
                int n = Math.min(st.getCount(), (copper - taken + values[c] - 1) / values[c]);
                st.shrink(n);
                taken += n * values[c];
            }
        }
        give(player, taken - copper);
        return true;
    }

    /** Gives {@code copper} worth of coins, in the biggest coins (into the inventory, or dropped if full). */
    public static void give(Player player, int copper) {
        int[] split = split(copper);
        Item[] coins = {GOLD_COIN.get(), SILVER_COIN.get(), COPPER_COIN.get()};
        for (int c = 0; c < 3; c++) if (split[c] > 0) hand(player, new ItemStack(coins[c], split[c]));
    }

    /** Into the player's inventory, or dropped at their feet if it is full. */
    public static void hand(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack) && !stack.isEmpty()) player.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY);
    }

    /** {@code copper} as {gold, silver, copper} coins. */
    public static int[] split(int copper) {
        return new int[] {copper / GOLD, copper % GOLD / SILVER, copper % SILVER};
    }

    private Coins() {}
}
