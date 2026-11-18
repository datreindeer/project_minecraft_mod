package com.mechanical;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A small bark basket: right-click to open its 3x3 inventory for fruit, berries and hides (see {@link BarkBasketMenu}). */
public class BarkBasketItem extends Item {
    private static final int OFFHAND_SLOT = 40;

    public BarkBasketItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            ItemStack stack = player.getItemInHand(hand);
            int slot = hand == InteractionHand.MAIN_HAND ? player.getInventory().getSelectedSlot() : OFFHAND_SLOT;
            player.openMenu(new SimpleMenuProvider((id, inventory, p) -> BarkBasketMenu.open(id, inventory, slot, stack),
                    Component.translatable("item.mechanical.bark_basket")));
        }
        return InteractionResult.SUCCESS;
    }
}
