package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Compost (+1), Manure (+2) and Guano (+3): right-click farmland, or a crop on it, to raise its fertility (max 3). */
public class FertilizerItem extends Item {
    private final int amount;

    public FertilizerItem(int amount, Properties properties) {
        super(properties);
        this.amount = amount;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!level.getBlockState(pos).hasProperty(Fertility.FERTILITY)) pos = pos.below(); // clicked the crop
        if (!level.getBlockState(pos).hasProperty(Fertility.FERTILITY) || Fertility.get(level.getBlockState(pos)) >= Fertility.MAX) return InteractionResult.PASS;
        if (level instanceof ServerLevel server) {
            Fertility.add(level, pos, amount);
            context.getItemInHand().consume(1, context.getPlayer());
            server.playSound(null, pos, SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 1.0F, 0.8F);
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 6, 0.3, 0.1, 0.3, 0);
        }
        return InteractionResult.SUCCESS;
    }
}
