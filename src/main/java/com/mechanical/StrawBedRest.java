package com.mechanical;

import com.mechanical.wildlife.Wildlife;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.StrawBedBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.CanContinueSleepingEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Straw Bed: the stone age can't sleep the night away, but lying down in straw is a rest. Right-click sets your respawn
 * point and resets the time since you last rested (so phantoms keep away). At night you lie down, and once everyone is
 * lying down the night runs fast ({@link #nightRunsFast}); there is no vanilla night skip.
 * Right-click one holding a Wolf Pelt to spread it over the bed: a Furred Straw Bed, which rests better (more sanity, a
 * little healing, and warmth for a while afterwards). Broken, it gives the pelt back.
 * The Iron Age Cloth Bed ({@link Weaving}) rests like the furred one and, unlike straw, isn't used up when you get out.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class StrawBedRest {
    static final float FURRED_HEAL = 4;

    @SubscribeEvent
    static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        boolean cloth = state.is(Weaving.CLOTH_BED.get());
        boolean furred = cloth || state.is(Mechanical.FURRED_STRAW_BED.get()); // the Cloth Bed rests as well as a pelt
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
        player.sendOverlayMessage(Component.translatable(cloth ? "message.mechanical.cloth_bed_rest" : furred ? "message.mechanical.furred_straw_bed_rest" : "message.mechanical.straw_bed_rest")
                .withStyle(ChatFormatting.GRAY));
        if (level.isDarkOutside() && level.dimensionType().defaultClock().isPresent()) lieDown(player, pos, state);
    }

    /** At night you lie down in it; once every player is lying in a bed the night runs fast (see nightRunsFast). */
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
     * True while every player in the overworld (but spectators) is lying down at night and at least one is in straw: then
     * {@link Calendar} runs the clock at {@link #SPEED} till morning (vanilla doesn't skip: PlayerMixin keeps straw sleepers
     * from "sleeping long enough"). The rate goes over the network, so clients see the sky spin instead of a jump. Called
     * every overworld tick; when a fast night ends in daylight, everyone gets {@link #SLEPT_ADVANCEMENT}.
     */
    static boolean nightRunsFast(ServerLevel level) {
        boolean straw = false, all = true;
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator()) continue;
            if (!p.isSleeping()) all = false;
            else if (inStrawBed(p)) straw = true;
        }
        boolean fast = all && straw && level.isDarkOutside();
        // players already woke up this tick (CanContinueSleepingEvent), so award whoever is here
        if (wasFast && !fast && !level.isDarkOutside())
            for (ServerPlayer p : level.players()) if (!p.isSpectator()) Kettle.award(p, SLEPT_ADVANCEMENT);
        wasFast = fast;
        return fast;
    }

    /** Clock ticks per game tick on a fast night: a night (~11000 clock ticks) in about 23 s. */
    static final float SPEED = 24.0F;
    public static final Identifier SLEPT_ADVANCEMENT = Identifier.fromNamespaceAndPath(Mechanical.MODID, "food/slept_in_straw");
    private static boolean wasFast;

    /** Swaps both halves for the furred bed, keeping facing and part (no shape updates, so neither half breaks the other). */
    static void spreadPelt(Level level, BlockPos pos, BlockState state) {
        Block fur = Mechanical.FURRED_STRAW_BED.get();
        BlockPos other = pos.relative(AbstractBedBlock.getConnectedDirection(state));
        BlockState otherState = level.getBlockState(other);
        level.setBlock(pos, fur.withPropertiesOf(state), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        if (otherState.is(Blocks.STRAW_BED)) level.setBlock(other, fur.withPropertiesOf(otherState), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
    }

    /** Falls apart like straw when you get up, but the Wolf Pelt drops instead of vanishing with it. */
    public static class FurredStrawBedBlock extends StrawBedBlock {
        public FurredStrawBedBlock(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        protected void destroyOnLeave(Level level, BlockPos pos) {
            Block.popResource(level, pos, new ItemStack(Wildlife.WOLF_PELT.get()));
            super.destroyOnLeave(level, pos);
        }
    }

    private StrawBedRest() {}
}
