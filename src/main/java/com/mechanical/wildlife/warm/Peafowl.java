package com.mechanical.wildlife.warm;

import com.mechanical.AnimalFear;
import com.mechanical.wildlife.Pheasant;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Peafowl: a big pheasant of jungle edges and savanna woodland, in small parties. The peacock is iridescent blue with a
 * long green train of eyed feathers; now and then, standing in the open, he raises it into a shimmering fan and rattles it
 * for a few seconds ({@link #DISPLAY_TICKS}). The peahen is mottled brown with a green neck. Flushes like a pheasant, with
 * a loud scream. Butchered, a peacock gives Peacock Feathers besides the plain ones.
 */
public class Peafowl extends Pheasant {
    private static final EntityDataAccessor<Boolean> MALE = SynchedEntityData.defineId(Peafowl.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DISPLAY = SynchedEntityData.defineId(Peafowl.class, EntityDataSerializers.INT);
    private static final Identifier PEACOCK_TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/peacock.png");
    private static final Identifier PEAHEN_TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/peahen.png");
    public static final int DISPLAY_TICKS = 120;

    public Peafowl(EntityType<? extends Peafowl> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 8.0).add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    public boolean isPeacock() {
        return entityData.get(MALE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MALE, false);
        builder.define(DISPLAY, 0);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, SpawnGroupData groupData) {
        entityData.set(MALE, level.getRandom().nextFloat() < 0.4F);
        return super.finalizeSpawn(level, difficulty, reason, groupData);
    }

    /** 0..1: the train raised into a fan (eases up and down over 10 ticks), for the model. */
    public float displayAmount(float partialTick) {
        int t = entityData.get(DISPLAY);
        if (t <= 0) return 0;
        float edge = Math.min(t, DISPLAY_TICKS - t) + (t < DISPLAY_TICKS / 2 ? -partialTick : partialTick);
        return Math.clamp(edge / 10.0F, 0.0F, 1.0F);
    }

    public boolean isDisplaying() {
        return entityData.get(DISPLAY) > 0;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide()) return;
        int t = entityData.get(DISPLAY);
        if (t > 0) {
            getNavigation().stop();
            if (!onGround() || AnimalFear.isPanicking(this) || hurtTime > 0) t = 1; // folds it to run
            entityData.set(DISPLAY, t - 1);
            if (t == DISPLAY_TICKS - 20 || t == DISPLAY_TICKS / 2) playSound(SoundEvents.ARMADILLO_SCUTE_DROP, 0.6F, 1.8F); // the train rattles
        } else if (isPeacock() && !isBaby() && onGround() && !isDozing() && level().isBrightOutside() && random.nextInt(600) == 0
                && !AnimalFear.isPanicking(this)) {
            entityData.set(DISPLAY, DISPLAY_TICKS);
            playSound(SoundEvents.PARROT_IMITATE_PHANTOM, 1.2F, 1.6F); // the scream
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("peacock", isPeacock());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(MALE, input.getBooleanOr("peacock", false));
    }

    @Override
    public Identifier texture() {
        return isPeacock() ? PEACOCK_TEXTURE : PEAHEN_TEXTURE;
    }

    /** The peacock's crest and train, for the model. */
    @Override
    public boolean showsHorns() {
        return isPeacock();
    }

    @Override
    protected float babyChance() {
        return 0.0F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isPeacock() ? SoundEvents.PARROT_IMITATE_PHANTOM : null;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 900;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PARROT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PARROT_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 1.3F;
    }
}
