package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
 * Hand quern: right-click with Grain to turn the upper stone a quarter (the model turns with TURN); a full turn grinds
 * one Grain into one Flour. The Rapakivi Quern ({@link StoneCrafts#RAPAKIVI_QUERN}) turns half a turn per click ({@link #step} 2).
 */
public class QuernBlock extends Block {
    public static final IntegerProperty TURN = IntegerProperty.create("turn", 0, 3);
    private static final VoxelShape SHAPE = Shapes.or(box(2, 0, 2, 14, 9, 14), box(10, 9, 7, 13, 14, 10));

    /** Quarter turns per click. */
    public final int step;

    public QuernBlock(Properties properties) {
        this(properties, 1);
    }

    public QuernBlock(Properties properties, int step) {
        super(properties);
        this.step = step;
        registerDefaultState(defaultBlockState().setValue(TURN, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TURN);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Farming.GRAINS)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        int turn = (state.getValue(TURN) + step) % 4;
        server.setBlock(pos, state.setValue(TURN, turn), Block.UPDATE_CLIENTS);
        server.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.5F, 0.6F + server.getRandom().nextFloat() * 0.2F);
        server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, stack.getItem()), pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 3, 0.25, 0.05, 0.25, 0.02);
        if (turn == 0) {
            stack.consume(1, player);
            Block.popResourceFromFace(server, pos, hit.getDirection(), new ItemStack(Farming.FLOUR.get()));
        }
        return InteractionResult.SUCCESS;
    }
}
