package com.mechanical;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FarmlandBlock;

/**
 * Fired clay bucket: holds only water, and wears a little each time it is poured out; it cracks when worn through.
 * Vanilla's bucket logic does the work; the vanilla bucket it hands back is swapped for the clay one, keeping the wear.
 */
public class ClayBucketItem extends BucketItem {
    public ClayBucketItem(Fluid content, Properties properties) {
        super(content, properties);
    }

    /** Pour it on farmland (or a crop on it): that farmland and the 8 around it get fully wet (moisture 7). */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = farmland(level, context.getClickedPos());
        if (content != Fluids.WATER || pos == null || context.getPlayer() == null) return InteractionResult.PASS;
        if (level instanceof ServerLevel server) {
            water(server, pos);
            Player player = context.getPlayer();
            if (!player.hasInfiniteMaterials()) {
                ItemStack empty = emptied(context.getItemInHand());
                if (empty.isEmpty()) player.playSound(SoundEvents.DECORATED_POT_SHATTER, 0.8F, 1.1F);
                player.setItemInHand(context.getHand(), empty);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** The farmland clicked, or under the crop clicked; null if neither. */
    static BlockPos farmland(Level level, BlockPos clicked) {
        if (level.getBlockState(clicked).getBlock() instanceof FarmlandBlock) return clicked;
        return level.getBlockState(clicked.below()).getBlock() instanceof FarmlandBlock ? clicked.below() : null;
    }

    /** Pours water on farmland: it and the 8 around it get fully wet (moisture 7). */
    static void water(ServerLevel server, BlockPos pos) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 0, 1))) {
            var soil = server.getBlockState(p);
            if (soil.getBlock() instanceof FarmlandBlock) server.setBlock(p, soil.setValue(FarmlandBlock.MOISTURE, FarmlandBlock.MAX_MOISTURE), Block.UPDATE_CLIENTS);
        }
        server.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
        server.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 30, 1.0, 0.1, 1.0, 0.1);
    }

    /** The empty clay bucket left after pouring, one pour more worn; empty when it cracks. */
    static ItemStack emptied(ItemStack full) {
        ItemStack empty = full.transmuteCopy(Mechanical.CLAY_BUCKET.get(), 1);
        empty.setDamageValue(full.getDamageValue() + 1);
        return empty.getDamageValue() >= empty.getMaxDamage() ? ItemStack.EMPTY : empty;
    }

    /** Used as water in a recipe (dough, porridge): the empty bucket stays in the grid. */
    @Override
    public ItemStackTemplate getCraftingRemainder(ItemInstance instance) {
        if (content != Fluids.WATER || !(instance instanceof ItemStack stack)) return null;
        ItemStack empty = emptied(stack);
        return empty.isEmpty() ? null : ItemStackTemplate.fromNonEmptyStack(empty);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (content == Fluids.EMPTY) {
            // only water: lava would burn through clay, and nothing else is a fluid you can scoop
            BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
            if (hit.getType() == HitResult.Type.BLOCK && !level.getFluidState(hit.getBlockPos()).is(FluidTags.WATER)) return InteractionResult.FAIL;
        }
        ItemStack before = stack.copy(); // vanilla consumes the held bucket when it fills, so keep its wear from before
        InteractionResult result = super.use(level, player, hand);
        if (!(result instanceof InteractionResult.Success success) || success.heldItemTransformedTo() == null) return result;
        ItemStack vanilla = success.heldItemTransformedTo();
        if (vanilla.is(Items.WATER_BUCKET)) return success.heldItemTransformedTo(before.transmuteCopy(Mechanical.CLAY_WATER_BUCKET.get(), 1));
        if (vanilla.is(Items.BUCKET)) {
            ItemStack empty = emptied(before);
            if (empty.isEmpty()) player.playSound(SoundEvents.DECORATED_POT_SHATTER, 0.8F, 1.1F);
            return success.heldItemTransformedTo(empty);
        }
        return result; // creative: the held stack itself
    }
}
