package com.mechanical.wildlife;

import java.util.ArrayList;
import java.util.List;

import com.mechanical.BarkBasketMenu;
import com.mechanical.Carting;
import com.mechanical.LeatherSatchelItem;
import com.mechanical.equipment.PackMenu;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

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
 * Up to {@link #MAX_SATCHELS} Leather Satchels hang on its sides (right-click one on the horse: left side, then right;
 * shears take the last one off). Each adds its 3x3 as three columns of the horse's inventory: the satchel's contents
 * move into the horse when it is put on and back into the satchel when it comes off or the horse dies.
 */
public class TameHorse extends Horse {
    public static final float BAREBACK_SPEED = 0.8F, BAREBACK_JUMP = 0.7F;
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/wild_horse.png");
    private static final Identifier FOAL_TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/wild_horse_foal.png");
    public static final int MAX_SATCHELS = 2, SATCHEL_COLUMNS = 3;
    private static final EntityDataAccessor<Byte> DATA_SATCHELS = SynchedEntityData.defineId(TameHorse.class, EntityDataSerializers.BYTE);
    /** The satchels hanging on it (server), emptied: their contents live in the inventory meanwhile. */
    private final List<ItemStack> satchels = new ArrayList<>();
    /** Granted from code, see scripts/gen_advancements.py. */
    private static final Identifier SADDLEBAGS_ADVANCEMENT = Identifier.fromNamespaceAndPath("mechanical", "bronze_age/saddlebags");

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
    /**
     * Swims under a rider too: the AI's float goal doesn't run while the horse is ridden, so it would sink. It treads water
     * until the water reaches {@link #RIDDEN_FLOAT_DEPTH} of its height (the
     * depth its swimming pose shows), deep enough that the rider stays in the saddle and above water. Runs on the
     * controlling side (the rider's client) like the rest of the ridden movement.
     */
    @Override
    public void aiStep() {
        super.aiStep();
        if (isVehicle() && isInWater() && getFluidHeight(net.minecraft.tags.FluidTags.WATER) > RIDDEN_FLOAT_DEPTH * getBbHeight()) {
            jumpInLiquid(net.minecraft.tags.FluidTags.WATER);
        }
    }

