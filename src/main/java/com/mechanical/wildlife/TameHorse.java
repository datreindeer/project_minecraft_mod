package com.mechanical.wildlife;

import com.mechanical.Carting;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A tamed {@link WildHorse}: a vanilla horse underneath (riding, jumping, inventory, horse armour, breeding) that keeps
 * the wild horse's looks (client/wildlife/TameHorseRenderer). Ridden bareback it is steered without a saddle but goes at
 * {@link #BAREBACK_SPEED} and jumps {@link #BAREBACK_JUMP} as high; the Hide Saddle Blanket (saddle slot) gives the full
 * speed and jump. Bred with sheaves or hay bales ({@link Carting#HORSE_BREEDING_FEED}); foals of two tame horses are tame.
 */
public class TameHorse extends Horse {
    public static final float BAREBACK_SPEED = 0.8F, BAREBACK_JUMP = 0.7F;
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/wild_horse.png");
    private static final Identifier FOAL_TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/wild_horse_foal.png");

    public TameHorse(EntityType<? extends TameHorse> type, Level level) {
        super(type, level);
    }

    /** Defaults (mid-range of {@link #randomizeAttributes}) for horses that skip it, e.g. summoned with data. */
    public static AttributeSupplier.Builder createAttributes() {
        return createBaseHorseAttributes().add(Attributes.MAX_HEALTH, 23.0).add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.JUMP_STRENGTH, 0.62);
    }

    public Identifier texture() {
        return isBaby() ? FOAL_TEXTURE : TEXTURE;
    }

    /** Hardy and quick, a little smaller than a vanilla horse: 20-26 health, speed 0.2-0.3, jump 0.5-0.75. */
    @Override
    public void randomizeAttributes(RandomSource random) {
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(20 + random.nextInt(7));
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.2 + random.nextDouble() * 0.1);
        getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(0.5 + random.nextDouble() * 0.25);
    }

    /** Steered bareback: no saddle needed. */
    @Override
    public LivingEntity getControllingPassenger() {
        return isTamed() && getFirstPassenger() instanceof Player p ? p : super.getControllingPassenger();
    }

    @Override
    protected float getRiddenSpeed(Player controller) {
        return super.getRiddenSpeed(controller) * (isSaddled() ? 1.0F : BAREBACK_SPEED);
    }

    @Override
    public boolean canJump() {
        return isTamed();
    }

    @Override
    public void onPlayerJump(int jumpAmount) {
        if (isSaddled()) {
            super.onPlayerJump(jumpAmount);
            return;
        }
        if (jumpAmount >= 0) {
            allowStandSliding = true;
            standIfPossible();
        }
        playerJumpPendingScale = (jumpAmount >= 90 ? 1.0F : 0.4F + 0.4F * Math.max(0, jumpAmount) / 90.0F) * BAREBACK_JUMP;
    }

    /** Led by a living Trader Caravan member: the lead never breaks or comes off, and players can't take the horse. */
    public boolean caravanLed() {
        return getLeashHolder() instanceof com.mechanical.people.Tribesperson t && t.isAlive() && t.role().caravan();
    }

    /** Too far from its caravan (stuck, pushed, chased off): it catches up behind the leader instead of snapping the lead. */
    @Override
    public void leashTooFarBehaviour() {
        Entity holder = getLeashHolder();
        if (!caravanLed() || holder == null) {
            super.leashTooFarBehaviour();
            return;
        }
        Vec3 back = holder.position().add(Vec3.directionFromRotation(0, holder.getYRot()).scale(-3));
        ejectPassengers();
        teleportTo(back.x, holder.getY(), back.z);
    }

    @Override
    public void dropLeash() {
        if (!caravanLed()) super.dropLeash();
    }

    @Override
    public void removeLeash() {
        if (!caravanLed()) super.removeLeash();
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        return caravanLed() ? InteractionResult.PASS : super.mobInteract(player, hand);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Carting.HORSE_FEED) || super.isFood(stack);
    }

    /** Vanilla's feeding, plus mod feed: heals a little, and sheaves or hay put a tame adult in love. */
    @Override
    protected boolean handleEating(Player player, ItemStack stack) {
        boolean vanilla = super.handleEating(player, stack);
        boolean used = vanilla;
        if (stack.is(Carting.HORSE_BREEDING_FEED) && !level().isClientSide() && isTamed() && getAge() == 0 && !isInLove()) {
            setInLove(player);
            used = true;
        }
        if (!vanilla && stack.is(Carting.HORSE_FEED) && getHealth() < getMaxHealth()) {
            heal(2.0F);
            used = true;
        }
        if (used && !vanilla) {
            playSound(getEatingSound());
            gameEvent(GameEvent.EAT);
        }
        return used;
    }

    /** Two tame horses foal a tame one (owned by whoever fed this one); with a vanilla horse or donkey, vanilla's rules. */
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        if (!(partner instanceof TameHorse)) return super.getBreedOffspring(level, partner);
        TameHorse foal = Wildlife.TAME_HORSE.get().create(level, EntitySpawnReason.BREEDING);
        if (foal == null) return null;
        setOffspringAttributes(partner, foal);
        foal.setTamed(true);
        if (getLoveCause() != null) foal.setOwner(getLoveCause());
        return foal;
    }

}
