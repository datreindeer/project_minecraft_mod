package com.mechanical;

import com.mechanical.equipment.PackMenu;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * Leather Satchel: a 3x3 inventory for anything but other containers (see {@link BarkBasketMenu#SATCHEL}). Right-click in
 * hand to open it; worn in the Back slot, press B ({@link PackMenu}).
 */
public class LeatherSatchelItem extends Item {
    public LeatherSatchelItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) PackMenu.openHeld(player, hand);
        return InteractionResult.SUCCESS;
    }
}
