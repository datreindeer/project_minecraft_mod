package com.mechanical.wildlife.waterfowl;

import com.mechanical.wildlife.FlockBird;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Mallard: the common wild duck of rivers, lakes and swamps. Paddles about in small flocks and now and then tips up to
 * dabble (tail in the air, head under), roosts on the bank, and at the first sight of a player the whole flock jumps
 * straight off the water and whirs away. Drakes have the green head, white collar and chestnut breast; hens are
 * mottled brown. Both show the blue speculum on the wing. Nests by the water (BirdNestBlock).
 */
public class Mallard extends FlockBird {
    public static final Kind KIND = new Kind("mallard", 0.55, 4, 12, 200, 500, 500, 1400, false, true, false, 1.1,
            () -> SoundEvents.FROG_AMBIENT, 1.6F);
    private static final EntityDataAccessor<Boolean> DRAKE = SynchedEntityData.defineId(Mallard.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DABBLE = SynchedEntityData.defineId(Mallard.class, EntityDataSerializers.INT);
    private static final int DABBLE_TICKS = 40;
    private static final Identifier DRAKE_TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/mallard_drake.png");
    private static final Identifier HEN_TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/mallard_hen.png");

    public Mallard(EntityType<? extends Mallard> type, Level level) {
        super(type, level, KIND);
    }

    public boolean isDrake() {
        return entityData.get(DRAKE);
    }

    @Override
    public Identifier texture() {
        return isDrake() ? DRAKE_TEXTURE : HEN_TEXTURE;
    }

    /** 0..1: tipped up, eased in and out over the first and last 8 ticks. */
    public float dabbleAmount(float partialTick) {
        int t = entityData.get(DABBLE);
        if (t <= 0) return 0.0F;
        return Math.clamp((Math.min(t, DABBLE_TICKS - t) + partialTick) / 8.0F, 0.0F, 1.0F);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DRAKE, false);
        builder.define(DABBLE, 0);
    }

    @Override
    public void aiStep() {
        int t = entityData.get(DABBLE);
        if (t > 0 && !level().isClientSide()) entityData.set(DABBLE, isInWater() && !isFlying() ? t - 1 : 0);
        super.aiStep();
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (isInWater() && !isFlying() && !isDozing() && entityData.get(DABBLE) == 0 && (tickCount + getId()) % 20 == 0 && random.nextInt(12) == 0) {
            entityData.set(DABBLE, DABBLE_TICKS);
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, SpawnGroupData groupData) {
        entityData.set(DRAKE, level.getRandom().nextBoolean());
        return super.finalizeSpawn(level, difficulty, reason, groupData);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("drake", isDrake());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(DRAKE, input.getBooleanOr("drake", false));
    }
}
