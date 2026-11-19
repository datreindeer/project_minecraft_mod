package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A torch the wind blew out (Wind): no light, a wisp of smoke for a while. Relight it with a torch, a fire drill or flint
 * and steel; broken, it drops the torch. The wall version is {@link Wall}.
 */
public class UnlitTorchBlock extends TorchBlock {
    public UnlitTorchBlock(Properties properties) {
        super(ParticleTypes.SMOKE, properties);
    }

    static boolean lights(ItemStack stack) {
        return stack.is(Items.TORCH) || stack.is(Items.FLINT_AND_STEEL) || stack.getItem() instanceof FireDrillItem;
    }

    static InteractionResult relight(ItemStack stack, Level level, BlockPos pos, Player player, InteractionHand hand, BlockState lit) {
        if (!lights(stack)) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            level.setBlock(pos, lit, Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.5F, 1.4F);
            if (stack.isDamageableItem()) stack.hurtAndBreak(1, player, hand);
        }
        return InteractionResult.SUCCESS;
    }

    /** Just a thin trail of smoke from the charred tip now and then. */
    static void smoke(Level level, BlockPos pos, double x, double y, double z, RandomSource random) {
        if (random.nextInt(5) == 0) level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.01, 0.0);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return relight(stack, level, pos, player, hand, Blocks.TORCH.defaultBlockState());
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        smoke(level, pos, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, random);
    }

    /** Unlit wall torch. */
    public static class Wall extends WallTorchBlock {
        public Wall(Properties properties) {
            super(ParticleTypes.SMOKE, properties);
        }

        @Override
        protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            return relight(stack, level, pos, player, hand, Blocks.WALL_TORCH.defaultBlockState().setValue(FACING, state.getValue(FACING)));
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            var opposite = state.getValue(FACING).getOpposite();
            smoke(level, pos, pos.getX() + 0.5 + 0.27 * opposite.getStepX(), pos.getY() + 0.92, pos.getZ() + 0.5 + 0.27 * opposite.getStepZ(), random);
        }
    }
}
