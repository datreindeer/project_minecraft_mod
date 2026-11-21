package com.mechanical;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Fishing rod with a bone hook: bites come about as much sooner as with Lure I (5 s), on top of any enchantment. The
 * Bronze Hook Fishing Rod is this with more lure and luck (BronzeTools).
 */
public class BoneHookRodItem extends FishingRodItem {
    public static final int LURE_TICKS = 100;
    public final int lureTicks, luck;

    public BoneHookRodItem(Properties properties) {
        this(properties, LURE_TICKS, 0);
    }

    /** {@code lureTicks}: how much sooner fish bite; {@code luck}: added to Luck of the Sea (better catches, fewer junk). */
    public BoneHookRodItem(Properties properties, int lureTicks, int luck) {
        super(properties);
        this.lureTicks = lureTicks;
        this.luck = luck;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.fishing != null) return super.use(level, player, hand); // reeling in works as usual
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.NEUTRAL,
                0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        if (level instanceof ServerLevel server) {
            int lure = (int) (EnchantmentHelper.getFishingTimeReduction(server, stack, player) * 20.0F) + lureTicks;
            int luck = EnchantmentHelper.getFishingLuckBonus(server, stack, player) + this.luck;
            Projectile.spawnProjectile(new FishingHook(player, level, luck, lure), server, stack);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        stack.causeUseVibration(player, GameEvent.ITEM_INTERACT_START);
        return InteractionResult.SUCCESS;
    }
}
