package com.mechanical;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Client options (config/mechanical-client.toml, also under Mods > Mechanical > Config). */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class ClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.BooleanValue BLEND_EDGES = BUILDER
            .comment("Related neighbouring blocks (ground, stone, planks) fray into each other on their top faces instead of meeting in a straight seam")
            .define("blendEdges", true);
    public static final ModConfigSpec SPEC = BUILDER.build();

    /** Read on chunk meshing threads; the toggle rebuilds the chunks (client.EdgeBlend). */
    public static volatile boolean blendEdges = true;
    /** Called when blendEdges changes (set by the client). */
    public static Runnable onChange = () -> {};

    private ClientConfig() {}

    @SubscribeEvent
    static void onLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC) blendEdges = BLEND_EDGES.get();
    }

    @SubscribeEvent
    static void onReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() != SPEC || blendEdges == BLEND_EDGES.get()) return;
        blendEdges = BLEND_EDGES.get();
        onChange.run();
    }
}
