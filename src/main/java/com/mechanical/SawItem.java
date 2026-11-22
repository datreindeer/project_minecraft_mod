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
 * at that same log, one stroke (push or pull) every {@link #strokeTicks} (Bronze {@link #STROKE_TICKS}; the forged Iron and
 * Steel Saws cut faster, Smithing). The arms' push-pull is drawn from the use
 * time (client: SawClient), so everyone sees it in step with the strokes.
 */
public class SawItem extends FlintPickaxeItem {
    public static final int STROKE_TICKS = 10;
    /** The fallen log each player is sawing. Kept on both sides: the client stops its own player's sawing too. */
    private static final Map<Player, BlockPos> SAWING = new WeakHashMap<>();
    /** The felled log block each player is sawing instead ({@link FelledLogEntity}). */
    private record LogCut(FelledLogEntity log, int index) {}
    private static final Map<Player, LogCut> SAWING_LOGS = new WeakHashMap<>(), SAWING_LOGS_CLIENT = new WeakHashMap<>();

    /** Per side: in singleplayer the client's and the server's player are equal (same entity id), and each side has its own log entity. */
    private static Map<Player, LogCut> sawingLogs(Player player) {
        return player.level().isClientSide() ? SAWING_LOGS_CLIENT : SAWING_LOGS;
    }

    /** Ticks a stroke takes with this saw. */
    public final int strokeTicks;

    public SawItem(Properties properties) {
        this(properties, STROKE_TICKS);
    }

    public SawItem(Properties properties, int strokeTicks) {
        super(properties);
        this.strokeTicks = strokeTicks;
    }

    static void startSawing(Player player, InteractionHand hand, BlockPos log) {
        sawingLogs(player).remove(player);
        SAWING.put(player, log.immutable());
        player.startUsingItem(hand);
    }

    static void startSawingLog(Player player, InteractionHand hand, FelledLogEntity log, int index) {
        SAWING.remove(player);
        sawingLogs(player).put(player, new LogCut(log, index));
        player.startUsingItem(hand);
    }

    /** 0 → 1 → 0 over one push and pull, the push ending on a stroke. */
    public static float push(float ticksUsing, int strokeTicks) {
        return (1 - (float) Math.cos(ticksUsing * Math.PI / strokeTicks)) / 2;
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
        int ticks = getUseDuration(stack, user) - ticksRemaining;
        LogCut cut = sawingLogs(player).get(player);
        if (cut != null) { // a block of a felled log: on while the player looks at that block
            int look = cut.log().pick(player), t = cut.index();
            // a block: still on it; a seam (-(k + 2)): still on one of its two blocks
            if (!cut.log().isAlive() || (t >= 0 ? look != t : look != -t - 2 && look != -t - 1)) {
                sawingLogs(player).remove(player);
                player.stopUsingItem();
            } else if (level instanceof ServerLevel server && ticks > 0 && ticks % strokeTicks == 0) {
                cut.log().sawStroke(server, player, player.getUsedItemHand(), cut.index(), stack, ticks / strokeTicks % 2 == 1);
            }
            return;
        }
        BlockPos log = SAWING.get(player);
        BlockState state = log == null ? null : level.getBlockState(log);
        if (state == null || !(state.getBlock() instanceof FallenLogBlock)
                || !(player.pick(player.blockInteractionRange(), 1.0F, false) instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK || !hit.getBlockPos().equals(log)) {
            player.stopUsingItem();
            return;
        }
        if (level instanceof ServerLevel server && ticks > 0 && ticks % strokeTicks == 0)
            TreeChopping.sawStroke(server, player, player.getUsedItemHand(), log, state, stack, ticks / strokeTicks % 2 == 1);
    }
}
