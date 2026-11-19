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

/**
 * Fired clay bucket: holds only water, and wears a little each time it is poured out; it cracks when worn through.
 * Vanilla's bucket logic does the work; the vanilla bucket it hands back is swapped for the clay one, keeping the wear.
 */
public class ClayBucketItem extends BucketItem {
    public ClayBucketItem(Fluid content, Properties properties) {
        super(content, properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (content == Fluids.EMPTY) {
            // only water: lava would burn through clay, and nothing else is a fluid you can scoop
            BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
            if (hit.getType() == HitResult.Type.BLOCK && !level.getFluidState(hit.getBlockPos()).is(FluidTags.WATER)) return InteractionResult.FAIL;
        }
        InteractionResult result = super.use(level, player, hand);
        if (!(result instanceof InteractionResult.Success success) || success.heldItemTransformedTo() == null) return result;
        ItemStack vanilla = success.heldItemTransformedTo();
        if (vanilla.is(Items.WATER_BUCKET)) return success.heldItemTransformedTo(stack.transmuteCopy(Mechanical.CLAY_WATER_BUCKET.get(), 1));
        if (vanilla.is(Items.BUCKET)) {
            ItemStack empty = stack.transmuteCopy(Mechanical.CLAY_BUCKET.get(), 1);
            empty.setDamageValue(stack.getDamageValue() + 1);
            if (empty.getDamageValue() >= empty.getMaxDamage()) {
                player.playSound(SoundEvents.DECORATED_POT_SHATTER, 0.8F, 1.1F);
                return success.heldItemTransformedTo(ItemStack.EMPTY);
            }
            return success.heldItemTransformedTo(empty);
        }
        return result; // creative: the held stack itself
    }
}
