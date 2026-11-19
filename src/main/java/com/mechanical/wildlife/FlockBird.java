package com.mechanical.wildlife;

import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

import com.mechanical.AncientOak;
import com.mechanical.AnimalFear;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * A bird that lives in a flock. On the ground it walks, pecks and roosts at night like other wildlife; now and then the
 * flock leader (the member with the lowest entity id) takes off and the others follow it into the air, either in a
 * loose cloud (each holds its own slot around the leader, keeping apart from its neighbours) or, for geese, in a V.
 * After a while the leader picks a landing spot (ground, tree tops or water, depending on the kind) and the flock
 * comes down around it. A player getting close (or any panic, see AnimalFear) makes the whole flock burst up and
 * fly off away from the threat. Any arrow kills (feathers drop; small birds leave no carcass, see Butchery).
 */
public class FlockBird extends WildAnimal {
    /**
     * Per-kind behaviour. speed: cruising speed (blocks/tick); altitudes are above the surface; times in ticks.
     * vFormation: followers fly in a V behind the leader. landsOnWater / perches: may land on water / on tree tops.
     */
    public record Kind(String name, double speed, int minAlt, int maxAlt, int minFlight, int maxFlight, int minRest, int maxRest,
            boolean vFormation, boolean landsOnWater, boolean perches, double fear, Supplier<SoundEvent> ambient, float pitch) {}

    public static final Kind CROW = new Kind("crow", 0.42, 6, 14, 300, 700, 500, 1400, false, false, true, 1.2,
            () -> SoundEvents.PARROT_AMBIENT, 0.5F);
    public static final Kind GOOSE = new Kind("goose", 0.5, 12, 24, 500, 1000, 700, 1800, true, true, false, 1.0,
            () -> SoundEvents.GOAT_AMBIENT, 1.45F);
    public static final Kind ROCK_DOVE = new Kind("rock_dove", 0.5, 5, 12, 200, 500, 400, 1200, false, false, false, 1.0,
            () -> SoundEvents.CAT_PURR_BABY.value(), 0.6F);
    public static final Kind PTARMIGAN = new Kind("ptarmigan", 0.45, 2, 5, 60, 140, 600, 1600, false, false, false, 0.7,
            () -> SoundEvents.CHICKEN_AMBIENT_BABY.value(), 0.55F);
    public static final Kind SEAGULL = new Kind("seagull", 0.36, 9, 18, 500, 1200, 400, 1000, false, true, false, 0.9,
            () -> SoundEvents.FOX_SCREECH, 1.35F);

    private static final EntityDataAccessor<Boolean> FLYING = SynchedEntityData.defineId(FlockBird.class, EntityDataSerializers.BOOLEAN);
    private static final double FLOCK_RANGE = 24, ROAM = 40;
    /** Notice distance for a sneaking / walking / sprinting player (times {@link Kind#fear()}). */
    private static final double NOTICE_SNEAK = 3.5, NOTICE_WALK = 11, NOTICE_SPRINT = 18;

    protected final Kind kind;
    private final Identifier texture;

    // ---- server-side flight state (not saved: a reloaded bird is on the ground, or flutters down to it) ----
    protected Vec3 home;
    protected Vec3 target;
    protected Vec3 landingSpot;
    protected boolean scared;
    protected int flightTimer, restTimer, landingTimer;
    protected List<FlockBird> flock = List.of();
    protected FlockBird leader = this;
    private float paddleYaw;

    // ---- client-side animation ----
    private float flap, flapO, flapSpeed, glide, glideO;

    public FlockBird(EntityType<? extends FlockBird> type, Level level, Kind kind) {
        super(type, level);
        this.kind = kind;
        this.texture = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/" + kind.name() + ".png");
        this.restTimer = 100 + random.nextInt(kind.maxRest());
    }

    public static AttributeSupplier.Builder createAttributes(double health) {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, health).add(Attributes.MOVEMENT_SPEED, 0.22);
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public Identifier texture() {
        return texture;
    }

