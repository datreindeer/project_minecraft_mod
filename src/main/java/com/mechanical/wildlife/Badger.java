package com.mechanical.wildlife;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Badger: stocky, low, grey with the black-and-white striped face. Lives where the ground slopes (it digs its sett into
 * banks). Keeps away from players, but when hurt or cornered (a player right on top of it) it turns and bites hard.
 */
public class Badger extends WildAnimal {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/badger.png");

    public Badger(EntityType<? extends Badger> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 14.0).add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.ATTACK_DAMAGE, 3.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.3);
    }

    /** Spawn rule: the ground around rises or falls at least 3 blocks within 4 (a bank, hillside or ravine edge). */
    public static boolean onSlope(LevelReader level, BlockPos pos) {
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int[] o : new int[][] {{0, 0}, {4, 0}, {-4, 0}, {0, 4}, {0, -4}}) {
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX() + o[0], pos.getZ() + o[1]);
            min = Math.min(min, y);
            max = Math.max(max, y);
        }
        return max - min >= 3;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.removeAllGoals(g -> g instanceof FollowHerdGoal); // solitary
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.6, true) {
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
        });
        targetSelector.addGoal(2, new CorneredGoal(this));
    }

    /** A player who gets right on top of it (not sneaking past) gets bitten. */
    static class CorneredGoal extends TargetGoal {
        private static final double TOO_CLOSE = 2.5;
        private final Badger badger;

        CorneredGoal(Badger badger) {
            super(badger, true);
            this.badger = badger;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (badger.isBaby() || badger.isDozing() || badger.getTarget() != null) return false;
            Player p = badger.level().getNearestPlayer(badger, TOO_CLOSE);
            if (p == null || p.isCreative() || p.isSpectator() || p.isCrouching() || !badger.hasLineOfSight(p)) return false;
            targetMob = p;
            return true;
        }

        @Override
        public void start() {
            badger.setTarget(targetMob);
            badger.playSound(SoundEvents.FOX_AGGRO, 1.0F, 0.6F);
            super.start();
        }
    }

    @Override
    public Identifier texture() {
        return TEXTURE;
    }

    @Override
    protected float babyChance() {
        return 0.3F;
    }

    @Override
    public double fearMultiplier() {
        return 0.8;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isCharging() ? SoundEvents.FOX_AGGRO : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.FOX_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.FOX_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.65F;
    }
}
