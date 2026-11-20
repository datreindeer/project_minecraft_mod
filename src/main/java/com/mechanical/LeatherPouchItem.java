package com.mechanical;

import com.mechanical.equipment.PackMenu;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * Leather Pouch: a small drawstring bag of Leather and Cordage that replaces vanilla's Bundle. A 2x2 inventory for what a
 * satchel takes ({@link BarkBasketMenu#SATCHEL}). Right-click in hand to open it; worn in the Hip slot (in place of the
 * Quiver), press B to open it beside the worn satchel ({@link PackMenu}).
 */
public class LeatherPouchItem extends Item {
    public static final int SIZE = 4;

    public LeatherPouchItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) PackMenu.openHeld(player, hand);
        return InteractionResult.SUCCESS;
    }
}
