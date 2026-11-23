package com.mechanical.client.wildlife.temperate;

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
 * Giant stag beetle: a broad, glossy body under the wing cases, a narrower thorax and a flat head; the male carries two
 * long, curved, antler-like jaws (shown by {@code showsHorns}), the female a short pair. Six splayed legs that row as it
 * walks. Rearing (the {@code charging} flag): front end up, jaws raised and open. Texture 32x32:
 * textures/entity/wildlife/temperate/stag_beetle(_female).png, scripts/gen_fauna_temperate_textures.py.
 */
public class StagBeetleModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "stag_beetle"), "main");

    private final ModelPart body, thorax, head, rightJaw, leftJaw, smallJaws;
    private final ModelPart[] rightLegs = new ModelPart[3], leftLegs = new ModelPart[3];

    public StagBeetleModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        thorax = body.getChild("thorax");
        head = thorax.getChild("head");
        rightJaw = head.getChild("right_jaw");
        leftJaw = head.getChild("left_jaw");
        smallJaws = head.getChild("small_jaws");
        for (int i = 0; i < 3; i++) {
            rightLegs[i] = root.getChild("right_leg_" + i);
            leftLegs[i] = root.getChild("left_leg_" + i);
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -1.5F, -1.0F, 4.0F, 3.0F, 5.0F),
                PartPose.offset(0.0F, 21.8F, -0.5F));
        PartDefinition thorax = body.addOrReplaceChild("thorax", CubeListBuilder.create().texOffs(0, 8).addBox(-1.5F, -1.25F, -2.0F, 3.0F, 2.5F, 2.0F),
                PartPose.offset(0.0F, -0.1F, -1.0F));
        PartDefinition head = thorax.addOrReplaceChild("head", CubeListBuilder.create().texOffs(10, 8).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 2.0F, 1.5F),
                PartPose.offset(0.0F, 0.0F, -2.0F));
        head.addOrReplaceChild("right_jaw", CubeListBuilder.create().texOffs(0, 14).addBox(-0.5F, -0.5F, -4.0F, 1.0F, 1.0F, 4.0F)
                .texOffs(10, 18).addBox(-0.5F, -0.5F, -4.5F, 1.5F, 1.0F, 0.5F), PartPose.offsetAndRotation(-1.0F, -0.2F, -1.2F, -0.05F, -0.3F, 0.0F));
        head.addOrReplaceChild("left_jaw", CubeListBuilder.create().texOffs(0, 14).mirror().addBox(-0.5F, -0.5F, -4.0F, 1.0F, 1.0F, 4.0F)
                .texOffs(10, 18).mirror().addBox(-1.0F, -0.5F, -4.5F, 1.5F, 1.0F, 0.5F).mirror(false), PartPose.offsetAndRotation(1.0F, -0.2F, -1.2F, -0.05F, 0.3F, 0.0F));
        head.addOrReplaceChild("small_jaws", CubeListBuilder.create().texOffs(10, 14).addBox(-1.0F, -0.5F, -1.5F, 2.0F, 1.0F, 1.5F),
                PartPose.offset(0.0F, 0.2F, -1.4F));
        float[] legZ = {-2.6F, -1.0F, 0.8F};
        for (int i = 0; i < 3; i++) {
            root.addOrReplaceChild("right_leg_" + i, CubeListBuilder.create().texOffs(20, 0).addBox(-2.5F, -0.25F, -0.25F, 2.5F, 0.5F, 0.5F),
                    PartPose.offsetAndRotation(-1.6F, 22.6F, legZ[i], 0.0F, (i - 1) * -0.5F, -0.45F));
            root.addOrReplaceChild("left_leg_" + i, CubeListBuilder.create().texOffs(20, 0).mirror().addBox(0.0F, -0.25F, -0.25F, 2.5F, 0.5F, 0.5F).mirror(false),
                    PartPose.offsetAndRotation(1.6F, 22.6F, legZ[i], 0.0F, (i - 1) * 0.5F, 0.45F));
        }
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        rightJaw.visible = leftJaw.visible = state.showsHorns;
        smallJaws.visible = !state.showsHorns;
        if (CarcassRenderer.isCarcass(state) || CarcassRenderer.isHung(state)) {
            for (int i = 0; i < 3; i++) {
                rightLegs[i].zRot = -1.2F;
                leftLegs[i].zRot = 1.2F;
            }
            return;
        }
        float speed = Math.min(state.walkAnimationSpeed * 2.0F, 1.0F);
        float phase = state.walkAnimationPos * 1.6F;
        for (int i = 0; i < 3; i++) {
            // the legs row in two tripods: front and hind of one side with the middle of the other
            float swing = Mth.cos(phase + (i % 2 == 0 ? 0.0F : Mth.PI)) * 0.45F * speed;
            rightLegs[i].yRot += swing;
            leftLegs[i].yRot -= swing;
        }
        head.yRot = Mth.clamp(state.yRot, -25.0F, 25.0F) * Mth.DEG_TO_RAD;
        if (state.charging) {
            // threat display: front reared, head up, jaws spread
            body.xRot = -0.15F;
            thorax.xRot = -0.1F;
            head.xRot = -0.1F + Mth.sin(state.ageInTicks * 0.5F) * 0.05F;
            rightJaw.yRot -= 0.3F;
            leftJaw.yRot += 0.3F;
        }
    }
}
