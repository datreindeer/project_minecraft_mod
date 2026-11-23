package com.mechanical.client.wildlife.deep;

import com.mechanical.Mechanical;
import com.mechanical.client.wildlife.WildAnimalRenderer;
import com.mechanical.client.wildlife.coast.CoastClient;
import com.mechanical.wildlife.deep.BatSwarm;
import com.mechanical.wildlife.deep.CavernSpider;
import com.mechanical.wildlife.deep.DeepFauna;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Renderers and model layers of the deep fauna (wildlife/deep/DeepFauna); the olm and the cave fish use the coast fish renderer. */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class DeepClient {
    private static Identifier tex(String name) {
        return Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/entity/wildlife/" + name + ".png");
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(DeepFauna.CAVE_SALAMANDER.get(), c -> new CoastClient.FishRenderer<>(c,
                new DeepModels.SalamanderModel(c.bakeLayer(DeepModels.SalamanderModel.LAYER)), tex("cave_salamander"), 0.15F, 0.4F, 0.0F));
        event.registerEntityRenderer(DeepFauna.BLIND_CAVE_FISH.get(), c -> new CoastClient.FishRenderer<>(c,
                new DeepModels.CaveFishModel(c.bakeLayer(DeepModels.CaveFishModel.LAYER)), tex("blind_cave_fish"), 0.12F, 0.4F, 0.0F));
        event.registerEntityRenderer(DeepFauna.BAT_SWARM.get(), SwarmRenderer::new);
        event.registerEntityRenderer(DeepFauna.CAVERN_SPIDER.get(), c -> {
            DeepModels.CavernSpiderModel model = new DeepModels.CavernSpiderModel(c.bakeLayer(DeepModels.CavernSpiderModel.LAYER));
            return new WildAnimalRenderer<CavernSpider>(c, model, model, 1.0F, 0.6F);
        });
    }

    @SubscribeEvent
    static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(DeepModels.SalamanderModel.LAYER, DeepModels.SalamanderModel::createBodyLayer);
        event.registerLayerDefinition(DeepModels.CaveFishModel.LAYER, DeepModels.CaveFishModel::createBodyLayer);
        event.registerLayerDefinition(DeepModels.BatSwarmModel.LAYER, DeepModels.BatSwarmModel::createBodyLayer);
        event.registerLayerDefinition(DeepModels.CavernSpiderModel.LAYER, DeepModels.CavernSpiderModel::createBodyLayer);
    }

    /** Draws the swarm's bats (BatSwarmModel); no shadow. */
    static class SwarmRenderer extends MobRenderer<BatSwarm, LivingEntityRenderState, DeepModels.BatSwarmModel> {
        private static final Identifier TEXTURE = tex("bat_swarm");

        SwarmRenderer(EntityRendererProvider.Context context) {
            super(context, new DeepModels.BatSwarmModel(context.bakeLayer(DeepModels.BatSwarmModel.LAYER)), 0.0F);
        }

        @Override
        public LivingEntityRenderState createRenderState() {
            return new LivingEntityRenderState();
        }

        @Override
        public Identifier getTextureLocation(LivingEntityRenderState state) {
            return TEXTURE;
        }
    }

    private DeepClient() {}
}
