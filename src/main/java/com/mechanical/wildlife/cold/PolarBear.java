package com.mechanical.wildlife.cold;

import java.util.Comparator;
import java.util.EnumSet;

import com.mechanical.Blood;
import com.mechanical.CarcassEntity;
import com.mechanical.Mechanical;
import com.mechanical.wildlife.BrownBear;
import com.mechanical.wildlife.Reindeer;
import com.mechanical.wildlife.coast.Seal;
import com.mechanical.wildlife.coast.Walrus;
import com.mechanical.wildlife.giants.MuskOx;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Polar bear, in place of vanilla's: the brown bear's build and fighting (rears and swipes, never flees, a mother guards
 * her cub), but a hunter. It picks out a seal (or a walrus, reindeer or musk ox calf) within {@link #PREY_RANGE} blocks,
 * or a player within {@link #PLAYER_RANGE} (8 sneaking), and stalks it: walks slowly in, head low, until it is
 * {@link #STALK_UNTIL} blocks off, then charges. A good swimmer. It eats a fresh kill for 10 s - the fat and the best
 * of it - and leaves the rest ({@link #LEFTOVERS}), which the Arctic Fox that follows it lives on. Never hibernates.
 */
public class PolarBear extends BrownBear {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/entity/wildlife/polar_bear.png");
    private static final Identifier CUB = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/entity/wildlife/polar_bear_cub.png");
    static final double PREY_RANGE = 24, PLAYER_RANGE = 20, PLAYER_RANGE_SNEAKING = 8, STALK_UNTIL = 5;
    /** Tag on a carcass a polar bear has fed on: it does not go back to it; foxes may clean it up. */
    public static final String LEFTOVERS = "mechanical.polar_bear_leftovers";

    public PolarBear(EntityType<? extends PolarBear> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 70.0).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 9.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.7).add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    public WaterHabit waterHabit() {
        return WaterHabit.SWIMMER;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // no berries, no den: it hunts the ice all year
        goalSelector.removeAllGoals(g -> switch (g.getClass().getSimpleName()) {
            case "ForageBerriesGoal", "LeaveDenGoal", "SeekDenGoal" -> true;
            default -> false;
        });
        goalSelector.addGoal(0, new StalkGoal());
        goalSelector.addGoal(4, new FeedGoal());
        targetSelector.addGoal(3, new HuntGoal());
    }

    /** Seals above all; the calves of the big tundra animals. */
    public static boolean isPrey(LivingEntity e) {
        return e instanceof Seal || (e instanceof Walrus || e instanceof Reindeer || e instanceof MuskOx) && e.isBaby();
    }

    /** Awake day and night (the polar night has no day); no winter sleep. */
    @Override
    protected boolean sleepTime() {
        return false;
    }

    @Override
    public Identifier texture() {
        return isBaby() ? CUB : TEXTURE;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() / 0.85F; // the brown bear lowers it; the polar bear's own voice
    }

    /** Chooses quarry: the nearest prey animal in range, else a player it can see in range. */
    class HuntGoal extends TargetGoal {
        HuntGoal() {
            super(PolarBear.this, false);
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (isBaby() || isDozing() || getTarget() != null || random.nextInt(10) != 0) return false;
            targetMob = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(PREY_RANGE), e -> e.isAlive() && isPrey(e))
                    .stream().min(Comparator.comparingDouble(PolarBear.this::distanceToSqr)).orElse(null);
            if (targetMob != null) return true;
            Player p = level().getNearestPlayer(PolarBear.this, PLAYER_RANGE);
            if (p == null || p.isCreative() || p.isSpectator() || distanceTo(p) > (p.isShiftKeyDown() ? PLAYER_RANGE_SNEAKING : PLAYER_RANGE)
                    || !hasLineOfSight(p)) return false;
            targetMob = p;
            return true;
        }

        @Override
        public void start() {
            setTarget(targetMob);
            super.start();
        }
    }

    /** Far off, it walks in slowly and low instead of charging; close in (or once hurt by it), the bear's attack takes over. */
    class StalkGoal extends Goal {
        private int repath;

        StalkGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = getTarget();
            return t != null && t.isAlive() && !isBaby() && getLastHurtByMob() != t && distanceTo(t) > STALK_UNTIL;
        }

        @Override
        public void start() {
            repath = 0;
        }

        @Override
        public void tick() {
            LivingEntity t = getTarget();
            getLookControl().setLookAt(t, 20.0F, 20.0F);
            if (--repath <= 0) {
                getNavigation().moveTo(t, 0.6);
                repath = 10;
            }
        }
    }

    /** A fresh carcass near by (not a bear's, nobody standing over it): eats for 10 s, then leaves the rest. */
    class FeedGoal extends Goal {
        private static final int FEED_TICKS = 200;
        private CarcassEntity carcass;
        private int eaten;

        FeedGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private boolean edible(CarcassEntity c) {
            return c.isAlive() && !c.entityTags().contains(LEFTOVERS) && c.animal() != getType() && c.haulerId() < 0 && !c.isPassenger()
                    && level().getNearestPlayer(c.getX(), c.getY(), c.getZ(), 6, false) == null;
        }

        @Override
        public boolean canUse() {
            if (getTarget() != null || isBaby() || random.nextInt(20) != 0) return false;
            carcass = level().getEntitiesOfClass(CarcassEntity.class, getBoundingBox().inflate(16), this::edible).stream()
                    .min(Comparator.comparingDouble(PolarBear.this::distanceToSqr)).orElse(null);
            return carcass != null && getNavigation().moveTo(carcass, 1.0);
        }

        @Override
        public void start() {
            eaten = 0;
        }

        @Override
        public boolean canContinueToUse() {
            return getTarget() == null && edible(carcass);
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Vec3 c = carcass.getBoundingBox().getCenter();
            getLookControl().setLookAt(c);
            double reach = 1.3 + (getBbWidth() + carcass.getBbWidth()) / 2;
            if (distanceToSqr(c.x, carcass.getY(), c.z) > reach * reach) {
                if (tickCount % 20 == 0) getNavigation().moveTo(carcass, 1.0);
                return;
            }
            getNavigation().stop();
            if (++eaten % 10 == 0) {
                Blood.spray((ServerLevel) level(), c.x, c.y, c.z, 3);
                playSound(SoundEvents.GENERIC_EAT.value(), 0.9F, 0.6F);
            }
            if (eaten < FEED_TICKS) return;
            carcass.addTag(LEFTOVERS);
            heal(10.0F);
        }
    }
}
