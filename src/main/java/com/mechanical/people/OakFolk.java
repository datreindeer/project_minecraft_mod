package com.mechanical.people;

import static com.mechanical.Mechanical.ITEMS;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import com.mechanical.AncientOak;
import com.mechanical.KnifeItem;
import com.mechanical.Mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * The Oak Folk (docs/living-world-plan.md F4): a small people in green who live round every Ancient Oak. Gatherers pick
 * berries and mushrooms and plant new ones round the tree, worshippers sit and pray to it, light-bearers walk the
 * clearing with lit candles, and their keeper, crowned with giant deer antlers, carries the Verdant Staff and gives out
 * the quest line (OakQuests). They are one tribe ({@link Tribe#OAK}) with a standing like any other: striking the tree or
 * tearing up its clearing lowers it, and at hostile the quest line closes for good and the keeper's staff turns on you.
 * The group is spawned once per oak by its Offering Altar ({@link #spawnGroup}). Their day: OakFolkGoal.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class OakFolk {
    /** Standing lost for striking the tree (at most once every {@link #HARM_COOLDOWN} ticks) or breaking a plant of its clearing. */
    public static final int HARM_TREE = -4, HARM_PLANT = -2, HARM_COOLDOWN = 40, CLEARING_R = 28;
    /** Who lives at each oak. */
    static final TribeRole[] GROUP = {TribeRole.OAK_KEEPER, TribeRole.OAK_GATHERER, TribeRole.OAK_GATHERER, TribeRole.OAK_GATHERER,
            TribeRole.OAK_WORSHIPPER, TribeRole.OAK_WORSHIPPER, TribeRole.OAK_WORSHIPPER, TribeRole.OAK_LIGHTBEARER, TribeRole.OAK_LIGHTBEARER};

    // ---- items ----

    /** The keeper's staff, given as a quest reward: right-click a plant to make it grow (5 s cooldown); it glows when held. */
    public static final DeferredItem<VerdantStaff> VERDANT_STAFF = ITEMS.registerItem("verdant_staff", VerdantStaff::new,
            p -> p.stacksTo(1).rarity(Rarity.EPIC));
    /** The last quest's reward: the best knife, the only one at {@link KnifeItem#RITUAL} (fine parts: Butchery#dropRitual). */
    public static final DeferredItem<KnifeItem> ANCIENT_RITUAL_KNIFE = ITEMS.registerItem("ancient_ritual_knife",
            p -> new KnifeItem(p, 3.0F, 2.0F, KnifeItem.RITUAL, 2.0F),
            p -> p.sword(com.mechanical.Smithing.STEEL, 2.0F, -2.0F).durability(3000).rarity(Rarity.EPIC));
    /** Fine parts only the ritual knife takes. */
    public static final DeferredItem<Item> ANIMAL_HEART = ITEMS.registerSimpleItem("animal_heart",
            p -> p.food(new FoodProperties.Builder().nutrition(5).saturationModifier(0.8F).build()));
    public static final DeferredItem<Item> FINE_SINEW = ITEMS.registerSimpleItem("fine_sinew");
    public static final DeferredItem<Item> TROPHY_SKULL = ITEMS.registerSimpleItem("trophy_skull", p -> p.rarity(Rarity.UNCOMMON));

    public static void init() {
        // the giant deer and the bear give everything to the ritual knife; the rest go by body size (Butchery#ritualParts)
        for (var big : List.of(com.mechanical.wildlife.Wildlife.GIANT_DEER, com.mechanical.wildlife.Wildlife.BROWN_BEAR))
            com.mechanical.Butchery.registerRitual(big, List.of(new com.mechanical.Butchery.Extra(ANIMAL_HEART, 1.0F, 1, 1),
                    new com.mechanical.Butchery.Extra(FINE_SINEW, 1.0F, 2, 3), new com.mechanical.Butchery.Extra(TROPHY_SKULL, 1.0F, 1, 1)));
    }

    /** DynamicLights: the light-bearers' candles and the Verdant Staff shine. */
    public static int lightOf(net.minecraft.world.entity.Entity e) {
        if (e instanceof Tribesperson t && t.role() == TribeRole.OAK_LIGHTBEARER) return 12;
        return 0;
    }

    // ---- the group ----

    /** Spawns the oak's group round the altar at {@code foot} (once, from OfferingAltarBlockEntity). Returns them. */
    public static List<Tribesperson> spawnGroup(ServerLevel level, BlockPos foot, BlockPos entrance) {
        List<Tribesperson> out = new java.util.ArrayList<>();
        double door = entrance != null ? Math.atan2(entrance.getZ() - foot.getZ(), entrance.getX() - foot.getX()) : 0;
        for (int i = 0; i < GROUP.length; i++) {
            TribeRole role = GROUP[i];
            // the keeper waits outside the door, the rest spread round the trunk
            double a = role == TribeRole.OAK_KEEPER ? door : door + 0.5 + i * (Math.PI * 2 - 1.0) / GROUP.length;
            double r = role == TribeRole.OAK_KEEPER ? 11 : 13 + (i % 3) * 2;
            BlockPos at = ground(level, BlockPos.containing(foot.getX() + 0.5 + Math.cos(a) * r, foot.getY(), foot.getZ() + 0.5 + Math.sin(a) * r));
            Tribesperson t = People.TRIBESPERSON.get().create(level, EntitySpawnReason.STRUCTURE);
            if (t == null) continue;
            t.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.getRandom().nextFloat() * 360, 0);
            for (int tries = 0; tries < 6 && (tries == 0 || out.stream().anyMatch(o -> o.tribeName().equals(t.tribeName()))); tries++)
                t.setup(role, null, Tribe.OAK, role == TribeRole.OAK_KEEPER, ClimateZone.TEMPERATE); // a name of their own
            t.oakFoot = foot.immutable();
            t.setPersistenceRequired();
            level.addFreshEntity(t);
            out.add(t);
        }
        return out;
    }

    /** The first standable spot in the column at {@code p}, looking 10 up and 10 down from its y (under the crown, over the roots). */
    static BlockPos ground(Level level, BlockPos p) {
        for (int dy = 10; dy >= -10; dy--) {
            BlockPos at = p.above(dy);
            BlockState here = level.getBlockState(at), below = level.getBlockState(at.below());
            if (!here.isSolid() && here.getFluidState().isEmpty() && !level.getBlockState(at.above()).isSolid()
                    && below.isFaceSturdy(level, at.below(), Direction.UP)) return at;
        }
        return p;
    }

    // ---- every tick of an Oak Folk person (Tribesperson.customServerAiStep, instead of CampLife) ----

    private static final Map<Tribesperson, Long> NEXT_SPELL = new WeakHashMap<>();

    static void tick(Tribesperson mob, ServerLevel level) {
        if (mob.role() == TribeRole.OAK_LIGHTBEARER && mob.idle() == 0) mob.setIdle(CampLife.CANDLES);
        if (mob.role() != TribeRole.OAK_KEEPER) return;
        // the staff: green motes and a glow round its head
        if (mob.tickCount % 5 == 0) {
            Vec3 tip = staffTip(mob);
            level.sendParticles(new DustParticleOptions(0x6FE36A, 0.9F), tip.x, tip.y, tip.z, 2, 0.12, 0.12, 0.12, 0);
            if (mob.tickCount % 20 == 0) level.sendParticles(ParticleTypes.HAPPY_VILLAGER, tip.x, tip.y, tip.z, 1, 0.2, 0.2, 0.2, 0);
        }
        // turned against a player: the staff's magic (no weapon) slows, weakens and stings them now and then
        if (mob.tickCount % 20 != 0) return;
        Player target = level.getNearestPlayer(mob.getX(), mob.getY(), mob.getZ(), 16, p -> p instanceof Player pl && mob.isAngryAt(pl) && mob.hasLineOfSight(pl));
        if (target == null || NEXT_SPELL.getOrDefault(mob, 0L) > level.getGameTime()) return;
        NEXT_SPELL.put(mob, level.getGameTime() + 60);
        mob.setIdle(CampLife.STAFF);
        mob.getLookControl().setLookAt(target);
        Vec3 from = staffTip(mob), to = target.getEyePosition();
        for (int i = 0; i <= 12; i++) {
            Vec3 p = from.lerp(to, i / 12.0);
            level.sendParticles(new DustParticleOptions(0x4FD34A, 1.2F), p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0);
        }
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 1));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
        target.hurtServer(level, level.damageSources().indirectMagic(mob, mob), 2.0F);
        level.playSound(null, mob.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 0.8F, 1.3F);
    }

    /** About where the head of the staff in the keeper's right hand is. */
    private static Vec3 staffTip(Tribesperson mob) {
        float yaw = mob.yBodyRot * ((float) Math.PI / 180F);
        return new Vec3(mob.getX() - Math.cos(yaw) * 0.4 - Math.sin(yaw) * 0.3, mob.getY() + 2.15, mob.getZ() - Math.sin(yaw) * 0.4 + Math.cos(yaw) * 0.3);
    }

    // ---- harm to the tree and its clearing ----

    private static final Map<Player, Long> LAST_HARM = new WeakHashMap<>();

    static boolean isTree(BlockState s) {
        Block b = s.getBlock();
        return b instanceof AncientOak.Log || b instanceof AncientOak.Branch || b instanceof AncientOak.Leaves || b == AncientOak.ALTAR.get();
    }

    static boolean isClearingPlant(BlockState s) {
        Block b = s.getBlock();
        return b == AncientOak.GLOWCAP.get() || b == AncientOak.SPIRIT_BLOOM.get() || b == AncientOak.GLOW_MOSS.get();
    }

    /** Is any of the Oak Folk within {@code r} of {@code pos}? (They see what is done to their tree.) */
    static List<Tribesperson> folkNear(Level level, BlockPos pos, double r) {
        return level.getEntitiesOfClass(Tribesperson.class, new AABB(pos).inflate(r), t -> t.isAlive() && t.role().oak());
    }

    /** Someone struck or broke at the tree, or tore up its clearing: the Oak Folk who saw it think less of them. */
    public static void harmed(Player player, BlockPos pos, int delta) {
        if (player.isCreative() || player.isSpectator()) return;
        List<Tribesperson> folk = folkNear(player.level(), pos, CLEARING_R + 8);
        if (folk.isEmpty()) return;
        Tribe.of(Tribe.OAK).addReputation(player, delta);
        folk.stream().min((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player)))
                .ifPresent(t -> t.say(player, Component.translatable("chat.mechanical.oak_folk.harm")));
    }

    @SubscribeEvent
    static void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) return;
        if (!isTree(player.level().getBlockState(event.getPos()))) return;
        long now = player.level().getGameTime();
        if (LAST_HARM.getOrDefault(player, -1000L) + HARM_COOLDOWN > now) return;
        LAST_HARM.put(player, now);
        harmed(player, event.getPos(), HARM_TREE);
    }

    @SubscribeEvent
    static void onBreak(BreakBlockEvent event) {
        if (event.getLevel().isClientSide() || !isClearingPlant(event.getState())) return;
        harmed(event.getPlayer(), event.getPos(), HARM_PLANT);
    }

    /** Right-click a plant: it grows as if bone-mealed, with a puff of green; 5 s between. */
    public static class VerdantStaff extends Item {
        public VerdantStaff(Properties p) {
            super(p);
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            Level level = context.getLevel();
            Player player = context.getPlayer();
            if (player == null || player.getCooldowns().isOnCooldown(context.getItemInHand())) return InteractionResult.PASS;
            BlockPos pos = context.getClickedPos();
            if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
            if (!BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), level, pos)) return InteractionResult.PASS;
            server.sendParticles(new DustParticleOptions(0x6FE36A, 1.0F), pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 12, 0.35, 0.3, 0.35, 0);
            server.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.2F);
            player.getCooldowns().addCooldown(context.getItemInHand(), 100);
            return InteractionResult.SUCCESS;
        }
    }

    private OakFolk() {}
}
