package com.mechanical.wildlife.warm;

import com.mechanical.Carting;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.Donkey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * A tamed {@link Onager}: a vanilla donkey underneath (riding, the pack inventory, breeding, pulling the Cart, which takes
 * any tamed horse) that keeps the onager's looks (client/wildlife/warm/TameOnagerRenderer). Ridden bareback (no saddle
 * needed) at {@link #BAREBACK_SPEED}. Its pack is a pair of Pack Baskets ({@link WarmFauna#PACK_BASKETS}) instead of a
 * chest: right-click it with them (they come off again when it dies). Bred with sheaves or hay; two tame onagers foal a
 * tame one.
 */
public class TameOnager extends Donkey {
    public static final float BAREBACK_SPEED = 0.85F;
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/onager.png");
    private static final Identifier FOAL_TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/onager_foal.png");

    public TameOnager(EntityType<? extends TameOnager> type, Level level) {
        super(type, level);
    }

    /** Defaults (mid-range of {@link #randomizeAttributes}) for ones that skip it, e.g. summoned with data. */
    public static AttributeSupplier.Builder createAttributes() {
        return createBaseChestedHorseAttributes().add(Attributes.MAX_HEALTH, 21.0).add(Attributes.MOVEMENT_SPEED, 0.24).add(Attributes.JUMP_STRENGTH, 0.55);
    }

    public Identifier texture() {
        return isBaby() ? FOAL_TEXTURE : TEXTURE;
    }

    /** Hardy and quick for an ass, slower than a horse when loaded: 18-24 health, speed 0.2-0.28, jump 0.45-0.65. */
    @Override
    public void randomizeAttributes(RandomSource random) {
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(18 + random.nextInt(7));
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.2 + random.nextDouble() * 0.08);
        getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(0.45 + random.nextDouble() * 0.2);
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
    public boolean isFood(ItemStack stack) {
        return stack.is(Carting.HORSE_FEED) || super.isFood(stack);
    }

    /** Pack Baskets go on like a donkey's chest; a plain chest doesn't (it has nothing to hang it by). */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.CHEST)) return InteractionResult.PASS;
        if (stack.is(WarmFauna.PACK_BASKETS.get()) && isTamed() && !isBaby() && !hasChest() && !isVehicle()) {
            if (!level().isClientSide()) {
                setChest(true);
                playChestEquipsSound();
                stack.consume(1, player);
                createInventory();
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    /** The baskets come off (not a chest); what was in them spills as usual. */
    @Override
    protected void dropEquipment(ServerLevel level) {
        if (hasChest()) {
            spawnAtLocation(level, WarmFauna.PACK_BASKETS.get());
            setChest(false);
        }
        super.dropEquipment(level);
    }

    /** Mod feed heals a little; sheaves or hay put a tame adult in love. */
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

    @Override
    public boolean canMate(Animal partner) {
        return partner != this && partner instanceof TameOnager other && canParent() && other.canParent();
    }

    /** Two tame onagers foal a tame one, owned by whoever fed this one. */
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        TameOnager foal = WarmFauna.TAME_ONAGER.get().create(level, EntitySpawnReason.BREEDING);
        if (foal == null) return null;
        setOffspringAttributes(partner, foal);
        foal.setTamed(true);
        if (getLoveCause() != null) foal.setOwner(getLoveCause());
        return foal;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 1.1F;
    }
}
