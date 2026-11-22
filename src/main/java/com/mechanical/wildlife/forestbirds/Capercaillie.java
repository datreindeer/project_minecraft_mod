package com.mechanical.wildlife.forestbirds;

import com.mechanical.AnimalFear;
import com.mechanical.wildlife.FlockBird;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Western capercaillie: the big grouse of the old pine forests. Walks the forest floor in small parties, roosts in the
 * pines, and bursts up with a loud clatter, flying low and short. At dawn a cock may sing on his lek: tail fanned, neck
 * stretched, wings drooped, a song of {@link #SONG} ticks of clicks, a cork-pop and a scraping "grind". While he grinds
 * he hears nothing: a hunter can walk up on him then, in steps between the songs, the old way it was done.
 * The hen is smaller and brown ({@link #isHen()}). Leaves a carcass (Birds), nests on the ground (BirdNestBlock).
 */
public class Capercaillie extends FlockBird {
    static final Kind KIND = new Kind("capercaillie", 0.42, 2, 6, 60, 160, 800, 2000, false, false, true, 0.9,
            () -> SoundEvents.CHICKEN_AMBIENT_BABY.value(), 0.4F);
    private static final EntityDataAccessor<Boolean> HEN = SynchedEntityData.defineId(Capercaillie.class, EntityDataSerializers.BOOLEAN);
    /** Ticks of display left (the cock's lek song), synced for the display pose. */
    private static final EntityDataAccessor<Integer> DISPLAY = SynchedEntityData.defineId(Capercaillie.class, EntityDataSerializers.INT);
    private static final Identifier COCK = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/capercaillie.png");
    private static final Identifier HEN_TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/capercaillie_hen.png");
    /** One song: clicks (0-39), the pop (44), the grind (46-71), a pause. A display lasts {@link #DISPLAY_TICKS}. */
    static final int SONG = 80, GRIND_START = 44, GRIND_END = 72, DISPLAY_TICKS = 6 * SONG;
    /** While grinding he only notices a player this close. */
    static final double DEAF_RANGE = 2.0;

    public Capercaillie(EntityType<? extends Capercaillie> type, Level level) {
        super(type, level, KIND);
    }

    /** For other grouse (the black grouse, wildlife.cold): the same lek and the same hen, their own kind and look. */
    protected Capercaillie(EntityType<? extends Capercaillie> type, Level level, Kind kind) {
        super(type, level, kind);
    }

    /** When a cock may start his lek song: at dawn. */
    protected boolean lekTime() {
        return ForestBirdsFauna.dawn(level());
    }

    public boolean isHen() {
        return entityData.get(HEN);
    }

    public boolean isDisplaying() {
        return entityData.get(DISPLAY) > 0;
    }

    /** In the grinding part of a song: deaf to the world. */
    public boolean isGrinding() {
        int t = entityData.get(DISPLAY);
        int phase = (DISPLAY_TICKS - t) % SONG;
        return t > 0 && phase >= GRIND_START && phase < GRIND_END;
    }

    @Override
    public Identifier texture() {
        return isHen() ? HEN_TEXTURE : COCK;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HEN, false);
        builder.define(DISPLAY, 0);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, SpawnGroupData groupData) {
        entityData.set(HEN, random.nextBoolean());
        return super.finalizeSpawn(level, difficulty, reason, groupData);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("hen", isHen());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(HEN, input.getBooleanOr("hen", false));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide()) return;
        int t = entityData.get(DISPLAY);
        if (t > 0) {
            if (isFlying() || !onGround() || isDozing() || AnimalFear.isPanicking(this)) {
                entityData.set(DISPLAY, 0);
                return;
            }
            entityData.set(DISPLAY, t - 1);
            getNavigation().stop();
            int phase = (DISPLAY_TICKS - t) % SONG;
            // clicks, closer and closer together, then the pop, then the soft scraping grind
            if (phase < 40 && phase % (phase < 20 ? 8 : 4) == 0) playSound(SoundEvents.NOTE_BLOCK_HAT.value(), 0.7F, 1.5F + phase * 0.01F);
            else if (phase == GRIND_START) playSound(SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 0.9F, 1.8F);
            else if (phase > GRIND_START && phase < GRIND_END && phase % 3 == 0) playSound(SoundEvents.NOTE_BLOCK_SNARE.value(), 0.25F, 1.9F + random.nextFloat() * 0.2F);
        } else if (!isHen() && onGround() && !isFlying() && !isDozing() && !isInWater() && lekTime()
                && (tickCount + getId()) % 20 == 0 && random.nextInt(15) == 0) {
            entityData.set(DISPLAY, DISPLAY_TICKS);
        }
    }

    /** Deaf while grinding: only a player right next to him (or a panic) puts him up. */
    @Override
    protected Vec3 threat() {
        if (!isGrinding()) return super.threat();
        Player p = level().getNearestPlayer(this, DEAF_RANGE);
        return p != null && !p.isCreative() && !p.isSpectator() ? p.position() : null;
    }

    @Override
    protected void takeOff(boolean frightened, Vec3 from) {
        if (!isFlying() && frightened) playSound(SoundEvents.PARROT_FLY, 1.4F, 0.55F); // the loud clatter of a big grouse
        entityData.set(DISPLAY, 0);
        super.takeOff(frightened, from);
    }
}
