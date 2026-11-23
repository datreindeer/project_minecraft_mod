package com.mechanical.client.vanilla;

import static com.mechanical.client.vanilla.FarmAnimalRenderers.skin;

import com.mechanical.Mechanical;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.model.animal.camel.AdultCamelModel;
import net.minecraft.client.model.animal.camel.BabyCamelModel;
import net.minecraft.client.model.animal.camel.CamelModel;
import net.minecraft.client.model.animal.camel.CamelSaddleModel;
import net.minecraft.client.model.animal.feline.AbstractFelineModel;
import net.minecraft.client.model.animal.feline.AdultCatModel;
import net.minecraft.client.model.animal.feline.AdultOcelotModel;
import net.minecraft.client.model.animal.feline.BabyCatModel;
import net.minecraft.client.model.animal.feline.BabyOcelotModel;
import net.minecraft.client.model.animal.fox.AdultFoxModel;
import net.minecraft.client.model.animal.fox.BabyFoxModel;
import net.minecraft.client.model.animal.fox.FoxModel;
import net.minecraft.client.model.animal.rabbit.AdultRabbitModel;
import net.minecraft.client.model.animal.rabbit.BabyRabbitModel;
import net.minecraft.client.model.animal.rabbit.RabbitModel;
import net.minecraft.client.model.animal.wolf.AdultWolfModel;
import net.minecraft.client.model.animal.wolf.BabyWolfModel;
import net.minecraft.client.model.animal.wolf.WolfModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.CamelRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.FoxHeldItemLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.renderer.entity.layers.WolfArmorLayer;
import net.minecraft.client.renderer.entity.state.CamelRenderState;
import net.minecraft.client.renderer.entity.state.CatRenderState;
import net.minecraft.client.renderer.entity.state.FelineRenderState;
import net.minecraft.client.renderer.entity.state.FoxRenderState;
import net.minecraft.client.renderer.entity.state.HoldingEntityRenderState;
import net.minecraft.client.renderer.entity.state.RabbitRenderState;
import net.minecraft.client.renderer.entity.state.WolfRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.feline.Ocelot;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * The second pass over the vanilla animals: rabbit, fox, wolf, cat, ocelot and camel. Each keeps the vanilla skeleton
 * (part names, pivots and rotations) with its own boxes ({@link VanillaGeometry}, scripts/gen_vanilla_models.py), so the
 * vanilla model classes themselves drive it: the rabbit's hop, the fox's sitting/sleeping/crouching/pouncing poses and
 * the berry in its mouth, the wolf's sitting, shaking and tail, the cats' sitting and lying, the camel's walk, sit and
 * dash keyframes, and the generic carcass poses (the models are still vanilla classes). Adults get the new look; the
 * babies keep vanilla's own baby models and skins. The wolf's armour and the camel's saddle stay on vanilla's shapes.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class WildVanillaRenderers {
    static final ModelLayerLocation RABBIT = layer("rabbit"), FOX = layer("fox"), WOLF = layer("wolf"), CAT = layer("cat"),
            OCELOT = layer("ocelot"), CAMEL = layer("camel");
    private static final Identifier WOLF_COLLAR = FarmAnimalRenderers.vanilla("wolf/wolf_collar"), CAT_COLLAR = FarmAnimalRenderers.vanilla("cat/cat_collar");

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mechanical.MODID, "vanilla_" + name), "main");
    }

    @SubscribeEvent
    static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(RABBIT, VanillaGeometry::rabbit);
        event.registerLayerDefinition(FOX, VanillaGeometry::fox);
        event.registerLayerDefinition(WOLF, VanillaGeometry::wolf);
        event.registerLayerDefinition(CAT, () -> VanillaGeometry.feline().apply(AdultCatModel.CAT_TRANSFORMER));
        event.registerLayerDefinition(OCELOT, VanillaGeometry::feline);
        event.registerLayerDefinition(CAMEL, VanillaGeometry::camel);
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityTypes.RABBIT, Rabbits::new);
        event.registerEntityRenderer(EntityTypes.FOX, Foxes::new);
        event.registerEntityRenderer(EntityTypes.WOLF, Wolves::new);
        event.registerEntityRenderer(EntityTypes.CAT, Cats::new);
        event.registerEntityRenderer(EntityTypes.OCELOT, Ocelots::new);
        event.registerEntityRenderer(EntityTypes.CAMEL, Camels::new);
    }

    /** Adults wear the new skin (same file name under textures/entity/vanilla/), babies vanilla's own. */
    private static Identifier texture(boolean baby, String vanillaPath) {
        Identifier vanilla = Identifier.withDefaultNamespace("textures/entity/" + vanillaPath + (baby ? "_baby" : "") + ".png");
        return baby ? vanilla : skin(vanilla);
    }

    // ------------------------------------------------------------------------------------------------------- rabbit
    static final class Rabbits extends AgeableMobRenderer<Rabbit, RabbitRenderState, RabbitModel> {
        Rabbits(EntityRendererProvider.Context context) {
            super(context, new AdultRabbitModel(context.bakeLayer(RABBIT)), new BabyRabbitModel(context.bakeLayer(ModelLayers.RABBIT_BABY)), 0.3F);
        }

        @Override
        public Identifier getTextureLocation(RabbitRenderState state) {
            String name = state.isToast ? "toast" : state.variant == Rabbit.Variant.EVIL ? "caerbannog" : state.variant.getSerializedName();
            return texture(state.isBaby, "rabbit/rabbit_" + name);
        }

        @Override
        public RabbitRenderState createRenderState() {
            return new RabbitRenderState();
        }

        @Override
        public void extractRenderState(Rabbit entity, RabbitRenderState state, float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            state.jumpCompletion = entity.getJumpCompletion(partialTicks);
            state.isToast = checkMagicName(entity, "Toast");
            state.variant = entity.getVariant();
            state.hopAnimationState.copyFrom(entity.hopAnimationState);
            state.idleHeadTiltAnimationState.copyFrom(entity.idleHeadTiltAnimationState);
        }
    }

    // ---------------------------------------------------------------------------------------------------------- fox
    static final class Foxes extends AgeableMobRenderer<Fox, FoxRenderState, FoxModel> {
        Foxes(EntityRendererProvider.Context context) {
            super(context, new AdultFoxModel(context.bakeLayer(FOX)), new BabyFoxModel(context.bakeLayer(ModelLayers.FOX_BABY)), 0.4F);
            addLayer(new FoxHeldItemLayer(this)); // the same head pivot as vanilla, so the item sits in the new jaws
        }

        @Override
        protected void setupRotations(FoxRenderState state, PoseStack poseStack, float bodyRot, float entityScale) {
            super.setupRotations(state, poseStack, bodyRot, entityScale);
            if (state.isPouncing || state.isFaceplanted) poseStack.rotateDegrees(Axis.XP, -state.xRot);
        }

        @Override
        public Identifier getTextureLocation(FoxRenderState state) {
            return texture(state.isBaby, "fox/fox" + (state.variant == Fox.Variant.SNOW ? "_snow" : "") + (state.isSleeping ? "_sleep" : ""));
        }

        @Override
        public FoxRenderState createRenderState() {
            return new FoxRenderState();
        }

        @Override
        public void extractRenderState(Fox entity, FoxRenderState state, float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            HoldingEntityRenderState.extractHoldingEntityRenderState(entity, state, itemModelResolver);
            state.headRollAngle = entity.getHeadRollAngle(partialTicks);
            state.isCrouching = entity.isCrouching();
            state.crouchAmount = entity.getCrouchAmount(partialTicks);
            state.isSleeping = entity.isSleeping();
            state.isSitting = entity.isSitting();
            state.isFaceplanted = entity.isFaceplanted();
            state.isPouncing = entity.isPouncing();
            state.variant = entity.getVariant();
        }
    }

    // --------------------------------------------------------------------------------------------------------- wolf
    static final class Wolves extends AgeableMobRenderer<Wolf, WolfRenderState, WolfModel> {
        Wolves(EntityRendererProvider.Context context) {
            super(context, new AdultWolfModel(context.bakeLayer(WOLF)), new BabyWolfModel(context.bakeLayer(ModelLayers.WOLF_BABY)), 0.5F);
            addLayer(new WolfArmorLayer(this, context.getModelSet(), context.getEquipmentRenderer())); // vanilla's armour shape
            addLayer(new Collar<>(this, WOLF_COLLAR, Identifier.withDefaultNamespace("textures/entity/wolf/wolf_collar_baby.png")));
        }

        @Override
        protected int getModelTint(WolfRenderState state) {
            float wet = state.wetShade;
            return wet == 1.0F ? -1 : ARGB.colorFromFloat(1.0F, wet, wet, wet);
        }

        @Override
        public Identifier getTextureLocation(WolfRenderState state) {
            return state.isBaby ? state.texture : skin(state.texture);
        }

        @Override
        public WolfRenderState createRenderState() {
            return new WolfRenderState();
        }

        @Override
        public void extractRenderState(Wolf entity, WolfRenderState state, float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            state.isAngry = entity.isAngry();
            state.isSitting = entity.isInSittingPose();
            state.tailAngle = entity.getTailAngle();
            state.headRollAngle = entity.getHeadRollAngle(partialTicks);
            state.shakeAnim = entity.getShakeAnim(partialTicks);
            state.texture = entity.getTexture();
            state.wetShade = entity.getWetShade(partialTicks);
            state.collarColor = entity.isTame() ? entity.getCollarColor() : null;
            state.bodyArmorItem = entity.getBodyArmorItem().copy();
        }
    }

    /**
     * A dyed collar drawn over the body with the parent's own model (as WolfCollarLayer does); the adult skin is a
     * band round the new neck, the baby keeps vanilla's.
     */
    static class Collar<S extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState, M extends net.minecraft.client.model.EntityModel<? super S>>
            extends RenderLayer<S, M> {
        private final Identifier adult, baby;

        Collar(RenderLayerParent<S, M> renderer, Identifier adult, Identifier baby) {
            super(renderer);
            this.adult = adult;
            this.baby = baby;
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, S state, float yRot, float xRot) {
            net.minecraft.world.item.DyeColor colour = state instanceof WolfRenderState w ? w.collarColor : state instanceof CatRenderState c ? c.collarColor : null;
            if (colour == null || state.isInvisible) return;
            collector.order(1).submitModel(getParentModel(), state, poseStack, RenderTypes.entityCutout(state.isBaby ? baby : adult), lightCoords,
                    OverlayTexture.NO_OVERLAY, colour.getTextureDiffuseColor(), null, state.outlineColor);
        }
    }

    // ---------------------------------------------------------------------------------------------- cat and ocelot
    static final class Cats extends AgeableMobRenderer<Cat, CatRenderState, AbstractFelineModel<CatRenderState>> {
        Cats(EntityRendererProvider.Context context) {
            super(context, new AdultCatModel(context.bakeLayer(CAT)), new BabyCatModel(context.bakeLayer(ModelLayers.CAT_BABY)), 0.4F);
            addLayer(new BabyCollar(this, context));
            addLayer(new Collar<>(this, CAT_COLLAR, CAT_COLLAR) {
                @Override
                public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, CatRenderState state, float yRot, float xRot) {
                    if (!state.isBaby) super.submit(poseStack, collector, lightCoords, state, yRot, xRot);
                }
            });
        }

        @Override
        public Identifier getTextureLocation(CatRenderState state) {
            return state.isBaby ? state.texture : skin(state.texture);
        }

        @Override
        public CatRenderState createRenderState() {
            return new CatRenderState();
        }

        @Override
        public void extractRenderState(Cat entity, CatRenderState state, float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            state.texture = entity.getVariant().value().assetInfo(state.isBaby).texturePath();
            state.isCrouching = entity.isCrouching();
            state.isSprinting = entity.isSprinting();
            state.isSitting = entity.isInSittingPose();
            state.lieDownAmount = entity.getLieDownAmount(partialTicks);
            state.lieDownAmountTail = entity.getLieDownAmountTail(partialTicks);
            state.relaxStateOneAmount = entity.getRelaxStateOneAmount(partialTicks);
            state.isLyingOnTopOfSleepingPlayer = entity.isLyingOnTopOfSleepingPlayer();
            state.collarColor = entity.isTame() ? entity.getCollarColor() : null;
        }

        @Override
        protected void setupRotations(CatRenderState state, PoseStack poseStack, float bodyRot, float entityScale) {
            super.setupRotations(state, poseStack, bodyRot, entityScale);
            float lie = state.lieDownAmount;
            if (lie > 0.0F) {
                poseStack.translate(0.4F * lie, 0.15F * lie, 0.1F * lie);
                poseStack.rotateDegrees(Axis.ZP, Mth.rotLerp(lie, 0.0F, 90.0F));
                if (state.isLyingOnTopOfSleepingPlayer) poseStack.translate(0.15F * lie, 0.0F, 0.0F);
            }
        }
    }

    /** The kitten's collar: vanilla's own collar model and skin, as CatCollarLayer draws it. */
    static final class BabyCollar extends RenderLayer<CatRenderState, AbstractFelineModel<CatRenderState>> {
        private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/entity/cat/cat_collar_baby.png");
        private final BabyCatModel model;

        BabyCollar(RenderLayerParent<CatRenderState, AbstractFelineModel<CatRenderState>> renderer, EntityRendererProvider.Context context) {
            super(renderer);
            model = new BabyCatModel(context.bakeLayer(ModelLayers.CAT_BABY_COLLAR));
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, CatRenderState state, float yRot, float xRot) {
            if (state.collarColor == null || !state.isBaby) return;
            coloredCutoutModelCopyLayerRender(model, TEXTURE, poseStack, collector, lightCoords, state, state.collarColor.getTextureDiffuseColor(), 1);
        }
    }

    static final class Ocelots extends AgeableMobRenderer<Ocelot, FelineRenderState, AbstractFelineModel<FelineRenderState>> {
        Ocelots(EntityRendererProvider.Context context) {
            super(context, new AdultOcelotModel(context.bakeLayer(OCELOT)), new BabyOcelotModel(context.bakeLayer(ModelLayers.OCELOT_BABY)), 0.4F);
        }

        @Override
        public Identifier getTextureLocation(FelineRenderState state) {
            return texture(state.isBaby, "cat/ocelot");
        }

        @Override
        public FelineRenderState createRenderState() {
            return new FelineRenderState();
        }

        @Override
        public void extractRenderState(Ocelot entity, FelineRenderState state, float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            state.isCrouching = entity.isCrouching();
            state.isSprinting = entity.isSprinting();
        }
    }

    // -------------------------------------------------------------------------------------------------------- camel
    static final class Camels extends AgeableMobRenderer<Camel, CamelRenderState, CamelModel> {
        Camels(EntityRendererProvider.Context context) {
            super(context, new AdultCamelModel(context.bakeLayer(CAMEL)), new BabyCamelModel(context.bakeLayer(ModelLayers.CAMEL_BABY)), 0.7F);
            // the vanilla saddle on its own vanilla-shaped model
            addLayer(new SimpleEquipmentLayer<>(this, context.getEquipmentRenderer(), EquipmentClientInfo.LayerType.CAMEL_SADDLE, state -> state.saddle,
                    new CamelSaddleModel(context.bakeLayer(ModelLayers.CAMEL_SADDLE)), null));
        }

        @Override
        public Identifier getTextureLocation(CamelRenderState state) {
            return texture(state.isBaby, "camel/camel");
        }

        @Override
        public CamelRenderState createRenderState() {
            return new CamelRenderState();
        }

        @Override
        public void extractRenderState(Camel entity, CamelRenderState state, float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            CamelRenderer.extractAdditionalState(entity, state, partialTicks);
        }
    }

    private WildVanillaRenderers() {}
}
