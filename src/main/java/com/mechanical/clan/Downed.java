package com.mechanical.clan;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.mechanical.CarcassHauling;
import com.mechanical.Mechanical;
import com.mechanical.people.Tribesperson;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Downed, helped up and waking (docs/living-world-plan.md E and its decisions). On a server with a tribe mate of yours
 * online, 0 health puts you down instead of killing you: you lie on the ground (forced SLEEPING pose, can't move),
 * hold nothing, use nothing, and after {@link #DOWN_TICKS} (or holding the downed key, R, for {@link #GIVE_UP_TICKS})
 * you die as usual. Mobs leave you alone; players and raiders finish you with a hit. Singleplayer, no tribe or no mate
 * online: you die at once. A mate sneak + holds right-click on you for {@link #HELP_TICKS} (a hit stops it), then
 * carries you on his shoulders (you ride him: mixin.EntityRideMixin), sneak + right-click puts you down, and holding R
 * where it is safe ({@link #safe}) for {@link #REVIVE_TICKS} revives you with {@link #REVIVE_HEALTH} and all your things.
 * The clock stops while you are carried.
 * <p>After every respawn (and a revive) you wake: lying down for {@link #WAKE_TICKS}, eyes opening, then you stand up
 * (client.DownedClient: third person, eyelids). Woken in a bed you lie in it; woken anywhere else (world spawn) you are a
 * spirit for {@link #SPIRIT_TICKS} or until you hit or use something: no mob targets you.
 * <p>State is kept per server run (not saved); leaving while downed is dying. Lang: scripts/gen_clans.py.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class Downed {
    public static final int DOWN_TICKS = 1200, GIVE_UP_TICKS = 30, HELP_TICKS = 60, REVIVE_TICKS = 60, WAKE_TICKS = 80, STAND_TICKS = 15,
            SPIRIT_TICKS = 2400, SAFE_HURT_TICKS = 200;
    /** Ticks without a right-click packet before a help counts as let go (holding the use key repeats it every 4). */
    static final int CLICK_GAP = 8;
    public static final double SAFE_RADIUS = 16, HELP_REACH = 4;
    public static final float REVIVE_HEALTH = 4.0F;
    /** Sync kinds. */
    public static final int NONE = 0, DOWN = 1, WAKING = 2;
    /** Key actions: R released, R pressed, put the carried one down (sneak + right-click in the air). */
    public static final int RELEASE = 0, HOLD = 1, PUT_DOWN = 2;
    static final Identifier REVIVE_MATE = Identifier.fromNamespaceAndPath(Mechanical.MODID, "stone_age/revive_mate"),
            REVIVED = Identifier.fromNamespaceAndPath(Mechanical.MODID, "stone_age/second_chance");
    private static final Identifier LOCK = Identifier.fromNamespaceAndPath(Mechanical.MODID, "downed"),
            CARRY = Identifier.fromNamespaceAndPath(Mechanical.MODID, "carrying_downed");
    /** Carrying a mate: as a medium carcass on the shoulders (CarcassHauling.SLOW_CARRY). */
    static final double SLOW_CARRY = 0.25;

    static final class Down {
        final DamageSource source;
        int ticks;

        Down(DamageSource source) {
            this.source = source;
        }
    }

    static final class Help {
        final ServerPlayer target;
        int ticks;
        long lastClick;

        Help(ServerPlayer target, long now) {
            this.target = target;
            this.lastClick = now;
        }
    }

    static final class Wake {
        int left;
        /** Woken in a bed: where vanilla stood you up, to go back to when you get up. */
        final @Nullable Vec3 standAt;

        Wake(int left, @Nullable Vec3 standAt) {
            this.left = left;
            this.standAt = standAt;
        }
    }

    static final Map<UUID, Down> DOWNED = new HashMap<>();
    /** helper -> whom he is helping up. */
    static final Map<UUID, Help> HELPING = new HashMap<>();
    /** Players holding R -> ticks held (giving up while downed, reviving while carrying). */
    static final Map<UUID, Integer> HOLDING = new HashMap<>();
    static final Map<UUID, Long> LAST_HURT = new HashMap<>(), SPIRIT = new HashMap<>();
    static final Map<UUID, Wake> WAKES = new HashMap<>();
    /** Mock players a game test counts as online (they aren't in the player list). */
    static final List<ServerPlayer> TEST_ONLINE = new java.util.ArrayList<>();
    /** Set while this code takes a downed player off his carrier (other dismounts are refused). */
    private static boolean releasing;
    /** Client side: entity id -> {kind, ticks (down: elapsed, waking: left), helper entity id or -1}. Filled by client.DownedClient. */
    public static final Map<Integer, int[]> CLIENT = new HashMap<>();

    // ---------------------------------------------------------------- queries (both sides)

    public static int kind(Player p) {
        if (!p.level().isClientSide()) return DOWNED.containsKey(p.getUUID()) ? DOWN : WAKES.containsKey(p.getUUID()) ? WAKING : NONE;
        int[] s = CLIENT.get(p.getId());
        return s == null ? NONE : s[0];
    }

    public static boolean isDowned(Player p) {
        return kind(p) == DOWN;
    }

    /** Downed or waking: no items, no hands, no walking. */
    public static boolean locked(Player p) {
        return kind(p) != NONE;
    }

    /** The downed player {@code p} carries on his shoulders, or null. */
    public static @Nullable Player carrying(Player p) {
        for (Entity e : p.getPassengers()) if (e instanceof Player d) return d;
        return null;
    }

    /** Server: may a player ride {@code vehicle}? Only a downed one, on a player's shoulders (mixin.EntityRideMixin). */
    public static boolean mayRide(Entity rider, Entity vehicle) {
        return rider instanceof ServerPlayer p && vehicle instanceof Player && DOWNED.containsKey(p.getUUID());
    }

    /** Mobs don't see a downed player or a spirit. */
    static boolean ignored(Player p) {
        return DOWNED.containsKey(p.getUUID()) || SPIRIT.containsKey(p.getUUID());
    }

    static boolean raider(@Nullable Entity e) {
        return e instanceof Tribesperson t && t.isRaider();
    }

    static boolean multiplayer(MinecraftServer server) {
        return !server.isSingleplayer() || server.isPublished(); // a dedicated server, or a world opened to LAN
    }

    /** Would {@code p} go down rather than die: multiplayer and a tribe mate online who isn't down himself. */
    static boolean canGoDown(Clans data, ServerPlayer p, List<? extends Player> online, boolean multiplayer) {
        Clans.Clan clan = multiplayer ? data.clanOf(p.getUUID()) : null;
        if (clan == null) return false;
        for (Player o : online)
            if (o != p && o.isAlive() && !o.isSpectator() && clan.member(o.getUUID()) != null && !DOWNED.containsKey(o.getUUID())) return true;
        return false;
    }

    static boolean mates(ServerPlayer a, ServerPlayer b) {
        Clans.Clan clan = Clans.get(a.level().getServer()).clanOf(a.getUUID());
        return clan != null && clan.member(b.getUUID()) != null;
    }

    /** Safe to revive: no hostile mob or raider within {@link #SAFE_RADIUS}, and the carrier not hurt for {@link #SAFE_HURT_TICKS}. */
    static boolean safe(ServerPlayer carrier) {
        Long hurt = LAST_HURT.get(carrier.getUUID());
        if (hurt != null && carrier.level().getGameTime() - hurt < SAFE_HURT_TICKS) return false;
        return carrier.level().getEntitiesOfClass(Mob.class, carrier.getBoundingBox().inflate(SAFE_RADIUS),
                m -> m.isAlive() && (m instanceof Enemy || raider(m)) && m.distanceToSqr(carrier) <= SAFE_RADIUS * SAFE_RADIUS).isEmpty();
    }

    // ---------------------------------------------------------------- going down and dying

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        MinecraftServer server = sp.level().getServer();
        List<ServerPlayer> online = TEST_ONLINE.isEmpty() ? server.getPlayerList().getPlayers() : TEST_ONLINE;
        if (!DOWNED.containsKey(sp.getUUID()) && canGoDown(Clans.get(server), sp, online, multiplayer(server))) {
            event.setCanceled(true);
            goDown(sp, event.getSource());
            return;
        }
        // a real death: given up, bled out, finished off, or nobody to help
        DOWNED.remove(sp.getUUID());
        SPIRIT.remove(sp.getUUID());
        release(sp);
    }

    static void goDown(ServerPlayer sp, DamageSource source) {
        release(sp);
        if (sp.isPassenger()) sp.stopRiding();
        DOWNED.put(sp.getUUID(), new Down(source));
        sp.setHealth(1.0F);
        sp.clearFire();
        sp.stopUsingItem();
        sp.closeContainer();
        CarcassHauling.dropHauled(sp);
        lock(sp, true);
        untarget(sp);
        sync(sp);
        MinecraftServer server = sp.level().getServer();
        Clans.Clan clan = Clans.get(server).clanOf(sp.getUUID());
        if (clan != null) for (ServerPlayer o : server.getPlayerList().getPlayers())
            if (o != sp && clan.member(o.getUUID()) != null) o.sendSystemMessage(Component.translatable("message.mechanical.downed.mate_down", sp.getDisplayName()));
    }

    /** A downed player dies for real (by: who finished him, null: of his wounds). */
    static void finish(ServerPlayer sp, @Nullable DamageSource by) {
        Down d = DOWNED.get(sp.getUUID());
        if (d == null) return;
        DamageSource source = by != null ? by : d.source;
        sp.getCombatTracker().recordDamage(source, 1.0F);
        sp.setHealth(0.0F);
        sp.die(source); // onDeath sees him downed and lets it happen
    }

    /** Lets go of everything: whoever he carried or helped, his carrier, the R key, the lying pose. */
    static void release(ServerPlayer sp) {
        HELPING.remove(sp.getUUID());
        HOLDING.remove(sp.getUUID());
        WAKES.remove(sp.getUUID());
        if (carrying(sp) instanceof ServerPlayer carried) putDown(sp, carried, null);
        if (sp.getVehicle() instanceof ServerPlayer carrier) putDown(carrier, sp, null);
        lock(sp, false);
        sync(sp);
    }

    /** Lying down, can't walk or jump (both sides apply it: the client from DownedClient). */
    public static void lock(Player p, boolean on) {
        p.setForcedPose(on ? Pose.SLEEPING : null);
        for (var attr : List.of(Attributes.MOVEMENT_SPEED, Attributes.JUMP_STRENGTH)) {
            AttributeInstance a = p.getAttribute(attr);
            if (a == null) continue;
            if (on) a.addOrUpdateTransientModifier(new AttributeModifier(LOCK, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            else a.removeModifier(LOCK);
        }
    }

    /** Mobs (not raiders) that have {@code p} as their target drop it. */
    static void untarget(ServerPlayer p) {
        for (Mob m : p.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(32), m -> m.getTarget() == p && !raider(m))) {
            m.setTarget(null);
            m.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        }
    }

    @SubscribeEvent
    static void onTarget(LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() instanceof Player p && !p.level().isClientSide() && ignored(p) && !raider(event.getEntity()))
            event.setCanceled(true);
    }

    /** Downed: only players and raiders hurt you, and their hit kills; the rest (mobs, fire, falls) does nothing. /kill still kills. */
    @SubscribeEvent
    static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp) || !DOWNED.containsKey(sp.getUUID())) return;
        DamageSource s = event.getSource();
        if (s.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        event.setCanceled(true);
        Entity by = s.getEntity();
        if (by instanceof Player p && p != sp || raider(by)) finish(sp, s);
    }

    @SubscribeEvent
    static void onDamaged(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer sp) hurt(sp);
    }

    /** Server: a hit stops a help, and makes the place unsafe for a while. */
    static void hurt(ServerPlayer sp) {
        LAST_HURT.put(sp.getUUID(), sp.level().getGameTime());
        Help h = HELPING.remove(sp.getUUID());
        if (h != null) {
            sp.sendOverlayMessage(Component.translatable("message.mechanical.downed.help_interrupted"));
            sync(h.target);
        }
    }

    // ---------------------------------------------------------------- helping up, carrying, reviving

    /** Server, each right-click packet of a sneaking mate on a downed player: starts the help or keeps it going. */
    static boolean help(ServerPlayer helper, ServerPlayer target) {
        if (!DOWNED.containsKey(target.getUUID()) || locked(helper) || !helper.isShiftKeyDown() || carrying(helper) != null
                || target.getVehicle() != null || !mates(helper, target)) return false;
        long now = helper.level().getGameTime();
        Help h = HELPING.get(helper.getUUID());
        if (h != null && h.target == target) h.lastClick = now;
        else {
            HELPING.put(helper.getUUID(), new Help(target, now));
            sync(target);
        }
        return true;
    }

    private static void tickHelp(ServerPlayer helper, Help h, long now) {
        ServerPlayer t = h.target;
        if (!DOWNED.containsKey(t.getUUID()) || t.isRemoved() || now - h.lastClick > CLICK_GAP || !helper.isShiftKeyDown()
                || helper.distanceTo(t) > HELP_REACH || carrying(helper) != null || t.getVehicle() != null) {
            HELPING.remove(helper.getUUID());
            sync(t);
            return;
        }
        h.ticks++;
        if (h.ticks % 10 == 0) helper.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
        Component bar = progress("message.mechanical.downed.helping", h.ticks, HELP_TICKS, t.getDisplayName());
        helper.sendOverlayMessage(bar);
        if (h.ticks < HELP_TICKS) return;
        HELPING.remove(helper.getUUID());
        pickUp(helper, t);
    }

    static Component progress(String key, int done, int of, Component name) {
        int filled = Math.min(10, done * 10 / of);
        return Component.translatable(key, name, "|".repeat(filled) + ".".repeat(10 - filled));
    }

    static void pickUp(ServerPlayer carrier, ServerPlayer downed) {
        if (!downed.startRiding(carrier, true, false)) return;
        AttributeInstance speed = carrier.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.addOrUpdateTransientModifier(new AttributeModifier(CARRY, -SLOW_CARRY, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        carrier.sendSystemMessage(Component.translatable("message.mechanical.downed.carrying", downed.getDisplayName(),
                Component.keybind("key.mechanical.downed")));
        downed.sendOverlayMessage(Component.translatable("message.mechanical.downed.carried", carrier.getDisplayName()));
        sync(downed);
    }

    /** {@code carrier} puts {@code downed} down at {@code at} (null: in front of him). */
    static void putDown(ServerPlayer carrier, ServerPlayer downed, @Nullable Vec3 at) {
        releasing = true;
        try {
            downed.stopRiding();
        } finally {
            releasing = false;
        }
        AttributeInstance speed = carrier.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(CARRY);
        HOLDING.remove(carrier.getUUID());
        if (at == null) at = carrier.position().add(Vec3.directionFromRotation(0, carrier.getYRot()).scale(0.9));
        if (!carrier.level().noCollision(downed, downed.getBoundingBox().move(at.subtract(downed.position())))) at = carrier.position();
        downed.teleportTo(at.x, at.y, at.z);
        sync(downed);
    }

    /** Back on his feet with {@link #REVIVE_HEALTH} and everything he had; he gets up like after a night's sleep. */
    static void revive(ServerPlayer carrier, ServerPlayer downed) {
        putDown(carrier, downed, null);
        DOWNED.remove(downed.getUUID());
        HOLDING.remove(downed.getUUID());
        downed.setHealth(REVIVE_HEALTH);
        startWake(downed, null, false);
        Clans.award(carrier, REVIVE_MATE);
        Clans.award(downed, REVIVED);
        carrier.sendOverlayMessage(Component.translatable("message.mechanical.downed.revived_other", downed.getDisplayName()));
        downed.sendSystemMessage(Component.translatable("message.mechanical.downed.revived", carrier.getDisplayName()));
    }

    // ---------------------------------------------------------------- waking up

    @SubscribeEvent
    static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.isEndConquered() || !(event.getEntity() instanceof ServerPlayer sp)) return;
        BlockPos bed = null;
        ServerPlayer.RespawnConfig rc = sp.getRespawnConfig();
        if (rc != null) {
            GlobalPos at = rc.respawnData().globalPos();
            if (at.dimension() == sp.level().dimension() && at.pos().closerToCenterThan(sp.position(), 3)
                    && sp.level().getBlockState(at.pos()).getBlock() instanceof net.minecraft.world.level.block.AbstractBedBlock) bed = at.pos();
        }
        startWake(sp, bed, bed == null);
    }

    /** Lies down to wake up: in the bed at {@code bed}, or where he is; {@code spirit}: a spirit until it wears off. */
    public static void startWake(ServerPlayer sp, @Nullable BlockPos bed, boolean spirit) {
        Vec3 standAt = sp.position();
        if (bed != null) sp.teleportTo(bed.getX() + 0.5, bed.getY() + 0.6875, bed.getZ() + 0.5);
        WAKES.put(sp.getUUID(), new Wake(WAKE_TICKS, bed != null ? standAt : null));
        if (spirit) SPIRIT.put(sp.getUUID(), sp.level().getGameTime() + SPIRIT_TICKS);
        lock(sp, true);
        sync(sp);
    }

    private static void endWake(ServerPlayer sp, Wake w) {
        WAKES.remove(sp.getUUID());
        lock(sp, false);
        if (w.standAt != null) sp.teleportTo(w.standAt.x, w.standAt.y, w.standAt.z);
        sync(sp);
        if (SPIRIT.containsKey(sp.getUUID())) sp.sendOverlayMessage(Component.translatable("message.mechanical.downed.spirit"));
    }

    static void endSpirit(ServerPlayer sp) {
        if (SPIRIT.remove(sp.getUUID()) != null) sp.sendOverlayMessage(Component.translatable("message.mechanical.downed.spirit_end"));
    }

    // ---------------------------------------------------------------- ticking

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer sp) tick(sp);
    }

    static void tick(ServerPlayer sp) {
        UUID id = sp.getUUID();
        long now = sp.level().getGameTime();
        Down d = DOWNED.get(id);
        if (d != null) {
            if (sp.getHealth() > 1.0F) sp.setHealth(1.0F); // no healing back up from food
            if (!(sp.getVehicle() instanceof Player)) d.ticks++; // the clock stops while a mate carries you
            Integer held = HOLDING.computeIfPresent(id, (k, v) -> v + 1);
            if (d.ticks >= DOWN_TICKS || held != null && held >= GIVE_UP_TICKS) {
                finish(sp, null);
                return;
            }
            if (sp.tickCount % 10 == 0) untarget(sp);
            if (sp.tickCount % 20 == 0) sync(sp);
        }
        Help h = HELPING.get(id);
        if (h != null) tickHelp(sp, h, now);
        if (carrying(sp) instanceof ServerPlayer carried) tickCarry(sp, carried);
        Wake w = WAKES.get(id);
        if (w != null) {
            w.left--;
            if (w.left == STAND_TICKS) sp.setForcedPose(Pose.CROUCHING); // getting up
            if (w.left <= 0) endWake(sp, w);
        }
        Long spirit = SPIRIT.get(id);
        if (spirit != null) {
            if (now >= spirit) endSpirit(sp);
            else if (now % 10 == 0) untarget(sp);
        }
    }

    private static void tickCarry(ServerPlayer carrier, ServerPlayer carried) {
        if (!DOWNED.containsKey(carried.getUUID())) {
            putDown(carrier, carried, null);
            return;
        }
        Integer held = HOLDING.get(carrier.getUUID());
        if (held == null) return;
        if (!safe(carrier)) {
            HOLDING.put(carrier.getUUID(), 0);
            carrier.sendOverlayMessage(Component.translatable("message.mechanical.downed.not_safe"));
            return;
        }
        HOLDING.put(carrier.getUUID(), ++held);
        carrier.sendOverlayMessage(progress("message.mechanical.downed.reviving", held, REVIVE_TICKS, carried.getDisplayName()));
        if (held >= REVIVE_TICKS) revive(carrier, carried);
    }

    // ---------------------------------------------------------------- hands: downed and waking hold nothing, a carrier's are full

    private static boolean handsTied(Player p) {
        return locked(p) || carrying(p) != null;
    }

    private static void refuse(ICancellableEvent event) {
        event.setCanceled(true);
    }

    @SubscribeEvent
    static void onInteractEntity(PlayerInteractEvent.EntityInteract event) {
        Player p = event.getEntity();
        if (handsTied(p)) {
            refuse(event);
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }
        if (!(event.getTarget() instanceof Player t) || !isDowned(t) || !p.isShiftKeyDown()) {
            if (p instanceof ServerPlayer sp) endSpirit(sp);
            return;
        }
        refuse(event);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getHand() == InteractionHand.MAIN_HAND && p instanceof ServerPlayer sp && t instanceof ServerPlayer st) help(sp, st);
    }

    @SubscribeEvent
    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player p = event.getEntity();
        if (p instanceof ServerPlayer sp && !handsTied(p)) endSpirit(sp);
        if (!handsTied(p)) return;
        refuse(event);
        event.setCancellationResult(InteractionResult.FAIL);
        if (event.getHand() == InteractionHand.MAIN_HAND && p.isShiftKeyDown() && p instanceof ServerPlayer sp && carrying(sp) instanceof ServerPlayer c)
            putDown(sp, c, Vec3.atBottomCenterOf(event.getPos().relative(event.getFace() == null ? net.minecraft.core.Direction.UP : event.getFace())));
    }

    @SubscribeEvent
    static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player p = event.getEntity();
        if (!handsTied(p)) return;
        refuse(event);
        event.setCancellationResult(InteractionResult.FAIL);
        if (event.getHand() == InteractionHand.MAIN_HAND && p.isShiftKeyDown() && p instanceof ServerPlayer sp && carrying(sp) instanceof ServerPlayer c) putDown(sp, c, null);
    }

    @SubscribeEvent
    static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (handsTied(event.getEntity())) refuse(event);
        else if (event.getEntity() instanceof ServerPlayer sp) endSpirit(sp);
    }

    @SubscribeEvent
    static void onAttack(AttackEntityEvent event) {
        if (handsTied(event.getEntity())) refuse(event);
        else if (event.getEntity() instanceof ServerPlayer sp) endSpirit(sp);
    }

    @SubscribeEvent
    static void onUseItem(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof Player p && handsTied(p)) refuse(event);
    }

    /** A downed player can't climb off his carrier (only putDown takes him off). */
    @SubscribeEvent
    static void onMount(EntityMountEvent event) {
        if (event.isDismounting() && !releasing && !event.getLevel().isClientSide() && event.getEntityBeingMounted() instanceof Player
                && event.getEntityMounting() instanceof ServerPlayer p && DOWNED.containsKey(p.getUUID())) event.setCanceled(true);
    }

    // ---------------------------------------------------------------- comings and goings

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (DOWNED.containsKey(sp.getUUID())) finish(sp, null); // leaving while down is dying
        else release(sp);
        SPIRIT.remove(sp.getUUID());
        LAST_HURT.remove(sp.getUUID());
    }

    @SubscribeEvent
    static void onTravel(net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp && carrying(sp) instanceof ServerPlayer c) putDown(sp, c, null);
    }

    @SubscribeEvent
    static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer t && event.getEntity() instanceof ServerPlayer watcher && kind(t) != NONE)
            PacketDistributor.sendToPlayer(watcher, state(t));
    }

    @SubscribeEvent
    static void onStopped(ServerStoppedEvent event) {
        DOWNED.clear();
        HELPING.clear();
        HOLDING.clear();
        LAST_HURT.clear();
        SPIRIT.clear();
        WAKES.clear();
    }

    // ---------------------------------------------------------------- network

    /** Server -> client: a player's state (kind, ticks, the entity id of whoever is helping him up or -1). */
    public record Sync(int entity, int kind, int ticks, int helper) implements CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "downed_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Sync> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Sync::entity, ByteBufCodecs.VAR_INT, Sync::kind, ByteBufCodecs.VAR_INT, Sync::ticks,
                ByteBufCodecs.VAR_INT, Sync::helper, Sync::new);

        @Override
        public Type<Sync> type() {
            return TYPE;
        }
    }

    /** Client -> server: the downed key ({@link #HOLD}, {@link #RELEASE}) or a put-down click in the air ({@link #PUT_DOWN}). */
    public record Key(int action) implements CustomPacketPayload {
        public static final Type<Key> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "downed_key"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Key> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Key::action, Key::new);

        @Override
        public Type<Key> type() {
            return TYPE;
        }
    }

    static Sync state(ServerPlayer p) {
        Down d = DOWNED.get(p.getUUID());
        if (d != null) {
            int helper = -1;
            for (var e : HELPING.entrySet())
                if (e.getValue().target == p && p.level().getServer().getPlayerList().getPlayer(e.getKey()) instanceof ServerPlayer h) helper = h.getId();
            return new Sync(p.getId(), DOWN, d.ticks, helper);
        }
        Wake w = WAKES.get(p.getUUID());
        return w != null ? new Sync(p.getId(), WAKING, w.left, -1) : new Sync(p.getId(), NONE, 0, -1);
    }

    static void sync(ServerPlayer p) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, state(p));
    }

    @SubscribeEvent
    static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional()
                .playToClient(Sync.TYPE, Sync.CODEC)
                .playToServer(Key.TYPE, Key.CODEC, Downed::onKey);
    }

    private static void onKey(Key k, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer sp)) return;
        switch (k.action()) {
            case HOLD -> HOLDING.putIfAbsent(sp.getUUID(), 0);
            case RELEASE -> HOLDING.remove(sp.getUUID());
            case PUT_DOWN -> {
                if (sp.isShiftKeyDown() && carrying(sp) instanceof ServerPlayer c) putDown(sp, c, null);
            }
            default -> {}
        }
    }

    private Downed() {}
}
