package com.mechanical.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mechanical.Campfires;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/** Adds the EMBERS state to campfires (glowing logs, no flame) and its sparks. */
@Mixin(CampfireBlock.class)
public abstract class CampfireBlockMixin extends BaseEntityBlock {
    protected CampfireBlockMixin(Properties properties) {
        super(properties);
    }

    @Inject(method = "createBlockStateDefinition", at = @At("TAIL"))
    private void mechanical$addEmbers(StateDefinition.Builder<Block, BlockState> builder, CallbackInfo ci) {
        builder.add(Campfires.EMBERS);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void mechanical$defaultNoEmbers(boolean spawnParticles, int fireDamage, Properties properties, CallbackInfo ci) {
        registerDefaultState(defaultBlockState().setValue(Campfires.EMBERS, false));
    }

    /** Embers glow faintly (light 1), just enough to find the fire in the dark. Overrides NeoForge's IBlockExtension default. */
    public int getLightEmission(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return Campfires.isEmbers(state) ? 1 : state.getLightEmission();
    }

    @Inject(method = "animateTick", at = @At("HEAD"))
    private void mechanical$emberSparks(BlockState state, Level level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (!Campfires.isEmbers(state)) return;
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.3, pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0, 0.02, 0);
        }
        if (random.nextInt(10) == 0) {
            level.addParticle(ParticleTypes.SMALL_FLAME, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.25, pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0, 0.01, 0);
        }
    }
}
