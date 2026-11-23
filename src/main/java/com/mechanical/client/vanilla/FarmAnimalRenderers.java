package com.mechanical.client.vanilla;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.Maps;
import com.mechanical.Mechanical;
import com.mechanical.client.CarcassRenderer;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.AdultAndBabyModelPair;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.animal.chicken.AdultChickenModel;
import net.minecraft.client.model.animal.chicken.ChickenModel;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.client.model.animal.pig.PigModel;
import net.minecraft.client.model.animal.sheep.SheepModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.renderer.entity.state.ChickenRenderState;
import net.minecraft.client.renderer.entity.state.CowRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.PigRenderState;
import net.minecraft.client.renderer.entity.state.SheepRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.chicken.ChickenVariant;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.cow.CowVariant;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.pig.PigVariant;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * The vanilla farm animals in the mod's style: own models ({@link VanillaGeometry}, scripts/gen_vanilla_models.py) for
 * the vanilla entity types, the vanilla render states and the vanilla variants (each variant texture has its own skin in
 * textures/entity/vanilla/, same file name). The models extend the vanilla model classes and keep their part names, so
 * vanilla animation, the livestock poses (LivestockPose) and the carcass poses (QuadrupedModelMixin) work unchanged.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class FarmAnimalRenderers {
    static final ModelLayerLocation COW = layer("cow"), WARM_COW = layer("warm_cow"), COLD_COW = layer("cold_cow"), CALF = layer("calf");
    static final ModelLayerLocation PIG = layer("pig"), COLD_PIG = layer("cold_pig"), PIGLET = layer("piglet");
    static final ModelLayerLocation SHEEP = layer("sheep"), FLEECE = layer("fleece"), LAMB = layer("lamb"), LAMB_FLEECE = layer("lamb_fleece");
    static final ModelLayerLocation CHICKEN = layer("chicken"), COLD_CHICKEN = layer("cold_chicken"), CHICK = layer("chick");

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mechanical.MODID, "vanilla_" + name), "main");
    }

    @SubscribeEvent
    static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(COW, VanillaGeometry::cow);
        event.registerLayerDefinition(WARM_COW, VanillaGeometry::warmCow);
        event.registerLayerDefinition(COLD_COW, VanillaGeometry::coldCow);
        event.registerLayerDefinition(CALF, VanillaGeometry::calf);
        event.registerLayerDefinition(PIG, VanillaGeometry::pig);
        event.registerLayerDefinition(COLD_PIG, VanillaGeometry::coldPig);
        event.registerLayerDefinition(PIGLET, VanillaGeometry::piglet);
        event.registerLayerDefinition(SHEEP, VanillaGeometry::sheep);
        event.registerLayerDefinition(FLEECE, VanillaGeometry::fleece);
        event.registerLayerDefinition(LAMB, VanillaGeometry::lamb);
        event.registerLayerDefinition(LAMB_FLEECE, VanillaGeometry::lambFleece);
        event.registerLayerDefinition(CHICKEN, VanillaGeometry::chicken);
        event.registerLayerDefinition(COLD_CHICKEN, VanillaGeometry::coldChicken);
        event.registerLayerDefinition(CHICK, VanillaGeometry::chick);
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityTypes.COW, Cows::new);
        event.registerEntityRenderer(EntityTypes.PIG, Pigs::new);
        event.registerEntityRenderer(EntityTypes.SHEEP, Sheep::new);
        event.registerEntityRenderer(EntityTypes.CHICKEN, Chickens::new);
    }

    private static final Map<Identifier, Identifier> SKINS = new ConcurrentHashMap<>();

    /** minecraft:textures/entity/cow/cow_warm.png -> mechanical:textures/entity/vanilla/cow/cow_warm.png */
    static Identifier skin(Identifier vanilla) {
        return SKINS.computeIfAbsent(vanilla, t -> Identifier.fromNamespaceAndPath(Mechanical.MODID, t.getPath().replace("textures/entity/", "textures/entity/vanilla/")));
    }

    /** The tail swings a little while walking (and idly); not on a carcass. */
    static void swayTail(ModelPart tail, LivingEntityRenderState state) {
        if (CarcassRenderer.isCarcass(state) || CarcassRenderer.isHung(state)) return;
        tail.zRot = Mth.sin(state.ageInTicks * 0.09F) * 0.06F + Mth.cos(state.walkAnimationPos * 0.6662F) * 0.3F * state.walkAnimationSpeed;
    }

    /** With the neck far down (grazing, a trough) the face flexes back, so the muzzle meets the ground instead of the chest. */
    static void flexSkull(ModelPart head, ModelPart skull) {
        if (head.xRot > 0.4F) skull.xRot -= 0.6F * (head.xRot - 0.4F);
    }

    // ---------------------------------------------------------------------------------------------------------- cow
    static final class FarmCowModel extends CowModel {
        private final ModelPart tail, skull;

        FarmCowModel(ModelPart root) {
            super(root);
            tail = body.getChild("tail");
            skull = head.getChild("neck").getChild("skull");
        }

        @Override
        public void setupAnim(LivingEntityRenderState state) {
            super.setupAnim(state);
            swayTail(tail, state);
            flexSkull(head, skull);
        }
    }

    static final class Cows extends MobRenderer<Cow, CowRenderState, CowModel> {
        private final Map<CowVariant.ModelType, AdultAndBabyModelPair<CowModel>> models;

        Cows(EntityRendererProvider.Context context) {
            super(context, new FarmCowModel(context.bakeLayer(COW)), 0.7F);
            CowModel calf = new FarmCowModel(context.bakeLayer(CALF));
            models = Maps.newEnumMap(Map.of(
                    CowVariant.ModelType.NORMAL, new AdultAndBabyModelPair<>(model, calf),
                    CowVariant.ModelType.WARM, new AdultAndBabyModelPair<>(new FarmCowModel(context.bakeLayer(WARM_COW)), calf),
                    CowVariant.ModelType.COLD, new AdultAndBabyModelPair<>(new FarmCowModel(context.bakeLayer(COLD_COW)), calf)));
        }

        @Override
        public Identifier getTextureLocation(CowRenderState state) {
            if (state.variant == null) return MissingTextureAtlasSprite.getLocation();
            return skin(state.isBaby ? state.variant.babyTexture().texturePath() : state.variant.modelAndTexture().asset().texturePath());
        }

        @Override
        public CowRenderState createRenderState() {
            return new CowRenderState();
        }

        @Override
        public void extractRenderState(Cow entity, CowRenderState state, float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            state.variant = entity.getVariant().value();
        }

        @Override
        public void submit(CowRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
            if (state.variant == null) return;
            model = models.get(state.variant.modelAndTexture().model()).getModel(state.isBaby);
            super.submit(state, poseStack, collector, camera);
        }
    }

    // ---------------------------------------------------------------------------------------------------------- pig
    static final class FarmPigModel extends PigModel {
        private final ModelPart tail, skull;

        FarmPigModel(ModelPart root) {
            super(root);
            tail = body.getChild("tail");
            skull = head.getChild("skull");
        }

        @Override
        public void setupAnim(LivingEntityRenderState state) {
            super.setupAnim(state);
            swayTail(tail, state);
            flexSkull(head, skull);
        }
    }

    static final class Pigs extends MobRenderer<Pig, PigRenderState, PigModel> {
        private final Map<PigVariant.ModelType, AdultAndBabyModelPair<PigModel>> models;

        Pigs(EntityRendererProvider.Context context) {
            super(context, new FarmPigModel(context.bakeLayer(PIG)), 0.7F);
            PigModel piglet = new FarmPigModel(context.bakeLayer(PIGLET));
            models = Maps.newEnumMap(Map.of(
                    PigVariant.ModelType.NORMAL, new AdultAndBabyModelPair<>(model, piglet),
                    PigVariant.ModelType.COLD, new AdultAndBabyModelPair<>(new FarmPigModel(context.bakeLayer(COLD_PIG)), piglet)));
            // the vanilla saddle, on its own vanilla-shaped model as in PigRenderer
            addLayer(new SimpleEquipmentLayer<>(this, context.getEquipmentRenderer(), EquipmentClientInfo.LayerType.PIG_SADDLE,
                    state -> state.saddle, new PigModel(context.bakeLayer(ModelLayers.PIG_SADDLE)), null));
        }

        @Override
        public Identifier getTextureLocation(PigRenderState state) {
            if (state.variant == null) return MissingTextureAtlasSprite.getLocation();
            return skin(state.isBaby ? state.variant.babyTexture().texturePath() : state.variant.modelAndTexture().asset().texturePath());
        }

        @Override
        public PigRenderState createRenderState() {
            return new PigRenderState();
        }

        @Override
        public void extractRenderState(Pig entity, PigRenderState state, float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            state.saddle = entity.getItemBySlot(EquipmentSlot.SADDLE).copy();
            state.variant = entity.getVariant().value();
        }

        @Override
        public void submit(PigRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
            if (state.variant == null) return;
            model = models.get(state.variant.modelAndTexture().model()).getModel(state.isBaby);
            super.submit(state, poseStack, collector, camera);
        }
    }

    // ---------------------------------------------------------------------------------------------------------- sheep
    /** Body and fleece share this class (same parts), so the fleece follows every pose of the body. */
    static final class FarmSheepModel extends SheepModel {
        private final ModelPart tail, skull;

        FarmSheepModel(ModelPart root) {
            super(root);
            tail = body.getChild("tail");
            skull = head.getChild("neck").getChild("skull");
        }

        @Override
        public void setupAnim(SheepRenderState state) {
            super.setupAnim(state);
            // vanilla drops a short head 9 px to graze; ours hangs from the neck base, so less drop and more nod
            head.y -= state.headEatPositionScale * 5.0F * state.ageScale;
            head.xRot += state.headEatPositionScale * state.headEatAngleScale;
            swayTail(tail, state);
            flexSkull(head, skull);
        }
    }

    static final class Sheep extends AgeableMobRenderer<net.minecraft.world.entity.animal.sheep.Sheep, SheepRenderState, SheepModel> {
        private static final Identifier SKIN = vanilla("sheep/sheep"), LAMB_SKIN = vanilla("sheep/sheep_baby");

        Sheep(EntityRendererProvider.Context context) {
            super(context, new FarmSheepModel(context.bakeLayer(SHEEP)), new FarmSheepModel(context.bakeLayer(LAMB)), 0.7F);
            addLayer(new Fleece(this, context));
        }

        @Override
        public Identifier getTextureLocation(SheepRenderState state) {
            return state.isBaby ? LAMB_SKIN : SKIN;
        }

        @Override
        public SheepRenderState createRenderState() {
            return new SheepRenderState();
        }

        @Override
        public void extractRenderState(net.minecraft.world.entity.animal.sheep.Sheep entity, SheepRenderState state, float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            state.headEatAngleScale = entity.getHeadEatAngleScale(partialTicks);
            state.headEatPositionScale = entity.getHeadEatPositionScale(partialTicks);
            state.isSheared = entity.isSheared();
            state.woolColor = entity.getColor();
            state.isJebSheep = checkMagicName(entity, "jeb_");
        }
    }

    /** The fleece in the wool colour (as SheepWoolLayer; the grey-white skin takes the tint), gone once shorn. */
    static final class Fleece extends RenderLayer<SheepRenderState, SheepModel> {
        private static final Identifier WOOL = vanilla("sheep/sheep_wool"), LAMB_WOOL = vanilla("sheep/sheep_wool_baby");
        private final EntityModel<SheepRenderState> adult, lamb;

        Fleece(RenderLayerParent<SheepRenderState, SheepModel> renderer, EntityRendererProvider.Context context) {
            super(renderer);
            adult = new FarmSheepModel(context.bakeLayer(FLEECE));
            lamb = new FarmSheepModel(context.bakeLayer(LAMB_FLEECE));
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, SheepRenderState state, float yRot, float xRot) {
            if (state.isSheared) return;
            EntityModel<SheepRenderState> model = state.isBaby ? lamb : adult;
            Identifier texture = state.isBaby ? LAMB_WOOL : WOOL;
            if (state.isInvisible) {
                if (state.appearsGlowing()) collector.submitModel(model, state, poseStack, RenderTypes.outline(texture), lightCoords,
                        LivingEntityRenderer.getOverlayCoords(state, 0.0F), -16777216, null, state.outlineColor);
                return;
            }
            coloredCutoutModelCopyLayerRender(model, texture, poseStack, collector, lightCoords, state, state.getWoolColor(), state.isBaby ? 1 : 0);
        }
    }

    // ---------------------------------------------------------------------------------------------------------- chicken
    static final class FarmChickenModel extends AdultChickenModel {
        private final ModelPart tail;

        FarmChickenModel(ModelPart root) {
            super(root);
            ModelPart body = root.getChild("body");
            tail = body.hasChild("tail") ? body.getChild("tail") : null;
        }

        @Override
        public void setupAnim(ChickenRenderState state) {
            super.setupAnim(state);
            // the hen bobs her tail as she walks
            if (tail != null && !CarcassRenderer.isCarcass(state)) tail.xRot += Mth.cos(state.walkAnimationPos * 1.3F) * 0.15F * state.walkAnimationSpeed;
        }
    }

    static final class Chickens extends MobRenderer<Chicken, ChickenRenderState, ChickenModel> {
        private final Map<ChickenVariant.ModelType, AdultAndBabyModelPair<ChickenModel>> models;

        Chickens(EntityRendererProvider.Context context) {
            super(context, new FarmChickenModel(context.bakeLayer(CHICKEN)), 0.3F);
            ChickenModel chick = new FarmChickenModel(context.bakeLayer(CHICK));
            models = Maps.newEnumMap(Map.of(
                    ChickenVariant.ModelType.NORMAL, new AdultAndBabyModelPair<>(model, chick),
                    ChickenVariant.ModelType.COLD, new AdultAndBabyModelPair<>(new FarmChickenModel(context.bakeLayer(COLD_CHICKEN)), chick)));
        }

        @Override
        public Identifier getTextureLocation(ChickenRenderState state) {
            if (state.variant == null) return MissingTextureAtlasSprite.getLocation();
            return skin(state.isBaby ? state.variant.babyTexture().texturePath() : state.variant.modelAndTexture().asset().texturePath());
        }

        @Override
        public ChickenRenderState createRenderState() {
            return new ChickenRenderState();
        }

        @Override
        public void extractRenderState(Chicken entity, ChickenRenderState state, float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            state.flap = Mth.lerp(partialTicks, entity.oFlap, entity.flap);
            state.flapSpeed = Mth.lerp(partialTicks, entity.oFlapSpeed, entity.flapSpeed);
            state.variant = entity.getVariant().value();
        }

        @Override
        public void submit(ChickenRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
            if (state.variant == null) return;
            model = models.get(state.variant.modelAndTexture().model()).getModel(state.isBaby);
            super.submit(state, poseStack, collector, camera);
        }
    }

    static Identifier vanilla(String path) {
        return Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/entity/vanilla/" + path + ".png");
    }

    private FarmAnimalRenderers() {}
}
