package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Leather bellows with a Clay Tuyere, placed beside a Clay Kiln's firebox (or against a Smelting Furnace's wall) with the
 * nozzle (FACING) into it. Each use is one pump ({@link KilnBlockEntity#pump}, {@link FurnaceTapBlockEntity#pump}): the bellows squeeze shut for {@link #PUMP_TICKS} (also the cooldown).
 */
public class BellowsBlock extends Block {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty PRESSED = BooleanProperty.create("pressed");
    public static final int PUMP_TICKS = 10;
    private static final VoxelShape SHAPE = box(1, 0, 1, 15, 9, 15);

    /** An Iron Bellows (Smithing): pumps twice as hard and holds the heat after a pump. */
    private final boolean iron;

    public BellowsBlock(Properties properties) {
        this(properties, false);
    }

    public BellowsBlock(Properties properties, boolean iron) {
        super(properties);
        this.iron = iron;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PRESSED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PRESSED);
    }

    /** Nozzle away from the player: you stand behind the bellows and blow into whatever is in front. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(PRESSED)) return InteractionResult.CONSUME;
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        server.setBlock(pos, state.setValue(PRESSED, true), Block.UPDATE_ALL);
        server.scheduleTick(pos, this, PUMP_TICKS);
        Direction facing = state.getValue(FACING);
        server.playSound(null, pos, SoundEvents.ENDER_DRAGON_FLAP, SoundSource.BLOCKS, 0.35F, 1.7F + server.getRandom().nextFloat() * 0.2F);
        double nx = pos.getX() + 0.5 + facing.getStepX() * 0.55, nz = pos.getZ() + 0.5 + facing.getStepZ() * 0.55;
        server.sendParticles(ParticleTypes.POOF, nx, pos.getY() + 0.25, nz, 2, 0.02, 0.02, 0.02, 0.02);
        BlockPos target = pos.relative(facing);
        if (level.getBlockEntity(target) instanceof BloomeryBlockEntity bloomery) {
            if (!bloomery.pump(iron)) player.sendOverlayMessage(Component.translatable("message.mechanical.bloomery.unlit"));
            else { // the charge roars at the top of the shaft
                BlockPos top = target.above(3);
                server.sendParticles(ParticleTypes.FLAME, top.getX() + 0.5, top.getY() + 0.05, top.getZ() + 0.5, 8, 0.12, 0.1, 0.12, 0.04);
                server.sendParticles(ParticleTypes.LAVA, top.getX() + 0.5, top.getY() + 0.05, top.getZ() + 0.5, 2, 0.1, 0.1, 0.1, 0.0);
                server.playSound(null, target, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.25F, 0.6F + server.getRandom().nextFloat() * 0.2F);
            }
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(target) instanceof KilnBlockEntity kiln)) {
            FurnaceTapBlockEntity furnace = FurnaceTapBlockEntity.find(server, target);
            if (furnace == null) player.sendOverlayMessage(Component.translatable("message.mechanical.bellows.no_kiln"));
            else if (furnace.problem() != null) player.sendOverlayMessage(furnace.problem());
            else if (!furnace.pump(iron)) player.sendOverlayMessage(Component.translatable("message.mechanical.furnace.unlit"));
            else { // the fire roars up the shaft
                BlockPos top = furnace.shaft().above(3);
                server.sendParticles(ParticleTypes.FLAME, top.getX() + 0.5, top.getY() + 0.1, top.getZ() + 0.5, 8, 0.15, 0.1, 0.15, 0.04);
                server.sendParticles(ParticleTypes.LAVA, top.getX() + 0.5, top.getY() + 0.1, top.getZ() + 0.5, 2, 0.1, 0.1, 0.1, 0.0);
                server.playSound(null, target, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.25F, 0.6F + server.getRandom().nextFloat() * 0.2F);
            }
            return InteractionResult.SUCCESS;
        }
        if (!kiln.pump(server, iron)) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.bellows.unlit"));
            return InteractionResult.SUCCESS;
        }
        // the fire roars: flames and sparks out of the kiln's fire opening
        Direction front = server.getBlockState(target).getValue(KilnBlock.FACING);
        double fx = target.getX() + 0.5 + front.getStepX() * 0.5, fz = target.getZ() + 0.5 + front.getStepZ() * 0.5;
        server.sendParticles(ParticleTypes.FLAME, fx, target.getY() + 0.3, fz, 8, 0.15, 0.1, 0.15, 0.04);
        server.sendParticles(ParticleTypes.LAVA, fx, target.getY() + 0.4, fz, 2, 0.1, 0.1, 0.1, 0.0);
        server.sendParticles(ParticleTypes.LARGE_SMOKE, target.getX() + 0.5, target.getY() + 2.0, target.getZ() + 0.5, 3, 0.05, 0.05, 0.05, 0.03);
        server.playSound(null, target, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.25F, 0.6F + server.getRandom().nextFloat() * 0.2F);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(PRESSED)) {
            level.setBlock(pos, state.setValue(PRESSED, false), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.4F, 0.7F);
        }
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
