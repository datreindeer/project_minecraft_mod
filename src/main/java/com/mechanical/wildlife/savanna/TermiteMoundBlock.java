package com.mechanical.wildlife.savanna;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Termite Mound: hard, baked red earth riddled with tunnels; savanna worldgen stacks it into spires 2-4 blocks tall.
 * Poke a Stick into it (right-click) and now and then ({@link #CHANCE}) it comes out covered in Termites. Aardvarks dig
 * at the foot of mounds at night. Breaks into itself.
 */
public class TermiteMoundBlock extends Block {
    static final float CHANCE = 0.35F;

    public TermiteMoundBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Items.STICK)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level instanceof ServerLevel server) {
            var at = hit.getLocation();
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), at.x, at.y, at.z, 5, 0.1, 0.1, 0.1, 0.05);
            level.playSound(null, pos, SoundEvents.ROOTED_DIRT_HIT, SoundSource.BLOCKS, 0.8F, 1.2F);
            if (level.getRandom().nextFloat() < CHANCE) {
                level.playSound(null, pos, SoundEvents.SILVERFISH_AMBIENT, SoundSource.BLOCKS, 0.4F, 1.8F);
                if (!player.getInventory().add(new ItemStack(SavannaFauna.TERMITES.get()))) player.spawnAtLocation(server, new ItemStack(SavannaFauna.TERMITES.get()));
            }
            player.getCooldowns().addCooldown(stack, 10);
        }
        return InteractionResult.SUCCESS;
    }
}
