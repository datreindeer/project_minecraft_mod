package com.mechanical;

import com.mechanical.wildlife.Wildlife;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.StrawBedBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.CanContinueSleepingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Straw Bed: the stone age can't sleep the night away, but lying down in straw is a rest. Right-click sets your respawn
 * point and resets the time since you last rested (so phantoms keep away); the night goes on.
 * Right-click one holding a Wolf Pelt to spread it over the bed: a Furred Straw Bed, which rests better (more sanity, a
 * little healing, and warmth for a while afterwards). Broken, it gives the pelt back.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class StrawBedRest {
    static final float FURRED_HEAL = 4;

    @SubscribeEvent
    static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        boolean furred = state.is(Mechanical.FURRED_STRAW_BED.get());
        if (!furred && !state.is(Blocks.STRAW_BED)) return;
        ItemStack held = event.getItemStack();
        if (event.getEntity().isSecondaryUseActive() && !held.isEmpty()) return; // sneak-place against it
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!furred && held.is(Wildlife.WOLF_PELT.get())) {
            spreadPelt(level, pos, state);
            held.consume(1, player);
            return;
        }
        player.setRespawnPosition(new ServerPlayer.RespawnConfig(
                LevelData.RespawnData.of(player.level().dimension(), pos, player.getYRot(), player.getXRot()), false), true); // vanilla "Respawn point set" (only when it changes)
        if (Sanity.rest(player, furred) && furred) player.heal(FURRED_HEAL);
        if (furred) Temperature.warmFromBedroll(player);
        player.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
        player.sendOverlayMessage(Component.translatable(furred ? "message.mechanical.furred_straw_bed_rest" : "message.mechanical.straw_bed_rest")
                .withStyle(ChatFormatting.GRAY));
        if (level.isDarkOutside() && level.dimensionType().defaultClock().isPresent()) lieDown(player, pos, state);
    }

    /** At night you lie down in it; once every player is lying in a bed the night runs fast (see onLevelTick). */
    private static void lieDown(ServerPlayer player, BlockPos pos, BlockState state) {
        ServerLevel level = player.level();
        if (state.getValue(AbstractBedBlock.OCCUPIED) || player.isSleeping()) return;
        Vec3 c = Vec3.atBottomCenterOf(pos);
        if (!level.getEntitiesOfClass(Monster.class, new AABB(c.x - 8, c.y - 5, c.z - 8, c.x + 8, c.y + 5, c.z + 8),
                m -> m.isPreventingPlayerRest(level, player)).isEmpty()) {
            player.sendOverlayMessage(Component.translatable("block.minecraft.bed.not_safe"));
            return;
        }
        player.startSleeping(state.getValue(AbstractBedBlock.PART) == BedPart.HEAD ? pos : pos.relative(AbstractBedBlock.getConnectedDirection(state)));
        level.updateSleepingPlayerList();
    }

    public static boolean inStrawBed(Player player) {
        return player.isSleeping() && player.getSleepingPos().map(p -> player.level().getBlockState(p).getBlock() instanceof StrawBedBlock).orElse(false);
    }

    /** Vanilla wakes you at once from a bed you can't sleep in; a straw bed keeps you lying until it gets light. */
    @SubscribeEvent
    static void onContinueSleeping(CanContinueSleepingEvent event) {
        if (event.getEntity() instanceof Player player && inStrawBed(player)) event.setContinueSleeping(player.level().isDarkOutside());
    }

    /**
     * When all players in a level are lying down and at least one is in straw, the clock runs SPEED times faster till
     * morning (vanilla doesn't skip: PlayerMixin keeps straw sleepers from "sleeping long enough"). The rate goes over the
     * network, so clients see the sun come up fast instead of a jump.
     */
    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.players().isEmpty()) return;
        var clock = level.dimensionType().defaultClock();
        if (clock.isEmpty()) return;
        boolean straw = false, all = true;
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator()) continue;
            if (!p.isSleeping()) all = false;
            else if (inStrawBed(p)) straw = true;
        }
        boolean fast = all && straw && level.isDarkOutside();
        var clocks = level.getServer().clockManager();
        float rate = clocks.getInstance(clock.get()).rate();
        // shortcut: assumes nothing else sets the rate to exactly SPEED, so a rate left over from a server stop mid-night resets to 1
        if (fast && rate != SPEED) {
            restoreRate = rate;
            clocks.setRate(clock.get(), SPEED);
        } else if (!fast && rate == SPEED) {
            clocks.setRate(clock.get(), restoreRate);
            restoreRate = 1.0F;
        }
    }

    static final float SPEED = 40.0F; // a night (~11000 ticks) in about 14 s
    private static float restoreRate = 1.0F;

    /** Swaps both halves for the furred bed, keeping facing and part (no shape updates, so neither half breaks the other). */
    static void spreadPelt(Level level, BlockPos pos, BlockState state) {
        Block fur = Mechanical.FURRED_STRAW_BED.get();
        BlockPos other = pos.relative(AbstractBedBlock.getConnectedDirection(state));
        BlockState otherState = level.getBlockState(other);
        level.setBlock(pos, fur.withPropertiesOf(state), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        if (otherState.is(Blocks.STRAW_BED)) level.setBlock(other, fur.withPropertiesOf(otherState), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
    }

    private StrawBedRest() {}
}
