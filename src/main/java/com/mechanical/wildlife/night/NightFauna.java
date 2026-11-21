package com.mechanical.wildlife.night;

import static com.mechanical.Mechanical.ENTITY_TYPES;
import static com.mechanical.Mechanical.ITEMS;
import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.function.Supplier;

import com.mechanical.Blood;
import com.mechanical.Butchery;
import com.mechanical.KnifeItem;
import com.mechanical.Mechanical;
import com.mechanical.Sanity;
import com.mechanical.wildlife.Wildlife;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The night's own monsters, in place of vanilla's Creeper (which never enters the world: every creeper is cancelled on
 * joining and a new one is replaced by one of these two). Neither breaks a block.
 * - {@link Gloson}: the grey sow of folklore, a real animal (carcass, butchered for Gloson Bristles, meat, hide, bone and
 *   the Razor Ridge) that circles you in the dark and charges: a {@link #BLEEDING} gash and a hard knock.
 * - {@link MushroomMan}: a walking fungus that creeps up, hisses and swells, and bursts into a spore cloud
 *   ({@link #sporeBurst}): Blindness, Nausea and a blow to the mind (Sanity). Drops Mushroom Flesh (tinder) and the Spore
 *   Sac, which with a Puffball makes a throwable Spore Bomb (the same cloud).
 * Data: scripts/gen_fauna_night.py, textures: scripts/gen_fauna_night_textures.py, docs: docs/fauna/night.md.
 */
@EventBusSubscriber(modid = MODID)
public final class NightFauna {
    static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, MODID);
    /** A gash: half a heart every 2 s and blood dripping, but never below one heart. */
    public static final DeferredHolder<MobEffect, MobEffect> BLEEDING = MOB_EFFECTS.register("bleeding", BleedingEffect::new);

    public static final DeferredHolder<EntityType<?>, EntityType<Gloson>> GLOSON = ENTITY_TYPES.registerEntityType(
            "gloson", Gloson::new, MobCategory.CREATURE, b -> b.sized(0.9F, 0.9F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<MushroomMan>> MUSHROOM_MAN = ENTITY_TYPES.registerEntityType(
            "mushroom_man", MushroomMan::new, MobCategory.MONSTER, b -> b.sized(0.7F, 1.4F).clientTrackingRange(8));

    // ---- the Gloson's carcass ----
    public static final DeferredItem<Item> RAW_GLOSON = food("raw_gloson", 2, 0.3F);
    public static final DeferredItem<Item> COOKED_GLOSON = food("cooked_gloson", 7, 0.7F);
    public static final DeferredItem<Item> GLOSON_BRISTLES = ITEMS.registerSimpleItem("gloson_bristles");
    /** The razor-sharp crest off its back. */
    public static final DeferredItem<Item> RAZOR_RIDGE = ITEMS.registerSimpleItem("razor_ridge");
    public static final ToolMaterial RAZOR = new ToolMaterial(BlockTags.INCORRECT_FOR_WOODEN_TOOL, 120, 3.0F, 0.5F, 8,
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "repairs_razor_knife")));
    /** The Razor Ridge lashed to a haft: a fast, keen flint-tier knife that gets a little more off every carcass. */
    public static final DeferredItem<KnifeItem> RAZOR_KNIFE = ITEMS.registerItem("razor_knife", p -> new KnifeItem(p, 1.3F, 1.15F, KnifeItem.FLINT),
            p -> p.sword(RAZOR, 1.5F, -2.0F));

    // ---- the Mushroom Man's parts ----
    /** Dry, corky fungus flesh: tinder (Tinder). */
    public static final DeferredItem<Item> MUSHROOM_FLESH = ITEMS.registerSimpleItem("mushroom_flesh");
    public static final DeferredItem<Item> SPORE_SAC = ITEMS.registerSimpleItem("spore_sac");
    /** Thrown like a snowball; bursts into a spore cloud where it lands (onImpact). */
    public static final DeferredItem<SnowballItem> SPORE_BOMB = ITEMS.registerItem("spore_bomb", SnowballItem::new, p -> p.stacksTo(16));

    public static final DeferredItem<SpawnEggItem> GLOSON_SPAWN_EGG = ITEMS.registerItem("gloson_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(GLOSON.get()));
    public static final DeferredItem<SpawnEggItem> MUSHROOM_MAN_SPAWN_EGG = ITEMS.registerItem("mushroom_man_spawn_egg", SpawnEggItem::new,
            p -> p.spawnEgg(MUSHROOM_MAN.get()));

    private static DeferredItem<Item> food(String name, int nutrition, float saturation) {
        return ITEMS.registerSimpleItem(name, p -> p.food(new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).build()));
    }

    /** Loads this class so its registrations happen, and registers the Gloson's butchering yield. Call from the mod constructor. */
    public static void init(IEventBus modEventBus) {
        MOB_EFFECTS.register(modEventBus);
        Supplier<Item> bone = () -> Items.BONE;
        // first pass: bristles and the hide; second: meat, the ridge (always exactly one: chance 1 is never scaled) and bones
        Butchery.register(GLOSON::get, new Butchery.Yield(RAW_GLOSON, 1, 3,
                List.of(new Butchery.Extra(GLOSON_BRISTLES, 1.0F, 1, 2), new Butchery.Extra(Mechanical.RAW_HIDE, 0.4F, 1, 1)),
                List.of(new Butchery.Extra(RAZOR_RIDGE, 1.0F, 1, 1), new Butchery.Extra(Wildlife.SINEW, 0.3F, 1, 1), new Butchery.Extra(bone, 0.5F, 1, 2))));
    }

    @SubscribeEvent
    static void attributes(EntityAttributeCreationEvent event) {
        event.put(GLOSON.get(), Gloson.createAttributes().build());
        event.put(MUSHROOM_MAN.get(), MushroomMan.createAttributes().build());
    }

    /** The Mushroom Man spawns like any monster (dark ground); its extra weight in forests and swamps is data (gen_fauna_night.py). */
    @SubscribeEvent
    static void spawnRules(RegisterSpawnPlacementsEvent event) {
        event.register(MUSHROOM_MAN.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    // ---- no creepers ----

    /** Every creeper is kept out of the world (old saves too); a newly spawned one is replaced by a Gloson or a Mushroom Man. */
    @SubscribeEvent
    static void onJoin(EntityJoinLevelEvent event) {
        Entity e = event.getEntity();
        if (e.getType() != EntityTypes.CREEPER) return;
        event.setCanceled(true);
        if (!event.loadedFromDisk() && event.getLevel() instanceof ServerLevel level) {
            Mob mob = create(level, e.blockPosition(), EntitySpawnReason.NATURAL);
            if (mob != null) {
                mob.snapTo(e.getX(), e.getY(), e.getZ(), e.getYRot(), 0);
                level.addFreshEntity(mob);
            }
        }
    }

    /** Forest, swamp and wet ground, or out of the sky (caves): where the Mushroom Man is the likelier of the two. */
    static boolean damp(ServerLevel level, BlockPos pos) {
        var biome = level.getBiome(pos);
        return !level.canSeeSky(pos) || biome.is(BiomeTags.IS_FOREST) || biome.is(BiomeTags.IS_TAIGA) || biome.is(BiomeTags.IS_JUNGLE)
                || biome.is(Tags.Biomes.IS_SWAMP) || biome.is(Tags.Biomes.IS_WET_OVERWORLD);
    }

    /**
     * The creeper's share of the night: a Gloson or a Mushroom Man (finalized, not yet added). Under the open sky in dry,
     * open land mostly Glosons (60 %); in forest, swamp and wet land mostly Mushroom Men (70 %); out of the sky (caves)
     * always the Mushroom Man (the sow keeps to the surface).
     */
    public static Mob create(ServerLevel level, BlockPos pos, EntitySpawnReason reason) {
        float fungus = !level.canSeeSky(pos) ? 1.0F : damp(level, pos) ? 0.7F : 0.4F;
        EntityType<? extends Mob> type = level.getRandom().nextFloat() < fungus ? MUSHROOM_MAN.get() : GLOSON.get();
        Mob mob = type.create(level, reason);
        if (mob == null) return null;
        mob.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360, 0);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), reason, null);
        return mob;
    }

    // ---- spores ----

    static final float CLOUD_RADIUS = 3.0F, BURST_DAMAGE = 2.0F, SANITY_LOSS = 15;
    static final int BLIND_TICKS = 100, NAUSEA_TICKS = 200;

    /**
     * A puff of spores at {@code at}: a short-lived cloud of Blindness and Nausea, every player within reach loses
     * {@link #SANITY_LOSS} sanity, mobs caught in it lose sight of their target, and (from a Mushroom Man, not a thrown
     * bomb) everything within 2.5 blocks takes {@link #BURST_DAMAGE}. Never touches a block.
     */
    public static void sporeBurst(ServerLevel level, Vec3 at, Entity source, boolean hurts) {
        AreaEffectCloud cloud = new AreaEffectCloud(level, at.x, at.y, at.z);
        cloud.setRadius(CLOUD_RADIUS);
        cloud.setRadiusOnUse(-0.3F);
        cloud.setWaitTime(0);
        cloud.setDuration(120);
        cloud.setRadiusPerTick(-CLOUD_RADIUS / 120);
        cloud.setCustomParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xFFCBBF8E));
        cloud.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLIND_TICKS));
        cloud.addEffect(new MobEffectInstance(MobEffects.NAUSEA, NAUSEA_TICKS));
        if (source instanceof LivingEntity owner) cloud.setOwner(owner);
        level.addFreshEntity(cloud);
        level.sendParticles(ParticleTypes.WHITE_ASH, at.x, at.y + 0.8, at.z, 120, 1.2, 0.8, 1.2, 0.02);
        level.sendParticles(ParticleTypes.POOF, at.x, at.y + 0.8, at.z, 20, 0.6, 0.5, 0.6, 0.05);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.PUFFER_FISH_BLOW_OUT, SoundSource.HOSTILE, 1.5F, 0.5F);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(CLOUD_RADIUS + 1))) {
            if (e == source || e instanceof MushroomMan || e.distanceToSqr(at) > (CLOUD_RADIUS + 1) * (CLOUD_RADIUS + 1)) continue;
            if (e instanceof Player p && !p.isCreative() && !p.isSpectator()) Sanity.set(p, Sanity.of(p) - SANITY_LOSS);
            if (e instanceof Mob m) m.setTarget(null);
            if (hurts && e.distanceToSqr(at) < 2.5 * 2.5) {
                e.hurtServer(level, source instanceof LivingEntity l ? level.damageSources().mobAttack(l) : level.damageSources().generic(), BURST_DAMAGE);
            }
        }
    }

    /** A thrown Spore Bomb bursts where it lands. */
    @SubscribeEvent
    static void onImpact(ProjectileImpactEvent event) {
        if (event.getProjectile() instanceof Snowball ball && ball.getItem().is(SPORE_BOMB.get()) && ball.level() instanceof ServerLevel level) {
            sporeBurst(level, event.getRayTraceResult().getLocation(), ball.getOwner(), false);
        }
    }

    static final class BleedingEffect extends MobEffect {
        BleedingEffect() {
            super(MobEffectCategory.HARMFUL, 0x9A1010);
        }

        @Override
        public boolean applyEffectTick(ServerLevel level, LivingEntity mob, int amplifier) {
            if (mob.getHealth() > 2.0F) mob.hurtServer(level, level.damageSources().generic(), Math.min(1.0F + amplifier, mob.getHealth() - 2.0F));
            Blood.spray(level, mob.getX(), mob.getY() + mob.getBbHeight() * 0.5, mob.getZ(), 4);
            return true;
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
            return tickCount % 40 == 0;
        }
    }

    private NightFauna() {}
}
