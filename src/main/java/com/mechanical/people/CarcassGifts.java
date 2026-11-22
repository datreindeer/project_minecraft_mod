package com.mechanical.people;

import java.util.HashMap;
import java.util.Map;

import com.mechanical.Butchery;
import com.mechanical.CarcassEntity;
import com.mechanical.clan.Clans;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * A carcass put down at an NPC tribe's camp or settlement (docs/living-world-plan.md C). At Neutral standing or better it
 * is a generous gift: + standing by its meat ({@link #worth}), thanks and happy sparks, and one of them comes to butcher it
 * or hang it on the camp's rack (ButcherCarcassGoal). Only the first gift a day to each tribe counts (per player tribe,
 * A4), so a pile of hares farms nothing. Below Neutral it reads as a threat: - standing, and their armed folk keep a
 * wary eye on the giver for a minute (Tribesperson#watch). Hooked in CarcassHauling#letGo.
 */
public final class CarcassGifts {
    /** How close to a camp fire (a camp folk's home) the body must be put down. */
    static final int RANGE = 12;
    static final int THREAT = -5, WARY_TICKS = 1200, MAX_WORTH = 10;
    /** "sharer/tribe" -> day of the last gift that counted. shortcut: not saved, a restart allows one more that day; save it if that is farmed. */
    static final Map<String, Long> LAST_GIFT = new HashMap<>();

    /** Standing a gift of this body is worth: its meat yield (Butchery), 1 for a hare up to {@link #MAX_WORTH} for the giants. */
    public static int worth(CarcassEntity carcass) {
        Butchery.Yield y = Butchery.yieldFor(carcass.animal());
        return Mth.clamp((y.minMeat() + y.maxMeat()) / 2, 1, MAX_WORTH);
    }

    /** The camp folk whose fire is nearest the body, within {@link #RANGE}; null when it lies at no camp. */
    static Tribesperson hostAt(ServerLevel level, CarcassEntity carcass) {
        Tribesperson best = null;
        double bestDist = RANGE * RANGE;
        for (Tribesperson t : level.getEntitiesOfClass(Tribesperson.class, carcass.getBoundingBox().inflate(RANGE + 8),
                t -> t.isAlive() && t.tribe() != null && t.home() != null && t.raid == null && t.visit == null)) {
            double d = t.home().distToCenterSqr(carcass.position());
            if (d <= bestDist) {
                bestDist = d;
                best = t;
            }
        }
        return best;
    }

    /** {@code giver} put {@code carcass} down; true when a tribe took it as a gift or a threat. */
    public static boolean left(CarcassEntity carcass, Player giver) {
        if (!(carcass.level() instanceof ServerLevel level) || carcass.gifted || carcass.haulerId() >= 0 || carcass.isCaravanProperty()
                || giver.isSpectator()) return false;
        Tribesperson host = hostAt(level, carcass);
        if (host == null) return false;
        Tribe tribe = host.tribe();
        var at = carcass.getBoundingBox().getCenter();
        if (tribe.reputation(giver) >= 0) {
            carcass.gifted = true;
            String key = Clans.sharerOf(giver) + "/" + tribe.id;
            long day = Clans.today(level.getServer());
            boolean counts = !Long.valueOf(day).equals(LAST_GIFT.get(key));
            if (counts) {
                LAST_GIFT.put(key, day);
                tribe.addReputation(giver, worth(carcass));
                if (giver instanceof ServerPlayer sp) TribeVisits.award(sp, "tribe_carcass_gift");
            }
            host.say(giver, Component.translatable(counts ? "chat.mechanical.tribesperson.carcass_gift" : "chat.mechanical.tribesperson.carcass_gift_again"));
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, at.x, at.y + 0.5, at.z, 12, 0.6, 0.4, 0.6, 0);
            // one of them comes over to cut it up, or carry it to the camp's rack
            host.killPos = carcass.position();
            host.killTime = level.getGameTime();
        } else {
            tribe.addReputation(giver, THREAT);
            host.say(giver, Component.translatable("chat.mechanical.tribesperson.carcass_threat"));
            level.sendParticles(ParticleTypes.ANGRY_VILLAGER, at.x, at.y + 0.5, at.z, 6, 0.6, 0.4, 0.6, 0);
            long until = level.getGameTime() + WARY_TICKS;
            for (Tribesperson t : host.groupNearby(32)) if (t.isArmed()) t.watch(giver, until);
        }
        return true;
    }

    private CarcassGifts() {}
}
