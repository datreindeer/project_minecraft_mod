package com.mechanical.wildlife.desert;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A tamed {@link FennecFox}: follows its owner, sits when right-clicked (again to get up) and eats meat from the hand to
 * heal. Its great ears hear what is coming: at night (or anywhere dark outside), when something hostile ({@link Enemy})
 * comes within {@link #HEAR_RANGE} of it that it hasn't warned of lately, it yips and turns to face it, the creature is
 * outlined (Glowing for {@link #MARK_TICKS}) and its owner, if near, is told. Doesn't fight. No carcass.
 */
public class TameFennec extends TamableAnimal {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/fennec_fox.png");
    public static final double HEAR_RANGE = 20, OWNER_RANGE = 48;
    public static final int MARK_TICKS = 120, REWARN_TICKS = 600, CHECK = 20;
    /** Game time each creature was last warned of (not saved: a reload may warn again). */
    private final Map<UUID, Long> warned = new HashMap<>();

    public TameFennec(EntityType<? extends TameFennec> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 0.33);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        goalSelector.addGoal(5, new FollowOwnerGoal(this, 1.3, 6.0F, 2.0F));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    public Identifier texture() {
        return TEXTURE;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false; // no breeding
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    /** Owner: meat heals it; otherwise sit / stand. */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isOwnedBy(player)) return super.mobInteract(player, hand);
        ItemStack stack = player.getItemInHand(hand);
        if (FennecFox.isMeat(stack) && getHealth() < getMaxHealth()) {
            if (!level().isClientSide()) {
                stack.consume(1, player);
                heal(4.0F);
                playSound(SoundEvents.FOX_EAT, 1.0F, 1.4F);
            }
            return InteractionResult.SUCCESS;
        }
        if (!level().isClientSide()) {
            setOrderedToSit(!isOrderedToSit());
            jumping = false;
            getNavigation().stop();
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level() instanceof ServerLevel level && (tickCount + getId()) % CHECK == 0 && listening()) listen(level);
    }

    /** Dark outside (night, or a storm's gloom): its ears are up. */
    public boolean listening() {
        return level().isDarkOutside();
    }

    /** Server: warns of the nearest hostile creature it hasn't warned of lately; returns it, or null. */
    public Mob listen(ServerLevel level) {
        long now = level.getGameTime();
        warned.values().removeIf(t -> now - t > REWARN_TICKS);
        Mob threat = level.getEntitiesOfClass(Mob.class, getBoundingBox().inflate(HEAR_RANGE),
                        m -> m instanceof Enemy && m.isAlive() && !warned.containsKey(m.getUUID()))
                .stream().min(java.util.Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        if (threat == null) return null;
        warned.put(threat.getUUID(), now);
        threat.addEffect(new MobEffectInstance(MobEffects.GLOWING, MARK_TICKS, 0, false, false), this);
        getLookControl().setLookAt(threat, 30.0F, 30.0F);
        playSound(SoundEvents.FOX_SCREECH, 1.2F, 1.5F);
        if (getOwner() instanceof Player owner && owner.distanceTo(this) < OWNER_RANGE)
            owner.sendOverlayMessage(Component.translatable("message.mechanical.fennec.warns", threat.getDisplayName()));
        return threat;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isInSittingPose() && !listening() ? SoundEvents.FOX_SLEEP : SoundEvents.FOX_AMBIENT;
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
        return super.getVoicePitch() * 1.5F;
    }

    @Override
    public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
        return false;
    }
}
