package com.mechanical.wildlife;

import com.mechanical.BloodMoon;
import com.mechanical.Mechanical;
import com.mechanical.NightDangers;

import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Night predator arrivals and the wolf howl sound. Besides the few packs/lions placed with the world (biome modifiers in
 * scripts/gen_predators.py), every 30 s at night each player in wolf country (biome tag {@code mechanical:has_grey_wolves})
 * has a WOLF_CHANCE to have a pack of 3-6 arrive 48-64 blocks away, if no grey wolves are already within 96 blocks; the
 * pack howls soon after. Half the packs are hunting and have the player's scent from the start. Lion country
 * ({@code mechanical:has_cave_lions}) gets a lion (or a pair) the same way, rarer. Not within 28 blocks of a burning fire.
 * Over a 10-minute night that is ~70 % for at least one pack in taiga (20 rolls at 6 %).
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class Predators {
    public static final SoundEvent WOLF_HOWL = SoundEvent.createFixedRangeEvent(Identifier.fromNamespaceAndPath(Mechanical.MODID, "entity.grey_wolf.howl"), 128.0F);
    public static final TagKey<Biome> WOLF_BIOMES = TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(Mechanical.MODID, "has_grey_wolves"));
    public static final TagKey<Biome> LION_BIOMES = TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(Mechanical.MODID, "has_cave_lions"));
    private static final int INTERVAL = 600;
    private static final float WOLF_CHANCE = 0.06F, LION_CHANCE = 0.025F, BLOOD_MOON_FACTOR = 2.0F;
    private static final float HUNTING_SHARE = 0.5F;
    private static final double NO_SPAWN_FIRE = 28, CROWDED = 96;
    /** A carcass within this many blocks of a player doubles the arrival chance. */
    private static final double CARCASS_RANGE = 16;
    private static final float CARCASS_FACTOR = 2.0F;

    @SubscribeEvent
    static void register(RegisterEvent event) {
        event.register(Registries.SOUND_EVENT, WOLF_HOWL.location(), () -> WOLF_HOWL);
    }

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD || level.getGameTime() % INTERVAL != 0
                || !level.isDarkOutside() || !level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS)) return;
        RandomSource random = level.getRandom();
        float factor = BloodMoon.isActive(level) ? BLOOD_MOON_FACTOR : 1.0F;
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.isCreative()) continue;
            var biome = level.getBiome(player.blockPosition());
            // the smell of a carcass nearby (carried, on a travois, hung at camp) draws them: twice as likely
            float f = factor * (level.getEntitiesOfClass(com.mechanical.CarcassEntity.class, player.getBoundingBox().inflate(CARCASS_RANGE)).isEmpty() ? 1 : CARCASS_FACTOR);
            if (biome.is(WOLF_BIOMES) && random.nextFloat() < WOLF_CHANCE * f) {
                spawnGroup(level, player.position(), player, Wildlife.GREY_WOLF.get(), 3 + random.nextInt(4));
            } else if (biome.is(LION_BIOMES) && random.nextFloat() < LION_CHANCE * f) {
                spawnGroup(level, player.position(), player, Wildlife.CAVE_LION.get(), random.nextInt(3) == 0 ? 2 : 1);
            }
        }
    }

    /**
     * A group of {@code count} 48-64 blocks from {@code at}, on the dark, dry surface; half the time hunting {@code quarry}
     * (may be null). Returns how many were placed.
     */
    public static int spawnGroup(ServerLevel level, Vec3 at, ServerPlayer quarry, EntityType<? extends Predator> type, int count) {
        RandomSource random = level.getRandom();
        if (!level.getEntitiesOfClass(Predator.class, new AABB(at, at).inflate(CROWDED), p -> p.getType() == type).isEmpty()) return 0;
        double angle = random.nextDouble() * Math.PI * 2, dist = 48 + random.nextInt(16);
        BlockPos center = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                BlockPos.containing(at.x + Math.cos(angle) * dist, at.y, at.z + Math.sin(angle) * dist));
        if (Math.abs(center.getY() - at.y) > 24 || NightDangers.nearFire(level, Vec3.atCenterOf(center), NO_SPAWN_FIRE)) return 0;
        boolean hunting = random.nextFloat() < HUNTING_SHARE;
        int placed = 0;
        for (int i = 0; i < count * 3 && placed < count; i++) {
            BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, center.offset(random.nextInt(7) - 3, 0, random.nextInt(7) - 3));
            if (level.getBrightness(LightLayer.BLOCK, pos) > 0 || !level.getFluidState(pos).isEmpty() || !level.getFluidState(pos.below()).isEmpty()
                    || !level.getBlockState(pos.below()).isSolid()) continue;
            Predator p = type.create(level, EntitySpawnReason.NATURAL);
            if (p == null) return placed;
            p.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360, 0);
            if (!level.noCollision(p)) continue;
            p.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.NATURAL, null);
            p.addTag(Predator.NIGHT_TAG);
            if (hunting && quarry != null) p.setTarget(quarry);
            if (placed == 0 && p instanceof GreyWolf wolf) wolf.howlSoon();
            level.addFreshEntity(p);
            placed++;
        }
        return placed;
    }

    /** /nightpredators wolf|lion: a night arrival round the command's position now (for testing; ignores the time of day). */
    @SubscribeEvent
    static void registerCommands(RegisterCommandsEvent event) {
        for (String kind : new String[] {"wolf", "lion"}) {
            event.getDispatcher().register(Commands.literal("nightpredators").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .then(Commands.literal(kind).executes(ctx -> {
                        boolean wolf = kind.equals("wolf");
                        ServerLevel level = ctx.getSource().getLevel();
                        int n = spawnGroup(level, ctx.getSource().getPosition(), ctx.getSource().getPlayer(),
                                wolf ? Wildlife.GREY_WOLF.get() : Wildlife.CAVE_LION.get(), wolf ? 3 + level.getRandom().nextInt(4) : 1);
                        ctx.getSource().sendSuccess(() -> Component.literal("Spawned " + n + " " + (wolf ? (n == 1 ? "wolf" : "wolves") : (n == 1 ? "lion" : "lions"))), true);
                        return n;
                    })));
        }
    }

    private Predators() {}
}
