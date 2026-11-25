package com.mechanical;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server options (the per-world "LOCAL" config, serverconfig/ in the world): the admin dashboard (dashboard.Dashboard). */
public final class ServerConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec.BooleanValue DASHBOARD = BUILDER
            .comment("Run the admin web dashboard (dedicated servers only; listens on 127.0.0.1 only, put a login proxy such as Caddy in front)")
            .define("dashboard", true);
    public static final ModConfigSpec.IntValue DASHBOARD_PORT = BUILDER
            .comment("Port of the admin web dashboard")
            .defineInRange("dashboardPort", 8100, 1024, 65535);
    public static final ModConfigSpec SPEC = BUILDER.build();

    private ServerConfig() {}
}
