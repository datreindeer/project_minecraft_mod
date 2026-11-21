package com.mechanical.wildlife.underground;

import com.mechanical.Mechanical;
import com.mechanical.wildlife.Predator;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

/**
 * Vittra, the small grey folk under the earth (Y 0 to -30). Nearly blind: it hunts by sound. A player who mines within
 * {@link #HEAR_MINING} blocks or sprints within {@link #HEAR_SPRINT} (or just walks within {@link #HEAR_STEPS}, not
 * sneaking) is heard and hunted; a Vittra Hood muffles all of it. Its touch saps the strength to dig (Mining Fatigue). It
 * shuns light: someone holding a torch or standing in torchlight ({@link Underground#inLight}) is let go and backed away
 * from, and it won't stay in a lit spot itself.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public class Vittra extends WildAnimal implements Underground.Fearless {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/entity/wildlife/vittra.png");
    static final double HEAR_MINING = 24, HEAR_SPRINT = 20, HEAR_STEPS = 6;
    static final int CURSE_TICKS = 200;

    public Vittra(EntityType<? extends Vittra> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 14.0).add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 3.0).add(Attributes.FOLLOW_RANGE, 28.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.removeAllGoals(g -> g.getClass().getSimpleName().equals("GrazeGoal"));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.25, true));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    /** Wears the hood of vittra hide: makes no sound a vittra can hear. */
    static boolean muffled(Player p) {
        return p.getItemBySlot(EquipmentSlot.HEAD).is(UndergroundFauna.VITTRA_HOOD.get());
    }

    /** Hears {@code p} if it can: not muffled, not in light, alive and in survival. */
    void hear(Player p) {
        if (getTarget() == null && !muffled(p) && !p.isCreative() && !p.isSpectator() && !Underground.inLight(p)) setTarget(p);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide() || (tickCount + getId()) % 10 != 0) return;
        if (getTarget() == null) {
            for (Player p : level().players()) {
                double d = distanceTo(p);
                if (p.isSprinting() && d < HEAR_SPRINT || !p.isShiftKeyDown() && d < HEAR_STEPS) hear(p);
            }
        }
        LivingEntity t = getTarget();
        if (t != null && Underground.inLight(t)) Underground.backOff(this, t.position(), 1.3);
        else if (level().getBrightness(net.minecraft.world.level.LightLayer.BLOCK, blockPosition()) >= Underground.LIT) {
            Player near = level().getNearestPlayer(this, 16);
            Underground.backOff(this, near != null ? near.position() : position(), 1.3);
        }
    }

    /** Pick and stone ring out: every vittra within earshot comes. */
    @SubscribeEvent
    static void onBreak(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || event.getPlayer() == null) return;
        BlockPos at = event.getPos();
        for (Vittra v : level.getEntitiesOfClass(Vittra.class, new AABB(at).inflate(HEAR_MINING))) v.hear(event.getPlayer());
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof LivingEntity living) living.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, CURSE_TICKS, 0), this);
        return hit;
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
        return 0.0; // it doesn't see you coming: it hears you
    }

    @Override
    protected boolean sleepTime() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.55F; // a low, hollow whisper
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ALLAY_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ALLAY_DEATH;
    }
}
