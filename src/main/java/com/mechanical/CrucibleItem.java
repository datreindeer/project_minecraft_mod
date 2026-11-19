package com.mechanical;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * The fired Crucible and what it becomes. ORE: click Crushed Copper Ore / Native Copper onto it in the inventory (or
 * hold the ore in the other hand and use the crucible) until it holds {@link CrucibleContents#CAPACITY} units; in a
 * Clay Kiln at the metal's melting point it turns MOLTEN. MOLTEN: pour it into a Clay Mold within
 * {@link #COOL_SECONDS} s (the bar shows the heat left), else it sets SOLID and must be melted again.
 */
public class CrucibleItem extends Item {
    public enum Kind { ORE, MOLTEN, SOLID }

    public static final int COOL_SECONDS = 60;
    private final Kind kind;

    public CrucibleItem(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    public static CrucibleContents contents(ItemStack stack) {
        return stack.getOrDefault(Casting.CRUCIBLE_CONTENTS.get(), CrucibleContents.EMPTY);
    }

    /** Puts up to {@code max} items of ore from {@code ore} into the crucible; returns how many went in. */
    static int fill(ItemStack crucible, ItemStack ore, int max) {
        Casting.OreUnits per = Casting.oreUnits(ore);
        if (per == null) return 0;
        CrucibleContents c = contents(crucible);
        int n = Math.min(Math.min(max, ore.getCount()), (CrucibleContents.CAPACITY - c.total()) / per.units());
        if (n <= 0) return 0;
        crucible.set(Casting.CRUCIBLE_CONTENTS.get(), c.add(per.metal(), n * per.units()));
        ore.shrink(n);
        return n;
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack self, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess carried) {
        if (kind != Kind.ORE || self.getCount() != 1 || other.isEmpty() || Casting.oreUnits(other) == null) return false;
        if (!slot.allowModification(player)) return false;
        int n = fill(self, other, action == ClickAction.PRIMARY ? Integer.MAX_VALUE : 1);
        player.playSound(n > 0 ? SoundEvents.BUNDLE_INSERT : SoundEvents.BUNDLE_INSERT_FAIL, 0.8F, 0.8F + player.level().getRandom().nextFloat() * 0.4F);
        return true;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack self = player.getItemInHand(hand);
        ItemStack ore = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (kind != Kind.ORE || self.getCount() != 1 || Casting.oreUnits(ore) == null) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            int n = fill(self, ore, 1);
            level.playSound(null, player.blockPosition(), n > 0 ? SoundEvents.BUNDLE_INSERT : SoundEvents.BUNDLE_INSERT_FAIL, SoundSource.PLAYERS, 0.8F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, EquipmentSlot slot) {
        if (kind != Kind.MOLTEN || level.getGameTime() % 20 != 0) return;
        long now = level.getGameTime();
        Long coolsAt = stack.get(Casting.COOLS_AT.get());
        if (coolsAt == null) {
            stack.set(Casting.COOLS_AT.get(), coolsAt = now + COOL_SECONDS * 20L);
        }
        long left = coolsAt - now;
        if (left > 0) {
            stack.setDamageValue(COOL_SECONDS - (int) Math.min(COOL_SECONDS, left / 20));
            return;
        }
        if (!(owner instanceof Player player)) return;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i) != stack) continue;
            player.getInventory().setItem(i, solidify(stack));
            level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.4F, 1.4F);
            return;
        }
    }

    /** A molten crucible that has gone cold: same metal, now solid. */
    public static ItemStack solidify(ItemStack molten) {
        ItemStack out = new ItemStack(Casting.SOLID_CRUCIBLE.get());
        out.set(Casting.CRUCIBLE_CONTENTS.get(), contents(molten));
        return out;
    }

    /** Fresh out of the kiln (or kept hot in its output slot): full heat again. */
    public static ItemStack molten(CrucibleContents melted, long now) {
        ItemStack out = new ItemStack(Casting.MOLTEN_CRUCIBLE.get());
        out.set(Casting.CRUCIBLE_CONTENTS.get(), melted);
        out.set(Casting.COOLS_AT.get(), now + COOL_SECONDS * 20L);
        return out;
    }

    /** Has it gone cold by now (also for stacks that sat in chests, where inventoryTick doesn't run)? */
    public static boolean isCold(ItemStack molten, long now) {
        Long coolsAt = molten.get(Casting.COOLS_AT.get());
        return coolsAt != null && now >= coolsAt;
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xFF8A1F;
    }

    @Override
    public Component getName(ItemStack stack) {
        Metal metal = contents(stack).alloy();
        if (kind == Kind.ORE || metal == null) return super.getName(stack);
        Component name = Component.translatable("metal.mechanical." + metal.getSerializedName());
        return Component.translatable(kind == Kind.MOLTEN ? "item.mechanical.molten_crucible.of" : "item.mechanical.solid_crucible.of", name);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        CrucibleContents c = contents(stack);
        if (kind == Kind.ORE && c.isEmpty()) {
            builder.accept(Component.translatable("tooltip.mechanical.crucible.empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        for (var e : c.units().entrySet()) {
            Component metal = Component.translatable("metal.mechanical." + e.getKey());
            builder.accept(Component.translatable(kind == Kind.ORE ? "tooltip.mechanical.crucible.ore" : "tooltip.mechanical.crucible.metal",
                    metal, e.getValue(), CrucibleContents.CAPACITY).withStyle(ChatFormatting.GRAY));
        }
        Metal metal = c.alloy();
        if (kind == Kind.ORE && metal != null)
            builder.accept(Component.translatable("tooltip.mechanical.crucible.melts", metal.melt).withStyle(ChatFormatting.DARK_GRAY));
        if (kind == Kind.MOLTEN) builder.accept(Component.translatable("tooltip.mechanical.crucible.pour").withStyle(ChatFormatting.GOLD));
        if (kind == Kind.SOLID) builder.accept(Component.translatable("tooltip.mechanical.crucible.remelt").withStyle(ChatFormatting.DARK_GRAY));
    }
}
