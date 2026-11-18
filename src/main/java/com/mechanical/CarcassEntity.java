package com.mechanical;

import net.minecraft.core.BlockPos;
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
 * comes from {@link Butchery}. Disappears after five minutes. Spawned where the body lies once the death animation has finished.
 */
public class CarcassEntity extends Entity implements IEntityWithComplexSpawn {
    public static final int HIDDEN_TICKS = 0, LIFETIME = 6000;
    /** Butchering work needed per block of body size, in knife-speed ticks (flint knife: 1 per tick). */
    private static final int WORK_PER_SIZE = 60;

    /** 0: whole (wool/hide/feathers still on), 1: skinned/sheared/plucked, only the meat left. Synced for the renderer. */
    private static final EntityDataAccessor<Integer> STAGE = SynchedEntityData.defineId(CarcassEntity.class, EntityDataSerializers.INT);
    /** Each of the two passes takes this share of the work for the body size. */
    private static final float WORK_PER_PASS = 0.6F;

    private EntityType<?> animal = EntityTypes.PIG;
    private float bodyYaw;
    /** Had antlers (a stag): they come off in the second pass. Server only. */
    private boolean trophy;
    private float work;
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
        carcass.bodyYaw = dead.yBodyRot;
        carcass.looks = looksOf(dead);
        carcass.trophy = dead instanceof com.mechanical.wildlife.WildAnimal wild && wild.showsHorns() && dead instanceof com.mechanical.wildlife.RedDeer;
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

    public float bodyYaw() {
        return bodyYaw;
    }

    /** Lying on its side: a square as long as the animal was tall or wide, as high as it was wide. The renderer centres the body on it. */
    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.scalable(Math.max(animal.getWidth(), animal.getHeight()), Math.max(0.25F, animal.getWidth()));
    }

    private int workNeeded() {
        return Math.round(WORK_PER_SIZE * WORK_PER_PASS * Math.max(1.0F, Math.max(animal.getWidth(), animal.getHeight())));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) return; // the client may not simulate movement; it gets the position from the server
        // fall and settle like a dropped item
        if (!onGround()) setDeltaMovement(getDeltaMovement().add(0, -0.04, 0));
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().multiply(0.5, 0.98, 0.5));
        if (tickCount > LIFETIME) discard();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (tickCount < HIDDEN_TICKS || !(player.getItemInHand(hand).getItem() instanceof KnifeItem knife)) return InteractionResult.PASS;
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
            Butchery.dropOuter(level, this, animal, knife.yieldMultiplier());
            Blood.splatter(level, blockPosition(), 1, 5);
            entityData.set(STAGE, 1);
            return;
        }
        Butchery.dropInner(level, this, animal, knife.yieldMultiplier(), trophy);
        Blood.splatter(level, blockPosition(), 2, 10); // a pool around the remains
        player.stopUsingItem();
        // the bones stay behind
        BlockPos pos = blockPosition();
        BlockState pile = Mechanical.BONE_PILE.get().defaultBlockState();
        if (level.getBlockState(pos).canBeReplaced() && pile.canSurvive(level, pos)) level.setBlock(pos, pile, Block.UPDATE_ALL);
        discard();
    }

    @Override
    public boolean isPickable() {
        return tickCount >= HIDDEN_TICKS;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(STAGE, 0);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        input.getString("animal").map(Identifier::parse).flatMap(BuiltInRegistries.ENTITY_TYPE::getOptional).ifPresent(t -> animal = t);
        bodyYaw = input.getFloatOr("body_yaw", 0);
        work = input.getFloatOr("work", 0);
        entityData.set(STAGE, input.getIntOr("stage", 0));
        trophy = input.getBooleanOr("trophy", false);
        looks = input.read("looks", CompoundTag.CODEC).orElseGet(CompoundTag::new);
        refreshDimensions();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putString("animal", BuiltInRegistries.ENTITY_TYPE.getKey(animal).toString());
        output.putFloat("body_yaw", bodyYaw);
        output.putFloat("work", work);
        output.putInt("stage", stage());
        output.putBoolean("trophy", trophy);
        output.store("looks", CompoundTag.CODEC, looks);
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buf) {
        buf.writeIdentifier(BuiltInRegistries.ENTITY_TYPE.getKey(animal));
        buf.writeFloat(bodyYaw);
        buf.writeNbt(looks);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buf) {
        animal = BuiltInRegistries.ENTITY_TYPE.getValue(buf.readIdentifier());
        bodyYaw = buf.readFloat();
        CompoundTag tag = buf.readNbt();
        looks = tag != null ? tag : new CompoundTag();
        refreshDimensions();
    }
}
