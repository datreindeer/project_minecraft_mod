package com.mechanical.wildlife.temperate;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Grub Log: the soft, rotten wood of a dead standing tree, riddled with beetle larvae (generated as snags in the woods).
 * Woodpeckers keep to these and drum on them ({@link Woodpecker}). Right-click with an empty hand to dig out the Grubs
 * (bait for snares and the hook); they come back over time ({@link #REFILL_CHANCE} per random tick, about once a minute).
 * Not a log for the axe: it crumbles (a stick or two and a grub).
 */
public class GrubLogBlock extends Block {
    public static final int MAX_GRUBS = 2;
    public static final IntegerProperty GRUBS = IntegerProperty.create("grubs", 0, MAX_GRUBS);
    static final float REFILL_CHANCE = 0.15F;

    public GrubLogBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(GRUBS, MAX_GRUBS));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GRUBS);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(GRUBS) < MAX_GRUBS;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() < REFILL_CHANCE) level.setBlock(pos, state.setValue(GRUBS, state.getValue(GRUBS) + 1), Block.UPDATE_ALL);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        int grubs = state.getValue(GRUBS);
        if (grubs == 0) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            player.getInventory().placeItemBackInInventory(new ItemStack(TemperateFauna.GRUB.get(), grubs), Prediction.SERVER_ONLY);
            level.setBlock(pos, state.setValue(GRUBS, 0), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.ROOTED_DIRT_BREAK, SoundSource.BLOCKS, 0.8F, 0.8F);
        }
        return InteractionResult.SUCCESS;
    }
}
