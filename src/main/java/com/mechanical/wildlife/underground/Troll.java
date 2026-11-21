package com.mechanical.wildlife.underground;

import com.mechanical.Mechanical;
import com.mechanical.wildlife.Predator;
import com.mechanical.wildlife.WildAnimal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Troll of the deep (below Y -30): huge, strong and slow, with a heavy knockback swing. Hunts players it sees within
 * {@link #SIGHT} blocks (sneaking: {@link #SIGHT_SNEAKING}) but shuns light like the Vittra ({@link Underground#inLight}):
 * someone holding a torch or standing in torchlight is let go and backed away from. Caught by daylight (open sky over it
 * while the sun is up) it turns to stone on the spot: two blocks of Troll Stone, and nothing else is left.
 */
public class Troll extends WildAnimal implements Underground.Fearless {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/entity/wildlife/troll.png");
    static final double SIGHT = 16, SIGHT_SNEAKING = 6;

    public Troll(EntityType<? extends Troll> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 80.0).add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.ATTACK_DAMAGE, 12.0).add(Attributes.ATTACK_KNOCKBACK, 1.5).add(Attributes.ARMOR, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7).add(Attributes.FOLLOW_RANGE, 24.0).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.removeAllGoals(g -> g.getClass().getSimpleName().equals("GrazeGoal") || g.getClass().getSimpleName().equals("FollowHerdGoal"));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.15, true));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true,
                (t, level) -> !Underground.inLight(t) && distanceTo(t) < (t.isShiftKeyDown() ? SIGHT_SNEAKING : SIGHT)));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide() || (tickCount + getId()) % 10 != 0) return;
        if (inDaylight()) {
            petrify();
            return;
        }
        LivingEntity t = getTarget();
        if (t != null && Underground.inLight(t)) Underground.backOff(this, t.position(), 1.2);
        else if (level().getBrightness(LightLayer.BLOCK, blockPosition()) >= Underground.LIT) {
            Player near = level().getNearestPlayer(this, 16);
            Underground.backOff(this, near != null ? near.position() : position(), 1.2);
        }
    }

    /** Open sky over it while the sun is up. */
    boolean inDaylight() {
        return level().isBrightOutside() && level().canSeeSky(BlockPos.containing(getX(), getEyeY(), getZ()));
    }

    /** Turns to two blocks of Troll Stone where it stands (only into air or plants), and is gone. */
    void petrify() {
        if (!(level() instanceof ServerLevel level)) return;
        BlockState stone = UndergroundFauna.TROLL_STONE.get().defaultBlockState();
        BlockPos feet = blockPosition();
        for (BlockPos p : new BlockPos[] {feet, feet.above()}) {
            if (level.getBlockState(p).canBeReplaced()) level.setBlockAndUpdate(p, stone);
        }
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, stone), getX(), getY() + 1, getZ(), 40, 0.5, 1.0, 0.5, 0.1);
        level.playSound(null, feet, SoundEvents.STONE_PLACE, SoundSource.HOSTILE, 1.5F, 0.5F);
        discard();
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return entityTags().contains(Predator.NIGHT_TAG);
    }

    @Override
    public Identifier texture() {
        return TEXTURE;
    }

    @Override
    protected float babyChance() {
        return 0.0F;
    }

    @Override
    public double fearMultiplier() {
        return 0.0;
    }

    @Override
    protected boolean sleepTime() {
        return false;
    }

    @Override
    public float bleedThreshold() {
        return 0.35F;
    }

    @Override
    public boolean bloodOnlyWhenBleeding() {
        return true; // the warty hide is thick
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.RAVAGER_AMBIENT;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 300;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.7F;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.RAVAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.RAVAGER_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.RAVAGER_STEP, 0.6F, 0.8F);
    }
}
