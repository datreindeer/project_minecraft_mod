package com.mechanical.client.wildlife.meadow;

import com.mechanical.client.CarcassRenderer;
import com.mechanical.client.wildlife.WildAnimalRenderState;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextKey;

/**
 * Snake (Adder and Grass Snake share it, each with its own texture): a broad flat head with a forked tongue, then a
 * chain of a neck, five body segments (thickest in the middle) and a thin tail, each hinged on the one before. It moves
 * in a travelling wave (each segment turns by the difference of a sine wave along the body, so the curve runs back down
 * the snake), flicking its tongue; basking or asleep it lies in a tightening coil with the head resting on top; striking
 * (adder) it rears the front up in an S; playing dead (grass snake) it rolls belly up, limp, tongue hanging out.
 * Hung on the rack it hangs straight by the tail. Texture: 64x32, scripts/gen_fauna_meadow_textures.py.
 */
public class SnakeModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "snake"), "main");
    public static final ContextKey<Boolean> COILED = new ContextKey<>(Identifier.fromNamespaceAndPath("mechanical", "snake_coiled"));
    public static final ContextKey<Boolean> BELLY_UP = new ContextKey<>(Identifier.fromNamespaceAndPath("mechanical", "snake_belly_up"));
    /** The rattlesnake (wildlife/desert) buzzing its rattle: the tail tip raised and shaking. */
    public static final ContextKey<Boolean> RATTLING = new ContextKey<>(Identifier.fromNamespaceAndPath("mechanical", "snake_rattling"));

    private final ModelPart neck, head, tongue;
    /** neck, seg2..seg6, tail: each a child of the one before. */
    private final ModelPart[] chain;

    public SnakeModel(ModelPart root) {
        super(root);
        neck = root.getChild("neck");
        head = neck.getChild("head");
        tongue = head.getChild("tongue");
        chain = new ModelPart[7];
        chain[0] = neck;
        String[] names = {"seg2", "seg3", "seg4", "seg5", "seg6", "tail"};
        for (int i = 0; i < names.length; i++) chain[i + 1] = chain[i].getChild(names[i]);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition neck = root.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(50, 0).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 3.0F), PartPose.offset(0.0F, 23.0F, -10.0F));
        PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -1.0F, -4.0F, 3.0F, 2.0F, 4.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        // the tongue is its own part so it can flick in and out
        head.addOrReplaceChild("tongue", CubeListBuilder.create().texOffs(28, 6).addBox(-0.5F, 0.5F, -6.0F, 1.0F, 0.0F, 2.0F, new CubeDeformation(0.001F)), PartPose.ZERO);
        PartDefinition seg2 = neck.addOrReplaceChild("seg2", CubeListBuilder.create().texOffs(14, 0).addBox(-1.5F, -1.0F, 0.0F, 3.0F, 2.0F, 3.0F), PartPose.offset(0.0F, 0.0F, 3.0F));
        PartDefinition seg3 = seg2.addOrReplaceChild("seg3", CubeListBuilder.create().texOffs(26, 0).addBox(-1.5F, -1.0F, 0.0F, 3.0F, 2.0F, 3.0F), PartPose.offset(0.0F, 0.0F, 3.0F));
        PartDefinition seg4 = seg3.addOrReplaceChild("seg4", CubeListBuilder.create().texOffs(38, 0).addBox(-1.5F, -1.0F, 0.0F, 3.0F, 2.0F, 3.0F), PartPose.offset(0.0F, 0.0F, 3.0F));
        PartDefinition seg5 = seg4.addOrReplaceChild("seg5", CubeListBuilder.create().texOffs(0, 6).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 3.0F), PartPose.offset(0.0F, 0.0F, 3.0F));
        PartDefinition seg6 = seg5.addOrReplaceChild("seg6", CubeListBuilder.create().texOffs(10, 6).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 3.0F), PartPose.offset(0.0F, 0.0F, 3.0F));
        seg6.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(20, 6).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 3.0F), PartPose.offset(0.0F, 0.0F, 3.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        tongue.visible = false;
        if (CarcassRenderer.isHung(state)) return; // straight down from the tail

        if (CarcassRenderer.isCarcass(state)) {
            // limp, in a loose S
            for (int i = 0; i < chain.length; i++) chain[i].yRot = 0.3F * Mth.sin(i * 1.3F);
            tongue.visible = true;
            return;
        }
        boolean bellyUp = Boolean.TRUE.equals(state.getRenderData(BELLY_UP));
        if (Boolean.TRUE.equals(state.getRenderData(COILED)) || bellyUp) {
            // a tightening coil, the later turns riding up on the first; the head resting on top, looking out
            for (int i = 1; i < chain.length; i++) {
                chain[i].yRot = 0.8F + 0.09F * i;
                if (i >= 5) chain[i].y -= 0.7F;
            }
            if (bellyUp) {
                neck.zRot = Mth.PI; // rolled over, limp
                head.xRot = 0.3F;
                head.yRot = 0.6F;
                tongue.visible = true;
                return;
            }
            float breathe = Mth.sin(t * 0.06F) * 0.04F;
            head.yRot = -0.7F;
            head.xRot = -0.15F + breathe;
            head.y -= 0.8F;
            tongue.visible = Mth.sin(t * 0.21F) > 0.93F; // a lazy flick now and then
            return;
        }

        float speed = Math.min(state.walkAnimationSpeed * 1.5F, 1.0F);
        float phase = state.walkAnimationPos * 0.9F + t * 0.04F;
        float amp = 0.12F + 0.38F * speed;
        float prev = 0.0F;
        for (int i = 0; i < chain.length; i++) {
            float theta = amp * Mth.sin(phase - i * 0.95F);
            chain[i].yRot = theta - prev;
            prev = theta;
        }
        // the head stays aimed where it's going (and at whatever it watches)
        head.yRot = -chain[0].yRot * 0.8F + Mth.clamp(state.yRot, -35.0F, 35.0F) * Mth.DEG_TO_RAD;
        head.xRot = state.xRot * Mth.DEG_TO_RAD * 0.3F;
        tongue.visible = Mth.sin(t * 0.45F) > 0.75F;
        tongue.z = tongue.visible ? -Mth.abs(Mth.sin(t * 2.5F)) * 0.6F : 0.0F;

        if (state.charging) {
            // reared in an S, head drawn back ready to strike
            neck.y -= 2.0F;
            neck.xRot = 0.6F;
            chain[1].xRot = -0.6F;
            head.xRot = -0.9F; // the neck tips it down: the snout ends up a little raised
            head.z += 0.6F + Mth.sin(t * 0.8F) * 0.3F;
            tongue.visible = true;
        }
        if (Boolean.TRUE.equals(state.getRenderData(RATTLING))) {
            chain[5].xRot = -0.5F; // the tail end held up, the rattle a blur
            chain[6].xRot = -0.9F;
            chain[6].yRot = Mth.sin(t * 3.1F) * 0.35F;
        }
    }
}
