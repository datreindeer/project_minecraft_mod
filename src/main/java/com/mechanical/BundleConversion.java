package com.mechanical;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Vanilla's Bundle (any colour) is the {@link LeatherPouchItem}: one that ends up in a player's inventory (found in a
 * chest, picked up, from an old world or another mod) turns into a Leather Pouch within a second, its first 4 stacks
 * inside, anything more given back loose (dropped if the inventory is full). Its name carries over, the dye doesn't.
 * Bundles are also gone from the creative tabs; their recipes are off (scripts/gen_age_recipes.py).
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class BundleConversion {
    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.tickCount % 20 != 0) return;
        Inventory inventory = player.getInventory();
        List<ItemStack> loose = new ArrayList<>();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(ItemTags.BUNDLES)) inventory.setItem(i, convert(stack, loose));
        }
        for (ItemStack s : loose) inventory.placeItemBackInInventory(s, net.minecraft.util.Prediction.SERVER_ONLY);
    }

    /** The pouch a bundle becomes; contents that don't fit (or a pouch may not hold) go to {@code loose}. */
    public static ItemStack convert(ItemStack bundle, List<ItemStack> loose) {
        ItemStack pouch = new ItemStack(Mechanical.LEATHER_POUCH.get());
        if (bundle.has(DataComponents.CUSTOM_NAME)) pouch.set(DataComponents.CUSTOM_NAME, bundle.get(DataComponents.CUSTOM_NAME));
        NonNullList<ItemStack> items = NonNullList.withSize(LeatherPouchItem.SIZE, ItemStack.EMPTY);
        int next = 0;
        for (ItemStack s : bundle.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY).itemCopies().toList()) {
            if (next < items.size() && BarkBasketMenu.SATCHEL.test(s)) items.set(next++, s);
            else loose.add(s);
        }
        pouch.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
        for (int i = 1; i < bundle.getCount(); i++) loose.add(new ItemStack(Mechanical.LEATHER_POUCH.get())); // pouches don't stack
        return pouch;
    }

    @SubscribeEvent
    static void onCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        event.removeIf(s -> s.is(ItemTags.BUNDLES), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
    }

    private BundleConversion() {}
}