    /** Share of its height a ridden horse floats at (the same as WildAnimalRenderer.LAND_DEPTH, kept here: no client class on the server). */
    static final float RIDDEN_FLOAT_DEPTH = 0.6F;

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
        if (caravanLed()) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (isTamed() && !isBaby() && !isVehicle() && !player.isSecondaryUseActive()) {
            if (held.getItem() instanceof LeatherSatchelItem) {
                if (satchelCount() >= MAX_SATCHELS) return InteractionResult.FAIL; // full: don't mount instead
                if (!level().isClientSide()) attachSatchel(held.copyWithCount(1));
                if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                    var advancement = sp.level().getServer().getAdvancements().get(SADDLEBAGS_ADVANCEMENT);
                    if (advancement != null) sp.getAdvancements().award(advancement, "done");
                }
                held.consume(1, player);
                playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value());
                return InteractionResult.SUCCESS;
            }
        }
        return super.mobInteract(player, hand);
    }

    /** Shears (anything with the shears-remove-armour ability) take the satchels off first, then vanilla's saddle and armour. */
    @Override
    protected boolean attemptToShearEquipment(Player player, InteractionHand hand, ItemStack heldItem) {
        if (satchelCount() == 0 || !isTamed()) return super.attemptToShearEquipment(player, hand, heldItem);
        heldItem.hurtAndBreak(1, player, hand.asEquipmentSlot());
        if (level() instanceof ServerLevel server) dropLastSatchel(server);
        playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value());
        return true;
    }

    // ---------------------------------------------------------------- satchels

    /** Its own screen ({@link SaddlebagMenu}): vanilla's has room for 5 columns, two satchels need 6. */
    @Override
    public void openCustomInventoryScreen(Player player) {
        if (!level().isClientSide() && (!isVehicle() || hasPassenger(player)) && isTamed()) SaddlebagMenu.open(player, this);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_SATCHELS, (byte) 0);
    }

    public int satchelCount() {
        return entityData.get(DATA_SATCHELS);
    }

    @Override
    public int getInventoryColumns() {
        return satchelCount() * SATCHEL_COLUMNS;
    }

    /** Vanilla copies slot by index, but the grid's width changes with the satchels: keep each item at its row and column. */
    @Override
    protected void createInventory() {
        SimpleContainer old = inventory;
        super.createInventory();
        int oldCols = old == null ? 0 : old.getContainerSize() / 3, cols = getInventoryColumns();
        if (oldCols == cols) return;
        inventory.clearContent();
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < Math.min(oldCols, cols); x++) inventory.setItem(x + y * cols, old.getItem(x + y * oldCols));
        }
    }

    /** Inventory index of slot i (0-8, row by row) of the satchel on that side (0 left, 1 right). */
    private int slot(int side, int i) {
        return side * SATCHEL_COLUMNS + i % 3 + i / 3 * getInventoryColumns();
    }

    private void attachSatchel(ItemStack satchel) {
        int side = satchels.size();
        entityData.set(DATA_SATCHELS, (byte) (side + 1));
        createInventory();
        NonNullList<ItemStack> items = NonNullList.withSize(PackMenu.SATCHEL_SIZE, ItemStack.EMPTY);
        satchel.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
        for (int i = 0; i < items.size(); i++) inventory.setItem(slot(side, i), items.get(i));
        satchel.remove(DataComponents.CONTAINER);
        satchels.add(satchel);
    }

    /** Takes the last satchel off with its slots packed back in; what a satchel may not hold drops loose beside it. */
    private void dropLastSatchel(ServerLevel level) {
        int side = satchels.size() - 1;
        List<ItemStack> drops = new ArrayList<>();
        NonNullList<ItemStack> items = NonNullList.withSize(PackMenu.SATCHEL_SIZE, ItemStack.EMPTY);
        for (int i = 0; i < items.size(); i++) {
            ItemStack s = inventory.removeItemNoUpdate(slot(side, i));
            if (BarkBasketMenu.SATCHEL.test(s)) items.set(i, s);
            else if (!s.isEmpty()) drops.add(s);
        }
        ItemStack satchel = satchels.remove(side);
        satchel.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
        drops.add(satchel);
        entityData.set(DATA_SATCHELS, (byte) side);
        createInventory();
        for (ItemStack s : drops) spawnAtLocation(level, s);
    }

    /** The satchels drop packed instead of spilling (vanilla then drops whatever is left in the inventory). */
    @Override
    protected void dropEquipment(ServerLevel level) {
        while (!satchels.isEmpty()) dropLastSatchel(level);
        super.dropEquipment(level);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        ValueOutput.TypedOutputList<ItemStack> bags = output.list("Satchels", ItemStack.CODEC);
        satchels.forEach(bags::add);
        ValueOutput.TypedOutputList<ItemStackWithSlot> items = output.list("SatchelItems", ItemStackWithSlot.CODEC);
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (!inventory.getItem(i).isEmpty()) items.add(new ItemStackWithSlot(i, inventory.getItem(i)));
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        satchels.clear();
        for (ItemStack s : input.listOrEmpty("Satchels", ItemStack.CODEC)) {
            if (satchels.size() < MAX_SATCHELS) satchels.add(s);
        }
        entityData.set(DATA_SATCHELS, (byte) satchels.size());
        inventory = null; // a fresh grid, not one remapped from whatever was there
        createInventory();
        for (ItemStackWithSlot item : input.listOrEmpty("SatchelItems", ItemStackWithSlot.CODEC)) {
            if (item.isValidInContainer(inventory.getContainerSize())) inventory.setItem(item.slot(), item.stack());
        }
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
