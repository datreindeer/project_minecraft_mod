package com.mechanical.wildlife;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

import com.mechanical.AnimalFear;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.wolf.WolfSoundVariant;
import net.minecraft.world.entity.animal.wolf.WolfSoundVariants;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Grey wolf: packs of 3-6 in taiga, forest, plains edges and snowy land. Sleeps by day and keeps away from people. At night
 * it hunts: the pack howls (heard ~100 blocks off), tracks a player's scent without needing to see him, follows at a
 * distance, spreads out round him (behind and to the sides) and darts in to bite, one after another, falling back to the
 * ring between bites. A wounded or lone player in the dark is attacked sooner. Breaks off when badly hurt or when the pack
 * is down to half. Afraid of fire (see {@link Predator}). Also hunts deer, sheep, hares and tribespeople.
 */
public class GreyWolf extends Predator {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/grey_wolf.png");
    /** Scent range for a player: normal, wounded (below half health), sneaking. No line of sight needed. */
    private static final double SCENT = 32, SCENT_WOUNDED = 48, SCENT_SNEAKING = 14, PREY_RANGE = 24, PACK_RANGE = 32;
    /** Ring the pack forms round the quarry, and how far it holds off a torch or a fire. */
    private static final double RING = 5.5, RING_TORCH = 6.5;
    /** Breaks off below this share of health, for this long. */
    private static final float RETREAT_HEALTH = 0.35F;
    private static final int RETREAT_TICKS = 600, HOWL_TICKS = 50;
    /** Flank angles (degrees from straight behind the quarry) handed out by pack rank. */
    private static final float[] FLANKS = {0, 55, -55, 110, -110, 180};

    private int maxPackSeen = 1, howlDelay = -1;

