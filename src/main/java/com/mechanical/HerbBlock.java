package com.mechanical;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A wild herb (Herbs.HERB_KINDS): right-click with a knife to cut 1-3 of it (by hand 1), and it stands cut back until it
 * grows again ({@link #REGROW} per random tick, not in winter). Broken, the plant itself drops, to be planted in a herb
 * garden.
 */
public class HerbBlock extends WildPlantBlock {
    public static final BooleanProperty CUT = BooleanProperty.create("cut");
    /** Chance per random tick that a cut herb has grown back (about 4 random ticks, a few minutes). */
    public static final float REGROW = 0.25F;
    private final Supplier<Item> gives;

    public HerbBlock(Supplier<Item> gives, Properties properties) {
        super(properties);
        this.gives = gives;
        registerDefaultState(stateDefinition.any().setValue(CUT, false));
    }

    public Item gives() {
        return gives.get();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CUT);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.isEmpty() && !stack.is(Watercraft.KNIVES)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (state.getValue(CUT)) return InteractionResult.PASS;
        if (level instanceof ServerLevel server) {
            boolean knife = stack.is(Watercraft.KNIVES);
            popResource(server, pos, cut(server.getRandom(), knife));
            server.setBlock(pos, state.setValue(CUT, true), Block.UPDATE_ALL);
            server.playSound(null, pos, knife ? SoundEvents.SHEEP_SHEAR : SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 0.7F, 1.3F);
            if (knife) stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
        return useItemOn(ItemStack.EMPTY, state, level, pos, player, InteractionHand.MAIN_HAND, hit);
    }

    /** What one cut gives: a knife 1-3, picked by hand 1. */
    public ItemStack cut(RandomSource random, boolean knife) {
        return new ItemStack(gives.get(), knife ? 1 + random.nextInt(3) : 1);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(CUT) && Calendar.season(level.getOverworldClockTime()) != 3 && random.nextFloat() < REGROW) {
            level.setBlock(pos, state.setValue(CUT, false), Block.UPDATE_CLIENTS);
        }
    }
}
