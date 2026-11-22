package com.mechanical.wildlife.forestbirds;

import java.util.Comparator;
import java.util.Map;
import java.util.WeakHashMap;

import com.mechanical.CarcassEntity;
import com.mechanical.Mechanical;
import com.mechanical.wildlife.Predator;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

/**
 * Griffon vulture: a carrion eater of the dry open country and the mountains. A group soars very high (SoaringBird),
 * and as soon as any of them sees a carcass lying out ({@link CarcassEntity}, within {@link #SIGHT}) they all circle
 * over it, a column of birds you can see from far off: there is meat there, for you and for the wolves and lions too,
 * which come over (night predators within {@link #LURE_RANGE} head for it). After a while of circling they drop down
 * and feed together; left alone long enough, only a pile of bones remains ({@link #FEED_TICKS} of feeding in all).
 * No carcass of its own (feathers and a Vulture Wing Bone, loot table): no one eats a carrion eater.
 */
public class Vulture extends SoaringBird {
    static final Kind KIND = new Kind("vulture", 0.4, 18, 34, 1200, 2400, 400, 1200, false, false, false, 1.0,
            () -> SoundEvents.CAT_HISS_BABY.value(), 0.45F);
    static final double SIGHT = 80, LURE_RANGE = 96;
    /** Circling this long over a carcass before going down; feeding ticks (all birds together) until it is bones. */
    static final int CIRCLE_TICKS = 600, FEED_TICKS = 2400;
    /** How long the vultures have fed on each carcass, all together. */
    private static final Map<CarcassEntity, Integer> FED = new WeakHashMap<>();
    private CarcassEntity carrion;
    private int circling;

    public Vulture(EntityType<? extends Vulture> type, Level level) {
        this(type, level, KIND);
    }

    /** For bigger carrion birds with their own flight (the condor, wildlife/desert). */
    protected Vulture(EntityType<? extends Vulture> type, Level level, Kind kind) {
        super(type, level, kind);
    }

    /** How far it sees a carcass from. */
    protected double sight() {
        return SIGHT;
    }

    /** Circling over a carcass: the sign of meat. */
    public boolean isCirclingCarrion() {
        return carrion != null && isFlying();
    }

    @Override
    protected double radius() {
        return 6 + getId() % 6;
    }

    @Override
    protected Vec3 focus() {
        return carrion != null && carrion.isAlive() ? carrion.position() : null;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if ((tickCount + getId()) % 40 == 0) {
            if (carrion == null || !carrion.isAlive() || carrion.hauler() != null || carrion.isPassenger()) {
                carrion = level.getEntitiesOfClass(CarcassEntity.class, getBoundingBox().inflate(sight(), 64, sight()),
                        c -> c.hauler() == null && c.hungAt() == null && !c.isPassenger())
                        .stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
                circling = 0;
            }
            if (carrion != null && isFlying() && (tickCount + getId()) % 200 == 0) lurePredators(level);
        }
        if (carrion == null || isFlying() || distanceToSqr(carrion) > 12) return;
        // feeding with the others
        restTimer = Math.max(restTimer, 40);
        getLookControl().setLookAt(carrion);
        int fed = FED.merge(carrion, 1, Integer::sum);
        if (fed % 25 == 0) playSound(SoundEvents.GENERIC_EAT.value(), 0.7F, 0.7F);
        if (fed % 60 == 0) playSound(SoundEvents.CAT_HISS_BABY.value(), 0.6F, 0.45F); // squabbling over it
        if (fed >= FEED_TICKS) {
            BlockPos pos = carrion.blockPosition();
            var pile = Mechanical.BONE_PILE.get().defaultBlockState();
            if (level.getBlockState(pos).canBeReplaced() && pile.canSurvive(level, pos)) level.setBlock(pos, pile, Block.UPDATE_ALL);
            FED.remove(carrion);
            carrion.discard();
            carrion = null;
        }
    }

    /** Wolves and lions see the circling birds: idle ones far off start towards the carcass (their scent goal takes over close by). */
    private void lurePredators(ServerLevel level) {
        for (Predator p : level.getEntitiesOfClass(Predator.class, carrion.getBoundingBox().inflate(LURE_RANGE, 32, LURE_RANGE))) {
            if (p.getTarget() == null && !p.isDozing() && !p.fearsPlayers() && p.getNavigation().isDone() && p.distanceToSqr(carrion) > 30 * 30)
                p.getNavigation().moveTo(carrion.getX(), carrion.getY(), carrion.getZ(), 1.0);
        }
    }

    @Override
    protected Vec3 cruise() {
        if (!scared && carrion != null && carrion.isAlive() && ++circling > CIRCLE_TICKS && leaderOrAlone()) {
            landingSpot = findLandingSpot(carrion.position(), 3);
            if (landingSpot != null) {
                landingTimer = 0;
                return approach(landingSpot);
            }
        }
        return super.cruise();
    }

    private boolean leaderOrAlone() {
        return leader == this || !leader.isFlying();
    }
}
