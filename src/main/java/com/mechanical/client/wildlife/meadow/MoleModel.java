package com.mechanical.client.wildlife.meadow;

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
import net.minecraft.util.context.ContextKey;

/**
 * Mole: a velvet cylinder of a body low on the ground, a pointed pink snout, the big spade forepaws turned out at the
 * sides like oars, tiny hind feet and a short tail. It shuffles along rowing with the forepaws, snout always working;
 * digging in, it noses down and scoops the ground away with both paws while it sinks. (Burrowed it isn't drawn: the
 * entity is invisible.) Texture: textures/entity/wildlife/mole.png (64x32), scripts/gen_fauna_meadow_textures.py.
 */
public class MoleModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "mole"), "main");
    /** 0..1 progress of digging in (Mole#digProgress). */
    public static final ContextKey<Float> DIG = new ContextKey<>(Identifier.fromNamespaceAndPath("mechanical", "mole_dig"));

    private final ModelPart body, head, rightHand, leftHand, rightFoot, leftFoot, tail;

    public MoleModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        head = body.getChild("head");
        rightHand = body.getChild("right_hand");
        leftHand = body.getChild("left_hand");
        rightFoot = root.getChild("right_foot");
        leftFoot = root.getChild("left_foot");
        tail = body.getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -2.0F, -3.5F, 4.0F, 4.0F, 7.0F), PartPose.offset(0.0F, 21.5F, 0.0F));
        body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(30, 0).addBox(-1.5F, -1.5F, -2.0F, 3.0F, 3.0F, 2.0F).texOffs(46, 0).addBox(-0.5F, -0.5F, -4.0F, 1.0F, 1.0F, 2.0F), PartPose.offset(0.0F, 0.5F, -3.5F));
        body.addOrReplaceChild("right_hand", CubeListBuilder.create().texOffs(22, 0).addBox(-1.0F, -1.0F, -1.5F, 1.0F, 3.0F, 3.0F), PartPose.offsetAndRotation(-2.0F, 1.0F, -2.0F, 0.0F, 0.0F, 0.4F));
        body.addOrReplaceChild("left_hand", CubeListBuilder.create().texOffs(22, 0).mirror().addBox(0.0F, -1.0F, -1.5F, 1.0F, 3.0F, 3.0F).mirror(false), PartPose.offsetAndRotation(2.0F, 1.0F, -2.0F, 0.0F, 0.0F, -0.4F));
        root.addOrReplaceChild("right_foot", CubeListBuilder.create().texOffs(40, 0).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 1.0F, 2.0F), PartPose.offset(-1.5F, 23.0F, 2.5F));
        root.addOrReplaceChild("left_foot", CubeListBuilder.create().texOffs(40, 0).mirror().addBox(-0.5F, 0.0F, -1.0F, 1.0F, 1.0F, 2.0F).mirror(false), PartPose.offset(1.5F, 23.0F, 2.5F));
        body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(52, 0).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -1.0F, 3.5F, 0.6F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        if (CarcassRenderer.isHung(state)) {
            // by the hind feet: the paws hang down past the snout
            CarcassRenderer.hangLegs(new ModelPart[0], new ModelPart[] {rightFoot, leftFoot});
            rightHand.xRot = leftHand.xRot = -0.9F;
            tail.xRot = 0.0F;
            return;
        }
        if (CarcassRenderer.isCarcass(state)) {
            rightHand.zRot = 0.9F;
            leftHand.zRot = -0.2F;
            head.yRot = state.yRot * Mth.DEG_TO_RAD * 0.5F;
            return;
        }
        float dig = state.getRenderDataOrDefault(DIG, 0.0F);
        if (dig > 0) {
            // nose down, paws scooping out and back in turn, sinking into the hole
            float scoop = Mth.sin(t * 1.4F);
            head.xRot = 0.6F;
            rightHand.xRot = 0.6F + scoop * 0.7F;
            leftHand.xRot = 0.6F - scoop * 0.7F;
            body.xRot = 0.35F;
            body.y += dig * 3.0F;
            rightFoot.y += dig * 2.0F;
            leftFoot.y += dig * 2.0F;
            return;
        }
        float speed = Math.min(state.walkAnimationSpeed * 1.6F, 1.0F);
        float phase = state.walkAnimationPos * 1.8F;
        // rows itself along: the forepaws swing back in turn, the body rolls a little
        rightHand.xRot = Mth.cos(phase) * 0.8F * speed;
        leftHand.xRot = -Mth.cos(phase) * 0.8F * speed;
        rightFoot.xRot = -Mth.cos(phase) * 0.9F * speed;
        leftFoot.xRot = Mth.cos(phase) * 0.9F * speed;
        body.zRot = Mth.sin(phase) * 0.06F * speed;
        // the snout never stops: sniffing side to side, down into the grass when foraging
        head.yRot = Mth.sin(t * 0.35F) * 0.2F + Mth.clamp(state.yRot, -30.0F, 30.0F) * Mth.DEG_TO_RAD;
        head.xRot = Mth.sin(t * 0.9F) * 0.06F + state.grazeAmount * 0.5F;
        tail.xRot = 0.6F + Mth.sin(t * 0.2F) * 0.1F;
    }
}
