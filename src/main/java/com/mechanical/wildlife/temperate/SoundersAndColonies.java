package com.mechanical.wildlife.temperate;

import java.util.List;

import com.mechanical.Calendar;
import com.mechanical.Mechanical;
import com.mechanical.wildlife.WildBoar;
import com.mechanical.wildlife.coast.CoastFauna;
import com.mechanical.wildlife.coast.Seal;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Two extensions of existing animals:
 * <ul>
 * <li><b>Wild boar sounders</b> in the dark forest ({@link #SOUNDER_BIOMES}): big groups (data: add_wild_boar_sounder), led by
 * a <b>tusker</b>, the first grown boar of the group: a third bigger, tougher and harder-hitting (permanent attribute
 * modifiers, so it stays a tusker after a reload). It doesn't wait to be hurt: a player within {@link #TUSKER_RANGE} who
 * doesn't sneak past is charged, and the sounder's grown boars charge with it.</li>
 * <li><b>Grey seal colonies</b> on rocky coasts (data: add_seal_colony): big groups hauled out on the rocks. Pups (white) are
 * born in spring only: no pups enter the world at other seasons, and in spring a colony near a player has pups
 * ({@link #pupping}) until about one in three is a pup.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class SoundersAndColonies {
    public static final TagKey<Biome> SOUNDER_BIOMES = TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(Mechanical.MODID, "has_boar_sounders"));
    static final Identifier TUSKER_ID = Identifier.fromNamespaceAndPath(Mechanical.MODID, "tusker");
    static final double TUSKER_RANGE = 7, TUSKER_RANGE_SNEAKING = 2.5, SOUNDER = 14, COLONY = 48;

    private SoundersAndColonies() {}

    public static boolean isTusker(WildBoar boar) {
        AttributeInstance scale = boar.getAttribute(Attributes.SCALE);
        return scale != null && scale.hasModifier(TUSKER_ID);
    }

    /** Makes a grown boar the sounder's tusker: bigger, tougher, harder-hitting, and it charges players on sight. */
    public static void makeTusker(WildBoar boar) {
        if (isTusker(boar) || boar.isBaby()) return;
        boar.getAttribute(Attributes.SCALE).addPermanentModifier(new AttributeModifier(TUSKER_ID, 0.3, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        boar.getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(new AttributeModifier(TUSKER_ID, 14.0, AttributeModifier.Operation.ADD_VALUE));
        boar.getAttribute(Attributes.ATTACK_DAMAGE).addPermanentModifier(new AttributeModifier(TUSKER_ID, 3.0, AttributeModifier.Operation.ADD_VALUE));
        boar.setHealth(boar.getMaxHealth());
        boar.targetSelector.addGoal(1, new TuskerChargeGoal(boar));
    }

    /** Spring (season 0): when seal pups are born. */
    public static boolean pupping(int season) {
        return season == 0;
    }

    @SubscribeEvent
    static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (event.getEntity() instanceof WildBoar boar) {
            if (isTusker(boar)) {
                boar.targetSelector.addGoal(1, new TuskerChargeGoal(boar));
            } else if (!event.loadedFromDisk() && !boar.isBaby() && level.getBiome(boar.blockPosition()).is(SOUNDER_BIOMES)
                    && level.getEntitiesOfClass(WildBoar.class, boar.getBoundingBox().inflate(SOUNDER), SoundersAndColonies::isTusker).isEmpty()) {
                makeTusker(boar);
            }
        } else if (event.getEntity() instanceof Seal seal && !event.loadedFromDisk() && seal.isBaby()
                && !seal.entityTags().contains(BORN) && !pupping(Calendar.season(level.getOverworldClockTime()))) {
            seal.setBaby(false); // a pup out of season: a yearling instead
        }
    }

    private static final String BORN = "mechanical:spring_pup";

    /** Spring: now and then a seal hauled out near a player has a pup, while fewer than a third of the colony are pups. */
    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD || level.getGameTime() % 1200 != 600
                || !pupping(Calendar.season(level.getOverworldClockTime()))) return;
        for (ServerPlayer player : level.players()) {
            List<Seal> colony = level.getEntitiesOfClass(Seal.class, player.getBoundingBox().inflate(COLONY));
            long pups = colony.stream().filter(Seal::isBaby).count();
            if (colony.size() < 3 || pups * 3 >= colony.size() || level.getRandom().nextInt(3) != 0) continue;
            colony.stream().filter(s -> !s.isBaby() && !s.isInWater() && s.onGround()).findAny().ifPresent(mother -> birth(level, mother));
        }
    }

    static Seal birth(ServerLevel level, Seal mother) {
        Seal pup = CoastFauna.SEAL.get().create(level, EntitySpawnReason.BREEDING);
        if (pup == null) return null;
        pup.addTag(BORN);
        pup.setBaby(true);
        pup.snapTo(mother.getX(), mother.getY(), mother.getZ(), mother.getYRot(), 0);
        level.addFreshEntity(pup);
        mother.playSound(SoundEvents.POLAR_BEAR_AMBIENT_BABY, 1.0F, 1.6F);
        return pup;
    }

    /** The tusker charges a player who comes close (sneaking: only very close), and its sounder charges with it. */
    static class TuskerChargeGoal extends TargetGoal {
        private final WildBoar boar;
        private Player player;

        TuskerChargeGoal(WildBoar boar) {
            super(boar, true);
            this.boar = boar;
        }

        @Override
        public boolean canUse() {
            if (boar.getTarget() != null || (boar.tickCount + boar.getId()) % 10 != 0) return false;
            player = boar.level().getNearestPlayer(boar, TUSKER_RANGE);
            return player != null && !player.isCreative() && !player.isSpectator()
                    && boar.distanceTo(player) < (player.isShiftKeyDown() ? TUSKER_RANGE_SNEAKING : TUSKER_RANGE) && boar.hasLineOfSight(player);
        }

        @Override
        public void start() {
            boar.setTarget(player);
            boar.playSound(SoundEvents.HOGLIN_ANGRY, 1.2F, 0.8F);
            for (WildBoar o : boar.level().getEntitiesOfClass(WildBoar.class, boar.getBoundingBox().inflate(SOUNDER), o -> o != boar && !o.isBaby())) o.setTarget(player);
            super.start();
        }
    }
}
