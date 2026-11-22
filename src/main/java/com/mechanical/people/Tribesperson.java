package com.mechanical.people;

import com.mechanical.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;

import com.mechanical.CampfireAccess;
import com.mechanical.Campfires;
import com.mechanical.Foraging;
import com.mechanical.Mechanical;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * A Stone Age person: hunter, gatherer, fisher, woodcutter or elder. Lives at a camp (home = its campfire) or roams in a
 * hunting band (no home, follows the band leader). Never despawns and never comes back once dead. Swims like a player
 * (surface swimming in the swimming pose, across water where the way round is much longer).
 * <p>
 * Friendly by default and greets players now and then. Hitting one angers its whole group (same {@link #group}) at
 * that player for a day; killing one makes the group hostile to that player for good. Elders and the Miner (Miners) barter
 * (vanilla merchant screen, item-for-item offers).
 */
public class Tribesperson extends PathfinderMob implements Merchant, RangedAttackMob, Npc {
    private static final EntityDataAccessor<Integer> ROLE = SynchedEntityData.defineId(Tribesperson.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SKIN = SynchedEntityData.defineId(Tribesperson.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CLIMATE = SynchedEntityData.defineId(Tribesperson.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> COLOUR = SynchedEntityData.defineId(Tribesperson.class, EntityDataSerializers.INT);
    /** The idle action and whether seated (CampLife), for the model. */
    private static final EntityDataAccessor<Integer> IDLE = SynchedEntityData.defineId(Tribesperson.class, EntityDataSerializers.INT);

    static final long ANGER_TICKS = Calendar.DAY, FOREVER = Long.MAX_VALUE;
    private static final double GROUP_RANGE = 48, GREET_RANGE = 6;
    /** Each new block a traveller between camps steps onto wears this many times (TrampledPaths), so paths form. */
    private static final int TRAVEL_WEAR = 3;
    /** Walking input times this in water: at a goal's 0.8 about 80% of a player's swimming speed. */
    private static final double SWIM_BOOST = 3.3;
    private static final int GREET_COOLDOWN = 6000, GREET_COOLDOWN_ANY = 600, FIRE_TEND_TICKS = 1200;
    private static final Codec<Map<UUID, Long>> ANGER_CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.LONG);

    static final String[] MEN = {"Aruk", "Bodan", "Grak", "Haddu", "Urgo", "Kesh", "Tamu", "Orrin", "Brannok", "Esk", "Dagu", "Torvik",
            "Mabun", "Rokka", "Ilgar", "Fenno", "Shur", "Gorm", "Kalek", "Vidu"};
    static final String[] WOMEN = {"Neka", "Saru", "Ivva", "Tesha", "Luma", "Oona", "Kira", "Asha", "Merru", "Yeli", "Hanna", "Duna",
            "Sefa", "Ralla", "Inka", "Vesa", "Eda", "Nuri", "Talli", "Bera"};

    /** Last greeting any tribesperson gave each player, so a whole camp doesn't talk at once. */
    private static final Map<Player, Long> LAST_GREETED = new WeakHashMap<>();

    private String tribeName = "";
    private BlockPos home;
    private UUID group;
    private boolean leader;
    private final Map<UUID, Long> anger = new HashMap<>();
    MerchantOffers offers;
    private long restockDay = -1;
    private Player tradingPlayer;

    // server-side work state, not saved
    private final Map<UUID, Long> greeted = new HashMap<>();
    private LivingEntity prey;
    Vec3 killPos;
    long killTime;
    boolean wantsHome, carryingWood;
    private long nextFireTend, corneredAt = -1000, nextGift;
    /** Set for a raider (people.Raids): what the raid is, saved with the entity. */
    RaidGoal.State raid;
    /** Where the group is going to set up a new camp (SetUpCampGoal); since when it has had no fire. */
    BlockPos newCamp;
    long homelessSince = -1;
    /** The visit (Neighbours.Visit id) this person came to a player camp with, saved; null when not visiting. */
    UUID visit;
    /** A Trader Caravan member (Caravans): the game time it moves on (despawns), saved; -1 for everyone else. */
    long leaveAt = -1;
    /** A walk to another camp and back (ErrandGoal), not saved; walking it wears paths. */
    ErrandGoal.Errand errand;
    boolean travelling;
    /** A carcass left as a threat (CarcassGifts): the player armed folk keep an eye on, and until when. Not saved. */
    private Player watching;
    private long watchUntil;
    /** One of the Oak Folk (OakFolk): the Ancient Oak's altar they live round, saved; null for everyone else. */
    BlockPos oakFoot;
    /** Sitting, idle actions, sleeping and watch round a fire (not saved). */
    final CampLife life = new CampLife(this);

    public Tribesperson(EntityType<? extends Tribesperson> type, Level level) {
        super(type, level);
        setPathfindingMalus(PathType.FIRE, -1.0F);
        setPathfindingMalus(PathType.FIRE_IN_NEIGHBOR, 16.0F);
        setPathfindingMalus(PathType.DAMAGING_IN_NEIGHBOR, 16.0F);
        setPathfindingMalus(PathType.LAVA, -1.0F);
        // they swim: across water when the way round is much longer (vanilla mobs pay 8 per block of it, and shun the shore)
        setPathfindingMalus(PathType.WATER, 1.5F);
        setPathfindingMalus(PathType.WATER_BORDER, 0.0F);
        setDropChance(EquipmentSlot.MAINHAND, 0.1F);
        setDropChance(EquipmentSlot.OFFHAND, 0.0F);
        setDropChance(EquipmentSlot.HEAD, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 3.0).add(Attributes.ARMOR, 2.0).add(Attributes.FOLLOW_RANGE, 24.0);
    }

    // ---- identity ----

    public TribeRole role() {
        return TribeRole.byId(entityData.get(ROLE));
    }

    /** CampLife's idle action, plus CampLife.SITTING while seated on the ground. */
    public int idle() {
        return entityData.get(IDLE);
    }

    void setIdle(int idle) {
        if (idle != idle()) entityData.set(IDLE, idle);
    }

    public int skin() {
        return entityData.get(SKIN);
    }

    public boolean isWoman() {
        return skin() % 2 == 1;
    }

    /** The climate this person dresses for (set where they were born, see ClimateZone). */
    public ClimateZone climate() {
        return ClimateZone.byId(entityData.get(CLIMATE));
    }

    /** The tribe's colour (a DyeColor id) for the dyed parts of the clothes, or -1 without a tribe. */
    public int colour() {
        return entityData.get(COLOUR);
    }

    /** The skin: temperate people in tribesperson/, the other climates in a folder of their own; its "_tint" mask marks the dyed parts. */
    public Identifier texture() {
        ClimateZone zone = role().travels() ? ClimateZone.TEMPERATE : climate();
        String dir = zone == ClimateZone.TEMPERATE ? "" : zone.id() + "/";
        return Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/entity/tribesperson/" + dir + role().id() + "_" + skin() + ".png");
    }

    public String tribeName() {
        return tribeName;
    }

    public BlockPos home() {
        return home;
    }

    public UUID group() {
        return group;
    }

    private Tribe tribe;

    /** The tribe this person belongs to (follows from the group), or null for a person without a group. */
    public Tribe tribe() {
        if (group == null) return null;
        if (tribe == null || !tribe.id.equals(group)) tribe = Tribe.of(group);
        return tribe;
    }

    /** This person's tribe's age (WorldAge.npcAge): the world's, or one behind. Stone Age without a tribe or on the client. */
    public int age() {
        return group != null && level() instanceof ServerLevel sl ? com.mechanical.WorldAge.npcAge(sl.getServer(), group) : com.mechanical.WorldAge.STONE;
    }

    private net.minecraft.world.item.Item spear() {
        return age() >= com.mechanical.WorldAge.BRONZE ? com.mechanical.Bronze.BRONZE_SPEAR.get() : Mechanical.FLINT_SPEAR.get();
    }

    public boolean isRaider() {
        return raid != null;
    }

    /** Turns this person into a raider: spear (or now and then a bow) or axe, a torch, more health, longer sight. */
    void makeRaider(RaidGoal.State state) {
        raid = state;
        if (role() == TribeRole.HUNTER) setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(getRandom().nextInt(4) == 0 ? Mechanical.CRUDE_BOW.get() : spear()));
        setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TORCH));
        // their weapons, torches and the leader's banner are the spoils of a beaten raid
        setDropChance(EquipmentSlot.MAINHAND, 0.5F);
        setDropChance(EquipmentSlot.OFFHAND, 0.5F);
        if (isLeader()) setDropChance(EquipmentSlot.HEAD, 1.0F);
        getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(48.0);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(24.0);
        setHealth(24.0F);
        raiderNavigation();
    }

    /** Raiders plan paths through wooden doors (and break them down) and look further for a way round. */
    private void raiderNavigation() {
        if (getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation nav) {
            nav.setCanOpenDoors(true);
            nav.setMaxVisitedNodesMultiplier(4.0F);
        }
    }

    public boolean isLeader() {
        return leader;
    }

    void setLeader(boolean leader) {
        this.leader = leader;
    }

    /** Hunters, and anyone holding a weapon (axe, sword, spear, bow ...), fight monsters; the rest keep away from them. */
    public boolean isArmed() {
        if (role() == TribeRole.HUNTER) return true;
        ItemStack held = getMainHandItem();
        return held.is(ItemTags.AXES) || held.is(ItemTags.SWORDS) || held.is(ItemTags.SPEARS) || held.is(Mechanical.TUSK_DAGGER.get())
                || held.getItem() instanceof net.minecraft.world.item.ProjectileWeaponItem || held.getItem() instanceof net.minecraft.world.item.TridentItem;
    }

    /** No way to run from a threat: an unarmed person hits back for a few seconds. */
    void cornered(LivingEntity threat) {
        corneredAt = level().getGameTime();
        if (getTarget() == null) setTarget(threat);
    }

    boolean isCornered() {
        return level().getGameTime() - corneredAt < 60;
    }

    /** The group made a new fire here: it is home now. */
    void settleAt(BlockPos fire) {
        home = fire.immutable();
        newCamp = null;
        homelessSince = -1;
    }

    public boolean inGroup(Tribesperson other) {
        return group != null && group.equals(other.group);
    }

    /** Sets role, home campfire (null for a roaming band) and group; gives a fresh look, name and the role's tools. */
    public void setup(TribeRole role, BlockPos home, UUID group, boolean leader) {
        // a loaded spot (runtime spawns); worldgen passes the climate it already knows instead
        setup(role, home, group, leader, ClimateZone.of(level().getBiome(home != null ? home : blockPosition())));
    }

    /** As {@link #setup(TribeRole, BlockPos, UUID, boolean)}, dressed for {@code climate}. */
    public void setup(TribeRole role, BlockPos home, UUID group, boolean leader, ClimateZone climate) {
        RandomSource r = getRandom();
        entityData.set(ROLE, role.ordinal());
        entityData.set(SKIN, role == TribeRole.MINER ? 2 * r.nextInt(2) : r.nextInt(TribeRole.SKINS));
        entityData.set(CLIMATE, climate.ordinal());
        entityData.set(COLOUR, group == null || role.oak() ? -1 : Tribe.of(group).baseColour().getId());
        this.home = home;
        this.group = group;
        this.leader = leader;
        String[] names = isWoman() ? WOMEN : MEN;
        tribeName = names[r.nextInt(names.length)];
        setCustomName(Component.translatable("entity.mechanical.tribesperson.named", tribeName,
                Component.translatable("entity.mechanical.tribesperson.role." + role.id()), tribe().name()));
        setCustomNameVisible(false);
        boolean bronze = age() >= com.mechanical.WorldAge.BRONZE; // no iron tools exist yet
        ItemStack tool = switch (role) {
            case HUNTER -> new ItemStack(r.nextBoolean() ? Mechanical.CRUDE_BOW.get() : spear());
            case GATHERER -> new ItemStack(bronze ? com.mechanical.Bronze.BRONZE_KNIFE.get() : Mechanical.FLINT_KNIFE.get());
            case FISHER -> new ItemStack(Mechanical.CRUDE_FISHING_ROD.get());
            case WOODCUTTER -> new ItemStack(bronze ? com.mechanical.Bronze.BRONZE_AXE.get() : Mechanical.FLINT_AXE.get());
            case ELDER -> new ItemStack(Mechanical.ELDERS_STAFF.get());
            case TRADER -> new ItemStack(Mechanical.WALKING_STAFF.get());
            case GUARD -> new ItemStack(spear()); // Caravans arms them for their tribe's age
            case MINER -> new ItemStack(bronze ? com.mechanical.Bronze.BRONZE_PICKAXE.get() : Mechanical.FLINT_PICKAXE.get());
            case OAK_GATHERER -> new ItemStack(Mechanical.FLINT_KNIFE.get());
            case OAK_LIGHTBEARER -> new ItemStack(Items.CANDLE);
            case OAK_KEEPER -> new ItemStack(OakFolk.VERDANT_STAFF.get());
            case OAK_WORSHIPPER -> ItemStack.EMPTY;
        };
        setItemSlot(EquipmentSlot.MAINHAND, tool);
        if (role.oak()) setDropChance(EquipmentSlot.MAINHAND, 0.0F); // the keeper's staff is a quest reward, not loot
        setItemSlot(EquipmentSlot.OFFHAND, role == TribeRole.GATHERER || role == TribeRole.OAK_GATHERER ? new ItemStack(Mechanical.BARK_BASKET.get())
                : role == TribeRole.OAK_LIGHTBEARER ? new ItemStack(Items.CANDLE)
                : role == TribeRole.MINER ? new ItemStack(Items.TORCH) : ItemStack.EMPTY); // the miner's torch lights his way (DynamicLights)
        // the tribe's flag: worn by the band leader and the elder, like a raid captain's banner
        setItemSlot(EquipmentSlot.HEAD, role == TribeRole.OAK_KEEPER ? new ItemStack(com.mechanical.AncientOak.ANTLER_CROWN.get()) // giant deer antlers
                : role.oak() ? ItemStack.EMPTY
                : leader && role != TribeRole.MINER || role == TribeRole.ELDER ? tribe().banner(registryAccess()) : ItemStack.EMPTY);
        offers = null;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, SpawnGroupData groupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
        setPersistenceRequired();
        if (tribeName.isEmpty() && reason != EntitySpawnReason.STRUCTURE) {
            // spawn egg / command: a random role, at the nearest campfire if there is one, in a group of its own
            BlockPos fire = null;
            for (BlockPos p : BlockPos.betweenClosed(blockPosition().offset(-12, -4, -12), blockPosition().offset(12, 4, 12))) {
                if (level.getBlockState(p).is(Blocks.CAMPFIRE)) { fire = p.immutable(); break; }
            }
            if (getType() == People.MINER.get()) setup(TribeRole.MINER, fire, Miners.tribe(getRandom()), true);
            else {
                TribeRole role = TribeRole.byId(getRandom().nextInt(TribeRole.ELDER.ordinal() + 1)); // caravans come only whole (Caravans)
                setup(role, role == TribeRole.HUNTER ? null : fire, UUID.randomUUID(), true);
            }
        }
        return data;
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    // ---- goals ----

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new FleeThreatGoal(this));
        goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.BreakDoorGoal(this, d -> true) {
            @Override
            public boolean canUse() {
                return raid != null && super.canUse();
            }
        });
        goalSelector.addGoal(2, new RangedBowAttackGoal<>(this, 1.0, 20, 15.0F));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true) {
            @Override
            public boolean canUse() {
                return !isHolding(s -> s.getItem() instanceof BowItem) && super.canUse();
            }
        });
        goalSelector.addGoal(3, new RaidGoal(this));
        goalSelector.addGoal(3, new ButcherCarcassGoal(this));
        goalSelector.addGoal(3, new VisitGoal(this));
        goalSelector.addGoal(3, new RoadBuildGoal(this));
        goalSelector.addGoal(4, new DailyRhythmGoal(this));
        goalSelector.addGoal(4, new OakFolkGoal(this));
        goalSelector.addGoal(4, new ErrandGoal(this));
        goalSelector.addGoal(4, new SetUpCampGoal(this));
        goalSelector.addGoal(4, new ReturnHomeGoal(this));
        goalSelector.addGoal(5, new GatherGoal(this));
        goalSelector.addGoal(5, new FishGoal(this));
        goalSelector.addGoal(5, new ChopWoodGoal(this));
        goalSelector.addGoal(5, new BuildCampGoal(this));
        goalSelector.addGoal(5, new FetchStoneGoal(this));
        goalSelector.addGoal(6, new PlantGoal(this));
        goalSelector.addGoal(6, new FollowBandLeaderGoal(this));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.55) {
            @Override
            public boolean canUse() {
                // bands rest together at night; camp folk stroll only within their (small, at night) home radius
                return !(home == null && level().isDarkOutside()) && (home != null || leader) && super.canUse();
            }
        });
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new HurtByTargetGoal(this, Tribesperson.class) {
            @Override
            public boolean canUse() {
                // unarmed folk hit back at monsters only when cornered (players they always answer)
                LivingEntity by = getLastHurtByMob();
                return super.canUse() && (isArmed() || by == null || !FleeThreatGoal.isThreat(Tribesperson.this, by) || isCornered());
            }
        });
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true, (t, l) -> isAngryAt((Player) t) && (raid == null || RaidGoal.fights(this, t))));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Animal.class, 10, true, false, (t, l) -> raid != null && RaidGoal.campAnimal(this, t)));
        // armed folk take on monsters near camp, and anything that goes for one of their own
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false, (t, l) -> defends(t)));
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Animal.class, 20, true, false,
                (t, l) -> role() == TribeRole.HUNTER && !level().isDarkOutside() && visit == null && errand == null && Settlements.wantsGame(this) && isPrey(t) && !FleeThreatGoal.dreads(this, t)));
    }

    /**
     * Armed folk take on monsters near camp, and anything that goes for one of their own. A visiting band's warriors guard
     * the whole camp zone round the player's fire (CampAge.RADIUS), raiders of other tribes too (docs/living-world-plan.md C).
     */
    boolean defends(LivingEntity t) {
        if (!isArmed() || t instanceof com.mechanical.wildlife.night.MushroomMan && !isHolding(s -> s.getItem() instanceof BowItem)) return false;
        BlockPos fire = visitFire();
        boolean guarding = fire != null && t.distanceToSqr(Vec3.atCenterOf(fire)) < com.mechanical.CampAge.RADIUS * com.mechanical.CampAge.RADIUS;
        if (!guarding && t.distanceToSqr(this) >= 12 * 12) return false;
        if (guarding && t instanceof Tribesperson other && other.raid != null && !inGroup(other)) return true;
        return t instanceof Monster && !(t instanceof NeutralMob) || MonstersHuntPeople.huntsPeople(t) || !(t instanceof Tribesperson) && t instanceof Mob m && m.getTarget() instanceof Tribesperson;
    }

    /** The player camp fire a visiting band gathered at (Neighbours.Visit#fire), or null when not visiting. */
    BlockPos visitFire() {
        if (visit == null || !(level() instanceof ServerLevel level)) return null;
        Neighbours.Visit v = Neighbours.get(level).visit(visit);
        return v == null ? null : v.fire;
    }

    /** Keep a wary eye on {@code player} until {@code until} (game time): face them, weapon ready (CarcassGifts). */
    void watch(Player player, long until) {
        watching = player;
        watchUntil = until;
    }

    /** Wild game a hunter may take: the mod's wild animals (not bears) and wild rabbits; never pets, leashed or village animals. */
    static boolean isPrey(LivingEntity e) {
        if (e.isBaby() || ((Animal) e).isLeashed()) return false;
        return e instanceof com.mechanical.wildlife.WildAnimal && !(e instanceof com.mechanical.wildlife.BrownBear)
                || e instanceof Rabbit rabbit && !rabbit.isPersistenceRequired();
    }

    // ---- anger ----

    public boolean isAngryAt(Player player) {
        if (player.isCreative() || player.isSpectator()) return false;
        return raid != null || anger.getOrDefault(player.getUUID(), 0L) > level().getGameTime() || tribe() != null && tribe().hostileTo(player);
    }

    public boolean isAngry() {
        long now = level().getGameTime();
        return anger.values().stream().anyMatch(t -> t > now);
    }

    List<Tribesperson> groupNearby(double range) {
        return level().getEntitiesOfClass(Tribesperson.class, getBoundingBox().inflate(range), this::inGroup);
    }

    /**
     * The whole group (those around) turns on the player until {@code until} (game time; FOREVER for a killer), and the
     * player's standing with the tribe changes by {@code reputation} (so members elsewhere remember too).
     */
    private void angerGroup(Player player, long until, int reputation) {
        boolean wasAngry = isAngryAt(player);
        // a killing makes the whole tribe hostile for good: gifts and trade are refused from then on
        if (until == FOREVER && tribe() != null) reputation = Math.min(reputation, Tribe.HOSTILE - 20 - tribe().reputation(player));
        if (!player.isCreative() && tribe() != null) tribe().addReputation(player, reputation);
        for (Tribesperson t : groupNearby(GROUP_RANGE)) {
            t.anger.merge(player.getUUID(), until, Math::max);
            if (t != this && t.isAlive() && !player.isCreative() && t.distanceToSqr(player) < 32 * 32) t.setTarget(player);
        }
        anger.merge(player.getUUID(), until, Math::max);
        if (!wasAngry) say(player, Component.translatable(until == FOREVER ? "chat.mechanical.tribesperson.killer" : "chat.mechanical.tribesperson.angry"));
    }

    /** Someone took the camp's fire away: the group is as angry as if one of them was hit. */
    public void fireTakenBy(Player player) {
        angerGroup(player, level().getGameTime() + ANGER_TICKS, Tribe.FIRE);
    }

    /** Seen raiding the tribe's storage. */
    public void caughtStealing(Player player) {
        say(player, Component.translatable("chat.mechanical.tribesperson.thief"));
        angerGroup(player, level().getGameTime() + ANGER_TICKS, Tribe.THEFT);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (source.getEntity() instanceof Tribesperson) return false; // no friendly fire (stray arrows)
        if (source.is(DamageTypes.CAMPFIRE)) return false;
        boolean hurt = super.hurtServer(level, source, damage);
        // fighting off raiders is no crime against their tribe
        if (hurt && source.getEntity() instanceof Player player && raid == null) angerGroup(player, level.getGameTime() + ANGER_TICKS, Tribe.HIT);
        if (hurt && source.getEntity() instanceof LivingEntity attacker && !(attacker instanceof Player)) {
            // the group around comes to help
            for (Tribesperson t : groupNearby(16)) if (t.getTarget() == null && t.isArmed()) t.setTarget(attacker);
        }
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        if (source.getEntity() instanceof Player player && level() instanceof ServerLevel && raid == null) angerGroup(player, FOREVER, Tribe.KILL);
        if (raid != null && level() instanceof ServerLevel level) {
            raid.loot.forEach(s -> spawnAtLocation(level, s)); // the stolen food back
            raid.loot.clear();
        }
        if (tradingPlayer != null) tradingPlayer.closeContainer();
        if (level() instanceof ServerLevel) life.wake(); // the bed they lay in goes with them
        super.die(source);
    }

    // ---- ticking ----

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        long now = level.getGameTime();
        boolean night = level.isDarkOutside();
        // keep near home: wide by day, close to the fire at night, further while fighting
        if (home != null && errand == null) setHomeTo(home, getTarget() != null ? 32 : night ? 5 : Math.max(role().roam, 4));
        else if (oakFoot != null) setHomeTo(oakFoot, getTarget() != null ? 32 : role().roam + 4); // the Oak Folk keep to the clearing
        else clearHome();
        if (travelling) com.mechanical.TrampledPaths.step(this, TRAVEL_WEAR);

        if (getTarget() instanceof Player p && !isAngryAt(p) && getLastHurtByMob() != p) setTarget(null);
        if (getTarget() != null && !isArmed() && !isCornered() && FleeThreatGoal.isThreat(this, getTarget())) setTarget(null);
        if (getTarget() instanceof Animal a) prey = a;
        if (prey != null && !prey.isAlive()) {
            killPos = prey.position();
            killTime = now;
            prey = null;
        }
        if (tickCount % 100 == 0) shareAnger();
        if (watching != null) {
            boolean over = now > watchUntil || !watching.isAlive() || watching.level() != level;
            if (over && getTarget() == null) setAggressive(false);
            if (over) watching = null;
            else if (getTarget() == null && distanceToSqr(watching) < 24 * 24) {
                getLookControl().setLookAt(watching);
                setAggressive(true);
            }
        }
        if (tickCount % 20 == 0) greetNearby(level, now);
        if (tickCount % 20 == 0 && raid != null) RaidGoal.update(this, level);
        if (tickCount % 20 == 0 && raid == null && visit == null && errand == null && tribe() != null && tribe().temperament.territorial) guardLand(level, now);
        if (home != null) tendFire(level, now);
        if (tickCount % 100 == 0 && raid == null && leaveAt < 0 && role() != TribeRole.MINER && !role().oak()) SetUpCampGoal.check(this, level, now);
        if (leaveAt >= 0 && now > leaveAt && tradingPlayer == null && tickCount % 20 == 0) Caravans.leave(this);
        if (getTarget() == null) tendWounds(level);
        restock(level); // elders, and envoys of any role
        if (getPose() == Pose.CROUCHING && !isWorking()) setPose(Pose.STANDING);
        if (getPose() == Pose.SLEEPING && !isSleeping()) setPose(Pose.STANDING);
        // the player's swimming pose while swimming somewhere (FloatGoal keeps the head above water, so nobody drowns)
        boolean swimming = isInWater() && !onGround() && getNavigation().isInProgress();
        if (swimming != (getPose() == Pose.SWIMMING)) setPose(swimming ? Pose.SWIMMING : Pose.STANDING);
        if (role().oak()) OakFolk.tick(this, level); // poses and the keeper's staff (OakFolkGoal does their day)
        else life.tick();
    }

    /** Client: the light-bearers' candle flames. */
    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide() && role() == TribeRole.OAK_LIGHTBEARER && getRandom().nextInt(3) == 0) {
            float yaw = yBodyRot * ((float) Math.PI / 180F);
            double fx = -Math.sin(yaw) * 0.55, fz = Math.cos(yaw) * 0.55, side = getRandom().nextBoolean() ? 0.3 : -0.3;
            level().addParticle(net.minecraft.core.particles.ParticleTypes.SMALL_FLAME, getX() + fx + Math.cos(yaw) * side, getY() + 1.45,
                    getZ() + fz + Math.sin(yaw) * side, 0, 0.005, 0);
        }
    }

    /** A mob's walking input is small (speed 0.3 times the goal's modifier), which in water crawls along: scaled up it swims about as fast as a player. */
    @Override
    protected void travelInWater(Vec3 input, double baseGravity, boolean isFalling, double oldY) {
        super.travelInWater(input.scale(SWIM_BOOST), baseGravity, isFalling, oldY);
    }

    /** Out of a fight, people heal slowly and now and then pull out a stuck arrow. */
    private void tendWounds(ServerLevel level) {
        if (tickCount % 60 == 0 && getHealth() < getMaxHealth()) heal(1.0F);
        List<ItemStack> stuck = getData(com.mechanical.ArrowWounds.STUCK);
        if (stuck.isEmpty() || tickCount % 600 != 0) return;
        spawnAtLocation(level, stuck.get(0));
        setData(com.mechanical.ArrowWounds.STUCK, List.copyOf(stuck.subList(1, stuck.size())));
        setArrowCount(stuck.size() - 1);
    }

    private boolean working;

    /** Kneels while picking berries or butchering. */
    void setWorking(boolean working) {
        this.working = working;
        setPose(working ? Pose.CROUCHING : Pose.STANDING);
    }

    boolean isWorking() {
        return working;
    }

    /** Free for daytime work: not fighting, trading or heading home. */
    boolean canWork(TribeRole needed) {
        return role() == needed && getTarget() == null && tradingPlayer == null && !wantsHome && !level().isDarkOutside();
    }

    /** Where work is looked for: around the camp, or where a band is. */
    BlockPos workCenter() {
        return home != null ? home : blockPosition();
    }

    /** A wall round home: how far out it stands, and a spot on the path just outside its gate. */
    record Wall(double radius, BlockPos gate) {}

    private Wall wall;
    private BlockPos wallFor;

    /** The wall round home: a tribe village's palisade, or a settled camp's once it goes up; null if none. Looked up per home. */
    Wall wall() {
        if (home == null || !(level() instanceof ServerLevel level)) return null;
        if (!home.equals(wallFor) || wall == null && tickCount % 200 == 0) {
            wallFor = home;
            var village = com.mechanical.worldgen.TribeVillageStructure.villageAt(level, home);
            Neighbours.Settlement s = village == null ? Settlements.of(this) : null;
            wall = village != null ? new Wall(com.mechanical.worldgen.TribeVillageStructure.WALL_R, village.trailEnd(-1))
                    : s != null && s.stage >= 2 && s.centre.equals(home) ? new Wall(Settlements.PALISADE_R, s.centre.relative(s.gate, Settlements.PALISADE_R + 2))
                    : null;
        }
        return wall;
    }

    /** For game tests: a wall round the current home. */
    void setWall(Wall wall) {
        this.wall = wall;
        wallFor = home;
    }

    /** How far round home the camp itself reaches (its buildings, its wall): nothing in it is felled, work is looked for beyond it. */
    public int campRadius() {
        if (home == null) return 0;
        Wall w = wall();
        return w != null ? (int) Math.ceil(w.radius()) + 2 : 5;
    }

    /** Outside the camp round home (anywhere, for a band without a home). */
    boolean outsideCamp(BlockPos p) {
        if (home == null) return true;
        double dx = p.getX() - home.getX(), dz = p.getZ() - home.getZ(), r = campRadius();
        return dx * dx + dz * dz > r * r;
    }

    private boolean insideWall(Wall w, Vec3 p) {
        double dx = p.x - home.getX() - 0.5, dz = p.z - home.getZ() - 0.5;
        return dx * dx + dz * dz < w.radius() * w.radius();
    }

    /**
     * Walks toward {@code target}; when the wall round home lies between, first to the gate (from inside or outside), and
     * outside it round the wall an eighth of the way at a time (a path straight at a spot behind the wall runs out of
     * search inside it and ends against the wall).
     */
    void walkTo(BlockPos target, double speed) {
        Wall w = wall();
        Vec3 to = Vec3.atBottomCenterOf(target), at = position();
        if (w != null) {
            if (insideWall(w, at) != insideWall(w, to) && distanceToSqr(Vec3.atBottomCenterOf(w.gate())) > 2.5 * 2.5) to = Vec3.atBottomCenterOf(w.gate());
            double hx = home.getX() + 0.5, hz = home.getZ() + 0.5;
            if (!insideWall(w, at) && !insideWall(w, to) && segmentDistance(hx, hz, at, to) < w.radius() + 1) {
                double from = Math.atan2(at.z - hz, at.x - hx), d = Math.atan2(to.z - hz, to.x - hx) - from;
                d = Math.atan2(Math.sin(d), Math.cos(d)); // the short way round
                double a = from + Math.signum(d) * Math.min(Math.abs(d), Math.PI / 4), r = w.radius() + 3;
                BlockPos round = level().getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        BlockPos.containing(hx + Math.cos(a) * r, 0, hz + Math.sin(a) * r));
                to = Vec3.atBottomCenterOf(round);
            }
        }
        getNavigation().moveTo(to.x, to.y, to.z, speed);
    }

    /** Horizontal distance from (x, z) to the segment a-b. */
    private static double segmentDistance(double x, double z, Vec3 a, Vec3 b) {
        double vx = b.x - a.x, vz = b.z - a.z, len = vx * vx + vz * vz;
        double t = len == 0 ? 0 : Math.max(0, Math.min(1, ((x - a.x) * vx + (z - a.z) * vz) / len));
        double dx = a.x + t * vx - x, dz = a.z + t * vz - z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** Out on a job (gathering, fishing, felling, planting), however far: ReturnHomeGoal leaves them to it. */
    boolean atWork() {
        for (var g : goalSelector.getAvailableGoals()) {
            if (g.isRunning() && (g.getGoal() instanceof GatherGoal || g.getGoal() instanceof FishGoal || g.getGoal() instanceof ChopWoodGoal
                    || g.getGoal() instanceof PlantGoal)) return true;
        }
        return false;
    }

    /** Group members around catch up on who the group is angry with (members that were unloaded when it happened). */
    private void shareAnger() {
        long now = level().getGameTime();
        anger.values().removeIf(t -> t <= now);
        for (Tribesperson t : groupNearby(32)) t.anger.forEach((k, v) -> { if (v > now) anger.merge(k, v, Math::max); });
    }

    /** Last time any territorial tribe warned each player off. */
    private static final Map<Player, Long> WARNED = new WeakHashMap<>();

    /** Territorial tribes warn strangers (standing below friendly) off their land, and attack if they stay. */
    private void guardLand(ServerLevel level, long now) {
        double range = home != null ? 20 : 10;
        BlockPos centre = home != null ? home : blockPosition();
        Player player = level.getNearestPlayer(centre.getX() + 0.5, centre.getY(), centre.getZ() + 0.5, range, false);
        if (player == null || player.isCreative() || player.isSpectator() || isAngryAt(player) || !hasLineOfSight(player)
                || tribe().reputation(player) >= Tribe.FRIENDLY) return;
        long warned = WARNED.getOrDefault(player, -10000L);
        if (now - warned > 1200) {
            WARNED.put(player, now);
            say(player, Component.translatable("chat.mechanical.tribesperson.warn"));
        } else if (now - warned > 100) {
            angerGroup(player, now + ANGER_TICKS / 4, 0);
        }
    }

    private void greetNearby(ServerLevel level, long now) {
        if (isAngry() || getTarget() != null) return;
        Player player = level.getNearestPlayer(this, GREET_RANGE);
        if (player == null || player.isSpectator() || !hasLineOfSight(player)) return;
        if (greeted.getOrDefault(player.getUUID(), -GREET_COOLDOWN * 2L) + GREET_COOLDOWN > now) return;
        if (LAST_GREETED.getOrDefault(player, -GREET_COOLDOWN_ANY * 2L) + GREET_COOLDOWN_ANY > now) return;
        if (getRandom().nextInt(4) != 0) return;
        greeted.put(player.getUUID(), now);
        LAST_GREETED.put(player, now);
        getLookControl().setLookAt(player);
        int standing = tribe() != null ? tribe().reputation(player) : 0;
        String key = standing >= Tribe.FRIENDLY && getRandom().nextBoolean() ? "chat.mechanical.tribesperson.greet.friend"
                : level.isDarkOutside() && home != null ? "chat.mechanical.tribesperson.greet.night"
                : "chat.mechanical.tribesperson.greet." + role().id() + "." + getRandom().nextInt(TribeRole.GREETINGS);
        say(player, Component.translatable(key));
    }

    /** A line in chat from this person: "&lt;Name&gt; text". */
    void say(Player player, Component line) {
        player.sendSystemMessage(Component.translatable("chat.mechanical.tribesperson.says", tribeName, line));
        playSound(TribeSounds.GREET, 0.6F, 0.9F + getRandom().nextFloat() * 0.2F);
    }

    /** Camp folk keep their fire going: wood from the woodcutter, and someone tops it up every minute or relights it. */
    private void tendFire(ServerLevel level, long now) {
        if (distanceToSqr(Vec3.atCenterOf(home)) > 4 * 4) return;
        BlockState state = level.getBlockState(home);
        if (!state.is(Blocks.CAMPFIRE) || !(level.getBlockEntity(home) instanceof CampfireAccess fire)) return; // fire taken away
        boolean lit = state.getValue(CampfireBlock.LIT);
        if (!carryingWood && (now < nextFireTend || (role() != TribeRole.ELDER && lit))) return;
        nextFireTend = now + FIRE_TEND_TICKS;
        getLookControl().setLookAt(Vec3.atCenterOf(home));
        swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT);
        if (carryingWood) Settlements.deliver(this, Neighbours.Settlement.WOOD, Settlements.WOOD_PER_TRIP);
        if (lit) {
            fire.mechanical$addFuel(carryingWood ? Campfires.FIREWOOD_TICKS * 2 : Campfires.FIREWOOD_TICKS);
        } else {
            BlockState relit = state.setValue(CampfireBlock.LIT, true).trySetValue(com.mechanical.Campfires.EMBERS, false);
            if (relit.hasProperty(Campfires.EMBERS)) relit = relit.setValue(Campfires.EMBERS, false);
            level.setBlock(home, relit, Block.UPDATE_ALL);
            fire.mechanical$relight();
        }
        level.playSound(null, home, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.5F, 1.4F);
        carryingWood = false;
    }

    // ---- greetings on hand, trading ----

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isAlive() || player.getItemInHand(hand).is(People.TRIBESPERSON_SPAWN_EGG.get()) || player.getItemInHand(hand).is(People.MINER_SPAWN_EGG.get()))
            return super.mobInteract(player, hand);
        if (level().isClientSide()) return InteractionResult.SUCCESS;
        if (isAngryAt(player) || anger.getOrDefault(player.getUUID(), 0L) > level().getGameTime()) {
            say(player, Component.translatable("chat.mechanical.tribesperson.no_trade"));
            return InteractionResult.CONSUME;
        }
        // food is a gift (to an elder: while sneaking, else it is trade); gifts raise the standing with the tribe
        ItemStack held = player.getItemInHand(hand);
        if (held.has(net.minecraft.core.component.DataComponents.FOOD) && (!barters() || player.isShiftKeyDown()) && tribe() != null) {
            if (level().getGameTime() < nextGift) return InteractionResult.CONSUME;
            nextGift = level().getGameTime() + 20;
            held.consume(1, player);
            tribe().addReputation(player, Tribe.GIFT);
            say(player, Component.translatable("chat.mechanical.tribesperson.gift"));
            return InteractionResult.SUCCESS_SERVER;
        }
        if (role() == TribeRole.OAK_KEEPER && hand == InteractionHand.MAIN_HAND && player instanceof net.minecraft.server.level.ServerPlayer sp) {
            getNavigation().stop();
            getLookControl().setLookAt(player);
            OakQuests.open(sp, this);
            return InteractionResult.SUCCESS_SERVER;
        }
        if (isEnvoy() && hand == InteractionHand.MAIN_HAND && tradingPlayer == null && getTarget() == null && player instanceof net.minecraft.server.level.ServerPlayer sp) {
            getNavigation().stop();
            getLookControl().setLookAt(player);
            TribeVisits.openDialog(sp, this);
            return InteractionResult.SUCCESS_SERVER;
        }
        if (barters() && tribe() != null && !tribe().trades(player)) {
            say(player, Component.translatable("chat.mechanical.tribesperson.no_trade_stranger"));
            return InteractionResult.CONSUME;
        }
        if (!barters() || tradingPlayer != null || getTarget() != null) {
            if (hand == InteractionHand.MAIN_HAND) say(player, Component.translatable("chat.mechanical.tribesperson.greet." + role().id() + "." + getRandom().nextInt(TribeRole.GREETINGS)));
            return InteractionResult.CONSUME;
        }
        openTrade(player);
        return InteractionResult.SUCCESS_SERVER;
    }

    /** Elders and the Miner trade when right-clicked (the others greet). */
    boolean barters() {
        return role() == TribeRole.ELDER || role() == TribeRole.MINER;
    }

    /** A visiting band's leader, or a camp's trader on the way to a player camp: they talk through TribeDialog. */
    public boolean isEnvoy() {
        return visit != null && leader || errand != null && errand.trade() || role() == TribeRole.TRADER
                || leader && visit == null && Settlements.of(this) != null; // a settled camp's leader talks roads (RoadBuilding)
    }

    /** Opens the barter screen, prices following the player's standing (not while angry or busy, nor for strangers they won't trade with). */
    void openTrade(Player player) {
        if (tradingPlayer != null || getTarget() != null || isAngryAt(player) || tribe() != null && !tribe().trades(player)) return;
        getNavigation().stop();
        for (MerchantOffer offer : getOffers())
            offer.setSpecialPriceDiff(tribe() != null && !Caravans.isExchange(offer) ? tribe().priceDiff(player, offer.getBaseCostA().getCount()) : 0);
        setTradingPlayer(player);
        openTradingScreen(player, getDisplayName(), 1);
    }

    /** One elder in this many offers seed potatoes (in a trading tribe one in {@link #POTATO_ODDS_TRADERS}). */
    public static final int POTATO_ODDS = 8, POTATO_ODDS_TRADERS = 3;

    /** All barter offers an elder may have; each elder gets a random handful. */
    private static List<MerchantOffer> allOffers() {
        return List.of(
                new MerchantOffer(new ItemCost(Mechanical.RAW_HIDE.get(), 6), new ItemStack(Mechanical.BONE_NEEDLE.get()), 4, 0, 0),
                new MerchantOffer(new ItemCost(Items.FLINT, 10), new ItemStack(Mechanical.FLINT_KNIFE.get()), 3, 0, 0),
                new MerchantOffer(new ItemCost(Foraging.BERRIES.get("raspberry").get(), 12), new ItemStack(Mechanical.COOKED_GAME_MEAT_SCRAPS.get(), 4), 6, 0, 0),
                new MerchantOffer(new ItemCost(Foraging.BERRIES.get("blueberry").get(), 12), new ItemStack(com.mechanical.wildlife.Wildlife.COOKED_VENISON_SCRAPS.get(), 4), 6, 0, 0),
                new MerchantOffer(new ItemCost(Mechanical.CORDAGE.get(), 6), new ItemStack(Mechanical.BONE_ARROW.get(), 8), 4, 0, 0),
                new MerchantOffer(new ItemCost(Mechanical.BARKS.get("oak").get(), 12), new ItemStack(Mechanical.TWINE.get(), 4), 6, 0, 0),
                new MerchantOffer(new ItemCost(Mechanical.SCRAPED_HIDE.get(), 3), new ItemStack(Items.LEATHER, 2), 4, 0, 0),
                new MerchantOffer(new ItemCost(Mechanical.WHOLE_COD.get(), 4), new ItemStack(Mechanical.CORDAGE.get(), 3), 6, 0, 0),
                new MerchantOffer(new ItemCost(com.mechanical.wildlife.Wildlife.SINEW.get(), 4), Optional.of(new ItemCost(Mechanical.CORDAGE.get(), 2)),
                        new ItemStack(Mechanical.CRUDE_BOW.get()), 2, 0, 0),
                new MerchantOffer(new ItemCost(com.mechanical.wildlife.Wildlife.TUSK.get(), 3), Optional.of(new ItemCost(Mechanical.CORDAGE.get(), 2)),
                        new ItemStack(Mechanical.TUSK_DAGGER.get()), 1, 0, 0),
                new MerchantOffer(new ItemCost(Mechanical.PLANT_FIBRE.get(), 16), new ItemStack(Mechanical.CORDAGE.get(), 2), 6, 0, 0),
                new MerchantOffer(new ItemCost(com.mechanical.wildlife.Wildlife.ANTLER.get(), 2), new ItemStack(Mechanical.BONE_FISH_HOOK.get(), 3), 4, 0, 0));
    }

    @Override
    public MerchantOffers getOffers() {
        if (offers == null && role() == TribeRole.TRADER) offers = Caravans.offers(0, getRandom()); // offers are made at spawn; a fallback
        if (offers == null && role() == TribeRole.MINER) {
            if (!(level() instanceof ServerLevel)) return new MerchantOffers();
            offers = Miners.offers(this); // from the world round him when first asked
        }
        if (offers == null) {
            offers = new MerchantOffers();
            List<MerchantOffer> all = new java.util.ArrayList<>(allOffers());
            java.util.Random random = new java.util.Random(getUUID().getLeastSignificantBits());
            java.util.Collections.shuffle(all, random);
            boolean traders = tribe() != null && tribe().temperament == Tribe.Temperament.TRADERS;
            offers.addAll(all.subList(0, traders ? 9 : 6));
            // potatoes come from far away (the only source, docs/farming.md): a few elders part with a handful of seed potatoes
            if (random.nextInt(traders ? POTATO_ODDS_TRADERS : POTATO_ODDS) == 0) offers.add(new MerchantOffer(new ItemCost(Items.LEATHER, 4),
                    Optional.of(new ItemCost(com.mechanical.wildlife.Wildlife.ANTLER.get(), 1)), new ItemStack(Items.POTATO, 3), 1, 0, 0));
        }
        return offers;
    }

    /** Offers come back each morning. */
    private void restock(ServerLevel level) {
        long day = level.getOverworldClockTime() / 24000;
        if (day == restockDay) return;
        restockDay = day;
        if (offers != null) offers.forEach(MerchantOffer::resetUses);
    }

    @Override
    public void setTradingPlayer(Player player) {
        tradingPlayer = player;
    }

    @Override
    public Player getTradingPlayer() {
        return tradingPlayer;
    }

    @Override
    public void overrideOffers(MerchantOffers offers) {}

    @Override
    public void notifyTrade(MerchantOffer offer) {
        offer.increaseUses();
        if (tradingPlayer != null && tribe() != null) tribe().addReputation(tradingPlayer, Tribe.TRADE);
        if (tradingPlayer instanceof net.minecraft.server.level.ServerPlayer sp && Caravans.isGem(offer.getBaseCostA())) TribeVisits.award(sp, "caravan_gem");
        if (tradingPlayer instanceof net.minecraft.server.level.ServerPlayer sp && role() == TribeRole.MINER) TribeVisits.award(sp, "miner_trade");
    }

    @Override
    public void notifyTradeUpdated(ItemStack stack) {}

    @Override
    public int getVillagerXp() {
        return 0;
    }

    @Override
    public void overrideXp(int xp) {}

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return TribeSounds.TRADE;
    }

    @Override
    public boolean isClientSide() {
        return level().isClientSide();
    }

    @Override
    public boolean stillValid(Player player) {
        return tradingPlayer == player && isAlive() && distanceToSqr(player) < 8 * 8;
    }

    // ---- bow ----

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        ItemStack bow = getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, item -> item instanceof BowItem));
        ItemStack arrowItem = new ItemStack(Items.ARROW);
        AbstractArrow arrow = ProjectileUtil.getMobArrow(this, arrowItem, power, bow);
        double dx = target.getX() - getX(), dy = target.getY(0.3333) - arrow.getY(), dz = target.getZ() - getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (level() instanceof ServerLevel level) {
            Projectile.spawnProjectileUsingShoot(arrow, level, arrowItem, dx, dy + dist * 0.2F, dz, 1.6F, 6.0F);
        }
        playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (getRandom().nextFloat() * 0.4F + 0.8F));
    }

    @Override
    public ItemStack getProjectile(ItemStack weapon) {
        return weapon.getItem() instanceof BowItem ? new ItemStack(Items.ARROW) : ItemStack.EMPTY;
    }

    // ---- sounds: their own set (TribeSounds), silent until its .ogg files are added (docs/tribe-sounds.md) ----

    @Override
    protected SoundEvent getAmbientSound() {
        return isSleeping() ? null : TribeSounds.AMBIENT;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 400; // about every 20 s, not the default 4
    }

    @Override
    public float getVoicePitch() {
        return 0.9F + getRandom().nextFloat() * 0.2F; // same range as say()
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return TribeSounds.HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return TribeSounds.DEATH;
    }

    // ---- data ----

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ROLE, 0);
        builder.define(SKIN, 0);
        builder.define(CLIMATE, ClimateZone.TEMPERATE.ordinal());
        builder.define(COLOUR, -1);
        builder.define(IDLE, 0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("role", entityData.get(ROLE));
        output.putInt("skin", skin());
        output.putInt("climate", climate().ordinal());
        output.putString("tribe_name", tribeName);
        if (home != null) output.store("camp", BlockPos.CODEC, home);
        if (group != null) output.store("group", UUIDUtil.CODEC, group);
        output.putBoolean("leader", leader);
        output.store("anger", ANGER_CODEC, anger);
        output.putLong("restock_day", restockDay);
        if (offers != null) output.store("offers", MerchantOffers.CODEC, offers);
        if (raid != null) raid.save(output);
        if (visit != null) output.store("visit", UUIDUtil.CODEC, visit);
        if (leaveAt >= 0) output.putLong("leave_at", leaveAt);
        if (oakFoot != null) output.store("oak", BlockPos.CODEC, oakFoot);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(ROLE, input.getIntOr("role", 0));
        entityData.set(SKIN, input.getIntOr("skin", 0));
        tribeName = input.getStringOr("tribe_name", "");
        home = input.read("camp", BlockPos.CODEC).orElse(null);
        group = input.read("group", UUIDUtil.CODEC).orElse(null);
        // people saved before climates dress as they did (temperate); the colour follows from the tribe
        entityData.set(CLIMATE, input.getIntOr("climate", ClimateZone.TEMPERATE.ordinal()));
        entityData.set(COLOUR, group == null ? -1 : Tribe.of(group).baseColour().getId());
        leader = input.getBooleanOr("leader", false);
        anger.clear();
        input.read("anger", ANGER_CODEC).ifPresent(anger::putAll);
        restockDay = input.getLongOr("restock_day", -1);
        offers = input.read("offers", MerchantOffers.CODEC).orElse(null);
        raid = RaidGoal.State.load(input);
        visit = input.read("visit", UUIDUtil.CODEC).orElse(null);
        leaveAt = input.getLongOr("leave_at", -1);
        oakFoot = input.read("oak", BlockPos.CODEC).orElse(null);
        if (role().oak()) entityData.set(COLOUR, -1);
        if (raid != null) raiderNavigation();
    }
}