    public boolean isFlying() {
        return entityData.get(FLYING);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FLYING, false);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.removeAllGoals(g -> g instanceof net.minecraft.world.entity.ai.goal.FollowParentGoal); // no young
    }

    @Override
    protected float babyChance() {
        return 0.0F;
    }

    @Override
    public double fearMultiplier() {
        return kind.fear();
    }

    /** Any arrow kills: a bird is too small to keep flying with one in it. */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (source.getDirectEntity() instanceof AbstractArrow) damage = Math.max(damage, getHealth() + 1.0F);
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt) alarmFlock(source);
        return hurt;
    }

    /** A bird hit or shot down: the rest of the flock bursts up, away from whoever did it (or from the fallen bird). */
    private void alarmFlock(DamageSource source) {
        Vec3 from = source.getEntity() != null ? source.getEntity().position() : position();
        refreshFlock();
        if (isAlive()) takeOff(true, from);
        for (FlockBird b : flock) b.takeOff(true, from);
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource source) {
        return false;
    }

    @Override
    protected void onWoken(Player near) {
        scare(near.position());
    }

    // ---------------------------------------------------------------- client animation

    /** Wing-beat phase (radians) for the model. */
    public float flapPhase(float partialTick) {
        return flapO + (flap - flapO) * partialTick;
    }

    /** 0 = flapping, 1 = gliding on still wings. */
    public float glideAmount(float partialTick) {
        return glideO + (glide - glideO) * partialTick;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide()) {
            flapO = flap;
            glideO = glide;
            if (isFlying()) {
                // beat hard while climbing or slow, glide on still wings when coasting down; geese beat steadily
                double dy = getY() - yo;
                boolean coast = dy < -0.04 && !kind.vFormation() || (tickCount + getId() * 7) % 90 > 60 && dy < 0.05;
                glide = Mth.approach(glide, coast ? 1.0F : 0.0F, 0.08F);
                flapSpeed = Mth.approach(flapSpeed, dy > 0.05 ? 1.25F : 0.85F, 0.05F);
                flap += flapSpeed * (1.0F - glide * 0.9F) * (kind == GOOSE ? 0.55F : 0.9F);
            } else {
                glide = Mth.approach(glide, 0.0F, 0.2F);
            }
            return;
        }
        if (!isFlying() && !onGround() && !isInWater()) {
            var v = getDeltaMovement();
            if (v.y < 0) setDeltaMovement(v.x, v.y * 0.6, v.z); // flutters down instead of falling
        }
    }

    @Override
    public void travel(Vec3 input) {
        if (isFlying()) {
            move(MoverType.SELF, getDeltaMovement()); // velocity is steered directly in customServerAiStep
            return;
        }
        super.travel(input);
    }

    // ---------------------------------------------------------------- server behaviour

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (home == null) home = position();
        if (tickCount % 10 == 0 || !leader.isAlive()) refreshFlock();
        if (isFlying()) flyTick(level);
        else groundTick(level);
    }

    private void refreshFlock() {
        flock = level().getEntitiesOfClass(FlockBird.class, getBoundingBox().inflate(FLOCK_RANGE),
                b -> b != this && b.isAlive() && b.getType() == getType());
        leader = flock.stream().filter(b -> b.getId() < getId()).min(Comparator.comparingInt(FlockBird::getId)).orElse(this);
    }

    /** Where a noticed player stands, or what it panics from; null when nothing scares it. */
    protected Vec3 threat() {
        if (AncientOak.isCalm(this)) return null;
        Player p = level().getNearestPlayer(this, NOTICE_SPRINT * kind.fear() * 1.5);
        boolean panicking = AnimalFear.isPanicking(this);
        if (p == null || p.isCreative() || p.isSpectator()) return panicking ? position().add(random.nextGaussian(), 0, random.nextGaussian()) : null;
        if (panicking) return p.position();
        double range = AncientOak.noticeRange(p, (p.isCrouching() ? NOTICE_SNEAK : p.isSprinting() ? NOTICE_SPRINT : NOTICE_WALK) * kind.fear());
        if (isFlying()) range *= 0.6; // up in the air they let you come closer
        double dist = distanceTo(p);
        return dist < range && (dist < NOTICE_SNEAK || hasLineOfSight(p)) ? p.position() : null;
    }

    /** The whole flock nearby bursts into the air, away from {@code from}. */
    public void scare(Vec3 from) {
        takeOff(true, from);
        for (FlockBird b : flock) {
            b.takeOff(true, from);
        }
    }

    protected void takeOff(boolean frightened, Vec3 from) {
        if (frightened && !scared) {
            scared = true;
            flightTimer = 200 + random.nextInt(160);
            landingSpot = null;
            if (from != null) target = pickTarget(from);
        }
        if (isFlying()) return;
        wake();
        entityData.set(FLYING, true);
        setNoGravity(true);
        getNavigation().stop();
        if (!frightened) flightTimer = kind.minFlight() + random.nextInt(kind.maxFlight() - kind.minFlight() + 1);
        landingSpot = null;
        Vec3 away = from == null ? Vec3.ZERO : position().subtract(from).multiply(1, 0, 1).normalize().scale(0.3);
        setDeltaMovement(away.x + (random.nextDouble() - 0.5) * 0.15, 0.4 + random.nextDouble() * 0.15, away.z + (random.nextDouble() - 0.5) * 0.15);
        playSound(SoundEvents.PARROT_FLY, frightened ? 1.0F : 0.6F, 0.9F + random.nextFloat() * 0.3F);
        if (target == null || !frightened) target = pickTarget(from);
    }

    protected void land() {
        entityData.set(FLYING, false);
        setNoGravity(false);
        scared = false;
        landingSpot = null;
        target = null;
        restTimer = kind.minRest() + random.nextInt(kind.maxRest() - kind.minRest() + 1);
        setDeltaMovement(getDeltaMovement().scale(0.2));
    }

    private void groundTick(ServerLevel level) {
        if (isNoGravity()) setNoGravity(false);
        if (isInWater()) {
            if (!kind.landsOnWater()) {
                takeOff(false, null);
                return;
            }
            swim();
        }
        if (isDozing()) return; // only waking (onWoken) or a hit gets it up
        Vec3 threat = threat();
        if (threat != null) {
            scare(threat);
            return;
        }
        if (restTimer > 0) restTimer--;
        if (level.isDarkOutside()) return; // roosts for the night
        if (leader == this) {
            if (restTimer <= 0) takeOff(false, null);
        } else if (leader.isFlying() && !leader.scared && distanceTo(leader) < 32 && random.nextInt(8) == 0) {
            takeOff(false, null); // follows the leader up, one after another
        }
    }

    /** Floats with the body half under and paddles about slowly. */
    private void swim() {
        double depth = getFluidHeight(FluidTags.WATER);
        Vec3 v = getDeltaMovement();
        if (random.nextInt(80) == 0) paddleYaw = random.nextFloat() * 360.0F;
        double vx = v.x, vz = v.z;
        if (getNavigation().isDone() && !isDozing()) {
            vx += -Mth.sin(paddleYaw * Mth.DEG_TO_RAD) * 0.004;
            vz += Mth.cos(paddleYaw * Mth.DEG_TO_RAD) * 0.004;
            setYRot(Mth.approachDegrees(getYRot(), paddleYaw, 3.0F));
            yBodyRot = getYRot();
        }
        setDeltaMovement(vx, Mth.clamp((depth - getBbHeight() * 0.3) * 0.3, -0.06, 0.06), vz);
    }

    private void flyTick(ServerLevel level) {
        getNavigation().stop();
        if (!isNoGravity()) setNoGravity(true);
        flightTimer--;
        if (!scared && tickCount % 10 == 0) {
            Vec3 threat = threat();
            if (threat != null) scare(threat);
        }
        if (level.isDarkOutside() && !scared) flightTimer = Math.min(flightTimer, 40); // home to roost
        boolean following = leader != this && leader.isFlying();
        if (landingSpot == null) {
            if (following && leader.landingSpot != null) landingSpot = findLandingSpot(leader.landingSpot, 4);
            else if (!following && flightTimer <= 0 && leader != this && leader.isAlive()) landingSpot = findLandingSpot(leader.position(), 4);
            else if (!following && flightTimer <= 0) {
                Vec3 ahead = position().add(getDeltaMovement().multiply(20, 0, 20));
                landingSpot = findLandingSpot(ahead, 10);
                if (landingSpot == null) landingSpot = findLandingSpot(position(), 8);
                if (landingSpot == null) flightTimer = 60; // nowhere to come down yet: fly on a little
            }
            if (landingSpot != null) landingTimer = 0;
        }
        Vec3 desired;
        if (landingSpot != null) {
            desired = approach(landingSpot);
            if (onGround() || kind.landsOnWater() && isInWater() || ++landingTimer > 400) {
                land();
                return;
            }
        } else if (following) {
            desired = follow(leader);
        } else {
            desired = cruise();
        }
        steer(desired);
    }

    /** Leader / lone bird: heads for its target, picking a new one when it gets there. */
    protected Vec3 cruise() {
        if (target == null || horizontalDistSqr(target) < 16) target = pickTarget(null);
        return keepAltitude(seek(target, speed()), 2);
    }

    /** Follower: holds its own slot around (or, for geese, in a V behind) the leader, apart from its neighbours. */
    protected Vec3 follow(FlockBird lead) {
        Vec3 slot;
        if (kind.vFormation()) {
            int rank = 1 + (int) flock.stream().filter(b -> b.leader == lead && b != lead && b.getId() < getId()).count();
            Vec3 fwd = lead.getDeltaMovement().multiply(1, 0, 1);
            fwd = fwd.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0, lead.getYRot()) : fwd.normalize();
            Vec3 right = new Vec3(-fwd.z, 0, fwd.x);
            int k = (rank + 1) / 2;
            double side = rank % 2 == 0 ? -1 : 1;
            slot = lead.position().subtract(fwd.scale(1.7 * k)).add(right.scale(side * 1.5 * k)).add(0, 0.25 * k, 0);
        } else {
            // a fixed offset per bird, drifting slowly, so the flock looks like a loose, shifting cloud
            double a = getId() * 2.399, t = tickCount * 0.03 + getId();
            double r = 2.0 + (getId() % 5) * 0.7;
            slot = lead.position().add(Math.cos(a + 0.4 * Math.sin(t)) * r, Math.sin(a * 1.7) * 1.5 + 0.6 * Math.sin(t * 1.3), Math.sin(a + 0.4 * Math.cos(t)) * r);
        }
        Vec3 desired = lead.getDeltaMovement().add(slot.subtract(position()).scale(0.12));
        for (FlockBird b : flock) {
            double d = b.distanceTo(this);
            if (b.isFlying() && d < 1.4 && d > 1.0E-3) desired = desired.add(position().subtract(b.position()).scale(0.08 / d));
        }
        double max = speed() * 1.4;
        if (desired.lengthSqr() > max * max) desired = desired.normalize().scale(max);
        return keepAltitude(desired, 2);
    }

    /** Glides in to the landing spot, staying above the trees until close, slowing down at the end. */
    protected Vec3 approach(Vec3 spot) {
        double dist = Math.sqrt(horizontalDistSqr(spot));
        Vec3 desired = seek(spot, speed() * Mth.clamp(dist / 6.0, 0.3, 1.0));
        return dist > 4 ? keepAltitude(desired, 3) : desired;
    }

    protected double speed() {
        return kind.speed() * (scared ? 1.3 : 1.0);
    }

    protected Vec3 seek(Vec3 to, double speed) {
        Vec3 d = to.subtract(position());
        return d.lengthSqr() < 1.0E-4 ? Vec3.ZERO : d.normalize().scale(speed);
    }

    /** Keeps at least {@code margin} blocks over the ground (trees, water) below, and not too far above it. */
    protected Vec3 keepAltitude(Vec3 desired, double margin) {
        double ground = surfaceY(getX(), getZ());
        if (getY() < ground + margin) return new Vec3(desired.x, Math.max(desired.y, 0.25), desired.z);
        if (getY() > ground + kind.maxAlt() + 8) return new Vec3(desired.x, Math.min(desired.y, -0.12), desired.z);
        return desired;
    }

    /** Eases the velocity towards {@code desired} and turns the body along it. */
    protected void steer(Vec3 desired) {
        Vec3 v = getDeltaMovement();
        v = v.add(desired.subtract(v).scale(0.12));
        if (horizontalCollision) v = new Vec3(v.x * 0.5, Math.max(v.y, 0.3), v.z * 0.5); // pull up over the obstacle
        setDeltaMovement(v);
        if (v.horizontalDistanceSqr() > 1.0E-4) {
            float yaw = (float) (Mth.atan2(v.z, v.x) * Mth.RAD_TO_DEG) - 90.0F;
            setYRot(Mth.wrapDegrees(Mth.approachDegrees(getYRot(), yaw, 14.0F)));
        }
        yBodyRot = getYRot();
        yHeadRot = getYRot();
    }

    /** A new cruising target: away from a threat, or somewhere around home. */
    protected Vec3 pickTarget(Vec3 from) {
        for (int i = 0; i < 10; i++) {
            double x, z;
            if (from != null) {
                double base = Math.atan2(getZ() - from.z, getX() - from.x) + (random.nextDouble() - 0.5) * 1.6;
                double d = 28 + random.nextDouble() * 20;
                x = getX() + Math.cos(base) * d;
                z = getZ() + Math.sin(base) * d;
            } else {
                double a = random.nextDouble() * Math.PI * 2, d = 8 + random.nextDouble() * ROAM;
                x = home.x + Math.cos(a) * d;
                z = home.z + Math.sin(a) * d;
                if ((x - getX()) * (x - getX()) + (z - getZ()) * (z - getZ()) < 12 * 12) continue;
            }
            if (!level().hasChunkAt(BlockPos.containing(x, getY(), z))) continue;
            return new Vec3(x, surfaceY(x, z) + kind.minAlt() + random.nextInt(kind.maxAlt() - kind.minAlt() + 1), z);
        }
        return position().add(random.nextGaussian() * 8, 4, random.nextGaussian() * 8);
    }

    /** A spot around {@code around} where this kind may come down; water birds try water first. Null if none. */
    protected Vec3 findLandingSpot(Vec3 around, int radius) {
        for (int pass = kind.landsOnWater() ? 0 : 1; pass < 2; pass++) {
            for (int i = 0; i < 12; i++) {
                int x = Mth.floor(around.x) + random.nextInt(radius * 2 + 1) - radius;
                int z = Mth.floor(around.z) + random.nextInt(radius * 2 + 1) - radius;
                BlockPos col = BlockPos.containing(x, getY(), z);
                if (!level().hasChunkAt(col)) continue;
                BlockPos top = level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, col);
                BlockState below = level().getBlockState(top.below());
                boolean water = below.getFluidState().is(FluidTags.WATER);
                if (pass == 0 ? water : canLandOn(below, water)) return new Vec3(x + 0.5, top.getY(), z + 0.5);
            }
        }
        return null;
    }

    protected boolean canLandOn(BlockState below, boolean water) {
        if (water) return kind.landsOnWater();
        if (!below.getFluidState().isEmpty() || below.is(BlockTags.FIRE) || below.is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK)) return false;
        if (below.is(BlockTags.LEAVES)) return kind.perches();
        return below.isSolid();
    }

    protected double surfaceY(double x, double z) {
        BlockPos col = BlockPos.containing(x, getY(), z);
        if (!level().hasChunkAt(col)) return getY() - 8;
        return level().getHeight(Heightmap.Types.MOTION_BLOCKING, col.getX(), col.getZ());
    }

    protected double horizontalDistSqr(Vec3 p) {
        double dx = p.x - getX(), dz = p.z - getZ();
        return dx * dx + dz * dz;
    }

    // ---------------------------------------------------------------- sounds

    @Override
    protected SoundEvent getAmbientSound() {
        return kind.ambient().get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return isFlying() ? 100 : 240;
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
        return super.getVoicePitch() * kind.pitch();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.PARROT_STEP, 0.1F, 1.0F);
    }
}
