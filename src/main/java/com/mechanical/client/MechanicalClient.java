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
        event.register(com.mechanical.Ironworking.BLOOMERY_MENU.get(), KilnScreen::new);
        event.register(com.mechanical.Smelting.FURNACE_MENU.get(), FurnaceScreen::new);
        event.register(Mechanical.BARK_BASKET_MENU.get(), BarkBasketScreen::new);
        event.register(com.mechanical.MoldBench.MOLD_CHEST_MENU.get(), net.minecraft.client.gui.screens.inventory.ContainerScreen::new);
        event.register(com.mechanical.FarmTools.SEED_BAG_MENU.get(), BarkBasketScreen::new);
        event.register(com.mechanical.equipment.PackMenu.TYPE.get(), PackScreen::new);
        event.register(com.mechanical.wildlife.SaddlebagMenu.TYPE.get(), SaddlebagScreen::new);
        event.<com.mechanical.FletchingMenu, RecipeStationScreen<com.mechanical.FletchingMenu>>register(Mechanical.FLETCHING_MENU.get(), (menu, inventory, title) ->
                new RecipeStationScreen<>(menu, inventory, title, net.minecraft.resources.Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/gui/container/fletching_table.png")));
        event.<com.mechanical.ArmorStationMenu, RecipeStationScreen<com.mechanical.ArmorStationMenu>>register(Mechanical.ARMOR_STATION_MENU.get(), (menu, inventory, title) ->
                new RecipeStationScreen<>(menu, inventory, title, net.minecraft.resources.Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/gui/container/armor_station.png")));
        event.<com.mechanical.LeatherworkingMenu, RecipeStationScreen<com.mechanical.LeatherworkingMenu>>register(Mechanical.LEATHERWORKING_MENU.get(), (menu, inventory, title) ->
                new RecipeStationScreen<>(menu, inventory, title, net.minecraft.resources.Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/gui/container/leatherworking_bench.png")));
        event.<com.mechanical.MoldBench.Menu, RecipeStationScreen<com.mechanical.MoldBench.Menu>>register(com.mechanical.MoldBench.MENU.get(), (menu, inventory, title) ->
                new RecipeStationScreen<>(menu, inventory, title, net.minecraft.resources.Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/gui/container/mold_bench.png")));
    }

    // Worn Grass and Trodden Path take the (seasonal) grass colour like a Grass Block: SeasonalFoliage.registerTints

    @SubscribeEvent
    static void registerHud(RegisterGuiLayersEvent event) {
        event.wrapLayer(VanillaGuiLayers.FOOD_LEVEL, vanilla -> (graphics, delta) -> {
            vanilla.render(graphics, delta);
            RidingFoodHud.render(graphics);
        });
        event.registerAbove(VanillaGuiLayers.FOOD_LEVEL, TemperatureHud.ID, new TemperatureHud());
        event.registerAbove(VanillaGuiLayers.ARMOR_LEVEL, HorseStaminaHud.ID, HorseStaminaHud::render);
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, CalendarHud.ID, new CalendarHud());
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, SanityClient.ID, SanityClient::render);
        event.wrapLayer(VanillaGuiLayers.TAB_LIST, vanilla -> (graphics, delta) -> {
            if (!com.mechanical.client.clan.TribeTabList.render(graphics)) vanilla.render(graphics, delta);
        });
    }

    @SubscribeEvent
    static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(Mechanical.BLOOD_DROP.get(), BloodDropParticle.Provider::new);
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Mechanical.FALLING_TREE.get(), FallingTreeRenderer::new);
        event.registerEntityRenderer(Mechanical.FELLED_LOG.get(), FelledLogRenderer::new);
        event.registerEntityRenderer(Mechanical.CARCASS.get(), CarcassRenderer::new);
        event.registerEntityRenderer(com.mechanical.CarcassHauling.TRAVOIS.get(), TravoisRenderer::new);
        event.registerEntityRenderer(com.mechanical.Carting.CART.get(), CartRenderer::new);
        event.registerEntityRenderer(com.mechanical.Carting.WAGON.get(), WagonRenderer::new);
        event.registerEntityRenderer(com.mechanical.Carting.PLOUGH.get(), PloughRenderer::new);
        event.registerEntityRenderer(Wildlife.TAME_HORSE.get(), com.mechanical.client.wildlife.TameHorseRenderer::new);
        event.registerEntityRenderer(com.mechanical.Watercraft.LOG_RAFT.get(), WatercraftRenderer::raft);
        event.registerEntityRenderer(com.mechanical.Watercraft.DUGOUT_CANOE.get(), WatercraftRenderer::canoe);
        // adult model, baby model (built at baby size), extra baby scale, shadow
        event.registerEntityRenderer(Wildlife.ROE_DEER.get(), c -> new WildAnimalRenderer<>(c,
                new RoeDeerModel(c.bakeLayer(RoeDeerModel.LAYER)), new FawnModel(c.bakeLayer(FawnModel.LAYER)), 0.5F, 0.5F));
        event.registerEntityRenderer(Wildlife.RED_DEER.get(), c -> new WildAnimalRenderer<>(c,
                new RedDeerModel(c.bakeLayer(RedDeerModel.LAYER)), new FawnModel(c.bakeLayer(FawnModel.LAYER)), 0.65F, 0.8F));
        event.registerEntityRenderer(Wildlife.WILD_BOAR.get(), c -> new WildAnimalRenderer<>(c,
                new WildBoarModel(c.bakeLayer(WildBoarModel.LAYER)), new PigletModel(c.bakeLayer(PigletModel.LAYER)), 0.5F, 0.7F));
        // the leveret uses the hare model, scaled down, with its own texture
        event.registerEntityRenderer(Wildlife.HARE.get(), c -> {
            HareModel model = new HareModel(c.bakeLayer(HareModel.LAYER));
            return new WildAnimalRenderer<>(c, model, model, 0.6F, 0.3F);
        });
        event.registerEntityRenderer(Wildlife.DESERT_HARE.get(), c -> {
            HareModel model = new HareModel(c.bakeLayer(HareModel.LAYER));
            return new WildAnimalRenderer<>(c, model, model, 0.6F, 0.3F);
        });
        event.registerEntityRenderer(Wildlife.PHEASANT.get(), c -> new WildAnimalRenderer<>(c,
                new PheasantModel(c.bakeLayer(PheasantModel.LAYER)), new PheasantChickModel(c.bakeLayer(PheasantChickModel.LAYER)), 1.0F, 0.3F));
        event.registerEntityRenderer(Wildlife.AUROCHS.get(), c -> new WildAnimalRenderer<>(c,
                new AurochsModel(c.bakeLayer(AurochsModel.LAYER)), new AurochsCalfModel(c.bakeLayer(AurochsCalfModel.LAYER)), 0.575F, 1.0F));
        event.registerEntityRenderer(Wildlife.WILD_HORSE.get(), c -> new WildAnimalRenderer<>(c,
                new WildHorseModel(c.bakeLayer(WildHorseModel.LAYER)), new WildHorseFoalModel(c.bakeLayer(WildHorseFoalModel.LAYER)), 0.5F, 0.8F));
        event.registerEntityRenderer(Wildlife.MOUFLON.get(), c -> new WildAnimalRenderer<>(c,
                new MouflonModel(c.bakeLayer(MouflonModel.LAYER)), new MouflonLambModel(c.bakeLayer(MouflonLambModel.LAYER)), 0.5F, 0.5F));
        event.registerEntityRenderer(Wildlife.BROWN_BEAR.get(), c -> new WildAnimalRenderer<>(c,
                new BrownBearModel(c.bakeLayer(BrownBearModel.LAYER)), new BearCubModel(c.bakeLayer(BearCubModel.LAYER)), 1.0F, 1.0F));
        event.registerEntityRenderer(Wildlife.MOOSE.get(), c -> new WildAnimalRenderer<>(c,
                new MooseModel(c.bakeLayer(MooseModel.LAYER)), new MooseCalfModel(c.bakeLayer(MooseCalfModel.LAYER)), 0.5F, 1.1F));
        event.registerEntityRenderer(Wildlife.REINDEER.get(), c -> new WildAnimalRenderer<>(c,
                new ReindeerModel(c.bakeLayer(ReindeerModel.LAYER)), new ReindeerCalfModel(c.bakeLayer(ReindeerCalfModel.LAYER)), 0.5F, 0.75F));
        // small forest animals: the young are the adult model scaled down
        event.registerEntityRenderer(Wildlife.SQUIRREL.get(), c -> {
            SquirrelModel model = new SquirrelModel(c.bakeLayer(SquirrelModel.LAYER));
            return new WildAnimalRenderer<>(c, model, model, 0.6F, 0.2F);
        });
        event.registerEntityRenderer(Wildlife.HEDGEHOG.get(), c -> {
            HedgehogModel model = new HedgehogModel(c.bakeLayer(HedgehogModel.LAYER));
            return new WildAnimalRenderer<>(c, model, model, 0.6F, 0.25F);
        });
        event.registerEntityRenderer(Wildlife.BADGER.get(), c -> {
            BadgerModel model = new BadgerModel(c.bakeLayer(BadgerModel.LAYER));
            return new WildAnimalRenderer<>(c, model, model, 0.55F, 0.45F);
        });
        event.registerEntityRenderer(Wildlife.GREY_WOLF.get(), c -> {
            GreyWolfModel model = new GreyWolfModel(c.bakeLayer(GreyWolfModel.LAYER));
            return new WildAnimalRenderer<>(c, model, model, 0.6F, 0.5F);
        });
        event.registerEntityRenderer(Wildlife.CAVE_LION.get(), c -> {
            CaveLionModel model = new CaveLionModel(c.bakeLayer(CaveLionModel.LAYER));
            return new WildAnimalRenderer<>(c, model, model, 0.6F, 0.9F);
        });
        // flocking birds: layer, how far the neck reaches forward in flight, wing-beat amplitude, shadow
        event.registerEntityRenderer(Wildlife.CROW.get(), c -> new BirdRenderer(c, BirdModel.CROW, 0.5F, 0.85F, 0.25F));
        event.registerEntityRenderer(Wildlife.GOOSE.get(), c -> new BirdRenderer(c, BirdModel.GOOSE, 1.45F, 0.6F, 0.4F));
        event.registerEntityRenderer(Wildlife.ROCK_DOVE.get(), c -> new BirdRenderer(c, BirdModel.ROCK_DOVE, 0.4F, 0.95F, 0.2F));
        event.registerEntityRenderer(Wildlife.PTARMIGAN.get(), c -> new BirdRenderer(c, BirdModel.PTARMIGAN, 0.3F, 1.0F, 0.25F));
        event.registerEntityRenderer(Wildlife.SEAGULL.get(), c -> new BirdRenderer(c, BirdModel.SEAGULL, 0.4F, 0.55F, 0.3F));
        event.registerEntityRenderer(Wildlife.GIANT_DEER.get(), c -> {
            GiantDeerModel model = new GiantDeerModel(c.bakeLayer(GiantDeerModel.LAYER));
            return new WildAnimalRenderer<>(c, model, model, 0.5F, 2.4F) {
                @Override
                protected void scale(com.mechanical.client.wildlife.WildAnimalRenderState state, com.mojang.blaze3d.vertex.PoseStack poseStack) {
                    super.scale(state, poseStack);
                    poseStack.scale(2.0F, 2.0F, 2.0F); // a giant: twice the model
                }

                {
                    addLayer(new com.mechanical.client.wildlife.AngryEyesLayer(this,
                            net.minecraft.resources.Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/entity/wildlife/giant_deer_eyes.png")));
                    addLayer(new com.mechanical.client.wildlife.AngryEyesLayer(this,
                            net.minecraft.resources.Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/entity/wildlife/giant_deer_runes.png"),
                            state -> !com.mechanical.client.CarcassRenderer.isCarcass(state) && !com.mechanical.client.CarcassRenderer.isHung(state)));
                }
            };
        });
    }

    @SubscribeEvent
    static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(WatercraftModel.RAFT, WatercraftModel::createRaft);
        event.registerLayerDefinition(TravoisModel.LAYER, TravoisModel::create);
        event.registerLayerDefinition(CartModel.LAYER, CartModel::create);
        event.registerLayerDefinition(WagonModel.LAYER, WagonModel::create);
        event.registerLayerDefinition(PloughModel.LAYER, PloughModel::create);
        event.registerLayerDefinition(TiltModel.CART, TiltModel::cart);
        event.registerLayerDefinition(TiltModel.WAGON, TiltModel::wagon);
        event.registerLayerDefinition(CarcassRenderer.Lashing.LAYER, CarcassRenderer.Lashing::create);
        event.registerLayerDefinition(com.mechanical.client.wildlife.TameHorseRenderer.BLANKET,
                () -> WildHorseModel.createBodyLayer(new net.minecraft.client.model.geom.builders.CubeDeformation(0.4F)));
        event.registerLayerDefinition(com.mechanical.client.wildlife.TameHorseRenderer.ARMOR,
                () -> WildHorseModel.createBodyLayer(new net.minecraft.client.model.geom.builders.CubeDeformation(0.2F)));
        event.registerLayerDefinition(com.mechanical.client.wildlife.TameHorseRenderer.SATCHELS, WildHorseModel::createSatchelLayer);
        event.registerLayerDefinition(WatercraftModel.CANOE, WatercraftModel::createCanoe);
        event.registerLayerDefinition(WatercraftModel.CANOE_WATER_PATCH, WatercraftModel::createCanoeWaterPatch);
        event.registerLayerDefinition(RoeDeerModel.LAYER, RoeDeerModel::createBodyLayer);
        event.registerLayerDefinition(RedDeerModel.LAYER, RedDeerModel::createBodyLayer);
        event.registerLayerDefinition(WildBoarModel.LAYER, WildBoarModel::createBodyLayer);
        event.registerLayerDefinition(HareModel.LAYER, HareModel::createBodyLayer);
        event.registerLayerDefinition(PheasantModel.LAYER, PheasantModel::createBodyLayer);
        event.registerLayerDefinition(FawnModel.LAYER, FawnModel::createBodyLayer);
        event.registerLayerDefinition(PigletModel.LAYER, PigletModel::createBodyLayer);
        event.registerLayerDefinition(PheasantChickModel.LAYER, PheasantChickModel::createBodyLayer);
        event.registerLayerDefinition(AurochsModel.LAYER, AurochsModel::createBodyLayer);
        event.registerLayerDefinition(AurochsCalfModel.LAYER, AurochsCalfModel::createBodyLayer);
        event.registerLayerDefinition(WildHorseModel.LAYER, WildHorseModel::createBodyLayer);
        event.registerLayerDefinition(WildHorseFoalModel.LAYER, WildHorseFoalModel::createBodyLayer);
        event.registerLayerDefinition(MouflonModel.LAYER, MouflonModel::createBodyLayer);
        event.registerLayerDefinition(MouflonLambModel.LAYER, MouflonLambModel::createBodyLayer);
        event.registerLayerDefinition(MooseModel.LAYER, MooseModel::createBodyLayer);
        event.registerLayerDefinition(BrownBearModel.LAYER, BrownBearModel::createBodyLayer);
        event.registerLayerDefinition(BearCubModel.LAYER, BearCubModel::createBodyLayer);
        event.registerLayerDefinition(MooseCalfModel.LAYER, MooseCalfModel::createBodyLayer);
        event.registerLayerDefinition(ReindeerModel.LAYER, ReindeerModel::createBodyLayer);
        event.registerLayerDefinition(ReindeerCalfModel.LAYER, ReindeerCalfModel::createBodyLayer);
        event.registerLayerDefinition(SquirrelModel.LAYER, SquirrelModel::createBodyLayer);
        event.registerLayerDefinition(HedgehogModel.LAYER, HedgehogModel::createBodyLayer);
        event.registerLayerDefinition(BadgerModel.LAYER, BadgerModel::createBodyLayer);
        event.registerLayerDefinition(GiantDeerModel.LAYER, GiantDeerModel::createBodyLayer);
        event.registerLayerDefinition(GreyWolfModel.LAYER, GreyWolfModel::createBodyLayer);
        event.registerLayerDefinition(CaveLionModel.LAYER, CaveLionModel::createBodyLayer);
        event.registerLayerDefinition(BirdModel.CROW, BirdModel::createCrowLayer);
        event.registerLayerDefinition(BirdModel.GOOSE, BirdModel::createGooseLayer);
        event.registerLayerDefinition(BirdModel.ROCK_DOVE, BirdModel::createRockDoveLayer);
        event.registerLayerDefinition(BirdModel.PTARMIGAN, BirdModel::createPtarmiganLayer);
        event.registerLayerDefinition(BirdModel.SEAGULL, BirdModel::createSeagullLayer);
        event.registerLayerDefinition(WolfHoodModel.LAYER, WolfHoodModel::createLayer);
    }

    @SubscribeEvent
    static void registerClientExtensions(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
        event.registerItem(WolfHoodModel.EXTENSIONS, Mechanical.WOLF_HOOD.get());
    }

    /** Passes the bear's rearing amount to its model. */
    @SubscribeEvent
    static void renderStateModifiers(net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent event) {
        BrownBearModel.registerRenderState(event);
        SquirrelModel.registerRenderState(event);
        HedgehogModel.registerRenderState(event);
    }
}
