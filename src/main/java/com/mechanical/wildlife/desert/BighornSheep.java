package com.mechanical.wildlife.desert;

import java.util.EnumSet;

import com.mechanical.AnimalFear;
import com.mechanical.wildlife.mountain.Ibex;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Bighorn Sheep: a stocky brown wild sheep with a white rump and muzzle, in small bands on the badland cliffs. Climbs
 * rock faces and flees uphill like the ibex it inherits from (rams also clash with each other in the rut). Rams carry
 * huge horns that curl round in a full circle past the cheek; ewes short, slightly curved ones. A ram stands up for
 * itself: hurt by someone within {@link #CHARGE_RANGE} it lowers its horns and charges ({@link ChargeGoal}), and the butt
 * knocks you flying ({@link #BUTT_DAMAGE} damage), then it makes off uphill. Gives bighorn mutton, a hide and the great
 * Bighorn Horn.
 */
public class BighornSheep extends Ibex {
    private static final Identifier RAM = tex("bighorn_ram"), EWE = tex("bighorn_ewe"), LAMB = tex("bighorn_lamb");
    private static final EntityDataAccessor<Boolean> CHARGING = SynchedEntityData.defineId(BighornSheep.class, EntityDataSerializers.BOOLEAN);
    public static final double CHARGE_RANGE = 10;
    public static final float BUTT_DAMAGE = 5.0F;
    private LivingEntity chargeAt;

    public BighornSheep(EntityType<? extends BighornSheep> type, Level level) {
        super(type, level);
    }

    private static Identifier tex(String name) {
        return Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/" + name + ".png");
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.STEP_HEIGHT, 1.0).add(Attributes.SAFE_FALL_DISTANCE, 8.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.3);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CHARGING, false);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(-2, new ChargeGoal()); // before the flight goals (0)
    }

    @Override
    protected float maleChance() {
        return 0.4F;
    }

    @Override
    public Identifier texture() {
        return isBaby() ? LAMB : isMale() ? RAM : EWE;
    }

    /** Charging a player, or clashing with a rival ram. */
    @Override
    public boolean isCharging() {
        return entityData.get(CHARGING) || super.isCharging();
    }

    public boolean isChargingPlayer() {
        return entityData.get(CHARGING);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && isAlive() && !isTamed() && isMale() && !isBaby() && source.getEntity() instanceof LivingEntity attacker && distanceTo(attacker) <= CHARGE_RANGE)
            chargeAt = attacker;
        return hurt;
    }

    /** The ram lowers its horns and runs at whoever hurt it; the butt throws him back, then it runs for the heights. */
    class ChargeGoal extends Goal {
        private int ticks;

        ChargeGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return chargeAt != null && chargeAt.isAlive() && distanceTo(chargeAt) <= CHARGE_RANGE * 1.5;
        }

        @Override
        public void start() {
            ticks = 100;
            entityData.set(CHARGING, true);
            playSound(SoundEvents.GOAT_PREPARE_RAM, 1.0F, 0.8F);
        }

        @Override
        public boolean canContinueToUse() {
            return --ticks > 0 && chargeAt != null && chargeAt.isAlive();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            getLookControl().setLookAt(chargeAt, 30.0F, 30.0F);
            if ((tickCount + getId()) % 5 == 0) getNavigation().moveTo(chargeAt, 1.9);
            if (distanceTo(chargeAt) < 1.6) {
                chargeAt.hurtServer((ServerLevel) level(), damageSources().mobAttack(BighornSheep.this), BUTT_DAMAGE);
                Vec3 push = chargeAt.position().subtract(position()).multiply(1, 0, 1).normalize().scale(1.4);
                chargeAt.push(push.x, 0.45, push.z);
                playSound(SoundEvents.GOAT_RAM_IMPACT, 1.5F, 0.7F);
                Vec3 from = chargeAt.position();
                chargeAt = null;
                AnimalFear.panic(BighornSheep.this, from);
            }
        }

        @Override
        public void stop() {
            entityData.set(CHARGING, false);
            chargeAt = null;
        }
    }
}
