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
 * Ape and monkey (Orangutan; Proboscis Monkey with {@code monkey}: tail, and the male's nose when showsHorns): a stooping
 * body, a broad head with a muzzle, long arms reaching to the ground for a knuckle-walk, short legs. Walks on all fours
 * (arms and legs swinging in the diagonal gait), looks about; asleep it sits hunched with the head sunk. Texture 64x64:
 * scripts/gen_fauna_jungle_textures.py (primate()), the parts below printed by its --java.
 */
public class PrimateModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "primate"), "main");
    private final ModelPart body, head, nose, rightArm, leftArm, rightLeg, leftLeg, tail;
    private final boolean monkey;

    public PrimateModel(ModelPart root, boolean monkey) {
        super(root);
        this.monkey = monkey;
        body = root.getChild("body");
        head = body.getChild("head");
        nose = head.getChild("nose");
        rightArm = body.getChild("right_arm");
        leftArm = body.getChild("left_arm");
        rightLeg = root.getChild("right_leg");
        leftLeg = root.getChild("left_leg");
        tail = body.getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(12, 0).addBox(-4.0F, -9.0F, -3.0F, 8.0F, 10.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 16.0F, 1.0F, 0.6F, 0.0F, 0.0F));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(28, 18).addBox(-3.5F, -6.0F, -3.5F, 7.0F, 6.0F, 6.0F).texOffs(12, 32).addBox(-2.0F, -3.0F, -5.5F, 4.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -9.0F, -1.0F, -0.6F, 0.0F, 0.0F));
        head.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(24, 32).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 1.0F), PartPose.offset(0.0F, -2.5F, -5.5F));
        body.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 15.0F, 3.0F), PartPose.offsetAndRotation(-5.0F, -8.0F, -0.5F, -0.6F, 0.0F, 0.0F));
        body.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-1.5F, 0.0F, -1.5F, 3.0F, 15.0F, 3.0F).mirror(false), PartPose.offsetAndRotation(5.0F, -8.0F, -0.5F, -0.6F, 0.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 32).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 8.0F, 3.0F), PartPose.offset(-2.5F, 16.0F, 2.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 32).mirror().addBox(-1.5F, 0.0F, -1.5F, 3.0F, 8.0F, 3.0F).mirror(false), PartPose.offset(2.5F, 16.0F, 2.0F));
        body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 18).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 13.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 2.5F, -1.6F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        tail.visible = monkey;
        nose.visible = monkey && state.showsHorns;
        float age = state.ageInTicks;
        if (CarcassRenderer.isHung(state)) {
            CarcassRenderer.hangLegs(new ModelPart[] {leftArm, rightArm}, new ModelPart[] {leftLeg, rightLeg});
            return;
        }
        if (CarcassRenderer.isCarcass(state)) {
            body.xRot = 1.45F; // lying on its front
            body.y += 6.0F;
            rightArm.xRot = leftArm.xRot = -1.4F;
            rightLeg.xRot = leftLeg.xRot = 1.3F;
            rightLeg.y = leftLeg.y = 22.0F;
            return;
        }
        if (state.dozing) {
            // sitting hunched, arms folded round the knees, head sunk
            body.xRot = 0.15F;
            body.y += 5.0F;
            head.xRot = 0.0F;
            rightArm.xRot = leftArm.xRot = -0.9F;
            rightArm.zRot = 0.35F;
            leftArm.zRot = -0.35F;
            rightLeg.xRot = leftLeg.xRot = -1.4F;
            rightLeg.y = leftLeg.y = 21.0F;
            head.xRot += 0.3F + Mth.sin(age * 0.06F) * 0.03F;
            return;
        }
        float speed = Math.min(state.walkAnimationSpeed * 1.3F, 1.0F);
        float phase = state.walkAnimationPos * 0.7F;
        float swing = Mth.cos(phase) * 0.7F * speed;
        rightArm.xRot += swing;
        leftLeg.xRot = swing;
        leftArm.xRot -= swing;
        rightLeg.xRot = -swing;
        body.y += Math.abs(Mth.sin(phase)) * 0.6F * speed;
        head.yRot = Mth.clamp(state.yRot, -50.0F, 50.0F) * Mth.DEG_TO_RAD;
        head.xRot += Mth.clamp(state.xRot, -30.0F, 30.0F) * Mth.DEG_TO_RAD;
        if (monkey) tail.xRot += Mth.sin(age * 0.07F) * 0.08F + speed * 0.3F;
        // idle: scratching now and then
        float c = (age + state.entityId * 31) % 160.0F;
        if (speed < 0.1F && c < 30) rightArm.xRot = -1.6F + Mth.sin(c * 0.9F) * 0.25F;
    }
}
