package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

/**
 * Threshing: hold sheaves in the other hand and beat them on a hard floor (stone, cobblestone, the Stone Anvil ...).
 * Every stroke has a 1 in {@link #STROKES} chance to finish a sheaf: 1-3 Grain (2 on average) + 1 Straw, 1 wear.
 */
public class FlailItem extends Item {
    public static final int STROKES = 3;

    public FlailItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || context.getHand() != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!context.getLevel().getBlockState(context.getClickedPos()).is(Farming.THRESHING_FLOORS)) return InteractionResult.PASS;
        ItemStack sheaf = player.getOffhandItem();
        ItemStack grain = Farming.grainOf(sheaf);
        if (grain.isEmpty()) {
            if (!context.getLevel().isClientSide()) player.sendOverlayMessage(Component.translatable("message.mechanical.flail.need_sheaf"));
            return InteractionResult.FAIL;
        }
        if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        BlockPos pos = context.getClickedPos();
        double x = pos.getX() + 0.5, y = pos.getY() + 1.05, z = pos.getZ() + 0.5;
        level.playSound(null, pos, SoundEvents.CROP_BREAK, SoundSource.PLAYERS, 0.8F, 0.8F + level.getRandom().nextFloat() * 0.3F);
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, sheaf.getItem()), x, y, z, 6, 0.2, 0.05, 0.2, 0.05);
        player.getCooldowns().addCooldown(context.getItemInHand(), 8);
        if (level.getRandom().nextInt(STROKES) == 0) {
            int r = level.getRandom().nextInt(4); // 1, 2, 2, 3
            grain.setCount(r == 0 ? 1 : r == 3 ? 3 : 2);
            sheaf.consume(1, player);
            Block.popResource(level, pos.above(), grain);
            Block.popResource(level, pos.above(), new ItemStack(Farming.STRAW.get()));
            context.getItemInHand().hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        }
        return InteractionResult.SUCCESS;
    }
}
