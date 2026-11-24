package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A High Furnace wall block (see {@link HighFurnace}): Banded Furnace Casing, and with a facing ({@link Facing}) the
 * Firebox and the Taphole. {@link HighFurnace#HEAT} is set by the Front while the bottom ring glows. Placing or breaking
 * one tells the furnace it stands in to check itself on its next tick.
 */
public class HighFurnaceWallBlock extends Block {
    public HighFurnaceWallBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HighFurnace.HEAT, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HighFurnace.HEAT);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel server && !oldState.is(this)) HighFurnaceBlockEntity.wallChanged(server, pos);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        HighFurnaceBlockEntity.wallChanged(level, pos);
    }

    /**
     * Banded Furnace Casing. Loose, a plain brick cube; in a formed furnace the Front gives every wall block its
     * {@link Part} by layer, its outward {@link #FACING} and whether it ends a side next to an empty corner
     * ({@link #END_CW}, {@link #END_CCW}: a vertical iron corner strap there), so the furnace reads as a stone hearth
     * with arches, sooty brick courses held by thin iron tie straps, an inset upper shaft and an iron-rimmed corbelled top.
     */
    public static class Casing extends HighFurnaceWallBlock {
        public enum Part implements net.minecraft.util.StringRepresentable {
            /** Loose or not part of a formed furnace. */
            NONE,
            /** The bottom layer: rusticated stone with a plinth stepping out. */
            BASE,
            /** The layer above the hearth: heat-stained brick whose joints glow. */
            HOT,
            PLAIN,
            /** A course with an iron tie strap round it (every third layer). */
            STRAP,
            /** The upper shaft, set back a little (the bosh narrowing to the throat), sootier. */
            UPPER,
            UPPER_STRAP,
            /** The top layer: inset, a corbelled iron rim at the charging throat that glows inside when hot. */
            TOP;

            @Override
            public String getSerializedName() {
                return name().toLowerCase(java.util.Locale.ROOT);
            }
        }

        public static final net.minecraft.world.level.block.state.properties.EnumProperty<Part> PART =
                net.minecraft.world.level.block.state.properties.EnumProperty.create("part", Part.class);
        public static final net.minecraft.world.level.block.state.properties.EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
        public static final net.minecraft.world.level.block.state.properties.BooleanProperty END_CW =
                net.minecraft.world.level.block.state.properties.BooleanProperty.create("end_cw"),
                END_CCW = net.minecraft.world.level.block.state.properties.BooleanProperty.create("end_ccw");

        public Casing(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(HighFurnace.HEAT, 0).setValue(PART, Part.NONE).setValue(FACING, Direction.NORTH)
                    .setValue(END_CW, false).setValue(END_CCW, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(HighFurnace.HEAT, PART, FACING, END_CW, END_CCW);
        }

        /** The look of a wall block at layer {@code dy} of a furnace {@code layers} tall. */
        public static Part part(int dy, int layers) {
            if (dy == 0) return Part.BASE;
            if (dy == layers - 1) return Part.TOP;
            if (dy == 1) return Part.HOT;
            boolean strap = dy % 3 == 0, upper = dy >= (layers + 1) / 2;
            return upper ? strap ? Part.UPPER_STRAP : Part.UPPER : strap ? Part.STRAP : Part.PLAIN;
        }
    }

    /**
     * The Firebox (a cast-iron door with a grate and the bronze tuyere, facing out of the furnace) and the Taphole. A
     * Firebox takes Charcoal or Coal (up to {@link HighFurnaceBlockEntity#FUEL_MAX}) and is lit with a Fire Drill or
     * Flint and Steel: every Firebox of the furnace with fuel catches. Empty hand: its fuel and blast.
     */
    public static class Facing extends HighFurnaceWallBlock {
        public static final net.minecraft.world.level.block.state.properties.EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

        public Facing(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(HighFurnace.HEAT, 0).setValue(FACING, Direction.NORTH));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(HighFurnace.HEAT, FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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
        protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            if (!state.is(HighFurnace.FIREBOX.get())) return InteractionResult.TRY_WITH_EMPTY_HAND;
            boolean fuel = HighFurnaceBlockEntity.fuelOf(stack) != null, drill = stack.getItem() instanceof FireDrillItem, flint = stack.is(Items.FLINT_AND_STEEL) || stack.getItem() instanceof FlintAndSteelItem;
            if (!fuel && !drill && !flint) return InteractionResult.TRY_WITH_EMPTY_HAND;
            if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
            HighFurnaceBlockEntity furnace = HighFurnaceBlockEntity.find(server, pos);
            if (furnace == null || !furnace.isFirebox(pos)) {
                player.sendOverlayMessage(furnace != null && furnace.problem() != null ? furnace.problem()
                        : Component.translatable("message.mechanical.high_furnace.no_furnace"));
                return InteractionResult.FAIL;
            }
            if (fuel) {
                int taken = furnace.addFuel(pos, stack);
                if (taken == 0) {
                    player.sendOverlayMessage(Component.translatable("message.mechanical.high_furnace.firebox_full", HighFurnaceBlockEntity.FUEL_MAX));
                    return InteractionResult.FAIL;
                }
                if (!player.getAbilities().instabuild) stack.shrink(taken);
                server.playSound(null, pos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 0.6F, 0.7F);
                return InteractionResult.SUCCESS;
            }
            // a Fire Drill catches about as often as on a kiln without tinder; Flint and Steel every time
            boolean catches = flint || server.getRandom().nextFloat() < Tinder.CHANCE_WITHOUT;
            if (!furnace.canLight()) {
                player.sendOverlayMessage(Component.translatable("message.mechanical.high_furnace.needs_fuel"));
                return InteractionResult.FAIL;
            }
            if (catches) {
                furnace.light();
                server.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, 0.9F);
            } else {
                server.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 5, 0.2, 0.1, 0.2, 0.01);
                server.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 0.8F, 1.5F);
            }
            stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            return InteractionResult.SUCCESS;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!(level instanceof ServerLevel server) || !state.is(HighFurnace.FIREBOX.get())) return InteractionResult.PASS;
            HighFurnaceBlockEntity furnace = HighFurnaceBlockEntity.find(server, pos);
            player.sendOverlayMessage(furnace != null && furnace.isFirebox(pos) ? furnace.describeFirebox(pos)
                    : furnace != null && furnace.problem() != null ? furnace.problem() : Component.translatable("message.mechanical.high_furnace.no_furnace"));
            return InteractionResult.SUCCESS;
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            int heat = state.getValue(HighFurnace.HEAT);
            if (heat == 0 || !state.is(HighFurnace.FIREBOX.get())) return;
            Direction out = state.getValue(FACING);
            double x = pos.getX() + 0.5 + out.getStepX() * 0.52, z = pos.getZ() + 0.5 + out.getStepZ() * 0.52;
            if (random.nextInt(4 - heat + 1) == 0)
                level.addParticle(ParticleTypes.SMALL_FLAME, x + (random.nextDouble() - 0.5) * 0.4 * Math.abs(out.getStepZ()),
                        pos.getY() + 0.2 + random.nextDouble() * 0.2, z + (random.nextDouble() - 0.5) * 0.4 * Math.abs(out.getStepX()), 0, 0.01, 0);
            if (random.nextInt(24) == 0) level.playLocalSound(pos, SoundEvents.BLASTFURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.7F, 0.8F, false);
        }
    }
}
