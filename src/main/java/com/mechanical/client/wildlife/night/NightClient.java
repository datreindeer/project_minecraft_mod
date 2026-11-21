package com.mechanical.client.wildlife.night;

import com.mechanical.Mechanical;
import com.mechanical.client.wildlife.AngryEyesLayer;
import com.mechanical.client.wildlife.WildAnimalRenderer;
import com.mechanical.client.wildlife.WildBoarModel;
import com.mechanical.wildlife.night.MushroomMan;
import com.mechanical.wildlife.night.NightFauna;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.CreeperRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Renderers for the night monsters (NightFauna): the Gloson on the wild boar's model with glowing eyes, the Mushroom Man. */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class NightClient {
    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(NightFauna.GLOSON.get(), c -> {
            WildBoarModel model = new WildBoarModel(c.bakeLayer(WildBoarModel.LAYER));
            return new WildAnimalRenderer<>(c, model, model, 1.0F, 0.6F) {
                {
                    // the eyes glow whenever it is awake (EyesLayerMixin hides them on a carcass)
                    addLayer(new AngryEyesLayer(this, Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/entity/wildlife/gloson_eyes.png"),
                            state -> !state.dozing));
                }
            };
        });
        event.registerEntityRenderer(NightFauna.MUSHROOM_MAN.get(), MushroomManRenderer::new);
    }

    @SubscribeEvent
    static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(MushroomManModel.LAYER, MushroomManModel::createBodyLayer);
    }

    /** Swells and flashes before it bursts, like the creeper it replaces. */
    static class MushroomManRenderer extends MobRenderer<MushroomMan, CreeperRenderState, MushroomManModel> {
        private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/entity/night/mushroom_man.png");

        MushroomManRenderer(EntityRendererProvider.Context context) {
            super(context, new MushroomManModel(context.bakeLayer(MushroomManModel.LAYER)), 0.45F);
        }

        @Override
        protected void scale(CreeperRenderState state, PoseStack poseStack) {
            float g = state.swelling;
            float wobble = 1.0F + Mth.sin(g * 100.0F) * g * 0.01F;
            g = Mth.clamp(g, 0.0F, 1.0F);
            g *= g;
            g *= g;
            poseStack.scale((1.0F + g * 0.4F) * wobble, (1.0F + g * 0.1F) / wobble, (1.0F + g * 0.4F) * wobble);
        }

        @Override
        protected float getWhiteOverlayProgress(CreeperRenderState state) {
            return (int) (state.swelling * 10.0F) % 2 == 0 ? 0.0F : Mth.clamp(state.swelling, 0.5F, 1.0F);
        }

        @Override
        public Identifier getTextureLocation(CreeperRenderState state) {
            return TEXTURE;
        }

        @Override
        public CreeperRenderState createRenderState() {
            return new CreeperRenderState();
        }

        @Override
        public void extractRenderState(MushroomMan entity, CreeperRenderState state, float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            state.swelling = entity.getSwelling(partialTicks);
        }
    }

    private NightClient() {}
}
