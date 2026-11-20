package com.mechanical.equipment;

import org.jspecify.annotations.Nullable;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * One {@link Accessory} slot in the player's inventory menu: takes one item of the slot's tag, shows the slot's grey
 * outline while empty, keeps Curse of Binding items on (like an armour slot) and plays the equip sound.
 */
public class AccessorySlot extends Slot {
    private final Player owner;
    public final Accessory accessory;

    public AccessorySlot(Player owner, Accessory accessory, int x, int y) {
        super(new EquipmentView(owner), accessory.ordinal(), x, y);
        this.owner = owner;
        this.accessory = accessory;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return Equipment.fits(accessory, stack);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean mayPickup(Player player) {
        ItemStack stack = getItem();
        if (!stack.isEmpty() && !player.isCreative() && EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)) return false;
        return super.mayPickup(player);
    }

    @Override
    public void setByPlayer(ItemStack stack, ItemStack previous) {
        Equipment.onEquip(owner, accessory, previous, stack);
        super.setByPlayer(stack, previous);
    }

    @Override
    public @Nullable Identifier getNoItemIcon() {
        return accessory.emptyIcon;
    }
}
