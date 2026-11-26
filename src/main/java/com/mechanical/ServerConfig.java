package com.mechanical;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server options (the "LOCAL" config: config/mechanical-local.toml): the admin dashboard (dashboard.Dashboard) and the closed
 * Nether and End (DimensionLock). */
public final class ServerConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec.BooleanValue DASHBOARD = BUILDER
            .comment("Run the admin web dashboard (dedicated servers only; local only; if ever proxied, the proxy must send Host: 127.0.0.1:<port>, e.g. Caddy header_up Host 127.0.0.1:8100, and add its own login)")
            .define("dashboard", true);
    public static final ModConfigSpec.IntValue DASHBOARD_PORT = BUILDER
            .comment("Port of the admin web dashboard")
            .defineInRange("dashboardPort", 8100, 1024, 65535);
    public static final ModConfigSpec.BooleanValue NETHER_AND_END_CLOSED = BUILDER
            .comment("Close the Nether and the End: no travel there, no new Nether portals outside the Nether, no Eyes of Ender into End Portal Frames. The way home stays open. Set to false to open them again.")
            .define("netherAndEndClosed", true);
    public static final ModConfigSpec SPEC = BUILDER.build();

    private ServerConfig() {}
}
