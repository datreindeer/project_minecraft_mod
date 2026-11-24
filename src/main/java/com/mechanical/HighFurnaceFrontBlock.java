package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The High Furnace Front (see {@link HighFurnace}): a steel door with hinges and a round sight hatch, in the bottom
 * ring, facing out. It is the controller ({@link HighFurnaceBlockEntity}). Right-click: the furnace's screen when it is
 * whole, else what is wrong. The hatch glows with {@link HighFurnace#HEAT}. Client: the hatch's shimmer (the open top's
 * flames, sparks and roar: {@link HighFurnaceBlockEntity#clientTick}).
 */
public class HighFurnaceFrontBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    public HighFurnaceFrontBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HighFurnace.HEAT, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HighFurnace.HEAT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel server) || !(level.getBlockEntity(pos) instanceof HighFurnaceBlockEntity furnace)) return InteractionResult.SUCCESS;
        furnace.check(server);
        if (furnace.problem() != null) player.sendOverlayMessage(furnace.problem());
        else player.openMenu(furnace, buf -> buf.writeBlockPos(pos));
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

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        Direction out = state.getValue(FACING);
        if (state.getValue(HighFurnace.HEAT) > 0 && random.nextInt(2) == 0) // heat shimmer off the sight hatch
            level.addParticle(ParticleTypes.WHITE_ASH, pos.getX() + 0.5 + out.getStepX() * 0.55, pos.getY() + 0.75, pos.getZ() + 0.5 + out.getStepZ() * 0.55, 0, 0.02, 0);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HighFurnaceBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != HighFurnace.HIGH_FURNACE_ENTITY.get()) return null;
        if (level.isClientSide()) return (BlockEntityTicker<T>) (BlockEntityTicker<HighFurnaceBlockEntity>) (l, p, s, f) -> f.clientTick(l);
        return (BlockEntityTicker<T>) (BlockEntityTicker<HighFurnaceBlockEntity>) (l, p, s, f) -> f.serverTick((ServerLevel) l);
    }
}
