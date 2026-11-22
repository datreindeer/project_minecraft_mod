package com.mechanical.wildlife.jungle;

import java.util.EnumSet;

import com.mechanical.AnimalFear;
import com.mechanical.CarcassEntity;
import com.mechanical.Mechanical;
import com.mechanical.wildlife.WildAnimal;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Monitor Lizard: a big grey-olive varan with yellow beading, 2 blocks nose to tail. A scavenger: it smells a carcass from
 * {@link #SCENT} blocks, walks to it and eats it down to the bones in 10 s (a hunter's kill left lying is lost to it).
 * Leaves people alone unless they hurt it, then bites back: its bite is venomous (Poison I, 8 s, and Hunger). Swims well.
 * Gives Monitor Skin (leather), meat and sometimes a Venom Gland.
 */
public class MonitorLizard extends WildAnimal {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/monitor_lizard.png");
    static final double SCENT = 24;
    static final int FEED_TICKS = 200, VENOM_TICKS = 160;

    public MonitorLizard(EntityType<? extends MonitorLizard> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 18.0).add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.ATTACK_DAMAGE, 3.0).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    public WaterHabit waterHabit() {
        return WaterHabit.SWIMMER;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.4, false));
        goalSelector.addGoal(4, new ScavengeGoal());
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    public Identifier texture() {
        return TEXTURE;
    }

    @Override
    public double fearMultiplier() {
        return 0.5;
    }

    @Override
    protected float babyChance() {
        return 0.2F;
    }

    /** The venomous bite. */
    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean bit = super.doHurtTarget(level, target);
        if (bit && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.POISON, VENOM_TICKS, 0), this);
            living.addEffect(new MobEffectInstance(MobEffects.HUNGER, VENOM_TICKS, 0), this);
        }
        playSound(SoundEvents.CAT_SOUNDS.get(net.minecraft.world.entity.animal.feline.CatSoundVariants.SoundSet.CLASSIC).adultSounds().hissSound().value(), 0.8F, 0.6F);
        return bit;
    }

    /**
     * One tick of feeding at {@code carcass} (it must be in reach). Returns true when it is eaten: gone, a Bone Pile left
     * where it lay.
     */
    public boolean feed(CarcassEntity carcass, int feeding) {
        ServerLevel level = (ServerLevel) level();
        Vec3 c = carcass.getBoundingBox().getCenter();
        if (feeding % 10 == 0) {
            com.mechanical.Blood.spray(level, c.x, c.y, c.z, 2);
            playSound(SoundEvents.GENERIC_EAT.value(), 0.7F, 0.7F);
        }
        if (feeding < FEED_TICKS) return false;
        BlockPos pos = carcass.blockPosition();
        var pile = Mechanical.BONE_PILE.get().defaultBlockState();
        if (carcass.hungAt() == null && level.getBlockState(pos).canBeReplaced() && pile.canSurvive(level, pos))
            level.setBlock(pos, pile, net.minecraft.world.level.block.Block.UPDATE_ALL);
        carcass.discard();
        return true;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SILVERFISH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SILVERFISH_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.45F;
    }

    /** Smells out an unattended carcass and eats it. */
    class ScavengeGoal extends Goal {
        private CarcassEntity carcass;
        private int feeding;

        ScavengeGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (getTarget() != null || isDozing() || AnimalFear.isPanicking(MonitorLizard.this) || random.nextInt(20) != 0) return false;
            carcass = level().getEntitiesOfClass(CarcassEntity.class, getBoundingBox().inflate(SCENT),
                    c -> c.hauler() == null && !c.isPassenger() && c.animal() != getType())
                    .stream().min(java.util.Comparator.comparingDouble(MonitorLizard.this::distanceToSqr)).orElse(null);
            return carcass != null && getNavigation().moveTo(carcass, 1.0);
        }

        @Override
        public void start() {
            feeding = 0;
        }

        @Override
        public boolean canContinueToUse() {
            return carcass.isAlive() && carcass.hauler() == null && getTarget() == null && !AnimalFear.isPanicking(MonitorLizard.this);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Vec3 c = carcass.getBoundingBox().getCenter();
            getLookControl().setLookAt(c);
            double reach = 1.0 + (getBbWidth() + carcass.getBbWidth()) / 2;
            if (distanceToSqr(c.x, carcass.getY(), c.z) > reach * reach) {
                if (tickCount % 20 == 0) getNavigation().moveTo(carcass, 1.0);
                return;
            }
            getNavigation().stop();
            feed(carcass, ++feeding);
        }

        @Override
        public void stop() {
            getNavigation().stop();
            carcass = null;
        }
    }
}
