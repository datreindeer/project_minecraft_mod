package com.mechanical.wildlife.waterfowl;

import com.mechanical.wildlife.FlockBird;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Mute swan: big, white, mostly silent; drifts on lakes and rivers in pairs and flies in lines with slow, whistling wing
 * beats. Unhurried about people (lets you come closer than other birds), except at its nest: a player who comes within
 * {@link #GUARD} blocks of a nest it claimed is hissed at, and the swan comes at him with raised wings, beating and
 * biting, until he is {@link #GIVE_UP} blocks from the nest. Hit near its nest, it fights instead of flying off.
 */
public class MuteSwan extends FlockBird {
    public static final Kind KIND = new Kind("mute_swan", 0.45, 8, 18, 400, 900, 900, 2000, true, true, false, 0.6,
            () -> SoundEvents.GOAT_AMBIENT, 0.7F);
    static final double GUARD = 6, GIVE_UP = 12, DEFEND = 16;

    public MuteSwan(EntityType<? extends MuteSwan> type, Level level) {
        super(type, level, KIND);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAttributes(16.0).add(Attributes.ATTACK_DAMAGE, 3.0).add(Attributes.ATTACK_KNOCKBACK, 0.6);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.5, true));
    }

    private boolean nearNest(Vec3 at, double range) {
        return nest != null && at.distanceToSqr(Vec3.atCenterOf(nest)) < range * range;
    }

    /** Defending: it doesn't scare. */
    @Override
    protected Vec3 threat() {
        return getTarget() != null ? null : super.threat();
    }

    @Override
    protected void takeOff(boolean frightened, Vec3 from) {
        if (getTarget() == null) super.takeOff(frightened, from);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (source.getEntity() instanceof LivingEntity attacker && !(attacker instanceof MuteSwan) && nearNest(position(), DEFEND)
                && !(attacker instanceof Player p && (p.isCreative() || p.isSpectator()))) {
            if (isFlying()) land();
            defend(attacker); // set before the hit, so the flock alarm doesn't send it up
        }
        return super.hurtServer(level, source, damage);
    }

    private void defend(LivingEntity target) {
        if (getTarget() != target) playSound(SoundEvents.CAT_HISS_BABY.value(), 1.2F, 0.4F + random.nextFloat() * 0.1F);
        setTarget(target);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if ((tickCount + getId()) % 10 != 0) return;
        LivingEntity target = getTarget();
        if (target != null) {
            if (!target.isAlive() || !nearNest(target.position(), GIVE_UP) || target instanceof Player p && (p.isCreative() || p.isSpectator())) {
                setTarget(null);
                setAggressive(false);
            } else if (random.nextInt(4) == 0) {
                playSound(SoundEvents.CAT_HISS_BABY.value(), 1.0F, 0.4F + random.nextFloat() * 0.1F);
            }
            return;
        }
        if (nest == null || isFlying() || isDozing()) return;
        Vec3 n = Vec3.atCenterOf(nest);
        Player p = level.getNearestPlayer(n.x, n.y, n.z, GUARD, pl -> pl instanceof Player pp && !pp.isCreative() && !pp.isSpectator());
        if (p != null && distanceToSqr(n) < DEFEND * DEFEND) defend(p);
    }
}
