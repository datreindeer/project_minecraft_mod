package com.mechanical;

import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;

/**
 * The Bronze Bucket: carries water (never lava) like the vanilla bucket and never wears out (the Clay Bucket cracks). Pouring
 * is vanilla's logic with the vanilla bucket it hands back swapped for the bronze one; scooping is done here so a stack of
 * empty buckets fills one at a time into the right bronze bucket. Every swap keeps the stack's components, so a Poor
 * Bronze bucket stays poor. Milk: {@link BronzeTools#onEntityInteract}.
 */
public class BronzeBucketItem extends BucketItem {
    public BronzeBucketItem(Fluid content, Properties properties) {
        super(content, properties);
    }

    /**
     * Scoops the water source at {@code pos} (also water out of a waterlogged block): the filled bronze bucket
     * (keeping {@code held}'s components), or empty if there is nothing to scoop.
     */
    static ItemStack scoop(Level level, BlockPos pos, @Nullable Player player, ItemStack held) {
        FluidState fluid = level.getFluidState(pos);
        if (!fluid.isSource()) return ItemStack.EMPTY;
        if (!fluid.is(FluidTags.WATER)) return ItemStack.EMPTY;
        Item filled = BronzeTools.BRONZE_WATER_BUCKET.get();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BucketPickup pickup) || pickup.pickupBlock(player, level, pos, state).isEmpty()) return ItemStack.EMPTY;
        if (player != null) pickup.getPickupSound(state).ifPresent(sound -> player.playSound(sound, 1.0F, 1.0F));
        level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
        return held.transmuteCopy(filled, 1);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (content == Fluids.EMPTY) {
            BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
            if (hit.getType() != HitResult.Type.BLOCK) return InteractionResult.PASS;
            BlockPos pos = hit.getBlockPos();
            if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos.relative(hit.getDirection()), hit.getDirection(), stack)) return InteractionResult.FAIL;
            ItemStack filled = scoop(level, pos, player, stack);
            if (filled.isEmpty()) return InteractionResult.FAIL;
            player.awardStat(Stats.ITEM_USED.get(this));
            if (player instanceof ServerPlayer sp) CriteriaTriggers.FILLED_BUCKET.trigger(sp, filled);
            return InteractionResult.SUCCESS.heldItemTransformedTo(ItemUtils.createFilledResult(stack, player, filled));
        }
        ItemStack before = stack.copy();
        InteractionResult result = super.use(level, player, hand);
        if (result instanceof InteractionResult.Success success && success.heldItemTransformedTo() != null && success.heldItemTransformedTo().is(Items.BUCKET))
            return success.heldItemTransformedTo(before.transmuteCopy(BronzeTools.BRONZE_BUCKET.get(), 1));
        return result;
    }

    /** Water poured on farmland (or a crop on it) wets it and the 8 around it, as the Clay Water Bucket does. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = ClayBucketItem.farmland(level, context.getClickedPos());
        if (content != Fluids.WATER || pos == null || player == null) return InteractionResult.PASS;
        if (level instanceof ServerLevel server) {
            ClayBucketItem.water(server, pos);
            if (!player.hasInfiniteMaterials()) player.setItemInHand(context.getHand(), context.getItemInHand().transmuteCopy(BronzeTools.BRONZE_BUCKET.get(), 1));
        }
        return InteractionResult.SUCCESS;
    }

    /** Used as water in a recipe (dough, porridge, paint, retting flax): the empty bronze bucket stays in the grid. */
    @Override
    public ItemStackTemplate getCraftingRemainder(ItemInstance instance) {
        if (content == Fluids.EMPTY || !(instance instanceof ItemStack stack)) return null;
        return ItemStackTemplate.fromNonEmptyStack(stack.transmuteCopy(BronzeTools.BRONZE_BUCKET.get(), 1));
    }
}
