package com.mechanical.wildlife.giants;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

import com.mechanical.Blood;
import com.mechanical.wildlife.Predator;
import com.mechanical.wildlife.WildAnimal;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Musk ox: herds on snowy ground. Threatened (a player walking up, a wolf or lion near, or one of them hurt) the herd
 * doesn't run: the grown oxen form a ring facing outward, heads down and horns out, with the calves in the middle. The
 * ox nearest the threat breaks out in a short charge when it comes too close, then backs into its place. An ox that is
 * badly hurt (bleeding) breaks from the ring and runs. Its fine underwool, Qiviut, comes off the carcass first.
 */
public class MuskOx extends WildAnimal {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/giants/musk_ox.png");
    private static final Identifier CALF_TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/giants/musk_ox_calf.png");
    /** How close a threat gets before the herd closes ranks (sneaking: closer), how long the alarm lasts, the herd's reach. */
    private static final double NOTICE = 12, NOTICE_SNEAKING = 6, PREDATOR_NOTICE = 18, HERD = 20, GIVE_UP = 28;
    private static final int ALARM_TICKS = 240;
    private LivingEntity threat;
    private long alarmUntil;

    public MuskOx(EntityType<? extends MuskOx> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 36.0).add(Attributes.MOVEMENT_SPEED, 0.23)
                .add(Attributes.ATTACK_DAMAGE, 6.0).add(Attributes.ATTACK_KNOCKBACK, 1.6).add(Attributes.KNOCKBACK_RESISTANCE, 0.7)
                .add(Attributes.ARMOR, 3.0).add(Attributes.STEP_HEIGHT, 1.0);
    }

    /** In the defensive ring (server side; the client sees it as the lowered head of {@link #isCharging}). */
    public boolean isAlarmed() {
        return threat != null && threat.isAlive() && level().getGameTime() < alarmUntil && !Blood.isBleeding(this);
    }

    LivingEntity threat() {
        return threat;
    }

    /** The whole herd nearby closes ranks against {@code from}. */
    void alarmHerd(LivingEntity from) {
        if (from instanceof MuskOx || from instanceof Player p && (p.isCreative() || p.isSpectator())) return;
        long until = level().getGameTime() + ALARM_TICKS;
        boolean fresh = !isAlarmed();
        for (MuskOx ox : level().getEntitiesOfClass(MuskOx.class, getBoundingBox().inflate(HERD))) {
            ox.threat = from;
            ox.alarmUntil = until;
            ox.setTarget(from); // keeps AnimalFear from sending it running while it stands its ground
        }
        if (fresh) playSound(SoundEvents.HOGLIN_ANGRY, 1.5F, 0.55F);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new RingGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide() || (tickCount + getId()) % 10 != 0) return;
        if (isAlarmed()) {
            if (distanceTo(threat) > GIVE_UP) calmDown();
            else if (!(threat instanceof Player) || distanceTo(threat) < NOTICE) alarmUntil = Math.max(alarmUntil, level().getGameTime() + 60);
            return;
        }
        if (threat != null) calmDown();
        if (isDozing()) return;
        Player p = level().getNearestPlayer(this, NOTICE);
        if (p != null && !p.isCreative() && !p.isSpectator() && distanceTo(p) < (p.isShiftKeyDown() ? NOTICE_SNEAKING : NOTICE) && hasLineOfSight(p)) {
            alarmHerd(p);
            return;
        }
        Predator hunter = level().getEntitiesOfClass(Predator.class, getBoundingBox().inflate(PREDATOR_NOTICE), LivingEntity::isAlive)
                .stream().findFirst().orElse(null);
        if (hunter != null) alarmHerd(hunter);
    }

    private void calmDown() {
        threat = null;
        alarmUntil = 0;
        if (!Blood.isBleeding(this)) setTarget(null);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && source.getEntity() instanceof LivingEntity attacker && !Blood.isBleeding(this)) alarmHerd(attacker);
        return hurt;
    }

    @Override
    protected void onWoken(Player near) {
        alarmHerd(near);
    }

    @Override
    public Identifier texture() {
        return isBaby() ? CALF_TEXTURE : TEXTURE;
    }

    @Override
    public boolean showsHorns() {
        return !isBaby();
    }

    /** It doesn't flee from players it notices: it closes ranks instead (see aiStep). */
    @Override
    public double fearMultiplier() {
        return 0.0;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.COW_SOUNDS.get(net.minecraft.world.entity.animal.cow.CowSoundVariants.SoundSet.MOODY).ambientSound().value();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.HOGLIN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.COW_SOUNDS.get(net.minecraft.world.entity.animal.cow.CowSoundVariants.SoundSet.MOODY).deathSound().value();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.COW_SOUNDS.get(net.minecraft.world.entity.animal.cow.CowSoundVariants.SoundSet.MOODY).stepSound().value(), 0.2F, 0.9F);
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * (isBaby() ? 1.2F : 0.7F);
    }

    /**
     * The ring: around the calves (or the herd's middle when there are none) the grown oxen take evenly spaced places,
     * the first one facing the threat, and stand facing outward; calves crowd into the middle. The ox nearest the
     * threat charges it when it comes within a few blocks of the ring, then returns.
     */
    static class RingGoal extends Goal {
        private static final int CHARGE_TICKS = 24, CHARGE_COOLDOWN = 50;
        private final MuskOx ox;
        private Vec3 spot;
        private int charge, cooldown;

        RingGoal(MuskOx ox) {
            this.ox = ox;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return ox.isAlarmed();
        }

        @Override
        public boolean canContinueToUse() {
            return ox.isAlarmed();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            spot = null;
            charge = 0;
        }

        @Override
        public void stop() {
            ox.getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity threat = ox.threat();
            if (threat == null) return;
            if (cooldown > 0) cooldown--;
            if (charge > 0) {
                charge--;
                ox.getNavigation().moveTo(threat, 1.9);
                if (ox.distanceToSqr(threat) < Mth.square(ox.getBbWidth() * 0.5 + threat.getBbWidth() * 0.5 + 0.9)
                        && ox.level() instanceof ServerLevel level) {
                    ox.doHurtTarget(level, threat);
                    ox.playSound(SoundEvents.GOAT_RAM_IMPACT, 1.2F, 0.7F);
                    threat.knockback(1.2, ox.getX() - threat.getX(), ox.getZ() - threat.getZ(), ox.damageSources().mobAttack(ox), 6.0F);
                    charge = 0;
                    cooldown = CHARGE_COOLDOWN;
                }
                if (charge == 0) spot = null;
                return;
            }
            if ((ox.tickCount + ox.getId()) % 5 == 0 || spot == null) spot = place(threat);
            if (spot != null && ox.position().distanceToSqr(spot.x, ox.getY(), spot.z) > 0.6) {
                ox.getNavigation().moveTo(spot.x, spot.y, spot.z, 1.4);
            } else {
                ox.getNavigation().stop();
            }
            ox.getLookControl().setLookAt(threat, 30, 30);
            // facing outward in place: turn the body towards the threat when standing
            if (ox.getNavigation().isDone()) {
                double dx = threat.getX() - ox.getX(), dz = threat.getZ() - ox.getZ();
                float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90;
                ox.setYRot(Mth.rotLerp(0.2F, ox.getYRot(), yaw));
                ox.yBodyRot = ox.getYRot();
            }
            if (!ox.isBaby() && cooldown == 0 && spot != null && threat.position().distanceTo(spot) < 4.0 && nearestToThreat(threat)) {
                charge = CHARGE_TICKS;
                ox.playSound(SoundEvents.HOGLIN_ANGRY, 1.2F, 0.6F);
            }
        }

        private List<MuskOx> herd() {
            return ox.level().getEntitiesOfClass(MuskOx.class, ox.getBoundingBox().inflate(HERD), o -> o.isAlarmed() && o.threat() == ox.threat());
        }

        private boolean nearestToThreat(LivingEntity threat) {
            return herd().stream().filter(o -> !o.isBaby()).min(Comparator.comparingDouble(o -> o.distanceToSqr(threat))).orElse(ox) == ox;
        }

        /** Where this ox stands in the ring right now. */
        private Vec3 place(LivingEntity threat) {
            List<MuskOx> herd = herd();
            List<MuskOx> calves = new ArrayList<>(), adults = new ArrayList<>();
            for (MuskOx o : herd) (o.isBaby() ? calves : adults).add(o);
            Vec3 centre = Vec3.ZERO;
            for (MuskOx o : calves.isEmpty() ? herd : calves) centre = centre.add(o.position());
            centre = centre.scale(1.0 / Math.max(1, (calves.isEmpty() ? herd : calves).size()));
            if (ox.isBaby()) return centre;
            if (adults.size() < 2) {
                // alone: stand between the calves and the threat
                Vec3 to = threat.position().subtract(centre).multiply(1, 0, 1);
                return to.lengthSqr() < 1.0E-3 ? ox.position() : centre.add(to.normalize().scale(1.8));
            }
            double radius = Math.max(1.8, adults.size() * 0.4 + 0.9);
            double base = Math.atan2(threat.getZ() - centre.z, threat.getX() - centre.x);
            // keep each ox's place stable: sort by the angle each one already has around the centre, from the threat's side
            final Vec3 c = centre;
            adults.sort(Comparator.comparingDouble(o -> Mth.positiveModulo(Math.atan2(o.getZ() - c.z, o.getX() - c.x) - base + Math.PI / adults.size(), Math.PI * 2)));
            int slot = adults.indexOf(ox);
            double angle = base + Math.PI * 2 * slot / adults.size();
            return centre.add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
        }
    }
}
