package com.mechanical.wildlife.jungle;

import java.util.EnumSet;

import com.mechanical.AnimalFear;
import com.mechanical.wildlife.WildAnimal;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Orangutan: a big shaggy red ape of the jungle, alone or a mother with her young. Slow and not shy, but it won't be
 * crowded: whoever comes within {@link #THROW_RANGE} blocks gets fruit and sticks thrown at him (no harm, a knock). And it
 * is a thief: anything left lying on the ground within {@link #STEAL_RANGE} blocks it walks over and takes, and carries off
 * in its hand. Hit it and it drops what it holds; killed, the loot falls out too. Gives Orangutan Hair (spun into String)
 * and meat.
 */
public class Orangutan extends WildAnimal {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/orangutan.png");
    static final double THROW_RANGE = 7, STEAL_RANGE = 10;
    private static final int THROW_COOLDOWN = 50;
    private int throwCooldown;

    public Orangutan(EntityType<? extends Orangutan> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.MOVEMENT_SPEED, 0.22).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(2, new StealGoal());
    }

    @Override
    public Identifier texture() {
        return TEXTURE;
    }

    @Override
    public double fearMultiplier() {
        return 0.4;
    }

    /** What it carries off (shown in its hand). */
    public ItemStack loot() {
        return getItemBySlot(EquipmentSlot.MAINHAND);
    }

    /** Takes {@code item} off the ground into its hand, if the hand is free. */
    public boolean take(ItemEntity item) {
        if (!loot().isEmpty() || !item.isAlive() || item.getItem().isEmpty()) return false;
        setItemSlot(EquipmentSlot.MAINHAND, item.getItem().copy());
        setGuaranteedDrop(EquipmentSlot.MAINHAND);
        take(item, item.getItem().getCount());
        item.discard();
        playSound(SoundEvents.ITEM_PICKUP, 0.6F, 0.8F);
        return true;
    }

    /** Throws a fruit or a stick at {@code target}. */
    public void pelt(ServerLevel level, LivingEntity target) {
        ItemStack missile = new ItemStack(random.nextInt(3) == 0 ? Items.STICK : random.nextBoolean() ? Items.SWEET_BERRIES : Items.APPLE);
        Snowball ball = new Snowball(level, this, missile);
        Vec3 d = target.getEyePosition().subtract(ball.position());
        ball.shoot(d.x, d.y + d.horizontalDistance() * 0.15, d.z, 1.1F, 6.0F);
        level.addFreshEntity(ball);
        level.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL, 0.6F, 0.7F);
        throwCooldown = THROW_COOLDOWN + random.nextInt(30);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel level) || isNoAi() || isDozing()) return;
        if (throwCooldown > 0) {
            throwCooldown--;
            return;
        }
        if (AnimalFear.isPanicking(this)) return;
        Player near = level.getNearestPlayer(this, THROW_RANGE);
        if (near != null && !near.isCreative() && !near.isSpectator() && hasLineOfSight(near)) {
            getLookControl().setLookAt(near);
            pelt(level, near);
        }
    }

    /** Hit: drops what it took (a thief caught). */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (!loot().isEmpty() && source.getEntity() != null) dropLoot(level);
        return super.hurtServer(level, source, amount);
    }

    @Override
    public void die(DamageSource source) {
        if (level() instanceof ServerLevel level && !loot().isEmpty()) dropLoot(level); // the carcass keeps no equipment
        super.die(source);
    }

    private void dropLoot(ServerLevel level) {
        spawnAtLocation(level, loot().copy());
        setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PANDA_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PANDA_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PANDA_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.8F;
    }

    /** Walks over to something lying on the ground and takes it. */
    class StealGoal extends Goal {
        private ItemEntity item;

        StealGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!loot().isEmpty() || isDozing() || AnimalFear.isPanicking(Orangutan.this) || (tickCount + getId()) % 20 != 0) return false;
            item = level().getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(STEAL_RANGE, 3, STEAL_RANGE),
                    i -> i.isAlive() && !i.hasPickUpDelay() && i.onGround()).stream().min(java.util.Comparator.comparingDouble(Orangutan.this::distanceToSqr)).orElse(null);
            return item != null && getNavigation().moveTo(item, 1.1);
        }

        @Override
        public boolean canContinueToUse() {
            return item.isAlive() && loot().isEmpty() && !AnimalFear.isPanicking(Orangutan.this) && !getNavigation().isDone();
        }

        @Override
        public void tick() {
            getLookControl().setLookAt(item);
            if (distanceToSqr(item) < 2.0 * 2.0) take(item);
        }

        @Override
        public void stop() {
            if (item != null && item.isAlive() && distanceToSqr(item) < 2.0 * 2.0) take(item);
            item = null;
        }
    }
}
