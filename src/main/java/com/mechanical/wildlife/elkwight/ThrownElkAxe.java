package com.mechanical.wildlife.elkwight;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Elkwight's axe in flight: it spins out at {@link #SPEED} blocks a tick to a little past where its target stood,
 * hitting everything on the way ({@link #DAMAGE}), then flies back to him ({@link Elkwight#catchAxe}). Striking a solid
 * block it sticks fast, and he has to walk over and pull it out: the window to hit him. When he dies it comes down as
 * his heavy axe ({@link Elkwight#die}); with him gone (unloaded) it vanishes and he gets it back.
 */
public class ThrownElkAxe extends Entity {
    static final double SPEED = 1.1, OVERSHOOT = 4;
    static final float DAMAGE = 14;
    static final int MAX_FLIGHT = 120;
    /** Synced for the renderer: spinning (flying out or back), or stuck in a block. */
    private static final EntityDataAccessor<Boolean> STUCK = SynchedEntityData.defineId(ThrownElkAxe.class, EntityDataSerializers.BOOLEAN);

    private java.util.UUID ownerId;
    private int ownerMissing;
    private double outLeft;
    private boolean returning;
    private int flight;
    private final Set<Integer> hit = new HashSet<>();

    public ThrownElkAxe(EntityType<? extends ThrownElkAxe> type, Level level) {
        super(type, level);
    }

    /** Server: the axe leaves {@code owner}'s hands at {@code target} (led a little where it runs). */
    static ThrownElkAxe thrownBy(ServerLevel level, Elkwight owner, LivingEntity target) {
        ThrownElkAxe axe = new ThrownElkAxe(Elkwights.THROWN_AXE.get(), level);
        Vec3 from = owner.position().add(0, 2.6, 0);
        Vec3 aim = target.position().add(0, target.getBbHeight() * 0.5, 0).add(target.getDeltaMovement().scale(8));
        Vec3 dir = aim.subtract(from).normalize();
        axe.setPos(from.subtract(0, 0.6, 0));
        axe.setDeltaMovement(dir.scale(SPEED));
        axe.ownerId = owner.getUUID();
        axe.outLeft = aim.distanceTo(from) + OVERSHOOT;
        level.addFreshEntity(axe);
        return axe;
    }

    public boolean isStuck() {
        return entityData.get(STUCK);
    }

    private Elkwight owner() {
        return ownerId != null && ((ServerLevel) level()).getEntity(ownerId) instanceof Elkwight e && e.isAlive() ? e : null;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            if (isStuck() && random.nextInt(6) == 0) // the runes glow
                level().addParticle(net.minecraft.core.particles.ParticleTypes.GLOW, getX() + random.nextGaussian() * 0.3, getY() + 0.5, getZ() + random.nextGaussian() * 0.3, 0, 0.02, 0);
            return;
        }
        ServerLevel level = (ServerLevel) level();
        Elkwight owner = owner();
        if (owner == null) { // its bearer is unloaded: it returns to him (Elkwight#fetchAxe)
            if (++ownerMissing > 100) discard();
            return;
        }
        ownerMissing = 0;
        if (isStuck()) return;
        if (++flight % 6 == 0) playSound(SoundEvents.TRIDENT_RIPTIDE_1.value(), 1.0F, 0.5F);
        if (returning || flight > MAX_FLIGHT) {
            returning = true;
            Vec3 to = owner.position().add(0, 2.0, 0).subtract(position());
            if (to.length() < 1.6 || flight > MAX_FLIGHT * 2) {
                owner.catchAxe(this);
                return;
            }
            setDeltaMovement(to.normalize().scale(SPEED));
            setPos(position().add(getDeltaMovement())); // flying back it passes through everything, like it was called
        } else {
            Vec3 from = position(), to = from.add(getDeltaMovement());
            BlockHitResult block = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (block.getType() != HitResult.Type.MISS) { // sticks fast in the block
                setPos(block.getLocation().subtract(getDeltaMovement().normalize().scale(0.3)));
                setDeltaMovement(Vec3.ZERO);
                entityData.set(STUCK, true);
                playSound(SoundEvents.TRIDENT_HIT_GROUND, 1.5F, 0.5F);
                return;
            }
            setPos(to);
            outLeft -= SPEED;
            if (outLeft <= 0) returning = true;
        }
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.3), e -> e != owner && e.isAlive()))
            if (hit.add(e.getId())) owner.strike(level, e, DAMAGE, 0.6, 0.25);
        setYRot(getYRot() + 40);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(STUCK, false);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        entityData.set(STUCK, input.getBooleanOr("stuck", false));
        ownerId = input.read("owner", net.minecraft.core.UUIDUtil.CODEC).orElse(null);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putBoolean("stuck", isStuck());
        if (ownerId != null) output.store("owner", net.minecraft.core.UUIDUtil.CODEC, ownerId);
    }
}
