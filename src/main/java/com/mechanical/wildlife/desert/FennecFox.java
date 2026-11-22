package com.mechanical.wildlife.desert;

import java.util.EnumSet;

import com.mechanical.AnimalFear;
import com.mechanical.wildlife.WildAnimal;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Fennec Fox: a tiny cream-coloured desert fox with enormous ears. Night-active: it sleeps curled up in its scrape by day
 * ({@link #sleepTime}) and roams at night; very shy. Tamed with meat: crouch holding raw meat (it then sits and watches
 * you) and feed it by hand {@link #TAME_FEEDS} times, once per {@link #FEED_COOLDOWN} ticks; standing up close it bolts,
 * and hurting it costs all its trust. Then it becomes a {@link TameFennec}.
 */
public class FennecFox extends WildAnimal {
    public static final int TAME_FEEDS = 3, FEED_COOLDOWN = 100;
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/fennec_fox.png");
    private int trust, feedCooldown;

    public FennecFox(EntityType<? extends FennecFox> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 8.0).add(Attributes.MOVEMENT_SPEED, 0.33);
    }

    /** Raw meat a fennec takes from the hand: vanilla meat and the mod's game (#mechanical:smokable_meat). */
    public static boolean isMeat(ItemStack stack) {
        return stack.is(ItemTags.MEAT) || stack.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                Identifier.fromNamespaceAndPath("mechanical", "smokable_meat")));
    }

    private static boolean holdsMeat(Player player) {
        return isMeat(player.getMainHandItem()) || isMeat(player.getOffhandItem());
    }

    public int trust() {
        return trust;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(-1, new CalmGoal()); // beats AnimalFear's flee goal (0, added on join)
    }

    /** Sleeps by day in its scrape, out at night. */
    @Override
    protected boolean sleepTime() {
        return level().isBrightOutside();
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (isBaby() || !isMeat(stack)) return super.mobInteract(player, hand);
        if (level().isClientSide()) return InteractionResult.SUCCESS;
        feed(player, stack);
        return InteractionResult.SUCCESS_SERVER;
    }

    /** Server: one hand-feeding; the last one tames it. */
    public void feed(Player player, ItemStack stack) {
        if (!player.isCrouching()) {
            AnimalFear.panic(this, player.position());
            player.sendOverlayMessage(Component.translatable("message.mechanical.fennec.shies"));
            return;
        }
        if (feedCooldown > 0) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.fennec.chewing"));
            return;
        }
        stack.consume(1, player);
        feedCooldown = FEED_COOLDOWN;
        trust++;
        if (isDozing()) wake();
        getNavigation().stop();
        playSound(SoundEvents.FOX_EAT, 1.0F, 1.4F);
        ((ServerLevel) level()).sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight() + 0.2, getZ(), 1, 0.2, 0.1, 0.2, 0);
        if (trust >= TAME_FEEDS) tame(player);
        else player.sendOverlayMessage(Component.translatable("message.mechanical.fennec.trust", trust, TAME_FEEDS));
    }

    /** Becomes a tame fennec owned by {@code player}. */
    public TameFennec tame(Player player) {
        TameFennec tame = convertTo(DesertFauna.TAME_FENNEC.get(), ConversionParams.single(this, false, false), f -> {
            f.tame(player);
            f.setHealth(Math.min(getHealth(), f.getMaxHealth()));
        });
        if (tame == null) return null;
        level().broadcastEntityEvent(tame, (byte) 7); // vanilla taming hearts
        player.sendOverlayMessage(Component.translatable("message.mechanical.fennec.tamed"));
        return tame;
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
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        trust = input.getIntOr("Trust", 0);
    }

    @Override
    public Identifier texture() {
        return TEXTURE;
    }

    @Override
    protected float babyChance() {
        return 0.3F;
    }

    @Override
    public double fearMultiplier() {
        return 1.6;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isDozing() ? SoundEvents.FOX_SLEEP : SoundEvents.FOX_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.FOX_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.FOX_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 1.5F;
    }

    /** A crouching player holding meat: it sits up and watches instead of running. */
    class CalmGoal extends Goal {
        private Player player;

        CalmGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            player = level().getNearestPlayer(FennecFox.this, 10);
            return player != null && !player.isSpectator() && player.isCrouching() && holdsMeat(player) && !AnimalFear.isPanicking(FennecFox.this)
                    && !isBaby();
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
