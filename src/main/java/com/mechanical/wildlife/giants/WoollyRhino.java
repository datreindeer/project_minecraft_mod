package com.mechanical.wildlife.giants;

import java.util.EnumSet;

import com.mechanical.wildlife.WildAnimal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Woolly rhinoceros: a solitary grazer of the cold steppe (a cow may have her calf along). Short-sighted, so it notices
 * a player late, but hurt it and it charges: it paws the ground, then thunders in a straight line at where its attacker
 * stood, tossing whatever it hits. It can't turn mid-charge: dodge aside and it runs on past, and if it slams into a
 * wall or a tree it stands dazed for a moment. Gives up and runs once it bleeds (half health).
 */
public class WoollyRhino extends WildAnimal {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/giants/woolly_rhino.png");
    private static final Identifier CALF_TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/giants/woolly_rhino_calf.png");
    static final int WINDUP = 20, RUN = 30, DAZE = 50, COOLDOWN = 60;
    private static final double CHARGE_SPEED = 2.6, CHARGE_DAMAGE = 9.0;

    public WoollyRhino(EntityType<? extends WoollyRhino> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 60.0).add(Attributes.MOVEMENT_SPEED, 0.23)
                .add(Attributes.ATTACK_DAMAGE, 7.0).add(Attributes.ATTACK_KNOCKBACK, 1.5)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8).add(Attributes.ARMOR, 6.0).add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.removeAllGoals(g -> g.getClass().getSimpleName().equals("FollowHerdGoal")); // loners
        goalSelector.addGoal(1, new ChargeGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, false) {
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
    }

    /** A hurt calf sends its mother after the attacker. */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && source.getEntity() instanceof LivingEntity attacker && !(attacker instanceof WoollyRhino)) {
            if (!isBaby()) setTarget(attacker); // turn on it right away, before the fear goal gets a look in
            else for (WoollyRhino r : level.getEntitiesOfClass(WoollyRhino.class, getBoundingBox().inflate(16), r -> !r.isBaby())) r.setTarget(attacker);
        }
        return hurt;
    }

    @Override
    public Identifier texture() {
        return isBaby() ? CALF_TEXTURE : TEXTURE;
    }

    @Override
    public boolean showsHorns() {
        return !isBaby();
    }

    @Override
    protected float babyChance() {
        return 0.4F;
    }

    /** Poor eyesight: notices a player only at about half the usual distance. */
    @Override
    public double fearMultiplier() {
        return 0.5;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.HOGLIN_AMBIENT;
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
        playSound(SoundEvents.HOGLIN_STEP, 0.4F, 0.6F);
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * (isBaby() ? 1.0F : 0.6F);
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    /**
     * Wind-up (pawing, head down), then a straight run at the spot the target stood on; anything in the way is tossed.
     * Stops after RUN ticks, on a hit, or dazed against a wall.
     */
    static class ChargeGoal extends Goal {
        private final WoollyRhino rhino;
        private int time, cooldown;
        private Vec3 dir;
        private boolean dazed;

        ChargeGoal(WoollyRhino rhino) {
            this.rhino = rhino;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (cooldown > 0) {
                cooldown -= 2; // goals are only checked every other tick
                return false;
            }
            LivingEntity t = rhino.getTarget();
            if (rhino.isBaby() || t == null || !t.isAlive() || !rhino.onGround()) return false;
            double d = rhino.distanceTo(t);
            return d > 4 && d < 18 && rhino.hasLineOfSight(t);
        }

        @Override
        public void start() {
            time = 0;
            dazed = false;
            dir = null;
            rhino.getNavigation().stop();
            rhino.playSound(SoundEvents.HOGLIN_ANGRY, 2.0F, 0.55F);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return time < WINDUP + RUN + (dazed ? DAZE : 0) && rhino.isAlive();
        }

        @Override
        public void tick() {
            time++;
            LivingEntity t = rhino.getTarget();
            if (time <= WINDUP) {
                // pawing the ground, lined up on the target
                if (t != null) rhino.getLookControl().setLookAt(t, 30, 30);
                if (time % 6 == 0 && rhino.level() instanceof ServerLevel level) {
                    rhino.playSound(SoundEvents.HOGLIN_STEP, 1.0F, 0.5F);
                    level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, rhino.getBlockStateOn()),
                            rhino.getX(), rhino.getY() + 0.1, rhino.getZ(), 6, 0.5, 0.05, 0.5, 0.05);
                }
                if (time == WINDUP) {
                    Vec3 to = (t != null ? t.position() : rhino.position().add(rhino.getLookAngle())).subtract(rhino.position()).multiply(1, 0, 1);
                    dir = to.lengthSqr() < 1.0E-4 ? rhino.getLookAngle().multiply(1, 0, 1).normalize() : to.normalize();
                }
                return;
            }
            if (dazed) {
                rhino.getNavigation().stop();
                if (time % 10 == 0 && rhino.level() instanceof ServerLevel level)
                    level.sendParticles(ParticleTypes.CRIT, rhino.getX(), rhino.getEyeY() + 0.4, rhino.getZ(), 4, 0.3, 0.1, 0.3, 0.0);
                return;
            }
            // thunder straight on: steer at a point far ahead along the locked direction
            Vec3 aim = rhino.position().add(dir.scale(4));
            rhino.getMoveControl().setWantedPosition(aim.x, rhino.getY(), aim.z, CHARGE_SPEED);
            rhino.setYRot((float) (Math.atan2(dir.z, dir.x) * 180 / Math.PI) - 90);
            rhino.yBodyRot = rhino.yHeadRot = rhino.getYRot();
            if (time % 4 == 0) rhino.playSound(SoundEvents.RAVAGER_STEP, 0.8F, 0.8F);
            if (!(rhino.level() instanceof ServerLevel level)) return;
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, rhino.getBoundingBox().inflate(0.4),
                    e -> e != rhino && !(e instanceof WoollyRhino) && e.isAlive())) {
                if (e.hurtServer(level, rhino.damageSources().mobAttack(rhino), (float) CHARGE_DAMAGE)) {
                    e.knockback(2.0, -dir.x, -dir.z, rhino.damageSources().mobAttack(rhino), (float) CHARGE_DAMAGE);
                    e.setDeltaMovement(e.getDeltaMovement().add(0, 0.6, 0));
                    e.needsSync = true;
                    rhino.playSound(SoundEvents.RAVAGER_ATTACK, 1.5F, 0.7F);
                    finish();
                    return;
                }
            }
            if (rhino.horizontalCollision && time > WINDUP + 3) {
                // slammed into something solid
                rhino.playSound(SoundEvents.RAVAGER_STUNNED, 1.5F, 0.8F);
                dazed = true;
                time = WINDUP + RUN;
            }
        }

        private void finish() {
            time = WINDUP + RUN;
        }

        @Override
        public void stop() {
            cooldown = COOLDOWN;
            rhino.getNavigation().stop();
        }
    }
}
