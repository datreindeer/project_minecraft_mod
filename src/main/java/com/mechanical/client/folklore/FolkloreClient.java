package com.mechanical.client.folklore;

import com.mechanical.Mechanical;
import com.mechanical.client.wildlife.WildAnimalRenderState;
import com.mechanical.client.wildlife.forestbirds.ForestBirdModel;
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
 * Folklore on the client: the Mountain Watcher is drawn only for a player whose sight reaches {@link Faith#WATCHER_SIGHT}
 * (no model, shadow or name otherwise), and Second Sight makes you "a little high": a soft violet vignette and a slow sway.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class FolkloreClient {
    static final Identifier LAYER = Identifier.fromNamespaceAndPath(Mechanical.MODID, "second_sight");
    private static final Identifier VIGNETTE = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/misc/sanity_vignette.png");
    /** The Watcher is the golden eagle's build, this much bigger. */
    static final float WATCHER_SCALE = 1.4F;

    @SubscribeEvent
    static void setup(FMLClientSetupEvent event) {
        Faith.localPlayer = () -> Minecraft.getInstance().player;
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Folklore.MOUNTAIN_WATCHER.get(), c -> new ForestBirdRenderer(c,
                new ForestBirdModel(c.bakeLayer(ForestBirdModel.GOLDEN_EAGLE), 0.4F, 0.55F, 0.45F), 0.0F) {
            @Override
            public boolean shouldRender(FlockBird bird, Frustum frustum, double camX, double camY, double camZ, float partialTicks) {
                return Faith.canSee(Minecraft.getInstance().player, Faith.WATCHER_SIGHT) && super.shouldRender(bird, frustum, camX, camY, camZ, partialTicks);
            }

            @Override
            protected void scale(WildAnimalRenderState state, PoseStack poseStack) {
                super.scale(state, poseStack);
                poseStack.scale(WATCHER_SCALE, WATCHER_SCALE, WATCHER_SCALE);
            }
        });
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
