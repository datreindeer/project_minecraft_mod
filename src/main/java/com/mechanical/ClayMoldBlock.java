package com.mechanical;

import java.util.Locale;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Fired clay mold lying on the ground. Use a Crucible of Molten Metal on an empty one to pour ({@link Shape#units}
 * units); after {@link #SET_TICKS} the metal has set and an empty hand takes the casting out. The clay survives
 * {@link #MAX_USES} casts, then cracks. USES is kept on the item when the mold is picked up (loot copy_state).
 */
public class ClayMoldBlock extends Block {
    public enum Shape implements StringRepresentable {
        INGOT(2), PICKAXE_HEAD(6), AXE_HEAD(6), SHOVEL_HEAD(2), HOE_HEAD(4), KNIFE_BLADE(2), SAW_BLADE(4), PLATE(4), SPEAR_HEAD(4), SWORD_BLADE(6), KETTLE(8), ARROWHEADS(2);

        /** Metal units one cast takes (a Crucible holds 10; one Crushed Copper Ore is 1). */
        public final int units;

        Shape(int units) {
            this.units = units;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public enum Fill implements StringRepresentable {
        EMPTY, MOLTEN, SOLID;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final EnumProperty<Fill> FILL = EnumProperty.create("fill", Fill.class);
    /** The metal poured in: its colour shows in the cavity, and it decides what comes out. */
    public static final EnumProperty<Metal> METAL = EnumProperty.create("metal", Metal.class, Metal.COPPER, Metal.BRONZE, Metal.POOR_BRONZE);
    public static final int MAX_USES = 5;
    public static final IntegerProperty USES = IntegerProperty.create("uses", 0, MAX_USES - 1);
    public static final int SET_TICKS = 100;
    private static final VoxelShape SHAPE = box(1, 0, 1, 15, 3, 15);

    public final Shape shape;

    public ClayMoldBlock(Properties properties, Shape shape) {
        super(properties);
        this.shape = shape;
        registerDefaultState(stateDefinition.any().setValue(FILL, Fill.EMPTY).setValue(USES, 0).setValue(METAL, Metal.COPPER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FILL, USES, METAL);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
            BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        return direction == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Casting.MOLTEN_CRUCIBLE.get()) || state.getValue(FILL) != Fill.EMPTY) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        if (CrucibleItem.isCold(stack, server.getGameTime())) {
            player.setItemInHand(hand, CrucibleItem.solidify(stack));
            player.sendOverlayMessage(Component.translatable("message.mechanical.mold.set_in_crucible"));
            return InteractionResult.SUCCESS;
        }
        CrucibleContents contents = CrucibleItem.contents(stack);
        Metal metal = contents.alloy();
        if (metal == null || !metal.casts(shape)) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.mold.wrong_metal",
                    Component.translatable("metal.mechanical." + (metal == null ? "tin" : metal.getSerializedName()))));
            return InteractionResult.SUCCESS;
        }
        if (contents.total() < shape.units) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.mold.not_enough", shape.units, contents.total()));
            return InteractionResult.SUCCESS;
        }
        CrucibleContents left = contents.add(metal, -shape.units);
        if (left.isEmpty()) player.setItemInHand(hand, new ItemStack(Mechanical.CRUCIBLE.get()));
        else stack.set(Casting.CRUCIBLE_CONTENTS.get(), left);
        server.setBlock(pos, state.setValue(FILL, Fill.MOLTEN).setValue(METAL, metal), Block.UPDATE_ALL);
        server.scheduleTick(pos, this, SET_TICKS);
        server.playSound(null, pos, SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.BLOCKS, 0.8F, 1.2F);
        server.sendParticles(ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 0.25, pos.getZ() + 0.5, 4, 0.2, 0.05, 0.2, 0.0);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        Fill fill = state.getValue(FILL);
        if (fill == Fill.EMPTY) return InteractionResult.PASS;
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        if (fill == Fill.MOLTEN) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.mold.molten"));
            return InteractionResult.SUCCESS;
        }
        player.getInventory().placeItemBackInInventory(state.getValue(METAL).cast(shape), net.minecraft.util.Prediction.SERVER_ONLY);
        int uses = state.getValue(USES) + 1;
        if (uses >= MAX_USES) {
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 0.15, pos.getZ() + 0.5, 20, 0.3, 0.05, 0.3, 0.0);
            server.playSound(null, pos, SoundEvents.DECORATED_POT_SHATTER, SoundSource.BLOCKS, 0.8F, 1.0F);
            server.removeBlock(pos, false);
            player.sendOverlayMessage(Component.translatable("message.mechanical.mold.cracked"));
        } else {
            server.setBlock(pos, state.setValue(FILL, Fill.EMPTY).setValue(USES, uses), Block.UPDATE_ALL);
            server.playSound(null, pos, SoundEvents.DECORATED_POT_HIT, SoundSource.BLOCKS, 0.6F, 1.3F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(FILL) != Fill.MOLTEN) return;
        level.setBlock(pos, state.setValue(FILL, Fill.SOLID), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 1.6F);
        level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.25, pos.getZ() + 0.5, 6, 0.2, 0.05, 0.2, 0.01);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (state.getValue(FILL) == Fill.MOLTEN && level instanceof ServerLevel server && !entity.isSteppingCarefully())
            entity.hurtServer(server, level.damageSources().hotFloor(), 1.0F);
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(FILL) != Fill.MOLTEN) return;
        if (random.nextInt(3) == 0) level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.2,
                pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0, 0.02, 0);
        if (random.nextInt(10) == 0) level.addParticle(ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 0, 0, 0);
    }
}
