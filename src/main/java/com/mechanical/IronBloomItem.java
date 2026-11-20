package com.mechanical;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Iron Bloom out of the Bloomery: hot ({@link Ironworking#HOT_IRON_BLOOM}, glowing, the bar shows the heat left,
 * burns a bare hand like molten metal) for {@link Ironworking#HOT_SECONDS} s, then a cold Iron Bloom that must be
 * reheated (1100°: Bloomery or Clay Kiln) before it can be worked. Blows struck so far stay on it ({@link Ironworking#BLOOM_HITS}).
 */
public class IronBloomItem extends Item {
    private final boolean hot;

    public IronBloomItem(Properties properties, boolean hot) {
        super(properties);
        this.hot = hot;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, EquipmentSlot slot) {
        if (!hot || level.getGameTime() % 20 != 0) return;
        long now = level.getGameTime();
        Long coolsAt = stack.get(Casting.COOLS_AT.get());
        if (coolsAt == null) stack.set(Casting.COOLS_AT.get(), coolsAt = now + Ironworking.HOT_SECONDS * 20L);
        long left = coolsAt - now;
        if (left > 0) {
            stack.setDamageValue(Ironworking.HOT_SECONDS - (int) Math.min(Ironworking.HOT_SECONDS, left / 20));
            return;
        }
        if (!(owner instanceof Player player)) return;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i) != stack) continue;
            player.getInventory().setItem(i, cooled(stack));
            level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.4F, 1.4F);
            return;
        }
    }

    /** The same bloom gone cold (its blows kept). */
    public static ItemStack cooled(ItemStack hot) {
        ItemStack out = new ItemStack(Ironworking.IRON_BLOOM.get());
        int hits = Ironworking.hits(hot);
        if (hits > 0) out.set(Ironworking.BLOOM_HITS.get(), hits);
        return out;
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xFF8A1F;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        int hits = Ironworking.hits(stack);
        if (hits > 0) builder.accept(Component.translatable("tooltip.mechanical.iron_bloom.hits", hits, Ironworking.HITS).withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable(hot ? "tooltip.mechanical.iron_bloom.hot" : "tooltip.mechanical.iron_bloom.cold",
                BloomeryBlockEntity.FORGE_TEMP).withStyle(hot ? ChatFormatting.GOLD : ChatFormatting.GRAY));
    }
}
