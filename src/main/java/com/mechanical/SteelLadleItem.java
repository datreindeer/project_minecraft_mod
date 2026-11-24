package com.mechanical;

import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import org.jspecify.annotations.Nullable;

/**
 * The Steel Ladle (docs/steel-furnace-plan.md phase 7): a long-handled steel ladle lined with silica that holds up to
 * {@link #CAPACITY} mB of any melt. Right-click a Refractory Trough to scoop its metal (one melt at a time); right-click a
 * mold, flask, basin or trough to pour a whole cast into it. The melt stays runny in the ladle (it is lined).
 */
public class SteelLadleItem extends Item {
    public static final int CAPACITY = 1000;

    public SteelLadleItem(Properties properties) {
        super(properties);
    }

    public static @Nullable Molten molten(ItemStack s) {
        CustomData d = s.get(DataComponents.CUSTOM_DATA);
        return d == null ? null : Molten.byId(d.copyTag().getStringOr("molten", ""));
    }

    public static int amount(ItemStack s) {
        CustomData d = s.get(DataComponents.CUSTOM_DATA);
        return d == null ? 0 : d.copyTag().getIntOr("mb", 0);
    }

    static void set(ItemStack s, @Nullable Molten m, int mb) {
        if (m == null || mb <= 0) {
            s.remove(DataComponents.CUSTOM_DATA);
            return;
        }
        CompoundTag t = new CompoundTag();
        t.putString("molten", m.id);
        t.putInt("mb", mb);
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        BlockPos pos = context.getClickedPos();
        ItemStack ladle = context.getItemInHand();
        Molten m = molten(ladle);
        int mb = amount(ladle);
        // pour a whole cast into what was clicked
        if (m != null) for (PouringSpoutBlockEntity.Sink sink : PouringSpoutBlockEntity.SINKS) {
            int need = sink.need(level, pos, m);
            if (need > 0 && need <= mb) {
                sink.fill(level, pos, m, need);
                set(ladle, m, mb - need);
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.PLAYERS, 0.8F, 1.0F);
                return InteractionResult.SUCCESS;
            }
        }
        // scoop a trough's metal
        if (level.getBlockEntity(pos) instanceof TroughBlockEntity t && t.molten() != null && !t.solid(level.getGameTime()) && (m == null || m == t.molten())) {
            Molten tm = t.molten();
            int got = t.take(Math.min(t.amount(), CAPACITY - mb), level.getGameTime());
            if (got > 0) {
                set(ladle, tm, mb + got);
                level.playSound(null, pos, SoundEvents.BUCKET_FILL_LAVA, SoundSource.PLAYERS, 0.8F, 1.0F);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        Molten m = molten(stack);
        tooltip.accept(m == null ? Component.translatable("item.mechanical.steel_ladle.empty")
                : Component.translatable("item.mechanical.steel_ladle.holds", amount(stack), CAPACITY, Component.translatable("molten.mechanical." + m.id)));
    }
}
