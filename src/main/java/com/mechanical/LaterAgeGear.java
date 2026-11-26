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
 * Monsters spawn without gear from ages the world hasn't reached (WorldAge; copper from the Bronze Age, iron and
 * chainmail and crossbows from the Iron Age): iron/gold/chainmail/diamond armour is taken off, and weapons they need
 * to fight (skeleton bows, iron swords, crossbows before the Iron Age, tridents) stay in hand but are never dropped.
 * Items a monster picked up itself are guaranteed drops and are left alone.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class LaterAgeGear {
    private static final String[] METALS = {"iron", "gold", "chainmail", "diamond", "netherite", "copper"};

    /** Is this gear from an age the world hasn't reached? Copper from the Bronze Age, iron and chainmail from the Iron Age. */
    static boolean isLaterAge(ItemStack stack, int worldAge) {
        if (stack.isEmpty()) return false;
        if (stack.is(Items.BOW) || stack.is(Items.TRIDENT) || stack.isEnchanted()) return true;
        if (stack.is(Items.CROSSBOW)) return worldAge < WorldAge.IRON; // forged from iron parts in the Iron Age (Smithing)
        if (stack.is(Smithing.STEEL_CROSSBOW.get())) return true; // Steel Age gear, and WorldAge has no Steel Age yet: never reached
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        for (String metal : METALS) {
            if (!id.contains(metal)) continue;
            boolean reached = metal.equals("copper") ? worldAge >= WorldAge.BRONZE : (metal.equals("iron") || metal.equals("chainmail")) && worldAge >= WorldAge.IRON;
            if (!reached) return true;
        }
        return false;
    }

    @SubscribeEvent
    static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob) || !(mob instanceof Enemy)) return;
        int worldAge = WorldAge.of(((net.minecraft.server.level.ServerLevel) event.getLevel()).getServer());
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = mob.getItemBySlot(slot);
            if (!isLaterAge(stack, worldAge) || mob.getDropChances().isPreserved(slot)) continue;
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) mob.setItemSlot(slot, ItemStack.EMPTY);
            else mob.setDropChance(slot, 0.0F);
        }
    }

    private LaterAgeGear() {}
}
