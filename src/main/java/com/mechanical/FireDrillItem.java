package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Lights an unlit campfire, Fire Pit, clay kiln or Smelting Furnace tap (if it has fuel), but only sometimes (more often with tinder, see Tinder). Every attempt wears it down. */
public class FireDrillItem extends Item {
    public FireDrillItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        // the Smithing Hearth: catches as often as a campfire without tinder
        if (state.getBlock() instanceof SmithingHearthBlock && level.getBlockEntity(pos) instanceof SmithingHearthBlockEntity hearth) {
            if (level instanceof ServerLevel server && context.getPlayer() != null && !hearth.lit()) {
                if (server.getRandom().nextFloat() < Tinder.CHANCE_WITHOUT) SmithingHearthBlock.lightUp(server, pos, hearth, context.getPlayer());
                else server.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 0.6F, 1.6F);
                context.getItemInHand().hurtAndBreak(1, context.getPlayer(), EquipmentSlot.MAINHAND);
            }
            return InteractionResult.SUCCESS;
        }
        // a covered wood pile: the vent of a charcoal clamp
        if (CharcoalClamp.isPile(state)) {
            if (level instanceof ServerLevel server && context.getPlayer() != null) {
                boolean catches = server.getRandom().nextFloat() < Tinder.CHANCE_WITHOUT;
                if (!CharcoalClamp.tryLight(server, pos, context.getClickedFace(), context.getPlayer(), catches))
                    server.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 0.6F, 1.6F);
                context.getItemInHand().hurtAndBreak(1, context.getPlayer(), EquipmentSlot.MAINHAND);
            }
            return InteractionResult.SUCCESS;
        }
        // candles catch easily from a fire drill
        if (state.getBlock() instanceof net.minecraft.world.level.block.AbstractCandleBlock && net.minecraft.world.level.block.CandleBlock.canLight(state)) {
            if (level instanceof ServerLevel server) {
                if (server.getRandom().nextFloat() < 0.6F) {
                    server.setBlock(pos, state.setValue(net.minecraft.world.level.block.AbstractCandleBlock.LIT, true), 11);
                    server.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.6F, 1.6F);
                } else {
                    server.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 0.6F, 1.6F);
                }
                if (context.getPlayer() != null) context.getItemInHand().hurtAndBreak(1, context.getPlayer(), EquipmentSlot.MAINHAND);
            }
            return InteractionResult.SUCCESS;
        }
        boolean kiln = state.getBlock() instanceof KilnBlock && !state.getValue(KilnBlock.LIT);
        boolean furnace = state.getBlock() instanceof FurnaceTapBlock && !state.getValue(FurnaceTapBlock.LIT);
        boolean pit = FirePit.canLight(state);
        boolean bloomery = state.getBlock() instanceof BloomeryHearthBlock && !state.getValue(BloomeryHearthBlock.LIT);
        if (!kiln && !furnace && !pit && !bloomery && !CampfireBlock.canLight(state)) return InteractionResult.PASS;
        if (level instanceof ServerLevel server) {
            KilnBlockEntity kilnEntity = kiln && server.getBlockEntity(KilnBlock.lowerPos(state, pos)) instanceof KilnBlockEntity k ? k : null;
            if (kiln && (kilnEntity == null || !kilnEntity.canLight())) {
                if (context.getPlayer() != null) context.getPlayer().sendOverlayMessage(Component.translatable("message.mechanical.kiln.needs_fuel"));
                return InteractionResult.FAIL;
            }
            FurnaceTapBlockEntity tap = furnace && server.getBlockEntity(pos) instanceof FurnaceTapBlockEntity t ? t : null;
            if (furnace && (tap == null || !tap.canLight())) {
                if (context.getPlayer() != null) context.getPlayer().sendOverlayMessage(tap != null && tap.problem() != null ? tap.problem()
                        : Component.translatable("message.mechanical.furnace.needs_fuel"));
                return InteractionResult.FAIL;
            }
            BloomeryBlockEntity hearth = bloomery && server.getBlockEntity(pos) instanceof BloomeryBlockEntity b ? b : null;
            if (bloomery && (hearth == null || !hearth.canLight())) {
                Component why = hearth != null ? hearth.check(server) : null;
                if (context.getPlayer() != null) context.getPlayer().sendOverlayMessage(why != null ? why : Component.translatable("message.mechanical.bloomery.needs_fuel"));
                return InteractionResult.FAIL;
            }
            var tinder = Tinder.holder(server, pos, state);
            if (server.getRandom().nextFloat() < (Tinder.has(tinder) ? Tinder.CHANCE_WITH : Tinder.CHANCE_WITHOUT)) {
                if (tinder != null) Tinder.set(tinder, false);
                if (kilnEntity != null) kilnEntity.tryLight();
                else if (tap != null) tap.tryLight();
                else if (hearth != null) hearth.tryLight(server);
                else if (pit) { FirePitBlockEntity e = FirePit.entity(server, pos, state); if (e != null) e.light(); }
                else server.setBlock(pos, state.setValue(CampfireBlock.LIT, true).trySetValue(Campfires.EMBERS, false), 11);
                server.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, 1.2F);
            } else {
                server.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 5, 0.2, 0.1, 0.2, 0.01);
                server.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 0.8F, 1.5F);
            }
            if (context.getPlayer() != null) context.getItemInHand().hurtAndBreak(1, context.getPlayer(), EquipmentSlot.MAINHAND);
        }
        return InteractionResult.SUCCESS;
    }
}
