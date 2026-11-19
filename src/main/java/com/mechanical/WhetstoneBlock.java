package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A sandstone whetstone on a wooden bench. Use a Rough Cast head on it {@link #STROKES} times (sparks, grinding
 * sound) to grind one into a head the Tool Station takes. Progress lives on the block (GRIND), so one piece at a time.
 */
public class WhetstoneBlock extends Block {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final int STROKES = 4;
    public static final IntegerProperty GRIND = IntegerProperty.create("grind", 0, STROKES - 1);
    private static final VoxelShape SHAPE = Shapes.or(box(1, 0, 3, 15, 8, 13), box(3, 8, 4, 13, 12, 12));

    public WhetstoneBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(GRIND, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, GRIND);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack ground = Casting.ground(stack);
        if (ground.isEmpty()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (player.getCooldowns().isOnCooldown(stack)) return InteractionResult.CONSUME;
        player.getCooldowns().addCooldown(stack, 6);
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        double x = pos.getX() + 0.5, y = pos.getY() + 0.8, z = pos.getZ() + 0.5;
        server.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 6, 0.2, 0.05, 0.2, 0.15);
        server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, stack.getItem()), x, y, z, 3, 0.15, 0.05, 0.15, 0.05);
        server.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.7F, 1.1F + server.getRandom().nextFloat() * 0.3F);
        int grind = state.getValue(GRIND) + 1;
        if (grind < STROKES) {
            server.setBlock(pos, state.setValue(GRIND, grind), Block.UPDATE_ALL);
            return InteractionResult.SUCCESS;
        }
        server.setBlock(pos, state.setValue(GRIND, 0), Block.UPDATE_ALL);
        stack.consume(1, player);
        player.getInventory().placeItemBackInInventory(ground, net.minecraft.util.Prediction.SERVER_ONLY);
        server.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.6F, 1.4F);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
