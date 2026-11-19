package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A log being hollowed into a Dugout Canoe, the old way: hew, burn, scrape out the char, burn again, hew, then smooth it
 * with a knife. Made by scoring a log lying on its side with a knife (see {@link Watercraft}).
 * Stages: 0 scored, 1 hewn (needs fire), 2 charred, 3 hollowed (needs fire), 4 charred deep, 5 carved (knife finishes it).
 * Axe stages are chopped like fallen logs (hold right-click), {@link #AXE_CHOP_FACTOR} times as many chops.
 */
public class DugoutBlankBlock extends Block {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 5);
    public static final BooleanProperty BURNING = BooleanProperty.create("burning");
    public static final int LAST_STAGE = 5;
    /** A burn lasts 30 seconds. */
    public static final int BURN_TICKS = 600;
    public static final int AXE_CHOP_FACTOR = 2;
    public static final int KNIFE_STROKES = 4;
    /** Depth of the hollow per stage (pixels from the top), and the wall thickness. Mirrors the block models in gen_watercraft.py. */
    static final int[] DEPTH = {0, 3, 3, 7, 7, 11};
    private static final VoxelShape[] SHAPES_Z = new VoxelShape[6], SHAPES_X = new VoxelShape[6];

    static {
        for (int stage = 0; stage <= LAST_STAGE; stage++) {
            int d = DEPTH[stage], wall = stage == LAST_STAGE ? 2 : 3;
            SHAPES_Z[stage] = d == 0 ? Shapes.block() : Shapes.join(Shapes.block(), box(wall, 16 - d, 2, 16 - wall, 16, 14), BooleanOp.ONLY_FIRST);
            SHAPES_X[stage] = d == 0 ? Shapes.block() : Shapes.join(Shapes.block(), box(2, 16 - d, wall, 14, 16, 16 - wall), BooleanOp.ONLY_FIRST);
        }
    }

    public DugoutBlankBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(AXIS, Direction.Axis.Z).setValue(STAGE, 0).setValue(BURNING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS, STAGE, BURNING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(AXIS, context.getHorizontalDirection().getAxis());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return (state.getValue(AXIS) == Direction.Axis.X ? SHAPES_X : SHAPES_Z)[state.getValue(STAGE)];
    }

    static boolean needsFire(int stage) {
        return stage == 1 || stage == 3;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        int stage = state.getValue(STAGE);
        if (state.getValue(BURNING)) {
            if (stack.is(ItemTags.AXES) || stack.is(Watercraft.KNIVES)) {
                if (!level.isClientSide()) player.sendOverlayMessage(Component.translatable("message.mechanical.dugout.burning"));
                return InteractionResult.FAIL;
            }
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (stack.is(ItemTags.AXES)) {
            if (stage == LAST_STAGE || needsFire(stage)) {
                if (!level.isClientSide()) player.sendOverlayMessage(Component.translatable(stage == LAST_STAGE
                        ? "message.mechanical.dugout.needs_knife" : "message.mechanical.dugout.needs_fire"));
                return InteractionResult.FAIL;
            }
            if (level instanceof ServerLevel server
                    && TreeChopping.chop(server, player, hand, pos, state, stack, TreeChopping.chopsNeeded(stack) * AXE_CHOP_FACTOR)) {
                // wood chips fly; the charred layers come away black
                Block.popResource(server, pos.above(), new ItemStack(Items.STICK, 1 + server.getRandom().nextInt(2)));
                server.setBlock(pos, state.setValue(STAGE, stage + 1), Block.UPDATE_ALL);
            }
            return InteractionResult.CONSUME;
        }
        if (stack.is(Watercraft.FIRE_STARTERS)) {
            if (!needsFire(stage)) return InteractionResult.TRY_WITH_EMPTY_HAND;
            if (level instanceof ServerLevel server) {
                // a fire drill can fail, but the fresh chips in the hollow are good tinder
                boolean drill = stack.getItem() instanceof FireDrillItem;
                if (!drill || server.getRandom().nextFloat() < Tinder.CHANCE_WITH) {
                    server.setBlock(pos, state.setValue(BURNING, true), Block.UPDATE_ALL);
                    server.scheduleTick(pos, this, BURN_TICKS);
                    server.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, 1.2F);
                } else {
                    server.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 5, 0.2, 0.1, 0.2, 0.01);
                    server.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 0.8F, 1.5F);
                }
                if (stack.isDamageableItem()) stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.is(Watercraft.KNIVES)) {
            if (stage != LAST_STAGE) {
                if (!level.isClientSide()) player.sendOverlayMessage(Component.translatable(needsFire(stage)
                        ? "message.mechanical.dugout.needs_fire" : "message.mechanical.dugout.needs_axe"));
                return InteractionResult.FAIL;
            }
            if (level instanceof ServerLevel server && TreeChopping.chop(server, player, hand, pos, state, stack, KNIFE_STROKES)) {
                server.removeBlock(pos, false);
                server.playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.0F, 1.2F);
                Block.popResource(server, pos, new ItemStack(Watercraft.DUGOUT_CANOE_ITEM.get()));
            }
            return InteractionResult.CONSUME;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    /** The burn is over: the hollow is charred, ready to be scraped out. Rain puts it out without charring it. */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(BURNING)) return;
        boolean rained = level.isRainingAt(pos.above());
        level.setBlock(pos, state.setValue(BURNING, false).setValue(STAGE, rained ? state.getValue(STAGE) : state.getValue(STAGE) + 1), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 1.0F);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(BURNING)) return;
        double y = pos.getY() + 1 - DEPTH[state.getValue(STAGE)] / 16.0;
        for (int i = 0; i < 2; i++)
            level.addParticle(ParticleTypes.FLAME, pos.getX() + 0.3 + random.nextDouble() * 0.4, y, pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0, 0.01, 0);
        if (random.nextInt(3) == 0)
            level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5, y + 0.2, pos.getZ() + 0.5, 0, 0.05, 0);
        if (random.nextInt(12) == 0)
            level.playLocalSound(pos, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.8F, 1.0F, false);
    }
}
