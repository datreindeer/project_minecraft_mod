package com.mechanical.client.folklore;

import com.mechanical.Mechanical;
import com.mechanical.client.wildlife.WildAnimalRenderState;
import com.mechanical.client.wildlife.BirdRenderState;
import com.mechanical.client.wildlife.SmallGeometry;
import com.mechanical.client.wildlife.forestbirds.ForestBirdRenderer;
import com.mechanical.folklore.Faith;
import com.mechanical.folklore.Folklore;
import com.mechanical.wildlife.FlockBird;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Folklore on the client: the Mountain Watcher ({@link WatcherRenderer}, {@link MountainWatcherModel}) is drawn only for a
 * player whose sight reaches {@link Faith#WATCHER_SIGHT} (no model, shadow or name otherwise), and Second Sight makes you "a little high": a soft violet vignette and a slow sway.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class FolkloreClient {
    static final Identifier LAYER = Identifier.fromNamespaceAndPath(Mechanical.MODID, "second_sight");
    private static final Identifier VIGNETTE = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/misc/sanity_vignette.png");
    /** The Watcher's model is drawn this much bigger. */
    static final float WATCHER_SCALE = 1.5F;

    @SubscribeEvent
    static void setup(FMLClientSetupEvent event) {
        Faith.localPlayer = () -> Minecraft.getInstance().player;
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Folklore.MOUNTAIN_WATCHER.get(), WatcherRenderer::new);
    }

    @SubscribeEvent
    static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(MountainWatcherModel.LAYER, SmallGeometry::mountainWatcher);
    }

    /**
     * The Watcher: its own model, drawn {@link #WATCHER_SCALE} times, its eyes, runes and feather tips glowing faintly
     * (mountain_watcher_glow.png, full bright). It beats its wings only now and then, slow and deep, and glides on.
     */
    static class WatcherRenderer extends ForestBirdRenderer {
        private static final RenderType GLOW = RenderTypes.eyes(Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/entity/wildlife/mountain_watcher_glow.png"));
        /** Ticks between bursts of wing beats, how long a burst lasts, and the beat speed (radians per tick). */
        static final float CYCLE = 260, BURST = 60, BEAT = 0.24F;

        WatcherRenderer(EntityRendererProvider.Context c) {
            super(c, new MountainWatcherModel(c.bakeLayer(MountainWatcherModel.LAYER)), 0.0F);
            addLayer(new EyesLayer<>(this) {
                @Override
                public RenderType renderType() {
                    return GLOW;
                }
            });
        }

        @Override
        public boolean shouldRender(FlockBird bird, Frustum frustum, double camX, double camY, double camZ, float partialTicks) {
            return Faith.canSee(Minecraft.getInstance().player, Faith.WATCHER_SIGHT) && super.shouldRender(bird, frustum, camX, camY, camZ, partialTicks);
        }

        @Override
        public void extractRenderState(FlockBird bird, WildAnimalRenderState state, float partialTicks) {
            super.extractRenderState(bird, state, partialTicks);
            if (!state.flying) return;
            BirdRenderState s = (BirdRenderState) state;
            float t = state.ageInTicks + bird.getId() * 37, cycle = t % CYCLE;
            float burst = cycle < BURST ? Mth.sin(cycle / BURST * Mth.PI) : 0.0F;
            float climb = Mth.clamp((float) (bird.getY() - bird.yo - 0.04) * 12.0F, 0.0F, 1.0F); // beats to climb
            s.glide = 1.0F - Math.max(burst, climb);
            s.flap = t * BEAT;
        }

        @Override
        protected void scale(WildAnimalRenderState state, PoseStack poseStack) {
            super.scale(state, poseStack);
            poseStack.scale(WATCHER_SCALE, WATCHER_SCALE, WATCHER_SCALE);
        }
    }

    @SubscribeEvent
    static void registerHud(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, LAYER, FolkloreClient::render);
    }

    /** 0..1: how strong Second Sight shows (fades out over its last 4 s). */
    private static float strength() {
        var player = Minecraft.getInstance().player;
        MobEffectInstance e = player == null ? null : player.getEffect(Folklore.SECOND_SIGHT);
        if (e == null) return 0;
        return e.isInfiniteDuration() ? 1 : Math.min(1, e.getDuration() / 80F);
    }

    static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        float k = strength();
        if (k <= 0) return;
        int w = graphics.guiWidth(), h = graphics.guiHeight();
        float t = (Minecraft.getInstance().player.tickCount + delta.getGameTimeDeltaPartialTick(false)) * 0.05F;
        float v = k * (0.55F + 0.15F * Mth.sin(t));
        // the vignette pipeline darkens by the tint: taking mostly green away leaves the edges a soft violet
        graphics.blit(RenderPipelines.VIGNETTE, VIGNETTE, 0, 0, 0, 0, w, h, w, h, ARGB.colorFromFloat(1, v * 0.25F, v, v * 0.1F));
    }

    @SubscribeEvent
    static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        float k = strength();
        if (k <= 0) return;
        float t = (float) ((Minecraft.getInstance().player.tickCount + event.getPartialTick()) * 0.03);
        event.setRoll(event.getRoll() + Mth.sin(t) * 1.5F * k);
        event.setYaw(event.getYaw() + Mth.sin(t * 0.6F + 2) * 0.6F * k);
    }

    private FolkloreClient() {}
}
