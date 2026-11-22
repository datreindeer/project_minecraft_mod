package com.mechanical.wildlife.jungle;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Blowpipe: a bamboo tube. Right-click puffs a dart from the inventory (a Poison Dart first, else a plain Dart): silent,
 * quick (0.75 s between shots), weak (1 damage) and short-ranged, but a Poison Dart poisons what it hits (Poison II, 10 s).
 * The dart flies as a small arrow and can be picked up again as a plain Dart (the poison is spent).
 */
public class BlowpipeItem extends Item {
    static final int COOLDOWN = 15, POISON_TICKS = 200;

    public BlowpipeItem(Properties properties) {
        super(properties);
    }

    /** The first Poison Dart, else the first Dart, in the inventory (empty if none). */
    static ItemStack ammo(Player player) {
        ItemStack plain = ItemStack.EMPTY;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(JungleFauna.POISON_DART.get())) return s;
            if (plain.isEmpty() && s.is(JungleFauna.DART.get())) plain = s;
        }
        return plain;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack pipe = player.getItemInHand(hand);
        ItemStack ammo = ammo(player);
        boolean creative = player.getAbilities().instabuild;
        if (ammo.isEmpty() && !creative) return InteractionResult.FAIL;
        boolean poison = ammo.is(JungleFauna.POISON_DART.get());
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BREEZE_SHOOT, SoundSource.PLAYERS, 0.4F, 2.0F);
        if (level instanceof ServerLevel serverLevel) {
            Arrow dart = new Arrow(level, player, new ItemStack(JungleFauna.DART.get()), pipe);
            dart.setBaseDamage(1.0);
            if (poison) dart.addEffect(new MobEffectInstance(MobEffects.POISON, POISON_TICKS, 1));
            if (creative) dart.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            dart.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 2.2F, 0.6F);
            serverLevel.addFreshEntity(dart);
            pipe.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            if (!creative) ammo.shrink(1);
        }
        player.getCooldowns().addCooldown(pipe, COOLDOWN);
        return InteractionResult.SUCCESS;
    }
}
