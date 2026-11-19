package com.mechanical;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Leather Satchel: right-click to open a 3x3 inventory for anything but other containers (see {@link BarkBasketMenu#SATCHEL}). */
public class LeatherSatchelItem extends Item {
    private static final int OFFHAND_SLOT = 40;

    public LeatherSatchelItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            ItemStack stack = player.getItemInHand(hand);
            int slot = hand == InteractionHand.MAIN_HAND ? player.getInventory().getSelectedSlot() : OFFHAND_SLOT;
            player.openMenu(new SimpleMenuProvider((id, inventory, p) -> BarkBasketMenu.open(Mechanical.LEATHER_SATCHEL_MENU.get(),
                    BarkBasketMenu.SATCHEL, id, inventory, slot, stack), stack.getHoverName()));
        }
        return InteractionResult.SUCCESS;
    }
}
