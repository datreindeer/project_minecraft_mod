package com.mechanical;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Knives: survive crafting (losing 1 durability), cut plant fibre, and butcher carcasses while right-click is held.
 * Better metals butcher faster ({@code speed}), get more out of each animal ({@code yieldMultiplier}) and can cut
 * tougher animals ({@code tier}: flint 1, tusk 2, iron 3; see {@link Butchery#requiredKnifeTier}). {@code hideMultiplier}
 * scales the first pass (hide, pelt, wool, feathers): the tusk dagger is a skinning knife and gets more of it.
 */
public class KnifeItem extends Item {
    public static final int FLINT = 1, TUSK = 2, BRONZE = 2, IRON = 3;
    private final float speed, yieldMultiplier, hideMultiplier;
    private final int tier;

    public KnifeItem(Properties properties, float speed, float yieldMultiplier, int tier) {
        this(properties, speed, yieldMultiplier, tier, yieldMultiplier);
    }

    public KnifeItem(Properties properties, float speed, float yieldMultiplier, int tier, float hideMultiplier) {
        super(properties);
        this.speed = speed;
        this.yieldMultiplier = yieldMultiplier;
        this.tier = tier;
        this.hideMultiplier = hideMultiplier;
    }

    public float hideMultiplier() {
        return hideMultiplier;
    }

    public int tier() {
        return tier;
    }

    public float speed() {
        return speed;
    }

    public float yieldMultiplier() {
        return yieldMultiplier;
    }

    @Override
    public ItemStackTemplate getCraftingRemainder(ItemInstance instance) {
        if (!(instance instanceof ItemStack stack)) return null;
        ItemStack used = stack.copyWithCount(1);
        used.setDamageValue(used.getDamageValue() + 1);
        return used.getDamageValue() >= used.getMaxDamage() ? null : ItemStackTemplate.fromNonEmptyStack(used);
    }

    // ---- butchering: started by CarcassEntity.interact, continues while the player keeps looking at the carcass ----

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BRUSH;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int ticksRemaining) {
        if (!(user instanceof Player player)) return;
        var hit = ProjectileUtil.getHitResultOnViewVector(player, e -> e instanceof CarcassEntity, player.entityInteractionRange());
        if (!(hit instanceof EntityHitResult entityHit) || !(entityHit.getEntity() instanceof CarcassEntity carcass)) {
            player.stopUsingItem();
            return;
        }
        if (level instanceof ServerLevel server) carcass.butcher(server, player, this);
    }
}
