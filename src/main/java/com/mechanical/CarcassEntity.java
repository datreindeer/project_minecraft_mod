package com.mechanical;

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
 */
public class CarcassEntity extends Entity implements IEntityWithComplexSpawn {
    public static final int HIDDEN_TICKS = 0, LIFETIME = 6000;
    /** Butchering work needed per block of body size, in knife-speed ticks (flint knife: 1 per tick). */
    private static final int WORK_PER_SIZE = 60;

    /** 0: whole (wool/hide/feathers still on), 1: skinned/sheared/plucked, only the meat left. Synced for the renderer. */
    private static final EntityDataAccessor<Integer> STAGE = SynchedEntityData.defineId(CarcassEntity.class, EntityDataSerializers.INT);
    /** Entity id of the player carrying or dragging it, -1 if none. Synced for the renderer and the sprint check. */
    private static final EntityDataAccessor<Integer> HAULER = SynchedEntityData.defineId(CarcassEntity.class, EntityDataSerializers.INT);
    /** The Hanging Rack it hangs on. Synced for the renderer and the hitbox. */
    private static final EntityDataAccessor<java.util.Optional<BlockPos>> HUNG_AT = SynchedEntityData.defineId(CarcassEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    /** Ticks after the kill when a carcass that is hauled, on a travois or hung has spoiled (one day). */
    public static final int SPOILS = 24000;
    /** On a Hanging Rack (cool, airy, off the ground) a body keeps three days from the kill. */
    public static final int HUNG_SPOILS = 3 * 24000;
    /** From this share of its time it starts to turn: greenish and flies (a warning before it spoils). */
    private static final float TURNING = 0.75F;
    /** Hung this long (3 minutes) it has aged: a little more meat on the rack. */
    public static final int AGED = 3600;
    /** Has hung {@link #AGED} ticks: synced so it looks dried (CarcassRenderer), stops dripping and draws a fly or two. */
    private static final EntityDataAccessor<Boolean> AGED_LOOK = SynchedEntityData.defineId(CarcassEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> TURNING_LOOK = SynchedEntityData.defineId(CarcassEntity.class, EntityDataSerializers.BOOLEAN);
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

    public int haulerId() {
        return entityData.get(HAULER);
    }

    /** The player carrying or dragging it, or null. */
    public Player hauler() {
        int id = haulerId();
        return id >= 0 && level().getEntity(id) instanceof Player p ? p : null;
    }

    void setHauler(Player player) {
        entityData.set(HAULER, player == null ? -1 : player.getId());
    }

    public BlockPos hungAt() {
        return entityData.get(HUNG_AT).orElse(null);
    }

    /** Hangs it from the rack at {@code pos}, belly to the rack's front. */
    void hang(BlockPos pos, net.minecraft.core.Direction front) {
        setHauler(null);
        stopRiding();
        entityData.set(HUNG_AT, java.util.Optional.of(pos));
        hungTicks = 0;
        entityData.set(AGED_LOOK, false);
        setYRot(front.getOpposite().toYRot());
        refreshDimensions();
        setPos(hangPosition(pos));
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
        return Math.round(WORK_PER_SIZE * WORK_PER_PASS * Math.max(1.0F, Math.max(animal.getWidth(), animal.getHeight())));
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
        age++;
        boolean turning = age >= spoilLimit() * TURNING;
        if (turning != isTurning()) entityData.set(TURNING_LOOK, turning);
        BlockPos rack = hungAt();
        if (rack != null) {
            if (!level().getBlockState(rack).is(CarcassHauling.HANGING_RACK.get())) unhang(); // the rack is gone: it drops
            else {
                setPos(hangPosition(rack));
                if (++hungTicks == AGED) entityData.set(AGED_LOOK, true);
                if (tickCount % 30 == 0 && !spoiled() && !aged() && random.nextInt(3) == 0) // aged: the blood has stopped
                    Blood.drip((ServerLevel) level(), getX() + random.nextGaussian() * 0.1, getY() + 0.1, getZ() + random.nextGaussian() * 0.1);
                return;
            }
        }
        if (haulerId() >= 0) {
            Player hauler = hauler();
            if (hauler != null && hauler.isAlive() && hauler.distanceTo(this) < 12) {
                CarcassHauling.follow(this, hauler);
                return;
            }
            setHauler(null); // dead, gone or left behind: it drops where it is
        }
        if (isPassenger()) return; // on a travois, which places it
        // fall and settle like a dropped item
        if (!onGround()) setDeltaMovement(getDeltaMovement().add(0, -0.04, 0));
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().multiply(0.5, 0.98, 0.5));
        if (++groundTicks > LIFETIME) discard();
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
        // a loaded body covers most of its cart: clicks meant for the cart (load another, harness) go through to it
        if (hand == InteractionHand.MAIN_HAND && getVehicle() instanceof TravoisEntity bed && (CarcassHauling.hauled(player) != null
                || player.getMainHandItem().isEmpty() && !player.isShiftKeyDown() && !(stage() > 0 && Birds.isBird(animal))))
            return bed.interact(player, hand, location);
        if (hand == InteractionHand.MAIN_HAND && player.getMainHandItem().isEmpty() && (player.isShiftKeyDown() || haulerId() == player.getId()))
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

    /** One tick of butchering by {@code player} with a knife of the given speed. Server side. */
    void butcher(ServerLevel level, Player player, KnifeItem knife) {
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
        builder.define(HAULER, -1);
        builder.define(HUNG_AT, java.util.Optional.empty());
        builder.define(AGED_LOOK, false);
        builder.define(TURNING_LOOK, false);
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
        looks = input.read("looks", CompoundTag.CODEC).orElseGet(CompoundTag::new);
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
        output.store("looks", CompoundTag.CODEC, looks);
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
