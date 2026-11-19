package com.mechanical;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;

/**
 * A bowl of paint (pigment mixed with tallow or water) in one {@link CavePaintingBlock.Colour}. Each painted block uses
 * one of its uses; the empty Bowl is left. Pressed on a wall without a brush it leaves a hand print.
 */
public class PaintItem extends Item {
    public static final int USES = 16;
    private final CavePaintingBlock.Colour colour;

    public PaintItem(Properties properties, CavePaintingBlock.Colour colour) {
        super(properties);
        this.colour = colour;
    }

    public CavePaintingBlock.Colour colour() {
        return colour;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        // with a brush in the other hand, the brush paints (vanilla tries the other hand on PASS)
        InteractionHand other = context.getHand() == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        if (player.getItemInHand(other).getItem() instanceof PaintbrushItem) return InteractionResult.PASS;
        int painted = CavePaintingBlock.paint(context.getLevel(), context.getClickedPos(), context.getClickedFace(), CavePaintingBlock.Motif.HAND, colour);
        if (painted == 0) return InteractionResult.PASS;
        use(player, context.getHand(), painted);
        return InteractionResult.SUCCESS;
    }

    /** Uses up {@code amount} of the paint in {@code hand} (not in creative), with a soft daub sound; the bowl is left when it runs out. */
    static void use(Player player, InteractionHand hand, int amount) {
        if (!(player.level() instanceof ServerLevel level)) return;
        level.playSound(null, player.blockPosition(), SoundEvents.HONEY_BLOCK_PLACE, SoundSource.PLAYERS, 0.6F, 1.3F);
        if (player.hasInfiniteMaterials()) return;
        ItemStack paint = player.getItemInHand(hand);
        paint.setDamageValue(paint.getDamageValue() + amount);
        if (paint.getDamageValue() >= paint.getMaxDamage()) player.setItemInHand(hand, new ItemStack(Items.BOWL));
    }
}
