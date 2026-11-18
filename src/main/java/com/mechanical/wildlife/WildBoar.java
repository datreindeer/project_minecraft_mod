package com.mechanical.wildlife;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import java.util.List;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Stocky boar in small groups. Wary of players, but when hurt it charges (and its group joins in). */
public class WildBoar extends WildAnimal {
    private static final Identifier BABY_TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/wild_boar_piglet.png");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/wild_boar.png");

    public WildBoar(EntityType<? extends WildBoar> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 26.0).add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 4.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.4);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.5, false) {
            @Override
            public boolean canUse() {
                return !isBaby() && super.canUse();
            }
        });
        targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                return !isBaby() && super.canUse();
            }
        }.setAlertOthers());
        targetSelector.addGoal(2, new GroupCourageGoal(this));
    }

    /** With at least one other boar close by, a boar that notices a player sometimes charges it, and the group follows. */
    static class GroupCourageGoal extends Goal {
        private static final double NOTICE = 8, GROUP = 10;
        private static final float CHANCE = 0.2F;
        private final WildBoar boar;
        private int cooldown;

        GroupCourageGoal(WildBoar boar) {
            this.boar = boar;
        }

        @Override
        public boolean canUse() {
            if (--cooldown > 0 || boar.isBaby() || boar.getTarget() != null) return false;
            cooldown = 40;
            Player player = boar.level().getNearestPlayer(boar, NOTICE);
            if (player == null || player.isCreative() || player.isSpectator() || !boar.hasLineOfSight(player)) return false;
            List<WildBoar> group = boar.level().getEntitiesOfClass(WildBoar.class, boar.getBoundingBox().inflate(GROUP), o -> o != boar);
            if (group.isEmpty() || boar.getRandom().nextFloat() >= CHANCE) return false;
            boar.setTarget(player);
            for (WildBoar other : group) if (other.getRandom().nextBoolean()) other.setTarget(player);
            cooldown = 600; // don't keep rolling once they've decided
            return false;
        }
    }

    /** A hurt piglet calls the grown boars nearby onto its attacker. */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && isBaby() && source.getEntity() instanceof LivingEntity attacker && !(attacker instanceof WildBoar)) {
            for (WildBoar adult : level.getEntitiesOfClass(WildBoar.class, getBoundingBox().inflate(16), b -> !b.isBaby())) adult.setTarget(attacker);
        }
        return hurt;
    }

    @Override
    public Identifier texture() {
        return isBaby() ? BABY_TEXTURE : TEXTURE;
    }

    @Override
    public boolean showsHorns() {
        return !isBaby(); // tusks
    }

    @Override
    protected float babyChance() {
        return 0.4F;
    }

    @Override
    public double fearMultiplier() {
        return 0.7;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isCharging() ? SoundEvents.HOGLIN_ANGRY : SoundEvents.HOGLIN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.HOGLIN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.HOGLIN_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.HOGLIN_STEP, 0.12F, 1.1F);
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 1.2F;
    }
}
