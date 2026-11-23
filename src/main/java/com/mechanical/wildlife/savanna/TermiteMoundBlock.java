package com.mechanical.wildlife.savanna;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Termite Mound: hard, baked red earth riddled with tunnels; savanna worldgen stacks it into spires 2-4 blocks tall.
 * Poke a Stick into it (right-click) and now and then ({@link #CHANCE}) it comes out covered in Termites. Aardvarks dig
 * at the foot of mounds at night. Breaks into itself. The top block of a spire ({@link #TIP}: nothing of the mound above it)
 * narrows to a stepped, knobbly point.
 */
public class TermiteMoundBlock extends Block {
    static final float CHANCE = 0.35F;
    public static final BooleanProperty TIP = BooleanProperty.create("tip");
    /** Matches models/block/termite_mound_tip.json (without its little side turret). */
    private static final VoxelShape TIP_SHAPE = Shapes.or(Block.box(1, 0, 1, 15, 6, 15), Block.box(3, 6, 3, 13, 11, 13), Block.box(5, 11, 5, 11, 14, 11),
            Block.box(6.5, 14, 6.5, 9.5, 16, 9.5));

    public TermiteMoundBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(TIP, true)); // a lone block is a spire's top
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TIP);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(TIP, !context.getLevel().getBlockState(context.getClickedPos().above()).is(this));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction toNeighbour,
            BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
        return toNeighbour == Direction.UP ? state.setValue(TIP, !neighbour.is(this)) : state;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(TIP) ? TIP_SHAPE : Shapes.block();
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
