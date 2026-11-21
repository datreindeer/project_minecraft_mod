package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Bloomery's hearth, the bottom of the shaft: the slag-tapping arch in front (FACING, toward the player who set it),
 * the clay tuyere on the back. Use: the bloomery's screen, or what is missing. A Fire Drill lights it; Bellows blow
 * into it from any side. Controller: {@link BloomeryBlockEntity}.
 */
public class BloomeryHearthBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public BloomeryHearthBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof FireDrillItem) return InteractionResult.PASS; // the drill lights it
        if (stack.is(Ironworking.FLUX)) { // limestone flux: the next bloom one ingot richer
            if (!(level instanceof ServerLevel server) || !(level.getBlockEntity(pos) instanceof BloomeryBlockEntity bloomery)) return InteractionResult.SUCCESS;
            if (!bloomery.addFlux()) {
                player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.mechanical.bloomery.flux_full", Ironworking.MAX_FLUX));
                return InteractionResult.FAIL;
            }
            stack.consume(1, player);
            server.playSound(null, pos, net.minecraft.sounds.SoundEvents.GRAVEL_PLACE, net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 1.2F);
            player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.mechanical.bloomery.flux", bloomery.flux(), Ironworking.MAX_FLUX));
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel server) || !(level.getBlockEntity(pos) instanceof BloomeryBlockEntity bloomery)) return InteractionResult.SUCCESS;
        var problem = bloomery.check(server);
        if (problem != null) player.sendOverlayMessage(problem);
        else player.openMenu(bloomery);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        for (int i = 1; i <= 2; i++) { // the stacks go dark with it
            BlockState s = level.getBlockState(pos.above(i));
            if (s.is(Ironworking.BLOOMERY_STACK.get()) && s.getValue(BloomeryStackBlock.LIT))
                level.setBlock(pos.above(i), s.setValue(BloomeryStackBlock.LIT, false), Block.UPDATE_ALL);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) return;
        Direction front = state.getValue(FACING);
        double x = pos.getX() + 0.5 + front.getStepX() * 0.52, z = pos.getZ() + 0.5 + front.getStepZ() * 0.52;
        if (random.nextInt(4) == 0) level.addParticle(ParticleTypes.SMALL_FLAME, x, pos.getY() + 0.3, z, 0, 0.01, 0);
        if (random.nextInt(14) == 0) level.addParticle(ParticleTypes.LAVA, x, pos.getY() + 0.25, z, 0, 0, 0);
        if (random.nextInt(16) == 0) level.playLocalSound(pos, SoundEvents.BLASTFURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.8F, 0.8F, false);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BloomeryBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != Ironworking.BLOOMERY_ENTITY.get()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<BloomeryBlockEntity>) (l, pos, s, b) -> BloomeryBlockEntity.serverTick((ServerLevel) l, b);
    }
}
