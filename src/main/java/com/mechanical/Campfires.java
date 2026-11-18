package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Campfires burn out: after a few minutes a lit campfire sinks to glowing embers (EMBERS, added to the vanilla block by
 * a mixin), which take a piece of firewood to blaze up again; left alone the embers die and it needs the fire drill.
 * Firewood on a burning fire adds time. Cooked food stays on the fire until taken off (empty hand) and burns if left.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class Campfires {
    public static final BooleanProperty EMBERS = BooleanProperty.create("embers");
    public static final int MIN_BURN = 3600, MAX_BURN = 6000;   // 3-5 minutes burning
    public static final int EMBER_TICKS = 2400;                 // 2 minutes glowing
    public static final int FIREWOOD_TICKS = 1200;              // +1 minute per firewood
    public static final int MAX_FUEL = 12000;
    public static final int MIN_BURN_FOOD = 600, MAX_BURN_FOOD = 1200; // 30-60 s after it is done, food burns

    public static boolean isEmbers(BlockState state) {
        return state.hasProperty(EMBERS) && state.getValue(EMBERS);
    }

    /** Lit -> embers (fire goes out, logs keep glowing). */
    public static void toEmbers(Level level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(CampfireBlock.LIT, false).setValue(EMBERS, true), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 0.6F);
    }

    /** Embers -> cold (needs the fire drill now). */
    public static void toCold(Level level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(EMBERS, false), Block.UPDATE_ALL);
    }

    @SubscribeEvent
    static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(Blocks.CAMPFIRE) || !(level.getBlockEntity(pos) instanceof CampfireAccess fire)) return;
        ItemStack stack = event.getItemStack();
        boolean lit = state.getValue(CampfireBlock.LIT);

        if (stack.is(Mechanical.FIREWOOD_TAG) && (lit || isEmbers(state))) {
            if (!level.isClientSide()) {
                if (lit) {
                    fire.mechanical$addFuel(FIREWOOD_TICKS);
                } else {
                    level.setBlock(pos, state.setValue(CampfireBlock.LIT, true).setValue(EMBERS, false), Block.UPDATE_ALL);
                    fire.mechanical$relight();
                }
                stack.consume(1, event.getEntity());
                level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.6F, 1.4F);
            }
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        } else if (stack.isEmpty() && fire.mechanical$hasItems()) {
            if (!level.isClientSide()) fire.mechanical$takeItem(event.getEntity());
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    private Campfires() {}
}
