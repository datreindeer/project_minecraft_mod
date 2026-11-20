package com.mechanical.equipment;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * Quiver: 4 slots for arrows, worn in the Hip slot (see {@link Quiver}). The arrows live on the item itself (container
 * component), so they stay in it when it is taken off or dropped. Right-click in hand to open it; worn, press B.
 */
public class QuiverItem extends Item {
    public QuiverItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) PackMenu.openHeld(player, hand);
        return InteractionResult.SUCCESS;
    }
}
