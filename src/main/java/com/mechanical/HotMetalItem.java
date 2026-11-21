package com.mechanical;

import java.util.function.Consumer;
import java.util.function.Supplier;

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
 * Iron or steel at forging heat ({@link Smithing}): glowing, the bar shows the heat left, burns a bare hand like a hot
 * bloom (Bronze Tongs in the other hand), and after {@link Ironworking#HOT_SECONDS} s it is the cold metal again. The
 * blows struck so far stay on it ({@link Ironworking#BLOOM_HITS}), so it can be reheated (1100°: Bloomery or Clay Kiln)
 * and worked on.
 */
public class HotMetalItem extends Item {
    private final Supplier<? extends Item> cold;

    public HotMetalItem(Properties properties, Supplier<? extends Item> cold) {
        super(properties);
        this.cold = cold;
    }

    public Item cold() {
        return cold.get();
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, EquipmentSlot slot) {
        if (level.getGameTime() % 20 != 0) return;
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

    /** The same piece gone cold (its blows kept). */
    public static ItemStack cooled(ItemStack hot) {
        if (!(hot.getItem() instanceof HotMetalItem metal)) return hot;
        ItemStack out = new ItemStack(metal.cold());
        int hits = Ironworking.hits(hot);
        if (hits > 0) out.set(Ironworking.BLOOM_HITS.get(), hits);
        return out;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // it glows
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xFF8A1F;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        int hits = Ironworking.hits(stack);
        if (hits > 0) builder.accept(Component.translatable("tooltip.mechanical.forge.hits", hits).withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("tooltip.mechanical.forge.hot").withStyle(ChatFormatting.GOLD));
    }
}
