package com.mechanical.wildlife.desert;

import java.util.EnumSet;

import com.mechanical.AnimalFear;
import com.mechanical.wildlife.meadow.Adder;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Sand Viper (horned viper): a short, thick, sand-coloured viper with a horn over each eye. On sand it shuffles itself
 * down until only its eyes and horns show ({@link #isBuried}) and lies there for minutes; buried it can hardly be seen,
 * and it bites whoever steps within {@link #BURIED_STRIKE} of it - sneaking doesn't help, it feels the steps. Above the
 * sand it is an adder ({@link Adder}): strikes the careless, Snakeskin Boots stop the fangs, poison. Gives snakeskin,
 * snake meat, a Venom Gland and Viper Fangs.
 */
public class SandViper extends Adder {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/sand_viper.png");
    private static final EntityDataAccessor<Boolean> BURIED = SynchedEntityData.defineId(SandViper.class, EntityDataSerializers.BOOLEAN);
    public static final double BURIED_STRIKE = 1.7;

    public SandViper(EntityType<? extends SandViper> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 6.0).add(Attributes.MOVEMENT_SPEED, 0.18).add(Attributes.ATTACK_DAMAGE, 2.0);
    }

    public boolean isBuried() {
        return entityData.get(BURIED);
    }

    public void setBuried(boolean buried) {
        entityData.set(BURIED, buried);
    }

    @Override
    public boolean isCoiled() {
        return isBuried() || super.isCoiled();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BURIED, false);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(2, new BuryGoal()); // ahead of dozing (3): at night too it lies buried
        targetSelector.addGoal(1, new BuriedStrikeGoal());
    }

    /** On sand (or red sand). */
    public boolean onSand() {
        return onGround() && level().getBlockState(blockPosition().below()).is(BlockTags.SAND);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        setBuried(false);
        return super.hurtServer(level, source, damage);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        setBuried(false); // bursts out of the sand
        return super.doHurtTarget(level, target);
    }

    @Override
    public Identifier texture() {
        return TEXTURE;
    }

    /** Idle on sand: shuffles down into it and lies there, still, a few minutes at a time. */
    class BuryGoal extends Goal {
        private int time;

        BuryGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return onSand() && getTarget() == null && !AnimalFear.isPanicking(SandViper.this) && (isBuried() || getRandom().nextInt(40) == 0);
        }

        @Override
        public void start() {
            time = 2400 + getRandom().nextInt(3600);
            getNavigation().stop();
            if (!isBuried()) playSound(SoundEvents.SAND_STEP, 0.6F, 0.8F);
            setBuried(true);
        }

        @Override
        public boolean canContinueToUse() {
            return time > 0 && onSand() && getTarget() == null && !AnimalFear.isPanicking(SandViper.this) && hurtTime == 0;
        }

        @Override
        public void tick() {
            time -= 2;
            getNavigation().stop();
        }

        @Override
        public void stop() {
            if (time <= 0 && getTarget() == null && hurtTime == 0) {
                // time's up: comes out now and then to move on, else lies on (canUse picks it up again while buried)
                if (getRandom().nextInt(3) == 0) setBuried(false);
                return;
            }
            setBuried(false);
        }
    }

    /** Buried: anyone (sneaking or not, not in creative) who steps within {@link #BURIED_STRIKE} is bitten. */
    class BuriedStrikeGoal extends TargetGoal {
        BuriedStrikeGoal() {
            super(SandViper.this, false);
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (!isBuried() || getTarget() != null) return false;
            Player p = level().getNearestPlayer(SandViper.this, BURIED_STRIKE);
            if (p == null || p.isCreative() || p.isSpectator()) return false;
            targetMob = p;
            return true;
        }

        @Override
        public void start() {
            setTarget(targetMob);
            super.start();
        }
    }
}
