package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A Root Cellar's 9 slots, sealed while the lid is on like a Storage Jar. Dug in ({@link Roots#dugIn}) it is dark and
 * cool: root crops and greens ({@link Roots#CELLAR_PRODUCE}) age at 1/4 of the normal rate, other perishables at 1/2.
 * Above ground it is just a box.
 */
public class RootCellarBlockEntity extends StorageJarBlockEntity {
    public RootCellarBlockEntity(BlockPos pos, BlockState state) {
        super(Roots.ROOT_CELLAR_ENTITY.get(), pos, state);
    }

    // shortcut: the rate is judged when the food comes out, so digging in (or out) just before opening counts for the whole time; track it per seal if that gets abused
    @Override
    protected float rate(ItemStack stack) {
        if (level == null || !Roots.dugIn(level, worldPosition)) return 1;
        return stack.is(Roots.CELLAR_PRODUCE) ? Roots.CELLAR_PRODUCE_RATE : Roots.CELLAR_OTHER_RATE;
    }

    @Override
    protected void lidSound(Level level, BlockPos pos, boolean open) {
        level.playSound(null, pos, open ? SoundEvents.BARREL_OPEN : SoundEvents.BARREL_CLOSE, SoundSource.BLOCKS, 0.5F, 0.9F + level.getRandom().nextFloat() * 0.1F);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.mechanical.root_cellar");
    }
}
