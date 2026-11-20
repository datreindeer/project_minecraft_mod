package com.mechanical.wildlife.warm;

import java.util.EnumSet;

import com.mechanical.AnimalFear;
import com.mechanical.Carting;
import com.mechanical.wildlife.WildAnimal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Onager (Asiatic wild ass): a slight, sandy wild ass with a dark stripe down the back, in small herds on desert, badlands
 * and savanna. Even more skittish and faster than the wild horse; never fights.
 * <p>Taming like the wild horse ({@link com.mechanical.wildlife.WildHorse}): sneak up holding horse feed and feed it by
 * hand, once per {@link #FEED_COOLDOWN} ticks; at {@link #HALTER_TRUST} it takes a Rope Halter ({@link WarmFauna#onHalter}),
 * and fed on the halter up to {@link #TAME_TRUST} it becomes a {@link TameOnager}: a pack animal that carries Pack Baskets
 * and pulls the Cart. Hurting it costs all its trust.
 */
public class Onager extends WildAnimal {
    public static final int HALTER_TRUST = 3, TAME_TRUST = 7, FEED_COOLDOWN = 200;
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/onager.png");
    private static final Identifier FOAL_TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/onager_foal.png");
    private int trust, feedCooldown;

    public Onager(EntityType<? extends Onager> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 22.0).add(Attributes.MOVEMENT_SPEED, 0.34).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // stands still and watches a calm player (beats AnimalFear's flee goal, which is added at 0 when it joins)
        goalSelector.addGoal(-1, new CalmGoal());
    }

    public int trust() {
        return trust;
    }

    private static boolean holdsFeed(Player player) {
        return player.getMainHandItem().is(Carting.HORSE_FEED) || player.getOffhandItem().is(Carting.HORSE_FEED);
    }

    /** Lets a player crouching with feed in hand come close, and, once it trusts people, one who walks calmly. */
    public boolean calmAround(Player player) {
        return !isBaby() && !AnimalFear.isPanicking(this) && (player.isCrouching() && holdsFeed(player) || trust >= HALTER_TRUST && !player.isSprinting());
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
            AnimalFear.panic(this, player.position());
            player.sendOverlayMessage(Component.translatable("message.mechanical.onager.shies"));
            return;
        }
        if (feedCooldown > 0) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.onager.chewing"));
            return;
        }
        if (trust >= HALTER_TRUST && !isLeashed()) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.onager.needs_halter"));
            return;
        }
        stack.consume(1, player);
        feedCooldown = FEED_COOLDOWN;
        trust++;
        getNavigation().stop();
        playSound(SoundEvents.DONKEY_EAT, 1.0F, 1.1F);
        ((ServerLevel) level()).sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight() + 0.2, getZ(), 2, 0.3, 0.2, 0.3, 0);
        if (trust >= TAME_TRUST) tame(player);
        else player.sendOverlayMessage(Component.translatable(trust >= HALTER_TRUST && !isLeashed()
                ? "message.mechanical.onager.ready_for_halter" : "message.mechanical.onager.trust", trust, TAME_TRUST));
    }

    /** Rope Halter on it: leads it once it trusts you enough. */
    public InteractionResult halter(Player player, ItemStack stack) {
        if (level().isClientSide()) return InteractionResult.SUCCESS;
        if (isLeashed()) return InteractionResult.PASS;
        if (trust < HALTER_TRUST) {
            AnimalFear.panic(this, player.position());
            player.sendOverlayMessage(Component.translatable("message.mechanical.onager.refuses_halter"));
            return InteractionResult.SUCCESS_SERVER;
        }
        setLeashedTo(player, true);
        stack.consume(1, player);
        playSound(SoundEvents.LEAD_TIED, 1.0F, 1.0F);
        player.sendOverlayMessage(Component.translatable("message.mechanical.onager.haltered", trust, TAME_TRUST));
        return InteractionResult.SUCCESS_SERVER;
    }

    /** Becomes a tame onager owned by {@code player}, keeping its halter rope, name and health. */
    private void tame(Player player) {
        TameOnager tame = convertTo(WarmFauna.TAME_ONAGER.get(), ConversionParams.single(this, false, false), o -> {
            o.setTamed(true);
            o.setOwner(player);
            o.randomizeAttributes(getRandom());
            o.setHealth(Math.min(getHealth(), o.getMaxHealth()));
        });
        if (tame == null) return;
        level().broadcastEntityEvent(tame, (byte) 7); // vanilla taming hearts
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) net.minecraft.advancements.triggers.CriteriaTriggers.TAME_ANIMAL.trigger(sp, tame);
        player.sendOverlayMessage(Component.translatable("message.mechanical.onager.tamed"));
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
        return 2.0;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.DONKEY_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.DONKEY_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.DONKEY_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.HORSE_STEP, 0.12F, 1.2F);
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * (isBaby() ? 1.4F : 1.1F);
    }

    @Override
    public int getAmbientSoundInterval() {
        return 500; // the odd bray across the flats
    }

    /** A calm player near (being fed / led): stand and watch him instead of bolting. */
    class CalmGoal extends Goal {
        private Player player;

        CalmGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (isLeashed()) return false; // led on the halter: follow the rope
            player = level().getNearestPlayer(Onager.this, 12);
            return player != null && !player.isCreative() && !player.isSpectator() && calmAround(player);
        }

        @Override
        public void start() {
            getNavigation().stop();
        }

        @Override
        public void tick() {
            getLookControl().setLookAt(player, 20.0F, 20.0F);
        }
    }
}
