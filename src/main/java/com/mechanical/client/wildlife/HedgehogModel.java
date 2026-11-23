package com.mechanical.client.wildlife;

import com.google.common.reflect.TypeToken;
import com.mechanical.client.CarcassRenderer;
import com.mechanical.wildlife.Hedgehog;
import com.mechanical.wildlife.WildAnimal;

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
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

/**
 * Hedgehog: a low spiny mantle with a domed back, ragged spine planes sticking out past it (flanks, crest, rump and a
 * crown over the brow), a furry face with a long pointed snout and shiny nose, and four stubby legs. Shuffles along
 * sniffing; curled, it pulls head and legs in and becomes a round ball of spines that breathes slowly.
 * Texture: textures/entity/wildlife/hedgehog.png, painted by scripts/gen_small_models.py (with the geometry, SmallGeometry).
 */
public class HedgehogModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "hedgehog"), "main");
    public static final ContextKey<Boolean> CURLED = new ContextKey<>(Identifier.fromNamespaceAndPath("mechanical", "hedgehog_curled"));

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart[] legs;

    public HedgehogModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        this.legs = new ModelPart[] {root.getChild("right_front_leg"), root.getChild("left_front_leg"),
                root.getChild("right_hind_leg"), root.getChild("left_hind_leg")};
    }

    public static void registerRenderState(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(new TypeToken<WildAnimalRenderer<WildAnimal>>() {}, (WildAnimal animal, WildAnimalRenderState state) -> {
            if (animal instanceof Hedgehog hedgehog && hedgehog.isCurled()) state.setRenderData(CURLED, true);
        });
    }

    public static LayerDefinition createBodyLayer() {
        return SmallGeometry.hedgehog(); // scripts/gen_small_models.py
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        for (ModelPart leg : legs) leg.visible = true; // resetPose doesn't restore visibility

        if (CarcassRenderer.isHung(state)) {
            CarcassRenderer.hangLegs(new ModelPart[] {legs[0], legs[1]}, new ModelPart[] {legs[2], legs[3]});
            return;
        }

        if (CarcassRenderer.isCarcass(state)) {
            for (ModelPart leg : legs) leg.zRot = CarcassRenderer.LIMB_SAG;
            head.yRot = state.yRot * Mth.DEG_TO_RAD;
            return;
        }

        if (Boolean.TRUE.equals(state.getRenderData(CURLED))) {
            // rolled up: head tucked in under the brow spines, legs drawn in, the mantle drawn into a rounder, taller
            // ball sitting on the ground, breathing slowly
            float breathe = Mth.sin(t * 0.1F) * 0.03F;
            body.y += 1.6F;
            body.xRot = 0.25F;
            body.zScale = 0.85F;
            body.yScale = 1.12F + breathe;
            body.xScale = 1.05F + breathe;
            head.z += 3.4F;
            head.y -= 0.6F;
            head.xRot = 1.6F;
            for (ModelPart leg : legs) leg.visible = false;
            return;
        }

        float speed = Math.min(state.walkAnimationSpeed * 1.5F, 1.0F);
        float phase = state.walkAnimationPos * 1.6F; // quick little steps
        legs[0].xRot = legs[3].xRot = Mth.cos(phase) * 0.9F * speed;
        legs[1].xRot = legs[2].xRot = -Mth.cos(phase) * 0.9F * speed;
        body.y -= Math.abs(Mth.sin(phase)) * 0.3F * speed;
        body.zRot = Mth.sin(phase) * 0.04F * speed; // waddle

        // nose always going: sniffing bobs, snuffling with the snout to the ground when grazing (foraging)
        head.yRot = Mth.clamp(state.yRot, -40.0F, 40.0F) * Mth.DEG_TO_RAD;
        head.xRot = state.xRot * Mth.DEG_TO_RAD * 0.5F + Mth.sin(t * 0.9F) * 0.05F;
        float graze = state.grazeAmount;
        head.xRot += graze * (0.55F + Mth.sin(t * 1.3F) * 0.08F);
        head.y += graze * 0.6F;
        head.yRot += graze * Mth.sin(t * 0.25F) * 0.4F;

    }
}
