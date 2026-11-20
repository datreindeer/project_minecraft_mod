package com.mechanical.wildlife.giants;

import java.util.function.Supplier;

import com.mechanical.BearRugBlock;
import com.mechanical.DryingRackBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A raw hide of the big Ice Age animals, too big for a Drying Rack: it is pegged out flat on the ground, fur side down,
 * and cures in the sun (the block is placed by the raw hide item itself). Daylight, open sky and no rain cure it; night
 * and rain just pause it. Once {@link #CURED} an empty hand takes the cured hide off the pegs; breaking it gives back
 * whatever it is at the moment (loot table).
 */
public class PeggedHideBlock extends BearRugBlock {
    public static final BooleanProperty CURED = BooleanProperty.create("cured");
    /** Sky light that still counts as out in the open (as the Drying Rack). */
    private static final int OUTDOORS_SKYLIGHT = 10;
    private final float cureChance;
    private final Supplier<? extends Item> cured;

    /** @param cureChance per random tick in dry daylight (a random tick comes every ~68 s on average) */
    public PeggedHideBlock(Properties properties, float cureChance, Supplier<? extends Item> cured) {
        super(properties);
        this.cureChance = cureChance;
        this.cured = cured;
        registerDefaultState(defaultBlockState().setValue(CURED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(CURED);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return !state.getValue(CURED);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean dry = DryingRackBlock.inDaylight(level, pos) && level.getBrightness(LightLayer.SKY, pos) >= OUTDOORS_SKYLIGHT
                && !level.isRainingAt(pos.above());
        if (dry && random.nextFloat() < cureChance) level.setBlock(pos, state.setValue(CURED, true), Block.UPDATE_ALL);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!state.getValue(CURED)) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            level.removeBlock(pos, false);
            player.getInventory().placeItemBackInInventory(new ItemStack(cured.get()), net.minecraft.util.Prediction.SERVER_ONLY);
            level.playSound(null, pos, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 1.0F, 0.8F);
        }
        return InteractionResult.SUCCESS;
    }
}
