package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The hub of a Water Wheel (see {@link WaterPower}): the only real block of the wheel; the 5-block wheel round it is drawn
 * by client/WaterPowerRenderers and turns in the plane across {@link #AXIS}. Placed facing the player, and only where the
 * 24 cells round the hub are free (air, water, grass); its solid body is {@link WaterWheelHitbox}. Right-click with an
 * empty hand: its power (and the axle's, when other wheels share it), or what stops it.
 */
public class WaterWheelBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    private static final VoxelShape X = box(0, 2, 2, 16, 14, 14), Z = box(2, 2, 0, 14, 14, 16);

    public WaterWheelBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction.Axis axis = context.getHorizontalDirection().getAxis();
        Level level = context.getLevel();
        for (int[] c : WaterWheelBlockEntity.SWEPT) {
            BlockPos p = WaterWheelBlockEntity.cell(context.getClickedPos(), axis, c[0], c[1]);
            if (!level.getBlockState(p).canBeReplaced()) {
                if (context.getPlayer() != null && !level.isClientSide())
                    context.getPlayer().sendOverlayMessage(Component.translatable("message.mechanical.water_wheel.no_room",
                            level.getBlockState(p).getBlock().getName(), p.getX() + " " + p.getY() + " " + p.getZ()));
                return null;
            }
        }
        return defaultBlockState().setValue(AXIS, axis);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90
                ? state.setValue(AXIS, state.getValue(AXIS) == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X) : state;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AXIS) == Direction.Axis.X ? X : Z;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel server) || !(level.getBlockEntity(pos) instanceof WaterWheelBlockEntity wheel)) return InteractionResult.SUCCESS;
        float sum = wheel.measure(server);
        int power = Math.min(WaterPower.MAX_POWER, Math.round(Math.abs(sum)));
        BlockPos b = wheel.blocked;
        if (b != null) player.sendOverlayMessage(Component.translatable("message.mechanical.water_wheel.blocked",
                level.getBlockState(b).getBlock().getName(), b.getX() + " " + b.getY() + " " + b.getZ()));
        else if (power == 0) player.sendOverlayMessage(Component.translatable("message.mechanical.water_wheel.no_flow"));
        else if (wheel.power() != power) // other wheels on the axle add to (or take from) its own
            player.sendOverlayMessage(Component.translatable("message.mechanical.water_wheel.power_line", power, wheel.power(), WaterPower.MAX_POWER));
        else player.sendOverlayMessage(Component.translatable("message.mechanical.water_wheel.power", power, WaterPower.MAX_POWER));
        return InteractionResult.SUCCESS;
    }

    /** Splashes where the paddles dip into water, and now and then the groan of the axle. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof WaterWheelBlockEntity wheel) || wheel.speed() == 0) return;
        Direction.Axis axis = state.getValue(AXIS);
        for (int i = 0; i < 3; i++) {
            int[] c = WaterWheelBlockEntity.SWEPT.get(random.nextInt(WaterWheelBlockEntity.SWEPT.size()));
            BlockPos p = WaterWheelBlockEntity.cell(pos, axis, c[0], c[1]);
            if (!level.getFluidState(p).is(FluidTags.WATER)) continue;
            double x = p.getX() + random.nextDouble(), y = p.getY() + 0.5 + random.nextDouble() * 0.5, z = p.getZ() + random.nextDouble();
            level.addParticle(ParticleTypes.SPLASH, x, y, z, 0, 0.1, 0);
            if (random.nextInt(3) == 0) level.addParticle(ParticleTypes.FALLING_WATER, x, y + 0.4, z, 0, 0, 0);
        }
        if (random.nextInt(12) == 0)
            level.playLocalSound(pos, SoundEvents.WOOD_STEP, SoundSource.BLOCKS, 0.35F, 0.45F + random.nextFloat() * 0.15F, false);
        if (random.nextInt(8) == 0)
            level.playLocalSound(pos, SoundEvents.WATER_AMBIENT, SoundSource.BLOCKS, 0.25F, 1.2F + random.nextFloat() * 0.3F, false);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WaterWheelBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != WaterPower.WATER_WHEEL_ENTITY.get()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<WaterWheelBlockEntity>) (l, p, s, w) -> WaterWheelBlockEntity.serverTick((ServerLevel) l, p, w);
    }
}
