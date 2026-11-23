package com.mechanical.folklore;

import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

/**
 * Skogsrået / Huldra: a fair woman at the forest edge at night, with a hollow back like a rotten tree. She stays ahead of
 * a player who can see her ({@link Faith#HULDRA_SIGHT}), drawing them off the path into the trees; after
 * {@link #LURE_TICKS} she waits and holds out her gift. Right-click her then to take it: a Huldra Comb and Luck for
 * {@link #LUCK_TICKS}. Walk away from the offered gift ({@link #REFUSE_RANGE} blocks) or strike her and you are Lost
 * for {@link #LOST_TICKS}. Gone by day.
 */
public class Huldra extends Spirit {
    static final int LURE_TICKS = 600, LUCK_TICKS = 20 * 60 * 20, LOST_TICKS = 20 * 180;
    static final double OFFER_RANGE = 4, LURE_RANGE = 24, REFUSE_RANGE = 12;
    private int lure;
    private UUID offeredTo;

    public Huldra(EntityType<? extends Huldra> type, Level level) {
        super(type, level);
        setItemInHand(InteractionHand.MAIN_HAND, Beings.HULDRA_COMB.toStack());
        setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND, 0);
    }

    @Override
    public float sight() {
        return Faith.HULDRA_SIGHT;
    }

    @Override
    String kind() {
        return "huldra";
    }

    /** Night, open sky, on forest soil; rare. */
    static boolean canSpawn(LevelAccessor level, BlockPos pos, RandomSource random) {
        return random.nextInt(6) == 0 && rule(level instanceof ServerLevelAccessor s && s.getLevel().isDarkOutside(), level.getBlockState(pos.below()).is(BlockTags.DIRT), level.canSeeSky(pos));
    }

    static boolean rule(boolean night, boolean soil, boolean sky) {
        return night && soil && sky;
    }

    boolean offering() {
        return lure >= LURE_TICKS;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel level) || tickCount % 5 != 0) return;
        if (offeredTo != null && level.getPlayerByUUID(offeredTo) instanceof ServerPlayer p && walkedAway(p)) {
            refused(p);
            return;
        }
        if (level.isBrightOutside()) {
            vanish();
            return;
        }
        Player p = nearestSeer(LURE_RANGE);
        if (p == null) return;
        getLookControl().setLookAt(p);
        if (offering()) {
            getNavigation().stop();
            if (p.distanceTo(this) < OFFER_RANGE) offerTo(p);
            return;
        }
        lure += 5;
        if (p.distanceTo(this) < 10) { // stays ahead, deeper among the trees
            Vec3 away = position().subtract(p.position()).horizontal().normalize().scale(8);
            getNavigation().moveTo(getX() + away.x, getY(), getZ() + away.z, 0.9);
        }
    }

    /** The player she held her gift out to has gone {@link #REFUSE_RANGE}+ blocks away. */
    boolean walkedAway(Player p) {
        return p.getUUID().equals(offeredTo) && p.distanceTo(this) > REFUSE_RANGE;
    }

    void offerTo(Player p) {
        offeredTo = p.getUUID();
    }

    /** Takes her gift: the comb and luck. Called when a player who sees her right-clicks while she offers. */
    boolean accept(ServerPlayer player) {
        if (!offering() || !seenBy(player)) return false;
        player.getInventory().placeItemBackInInventory(Beings.HULDRA_COMB.toStack(), net.minecraft.util.Prediction.SERVER_ONLY);
        player.addEffect(new MobEffectInstance(MobEffects.LUCK, LUCK_TICKS, 0));
        player.sendOverlayMessage(Component.translatable("message.mechanical.huldra.accept").withStyle(ChatFormatting.GREEN));
        Faith.add(player, Faith.GIFT);
        Faith.award(player, "folklore/huldra_gift");
        vanish();
        return true;
    }

    void refused(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(Beings.LOST, LOST_TICKS, 0));
        player.sendOverlayMessage(Component.translatable("message.mechanical.huldra.refuse").withStyle(ChatFormatting.DARK_GREEN));
        vanish();
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!offering() || !seenBy(player)) return InteractionResult.PASS;
        if (player instanceof ServerPlayer sp) accept(sp);
        return InteractionResult.SUCCESS;
    }

    @Override
    void struck(ServerLevel level, ServerPlayer player) {
        refused(player);
    }

    /** For tests: as if she had led for long enough. */
    void skipLure() {
        lure = LURE_TICKS;
    }
}
