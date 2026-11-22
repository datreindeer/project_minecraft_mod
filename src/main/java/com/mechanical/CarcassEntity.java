package com.mechanical;

import net.minecraft.tags.FluidTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LinearInterpolationHandler;
import java.util.List;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;

/**
 * A dead animal lying on the ground. Hold right-click on it with a knife to butcher it ({@link KnifeItem}); the yield
 * comes from {@link Butchery}. Disappears after five minutes lying on the ground. Spawned where the body lies once the
 * death animation has finished.
 * <p>
 * Hauling ({@link CarcassHauling}): sneak + right-click with an empty hand carries a small or medium body on the
 * shoulders, or drags a large one behind you ({@link #hauler()}). It can go on a {@link TravoisEntity} (as a passenger)
 * or hang on a {@link HangingRackBlock} ({@link #hungAt()}), where butchering yields twice the meat in big cuts. While
 * hauled, on a travois or hung it does not vanish, but it spoils a day after the kill ({@link #spoiled()}).
 * <p>
 * A Trader Caravan's trophy (people.Caravans#trophy) is {@link #isPrepared() prepared} trade goods: while it lies on its
 * caravan's cart ({@link #isCaravanProperty()}) it can't be butchered or taken (trying angers the caravan as theft), and
 * it neither spoils nor vanishes. Once the cart's horse falls it is an ordinary carcass.
 */
public class CarcassEntity extends Entity implements IEntityWithComplexSpawn {
    public static final int HIDDEN_TICKS = 0, LIFETIME = 6000;
    /**
     * Butchering work per pass, in knife-speed ticks (flint knife: 1 per tick; every player cutting at once adds his own):
     * {@code WORK_PER_SIZE * WORK_PER_PASS * max(1, bulk)^WORK_EXPONENT}, bulk = hitbox width² × height. A deer or anything
     * smaller ~2 s with flint, a bear ~3.5 s, a moose 6 s, a mammoth 17 s (4 hunters with bronze knives: 2.5 s).
     */
    private static final int WORK_PER_SIZE = 60;
    private static final double WORK_EXPONENT = 0.75;
    /** Players who can drag one large body together, each holding his own limb ({@link #gripPoint}). */
    public static final int MAX_HAULERS = 4;

