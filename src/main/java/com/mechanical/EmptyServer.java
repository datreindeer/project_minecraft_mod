package com.mechanical;

import com.mojang.logging.LogUtils;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The world lives on while nobody is online (docs/living-world-plan.md A1): a dedicated server keeps ticking instead of
 * pausing (vanilla's pause-when-empty-seconds is set to 0), but at half the tick rate, so the whole world runs at half
 * speed: the clock and so the calendar, seasons and world age (Calendar reads them off the clock), and everything in the
 * chunks tribe Fire Pits keep loaded (Clans#keepPitsLoaded), crops, furnaces and spoiling alike. A game day takes 2 h.
 * Halving the tick rate rather than the clock keeps game-time durations (spoiling, kilns) in step with the calendar,
 * and halves the work an empty server does. The rate is back to normal the tick someone logs in.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class EmptyServer {
    public static final float NORMAL = 20, EMPTY = NORMAL / 2;
    /** Whether we slowed the server, so a rate an op set by hand while players are on is left alone. */
    private static boolean slowed;

    private EmptyServer() {}

    /** The tick rate to run at: half while empty (from the normal rate only), normal again once someone is on if we slowed it. */
    public static float target(float current, boolean empty, boolean slowed) {
        if (empty) return current == NORMAL ? EMPTY : current;
        return slowed && current == EMPTY ? NORMAL : current;
    }

    @SubscribeEvent
    static void onStarted(ServerStartedEvent event) {
        slowed = false;
        if (event.getServer() instanceof DedicatedServer server && server.pauseWhenEmptySeconds() > 0) {
            LogUtils.getLogger().info("Mechanical: pause-when-empty-seconds set to 0, the world runs on at half speed while nobody is online");
            server.setPauseWhenEmptySeconds(0);
        }
    }

    @SubscribeEvent
    static void onTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (!(server instanceof DedicatedServer)) return; // singleplayer pauses anyway; the game test server must run at full speed
        var rates = server.tickRateManager();
        boolean empty = server.getPlayerList().getPlayerCount() == 0;
        float rate = target(rates.tickrate(), empty, slowed);
        if (rate != rates.tickrate()) {
            rates.setTickRate(rate);
            slowed = empty;
        }
    }
}
