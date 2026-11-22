package com.mechanical.wildlife.jungle;

import com.mechanical.AnimalFear;
import com.mechanical.wildlife.Predator;
import com.mechanical.wildlife.WildAnimal;
import com.mechanical.wildlife.meadow.Snake;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Python: a heavy, patterned constrictor, 3 blocks long. It lies still for hours, coiled on a branch of the canopy or at
 * the water's edge, and strikes whatever comes within {@link #STRIKE_RANGE} blocks; from a branch it drops onto prey
 * passing below. Then it coils round its victim and squeezes ({@link Grip}): a held player can't walk or jump away and
 * loses half a heart a second, until someone else hits the python loose (one blow) or he struggles free (5 blows of his
 * own); after 15 s it gives up. No venom. Gives Python Skin (cut into Snakeskin) and meat.
 */
public class Python extends Snake {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/python.png");
    private static final EntityDataAccessor<Boolean> CONSTRICTING = SynchedEntityData.defineId(Python.class, EntityDataSerializers.BOOLEAN);
    static final double STRIKE_RANGE = 1.8;
    /** From a branch: prey this far below (and within STRIKE_RANGE + 1 sideways) is dropped on. */
    private static final int DROP_HEIGHT = 6;
    private static final int COOLDOWN = 600;
    private final Grip grip = new Grip(this, 1.0F, 5, 300);
    private int cooldown;

    public Python(EntityType<? extends Python> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.16)
                .add(Attributes.ATTACK_DAMAGE, 2.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CONSTRICTING, false);
    }

    /** Coiled round a victim (synced for the model). */
    public boolean isConstricting() {
        return entityData.get(CONSTRICTING);
    }

    public LivingEntity victim() {
        return grip.victim();
    }

    @Override
    public boolean isCoiled() {
        return super.isCoiled() || isConstricting();
    }

    @Override
    public Identifier texture() {
        return TEXTURE;
    }

    @Override
    public double fearMultiplier() {
        return 0.2; // trusts its camouflage
    }

    /** What it squeezes: people and the smaller animals, never other hunters. */
    boolean isPrey(LivingEntity e) {
        if (e instanceof Player p) return !p.isCreative() && !p.isSpectator();
        return e instanceof WildAnimal && !(e instanceof Predator) && !(e instanceof Python) && !(e instanceof Crocodile)
                && !(e instanceof Tiger) && e.getBbHeight() < 1.3F && e.isAlive();
    }

    /** Coils round {@code target} (it must be close). */
    public void constrict(LivingEntity target) {
        grip.seize(target);
        entityData.set(CONSTRICTING, true);
        setBasking(false);
        getNavigation().stop();
        playSound(hiss(), 1.0F, 0.6F);
    }

    private void letGo(Vec3 from) {
        grip.release();
        entityData.set(CONSTRICTING, false);
        cooldown = COOLDOWN;
        if (from != null) AnimalFear.panic(this, from);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) return;
        if (cooldown > 0) cooldown--;
        if (isConstricting()) {
            LivingEntity v = grip.victim();
            if (!grip.tick(level)) {
                letGo(null);
                return;
            }
            setPos(grip.anchor().x, grip.anchor().y, grip.anchor().z); // wrapped round it
            setDeltaMovement(Vec3.ZERO);
            getLookControl().setLookAt(v);
        } else if (cooldown == 0 && !isNoAi() && (tickCount + getId()) % 5 == 0 && !AnimalFear.isPanicking(this)) {
            strike(level);
        }
    }

    /** Strikes prey in reach; from a branch, drops onto prey passing below. */
    void strike(ServerLevel level) {
        boolean branch = level.getBlockState(blockPosition().below()).is(BlockTags.LEAVES);
        AABB reach = getBoundingBox().inflate(STRIKE_RANGE + (branch ? 1 : 0), 1, STRIKE_RANGE + (branch ? 1 : 0))
                .expandTowards(0, branch ? -DROP_HEIGHT : 0, 0);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, reach, this::isPrey)) {
            if (e instanceof Player p && p.isSteppingCarefully() && distanceTo(p) > 1.2) continue; // crept past
            if (branch && e.getY() < getY() - 1) setPos(e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ()); // drops from the branch
            constrict(e);
            return;
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isConstricting() && grip.onHolderHurt(source)) letGo(source.getEntity() == null ? null : source.getEntity().position());
        return super.hurtServer(level, source, amount);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isConstricting() ? hiss() : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SILVERFISH_HURT;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.6F;
    }
}
