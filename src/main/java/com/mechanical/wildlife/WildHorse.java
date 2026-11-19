package com.mechanical.wildlife;

import com.mechanical.Carting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Stocky dun wild horse (tarpan / Przewalski type) in bands on the open plains. Very skittish and fast; never fights.
 * <p>Taming (Bronze Age): sneak up holding horse feed ({@link Carting#HORSE_FEED}; it doesn't shy from that, see
 * {@link #calmAround}) and feed it by hand, once per {@link #FEED_COOLDOWN} ticks. Each feeding is one point of trust; at
 * {@link #HALTER_TRUST} it no longer runs from you (unless you sprint) and takes a Rope Halter, which leads it. Fed on the
 * halter up to {@link #TAME_TRUST} it becomes a {@link TameHorse}. Hurting it costs all its trust.
 */
public class WildHorse extends WildAnimal {
    public static final int HALTER_TRUST = 3, TAME_TRUST = 6, FEED_COOLDOWN = 200;
    private int trust, feedCooldown;
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/wild_horse.png");
    private static final Identifier FOAL_TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/wild_horse_foal.png");

    public WildHorse(EntityType<? extends WildHorse> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 24.0).add(Attributes.MOVEMENT_SPEED, 0.33).add(Attributes.STEP_HEIGHT, 1.0);
    }

    public int trust() {
        return trust;
    }

    /** Doesn't notice (AnimalFear) a player crouching with feed in hand, nor, once it trusts people, one who walks calmly. */
    public boolean calmAround(Player player) {
        boolean feed = player.getMainHandItem().is(Carting.HORSE_FEED) || player.getOffhandItem().is(Carting.HORSE_FEED);
        return !isBaby() && (player.isCrouching() && feed || trust >= HALTER_TRUST && !player.isSprinting());
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (isBaby() || !stack.is(Carting.HORSE_FEED)) return super.mobInteract(player, hand);
        if (level().isClientSide()) return InteractionResult.SUCCESS;
        feed(player, stack);
        return InteractionResult.SUCCESS_SERVER;
    }

    /** Server: one hand-feeding. Standing up close before it trusts you, it bolts. */
    void feed(Player player, ItemStack stack) {
        if (!player.isCrouching() && trust < HALTER_TRUST) {
            com.mechanical.AnimalFear.panic(this, player.position());
            player.sendOverlayMessage(Component.translatable("message.mechanical.horse.shies"));
            return;
        }
        if (feedCooldown > 0) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.horse.chewing"));
            return;
        }
        if (trust >= HALTER_TRUST && !isLeashed()) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.horse.needs_halter"));
            return;
        }
        stack.consume(1, player);
        feedCooldown = FEED_COOLDOWN;
        trust++;
        getNavigation().stop();
        playSound(SoundEvents.HORSE_EAT, 1.0F, 1.0F);
        ((ServerLevel) level()).sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight() + 0.2, getZ(), 2, 0.3, 0.2, 0.3, 0);
        if (trust >= TAME_TRUST) tame(player);
        else player.sendOverlayMessage(Component.translatable(trust >= HALTER_TRUST && !isLeashed()
                ? "message.mechanical.horse.ready_for_halter" : "message.mechanical.horse.trust", trust, TAME_TRUST));
    }

    /** Rope Halter on it: leads it once it trusts you enough. */
    public InteractionResult halter(Player player, ItemStack stack) {
        if (level().isClientSide()) return InteractionResult.SUCCESS;
        if (isLeashed()) return InteractionResult.PASS;
        if (trust < HALTER_TRUST) {
            com.mechanical.AnimalFear.panic(this, player.position());
            player.sendOverlayMessage(Component.translatable("message.mechanical.horse.refuses_halter"));
            return InteractionResult.SUCCESS_SERVER;
        }
        setLeashedTo(player, true);
        stack.consume(1, player);
        playSound(SoundEvents.LEAD_TIED, 1.0F, 1.0F);
        player.sendOverlayMessage(Component.translatable("message.mechanical.horse.haltered", trust, TAME_TRUST));
        return InteractionResult.SUCCESS_SERVER;
    }

    /** Becomes a tame horse owned by {@code player}, keeping its halter rope, name and health. */
    private void tame(Player player) {
        TameHorse horse = convertTo(Wildlife.TAME_HORSE.get(), ConversionParams.single(this, false, false), h -> {
            h.setTamed(true);
            h.setOwner(player);
            h.randomizeAttributes(getRandom());
            h.setHealth(Math.min(getHealth(), h.getMaxHealth()));
        });
        if (horse == null) return;
        level().broadcastEntityEvent(horse, (byte) 7); // vanilla taming hearts
        // the Bronze Age "Horse Whisperer" advancement listens for this
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) net.minecraft.advancements.triggers.CriteriaTriggers.TAME_ANIMAL.trigger(sp, horse);
        player.sendOverlayMessage(Component.translatable("message.mechanical.horse.tamed"));
    }

    @Override
    public void aiStep() {
        if (feedCooldown > 0) feedCooldown--;
        super.aiStep();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (source.getEntity() instanceof Player) trust = 0;
        return super.hurtServer(level, source, damage);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Trust", trust);
        output.putInt("FeedCooldown", feedCooldown);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        trust = input.getIntOr("Trust", 0);
        feedCooldown = input.getIntOr("FeedCooldown", 0);
    }

    @Override
    public Identifier texture() {
        return isBaby() ? FOAL_TEXTURE : TEXTURE;
    }

    @Override
    protected float babyChance() {
        return 0.25F;
    }

    @Override
    public double fearMultiplier() {
        return 1.8;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isBaby() ? SoundEvents.HORSE_AMBIENT_BABY : SoundEvents.HORSE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return isBaby() ? SoundEvents.HORSE_HURT_BABY : SoundEvents.HORSE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return isBaby() ? SoundEvents.HORSE_DEATH_BABY : SoundEvents.HORSE_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(isBaby() ? SoundEvents.HORSE_STEP_BABY : SoundEvents.HORSE_STEP, 0.15F, 1.0F);
    }

    @Override
    public int getAmbientSoundInterval() {
        return 400; // the occasional whinny across the plain, not constant chatter
    }
}
