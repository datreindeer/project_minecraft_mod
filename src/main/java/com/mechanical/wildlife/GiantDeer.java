package com.mechanical.wildlife;

import net.minecraft.core.particles.ParticleTypes;
import java.util.EnumSet;

import com.mechanical.AnimalFear;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Giant deer (Megaloceros): rare, solitary mini-boss with huge palmate antlers. Attacks players who come near with
 * heavy antler charges and doesn't bleed or back off until it is down to a quarter of its health; then it bleeds and
 * runs (AnimalFear takes over once {@link com.mechanical.Blood#isBleeding} says so). Always yields a giant antler.
 */
public class GiantDeer extends WildAnimal {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/giant_deer.png");
    private static final float BREAK_POINT = 0.25F;
    private final ServerBossEvent bossEvent = new ServerBossEvent(net.minecraft.util.Mth.createInsecureUUID(random), getDisplayName(), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);

    public GiantDeer(EntityType<? extends GiantDeer> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 120.0).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 10.0).add(Attributes.ATTACK_KNOCKBACK, 2.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8).add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.STEP_HEIGHT, 1.5);
    }

    /** Down to a quarter of its health: it gives up and runs. */
    public boolean isBroken() {
        return getHealth() <= getMaxHealth() * BREAK_POINT;
    }

    @Override
    public float bleedThreshold() {
        return BREAK_POINT;
    }

    @Override
    public boolean bloodOnlyWhenBleeding() {
        return true;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.removeAllGoals(g -> g instanceof FollowHerdGoal); // always alone
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.8, true) {
            @Override
            public boolean canUse() {
                return !isBroken() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !isBroken() && super.canContinueToUse();
            }
        });
        targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                return !isBroken() && super.canUse();
            }
        });
        targetSelector.addGoal(2, new AttackNearbyPlayerGoal(this));
    }

    private boolean wasAggressive;

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide()) {
            magicMotes();
            return;
        }
        // a deep horn call across the forest when it turns on you
        if (isAggressive() && !wasAggressive) playSound(SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(6).value(), 5.0F, 0.6F);
        wasAggressive = isAggressive();
        flowerTrail();
        if (isAggressive() && !isBroken() && horizontalCollision && tickCount % 4 == 0 && level() instanceof ServerLevel server
                && net.neoforged.neoforge.event.EventHooks.canEntityGrief(server, this)) ramTree(server);
        if (!isBroken()) return;
        // beaten: drops the fight and runs from whoever is near, for as long as it stays this weak
        if (getTarget() != null) {
            AnimalFear.panic(this, getTarget().position());
            setTarget(null);
        } else if (tickCount % 20 == 0 && !AnimalFear.isPanicking(this)) {
            Player near = level().getNearestPlayer(this, 24);
            if (near != null && !near.isCreative() && !near.isSpectator()) AnimalFear.panic(this, near.position());
        }
    }

    /** Charging into a tree trunk: the whole tree comes down, falling the way the deer is running. */
    private void ramTree(ServerLevel level) {
        net.minecraft.world.phys.AABB ahead = getBoundingBox().move(getDeltaMovement().x, 0, getDeltaMovement().z).inflate(0.5, 0, 0.5).setMaxY(getY() + 3);
        for (net.minecraft.core.BlockPos pos : net.minecraft.core.BlockPos.betweenClosed(ahead)) {
            var state = level.getBlockState(pos);
            // the Ancient Oak's enchanted wood is wither immune and must never fall
            if (!state.is(net.minecraft.tags.BlockTags.LOGS) || state.is(net.minecraft.tags.BlockTags.WITHER_IMMUNE)) continue;
            net.minecraft.core.BlockPos cut = pos.immutable();
            while (level.getBlockState(cut.below()).is(net.minecraft.tags.BlockTags.LOGS)) cut = cut.below(); // fell from the base
            var tree = com.mechanical.TreeFelling.find(level, cut);
            if (tree == null) continue; // a log wall or a cabin, not a natural tree
            com.mechanical.TreeFelling.fell(level, cut, tree, getDirection(), this, net.minecraft.world.item.ItemStack.EMPTY);
            playSound(SoundEvents.GOAT_RAM_IMPACT, 2.0F, 0.5F);
            level.playSound(null, cut, SoundEvents.WOOD_BREAK, net.minecraft.sounds.SoundSource.HOSTILE, 2.0F, 0.5F);
            return;
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        bossEvent.setProgress(getHealth() / getMaxHealth());
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    /** Woken by a player: it goes for them instead of bolting. */
    @Override
    protected void onWoken(Player near) {
        if (!isBroken()) setTarget(near);
        else super.onWoken(near);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit) playSound(SoundEvents.GOAT_RAM_IMPACT, 1.2F, 0.6F);
        return hit;
    }

    /** Charges any player that comes within range and sight (much closer when sneaking). */
    static class AttackNearbyPlayerGoal extends TargetGoal {
        private static final double RANGE = 14, RANGE_SNEAKING = 6;
        private final GiantDeer deer;

        AttackNearbyPlayerGoal(GiantDeer deer) {
            super(deer, false);
            this.deer = deer;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (deer.isBroken() || deer.isDozing() || deer.getTarget() != null || deer.getRandom().nextInt(10) != 0) return false;
            Player p = deer.level().getNearestPlayer(deer, RANGE);
            if (p == null || p.isCreative() || p.isSpectator() || !deer.hasLineOfSight(p)) return false;
            if (deer.distanceTo(p) > (p.isShiftKeyDown() ? RANGE_SNEAKING : RANGE)) return false;
            targetMob = p;
            return true;
        }

        @Override
        public void start() {
            deer.setTarget(targetMob);
            deer.playSound(SoundEvents.RAVAGER_ROAR, 1.0F, 1.3F);
            super.start();
        }

        @Override
        public boolean canContinueToUse() {
            return !deer.isBroken() && super.canContinueToUse();
        }
    }

    @Override
    public Identifier texture() {
        return TEXTURE;
    }

    @Override
    public boolean showsHorns() {
        return true;
    }

    @Override
    protected float babyChance() {
        return 0.0F;
    }

    @Override
    public double fearMultiplier() {
        return isBroken() ? 2.0 : 0.0;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.GOAT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GOAT_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.GOAT_STEP, 0.3F, 0.6F);
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.5F;
    }

    /** Drifting spores and glints around its antlers; a swirl of them while it charges. */
    private void magicMotes() {
        var r = getRandom();
        int n = isAggressive() ? 3 : (r.nextInt(3) == 0 ? 1 : 0);
        for (int i = 0; i < n; i++) {
            double a = r.nextDouble() * Math.PI * 2, d = 0.8 + r.nextDouble() * 2.2;
            double x = getX() + Math.cos(a) * d, y = getY() + getBbHeight() * (0.75 + r.nextDouble() * 0.35), z = getZ() + Math.sin(a) * d;
            level().addParticle(r.nextInt(5) == 0 ? ParticleTypes.END_ROD : ParticleTypes.SPORE_BLOSSOM_AIR, x, y, z,
                    0, r.nextDouble() * 0.02, 0);
        }
        if (r.nextInt(12) == 0) level().addParticle(ParticleTypes.HAPPY_VILLAGER, getX() + (r.nextDouble() - 0.5) * 2.5,
                getY() + 0.2, getZ() + (r.nextDouble() - 0.5) * 2.5, 0, 0, 0);
    }

    private static final net.minecraft.world.level.block.Block[] TRAIL_FLOWERS = {
            net.minecraft.world.level.block.Blocks.OXEYE_DAISY, net.minecraft.world.level.block.Blocks.AZURE_BLUET,
            net.minecraft.world.level.block.Blocks.CORNFLOWER, net.minecraft.world.level.block.Blocks.LILY_OF_THE_VALLEY,
            net.minecraft.world.level.block.Blocks.FERN};

    /** Flowers spring up now and then where it walks. */
    private void flowerTrail() {
        if (!onGround() || getDeltaMovement().horizontalDistanceSqr() < 1.0E-3 || getRandom().nextInt(25) != 0) return;
        net.minecraft.core.BlockPos at = blockPosition().offset(getRandom().nextInt(3) - 1, 0, getRandom().nextInt(3) - 1);
        var flower = TRAIL_FLOWERS[getRandom().nextInt(TRAIL_FLOWERS.length)].defaultBlockState();
        if (level().getBlockState(at).isAir() && level().getBlockState(at.below()).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)
                && flower.canSurvive(level(), at)) {
            level().setBlock(at, flower, net.minecraft.world.level.block.Block.UPDATE_ALL);
        }
    }

    @Override
    public void die(net.minecraft.world.damagesource.DamageSource source) {
        super.die(source);
        if (level() instanceof net.minecraft.server.level.ServerLevel server) {
            server.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, getX(), getY() + getBbHeight() * 0.6, getZ(), 80, 1.2, 1.4, 1.2, 0.35);
            server.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, getX(), getY() + getBbHeight() * 0.5, getZ(), 60, 2.0, 1.5, 2.0, 0.02);
            playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 2.0F, 0.5F);
        }
    }
}
