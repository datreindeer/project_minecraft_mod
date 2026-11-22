package com.mechanical;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ITEMS;
import static com.mechanical.Mechanical.MODID;

import java.util.EnumSet;
import java.util.function.Supplier;

import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.AbstractCow;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.SkeletonHorse;
import net.minecraft.world.entity.animal.equine.ZombieHorse;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Living-world plan D6 (and the grazing of A10): livestock (cows, sheep, pigs, chickens, goats and tamed horses) need food
 * and water. Kept as two "lasts until" game times, so nothing counts down: food lasts {@link #FOOD_TICKS}, a drink
 * {@link #WATER_TICKS}. Run out of either and the animal doesn't die but is tired: it walks at half speed, droops its head
 * (client: LivestockPose) and won't breed. Grazers (cows, sheep, goats, horses) standing on grass eat it when hungry,
 * now and then leaving bare dirt (grass grows back over it); the rest eat from a {@link FeedingTroughBlock} or the hand.
 * Water only comes from a trough. Separate from a horse's stamina (its own attachment, WagonEntity's business).
 */
@EventBusSubscriber(modid = MODID)
public final class Husbandry {
    /** A meal lasts a day, a drink two; below half of that the animal looks for more. */
    public static final long FOOD_TICKS = 24000, WATER_TICKS = 48000;
    /** Head down at the grass or the trough. */
    public static final int EAT_TICKS = 40;
    /** Hungry grazers on grass: chance per check (every {@link #CHECK} ticks) to graze; chance a graze bares the ground. */
    static final int GRAZE_ONE_IN = 3, BARE_ONE_IN = 3, CHECK = 20;
    /** How far animals look for a trough. */
    static final int TROUGH_RANGE = 12;
    static final Identifier TIRED_ID = Identifier.fromNamespaceAndPath(MODID, "tired");
    private static final AttributeModifier TIRED = new AttributeModifier(TIRED_ID, -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    /** fed / watered: game time the last meal / drink lasts until (-1: never seen, counts as just fed); eating: head down until. */
    public record Needs(long fed, long watered, long eating) {
        static final Needs UNSET = new Needs(-1, -1, 0);
        static final com.mojang.serialization.MapCodec<Needs> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                com.mojang.serialization.Codec.LONG.fieldOf("fed").forGetter(Needs::fed),
                com.mojang.serialization.Codec.LONG.fieldOf("watered").forGetter(Needs::watered),
                com.mojang.serialization.Codec.LONG.optionalFieldOf("eating", 0L).forGetter(Needs::eating)).apply(i, Needs::new));
        static final StreamCodec<io.netty.buffer.ByteBuf, Needs> STREAM = StreamCodec.composite(ByteBufCodecs.VAR_LONG, Needs::fed,
                ByteBufCodecs.VAR_LONG, Needs::watered, ByteBufCodecs.VAR_LONG, Needs::eating, Needs::new);

        public boolean tired(long now) {
            return fed >= 0 && (now >= fed || now >= watered);
        }

        public boolean hungry(long now) {
            return fed - now < FOOD_TICKS / 2;
        }

        public boolean thirsty(long now) {
            return watered - now < WATER_TICKS / 2;
        }

        public boolean eating(long now) {
            return now < eating;
        }
    }

    /** Synced so the client can draw the tired and eating poses. */
    public static final Supplier<AttachmentType<Needs>> NEEDS = Mechanical.ATTACHMENTS.register("livestock_needs", () -> AttachmentType
            .builder(() -> Needs.UNSET)
            .serialize(Needs.CODEC)
            .sync(Needs.STREAM)
            .build());

    public static final DeferredBlock<FeedingTroughBlock> FEEDING_TROUGH = BLOCKS.registerBlock("feeding_trough", FeedingTroughBlock::new, p -> p
            .mapColor(MapColor.WOOD).strength(1.5F).sound(SoundType.WOOD).noOcclusion().ignitedByLava());
    public static final DeferredItem<BlockItem> FEEDING_TROUGH_ITEM = ITEMS.registerSimpleBlockItem("feeding_trough", FEEDING_TROUGH);
    public static final Supplier<BlockEntityType<FeedingTroughBlockEntity>> FEEDING_TROUGH_ENTITY = Mechanical.BLOCK_ENTITIES.register("feeding_trough",
            () -> new BlockEntityType<>(FeedingTroughBlockEntity::new, FEEDING_TROUGH.get()));

    public static void init() {}

    /** Kept animals: the farm animals and tamed horses (the mod's wild game is not). */
    public static boolean livestock(Entity e) {
        return e instanceof AbstractCow || e instanceof Sheep || e instanceof Pig || e instanceof Chicken || e instanceof Goat
                || e instanceof AbstractHorse h && h.isTamed() && !(h instanceof SkeletonHorse) && !(h instanceof ZombieHorse);
    }

    /** Livestock that eat grass. */
    public static boolean grazer(Entity e) {
        return e instanceof AbstractCow || e instanceof Sheep || e instanceof Goat || e instanceof AbstractHorse;
    }

    /** Can some livestock eat this (what a trough takes)? */
    public static boolean fodder(ItemStack s) {
        return s.is(ItemTags.COW_FOOD) || s.is(ItemTags.SHEEP_FOOD) || s.is(ItemTags.PIG_FOOD) || s.is(ItemTags.CHICKEN_FOOD)
                || s.is(ItemTags.GOAT_FOOD) || s.is(ItemTags.HORSE_FOOD) || s.is(ItemTags.LLAMA_FOOD);
    }

    /** The animal's needs; the first time it is seen it counts as just fed and watered. */
    public static Needs needs(Animal a) {
        Needs n = a.getData(NEEDS);
        if (n.fed() < 0 && !a.level().isClientSide()) {
            long now = a.level().getGameTime();
            n = new Needs(now + FOOD_TICKS, now + WATER_TICKS, 0);
            a.setData(NEEDS, n);
        }
        return n;
    }

    public static boolean tired(Animal a) {
        return livestock(a) && needs(a).tired(a.level().getGameTime());
    }

    /** Server: a meal (food) or a drink (!food), head down for {@link #EAT_TICKS}. */
    public static void feed(Animal a, boolean food) {
        long now = a.level().getGameTime();
        Needs n = needs(a);
        a.setData(NEEDS, food ? new Needs(now + FOOD_TICKS, n.watered(), now + EAT_TICKS) : new Needs(n.fed(), now + WATER_TICKS, now + EAT_TICKS));
        a.getNavigation().stop();
        a.level().playSound(null, a.blockPosition(), food ? SoundEvents.GENERIC_EAT.value() : SoundEvents.GENERIC_DRINK.value(), SoundSource.NEUTRAL, 0.6F, 0.8F);
    }

    /** Test/command use: set both needs to run out at these game times. */
    public static void setNeeds(Animal a, long fed, long watered) {
        a.setData(NEEDS, new Needs(fed, watered, 0));
    }

    @SubscribeEvent
    static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Animal a) || a.level().isClientSide() || (a.tickCount + a.getId()) % CHECK != 0 || !livestock(a)) return;
        update(a);
    }

    /** Server, every {@link #CHECK} ticks: tired or not, and graze when hungry on grass. */
    static void update(Animal a) {
        long now = a.level().getGameTime();
        Needs n = needs(a);
        boolean tired = n.tired(now);
        AttributeInstance speed = a.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && tired != speed.hasModifier(TIRED_ID)) {
            if (tired) speed.addTransientModifier(TIRED);
            else speed.removeModifier(TIRED_ID);
        }
        if (tired && a.isInLove()) a.resetLove();
        if (grazer(a) && n.hungry(now) && !n.eating(now) && a.onGround() && !a.isVehicle() && a.getRandom().nextInt(GRAZE_ONE_IN) == 0) graze(a);
    }

    /** Eats the grass it stands on, if any; now and then that leaves bare dirt. */
    static boolean graze(Animal a) {
        ServerLevel level = (ServerLevel) a.level();
        BlockPos on = a.getOnPos();
        if (!level.getBlockState(on).is(Blocks.GRASS_BLOCK)) return false;
        feed(a, true);
        if (a.getRandom().nextInt(BARE_ONE_IN) == 0) bare(level, on);
        return true;
    }

    /** Grass eaten down to the dirt (vanilla grass spreads back over it). */
    public static void bare(ServerLevel level, BlockPos pos) {
        level.levelEvent(2001, pos, Block.getId(Blocks.GRASS_BLOCK.defaultBlockState()));
        level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
    }

    /** No foal, calf or lamb from a tired parent. */
    @SubscribeEvent
    static void onBaby(BabyEntitySpawnEvent event) {
        if (event.getParentA() instanceof Animal a && tired(a) || event.getParentB() instanceof Animal b && tired(b)) event.setCanceled(true);
    }

    /** A hungry animal eats what it likes from the hand (instead of going into love). */
    @SubscribeEvent
    static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Animal a) || !livestock(a)) return;
        ItemStack stack = event.getItemStack();
        if (!a.isFood(stack) || a.isBaby() || !needs(a).hungry(a.level().getGameTime())) return;
        if (!a.level().isClientSide()) {
            stack.consume(1, event.getEntity());
            feed(a, true);
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    @SubscribeEvent
    static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Animal a && livestock(a)) a.goalSelector.addGoal(2, new TroughGoal(a));
    }

    /** The nearest trough within range whose group has what the animal needs, or null. */
    static BlockPos findTrough(Animal a, boolean food, boolean water) {
        Level level = a.level();
        BlockPos at = a.blockPosition(), best = null;
        int cr = (TROUGH_RANGE >> 4) + 1;
        for (int cx = (at.getX() >> 4) - cr; cx <= (at.getX() >> 4) + cr; cx++) {
            for (int cz = (at.getZ() >> 4) - cr; cz <= (at.getZ() >> 4) + cr; cz++) {
                if (!level.hasChunk(cx, cz)) continue;
                for (var be : level.getChunk(cx, cz).getBlockEntities().values()) {
                    if (!(be instanceof FeedingTroughBlockEntity t)) continue;
                    BlockPos p = t.getBlockPos();
                    if (p.distSqr(at) > TROUGH_RANGE * TROUGH_RANGE || best != null && p.distSqr(at) >= best.distSqr(at)) continue;
                    if (water && t.water() > 0 || food && a.isFood(t.food())) best = p;
                }
            }
        }
        return best;
    }

    /** Walks to a trough with what it lacks, lowers its head and eats or drinks from the trough group. */
    static class TroughGoal extends Goal {
        private static final int GIVE_UP = 300;
        private final Animal a;
        private BlockPos trough;
        private int ticks, eating;

        TroughGoal(Animal a) {
            this.a = a;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if ((a.tickCount + a.getId()) % 40 != 0 || a.isVehicle() || a.isLeashed()) return false;
            long now = a.level().getGameTime();
            Needs n = needs(a);
            boolean food = n.hungry(now), water = n.thirsty(now);
            if (!food && !water) return false;
            trough = findTrough(a, food, water);
            return trough != null;
        }

        @Override
        public void start() {
            ticks = eating = 0;
            a.getNavigation().moveTo(a.getNavigation().createPath(trough, 1), 1.0);
        }

        @Override
        public boolean canContinueToUse() {
            return ticks < GIVE_UP && eating <= EAT_TICKS && a.level().getBlockEntity(trough) instanceof FeedingTroughBlockEntity && !a.isVehicle();
        }

        @Override
        public void stop() {
            a.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            ticks++;
            a.getLookControl().setLookAt(Vec3.atCenterOf(trough));
            if (!new AABB(trough).inflate(a.getBbWidth() / 2 + 0.8, 0.5, a.getBbWidth() / 2 + 0.8).contains(a.position())) {
                if (a.getNavigation().isDone()) a.getNavigation().moveTo(a.getNavigation().createPath(trough, 1), 1.0);
                return;
            }
            a.getNavigation().stop();
            if (eating++ == 0) {
                // head down now, the meal counts when it is done
                Needs n = needs(a);
                a.setData(NEEDS, new Needs(n.fed(), n.watered(), a.level().getGameTime() + EAT_TICKS));
            }
            if (eating == EAT_TICKS && a.level() instanceof ServerLevel level) {
                long now = level.getGameTime();
                Needs n = needs(a);
                if (n.thirsty(now) && FeedingTroughBlock.drink(level, trough)) feed(a, false);
                n = needs(a);
                if (n.hungry(now) && FeedingTroughBlock.eat(level, trough, a)) feed(a, true);
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.HAY_BLOCK.defaultBlockState()), trough.getX() + 0.5, trough.getY() + 0.6, trough.getZ() + 0.5, 4, 0.2, 0.05, 0.2, 0.0);
            }
        }
    }

    private Husbandry() {}
}
