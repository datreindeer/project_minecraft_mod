package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A wattle pen for plant waste. Each compostable item (#mechanical:compostable: Leaf Litter, Straw, Plant Fibre, leaves,
 * rotten flesh ...) fills it one step; full ({@link #FULL}) it rots for {@link #ROT_TICKS} and is then ready (LEVEL 8):
 * right-click takes {@link #YIELD} Compost and it starts over. Manure fills two steps.
 */
public class CompostHeapBlock extends Block {
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 8);
    public static final int FULL = 7, READY = 8, ROT_TICKS = 2400, YIELD = 2;
    private static final VoxelShape SHAPE = Shapes.or(box(0, 0, 0, 16, 12, 1), box(0, 0, 15, 16, 12, 16), box(0, 0, 0, 1, 12, 16),
            box(15, 0, 0, 16, 12, 16), box(1, 0, 1, 15, 1, 15));

    public CompostHeapBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(LEVEL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        int fill = state.getValue(LEVEL);
        if (!stack.is(Farming.COMPOSTABLE) || fill >= FULL) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level instanceof ServerLevel server) {
            int next = Math.min(FULL, fill + (stack.is(Farming.MANURE.get()) ? 2 : 1));
            stack.consume(1, player);
            server.setBlock(pos, state.setValue(LEVEL, next), Block.UPDATE_ALL);
            server.playSound(null, pos, SoundEvents.COMPOSTER_FILL, SoundSource.BLOCKS, 1.0F, 0.9F);
            if (next == FULL) server.scheduleTick(pos, this, ROT_TICKS);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(LEVEL) != READY) return InteractionResult.PASS;
        if (level instanceof ServerLevel server) {
            server.setBlock(pos, state.setValue(LEVEL, 0), Block.UPDATE_ALL);
            Block.popResource(server, pos.above(), new ItemStack(Farming.COMPOST.get(), YIELD));
            server.playSound(null, pos, SoundEvents.COMPOSTER_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(LEVEL) != FULL) return;
        level.setBlock(pos, state.setValue(LEVEL, READY), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.COMPOSTER_READY, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(LEVEL) == FULL && random.nextInt(4) == 0) { // rotting: a little steam
            level.addParticle(ParticleTypes.WHITE_SMOKE, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.8, pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0, 0.02, 0);
        }
    }
}
