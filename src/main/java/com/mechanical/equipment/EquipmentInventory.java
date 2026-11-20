package com.mechanical.equipment;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * What a player wears in the {@link Accessory} slots: one item per slot, saved with the player (attachment
 * {@link Equipment#INVENTORY}). The fields below are server-side bookkeeping that is never saved: what each slot held
 * the last time {@link Equipment} looked, and the attribute modifiers it applied for it.
 */
public class EquipmentInventory extends SimpleContainer {
    final ItemStack[] lastSeen = new ItemStack[Accessory.COUNT];
    @SuppressWarnings("unchecked")
    final List<AppliedModifier>[] applied = new List[Accessory.COUNT];

    record AppliedModifier(Holder<Attribute> attribute, Identifier id) {}

    public EquipmentInventory() {
        super(Accessory.COUNT);
        for (int i = 0; i < Accessory.COUNT; i++) {
            lastSeen[i] = ItemStack.EMPTY;
            applied[i] = new ArrayList<>();
        }
    }

    public ItemStack get(Accessory slot) {
        return getItem(slot.ordinal());
    }

    public void set(Accessory slot, ItemStack stack) {
        setItem(slot.ordinal(), stack);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    void save(ValueOutput output) {
        ContainerHelper.saveAllItems(output, getItems());
    }

    void load(ValueInput input) {
        ContainerHelper.loadAllItems(input, getItems());
    }
}
