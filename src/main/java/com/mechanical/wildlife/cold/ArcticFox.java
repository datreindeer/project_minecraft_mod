package com.mechanical.wildlife.cold;

import java.util.Comparator;
import java.util.EnumSet;

import com.mechanical.Blood;
import com.mechanical.Calendar;
import com.mechanical.CarcassEntity;
import com.mechanical.Mechanical;
import com.mechanical.wildlife.WildAnimal;
import com.mechanical.wildlife.coldhunters.Lemming;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Arctic fox: small, round-eared and bushy-tailed. It shadows a polar bear at a distance ({@link FollowBearGoal}) and
 * lives on what the bear leaves: any carcass nobody guards - no player within 10 blocks, no bear right at it - it eats
 * down to the bones ({@link ScavengeGoal}). Hunts lemmings itself. Its coat follows the calendar
 * ({@link #whiteCoat(int)}): snow white from late autumn to the first month of spring, grey-brown the rest of the year.
 */
public class ArcticFox extends WildAnimal {
    private static final EntityDataAccessor<Boolean> WHITE = SynchedEntityData.defineId(ArcticFox.class, EntityDataSerializers.BOOLEAN);
    private static final Identifier WINTER = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/entity/wildlife/arctic_fox.png");
    private static final Identifier SUMMER = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/entity/wildlife/arctic_fox_summer.png");
    static final double BEAR_SEARCH = 32, BEAR_KEEP = 10, SCENT = 24, PEOPLE_GUARD = 10, BEAR_AT_CARCASS = 4;

    public ArcticFox(EntityType<? extends ArcticFox> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 8.0).add(Attributes.MOVEMENT_SPEED, 0.32).add(Attributes.ATTACK_DAMAGE, 2.0);
    }

    /**
     * White from the last month of autumn (day 240) through winter to the end of the first month of spring (day 29),
     * grey-brown from then to the end of summer and into early autumn. Days of the year as {@link Calendar#dayOfYear}.
     */
    public static boolean whiteCoat(int dayOfYear) {
        return dayOfYear >= 2 * Calendar.SEASON_DAYS + 2 * Calendar.MONTH_DAYS || dayOfYear < Calendar.MONTH_DAYS;
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
        builder.define(WHITE, true);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.removeAllGoals(g -> g.getClass().getSimpleName().equals("FollowHerdGoal")); // alone, or with its bear
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.5, false));
        goalSelector.addGoal(2, new ScavengeGoal());
        goalSelector.addGoal(5, new FollowBearGoal());
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Lemming.class, true, (t, level) -> !isBaby() && random.nextInt(4) == 0));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide() && (tickCount + getId()) % 100 == 1) entityData.set(WHITE, whiteCoat(Calendar.dayOfYear(level().getOverworldClockTime())));
    }

    @Override
    protected float babyChance() {
        return 0.4F;
    }

    @Override
    public double fearMultiplier() {
        return 1.2;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.FOX_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.FOX_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.FOX_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 1.15F;
    }

    /** Keeps within about 10 blocks of the nearest grown polar bear in sight of 32. */
    class FollowBearGoal extends Goal {
        private PolarBear bear;

        FollowBearGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (getTarget() != null || isDozing() || random.nextInt(20) != 0) return false;
            bear = level().getEntitiesOfClass(PolarBear.class, getBoundingBox().inflate(BEAR_SEARCH), b -> !b.isBaby()).stream()
                    .min(Comparator.comparingDouble(ArcticFox.this::distanceToSqr)).orElse(null);
            return bear != null && distanceTo(bear) > BEAR_KEEP && getNavigation().moveTo(bear, 1.1);
        }

        @Override
        public boolean canContinueToUse() {
            return bear.isAlive() && getTarget() == null && distanceTo(bear) > BEAR_KEEP * 0.6 && !getNavigation().isDone();
        }

        @Override
        public void tick() {
            if ((tickCount + getId()) % 20 == 0) getNavigation().moveTo(bear, 1.1);
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }
    }

    /** Eats an unguarded carcass within 24 blocks (8 s), leaving a Bone Pile; waits while a bear is still at it. */
    class ScavengeGoal extends Goal {
        private static final int FEED_TICKS = 160;
        private CarcassEntity carcass;
        private int eaten;

        ScavengeGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private boolean free(CarcassEntity c) {
            return c.isAlive() && c.haulerId() < 0 && !c.isPassenger() && c.hungAt() == null && c.animal() != getType()
                    && level().getNearestPlayer(c.getX(), c.getY(), c.getZ(), PEOPLE_GUARD, false) == null
                    && level().getEntitiesOfClass(PolarBear.class, c.getBoundingBox().inflate(BEAR_AT_CARCASS)).isEmpty();
        }

        @Override
        public boolean canUse() {
            if (getTarget() != null || isDozing() || isBaby() || random.nextInt(20) != 0) return false;
            carcass = level().getEntitiesOfClass(CarcassEntity.class, getBoundingBox().inflate(SCENT), this::free).stream()
                    .min(Comparator.comparingDouble(ArcticFox.this::distanceToSqr)).orElse(null);
            return carcass != null && getNavigation().moveTo(carcass, 1.1);
        }

        @Override
        public void start() {
            eaten = 0;
        }

        @Override
        public boolean canContinueToUse() {
            return getTarget() == null && free(carcass);
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
            double reach = 1.2 + (getBbWidth() + carcass.getBbWidth()) / 2;
            if (distanceToSqr(c.x, carcass.getY(), c.z) > reach * reach) {
                if (tickCount % 20 == 0) getNavigation().moveTo(carcass, 1.1);
                return;
            }
            getNavigation().stop();
            ServerLevel level = (ServerLevel) level();
            if (++eaten % 10 == 0) {
                Blood.spray(level, c.x, c.y, c.z, 2);
                playSound(SoundEvents.FOX_EAT, 0.8F, 1.1F);
            }
            if (eaten < FEED_TICKS) return;
            BlockPos pos = carcass.blockPosition();
            BlockState pile = Mechanical.BONE_PILE.get().defaultBlockState();
            if (level.getBlockState(pos).canBeReplaced() && pile.canSurvive(level, pos)) level.setBlock(pos, pile, Block.UPDATE_ALL);
            carcass.discard();
            heal(4.0F);
        }
    }
}
