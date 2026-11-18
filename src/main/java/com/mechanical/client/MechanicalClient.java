package com.mechanical.client;

import com.mechanical.Mechanical;
import com.mechanical.client.wildlife.*;
import com.mechanical.wildlife.Wildlife;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public class MechanicalClient {
    @SubscribeEvent
    static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(Mechanical.TOOL_STATION_MENU.get(), ToolStationScreen::new);
        event.register(Mechanical.KILN_MENU.get(), KilnScreen::new);
    }

    @SubscribeEvent
    static void registerHud(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.FOOD_LEVEL, TemperatureHud.ID, new TemperatureHud());
    }

    @SubscribeEvent
    static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(Mechanical.BLOOD_DROP.get(), BloodDropParticle.Provider::new);
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Mechanical.FALLING_TREE.get(), FallingTreeRenderer::new);
        event.registerEntityRenderer(Mechanical.CARCASS.get(), CarcassRenderer::new);
        // adult model, baby model (built at baby size), extra baby scale, shadow
        event.registerEntityRenderer(Wildlife.ROE_DEER.get(), c -> new WildAnimalRenderer<>(c,
                new RoeDeerModel(c.bakeLayer(RoeDeerModel.LAYER)), new FawnModel(c.bakeLayer(FawnModel.LAYER)), 1.0F, 0.5F));
        event.registerEntityRenderer(Wildlife.RED_DEER.get(), c -> new WildAnimalRenderer<>(c,
                new RedDeerModel(c.bakeLayer(RedDeerModel.LAYER)), new FawnModel(c.bakeLayer(FawnModel.LAYER)), 1.3F, 0.8F));
        event.registerEntityRenderer(Wildlife.WILD_BOAR.get(), c -> new WildAnimalRenderer<>(c,
                new WildBoarModel(c.bakeLayer(WildBoarModel.LAYER)), new PigletModel(c.bakeLayer(PigletModel.LAYER)), 1.0F, 0.7F));
        // the leveret uses the hare model, scaled down, with its own texture
        event.registerEntityRenderer(Wildlife.HARE.get(), c -> {
            HareModel model = new HareModel(c.bakeLayer(HareModel.LAYER));
            return new WildAnimalRenderer<>(c, model, model, 0.6F, 0.3F);
        });
        event.registerEntityRenderer(Wildlife.PHEASANT.get(), c -> new WildAnimalRenderer<>(c,
                new PheasantModel(c.bakeLayer(PheasantModel.LAYER)), new PheasantChickModel(c.bakeLayer(PheasantChickModel.LAYER)), 1.0F, 0.3F));
    }

    @SubscribeEvent
    static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(RoeDeerModel.LAYER, RoeDeerModel::createBodyLayer);
        event.registerLayerDefinition(RedDeerModel.LAYER, RedDeerModel::createBodyLayer);
        event.registerLayerDefinition(WildBoarModel.LAYER, WildBoarModel::createBodyLayer);
        event.registerLayerDefinition(HareModel.LAYER, HareModel::createBodyLayer);
        event.registerLayerDefinition(PheasantModel.LAYER, PheasantModel::createBodyLayer);
        event.registerLayerDefinition(FawnModel.LAYER, FawnModel::createBodyLayer);
        event.registerLayerDefinition(PigletModel.LAYER, PigletModel::createBodyLayer);
        event.registerLayerDefinition(PheasantChickModel.LAYER, PheasantChickModel::createBodyLayer);
    }
}
