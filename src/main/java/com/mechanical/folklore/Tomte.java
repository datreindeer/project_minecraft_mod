package com.mechanical.folklore;

import com.mechanical.Calendar;
import com.mechanical.Farming;
import com.mechanical.Mechanical;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Vätten / Tomte: a little grey man who moves in where someone farms ({@link #MIN_FARMLAND}+ farmland within
 * {@link #FARM_RANGE} blocks of a player at night) and stays by that farm for good. Seen with sight {@link Faith#TOMTE_SIGHT}.
 * Give him a bowl of Porridge in the Yule month (Deepwinter, {@link #YULE}) and he is {@link Mood#PLEASED} until the next
 * Yule: crops round his farm grow faster, and he leaves a Straw Yule Goat. Let a Yule go by without porridge, or strike
 * him, and he is {@link Mood#ANGRY}: the farm's animals get pinched and hurt. Porridge outside Yule he leaves alone.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public class Tomte extends Spirit {
    /** Deepwinter: the Yule month. */
    public static final int YULE = 9;
    static final int FARM_RANGE = 8, MIN_FARMLAND = 8, HOME_RADIUS = 12, ANGRY_DAYS = 30, GROW_TICKS = 100, PINCH_TICKS = 600;
    static final double TOMTE_RANGE = 48;

    public enum Mood { CONTENT, PLEASED, ANGRY }

    /** Calendar year of the last Yule porridge (-1 never), the last Yule before he came, and the day a blow stops stinging. */
    long fedYear = -1, bornYule = Long.MIN_VALUE, angryUntil = -1;

    public Tomte(EntityType<? extends Tomte> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new MoveTowardsRestrictionGoal(this, 0.8));
        goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8));
    }

    @Override
    public float sight() {
        return Faith.TOMTE_SIGHT;
    }

    @Override
    String kind() {
        return "tomte";
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }

    /** The last Yule that is over: this year's once Deepwinter has passed, else last year's. */
    static long dueYule(int month, long year) {
        return month > YULE ? year : year - 1;
    }

    /** Pure: his mood from the calendar and what he remembers. */
    static Mood mood(int month, long year, long day, long fedYear, long bornYule, long angryUntil) {
        long due = dueYule(month, year);
        if (day < angryUntil || due > bornYule && fedYear < due) return Mood.ANGRY;
        return fedYear >= due && fedYear >= 0 ? Mood.PLEASED : Mood.CONTENT;
    }

    Mood mood() {
        long clock = level().getOverworldClockTime();
        return mood(Calendar.month(clock), Calendar.year(clock), Calendar.day(clock), fedYear, bornYule, angryUntil);
    }

    /** Porridge offered (month and year from the calendar): eaten only in Yule. True if he took it. */
    boolean feed(ServerPlayer player, ItemStack stack, int month, long year) {
        if (!stack.is(Farming.PORRIDGE.get())) return false;
        if (month != YULE) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.tomte.not_yule").withStyle(ChatFormatting.GRAY));
            return false;
        }
        boolean first = fedYear < year;
        fedYear = year;
        angryUntil = -1;
        stack.shrink(1);
        player.getInventory().placeItemBackInInventory(new ItemStack(Items.BOWL), net.minecraft.util.Prediction.SERVER_ONLY);
        if (first) {
            player.getInventory().placeItemBackInInventory(Beings.STRAW_YULE_GOAT.toStack(), net.minecraft.util.Prediction.SERVER_ONLY);
            Faith.add(player, Faith.GIFT);
        }
        Faith.award(player, "folklore/yule_porridge");
        player.sendOverlayMessage(Component.translatable("message.mechanical.tomte.fed").withStyle(ChatFormatting.GOLD));
        if (level() instanceof ServerLevel level) level.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 0.8, getZ(), 6, 0.3, 0.3, 0.3, 0);
        return true;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(Farming.PORRIDGE.get()) || !seenBy(player)) return InteractionResult.PASS;
        if (player instanceof ServerPlayer sp) {
            long clock = level().getOverworldClockTime();
            feed(sp, held, Calendar.month(clock), Calendar.year(clock));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    void struck(ServerLevel level, ServerPlayer player) {
        angryUntil = Calendar.day(level.getOverworldClockTime()) + ANGRY_DAYS;
        player.sendOverlayMessage(Component.translatable("message.mechanical.tomte.angry").withStyle(ChatFormatting.RED));
        level.sendParticles(ParticleTypes.ANGRY_VILLAGER, getX(), getY() + 0.9, getZ(), 3, 0.2, 0.2, 0.2, 0);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel level) || !hasHome()) return;
        Mood m = mood();
        if (m == Mood.PLEASED && tickCount % GROW_TICKS == 0) tendCrop(level);
        else if (m == Mood.ANGRY && tickCount % PINCH_TICKS == 0) pinch(level);
    }

    /** One of the farm's unripe crops grows a step. */
    boolean tendCrop(ServerLevel level) {
        BlockPos home = getHomePosition();
        java.util.List<BlockPos> unripe = new java.util.ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(home.offset(-HOME_RADIUS, -2, -HOME_RADIUS), home.offset(HOME_RADIUS, 2, HOME_RADIUS))) {
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof CropBlock crop && !crop.isMaxAge(s)) unripe.add(p.immutable());
        }
        if (unripe.isEmpty()) return false;
        BlockPos p = unripe.get(random.nextInt(unripe.size()));
        BlockState s = level.getBlockState(p);
        CropBlock crop = (CropBlock) s.getBlock();
        level.setBlockAndUpdate(p, crop.getStateForAge(crop.getAge(s) + 1));
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 2, 0.2, 0.2, 0.2, 0);
        return true;
    }

    /** One of the farm's animals gets pinched. */
    boolean pinch(ServerLevel level) {
        var animals = level.getEntitiesOfClass(Animal.class, new AABB(getHomePosition()).inflate(HOME_RADIUS));
        if (animals.isEmpty()) return false;
        Animal a = animals.get(random.nextInt(animals.size()));
        a.hurtServer(level, level.damageSources().magic(), 1.0F);
        level.sendParticles(ParticleTypes.ANGRY_VILLAGER, a.getX(), a.getY() + a.getBbHeight(), a.getZ(), 1, 0, 0, 0, 0);
        return true;
    }

    /** Settles at home: remembers the last Yule before he came, so that one is not held against anyone. */
    void settle(BlockPos home) {
        setHomeTo(home, HOME_RADIUS);
        long clock = level().getOverworldClockTime();
        bornYule = dueYule(Calendar.month(clock), Calendar.year(clock));
    }

    /** Once a minute at night: a farmer without a tomte nearby may get one. */
    @SubscribeEvent
    static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 1200 != 0 || player.isSpectator()) return;
        ServerLevel level = player.level();
        if (!level.isDarkOutside() || level.getRandom().nextInt(3) != 0) return;
        if (!level.getEntitiesOfClass(Tomte.class, player.getBoundingBox().inflate(TOMTE_RANGE)).isEmpty()) return;
        BlockPos farm = farm(level, player.blockPosition());
        if (farm != null) spawnAt(level, farm);
    }

    /** The middle of the farmland near pos, if there are {@link #MIN_FARMLAND}+ blocks of it; else null. */
    static BlockPos farm(ServerLevel level, BlockPos pos) {
        long x = 0, y = 0, z = 0;
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-FARM_RANGE, -2, -FARM_RANGE), pos.offset(FARM_RANGE, 2, FARM_RANGE))) {
            if (!level.getBlockState(p).is(Blocks.FARMLAND)) continue;
            x += p.getX();
            y += p.getY();
            z += p.getZ();
            n++;
        }
        return n < MIN_FARMLAND ? null : new BlockPos((int) (x / n), (int) (y / n) + 1, (int) (z / n));
    }

    static Tomte spawnAt(ServerLevel level, BlockPos farm) {
        Tomte t = Beings.TOMTE.get().create(level, EntitySpawnReason.EVENT);
        if (t == null) return null;
        BlockPos at = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, farm);
        t.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.getRandom().nextFloat() * 360, 0);
        t.settle(farm);
        level.addFreshEntity(t);
        return t;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putLong("fed_year", fedYear);
        output.putLong("born_yule", bornYule);
        output.putLong("angry_until", angryUntil);
        if (hasHome()) output.putLong("home", getHomePosition().asLong());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        fedYear = input.getLongOr("fed_year", -1);
        bornYule = input.getLongOr("born_yule", Long.MIN_VALUE);
        angryUntil = input.getLongOr("angry_until", -1);
        input.getLong("home").ifPresent(h -> setHomeTo(BlockPos.of(h), HOME_RADIUS));
    }

    static void selfCheck() {
        // a tomte that came in summer of year 2: content, then angry once the Yule of year 2 passed unfed
        check(mood(4, 2, 0, -1, 1, -1) == Mood.CONTENT, "new tomte is content");
        check(mood(YULE, 2, 0, -1, 1, -1) == Mood.CONTENT, "during Yule there is still time");
        check(mood(YULE + 1, 2, 0, -1, 1, -1) == Mood.ANGRY, "a Yule without porridge angers him");
        check(mood(YULE, 2, 0, 2, 1, -1) == Mood.PLEASED && mood(3, 3, 0, 2, 1, -1) == Mood.PLEASED, "fed at Yule: pleased till the next");
        check(mood(YULE + 1, 3, 0, 2, 1, -1) == Mood.ANGRY, "and angry when the next one passes unfed");
        check(mood(3, 3, 10, 2, 1, 20) == Mood.ANGRY, "a blow stings for a while");
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new IllegalStateException("Tomte self-check failed: " + what);
    }
}
