package com.mechanical.folklore;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * A folklore being (docs/folklore-plan.md): only players whose sight ({@link Faith#sight}) reaches {@link #sight()} see it
 * (client.folklore.BeingsClient) or can click it; nothing hurts it, and a blow from a player makes it {@link #vanish} (or
 * whatever {@link #struck} says). No carcass, no drops, no sounds unless a being has its own. The first time a player sees
 * a kind: +5 Faith and the advancement folklore/&lt;kind&gt;.
 */
public abstract class Spirit extends PathfinderMob {
    static final double SEEN_RANGE = 32;

    protected Spirit(EntityType<? extends Spirit> type, Level level) {
        super(type, level);
        setSilent(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    /** Sight needed to see it. */
    public abstract float sight();

    /** Kind for Faith.seen and the advancement folklore/&lt;kind&gt;. */
    abstract String kind();

    public boolean seenBy(Player p) {
        return Faith.canSee(p, sight());
    }

    /** The nearest player within r who can see it, or null. */
    Player nearestSeer(double r) {
        return level().getNearestPlayer(getX(), getY(), getZ(), r, e -> e instanceof Player p && seenBy(p));
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurtServer(level, source, damage);
        if (source.getEntity() instanceof ServerPlayer p && source.getDirectEntity() == p) struck(level, p);
        return false;
    }

    /** A player hit it: by default it is gone. */
    void struck(ServerLevel level, ServerPlayer player) {
        vanish();
    }

    /** Gone in a puff, nothing left. */
    void vanish() {
        if (level() instanceof ServerLevel level) level.sendParticles(ParticleTypes.POOF, getX(), getY() + getBbHeight() / 2, getZ(), 12, 0.3, 0.4, 0.3, 0.02);
        discard();
    }

    @Override
    public boolean isPickable() {
        return !level().isClientSide() || seenBy(Faith.localPlayer.get());
    }

    @Override
    public boolean canBeHitByProjectile() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {}

    @Override
    public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource source) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level) || (tickCount + getId()) % 20 != 0) return;
        for (ServerPlayer p : level.getPlayers(p -> p.distanceToSqr(this) < SEEN_RANGE * SEEN_RANGE && seenBy(p))) Faith.seen(p, kind(), "folklore/" + kind());
    }
}
