package com.mechanical.wildlife.coast;

import com.mechanical.Mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Oil Lamp: a hollowed stone dish of rendered fat with a moss-and-fibre wick along its lip, as the arctic hunters kept
 * them. Right-click with lamp fuel (Blubber; Tallow, Lard or Bear Fat burn too) to fill it, up to {@link #MAX_FUEL}; light
 * it with a Fire Drill, Flint and Steel or a Torch (empty hand puts it out). A lit lamp gives a steady light 14 and burns
 * one fill in about seven minutes (random ticks), then goes out. Rain does it no harm: it is meant for indoors and tents.
 */
public class OilLampBlock extends Block {
    public static final int MAX_FUEL = 4, LIGHT = 14;
    public static final IntegerProperty FUEL = IntegerProperty.create("fuel", 0, MAX_FUEL);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    /** Per random tick (~68 s): about 7 minutes per fill. */
    private static final float BURN_CHANCE = 1.0F / 6.0F;
    private static final VoxelShape SHAPE = box(4, 0, 4, 12, 4, 12);

    public OilLampBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FUEL, 0).setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FUEL, LIT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return canSupportCenter(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, net.minecraft.world.level.ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        return direction == Direction.DOWN && !canSurvive(state, level, pos) ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
                : super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    private static boolean lighter(ItemStack stack) {
        return stack.is(Mechanical.FIRE_DRILL.get()) || stack.is(Items.FLINT_AND_STEEL) || stack.is(Items.TORCH);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.is(CoastFauna.LAMP_FUEL)) {
            if (state.getValue(FUEL) >= MAX_FUEL) return InteractionResult.FAIL;
            if (!level.isClientSide()) {
                stack.consume(1, player);
                level.setBlock(pos, state.setValue(FUEL, state.getValue(FUEL) + 1), Block.UPDATE_ALL);
                level.playSound(null, pos, SoundEvents.HONEY_BLOCK_PLACE, SoundSource.BLOCKS, 0.6F, 0.8F);
            }
            return InteractionResult.SUCCESS;
        }
        if (lighter(stack) && !state.getValue(LIT)) {
            if (state.getValue(FUEL) == 0) return InteractionResult.FAIL;
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL);
                level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.5F, 1.6F);
                if (stack.isDamageableItem()) stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    /** Empty hand: snuff it out (the oil stays). */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!state.getValue(LIT)) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            level.setBlock(pos, state.setValue(LIT, false), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(LIT);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() >= BURN_CHANCE) return;
        int fuel = state.getValue(FUEL) - 1;
        level.setBlock(pos, state.setValue(FUEL, Math.max(0, fuel)).setValue(LIT, fuel > 0), Block.UPDATE_ALL);
        if (fuel <= 0) level.playSound(null, pos, SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 0.8F, 0.8F);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) return;
        double x = pos.getX() + 0.5, y = pos.getY() + 0.42, z = pos.getZ() + 0.3;
        if (random.nextInt(3) == 0) level.addParticle(ParticleTypes.SMALL_FLAME, x, y, z, 0.0, 0.0, 0.0);
        if (random.nextInt(6) == 0) level.addParticle(ParticleTypes.SMOKE, x, y + 0.15, z, 0.0, 0.01, 0.0);
    }
}
