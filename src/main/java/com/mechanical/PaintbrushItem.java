package com.mechanical;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Paintbrush: a tuft of plant fibre on a stick. With paint ({@link PaintItem}) in the other hand, use it on a block face
 * to paint the selected motif in the paint's colour. Sneak-use picks the next motif (kept on the brush).
 */
public class PaintbrushItem extends Item {
    private static final CavePaintingBlock.Motif[] MOTIFS = java.util.Arrays.stream(CavePaintingBlock.Motif.values())
            .filter(CavePaintingBlock.Motif::brushable).toArray(CavePaintingBlock.Motif[]::new);

    public PaintbrushItem(Properties properties) {
        super(properties);
    }

    static CavePaintingBlock.Motif motif(ItemStack brush) {
        return MOTIFS[Math.floorMod(brush.getOrDefault(Pigments.BRUSH_MOTIF.get(), 0), MOTIFS.length)];
    }

    private static InteractionResult cycle(Player player, ItemStack brush) {
        if (player.level().isClientSide()) return InteractionResult.SUCCESS;
        brush.set(Pigments.BRUSH_MOTIF.get(), Math.floorMod(brush.getOrDefault(Pigments.BRUSH_MOTIF.get(), 0) + 1, MOTIFS.length));
        player.sendOverlayMessage(Component.translatable("message.mechanical.paintbrush.motif", motifName(motif(brush))));
        return InteractionResult.SUCCESS;
    }

    static Component motifName(CavePaintingBlock.Motif motif) {
        return Component.translatable("motif.mechanical." + motif.getSerializedName());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return player.isSecondaryUseActive() ? cycle(player, player.getItemInHand(hand)) : InteractionResult.PASS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        ItemStack brush = context.getItemInHand();
        if (player.isSecondaryUseActive()) return cycle(player, brush);
        InteractionHand other = context.getHand() == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        if (!(player.getItemInHand(other).getItem() instanceof PaintItem paint)) {
            if (!context.getLevel().isClientSide()) player.sendOverlayMessage(Component.translatable("message.mechanical.paintbrush.no_paint"));
            return InteractionResult.FAIL;
        }
        CavePaintingBlock.Motif motif = motif(brush);
        int painted = CavePaintingBlock.paint(context.getLevel(), context.getClickedPos(), context.getClickedFace(), motif, paint.colour());
        if (painted == 0) {
            if (!context.getLevel().isClientSide()) player.sendOverlayMessage(Component.translatable(
                    motif.scene() ? "message.mechanical.paintbrush.no_room_scene" : "message.mechanical.paintbrush.no_room"));
            return InteractionResult.FAIL;
        }
        PaintItem.use(player, other, painted);
        if (!context.getLevel().isClientSide()) brush.hurtAndBreak(1, player, context.getHand());
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
            java.util.function.Consumer<Component> builder, net.minecraft.world.item.TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.mechanical.paintbrush.motif", motifName(motif(stack))).withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
