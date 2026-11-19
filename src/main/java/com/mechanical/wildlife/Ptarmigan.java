package com.mechanical.wildlife;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Willow ptarmigan: a plump grouse of the taiga and the snow. Sits tight (lets you come close), then whirs up low over
 * the ground with the covey and drops down again a short way off. White in winter plumage where it snows, mottled
 * chestnut-brown (white wings and belly) elsewhere.
 */
public class Ptarmigan extends FlockBird {
    private static final EntityDataAccessor<Boolean> WHITE = SynchedEntityData.defineId(Ptarmigan.class, EntityDataSerializers.BOOLEAN);
    private static final Identifier SUMMER = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/ptarmigan.png");
    private static final Identifier WINTER = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/ptarmigan_winter.png");

    public Ptarmigan(EntityType<? extends Ptarmigan> type, Level level) {
        super(type, level, PTARMIGAN);
    }

    public boolean isWhite() {
        return entityData.get(WHITE);
    }

    @Override
    public Identifier texture() {
        return isWhite() ? WINTER : SUMMER;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WHITE, false);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, SpawnGroupData groupData) {
        entityData.set(WHITE, level.getBiome(blockPosition()).is(BiomeTags.SPAWNS_SNOW_FOXES));
        return super.finalizeSpawn(level, difficulty, reason, groupData);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("white", isWhite());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(WHITE, input.getBooleanOr("white", false));
    }
}
