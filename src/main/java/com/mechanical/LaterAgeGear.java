package com.mechanical;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Monsters spawn without gear from later ages: iron/gold/chainmail/diamond armour is taken off, and weapons they need
 * to fight (skeleton bows, iron swords, crossbows, tridents) stay in hand but are never dropped.
 * Items a monster picked up itself are guaranteed drops and are left alone.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class LaterAgeGear {
    private static final String[] METALS = {"iron", "gold", "chainmail", "diamond", "netherite", "copper"};

    static boolean isLaterAge(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.is(Items.BOW) || stack.is(Items.CROSSBOW) || stack.is(Items.TRIDENT) || stack.isEnchanted()) return true;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        for (String metal : METALS) if (id.contains(metal)) return true;
        return false;
    }

    @SubscribeEvent
    static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob) || !(mob instanceof Enemy)) return;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = mob.getItemBySlot(slot);
            if (!isLaterAge(stack) || mob.getDropChances().isPreserved(slot)) continue;
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) mob.setItemSlot(slot, ItemStack.EMPTY);
            else mob.setDropChance(slot, 0.0F);
        }
    }

    private LaterAgeGear() {}
}