    /** 0: whole (wool/hide/feathers still on), 1: skinned/sheared/plucked, only the meat left. Synced for the renderer. */
    private static final EntityDataAccessor<Integer> STAGE = SynchedEntityData.defineId(CarcassEntity.class, EntityDataSerializers.INT);
    /**
     * Entity ids of the players carrying or dragging it, -1 for a free hold; slot = the limb held (0/1 the forelegs, 2/3
     * the hind legs). Only a large body takes more than slot 0. Synced for the renderers, the sprint check and hauled().
     */
    private static final List<EntityDataAccessor<Integer>> HAULERS = java.util.stream.IntStream.range(0, MAX_HAULERS)
            .mapToObj(i -> SynchedEntityData.defineId(CarcassEntity.class, EntityDataSerializers.INT)).toList();
    /** The Hanging Rack it hangs on. Synced for the renderer and the hitbox. */
    private static final EntityDataAccessor<java.util.Optional<BlockPos>> HUNG_AT = SynchedEntityData.defineId(CarcassEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    /** Ticks after the kill when a carcass that is hauled, on a travois or hung has spoiled (one day). */
    public static final int SPOILS = Calendar.DAY;
    /** On a Hanging Rack (cool, airy, off the ground) a body keeps three days from the kill. */
    public static final int HUNG_SPOILS = 3 * Calendar.DAY;
    /** From this share of its time it starts to turn: greenish and flies (a warning before it spoils). */
    private static final float TURNING = 0.75F;
    /** Hung this long (3 minutes) it has aged: a little more meat on the rack. */
    public static final int AGED = 3600;
    /** A skinned carcass on the rack may stain the ground under it this often (1 in 2): about 9 times before it has aged. */
    private static final int RACK_BLOOD_INTERVAL = 200;
    /** Has hung {@link #AGED} ticks: synced so it looks dried (CarcassRenderer), stops dripping and draws a fly or two. */
    private static final EntityDataAccessor<Boolean> AGED_LOOK = SynchedEntityData.defineId(CarcassEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> TURNING_LOOK = SynchedEntityData.defineId(CarcassEntity.class, EntityDataSerializers.BOOLEAN);
    /** Dressed and lashed as a caravan's trade goods. Synced for the renderer and the property check on both sides. */
    private static final EntityDataAccessor<Boolean> PREPARED = SynchedEntityData.defineId(CarcassEntity.class, EntityDataSerializers.BOOLEAN);
    /** Hanging on a horse's side (HorseWork, living-world-plan D4): the horse's entity id * 2 + side (0 left, 1 right), -1 if not. Synced (renderer). */
    private static final EntityDataAccessor<Integer> SLUNG_ON = SynchedEntityData.defineId(CarcassEntity.class, EntityDataSerializers.INT);
    /** Server: the horse it hangs on (saved, found again after a reload), its side, and ticks the horse has been missing. */
    private java.util.UUID slungHorse;
    private int slungSide, slungMissing;
    /** Out from the horse's middle and up from its feet, where a body hangs on its side (blocks). */
    public static final double SIDE_OUT = 0.75, SIDE_UP = 0.55;
    /** Colour of the flies around an aged carcass. */
    private static final int FLY = 0x1A1712;
    /** Each of the two passes takes this share of the work for the body size. */
    private static final float WORK_PER_PASS = 0.6F;

    private EntityType<?> animal = EntityTypes.PIG;
    /** Ticks since the kill, ticks lying free on the ground, ticks hung. Server only. */
    private int age, groundTicks, hungTicks;
    /** Had antlers (a stag): they come off in the second pass. Server only. */
    private boolean trophy;
    private float work;
    /** Where the hauled body last left blood (CarcassHauling#bleed). Server only, not saved. */
    Vec3 lastStain;
    /** The dead animal's saved data (coat colour, variant ...), so the body looks like the animal did. */
    private CompoundTag looks = new CompoundTag();
    /** Client only: a never-added copy of the animal used to render the body. */
    public Entity renderDummy;

    public CarcassEntity(EntityType<? extends CarcassEntity> type, Level level) {
        super(type, level);
    }

    public static CarcassEntity of(Level level, LivingEntity dead) {
        CarcassEntity carcass = new CarcassEntity(Mechanical.CARCASS.get(), level);
        carcass.animal = dead.getType();
        carcass.setYRot(dead.yBodyRot);
        carcass.looks = looksOf(dead);
        carcass.trophy = dead instanceof com.mechanical.wildlife.WildAnimal wild && wild.showsHorns()
                && (dead instanceof com.mechanical.wildlife.RedDeer || dead instanceof com.mechanical.wildlife.Moose || dead instanceof com.mechanical.wildlife.Reindeer);
        carcass.setPos(dead.position());
        carcass.refreshDimensions();
        return carcass;
    }

    private static CompoundTag looksOf(LivingEntity dead) {
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, dead.registryAccess());
        dead.saveWithoutId(out);
        CompoundTag tag = out.buildResult();
        // drop what would make the copy look dead or dragged along
        for (String key : new String[] {"DeathTime", "HurtTime", "HurtByTimestamp", "Health", "Fire", "Passengers", "leash", "Brain", "active_effects"}) tag.remove(key);
        return tag;
    }

    public CompoundTag looks() {
        return looks;
    }

    public EntityType<?> animal() {
        return animal;
    }

    public int stage() {
        return entityData.get(STAGE);
    }

    /** Body width class: small (< 0.6, hare, birds, squirrel), large (>= 1.1, aurochs, horse, bear, moose, lion), huge (>= 2, giant deer). */
    public boolean isSmall() {
        return animal.getWidth() < 0.6F;
    }

    /** Light enough to carry at full speed: small bodies and the edible birds (a goose too). */
    public boolean isLight() {
        return isSmall() || Birds.isBird(animal);
    }

    public boolean isLarge() {
        return animal.getWidth() >= 1.1F;
    }

    public boolean isHuge() {
        return animal.getWidth() >= 2.0F;
    }

    /** Room it takes on a travois (holds 3) or a cart (holds 6): light 1, medium 2, large 3. Huge ones don't go on at all. */
    public int loadUnits() {
        return isLight() ? 1 : isLarge() ? 3 : 2;
    }

    /**
     * Players it takes to drag it at all: 1 for a large body (more just go faster), 3 for a huge one (giant deer, walrus)
     * and 4 for the very biggest (width 2.3+: the Woolly Mammoth). Fewer can only strain at it (CarcassHauling#follow).
     */
    public int pullersNeeded() {
        return !isHuge() ? 1 : animal.getWidth() >= 2.3F ? 4 : 3;
    }

    /** Entity id of the first player hauling it, -1 if none. */
    public int haulerId() {
        for (var slot : HAULERS) if (entityData.get(slot) >= 0) return entityData.get(slot);
        return -1;
    }

    /** Entity id of the player holding limb {@code slot}, -1 if free. */
    public int haulerId(int slot) {
        return entityData.get(HAULERS.get(slot));
    }

    /** The limb {@code player} holds, or -1. */
    public int slotOf(Player player) {
        for (int i = 0; i < MAX_HAULERS; i++) if (haulerId(i) == player.getId()) return i;
        return -1;
    }

    public int haulerCount() {
        int n = 0;
        for (int i = 0; i < MAX_HAULERS; i++) if (haulerId(i) >= 0) n++;
        return n;
    }

    /** The player holding limb {@code slot}, or null. */
    public Player hauler(int slot) {
        int id = haulerId(slot);
        return id >= 0 && level().getEntity(id) instanceof Player p ? p : null;
    }

    /** The (first) player carrying or dragging it, or null. */
    public Player hauler() {
        int id = haulerId();
        return id >= 0 && level().getEntity(id) instanceof Player p ? p : null;
    }

    /** null: everyone lets go; a player: he alone holds it. */
    void setHauler(Player player) {
        for (var slot : HAULERS) entityData.set(slot, -1);
        if (player != null) entityData.set(HAULERS.get(0), player.getId());
    }

    /** Takes the first free limb; false when all are held (or a smaller body is already held by someone). */
    boolean addHauler(Player player) {
        for (int i = 0; i < (isLarge() ? MAX_HAULERS : 1); i++) {
            if (haulerId(i) < 0) {
                entityData.set(HAULERS.get(i), player.getId());
                return true;
            }
        }
        return false;
    }

    void removeHauler(int slot) {
        entityData.set(HAULERS.get(slot), -1);
    }

    /**
     * Where the player holding limb {@code slot} grips a dragged body, at the frame (both sides). It lies on its side with
     * the head towards the pullers ({@link #getYRot()}) and the legs out to its belly side: the forelegs (0, 1) are pulled
     * out ahead of the head, the hind legs (2, 3) forward along the belly (CarcassRenderer stretches the held legs so).
     */
    public Vec3 gripPoint(int slot, float partialTicks) {
        Vec3 head = Vec3.directionFromRotation(0, getViewYRot(partialTicks)), belly = new Vec3(-head.z, 0, head.x);
        double forward = animal.getWidth() * (slot < 2 ? 0.5 : 0.1), out = animal.getHeight() * (0.25 + 0.2 * (slot % 2));
        return getPosition(partialTicks).add(head.scale(forward)).add(belly.scale(out));
    }

    public BlockPos hungAt() {
        return entityData.get(HUNG_AT).orElse(null);
    }

    /** Hangs it from the rack at {@code pos}, belly to the rack's front. */
    public void hang(BlockPos pos, net.minecraft.core.Direction front) {
        setHauler(null);
        stopRiding();
        entityData.set(HUNG_AT, java.util.Optional.of(pos));
        hungTicks = 0;
        entityData.set(AGED_LOOK, false);
        setYRot(front.getOpposite().toYRot());
        refreshDimensions();
        setPos(hangPosition(pos));
    }

    /** The horse it hangs on, or null. Both sides. */
    public net.minecraft.world.entity.animal.equine.AbstractHorse slungHorse() {
        int v = entityData.get(SLUNG_ON);
        return v >= 0 && level().getEntity(v >> 1) instanceof net.minecraft.world.entity.animal.equine.AbstractHorse h ? h : null;
    }

    /** The side it hangs on: 0 left, 1 right. Both sides. */
    public int slungSide() {
        return Math.max(0, entityData.get(SLUNG_ON)) & 1;
    }

    /** Hangs it on {@code horse}'s side {@code side} (0 left, 1 right), out of everyone's hands. */
    void slingOn(net.minecraft.world.entity.animal.equine.AbstractHorse horse, int side) {
        setHauler(null);
        stopRiding();
        slungHorse = horse.getUUID();
        slungSide = side;
        slungMissing = 0;
        entityData.set(SLUNG_ON, horse.getId() * 2 + side);
        setPos(sidePoint(horse, side));
    }

    void unsling() {
        slungHorse = null;
        entityData.set(SLUNG_ON, -1);
    }

    /** Where a body hangs on {@code horse}'s side {@code side}, its flank. */
    public static Vec3 sidePoint(Entity horse, int side) {
        float yaw = horse instanceof LivingEntity l ? l.yBodyRot : horse.getYRot();
        return horse.position().add(Vec3.directionFromRotation(0, yaw + (side == 0 ? -90 : 90)).scale(SIDE_OUT)).add(0, SIDE_UP, 0);
    }

    /** Server: keeps it on its horse's flank; it falls off when the horse dies or is gone for 5 s. False: it isn't slung. */
    private boolean keepSlung(ServerLevel level) {
        if (slungHorse == null) return false;
        if (level.getEntity(slungHorse) instanceof net.minecraft.world.entity.animal.equine.AbstractHorse horse && horse.isAlive()) {
            slungMissing = 0;
            if (entityData.get(SLUNG_ON) != horse.getId() * 2 + slungSide) entityData.set(SLUNG_ON, horse.getId() * 2 + slungSide);
            setPos(sidePoint(horse, slungSide));
            setDeltaMovement(Vec3.ZERO);
            return true;
        }
        if (++slungMissing < 100) return true; // not loaded yet after a reload: wait for it
        unsling();
        return false;
    }

    void unhang() {
        entityData.set(HUNG_AT, java.util.Optional.empty());
        refreshDimensions();
    }

    /** Hind feet at the rack's gambrel hook ({@link HangingRackBlock#HOOK} above its base), body hanging below. */
    private Vec3 hangPosition(BlockPos pos) {
        return new Vec3(pos.getX() + 0.5, pos.getY() + HangingRackBlock.HOOK - getDimensions(Pose.STANDING).height(), pos.getZ() + 0.5);
    }

    /** Ticks from the kill until the meat turns: a day, or three while it hangs on a rack. */
    public int spoilLimit() {
        return hungAt() != null ? HUNG_SPOILS : SPOILS;
    }

    /** The meat has turned. Only bodies kept off the ground last long enough for that. */
    public boolean spoiled() {
        return age > spoilLimit();
    }

    /** Both sides: close to spoiling (greenish, flies). */
    public boolean isTurning() {
        return entityData.get(TURNING_LOOK);
    }

    public boolean aged() {
        return hungTicks >= AGED;
    }

    /** Both sides: has aged on the rack (it keeps the dried look when taken down, until hung again). */
    public boolean isAged() {
        return entityData.get(AGED_LOOK);
    }

    public boolean isPrepared() {
        return entityData.get(PREPARED);
    }

    public void setPrepared() {
        entityData.set(PREPARED, true);
    }

    /** A caravan's goods still on its caravan's cart (CartEntity#isCaravan: until the horse falls or the caravan leaves). */
    public boolean isCaravanProperty() {
        return isPrepared() && getVehicle() instanceof CartEntity cart && cart.isCaravan();
    }

    /** Flat fliers (the phantom) rest on their belly instead of lying on the side. */
    public boolean liesFlat() {
        return animal == EntityTypes.PHANTOM;
    }

    /** Lying on its side: a square as long as the animal was tall or wide, as high as it was wide. The renderer centres the body on it. */
    @Override
    public EntityDimensions getDimensions(Pose pose) {
        // hanging head down: as tall as the body is long
        if (entityData.get(HUNG_AT).isPresent()) return EntityDimensions.scalable(Math.max(0.25F, animal.getWidth() * 0.8F), Math.max(animal.getWidth(), animal.getHeight()));
        if (liesFlat()) return EntityDimensions.scalable(animal.getWidth(), animal.getHeight());
        return EntityDimensions.scalable(Math.max(animal.getWidth(), animal.getHeight()), Math.max(0.25F, animal.getWidth()));
    }

    private int workNeeded() {
        float bulk = animal.getWidth() * animal.getWidth() * animal.getHeight();
        return (int) Math.round(WORK_PER_SIZE * WORK_PER_PASS * Math.pow(Math.max(1.0F, bulk), WORK_EXPONENT));
    }

    /** Client: glides between the server's positions (sent every tick, see Mechanical.CARCASS) instead of jumping, a step up smoothed over 4 ticks. */
    @Override
    protected InterpolationHandler createInterpolationHandler() {
        return LinearInterpolationHandler.create(this, 4);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            // a fly or two buzzing round an aged body on the rack
            if ((isAged() && hungAt() != null && random.nextInt(8) == 0) || (isTurning() && random.nextInt(3) == 0))
                level().addParticle(new DustParticleOptions(FLY, 0.45F), getX() + random.nextGaussian() * getBbWidth() * 0.6,
                        getY() + random.nextDouble() * getBbHeight(), getZ() + random.nextGaussian() * getBbWidth() * 0.6,
                        random.nextGaussian() * 0.02, random.nextGaussian() * 0.01, random.nextGaussian() * 0.02);
            return; // the client may not simulate movement; it gets the position from the server
        }
        if (isCaravanProperty()) return; // salted and kept by the traders
        age++;
        boolean turning = age >= spoilLimit() * TURNING;
        if (turning != isTurning()) entityData.set(TURNING_LOOK, turning);
        BlockPos rack = hungAt();
        if (rack != null) {
            if (!level().getBlockState(rack).is(CarcassHauling.HANGING_RACK.get())) unhang(); // the rack is gone: it drops
            else {
                setPos(hangPosition(rack));
                if (++hungTicks == AGED) entityData.set(AGED_LOOK, true);
                boolean bleeds = !spoiled() && !aged() && !animal.builtInRegistryHolder().is(Blood.BLOODLESS); // aged: the blood has stopped
                // skinned, it drips twice as often and the ground under the rack soaks red (HangingRackBlock#bleedOnto)
                if (bleeds && tickCount % (stage() > 0 ? 15 : 30) == 0 && random.nextInt(3) == 0)
                    Blood.drip((ServerLevel) level(), getX() + random.nextGaussian() * 0.1, getY() + 0.1, getZ() + random.nextGaussian() * 0.1);
                if (bleeds && stage() > 0 && tickCount % RACK_BLOOD_INTERVAL == 0 && random.nextInt(2) == 0)
                    HangingRackBlock.bleedOnto((ServerLevel) level(), rack);
                return;
            }
        }
        if (keepSlung((ServerLevel) level())) return;
        if (haulerId() >= 0) {
            for (int i = 0; i < MAX_HAULERS; i++) {
                Player hauler = hauler(i);
                // dead, gone or left behind: he lets go (with nobody left it drops where it is)
                if (haulerId(i) >= 0 && (hauler == null || !hauler.isAlive() || hauler.distanceTo(this) >= 12)) removeHauler(i);
            }
            if (haulerId() >= 0) {
                CarcassHauling.follow(this);
                return;
            }
        }
        if (isPassenger()) return; // on a travois, which places it
        // fall and settle like a dropped item; in water it rises and floats half under, drifting with the current
        Vec3 v = getDeltaMovement();
        if (getFluidHeight(FluidTags.WATER) > getBbHeight() * 0.5) setDeltaMovement(v.x, Math.min(v.y + 0.01, 0.06), v.z);
        else if (!onGround()) setDeltaMovement(v.add(0, -0.04, 0));
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().multiply(0.5, 0.98, 0.5));
        if (++groundTicks > LIFETIME) { // rotted away to the bones
            BlockPos pos = blockPosition();
            BlockState pile = Mechanical.BONE_PILE.get().defaultBlockState();
            if (level().getBlockState(pos).canBeReplaced() && level().getFluidState(pos).isEmpty() && pile.canSurvive(level(), pos)) level().setBlock(pos, pile, Block.UPDATE_ALL);
            discard();
        }
    }

    /** Dragged, it slides up steps and over roots. */
    @Override
    public float maxUpStep() {
        return haulerId() >= 0 ? 1.0F : 0.0F;
    }

    /** Moves it by {@code delta} as far as blocks allow (dragging). */
    void drag(Vec3 delta) {
        move(MoverType.SELF, delta);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (HUNG_AT.equals(key)) refreshDimensions();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (tickCount < HIDDEN_TICKS) return InteractionResult.PASS;
        if (isCaravanProperty() && (player.getItemInHand(hand).getItem() instanceof KnifeItem
                || hand == InteractionHand.MAIN_HAND && player.getMainHandItem().isEmpty() && player.isShiftKeyDown())) {
            if (!level().isClientSide()) {
                player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.mechanical.carcass.caravan"));
                if (getVehicle() instanceof CartEntity cart && cart.harness() instanceof net.minecraft.world.entity.Leashable horse
                        && horse.getLeashHolder() instanceof com.mechanical.people.Tribesperson owner) owner.caughtStealing(player);
            }
            return InteractionResult.FAIL;
        }
        // a loaded body covers most of its cart: clicks meant for the cart (load another, harness) go through to it
        if (hand == InteractionHand.MAIN_HAND && getVehicle() instanceof TravoisEntity bed && (CarcassHauling.hauled(player) != null
                || player.getMainHandItem().isEmpty() && !player.isShiftKeyDown() && !(stage() > 0 && Birds.isBird(animal))))
            return bed.interact(player, hand, location);
        if (hand == InteractionHand.MAIN_HAND && player.getMainHandItem().isEmpty() && (player.isShiftKeyDown() || slotOf(player) >= 0))
            return CarcassHauling.pickUpOrLetGo(player, this);
        if (hand == InteractionHand.MAIN_HAND && player.getMainHandItem().isEmpty() && stage() > 0 && Birds.isBird(animal)) {
            // a plucked bird is taken whole (Birds): cut up in the grid with a knife or cooked whole
            if (!level().isClientSide()) {
                player.getInventory().placeItemBackInInventory(new net.minecraft.world.item.ItemStack(spoiled()
                        ? net.minecraft.world.item.Items.ROTTEN_FLESH : Birds.pluckedOf(animal)), net.minecraft.util.Prediction.SERVER_ONLY);
                playSound(SoundEvents.ITEM_PICKUP, 0.6F, 0.8F);
                discard();
            }
            return InteractionResult.SUCCESS;
        }
        if (hand == InteractionHand.MAIN_HAND && player.getMainHandItem().isEmpty() && hungAt() != null) {
            // a look at the body on the rack: how long it has hung and how long it keeps
            if (!level().isClientSide()) player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(
                    spoiled() ? "message.mechanical.carcass.hung_spoiled" : aged() ? "message.mechanical.carcass.hung_aged" : "message.mechanical.carcass.hung",
                    hungTicks / 1200, Math.max(0, spoilLimit() - age) / 1200));
            return InteractionResult.SUCCESS;
        }
        if (!(player.getItemInHand(hand).getItem() instanceof KnifeItem knife)) return InteractionResult.PASS;
        if (knife.tier() < Butchery.requiredKnifeTier(animal)) {
            if (!level().isClientSide()) player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.mechanical.knife_too_weak"));
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    /** One tick of butchering by {@code player} with a knife of the given speed. Server side. Players cutting at once all add to the same work: two finish in half the time. */
    void butcher(ServerLevel level, Player player, KnifeItem knife) {
        if (isCaravanProperty()) {
            player.stopUsingItem();
            return;
        }
        work += knife.speed();
        if (tickCount % 4 == 0) {
            Vec3 c = getBoundingBox().getCenter();
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, Butchery.yieldFor(animal).meat().get()), c.x, c.y, c.z, 3, 0.2, 0.1, 0.2, 0.05);
            Blood.spray(level, c.x, c.y, c.z, 4);
            if (tickCount % 20 == 0) Blood.splatter(level, blockPosition(), 1, 1); // fills up around the body while you work
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.SLIME_SQUISH_SMALL, SoundSource.PLAYERS, 0.6F, 0.7F + random.nextFloat() * 0.3F);
        }
        if (work < workNeeded()) return;
        work = 0;
        player.getUseItem().hurtAndBreak(1, player, player.getUsedItemHand());
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) Butchery.awardButchered(sp, animal);
        if (stage() == 0 && !Butchery.yieldFor(animal).outer().isEmpty()) {
            // first pass: wool sheared / hide skinned / feathers plucked; keep holding to go on with the meat
            Butchery.dropOuter(level, this, animal, knife.hideMultiplier(), hungAt() != null);
            Blood.splatter(level, blockPosition(), 1, 5);
            entityData.set(STAGE, 1);
            return;
        }
        boolean hung = hungAt() != null;
        if (spoiled()) player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.mechanical.carcass.spoiled"));
        Butchery.dropInner(level, this, animal, knife.yieldMultiplier(), trophy, hung, aged(), spoiled());
        Blood.splatter(level, blockPosition(), 2, 10); // a pool around the remains
        player.stopUsingItem();
        if (hung) {
            discard(); // the bones came off with the cuts; nothing is left on the hook
            return;
        }
        // the bones stay behind
        BlockPos pos = blockPosition();
        BlockState pile = Mechanical.BONE_PILE.get().defaultBlockState();
        if (level.getBlockState(pos).canBeReplaced() && pile.canSurvive(level, pos)) level.setBlock(pos, pile, Block.UPDATE_ALL);
        discard();
    }

    @Override
    public boolean isPickable() {
        // carried on someone's shoulders it would sit in front of his eyes and catch every click
        return tickCount >= HIDDEN_TICKS && (haulerId() < 0 || isLarge());
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(STAGE, 0);
        for (var slot : HAULERS) builder.define(slot, -1);
        builder.define(HUNG_AT, java.util.Optional.empty());
        builder.define(AGED_LOOK, false);
        builder.define(TURNING_LOOK, false);
        builder.define(PREPARED, false);
        builder.define(SLUNG_ON, -1);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        input.getString("animal").map(Identifier::parse).flatMap(BuiltInRegistries.ENTITY_TYPE::getOptional).ifPresent(t -> animal = t);
        input.read("body_yaw", com.mojang.serialization.Codec.FLOAT).ifPresent(this::setYRot); // saves from before the yaw was the entity's own rotation
        age = input.getIntOr("age", 0);
        groundTicks = input.getIntOr("ground_ticks", 0);
        hungTicks = input.getIntOr("hung_ticks", 0);
        entityData.set(AGED_LOOK, hungTicks >= AGED);
        input.read("hung_at", BlockPos.CODEC).ifPresent(p -> entityData.set(HUNG_AT, java.util.Optional.of(p)));
        work = input.getFloatOr("work", 0);
        entityData.set(STAGE, input.getIntOr("stage", 0));
        trophy = input.getBooleanOr("trophy", false);
        entityData.set(PREPARED, input.getBooleanOr("prepared", false));
        looks = input.read("looks", CompoundTag.CODEC).orElseGet(CompoundTag::new);
        slungHorse = input.read("slung_horse", net.minecraft.core.UUIDUtil.CODEC).orElse(null);
        slungSide = input.getIntOr("slung_side", 0);
        refreshDimensions();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putString("animal", BuiltInRegistries.ENTITY_TYPE.getKey(animal).toString());
        output.putInt("age", age);
        output.putInt("ground_ticks", groundTicks);
        output.putInt("hung_ticks", hungTicks);
        BlockPos rack = hungAt();
        if (rack != null) output.store("hung_at", BlockPos.CODEC, rack);
        output.putFloat("work", work);
        output.putInt("stage", stage());
        output.putBoolean("trophy", trophy);
        output.putBoolean("prepared", isPrepared());
        output.store("looks", CompoundTag.CODEC, looks);
        if (slungHorse != null) {
            output.store("slung_horse", net.minecraft.core.UUIDUtil.CODEC, slungHorse);
            output.putInt("slung_side", slungSide);
        }
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buf) {
        buf.writeIdentifier(BuiltInRegistries.ENTITY_TYPE.getKey(animal));
        buf.writeNbt(looks);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buf) {
        animal = BuiltInRegistries.ENTITY_TYPE.getValue(buf.readIdentifier());
        CompoundTag tag = buf.readNbt();
        looks = tag != null ? tag : new CompoundTag();
        refreshDimensions();
    }
}
