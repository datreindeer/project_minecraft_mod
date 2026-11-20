package com.mechanical;

import static com.mechanical.Mechanical.ITEMS;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Money replaces vanilla's emeralds. Copper Coin from the Bronze Age, Silver and Gold Coin from the Iron Age;
 * 1 Silver = 10 Copper, 1 Gold = 10 Silver. Villagers in the Stone Age barter goods for goods.
 */
public final class Coins {
    public static final int SILVER_IN_COPPER = 10, GOLD_IN_SILVER = 10;
    public static final DeferredItem<Item> COPPER_COIN = ITEMS.registerSimpleItem("copper_coin");
    public static final DeferredItem<Item> SILVER_COIN = ITEMS.registerSimpleItem("silver_coin");
    public static final DeferredItem<Item> GOLD_COIN = ITEMS.registerSimpleItem("gold_coin");

    public static void init() {}

    private Coins() {}
}
