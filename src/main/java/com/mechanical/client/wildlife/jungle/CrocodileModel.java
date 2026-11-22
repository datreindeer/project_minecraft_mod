package com.mechanical.client.wildlife.jungle;

import com.mechanical.client.CarcassRenderer;
import com.mechanical.client.wildlife.WildAnimalRenderState;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Saltwater crocodile: long armoured body, a long snout over a lower jaw that gapes, eye bumps on top of the head, a
 * four-part tapering tail and sprawled legs. Walks in the sprawling side-to-side wave; swimming the legs fold back and
 * the tail sculls; lurking it lies dead still, jaws shut; seizing it gapes and the renderer rolls the whole animal
 * (JungleRenderer). Texture 128x64: scripts/gen_fauna_jungle_textures.py (crocodile()).
 */
public class CrocodileModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "saltwater_crocodile"), "main");
    private static final float SPRAWL = 0.7F;
    private final ModelPart body, head, jaw;
    private final ModelPart[] tail = new ModelPart[4];
    private final ModelPart rightFront, leftFront, rightHind, leftHind;

    public CrocodileModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        head = body.getChild("head");
        jaw = head.getChild("jaw");
        ModelPart t = body;
        for (int i = 0; i < 4; i++) tail[i] = t = t.getChild("tail" + i);
        rightFront = body.getChild("right_front_leg");
        leftFront = body.getChild("left_front_leg");
        rightHind = body.getChild("right_hind_leg");
        leftHind = body.getChild("left_hind_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -3.0F, -9.0F, 10.0F, 5.0F, 18.0F), PartPose.offset(0.0F, 19.0F, 0.0F));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(80, 23).addBox(-3.5F, -2.0F, -6.0F, 7.0F, 3.0F, 6.0F).texOffs(0, 23).addBox(-2.5F, -1.5F, -15.0F, 5.0F, 2.0F, 9.0F).texOffs(12, 34).addBox(-3.0F, -3.0F, -5.0F, 2.0F, 1.0F, 2.0F).texOffs(12, 34).mirror().addBox(1.0F, -3.0F, -5.0F, 2.0F, 1.0F, 2.0F).mirror(false), PartPose.offset(0.0F, -1.0F, -9.0F));
        head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(88, 0).addBox(-2.5F, 0.0F, -10.0F, 5.0F, 1.0F, 10.0F), PartPose.offset(0.0F, 1.0F, -5.0F));
        PartDefinition tail0 = body.addOrReplaceChild("tail0", CubeListBuilder.create().texOffs(56, 0).addBox(-4.0F, -2.0F, 0.0F, 8.0F, 4.0F, 8.0F), PartPose.offset(0.0F, -1.0F, 9.0F));
        PartDefinition tail1 = tail0.addOrReplaceChild("tail1", CubeListBuilder.create().texOffs(28, 23).addBox(-3.0F, -2.0F, 0.0F, 6.0F, 3.0F, 8.0F), PartPose.offset(0.0F, 0.5F, 8.0F));
        PartDefinition tail2 = tail1.addOrReplaceChild("tail2", CubeListBuilder.create().texOffs(56, 23).addBox(-2.0F, -1.5F, 0.0F, 4.0F, 2.0F, 8.0F), PartPose.offset(0.0F, 0.0F, 8.0F));
        tail2.addOrReplaceChild("tail3", CubeListBuilder.create().texOffs(106, 23).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 7.0F), PartPose.offset(0.0F, 0.0F, 8.0F));
        body.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(0, 34).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 5.0F, 3.0F), PartPose.offsetAndRotation(-4.5F, 1.0F, -6.0F, 0.0F, 0.0F, SPRAWL));
        body.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(0, 34).mirror().addBox(-1.5F, 0.0F, -1.5F, 3.0F, 5.0F, 3.0F).mirror(false), PartPose.offsetAndRotation(4.5F, 1.0F, -6.0F, 0.0F, 0.0F, -SPRAWL));
        body.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(0, 34).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 5.0F, 3.0F), PartPose.offsetAndRotation(-4.5F, 1.0F, 6.0F, 0.0F, 0.0F, SPRAWL));
        body.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(0, 34).mirror().addBox(-1.5F, 0.0F, -1.5F, 3.0F, 5.0F, 3.0F).mirror(false), PartPose.offsetAndRotation(4.5F, 1.0F, 6.0F, 0.0F, 0.0F, -SPRAWL));
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        float age = state.ageInTicks;
        ModelPart[] legs = {rightFront, leftFront, rightHind, leftHind};
        if (CarcassRenderer.isHung(state)) {
            CarcassRenderer.hangLegs(new ModelPart[] {leftFront, rightFront}, new ModelPart[] {leftHind, rightHind});
            return;
        }
        if (CarcassRenderer.isCarcass(state)) {
            for (ModelPart leg : legs) leg.zRot *= 1.8F; // splayed flat
            jaw.xRot = 0.3F;
            for (int i = 0; i < 4; i++) tail[i].yRot = 0.12F;
            return;
        }
        boolean rolling = Boolean.TRUE.equals(state.getRenderData(JungleRenderer.ROLLING));
        boolean lurking = Boolean.TRUE.equals(state.getRenderData(JungleRenderer.LURKING));
        if (rolling) {
            jaw.xRot = 0.5F; // jaws clamped round the prey, a gap showing
            for (int i = 0; i < 4; i++) tail[i].yRot = Mth.sin(age * 0.6F - i) * 0.25F;
            for (ModelPart leg : legs) leg.xRot = 1.2F * Math.signum(leg.z); // legs tucked to the body
            return;
        }
        if (state.swimming || lurking) {
            // legs folded back along the flanks, the tail sculling (lurking: barely)
            for (ModelPart leg : legs) {
                leg.xRot = 1.3F;
                leg.zRot *= 0.4F;
            }
            float amp = lurking ? 0.03F : 0.35F;
            for (int i = 0; i < 4; i++) tail[i].yRot = Mth.sin(age * (lurking ? 0.05F : 0.3F) - i * 0.9F) * amp;
            head.yRot = lurking ? 0.0F : Mth.clamp(state.yRot, -20.0F, 20.0F) * Mth.DEG_TO_RAD;
            return;
        }
        float speed = Math.min(state.walkAnimationSpeed * 1.4F, 1.0F);
        float phase = state.walkAnimationPos * 0.9F;
        for (ModelPart leg : legs) {
            boolean a = leg == rightFront || leg == leftHind;
            float p = a ? phase : phase + Mth.PI;
            leg.xRot = Mth.cos(p) * 0.7F * speed;
            leg.zRot -= Math.signum(leg.x) * Math.max(0.0F, -Mth.sin(p)) * 0.4F * speed;
        }
        float wave = Mth.sin(phase) * speed;
        body.yRot = wave * 0.12F;
        for (int i = 0; i < 4; i++) tail[i].yRot = Mth.sin(phase - 1.0F - i * 0.8F) * 0.25F * speed + Mth.sin(age * 0.04F + i) * 0.03F;
        head.yRot = -wave * 0.1F + Mth.clamp(state.yRot, -30.0F, 30.0F) * Mth.DEG_TO_RAD;
        // the gape: basking crocodiles open their jaws now and then
        float c = (age + state.entityId * 53) % 300.0F;
        jaw.xRot = speed < 0.1F && c < 80 ? Mth.sin(Math.min(c, 80 - c) / 20.0F * Mth.HALF_PI) * 0.55F : 0.0F;
        if (jaw.xRot < 0) jaw.xRot = 0;
    }
}
