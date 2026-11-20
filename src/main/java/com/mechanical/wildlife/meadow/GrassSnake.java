package com.mechanical.wildlife.meadow;

import java.util.EnumSet;

import com.mechanical.AnimalFear;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;

/**
 * Grass snake: long and slim, olive grey-green with black bars along the flanks and a yellow-and-black collar behind
 * the head; harmless. Near water - it swims well and hunts there - and in damp meadows. It slides away from people, and
 * when caught (hurt but not killed) it often plays dead: rolls belly up, mouth open, limp, for several seconds, then
 * comes to and makes off. Gives snakeskin and snake meat.
 */
public class GrassSnake extends Snake {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/grass_snake.png");
    /** Ticks left playing dead (synced for the belly-up pose). */
    private static final EntityDataAccessor<Integer> PLAYING_DEAD = SynchedEntityData.defineId(GrassSnake.class, EntityDataSerializers.INT);
    private static final float PLAY_DEAD_CHANCE = 0.65F;

    public GrassSnake(EntityType<? extends GrassSnake> type, Level level) {
        super(type, level);
        setPathfindingMalus(PathType.WATER, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 8.0).add(Attributes.MOVEMENT_SPEED, 0.24);
    }

    public boolean isPlayingDead() {
        return entityData.get(PLAYING_DEAD) > 0;
    }

    @Override
    public boolean isCoiled() {
        return super.isCoiled() || isPlayingDead();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PLAYING_DEAD, 0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // it goes into the water readily (WildAnimal's stroll keeps out of it)
        MeadowFauna.removeGoals(goalSelector, "WaterAvoidingRandomStrollGoal");
        goalSelector.addGoal(7, new RandomStrollGoal(this, 0.8));
        // above AnimalFear's flee goal (priority 0): playing dead it doesn't move at all
        goalSelector.addGoal(-1, new PlayDeadGoal(this));
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && isAlive() && !isPlayingDead() && !isInWater() && random.nextFloat() < PLAY_DEAD_CHANCE) {
            entityData.set(PLAYING_DEAD, 120 + random.nextInt(100));
            setBasking(false);
        }
        return hurt;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide()) return;
        int t = entityData.get(PLAYING_DEAD);
        if (t > 0) {
            entityData.set(PLAYING_DEAD, t - 1);
            if (t == 1) { // comes to and makes off
                Player near = level().getNearestPlayer(this, 16);
                if (near != null) AnimalFear.panic(this, near.position());
            }
        }
    }

    @Override
    public boolean isCharging() {
        return false; // never strikes
    }

    @Override
    public Identifier texture() {
        return TEXTURE;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    /** Limp and still while playing dead. */
    static class PlayDeadGoal extends Goal {
        private final GrassSnake snake;

        PlayDeadGoal(GrassSnake snake) {
            this.snake = snake;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return snake.isPlayingDead();
        }

        @Override
        public void start() {
            snake.getNavigation().stop();
        }

        @Override
        public void tick() {
            snake.getNavigation().stop();
        }
    }
}
