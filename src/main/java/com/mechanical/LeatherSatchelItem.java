package com.mechanical;

import com.mechanical.equipment.PackMenu;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Leather Satchel: a 3x3 inventory for anything but other containers (see {@link BarkBasketMenu#SATCHEL}). Right-click in
 * hand to open it; worn in the Back slot, press B ({@link PackMenu}). Tool Loops stacked onto it in the inventory add
 * {@link Satchels#TOOL_SLOTS} tool-only slots; the Steel-Frame Pack is the same item class with 5x5 and the loops built in.
 */
public class LeatherSatchelItem extends Item {
    public final int columns;
    public final boolean builtInLoops;

    public LeatherSatchelItem(Properties properties) {
        this(properties, 3, false);
    }

    public LeatherSatchelItem(Properties properties, int columns, boolean builtInLoops) {
        super(properties);
        this.columns = columns;
        this.builtInLoops = builtInLoops;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) PackMenu.openHeld(player, hand);
        return InteractionResult.SUCCESS;
    }

    /** Tool Loops dropped onto a satchel without them: sewn on (one used up). */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack self, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess carried) {
        if (!other.is(Satchels.TOOL_LOOPS.get()) || Satchels.toolSlots(self) > 0 || self.getCount() != 1 || !slot.allowModification(player)) return false;
        self.set(Satchels.LOOPED.get(), Unit.INSTANCE);
        other.shrink(1);
        player.playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 0.8F, 1.0F);
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) FirePit.award(sp, "iron_age/tool_loops");
        return true;
    }
}
