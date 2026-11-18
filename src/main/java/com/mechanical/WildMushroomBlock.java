package com.mechanical;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A wild mushroom on the forest floor. Right-click (or break) to pick it. Can't be planted, but mushrooms slowly spread
 * to a nearby spot in the shade, as long as there aren't already a few of the same kind around.
 */
public class WildMushroomBlock extends VegetationBlock {
    private static final VoxelShape SHAPE = column(8, 0, 7);
    private static final int SPREAD_ONE_IN = 40, MAX_NEARBY = 3, MAX_LIGHT = 12;

    private final Supplier<Item> item;

    public WildMushroomBlock(Supplier<Item> item, Properties properties) {
        super(properties);
        this.item = item;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(SPREAD_ONE_IN) != 0) return;
        int nearby = 0;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-4, -1, -4), pos.offset(4, 1, 4))) {
            if (level.getBlockState(p).is(this) && ++nearby > MAX_NEARBY) return;
        }
        BlockPos target = pos.offset(random.nextInt(5) - 2, random.nextInt(3) - 1, random.nextInt(5) - 2);
        if (level.isEmptyBlock(target) && level.getMaxLocalRawBrightness(target) <= MAX_LIGHT && defaultBlockState().canSurvive(level, target)) {
            level.setBlock(target, defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server) {
            server.removeBlock(pos, false);
            server.playSound(null, pos, SoundEvents.GRASS_BREAK, SoundSource.BLOCKS, 0.7F, 1.3F);
            Block.popResource(server, pos, new ItemStack(item.get()));
        }
        return InteractionResult.SUCCESS;
    }
}
