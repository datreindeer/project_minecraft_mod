package com.mechanical.wildlife.forestbirds;

import java.util.Comparator;

import com.mechanical.Sanity;
import com.mechanical.wildlife.FlockBird;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Eurasian eagle-owl: the night hunter of the forest. Sits out the day dozing on a branch or a stump (a player who
 * comes close puts it up; under its roost lie the odd Owl Pellet), and at dusk it wakes and quarters the forest low and
 * utterly silent (no wing sound at all), now and then hooting its deep double "hoo-hoo". Hearing it in the dark wears
 * on an already uneasy mind (Sanity). It drops on small animals ({@link ForestBirdsFauna#OWL_PREY}) from the air and
 * kills them outright, then sits by the kill a while: the carcass is left for whoever finds it.
 * No carcass of its own: it drops feathers and its soft Owl Feathers (loot table).
 */
public class Owl extends FlockBird {
    static final Kind KIND = new Kind("owl", 0.34, 3, 8, 160, 400, 300, 900, false, false, true, 0.8,
            () -> SoundEvents.NOTE_BLOCK_FLUTE.value(), 0.5F);
    static final double HUNT_RANGE = 20, STRIKE = 1.3;
    /** Sanity lost by a player within {@link #HOOT_RANGE} of a hoot at night, if already below Sanity.UNEASY. */
    static final float HOOT_DREAD = 1.0F;
    static final double HOOT_RANGE = 20;
    private LivingEntity prey;
    private int echo;

    public Owl(EntityType<? extends Owl> type, Level level) {
        super(type, level, KIND);
    }

    /** Roosts by day, flies by night. */
    @Override
    protected boolean restTime(Level level) {
        return !level.isDarkOutside();
    }

    @Override
    protected boolean sleepTime() {
        return !level().isDarkOutside();
    }

    /** Silent flight: no wing clatter when it takes off. */
    @Override
    public void playSound(SoundEvent sound, float volume, float pitch) {
        if (sound == SoundEvents.PARROT_FLY) return;
        super.playSound(sound, volume, pitch);
    }

    @Override
    protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {}

    @Override
    protected SoundEvent getAmbientSound() {
        return level().isDarkOutside() ? SoundEvents.NOTE_BLOCK_FLUTE.value() : null;
    }

    @Override
    public void playAmbientSound() {
        super.playAmbientSound();
        if (level().isClientSide() || !level().isDarkOutside()) return;
        echo = 9; // the second "hoo"
        for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(HOOT_RANGE))) {
            if (!p.isCreative() && !p.isSpectator() && Sanity.of(p) < Sanity.UNEASY) Sanity.set(p, Sanity.of(p) - HOOT_DREAD);
        }
    }

    @Override
    public int getAmbientSoundInterval() {
        return 300;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel level)) return;
        if (echo > 0 && --echo == 0) playSound(SoundEvents.NOTE_BLOCK_FLUTE.value(), getSoundVolume(), getVoicePitch() * 0.94F);
        // a pellet now and then under the day roost
        if (!isFlying() && onGround() && restTime(level) && random.nextInt(12000) == 0) spawnAtLocation(level, new ItemStack(ForestBirdsFauna.OWL_PELLET.get()));
    }

    @Override
    protected Vec3 cruise() {
        if (!level().isDarkOutside()) return super.cruise();
        if (prey != null && (!prey.isAlive() || distanceToSqr(prey) > HUNT_RANGE * HUNT_RANGE * 2.25)) prey = null;
        if (prey == null && (tickCount + getId()) % 40 == 0) prey = findPrey();
        if (prey == null) return super.cruise();
        Vec3 at = prey.position().add(0, prey.getBbHeight() * 0.5, 0);
        if (distanceToSqr(at) < STRIKE * STRIKE) {
            // talons first: a small animal doesn't survive it; then down by the kill
            prey.hurtServer((ServerLevel) level(), damageSources().mobAttack(this), 40.0F);
            landingSpot = prey.position();
            landingTimer = 0;
            prey = null;
            return seek(landingSpot, speed() * 0.5);
        }
        double dist = Math.sqrt(horizontalDistSqr(at));
        // glide in level above it, then drop steeply for the last few blocks
        Vec3 aim = dist > 5 ? new Vec3(at.x, Math.max(at.y + 3, getY() - 0.2), at.z) : at;
        return seek(aim, speed() * (dist > 5 ? 1.2 : 1.6));
    }

    private LivingEntity findPrey() {
        return level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(HUNT_RANGE, 12, HUNT_RANGE),
                e -> e.isAlive() && e.getType().builtInRegistryHolder().is(ForestBirdsFauna.OWL_PREY) && e.onGround() && e.getY() < getY())
                .stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
    }

    @Override
    protected void land() {
        prey = null;
        super.land();
    }
}
