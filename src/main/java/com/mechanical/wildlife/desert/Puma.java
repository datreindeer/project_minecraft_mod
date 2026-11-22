package com.mechanical.wildlife.desert;

import java.util.EnumSet;

import com.mechanical.wildlife.CaveLion;
import com.mechanical.wildlife.DesertHare;
import com.mechanical.wildlife.Mouflon;
import com.mechanical.wildlife.WildBoar;
import com.mechanical.wildlife.warm.DesertLizard;
import com.mechanical.wildlife.warm.Onager;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Puma (mountain lion): a lone, plain tawny cat of the badlands. Hunts like the cave lion it inherits from (creeps up,
 * freezes when watched, pounces, fears fire), but it is a cliff cat: by day it lies up on a ledge, a spot with a drop of
 * {@link #LEDGE_DROP} blocks or more beside it ({@link LedgeGoal}), and from a ledge it leaps straight down onto prey
 * passing below ({@link #ledgeLeap}: a target 2.5 or more blocks below and up to {@link #LEDGE_REACH} away), which the
 * lion's pounce then lands. Lands on its feet: the first {@link #SAFE_FALL} blocks of a fall don't hurt it.
 */
public class Puma extends CaveLion {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/puma.png");
    public static final float SAFE_FALL = 12.0F;
    public static final int LEDGE_DROP = 4, LEDGE_RANGE = 14;
    public static final double LEDGE_REACH = 10;
    private int leapCooldown;

    public Puma(EntityType<? extends Puma> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 26.0).add(Attributes.MOVEMENT_SPEED, 0.33)
                .add(Attributes.ATTACK_DAMAGE, 4.5).add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.2)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(5, new LedgeGoal());
    }

    @Override
    protected boolean isPrey(LivingEntity e) {
        return !e.isBaby() && (e instanceof BighornSheep || e instanceof DesertHare || e instanceof Onager || e instanceof Mouflon
                || e instanceof WildBoar || e instanceof DesertLizard || e instanceof FennecFox || e instanceof com.mechanical.people.Tribesperson)
                || e instanceof WildCamel && e.isBaby();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide()) return;
        if (leapCooldown > 0) leapCooldown--;
        LivingEntity t = getTarget();
        if (t != null && leapCooldown == 0 && canLedgeLeap(t)) ledgeLeap(t);
    }

    /** {@code t} is below this ledge and in reach, in sight, and it is standing. */
    public boolean canLedgeLeap(LivingEntity t) {
        if (!onGround() || isLeaping() || !t.isAlive()) return false;
        double drop = getY() - t.getY(), flat = position().subtract(t.position()).horizontalDistance();
        return drop >= 2.5 && flat >= 1.5 && flat <= LEDGE_REACH && hasLineOfSight(t);
    }

    /** Springs out and down onto {@code t}; CaveLion's ambush lands the blow when it comes down on it. */
    public void ledgeLeap(LivingEntity t) {
        Vec3 d = t.position().subtract(position());
        double flat = d.horizontalDistance();
        double time = Math.max(6, Math.sqrt(2 * Math.max(0.5, d.y * -1) / 0.08)); // ticks to fall the height
        Vec3 v = new Vec3(d.x / time, 0.25, d.z / time);
        if (v.horizontalDistance() > 1.2) v = v.multiply(1.2 / v.horizontalDistance(), 1, 1.2 / v.horizontalDistance());
        setLeaping(true);
        setCrouching(false);
        setDeltaMovement(v);
        getNavigation().stop();
        leapCooldown = 100;
        playSound(SoundEvents.OCELOT_HURT, 1.4F, 0.5F);
    }

    /** A spot with open air and a drop of at least {@link #LEDGE_DROP} next to it (a cliff edge), or not. */
    public boolean isLedge(BlockPos feet) {
        for (int[] o : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {2, 0}, {-2, 0}, {0, 2}, {0, -2}}) {
            int top = level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, feet.getX() + o[0], feet.getZ() + o[1]);
            if (top <= feet.getY() - LEDGE_DROP) return true;
        }
        return false;
    }

    @Override
    public Identifier texture() {
        return TEXTURE;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource source) {
        return super.causeFallDamage(Math.max(0, fallDistance - SAFE_FALL), damageModifier, source);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isDozing() || isCrouching() || !huntingTime() ? null : SoundEvents.FOX_SCREECH; // the cougar's scream, low
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.OCELOT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.OCELOT_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.5F;
    }

    /**
     * By day: up to the nearest ledge within {@link #LEDGE_RANGE} (the highest open spot with a drop beside it), and lie up
     * there; it stays put on the ledge until dusk.
     */
    class LedgeGoal extends Goal {
        private BlockPos ledge;

        LedgeGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
        }

        private BlockPos findLedge() {
            BlockPos me = blockPosition(), best = null;
            for (int i = 0; i < 24; i++) {
                int x = me.getX() + getRandom().nextInt(LEDGE_RANGE * 2 + 1) - LEDGE_RANGE;
                int z = me.getZ() + getRandom().nextInt(LEDGE_RANGE * 2 + 1) - LEDGE_RANGE;
                BlockPos p = new BlockPos(x, level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
                if (p.getY() > me.getY() - 2 && isLedge(p) && (best == null || p.getY() > best.getY())) best = p;
            }
            return best;
        }

        @Override
        public boolean canUse() {
            if (getTarget() != null || !level().isBrightOutside() || (tickCount + getId()) % 40 != 0) return false;
            if (onGround() && isLedge(blockPosition())) {
                ledge = blockPosition();
                return true; // already up: stay
            }
            ledge = findLedge();
            return ledge != null && getNavigation().moveTo(ledge.getX() + 0.5, ledge.getY(), ledge.getZ() + 0.5, 1.0);
        }

        @Override
        public boolean canContinueToUse() {
            return getTarget() == null && level().isBrightOutside() && ledge != null;
        }

        @Override
        public void tick() {
            if (blockPosition().distManhattan(ledge) <= 1) getNavigation().stop(); // up there: lie still (dozing comes on its own)
            else if (getNavigation().isDone() && !getNavigation().moveTo(ledge.getX() + 0.5, ledge.getY(), ledge.getZ() + 0.5, 1.0)) ledge = null;
        }
    }
}
