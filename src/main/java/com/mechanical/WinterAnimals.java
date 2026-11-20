package com.mechanical;

import com.mechanical.wildlife.Wildlife;
import com.mechanical.wildlife.waterfowl.WaterfowlFauna;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;

/**
 * Animals in winter (docs/seasons-and-tribes.md, "Djur"). Wild animals move slower and wander less (WildAnimal), no animal
 * mates (here), bears hibernate in a den (BrownBear), and migratory birds fly south in the last month of autumn and are
 * gone until spring (FlockBird; they don't spawn or restock meanwhile). Restocking is halved in winter (WildlifeRestock).
 * Advancements: Rude Awakening (waking a hibernating bear) and The Cranes Return (seeing migrants back in spring).
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class WinterAnimals {
    public static final Identifier RUDE_AWAKENING = Identifier.fromNamespaceAndPath(Mechanical.MODID, "wilderness/rude_awakening"),
            CRANES_RETURN = Identifier.fromNamespaceAndPath(Mechanical.MODID, "wilderness/cranes_return");
    /** Migratory birds are away from this month (Frostfall, the last of autumn) to the end of winter. */
    static final int LEAVE_MONTH = 8;

    private WinterAnimals() {}

    public static boolean winter(Level level) {
        return Calendar.season(level.getOverworldClockTime()) == 3;
    }

    /** Are the migratory birds away (late autumn and winter)? */
    public static boolean birdsAway(long clock) {
        return Calendar.month(clock) >= LEAVE_MONTH;
    }

    public static boolean birdsAway(Level level) {
        return birdsAway(level.getOverworldClockTime());
    }

    /** The migratory birds: Common Crane, Grey Heron, Goose and Mute Swan. Mallards and the rest stay all year. */
    public static boolean migrates(EntityType<?> type) {
        return type == WaterfowlFauna.COMMON_CRANE.get() || type == WaterfowlFauna.GREY_HERON.get() || type == Wildlife.GOOSE.get()
                || type == WaterfowlFauna.MUTE_SWAN.get();
    }

    /** No animal mates in winter: the pair's love just fades, and whoever fed them is told why. */
    @SubscribeEvent
    static void onBreed(BabyEntitySpawnEvent event) {
        if (!(event.getParentA() instanceof Animal parent) || !winter(parent.level())) return;
        event.setCanceled(true);
        if (event.getCausedByPlayer() instanceof ServerPlayer player) player.sendOverlayMessage(Component.translatable("message.mechanical.no_mating_in_winter"));
    }

    /** A player woke a bear from its winter sleep. */
    public static void wokeHibernatingBear(ServerPlayer player) {
        Kettle.award(player, RUDE_AWAKENING);
    }

    /** A migratory bird back in spring: players nearby who can see it get The Cranes Return. */
    public static void migrantBack(ServerLevel level, Entity bird) {
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, bird.getBoundingBox().inflate(32))) {
            if (player.hasLineOfSight(bird)) Kettle.award(player, CRANES_RETURN);
        }
    }
}
