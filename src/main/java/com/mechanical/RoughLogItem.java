package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A Rough Log of one wood. Right-clicking the top of a plain solid block starts a {@link RoughLogPileBlock} there, the logs
 * lying with their ends towards the player. Item use only runs when the block clicked did nothing with it (a stump takes
 * the log first, see TreeChopping); blocks with a block entity never get a pile on top, and sneaking does nothing.
 */
public class RoughLogItem extends Item {
    private final String wood;

    public RoughLogItem(Properties properties, String wood) {
        super(properties);
        this.wood = wood;
    }

    public String wood() {
        return wood;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos below = context.getClickedPos(), at = below.above();
        BlockState ground = level.getBlockState(below);
        if (player == null || player.isSecondaryUseActive() || context.getClickedFace() != Direction.UP || ground.hasBlockEntity()
                || !ground.isFaceSturdy(level, below, Direction.UP) || !level.isInsideBuildHeight(at)
                || !level.getBlockState(at).canBeReplaced() || !level.getFluidState(at).isEmpty())
            return InteractionResult.PASS;
        return RoughLogPileBlock.stack(level, at, wood, context.getHorizontalDirection().getAxis(), player, context.getItemInHand());
    }
}
