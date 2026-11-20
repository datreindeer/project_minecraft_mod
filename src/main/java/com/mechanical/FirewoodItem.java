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
 * Firewood of one wood type. Right-clicking the top of a plain solid block starts a {@link FirewoodPileBlock} there.
 * Item use only runs when the block clicked did nothing with it, so feeding campfires, Fire Pits, kilns and the tripod
 * (their own block / event handlers) comes first; blocks with a block entity (stations, fires) never get a pile on top,
 * and sneaking keeps the plain behaviour (nothing happens).
 */
public class FirewoodItem extends Item {
    static final net.minecraft.resources.Identifier WOODPILE = net.minecraft.resources.Identifier.fromNamespaceAndPath(Mechanical.MODID, "stone_age/woodpile");
    private final String wood;

    public FirewoodItem(Properties properties, String wood) {
        super(properties);
        this.wood = wood;
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
        if (!level.isClientSide()) {
            FirewoodPileBlock.place(level, at, wood, context.getHorizontalDirection().getAxis());
            context.getItemInHand().consume(1, player);
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) Kettle.award(sp, WOODPILE);
        }
        return InteractionResult.SUCCESS;
    }
}
