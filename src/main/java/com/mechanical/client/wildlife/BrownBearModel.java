package com.mechanical.client.wildlife;

import com.google.common.reflect.TypeToken;
import com.mechanical.client.CarcassRenderer;
import com.mechanical.wildlife.BrownBear;
import com.mechanical.wildlife.WildAnimal;

import net.minecraft.client.Minecraft;
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
 * Brown bear: massive forequarters under a two-step shoulder hump, a lower rump, a broad head carried below the hump
 * with fluffy cheek ruffs, a dished face (forehead stop, dipped bridge, nose pad raised at the tip), small round ears,
 * thick pillar legs with broad paws and long front claws, a stubby tail and a shaggy fringe hanging under the belly.
 * The body pivots at the hips so it can rear up on its hind legs.
 * Texture 128x64: textures/entity/wildlife/brown_bear.png (made by scripts/gen_brown_bear_texture.py).
 */
public class BrownBearModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "brown_bear"), "main");
    /** 0..1 how far a bear has reared up; put on the render state by {@link #registerRenderState}. */
    public static final ContextKey<Float> STAND = new ContextKey<>(Identifier.fromNamespaceAndPath("mechanical", "bear_stand"));

    private static final float HEAD_X_ROT = 0.12F;
    private static final float TAIL_X_ROT = 0.5F;
    /** Heavy, rolling walk: slower cadence than the vanilla 0.6662 quadruped. */
    private static final float GAIT = 0.55F;
    /** Body pivot (hips) and front leg pivot in model space, for moving the forelegs with the body when it rears. */
    private static final float HIP_Y = 12.0F, HIP_Z = 8.0F, SHOULDER_Y = 14.0F, SHOULDER_Z = -7.5F;
    private static final float REAR = 1.05F;

    private final ModelPart body;
    private final ModelPart tail;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart rightEar;
    private final ModelPart leftEar;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightHindLeg;
    private final ModelPart leftHindLeg;
    private final ModelPart[] legs;

    public BrownBearModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.tail = this.body.getChild("tail");
        this.head = this.body.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.rightEar = this.head.getChild("right_ear");
        this.leftEar = this.head.getChild("left_ear");
        this.rightFrontLeg = root.getChild("right_front_leg");
        this.leftFrontLeg = root.getChild("left_front_leg");
        this.rightHindLeg = root.getChild("right_hind_leg");
        this.leftHindLeg = root.getChild("left_hind_leg");
        this.legs = new ModelPart[] {rightFrontLeg, leftFrontLeg, rightHindLeg, leftHindLeg};
    }

    /** Call from the client mod bus {@link RegisterRenderStateModifiersEvent}: passes the rearing amount to the model. */
    public static void registerRenderState(RegisterRenderStateModifiersEvent event) {
        // registered for every wildlife renderer, so check the entity
        event.registerEntityModifier(new TypeToken<WildAnimalRenderer<WildAnimal>>() {}, (WildAnimal animal, WildAnimalRenderState state) -> {
            if (animal instanceof BrownBear bear) {
                state.setRenderData(STAND, bear.standAmount(Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false)));
            }
        });
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation plane = new CubeDeformation(0.001F);

        // Body pivot at the hips (0, 12, 8). Forequarters: x -6..6, y 3..16, z -12..0 in model space; hump on top of the
        // withers (y 0..3); hindquarters a bit lower and narrower (y 5..16, z -1..12). Shaggy belly fringe planes hang
        // 3 px below the chest on both sides.
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-6.0F, -9.0F, -20.0F, 12.0F, 13.0F, 12.0F)
                        .texOffs(48, 0).addBox(-4.5F, -11.0F, -18.0F, 9.0F, 3.0F, 7.0F)
                        .texOffs(48, 10).addBox(-3.0F, -12.0F, -16.5F, 6.0F, 1.0F, 4.0F)
                        .texOffs(0, 25).addBox(-5.5F, -7.0F, -9.0F, 11.0F, 11.0F, 13.0F)
                        .texOffs(48, 25).addBox(-6.2F, 3.0F, -19.0F, 0.0F, 4.0F, 10.0F, plane)
                        .texOffs(48, 25).mirror().addBox(6.2F, 3.0F, -19.0F, 0.0F, 4.0F, 10.0F, plane),
                PartPose.offset(0.0F, HIP_Y, HIP_Z));
        body.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(114, 20).addBox(-1.5F, -1.0F, 0.0F, 3.0F, 3.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -5.0F, 3.5F, TAIL_X_ROT, 0.0F, 0.0F));

        // Head in front of the shoulders, carried below the hump. Skull 8 wide, cheek ruffs 10 wide at the back,
        // forehead stop, muzzle 5 wide, nose pad sticking up half a pixel above the bridge (the "dish").
        PartDefinition head = body.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(80, 0).addBox(-4.0F, -4.0F, -6.0F, 8.0F, 7.0F, 6.0F)
                        .texOffs(48, 15).addBox(-5.0F, -2.0F, -2.5F, 10.0F, 6.0F, 3.0F)
                        .texOffs(108, 0).addBox(-3.0F, -3.0F, -7.0F, 6.0F, 2.0F, 1.0F)
                        .texOffs(80, 13).addBox(-2.5F, -1.0F, -10.0F, 5.0F, 4.0F, 4.0F)
                        .texOffs(114, 13).addBox(-1.5F, -1.5F, -11.0F, 3.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -5.0F, -20.0F, HEAD_X_ROT, 0.0F, 0.0F));
        head.addOrReplaceChild("jaw",
                CubeListBuilder.create().texOffs(98, 13).addBox(-2.0F, 0.0F, -3.5F, 4.0F, 2.0F, 4.0F),
                PartPose.offset(0.0F, 2.5F, -6.0F));
        // small round ears (corners transparent in the texture), 1 px sunk into the skull
        head.addOrReplaceChild("right_ear",
                CubeListBuilder.create().texOffs(114, 16).addBox(-1.5F, -2.5F, -0.5F, 3.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(-3.0F, -3.5F, -2.0F, 0.0F, 0.0F, -0.25F));
        head.addOrReplaceChild("left_ear",
                CubeListBuilder.create().mirror().texOffs(114, 16).addBox(-1.5F, -2.5F, -0.5F, 3.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(3.0F, -3.5F, -2.0F, 0.0F, 0.0F, 0.25F));

        // Pillar legs, ground at y = 24: front leg 5x8x5 over a broad paw with a row of long claws in front;
        // hind leg a thick 6x8x7 haunch over a long flat-footed paw.
        CubeListBuilder front = CubeListBuilder.create()
                .texOffs(0, 49).addBox(-2.5F, -1.0F, -2.5F, 5.0F, 8.0F, 5.0F)
                .texOffs(20, 49).addBox(-2.5F, 7.0F, -3.5F, 5.0F, 3.0F, 6.0F)
                .texOffs(68, 59).addBox(-2.0F, 9.0F, -4.5F, 4.0F, 1.0F, 1.0F);
        CubeListBuilder frontMirror = CubeListBuilder.create().mirror()
                .texOffs(0, 49).addBox(-2.5F, -1.0F, -2.5F, 5.0F, 8.0F, 5.0F)
                .texOffs(20, 49).addBox(-2.5F, 7.0F, -3.5F, 5.0F, 3.0F, 6.0F)
                .texOffs(68, 59).addBox(-2.0F, 9.0F, -4.5F, 4.0F, 1.0F, 1.0F);
        CubeListBuilder hind = CubeListBuilder.create()
                .texOffs(42, 49).addBox(-3.0F, -2.0F, -3.5F, 6.0F, 8.0F, 7.0F)
                .texOffs(68, 49).addBox(-2.5F, 6.0F, -3.5F, 5.0F, 4.0F, 6.0F);
        CubeListBuilder hindMirror = CubeListBuilder.create().mirror()
                .texOffs(42, 49).addBox(-3.0F, -2.0F, -3.5F, 6.0F, 8.0F, 7.0F)
                .texOffs(68, 49).addBox(-2.5F, 6.0F, -3.5F, 5.0F, 4.0F, 6.0F);
        root.addOrReplaceChild("right_front_leg", front, PartPose.offset(-3.5F, SHOULDER_Y, SHOULDER_Z));
        root.addOrReplaceChild("left_front_leg", frontMirror, PartPose.offset(3.5F, SHOULDER_Y, SHOULDER_Z));
        root.addOrReplaceChild("right_hind_leg", hind, PartPose.offset(-3.5F, 14.0F, 7.0F));
        root.addOrReplaceChild("left_hind_leg", hindMirror, PartPose.offset(3.5F, 14.0F, 7.0F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);

        if (CarcassRenderer.isHung(state)) {
            CarcassRenderer.hangLegs(new ModelPart[] {leftFrontLeg, rightFrontLeg}, new ModelPart[] {leftHindLeg, rightHindLeg});
            head.xRot = 0.25F; // in line with the body, muzzle down
            jaw.xRot = 0.25F;
            leftEar.xRot = rightEar.xRot = 1.0F;
            tail.xRot = 2.6F; // the stub sticks back (+z): folds down over the rump
            return;
        }

        if (CarcassRenderer.isCarcass(state)) {
            for (ModelPart leg : legs) leg.zRot = CarcassRenderer.LIMB_SAG;
            // the carcass renderer puts a head yaw in yRot that points the head at the ground
            this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
            this.head.xRot += 0.3F;
            this.jaw.xRot = 0.25F;
            this.tail.xRot = 1.0F;
            return;
        }

        float age = state.ageInTicks;
        if (state.dozing) {
            // lying on its belly: body (bottom y 16) dropped onto y = 24, slow breathing; forelegs stretched out in front
            // with the head resting on the paws, hind legs splayed out behind ("sploot"); pivots half a leg's thickness
            // above the ground so the legs lie on it
            this.body.y += 8.0F + Mth.sin(age * 0.06F) * 0.2F;
            // forelegs: folded back (claws down) then turned round to point forward, so the claws rest on the ground
            this.rightFrontLeg.y = 20.5F;
            this.leftFrontLeg.y = 20.5F;
            this.rightFrontLeg.xRot = Mth.HALF_PI;
            this.leftFrontLeg.xRot = Mth.HALF_PI;
            this.rightFrontLeg.yRot = Mth.PI - 0.12F;
            this.leftFrontLeg.yRot = Mth.PI + 0.12F;
            this.rightHindLeg.y = 20.5F;
            this.leftHindLeg.y = 20.5F;
            this.rightHindLeg.xRot = Mth.HALF_PI;
            this.leftHindLeg.xRot = Mth.HALF_PI;
            this.rightHindLeg.yRot = -0.2F;
            this.leftHindLeg.yRot = 0.2F;
            // chin on the forelegs (their top is at y 18), nose tipped down a little
            this.head.y -= 1.0F;
            this.head.xRot = 0.1F;
            this.head.yRot = 0.1F;
            this.rightEar.zRot -= 0.25F;
            this.leftEar.zRot += 0.25F;
            this.tail.xRot = 1.2F;
            return;
        }

        float pos = state.walkAnimationPos;
        float speed = Math.min(state.walkAnimationSpeed, 1.5F);
        float graze = state.grazeAmount;
        Float standData = state.getRenderData(STAND);
        float stand = standData == null ? 0.0F : standData;

        // Walk: lateral sequence (both legs of a side move nearly together), the bear's rolling amble; long swing.
        float swing = Mth.cos(pos * GAIT) * speed * 0.9F;
        float lag = Mth.cos(pos * GAIT + 0.6F) * speed * 0.9F;
        this.rightFrontLeg.xRot = swing;
        this.rightHindLeg.xRot = lag;
        this.leftFrontLeg.xRot = -swing;
        this.leftHindLeg.xRot = -lag;
        // shoulders roll side to side and the heavy head swings against them
        float roll = Mth.sin(pos * GAIT) * speed;
        this.body.zRot = roll * 0.05F;
        this.body.y += Mth.abs(Mth.cos(pos * GAIT)) * 0.5F * speed;
        this.head.zRot = -roll * 0.06F;
        this.head.xRot += Mth.sin(pos * GAIT * 2.0F) * 0.05F * speed;

        this.head.yRot = Mth.clamp(state.yRot, -45.0F, 45.0F) * Mth.DEG_TO_RAD * (1.0F - graze);
        this.head.xRot += Mth.clamp(state.xRot, -25.0F, 25.0F) * 0.6F * Mth.DEG_TO_RAD * (1.0F - graze);
        // idle: sniffing, the nose lifts and the jaw works; ears turn now and then
        float sniff = Mth.sin(age * 0.09F) > 0.6F ? Mth.sin(age * 0.9F) : 0.0F;
        this.head.xRot -= sniff * 0.04F * (1.0F - speed);
        float flick = Mth.sin(age * 0.07F) > 0.9F ? Mth.sin(age * 1.3F) * 0.2F : 0.0F;
        this.rightEar.zRot -= flick;
        this.leftEar.zRot += Mth.sin(age * 0.05F + 2.0F) > 0.92F ? 0.15F : 0.0F;

        // Foraging: nose down to the ground, digging and turning over the soil.
        if (graze > 0.0F) {
            this.head.xRot = Mth.lerp(graze, this.head.xRot, 0.85F + Mth.sin(age * 0.4F) * 0.06F);
            this.head.y += graze * 2.0F;
            this.rightFrontLeg.xRot = Mth.lerp(graze, this.rightFrontLeg.xRot, -0.35F + Mth.sin(age * 0.5F) * 0.3F);
        }

        // Angry: head low and forward, ears flat back, jaw open.
        if (state.charging) {
            this.head.xRot = Mth.lerp(0.6F, this.head.xRot, 0.35F);
            this.head.z -= 1.0F;
            this.rightEar.xRot = -0.6F;
            this.leftEar.xRot = -0.6F;
            this.jaw.xRot = 0.2F + Mth.sin(age * 0.3F) * 0.05F;
        }

        if (stand > 0.0F) rear(stand, age);

        this.tail.zRot = Mth.sin(age * 0.15F) * 0.1F + roll * 0.15F;
    }

    /** Up on the hind legs: body tilted back about the hips, forelegs carried along and raised to swipe, roaring. */
    private void rear(float stand, float age) {
        float tilt = -REAR * stand;
        this.body.xRot += tilt;
        this.body.zRot *= 1.0F - stand;
        // move each shoulder pivot with the body (rotation about x through the hips)
        float dy = SHOULDER_Y - HIP_Y, dz = SHOULDER_Z - HIP_Z;
        float cos = Mth.cos(tilt), sin = Mth.sin(tilt);
        float y = this.body.y + dy * cos - dz * sin;
        float z = this.body.z + dy * sin + dz * cos;
        // claws raised high and forward, the right paw a beat ahead, ready to come down
        float paw = Mth.sin(age * 0.5F) * 0.25F;
        for (ModelPart leg : new ModelPart[] {rightFrontLeg, leftFrontLeg}) {
            leg.y = Mth.lerp(stand, leg.y, y);
            leg.z = Mth.lerp(stand, leg.z, z);
        }
        this.rightFrontLeg.xRot = Mth.lerp(stand, this.rightFrontLeg.xRot, tilt - 1.0F + paw);
        this.leftFrontLeg.xRot = Mth.lerp(stand, this.leftFrontLeg.xRot, tilt - 0.7F - paw);
        this.rightFrontLeg.zRot = 0.25F * stand;
        this.leftFrontLeg.zRot = -0.25F * stand;
        // hind legs brace a little forward under the tilted body
        this.rightHindLeg.xRot = Mth.lerp(stand, this.rightHindLeg.xRot, -0.2F);
        this.leftHindLeg.xRot = Mth.lerp(stand, this.leftHindLeg.xRot, -0.2F);
        // head stays level-ish, looking down at the target, mouth wide open
        this.head.xRot -= tilt * 0.75F;
        this.head.z += stand * 1.0F;
        this.jaw.xRot = Mth.lerp(stand, this.jaw.xRot, 0.6F);
        this.tail.xRot = TAIL_X_ROT + 0.6F * stand;
    }
}
