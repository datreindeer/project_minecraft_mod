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

/** Lights an unlit campfire or clay kiln (if it has fuel), but only sometimes (more often with tinder, see Tinder). Every attempt wears it down. */
public class FireDrillItem extends Item {
    public FireDrillItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
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
        if (!kiln && !CampfireBlock.canLight(state)) return InteractionResult.PASS;
        if (level instanceof ServerLevel server) {
            KilnBlockEntity kilnEntity = kiln && server.getBlockEntity(KilnBlock.lowerPos(state, pos)) instanceof KilnBlockEntity k ? k : null;
            if (kiln && (kilnEntity == null || !kilnEntity.canLight())) {
                if (context.getPlayer() != null) context.getPlayer().sendOverlayMessage(Component.translatable("message.mechanical.kiln.needs_fuel"));
                return InteractionResult.FAIL;
            }
            var tinder = Tinder.holder(server, pos, state);
            if (server.getRandom().nextFloat() < (Tinder.has(tinder) ? Tinder.CHANCE_WITH : Tinder.CHANCE_WITHOUT)) {
                if (tinder != null) Tinder.set(tinder, false);
                if (kilnEntity != null) kilnEntity.tryLight();
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
