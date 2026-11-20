package com.mechanical.wildlife.warm;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
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

import com.mechanical.wildlife.Pheasant;

/**
 * Red Jungle Fowl: the wild ancestor of the chicken, in small flocks on the jungle floor. Roosters are flame-red and gold
 * with a red comb, wattles and a sickle tail of glossy green-black; hens are brown and plain. Sits tight and bursts up
 * like a pheasant when you come close. The rooster crows at dawn. Hens lay a Jungle Fowl Egg on the ground now and then
 * ({@link #layTime}); thrown, one may hatch a chick.
 */
public class JungleFowl extends Pheasant {
    private static final EntityDataAccessor<Boolean> ROOSTER = SynchedEntityData.defineId(JungleFowl.class, EntityDataSerializers.BOOLEAN);
    private static final Identifier ROOSTER_TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/jungle_fowl_rooster.png");
    private static final Identifier HEN_TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/jungle_fowl_hen.png");
    private static final Identifier CHICK_TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/jungle_fowl_chick.png");
    private int layTime = 6000 + random.nextInt(6000);
    private boolean crowedToday;

    public JungleFowl(EntityType<? extends JungleFowl> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 5.0).add(Attributes.MOVEMENT_SPEED, 0.27);
    }

    public boolean isRooster() {
        return entityData.get(ROOSTER);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ROOSTER, false);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, SpawnGroupData groupData) {
        entityData.set(ROOSTER, level.getRandom().nextFloat() < 0.35F);
        return super.finalizeSpawn(level, difficulty, reason, groupData);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel server) || isBaby()) return;
        if (!isRooster() && onGround() && --layTime <= 0) {
            playSound(SoundEvents.CHICKEN_EGG, 1.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);
            spawnAtLocation(server, WarmFauna.JUNGLE_FOWL_EGG.get());
            layTime = 6000 + random.nextInt(6000);
        }
        // the rooster greets the sunrise
        long time = server.getOverworldClockTime() % 24000;
        if (isRooster() && time > 23000 && !crowedToday && random.nextInt(200) == 0) {
            crowedToday = true;
            playSound(SoundEvents.PARROT_IMITATE_ZOMBIE_VILLAGER, 1.4F, 1.6F);
        } else if (time > 1000 && time < 23000) {
            crowedToday = false;
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("rooster", isRooster());
        output.putInt("EggLayTime", layTime);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(ROOSTER, input.getBooleanOr("rooster", false));
        layTime = input.getIntOr("EggLayTime", layTime);
    }

    @Override
    public Identifier texture() {
        return isBaby() ? CHICK_TEXTURE : isRooster() ? ROOSTER_TEXTURE : HEN_TEXTURE;
    }

    /** The rooster's comb, wattles and sickle tail, for the model. */
    @Override
    public boolean showsHorns() {
        return isRooster() && !isBaby();
    }

    @Override
    protected float babyChance() {
        return 0.3F;
    }

    private static net.minecraft.world.entity.animal.chicken.ChickenSoundVariant.ChickenSoundSet chicken() {
        return SoundEvents.CHICKEN_SOUNDS.get(net.minecraft.world.entity.animal.chicken.ChickenSoundVariants.SoundSet.CLASSIC).adultSounds();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return chicken().ambientSound().value();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return chicken().hurtSound().value();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return chicken().deathSound().value();
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 1.1F;
    }
}
