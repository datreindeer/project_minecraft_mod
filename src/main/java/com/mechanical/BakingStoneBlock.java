package com.mechanical;

import com.mechanical.skills.Skills;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A flat stone next to (or on) a burning campfire. Put Dough on it: after {@link #BAKE_TICKS} of fire it is Bread; left
 * {@link #BURN_TICKS} longer it burns. Empty hand takes it off. No fire: the dough waits. A Magma Block under it will do
 * for a fire, at half the speed ({@link #slowness}).
 */
public class BakingStoneBlock extends Block {
    public enum Contents implements StringRepresentable {
        EMPTY, DOUGH, BREAD, BURNT;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static final EnumProperty<Contents> CONTENTS = EnumProperty.create("contents", Contents.class);
    public static final int BAKE_TICKS = 600, BURN_TICKS = 900, NO_FIRE_RETRY = 100;
    private static final VoxelShape SHAPE = box(1, 0, 1, 15, 2, 15);

    public BakingStoneBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(CONTENTS, Contents.EMPTY));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CONTENTS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** A lit campfire beside it or under it. */
    public static boolean nearFire(Level level, BlockPos pos) {
        for (Direction d : Direction.values()) {
            if (d == Direction.UP) continue;
            BlockState s = level.getBlockState(pos.relative(d));
            if (s.getBlock() instanceof CampfireBlock && s.getValue(CampfireBlock.LIT) || FirePit.isLit(s)) return true;
        }
        return false;
    }

    /** How many times longer a step takes: 1 by a fire, 2 on nothing but a Magma Block (weaker than any fire). */
    public static int slowness(Level level, BlockPos pos) {
        return !nearFire(level, pos) && Magma.under(level, pos) ? 2 : 1;
    }

    /** A fire beside or under it, or a Magma Block under it. */
    public static boolean heated(Level level, BlockPos pos) {
        return nearFire(level, pos) || Magma.under(level, pos);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Farming.DOUGH.get()) || state.getValue(CONTENTS) != Contents.EMPTY) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level instanceof ServerLevel server) {
            stack.consume(1, player);
            server.setBlock(pos, state.setValue(CONTENTS, Contents.DOUGH), Block.UPDATE_ALL);
            server.scheduleTick(pos, this, BAKE_TICKS * slowness(server, pos));
            server.playSound(null, pos, SoundEvents.MUD_PLACE, SoundSource.BLOCKS, 0.6F, 1.2F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        Item out = switch (state.getValue(CONTENTS)) {
            case DOUGH -> Farming.DOUGH.get();
            case BREAD -> Items.BREAD;
            case BURNT -> Mechanical.BURNT_FOOD.get();
            case EMPTY -> null;
        };
        if (out == null) return InteractionResult.PASS;
        if (level instanceof ServerLevel server) {
            server.setBlock(pos, state.setValue(CONTENTS, Contents.EMPTY), Block.UPDATE_ALL);
            Block.popResource(server, pos.above(), new ItemStack(out));
            if (out == Items.BREAD) Skills.cooked(player, new ItemStack(out));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Contents c = state.getValue(CONTENTS);
        if (c != Contents.DOUGH && c != Contents.BREAD) return;
        if (!heated(level, pos)) {
            level.scheduleTick(pos, this, NO_FIRE_RETRY); // shortcut: baking progress isn't kept while the fire is out
            return;
        }
        level.setBlock(pos, state.setValue(CONTENTS, c == Contents.DOUGH ? Contents.BREAD : Contents.BURNT), Block.UPDATE_ALL);
        if (c == Contents.DOUGH) level.scheduleTick(pos, this, BURN_TICKS * slowness(level, pos));
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.3F, 1.6F);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(CONTENTS) != Contents.EMPTY && random.nextInt(3) == 0 && heated(level, pos)) {
            level.addParticle(state.getValue(CONTENTS) == Contents.BURNT ? ParticleTypes.SMOKE : ParticleTypes.WHITE_SMOKE,
                    pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.4, pos.getY() + 0.2, pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.4, 0, 0.02, 0);
        }
    }
}
