package com.mechanical;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * The Water Jug in hand: dip it in water to fill it ({@link WaterJugBlock#MAX} pours), pour on farmland to wet it and the
 * 8 around it (one pour, like the Clay Water Bucket), otherwise it is placed as a block. The level rides on the item as
 * the block_state component, so a jug picked up keeps its water.
 */
public class WaterJugItem extends BlockItem {
    public WaterJugItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static int water(ItemStack stack) {
        Integer n = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY).get(WaterJugBlock.WATER);
        return n == null ? 0 : n;
    }

    static ItemStack withWater(ItemStack stack, int water) {
        ItemStack out = stack.copyWithCount(1);
        out.set(DataComponents.BLOCK_STATE, stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY).with(WaterJugBlock.WATER, water));
        return out;
    }

    /** Looking at a water source (through the water, unlike block clicks): fills the jug. */
    private static InteractionResult fill(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        // full, it's placed as usual
        if (hit.getType() != HitResult.Type.BLOCK || !level.getFluidState(hit.getBlockPos()).is(FluidTags.WATER) || water(stack) >= WaterJugBlock.MAX) return null;
        player.playSound(SoundEvents.BUCKET_FILL, 1.0F, 0.9F);
        if (!level.isClientSide()) player.setItemInHand(hand, withWater(stack, WaterJugBlock.MAX));
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return super.useOn(context);
        InteractionResult filled = fill(context.getLevel(), player, context.getHand());
        if (filled != null) return filled;
        ItemStack stack = context.getItemInHand();
        BlockPos farmland = ClayBucketItem.farmland(context.getLevel(), context.getClickedPos());
        if (farmland == null || water(stack) == 0) return super.useOn(context);
        if (context.getLevel() instanceof ServerLevel server) {
            ClayBucketItem.water(server, farmland);
            if (!player.hasInfiniteMaterials()) player.setItemInHand(context.getHand(), withWater(stack, water(stack) - 1));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        InteractionResult filled = fill(level, player, hand);
        return filled != null ? filled : super.use(level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.mechanical.water_jug", water(stack), WaterJugBlock.MAX).withStyle(ChatFormatting.GRAY));
    }
}
