package com.mechanical.client.folklore;

import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import com.mechanical.Mechanical;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * The folklore beings' models: parts and boxes from scripts/gen_folklore_models.py (FolkloreGeometry, with the skins it
 * paints), the poses and motion here, per {@link Kind}. Missing parts are simply not animated.
 */
public class BeingModel extends EntityModel<BeingModel.State> {
    public static class State extends LivingEntityRenderState {
        public boolean aggressive;
    }

    public enum Kind {
        HULDRA(FolkloreGeometry::huldra), TOMTE(FolkloreGeometry::tomte), NACKEN(FolkloreGeometry::nacken), BOG_BODY(FolkloreGeometry::bogBody),
        FROST_GIANT(FolkloreGeometry::frostGiant), DUNE_WRAITH(FolkloreGeometry::duneWraith), MASK(FolkloreGeometry::jungleSpiritMask),
        WISP(FolkloreGeometry::willOWisp);

        public final ModelLayerLocation layer = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mechanical.MODID, "folk_" + name().toLowerCase()), "main");
        public final java.util.function.Supplier<LayerDefinition> geometry;

        Kind(java.util.function.Supplier<LayerDefinition> geometry) {
            this.geometry = geometry;
        }
    }

    private final Kind kind;
    private final Function<String, @Nullable ModelPart> part;
    private final @Nullable ModelPart head, body, rightArm, leftArm, rightLeg, leftLeg;

    public BeingModel(ModelPart root, Kind kind) {
        super(root, kind == Kind.DUNE_WRAITH ? RenderTypes::entityTranslucent : kind == Kind.WISP ? RenderTypes::entityTranslucentEmissive
                : RenderTypes::entityCutout);
        this.kind = kind;
        part = root.createPartLookup();
        head = part.apply("head");
        body = part.apply("body");
        rightArm = part.apply("right_arm");
        leftArm = part.apply("left_arm");
        rightLeg = part.apply("right_leg");
        leftLeg = part.apply("left_leg");
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks, spd = Math.min(s.walkAnimationSpeed, 1.0F), swing = Mth.cos(s.walkAnimationPos * 0.6662F) * spd;
        if (head != null) {
            head.yRot = Mth.clamp(s.yRot, -50.0F, 50.0F) * Mth.DEG_TO_RAD;
            head.xRot = s.xRot * Mth.DEG_TO_RAD;
        }
        switch (kind) {
            case HULDRA -> {
                legs(0.5F * swing);
                arms(-0.5F * swing, 0.5F * swing, 0.06F + Mth.sin(t * 0.05F) * 0.02F);
                rightArm.xRot -= 0.45F; // the comb held a little out
                head.zRot = Mth.sin(t * 0.03F) * 0.07F;
                ModelPart tail = part.apply("tail");
                tail.zRot = Mth.sin(t * 0.07F) * 0.3F;
                tail.xRot += Mth.sin(t * 0.05F) * 0.08F;
            }
            case TOMTE -> {
                legs(1.1F * swing);
                arms(-0.9F * swing, 0.9F * swing, 0.12F);
                for (int i = 1; i <= 4; i++) part.apply("cap" + i).xRot += Mth.sin(t * 0.1F + i) * (0.04F + spd * 0.1F);
            }
            case NACKEN -> {
                legs(0.6F * swing);
                // playing: the fiddle under his chin, the left hand on its neck, the right sawing the bow across it
                leftArm.xRot = -1.4F;
                leftArm.yRot = -0.1F;
                rightArm.xRot = -1.2F;
                rightArm.yRot = -0.3F + Mth.sin(t * 0.2F) * 0.25F;
                head.zRot = 0.15F;
                head.xRot = 0.2F + Mth.sin(t * 0.04F) * 0.05F;
                head.yRot *= 0.3F;
            }
            case BOG_BODY -> {
                float lurch = Mth.cos(s.walkAnimationPos * 0.45F) * spd;
                legs(0.8F * lurch);
                body.xRot = 0.4F + Mth.sin(t * 0.05F) * 0.02F;
                body.zRot = Mth.sin(s.walkAnimationPos * 0.225F) * 0.12F * spd;
                head.xRot -= 0.35F;
                float reach = -1.0F + Mth.sin(t * 0.1F) * 0.08F;
                if (s.aggressive) arms(reach, reach, 0.08F);
                else arms(-0.4F - 0.3F * lurch, -0.4F + 0.3F * lurch, 0.1F);
                part.apply("jaw").xRot += Mth.sin(t * 0.07F) * 0.06F;
            }
            case FROST_GIANT -> {
                float step = Mth.cos(s.walkAnimationPos * 0.4F) * spd;
                legs(0.55F * step);
                body.xRot = 0.22F;
                body.y += Mth.sin(t * 0.06F) * 0.3F;
                head.xRot = head.xRot * 0.5F - 0.2F;
                float raise = -0.9F + Mth.sin(t * 0.08F) * 0.1F;
                if (s.aggressive) arms(raise, raise, 0.25F);
                else arms(-0.2F - 0.3F * step, -0.2F + 0.3F * step, 0.14F);
                part.apply("beard").xRot += Mth.sin(t * 0.05F) * 0.04F;
            }
            case DUNE_WRAITH -> {
                part.apply("ring0").yRot = t * 0.15F;
                part.apply("ring1").yRot = -t * 0.27F;
                part.apply("ring2").yRot = t * 0.34F;
                body.y += Mth.sin(t * 0.08F) * 0.6F;
                body.yRot = Mth.sin(t * 0.04F) * 0.12F;
                arms(-0.7F, -0.7F, 0.15F);
                rightArm.xRot += Mth.sin(t * 0.09F) * 0.15F;
                leftArm.xRot += Mth.sin(t * 0.09F + 2.0F) * 0.15F;
                for (int i = 0; i < 4; i++) part.apply("tatter" + i).xRot += Mth.sin(t * 0.3F + i * 1.7F) * 0.15F;
            }
            case MASK -> {
                ModelPart mask = part.apply("mask");
                mask.yRot = Mth.sin(t * 0.03F) * 0.25F;
                mask.zRot = Mth.sin(t * 0.05F) * 0.08F;
                mask.y += Mth.sin(t * 0.08F) * 1.2F;
                for (int i = 0; i < 7; i++) part.apply("feather" + i).zRot += Mth.sin(t * 0.12F + i) * 0.03F;
                part.apply("tassel_r").zRot = Mth.sin(t * 0.07F) * 0.15F - mask.zRot;
                part.apply("tassel_l").zRot = Mth.sin(t * 0.07F + 1.0F) * 0.15F - mask.zRot;
            }
            case WISP -> {
                part.apply("orb").y += Mth.sin(t * 0.1F) * 1.0F;
                ModelPart halo = part.apply("halo");
                float pulse = 1.0F + 0.12F * Mth.sin(t * 0.23F) + 0.05F * Mth.sin(t * 0.91F);
                halo.xScale = halo.yScale = halo.zScale = pulse;
            }
        }
    }

    private void legs(float swing) {
        rightLeg.xRot = swing;
        leftLeg.xRot = -swing;
    }

    /** Arm pitch (right, left), the arms held {@code out} from the sides. */
    private void arms(float right, float left, float out) {
        rightArm.xRot = right;
        leftArm.xRot = left;
        rightArm.zRot = out;
        leftArm.zRot = -out;
    }
}
