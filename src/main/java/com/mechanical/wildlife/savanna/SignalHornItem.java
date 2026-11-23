package com.mechanical.wildlife.savanna;

import com.mechanical.clan.Clans;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Signal Horn: a hollowed gazelle horn blown like the goat horn. Its long call carries {@link #RANGE} blocks; every other
 * player within that range is told which way it came from and how far, and a tribe mate (Clans.sameTribe) also who blew
 * it. 5 s between calls; each call wears it a little.
 */
public class SignalHornItem extends Item {
    public static final int RANGE = 256, COOLDOWN = 100, USE_TICKS = 50;

    public SignalHornItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        if (level instanceof ServerLevel server && player instanceof ServerPlayer blower) {
            level.playSound(null, player, SavannaFauna.SIGNAL_HORN_SOUND, SoundSource.RECORDS, RANGE / 16.0F, 0.9F + level.getRandom().nextFloat() * 0.1F);
            level.gameEvent(GameEvent.INSTRUMENT_PLAY, player.position(), GameEvent.Context.of(player));
            alert(server, blower);
            stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
        }
        player.getCooldowns().addCooldown(stack, COOLDOWN);
        return InteractionResult.CONSUME;
    }

    /** Tells every other player in range where the call came from; returns how many heard it. */
    public static int alert(ServerLevel level, ServerPlayer blower) {
        int heard = 0;
        for (ServerPlayer p : level.players()) {
            if (p == blower || p.distanceTo(blower) > RANGE) continue;
            int dist = (int) p.distanceTo(blower);
            Component dir = Component.translatable("message.mechanical.signal_horn.dir." + direction(p.getX(), p.getZ(), blower.getX(), blower.getZ()));
            p.sendSystemMessage(Clans.sameTribe(p, blower)
                    ? Component.translatable("message.mechanical.signal_horn.tribe", blower.getDisplayName(), dir, dist)
                    : Component.translatable("message.mechanical.signal_horn.heard", dir, dist), true);
            heard++;
        }
        return heard;
    }

    /** Compass point (n, ne, e ... nw) from (x, z) towards (tx, tz); north is -z. */
    static String direction(double x, double z, double tx, double tz) {
        double a = Math.toDegrees(Math.atan2(tx - x, -(tz - z))); // 0 = north, 90 = east
        String[] points = {"n", "ne", "e", "se", "s", "sw", "w", "nw"};
        return points[(int) Math.floorMod(Math.round(a / 45.0), 8L)];
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return USE_TICKS;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.TOOT_HORN;
    }
}