    public GreyWolf(EntityType<? extends GreyWolf> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 14.0).add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 4.0).add(Attributes.FOLLOW_RANGE, 64.0).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(2, new PackHuntGoal());
        goalSelector.addGoal(4, new HowlGoal());
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new ScentTargetGoal());
    }

    @Override
    protected boolean isPrey(LivingEntity e) {
        return e instanceof RoeDeer || e instanceof RedDeer && !((RedDeer) e).showsHorns() || e instanceof Reindeer
                || e instanceof Hare || e instanceof Mouflon || e instanceof Sheep || e instanceof com.mechanical.people.Tribesperson;
    }

    @Override
    protected float babyChance() {
        return 0.0F;
    }

    @Override
    public Identifier texture() {
        return TEXTURE;
    }

    /** The pack: grey wolves within 32 blocks, this one included, in a fixed order (by entity id) so flanks stay put. */
    List<GreyWolf> pack() {
        return level().getEntitiesOfClass(GreyWolf.class, getBoundingBox().inflate(PACK_RANGE), w -> w.isAlive())
                .stream().sorted(Comparator.comparingInt(GreyWolf::getId)).toList();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide()) return;
        if (howlDelay > 0 && --howlDelay == 0) howl(false);
        if (tickCount % 20 != 0) return;
        int n = pack().size();
        maxPackSeen = Math.max(maxPackSeen, n);
        LivingEntity target = getTarget();
        if (target != null && !retreating() && (getHealth() < getMaxHealth() * RETREAT_HEALTH || maxPackSeen >= 2 && n * 2 <= maxPackSeen)) {
            retreat(target.position(), RETREAT_TICKS);
            maxPackSeen = n; // regrouped, the rest may try again another night
        }
    }

    /** Howls; the leader's howl sets the others off one after another (a chorus), each a little higher or lower. */
    void howl(boolean chorus) {
        startCalling(HOWL_TICKS);
        level().playSound(null, getX(), getEyeY(), getZ(), Predators.WOLF_HOWL, SoundSource.HOSTILE, 1.0F, 0.85F + random.nextFloat() * 0.3F);
        if (chorus) {
            for (GreyWolf w : pack()) if (w != this && w.howlDelay <= 0) w.howlDelay = 15 + random.nextInt(40);
        }
    }

    /** Called by the night spawner: the pack announces itself a few seconds after arriving. */
    public void howlSoon() {
        howlDelay = 40 + random.nextInt(60);
    }

    private static WolfSoundVariant.WolfSoundSet sounds() {
        return SoundEvents.WOLF_SOUNDS.get(WolfSoundVariants.SoundSet.BIG).adultSounds();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return getTarget() != null ? sounds().growlSound().value() : isDozing() ? null : sounds().pantSound().value();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return sounds().hurtSound().value();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return sounds().deathSound().value();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.WOLF_STEP.value(), 0.15F, 1.0F);
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.9F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    /** Lone (no other player or tribesperson within 10) and standing in the dark. */
    private static boolean aloneInTheDark(LivingEntity t) {
        Level level = t.level();
        return level.getBrightness(LightLayer.BLOCK, t.blockPosition()) < 5
                && level.getEntitiesOfClass(LivingEntity.class, t.getBoundingBox().inflate(10),
                        e -> e != t && (e instanceof Player || e instanceof com.mechanical.people.Tribesperson)).isEmpty();
    }

    /**
     * Picks quarry at night: a player it can smell (no sight needed), else prey nearby. Shares a packmate's quarry. The first
     * wolf of the pack to pick up a scent howls and the others answer.
     */
    class ScentTargetGoal extends TargetGoal {
        ScentTargetGoal() {
            super(GreyWolf.this, false);
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (random.nextInt(10) != 0 || !huntingTime() || retreating() || isDozing() || getTarget() != null) return false;
            List<GreyWolf> pack = pack();
            for (GreyWolf w : pack) {
                if (w != GreyWolf.this && canHunt(w.getTarget()) && !w.retreating()) {
                    targetMob = w.getTarget();
                    return true;
                }
            }
            targetMob = smell();
            if (targetMob != null && pack.get(0) == GreyWolf.this) howl(true);
            return targetMob != null;
        }

        private LivingEntity smell() {
            Player best = null;
            double bestD = Double.MAX_VALUE;
            for (Player p : level().players()) {
                if (!canHunt(p)) continue;
                double range = p.isShiftKeyDown() ? SCENT_SNEAKING : p.getHealth() < p.getMaxHealth() / 2 ? SCENT_WOUNDED : SCENT;
                double d = distanceTo(p);
                if (d < range && d < bestD) { best = p; bestD = d; }
            }
            if (best != null) return best;
            return level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(PREY_RANGE), GreyWolf.this::canHunt)
                    .stream().min(Comparator.comparingDouble(GreyWolf.this::distanceToSqr)).orElse(null);
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity t = getTarget();
            if (!canHunt(t) || retreating()) return false;
            // daylight ends the hunt, unless it is a fight the quarry started
            if (!huntingTime() && getLastHurtByMob() != t) return false;
            return distanceTo(t) < getFollowDistance();
        }

        @Override
        public void start() {
            setTarget(targetMob);
            super.start();
        }
    }

    /**
     * Stalk, surround, dart in. Far off it trots after the quarry; within 20 blocks it takes up its flank on a ring round
     * the quarry and holds there, and bites when the moment is right: it is behind the quarry, or two packmates are in
     * place, or the quarry is wounded, or alone in the dark, or simply after a while. After a bite it falls back to the
     * ring. Holds off further from a torch, and lurks at the edge of a fire's light without attacking.
     */
    class PackHuntGoal extends Goal {
        private int surroundTicks, biteCooldown, repath;

        PackHuntGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return canHunt(getTarget()) && !retreating();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            surroundTicks = 0;
            repath = 0;
        }

        @Override
        public void stop() {
            setCrouching(false);
            getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity t = getTarget();
            if (t == null) return;
            getLookControl().setLookAt(t, 30.0F, 30.0F);
            if (biteCooldown > 0) biteCooldown--;
            if (t instanceof net.minecraft.world.entity.animal.Animal && distanceTo(t) < 16) AnimalFear.panic(t, position());
            double d = distanceTo(t);
            List<GreyWolf> pack = pack();
            boolean torch = torchKeepsOff(t);
            if (prowlRoundFire(t, 1.0)) { // eyes at the edge of the firelight
                setCrouching(false);
                repath = 0;
                return;
            }
            setCrouching(d < 20 && d > RING + 1);
            if (d > 20) { // on the trail
                if (--repath <= 0) { getNavigation().moveTo(t, 1.0); repath = 20; }
                return;
            }
            boolean wounded = t.getHealth() < t.getMaxHealth() / 2;
            boolean behind = angleFromBehind(t) < 70;
            long inPlace = pack.stream().filter(w -> w != GreyWolf.this && w.getTarget() == t && w.distanceTo(t) < RING + 2).count();
            boolean bold = wounded || getLastHurtByMob() == t || !(t instanceof Player) || aloneInTheDark(t);
            boolean engage = !torch && biteCooldown == 0
                    && (surroundTicks > (bold ? 20 : 70) && (behind || inPlace >= 2 || bold) || surroundTicks > 240);
            if (engage) {
                setCrouching(false);
                if (--repath <= 0) { getNavigation().moveTo(t, 1.55); repath = 5; }
                if (isWithinMeleeAttackRange(t) && hasLineOfSight(t)) {
                    doHurtTarget((ServerLevel) level(), t);
                    biteCooldown = 30 + random.nextInt(50);
                    surroundTicks = 0;
                    playSound(sounds().growlSound().value(), 1.0F, getVoicePitch());
                }
                return;
            }
            surroundTicks++;
            moveToFlank(t, pack, torch ? Math.max(RING_TORCH, TORCH_KEEP_OFF + 2) : RING, 1.3);
            if (torch && d < TORCH_KEEP_OFF) { // too close to the flame: jump back
                Vec3 away = position().subtract(t.position()).multiply(1, 0, 1).normalize();
                getNavigation().moveTo(getX() + away.x * 4, getY(), getZ() + away.z * 4, 1.6);
                repath = 10;
            }
        }

        /** Degrees between "straight behind the quarry" and where this wolf stands. */
        private float angleFromBehind(LivingEntity t) {
            float toMe = (float) (Mth.atan2(getZ() - t.getZ(), getX() - t.getX()) * Mth.RAD_TO_DEG) - 90.0F;
            return Math.abs(Mth.wrapDegrees(toMe - (t.getYHeadRot() + 180.0F)));
        }

        private void moveToFlank(LivingEntity t, List<GreyWolf> pack, double radius, double speed) {
            if (--repath > 0) return;
            repath = 10;
            int rank = Math.max(0, pack.indexOf(GreyWolf.this));
            float angle = (t.getYHeadRot() + 180.0F + FLANKS[rank % FLANKS.length]) * Mth.DEG_TO_RAD;
            double x = t.getX() - Mth.sin(angle) * radius, z = t.getZ() + Mth.cos(angle) * radius;
            getNavigation().moveTo(x, t.getY(), z, speed);
        }
    }

    /** Now and then at night, when not hunting, the pack's leader stops, throws its head back and howls; the others answer. */
    class HowlGoal extends Goal {
        private int ticks;

        HowlGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return huntingTime() && getTarget() == null && !isDozing() && onGround() && random.nextInt(2400) == 0 && pack().get(0) == GreyWolf.this;
        }

        @Override
        public void start() {
            ticks = HOWL_TICKS;
            getNavigation().stop();
            howl(true);
        }

        @Override
        public boolean canContinueToUse() {
            return --ticks > 0 && getTarget() == null;
        }
    }
}
