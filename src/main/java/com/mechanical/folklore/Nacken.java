package com.mechanical.folklore;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.mechanical.Calendar;
import com.mechanical.Sanity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

/**
 * Näcken: on moonlit nights (dark, no rain) he sits in the rapids and plays the fiddle. Everyone within
 * {@link #HEAR_RANGE} blocks hears it; only sight {@link Faith#NACKEN_SIGHT} sees him. Listen for more than
 * {@link #LISTEN_TICKS} and you are drawn toward the water; once you are in it beside him the music stops and he is gone.
 * Right-click him with a gift (#mechanical:nacken_gifts: coins or meat), once a calendar year, and he teaches you: Fiddler's
 * Blessing (+2 Luck) for {@link #BLESSING_TICKS}, +{@link #MORALE} sanity and a Näcken's String; his music never pulls you
 * again.
 */
public class Nacken extends Spirit {
    static final double HEAR_RANGE = 20, PULL = 0.06, TAKEN = 2;
    static final int LISTEN_TICKS = 400, BLESSING_TICKS = 3 * 72000, MORALE = 30;
    /** A slow minor tune, as note-block pitches (0.5..2). */
    static final float[] TUNE = {0.71F, 0.84F, 0.94F, 1.06F, 0.94F, 0.84F, 0.79F, 0.71F, 0.63F, 0.71F, 0.84F, 0.71F};
    final Map<UUID, Integer> listening = new HashMap<>();
    final Set<UUID> taught = new HashSet<>();

    public Nacken(EntityType<? extends Nacken> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    @Override
    public float sight() {
        return Faith.NACKEN_SIGHT;
    }

    @Override
    String kind() {
        return "nacken";
    }

    /** In water by the rapids (flowing water close by) on a moonlit night; rare. */
    static boolean canSpawn(LevelAccessor level, BlockPos pos, RandomSource random) {
        return random.nextInt(8) == 0 && level instanceof ServerLevelAccessor s && rule(s.getLevel().isDarkOutside(), s.getLevel().isRaining(), rapids(level, pos));
    }

    static boolean rule(boolean night, boolean rain, boolean rapids) {
        return night && !rain && rapids;
    }

    /** Water here and flowing (not still) water within 3 blocks. */
    static boolean rapids(LevelReader level, BlockPos pos) {
        if (!level.getFluidState(pos).is(FluidTags.WATER)) return false;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-3, -1, -3), pos.offset(3, 1, 3))) {
            var f = level.getFluidState(p);
            if (f.is(FluidTags.WATER) && !f.isSource()) return true;
        }
        return false;
    }

    @Override
    public boolean checkSpawnObstruction(LevelReader level) {
        return level.isUnobstructed(this);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        setDeltaMovement(Vec3.ZERO);
        if (!(level() instanceof ServerLevel level)) return;
        if (level.isBrightOutside() || level.isRaining()) {
            vanish();
            return;
        }
        if (tickCount % 10 == 0) level.playSound(null, blockPosition(), SoundEvents.NOTE_BLOCK_GUITAR.value(), SoundSource.HOSTILE, 1.2F, TUNE[(tickCount / 10) % TUNE.length]);
        if (tickCount % 20 == 0) level.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 0.3, getZ(), 4, 0.6, 0.05, 0.6, 0);
        for (ServerPlayer p : level.getPlayers(p -> p.distanceTo(this) < HEAR_RANGE && !p.isSpectator() && !p.isCreative())) {
            if (listen(p)) {
                vanish();
                return;
            }
        }
        listening.keySet().removeIf(id -> !(level.getPlayerByUUID(id) instanceof Player p) || p.distanceTo(this) >= HEAR_RANGE);
    }

    /** One tick of listening; after {@link #LISTEN_TICKS} the music pulls. True when the player is taken into the water by him. */
    boolean listen(ServerPlayer p) {
        if (taught.contains(p.getUUID())) return false;
        int t = listening.merge(p.getUUID(), 1, Integer::sum);
        if (t == 1) {
            p.sendOverlayMessage(Component.translatable("message.mechanical.nacken.fiddle").withStyle(ChatFormatting.DARK_AQUA));
            Faith.award(p, "folklore/heard_the_fiddle");
        }
        if (t < LISTEN_TICKS) return false;
        if (p.isInWater() && p.distanceTo(this) < TAKEN) return true;
        Vec3 pull = position().subtract(p.position()).normalize().scale(PULL);
        p.push(pull.x, 0, pull.z);
        return false;
    }

    /** A gift: once a calendar year per player he teaches (blessing, morale, the string). False if it isn't a gift or this year's is given. */
    boolean gift(ServerPlayer player, ItemStack stack) {
        if (!stack.is(Beings.NACKEN_GIFTS)) return false;
        String key = "mechanical_nacken_year";
        long year = Calendar.year(level().getOverworldClockTime());
        if (player.getPersistentData().getLongOr(key, -1) >= year) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.nacken.again").withStyle(ChatFormatting.GRAY));
            return false;
        }
        player.getPersistentData().putLong(key, year);
        stack.shrink(1);
        taught.add(player.getUUID());
        listening.remove(player.getUUID());
        player.addEffect(new MobEffectInstance(Beings.FIDDLERS_BLESSING, BLESSING_TICKS, 0));
        Sanity.set(player, Sanity.of(player) + MORALE);
        player.getInventory().placeItemBackInInventory(Beings.NACKEN_STRING.toStack(), net.minecraft.util.Prediction.SERVER_ONLY);
        Faith.add(player, Faith.GIFT);
        Faith.award(player, "folklore/nacken_lesson");
        player.sendOverlayMessage(Component.translatable("message.mechanical.nacken.lesson").withStyle(ChatFormatting.AQUA));
        return true;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(Beings.NACKEN_GIFTS) || !seenBy(player)) return InteractionResult.PASS;
        if (player instanceof ServerPlayer sp) gift(sp, held);
        return InteractionResult.SUCCESS;
    }
}
