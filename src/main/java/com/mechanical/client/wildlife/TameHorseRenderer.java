package com.mechanical.client.wildlife;

import com.mechanical.wildlife.TameHorse;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;

/**
 * The tamed horse keeps the wild horse's looks: {@link WildHorseModel} / {@link WildHorseFoalModel} with the wild
 * horse textures. Vanilla's eating animation lowers the head as in grazing, rearing tips the whole body up on the hind
 * legs, and the Hide Saddle Blanket is an inflated copy of the model drawn with a texture that only covers the back
 * (textures/entity/wildlife/hide_saddle_blanket.png, scripts/gen_cart_textures.py).
 */
public class TameHorseRenderer extends MobRenderer<TameHorse, WildAnimalRenderState, EntityModel<WildAnimalRenderState>> {
    public static final ModelLayerLocation BLANKET = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "wild_horse"), "blanket");
    private static final Identifier BLANKET_TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/hide_saddle_blanket.png");
    public static final ModelLayerLocation ARMOR = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "wild_horse"), "armor");
    public static final ModelLayerLocation SATCHELS = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "wild_horse"), "satchels");
    private static final Identifier ARMOR_TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/leather_horse_armor.png");
    private static final Identifier SATCHEL_TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/tame_horse_satchels.png");
    /** Hind leg pivot (blocks behind the middle) and how far up it rears. */
    private static final float HIND_Z = 7.5F / 16.0F, REAR_DEGREES = 40.0F;
    private final EntityModel<WildAnimalRenderState> adult, foal;

    public TameHorseRenderer(EntityRendererProvider.Context context) {
        super(context, new WildHorseModel(context.bakeLayer(WildHorseModel.LAYER)), 0.8F);
        adult = model;
        foal = new WildHorseFoalModel(context.bakeLayer(WildHorseFoalModel.LAYER));
        addLayer(new StuckArrowLayer(this, context));
        addLayer(new ArmorLayer(this, new WildHorseModel(context.bakeLayer(ARMOR))));
        addLayer(new BlanketLayer(this, new WildHorseModel(context.bakeLayer(BLANKET))));
        addLayer(new SatchelLayer(this, new WildHorseModel(context.bakeLayer(SATCHELS))));
    }

    @Override
    public WildAnimalRenderState createRenderState() {
        return new WildAnimalRenderState();
    }

    @Override
    public void extractRenderState(TameHorse horse, WildAnimalRenderState state, float partialTicks) {
        super.extractRenderState(horse, state, partialTicks);
        state.texture = horse.texture();
        state.grazeAmount = horse.getEatAnim(partialTicks);
        state.stand = horse.getStandAnim(partialTicks);
        state.saddled = horse.isSaddled();
        state.satchels = horse.satchelCount();
        state.dozing = com.mechanical.HorseWork.exhausted(horse); // all in: lies down until rested (HorseWork)
        ItemStack armor = horse.getBodyArmorItem();
        // shortcut: every horse armour is drawn with the leather texture (only leather is reachable yet); give the metal ones their own when they come back
        state.armorColor = armor.isEmpty() ? 0 : armor.is(Items.LEATHER_HORSE_ARMOR) ? ARGB.opaque(DyedItemColor.getOrDefault(armor, DyedItemColor.LEATHER_COLOR)) : -1;
        state.arrowCount = horse.getArrowCount();
        state.entityId = horse.getId();
        state.swimming = horse.isInWater() && !horse.onGround() && horse.isAlive();
        state.swimSink = 0.0F;
        if (state.swimming) WildAnimalRenderer.swimPose(horse, state, WildAnimalRenderer.LAND_DEPTH);
    }

    @Override
    public void submit(WildAnimalRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        model = state.isBaby ? foal : adult;
        super.submit(state, poseStack, collector, camera);
    }

    @Override
    protected void setupRotations(WildAnimalRenderState state, PoseStack poseStack, float bodyRot, float entityScale) {
        super.setupRotations(state, poseStack, bodyRot, entityScale);
        WildAnimalRenderer.swimRotations(state, poseStack, entityScale);
        if (state.stand <= 0 || state.deathTime > 0) return;
        poseStack.translate(0, 0, HIND_Z);
        poseStack.rotateDegrees(Axis.XP, state.stand * REAR_DEGREES);
        poseStack.translate(0, 0, -HIND_Z);
    }

    @Override
    public Identifier getTextureLocation(WildAnimalRenderState state) {
        return state.texture;
    }

    static class BlanketLayer extends RenderLayer<WildAnimalRenderState, EntityModel<WildAnimalRenderState>> {
        private final WildHorseModel blanket;

        BlanketLayer(RenderLayerParent<WildAnimalRenderState, EntityModel<WildAnimalRenderState>> parent, WildHorseModel blanket) {
            super(parent);
            this.blanket = blanket;
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, WildAnimalRenderState state, float yRot, float xRot) {
            if (state.saddled && !state.isBaby) coloredCutoutModelCopyLayerRender(blanket, BLANKET_TEXTURE, poseStack, collector, lightCoords, state, -1, 1);
        }
    }

    /** Horse armour: an inflated model (under the blanket) in a pale leather texture tinted with the armour's dye. */
    static class ArmorLayer extends RenderLayer<WildAnimalRenderState, EntityModel<WildAnimalRenderState>> {
        private final WildHorseModel armor;

        ArmorLayer(RenderLayerParent<WildAnimalRenderState, EntityModel<WildAnimalRenderState>> parent, WildHorseModel armor) {
            super(parent);
            this.armor = armor;
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, WildAnimalRenderState state, float yRot, float xRot) {
            if (state.armorColor != 0 && !state.isBaby) coloredCutoutModelCopyLayerRender(armor, ARMOR_TEXTURE, poseStack, collector, lightCoords, state, state.armorColor, 1);
        }
    }

    /** Saddlebags: the first satchel hangs on the left flank, the second on the right. */
    static class SatchelLayer extends RenderLayer<WildAnimalRenderState, EntityModel<WildAnimalRenderState>> {
        private final WildHorseModel model;
        private final ModelPart left, right;

        SatchelLayer(RenderLayerParent<WildAnimalRenderState, EntityModel<WildAnimalRenderState>> parent, WildHorseModel model) {
            super(parent);
            this.model = model;
            ModelPart body = model.root().getChild("body");
            this.left = body.getChild(WildHorseModel.LEFT_SATCHEL);
            this.right = body.getChild(WildHorseModel.RIGHT_SATCHEL);
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, WildAnimalRenderState state, float yRot, float xRot) {
            if (state.satchels == 0 || state.isBaby) return;
            left.visible = true;
            right.visible = state.satchels > 1;
            coloredCutoutModelCopyLayerRender(model, SATCHEL_TEXTURE, poseStack, collector, lightCoords, state, -1, 2);
        }
    }
}
