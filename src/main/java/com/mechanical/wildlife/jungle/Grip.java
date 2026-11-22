package com.mechanical.wildlife.jungle;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * A victim held fast: the Python's coils, the Crocodile's jaws. The victim is kept at an anchor beside the holder (a
 * player who walks off is put back), hurt every {@link #INTERVAL} ticks, and can't get away on its own; only a blow from
 * someone else knocks the holder loose at once. The victim's own blows count as struggling: {@code struggles} of them and
 * it breaks free. Lets go when the victim dies or gets out of reach, and after {@code maxTicks}.
 */
final class Grip {
    static final int INTERVAL = 20;
    /** A player farther than this from the anchor is put back (smaller moves are the client's jitter). */
    private static final double SLIP = 0.6;
    private final Mob holder;
    private final float damage;
    private final int struggles, maxTicks;
    private LivingEntity victim;
    private Vec3 anchor;
    private int ticks, struggled;

    Grip(Mob holder, float damage, int struggles, int maxTicks) {
        this.holder = holder;
        this.damage = damage;
        this.struggles = struggles;
        this.maxTicks = maxTicks;
    }

    LivingEntity victim() {
        return victim;
    }

    boolean holding() {
        return victim != null;
    }

    void seize(LivingEntity target) {
        victim = target;
        anchor = target.position();
        ticks = 0;
        struggled = 0;
    }

    void release() {
        victim = null;
        anchor = null;
    }

    /** Where the victim is held; the holder can move it (the crocodile drags it under). */
    Vec3 anchor() {
        return anchor;
    }

    void moveAnchor(Vec3 to) {
        anchor = to;
    }

    /** Server tick. Returns false once it has let go. */
    boolean tick(ServerLevel level) {
        if (victim == null) return false;
        if (!victim.isAlive() || victim.isRemoved() || victim.level() != level || holder.distanceToSqr(victim) > 6 * 6
                || ++ticks > maxTicks || victim instanceof net.minecraft.world.entity.player.Player p && (p.isCreative() || p.isSpectator())) {
            release();
            return false;
        }
        // pinned: no walking, no jumping away
        victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 10, 5, false, false));
        if (victim instanceof ServerPlayer player) {
            if (player.position().distanceToSqr(anchor) > SLIP * SLIP) player.teleportTo(anchor.x, anchor.y, anchor.z);
        } else {
            victim.setPos(anchor);
            victim.setDeltaMovement(Vec3.ZERO);
        }
        if (ticks % INTERVAL == 0) victim.hurtServer(level, level.damageSources().mobAttack(holder), damage);
        return true;
    }

    /**
     * The holder is being hit: someone else knocks it loose at once; the victim struggles. Returns true if it let go.
     */
    boolean onHolderHurt(DamageSource source) {
        if (victim == null) return false;
        Entity attacker = source.getEntity();
        if (attacker == null) return false; // fire, falling, drowning: holds on
        if (attacker == victim && ++struggled < struggles) return false;
        release();
        return true;
    }
}
