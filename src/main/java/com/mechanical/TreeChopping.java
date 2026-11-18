package com.mechanical;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Working felled wood by hand, holding right-click with an axe: one heavy chop every {@link #CHOP_TICKS}.
 * - Fallen log: bark comes off (Bark), it is hewn thinner twice, then it becomes a Rough Log (or only sticks if it fails).
 * - Rough Log stood on a stump (right-click the stump with it): split into 2-3 Firewood (or 1 Firewood + sticks if it fails).
 * A better axe needs fewer chops per stage and fails less often.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class TreeChopping {
    private static final int CHOP_TICKS = 12;
    private static final Map<Player, Long> LAST_CHOP = new WeakHashMap<>();
    /** Chops done on a block towards its next stage. Lost if the server restarts, which only costs a few chops. */
    private static final Map<Level, Map<BlockPos, Integer>> PROGRESS = new WeakHashMap<>();

    /** Chops per stage: flint axe 4, stone 3, iron 2. */
    static int chopsNeeded(ItemStack axe) {
        return Math.max(2, (int) Math.ceil(10 / axeSpeed(axe)));
    }

    /** Chance a finished piece comes out whole: flint axe 60 %, stone 75 %, iron 95 %. */
    static float successChance(ItemStack axe) {
        return Math.min(0.95F, 0.35F + 0.1F * axeSpeed(axe));
    }

    /** Extra firewood per successful split: flint / crude stone 0, stone and copper +1, iron +2, diamond +3. */
    static int firewoodBonus(ItemStack axe) {
        return Math.max(0, (int) ((axeSpeed(axe) - 2) / 2));
    }

    private static float axeSpeed(ItemStack axe) {
        return Math.max(1, axe.getDestroySpeed(Blocks.OAK_LOG.defaultBlockState()));
    }

    @SubscribeEvent
    static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        boolean axe = stack.is(ItemTags.AXES);

        if (state.getBlock() instanceof FallenLogBlock && axe) {
            take(event, InteractionResult.CONSUME);
            if (level instanceof ServerLevel server && chop(server, player, event.getHand(), pos, state, stack)) finishFallenLog(server, pos, state, stack);
        } else if (state.getBlock() instanceof StumpBlock) {
            int log = state.getValue(StumpBlock.LOG);
            String wood = roughLogWood(stack);
            if (log == 0 && wood != null) {
                take(event, InteractionResult.SUCCESS);
                if (!level.isClientSide()) {
                    level.setBlock(pos, state.setValue(StumpBlock.LOG, Mechanical.WOODS.indexOf(wood) + 1), Block.UPDATE_ALL);
                    level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
                    stack.consume(1, player);
                }
            } else if (log > 0 && axe) {
                take(event, InteractionResult.CONSUME);
                if (level instanceof ServerLevel server && chop(server, player, event.getHand(), pos.above(), state, stack)) splitOnStump(server, pos, state, stack);
            } else if (log > 0 && stack.isEmpty()) { // take the log back off
                take(event, InteractionResult.SUCCESS);
                if (!level.isClientSide()) {
                    level.setBlock(pos, state.setValue(StumpBlock.LOG, 0), Block.UPDATE_ALL);
                    player.getInventory().placeItemBackInInventory(new ItemStack(Mechanical.ROUGH_LOGS.get(Mechanical.WOODS.get(log - 1)).get()), net.minecraft.util.Prediction.SERVER_ONLY);
                }
            }
        }
    }

    private static void take(PlayerInteractEvent.RightClickBlock event, InteractionResult result) {
        event.setCanceled(true);
        event.setCancellationResult(result);
    }

    private static String roughLogWood(ItemStack stack) {
        for (var e : Mechanical.ROUGH_LOGS.entrySet()) if (stack.is(e.getValue().get())) return e.getKey();
        return null;
    }

    /** One chop (if the cooldown allows). Returns true when this chop completes the stage. {@code at} is where the chopping shows. */
    private static boolean chop(ServerLevel level, Player player, InteractionHand hand, BlockPos at, BlockState state, ItemStack axe) {
        long now = level.getGameTime();
        Long last = LAST_CHOP.get(player);
        if (last != null && now - last < CHOP_TICKS) return false;
        LAST_CHOP.put(player, now);

        RandomSource random = level.getRandom();
        // slow, heavy swing
        player.swing(hand, new net.minecraft.world.item.component.SwingAnimation(net.minecraft.world.item.SwingAnimationType.WHACK, CHOP_TICKS), true);
        level.playSound(null, at, SoundEvents.AXE_STRIP.value(), SoundSource.BLOCKS, 0.9F, 0.6F + random.nextFloat() * 0.2F);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state.getBlock() instanceof StumpBlock ? Blocks.OAK_LOG.defaultBlockState() : state),
                at.getX() + 0.5, at.getY() + 0.6, at.getZ() + 0.5, 6, 0.25, 0.2, 0.25, 0.05);

        Map<BlockPos, Integer> progress = PROGRESS.computeIfAbsent(level, l -> new HashMap<>());
        int done = progress.merge(at.immutable(), 1, Integer::sum);
        int needed = chopsNeeded(axe);
        if (done < needed) {
            level.destroyBlockProgress(player.getId(), at, Math.min(9, done * 10 / needed));
            return false;
        }
        progress.remove(at);
        level.destroyBlockProgress(player.getId(), at, -1);
        axe.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        return true;
    }

    private static void finishFallenLog(ServerLevel level, BlockPos pos, BlockState state, ItemStack axe) {
        int stage = state.getValue(FallenLogBlock.STAGE);
        RandomSource random = level.getRandom();
        if (stage == 0) Block.popResource(level, pos, new ItemStack(Mechanical.BARK.get(), 1 + random.nextInt(2)));
        if (stage < FallenLogBlock.LAST_STAGE) {
            level.setBlock(pos, state.setValue(FallenLogBlock.STAGE, stage + 1), Block.UPDATE_ALL);
            return;
        }
        level.removeBlock(pos, false);
        level.playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.0F, 0.8F);
        String wood = ((FallenLogBlock) state.getBlock()).wood();
        if (random.nextFloat() < successChance(axe)) Block.popResource(level, pos, new ItemStack(Mechanical.ROUGH_LOGS.get(wood).get()));
        else Block.popResource(level, pos, new ItemStack(Items.STICK, 2 + random.nextInt(2)));
    }

    private static void splitOnStump(ServerLevel level, BlockPos pos, BlockState state, ItemStack axe) {
        RandomSource random = level.getRandom();
        String wood = Mechanical.WOODS.get(state.getValue(StumpBlock.LOG) - 1);
        level.setBlock(pos, state.setValue(StumpBlock.LOG, 0), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
        var firewood = Mechanical.FIREWOOD.get(wood).get();
        if (random.nextFloat() < successChance(axe)) {
            Block.popResource(level, pos.above(), new ItemStack(firewood, 2 + random.nextInt(2) + firewoodBonus(axe)));
        } else {
            Block.popResource(level, pos.above(), new ItemStack(firewood, 1));
            Block.popResource(level, pos.above(), new ItemStack(Items.STICK, 1 + random.nextInt(2)));
        }
    }

    private TreeChopping() {}
}
