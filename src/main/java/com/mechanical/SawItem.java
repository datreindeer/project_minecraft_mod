package com.mechanical;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * The Bronze Saw: a crafting tool like the flint pickaxe (Log + Saw → planks, losing 1 durability), and it cuts fallen
 * logs across while right-click is held on one: started by {@link TreeChopping}, it goes on while the player keeps looking
 * at that same log, one stroke (push or pull) every {@link #STROKE_TICKS}. The arms' push-pull is drawn from the use
 * time (client: SawClient), so everyone sees it in step with the strokes.
 */
public class SawItem extends FlintPickaxeItem {
    public static final int STROKE_TICKS = 10;
    /** The fallen log each player is sawing. Kept on both sides: the client stops its own player's sawing too. */
    private static final Map<Player, BlockPos> SAWING = new WeakHashMap<>();

    public SawItem(Properties properties) {
        super(properties);
    }

    static void startSawing(Player player, InteractionHand hand, BlockPos log) {
        SAWING.put(player, log.immutable());
        player.startUsingItem(hand);
    }

    /** 0 → 1 → 0 over one push and pull, the push ending on a stroke. */
    public static float push(float ticksUsing) {
        return (1 - (float) Math.cos(ticksUsing * Math.PI / STROKE_TICKS)) / 2;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.NONE; // the arm pose and first-person motion come from SawClient
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int ticksRemaining) {
        // other players' clients only draw the sawing; their server decides when it stops
        if (!(user instanceof Player player) || level.isClientSide() && !player.isLocalPlayer()) return;
        BlockPos log = SAWING.get(player);
        BlockState state = log == null ? null : level.getBlockState(log);
        if (state == null || !(state.getBlock() instanceof FallenLogBlock)
                || !(player.pick(player.blockInteractionRange(), 1.0F, false) instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK || !hit.getBlockPos().equals(log)) {
            player.stopUsingItem();
            return;
        }
        int ticks = getUseDuration(stack, user) - ticksRemaining;
        if (level instanceof ServerLevel server && ticks > 0 && ticks % STROKE_TICKS == 0)
            TreeChopping.sawStroke(server, player, player.getUsedItemHand(), log, state, stack, ticks / STROKE_TICKS % 2 == 1);
    }
}
