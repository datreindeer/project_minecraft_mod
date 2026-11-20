package com.mechanical;

import static com.mechanical.Mechanical.ITEMS;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Money replaces vanilla's emeralds. Copper Coin from the Bronze Age, Silver and Gold Coin from the Iron Age;
 * 1 Silver = 10 Copper, 1 Gold = 10 Silver. Villagers in the Stone Age barter goods for goods.
 */
public final class Coins {
    public static final int SILVER_IN_COPPER = 10, GOLD_IN_SILVER = 10;
    /** A Copper Ingot struck cold on the Bronze Anvil ({@link BronzeAnvilBlockEntity}) with the Bronze Smithing Hammer. */
    public static final int MINT_HITS = 4, COINS_PER_INGOT = 4;
    public static final DeferredItem<Item> COPPER_COIN = ITEMS.registerSimpleItem("copper_coin");
    public static final DeferredItem<Item> SILVER_COIN = ITEMS.registerSimpleItem("silver_coin");
    public static final DeferredItem<Item> GOLD_COIN = ITEMS.registerSimpleItem("gold_coin");

    public static void init() {}

    /** What the Bronze Anvil mints into Copper Coins (Silver and Gold Coins wait for the Iron Age). */
    public static boolean mintable(ItemStack stack) {
        return stack.is(Items.COPPER_INGOT);
    }

    private Coins() {}
}
