package com.mechanical.client;

import java.util.HashMap;
import java.util.Map;

import com.mechanical.CarcassEntity;
import com.mechanical.Mechanical;
import com.mechanical.TravoisEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.PhantomRenderState;
import net.minecraft.client.renderer.entity.state.BatRenderState;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.ambient.BatModel;
import net.minecraft.client.model.animal.chicken.ChickenModel;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the carcass as the animal itself lying on its side: a never-added copy of the animal is rendered with its own
 * vanilla renderer inside a rotated pose. Carried ({@link CarcassEntity#hauler()}, not large) it lies across the
 * hauler's shoulders, drawn from the hauler's own position so it never lags; hung on a Hanging Rack it hangs head down by
 * the hind legs from the top of its hitbox. Dragged or on a travois it lies as on the ground, on a travois or cart tilted
 * with the bed. Hung long enough to have aged ({@link CarcassEntity#isAged()}) it looks dried: the hide darker and duller
 * ({@link #AGED_TINT}), the bare flesh of a skinned one from textures/entity/skinned_aged/ (scripts/gen_aged_textures.py).
 * A caravan's trade goods ({@link CarcassEntity#isPrepared()}) are wrapped round the middle in a salted hide tied with
 * cords and lashed under a carrying pole ({@link Lashing}, textures/entity/carcass_lashing.png from
 * scripts/gen_carcass_lashing_textures.py), and shrunk to fit under a caravan cart's canopy.
 * shortcut: the copy has default looks (white sheep, default cow variant); sync the dead animal's data if that matters.
 */
public class CarcassRenderer extends EntityRenderer<CarcassEntity, CarcassRenderer.State> {
    /** Marks the animal's render state as a carcass so the model mixins let the limbs sag. */
    private static final ContextKey<Boolean> CARCASS = new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "carcass"));
    /** Marks the animal's render state as hung head down on a Hanging Rack, so the models let the limbs hang (see {@link #hangLegs}). */
    private static final ContextKey<Boolean> HUNG = new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "hung"));
    /** Dragged: bit i set when a player holds limb i (CarcassEntity#gripPoint): those legs are pulled out ahead ({@link #gripLegs}). */
    private static final ContextKey<Integer> GRIP = new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "grip"));
    /** Limb order of the grip slots, by the part names vanilla and the mod's animal models share. */
    private static final String[] GRIP_LEGS = {"right_front_leg", "left_front_leg", "right_hind_leg", "left_hind_leg"};
    /** A held leg: pitched forward towards the head (towards the puller), less sag. */
    private static final float GRIP_PITCH = -1.3F, GRIP_SAG = -0.25F;
    /** Limb roll (model z rotation, radians) towards the ground for an animal lying on its side. */
    public static final float LIMB_SAG = -0.7F;
    /** Head yaw (degrees) that turns the head down to the ground. */
    private static final float HEAD_SAG = -50.0F;
    /** PhantomModel flap time where the wings droop a little (cos ~0.35 of the 16 degree flap). */
    private static final float LIMP_FLAP = 9.3F;

    /** Skinned/plucked carcass: rendered with the flesh texture (see LivingEntityRendererMixin). */
    private static final ContextKey<Boolean> SKINNED = new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "skinned"));
    private static final Map<Identifier, Identifier> SKINNED_TEXTURES = new HashMap<>();
    /** Aged on the rack: a dried look (see LivingEntityRendererMixin). */
    private static final ContextKey<Boolean> AGED = new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "aged"));
    private static final Map<Identifier, Identifier> AGED_TEXTURES = new HashMap<>();
    /** Model tint of an aged carcass that has no dried texture: hide and coat gone darker, browner and duller. */
    public static final int AGED_TINT = 0xFFA08C78;
    private static final ContextKey<Boolean> TURNING = new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "turning"));
    /** Model tint of a body close to spoiling: a sickly green-grey cast. */
    public static final int TURNING_TINT = 0xFF9AA67A;
    /** Two-legged bodies (the folk of the dark): lie on their back, limbs spread on the ground, and hang by the feet. */
    public static final java.util.Set<net.minecraft.world.entity.EntityType<?>> ON_BACK = new java.util.HashSet<>();

    /** Room under a caravan cart's canopy for a prepared body lying across the bed (its legs) and along it (its length). */
    private static final float FIT_ACROSS = 1.2F, FIT_ALONG = 1.75F, POLE_LENGTH = 1.7F;
    private static final Identifier LASHING_TEXTURE = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/entity/carcass_lashing.png");

    /**
     * A prepared body's wrapping (part 0: a 1-block cube, the salted hide with its cords) and carrying pole (part 1: 2 x 2 px,
     * a block long); the renderer scales each to the body. Texture 64 x 64: scripts/gen_carcass_lashing_textures.py.
     */
    public static class Lashing extends Model<Integer> {
        public static final net.minecraft.client.model.geom.ModelLayerLocation LAYER = new net.minecraft.client.model.geom.ModelLayerLocation(
                Identifier.fromNamespaceAndPath(Mechanical.MODID, "carcass"), "lashing");
        private final ModelPart wrap, pole;

        public Lashing(ModelPart root) {
            super(root, net.minecraft.client.renderer.rendertype.RenderTypes::entityCutout);
            wrap = root.getChild("wrap");
            pole = root.getChild("pole");
        }

        @Override
        public void setupAnim(Integer part) {
            super.setupAnim(part);
            wrap.visible = part == 0;
            pole.visible = part == 1;
        }

        public static net.minecraft.client.model.geom.builders.LayerDefinition create() {
            var mesh = new net.minecraft.client.model.geom.builders.MeshDefinition();
            var root = mesh.getRoot();
            root.addOrReplaceChild("wrap", net.minecraft.client.model.geom.builders.CubeListBuilder.create().texOffs(0, 0).addBox(-8, -8, -8, 16, 16, 16),
                    net.minecraft.client.model.geom.PartPose.ZERO);
            root.addOrReplaceChild("pole", net.minecraft.client.model.geom.builders.CubeListBuilder.create().texOffs(0, 32).addBox(-1, -1, -8, 2, 2, 16),
                    net.minecraft.client.model.geom.PartPose.ZERO);
            return net.minecraft.client.model.geom.builders.LayerDefinition.create(mesh, 64, 64);
        }
    }

    public static boolean isCarcass(EntityRenderState state) {
        return Boolean.TRUE.equals(state.getRenderData(CARCASS));
    }

    public static boolean isHung(EntityRenderState state) {
        return Boolean.TRUE.equals(state.getRenderData(HUNG));
    }

    /**
     * Hung head down (the model's -z is world down, +z up): hind legs pitched back along the body, up to the gambrel and a
     * little spread; front legs pitched forward past the head, dangling to the ground. A leg box hangs along +y, and a
     * positive xRot swings it to +z (LyingPose folds the forearms forward with a negative one).
     */
    public static void hangLegs(ModelPart[] front, ModelPart[] hind) {
        for (ModelPart leg : hind) hangLeg(leg, 1.5F, 0.12F);
        for (ModelPart leg : front) hangLeg(leg, -1.45F, -0.08F);
    }

    /** yRot after the pitch swings the leg sideways: positive spread moves a +z leg to +x, so sign it by the leg's side. */
    private static void hangLeg(ModelPart leg, float pitch, float spread) {
        leg.xRot = pitch;
        leg.yRot = Math.signum(leg.x) * spread;
        leg.zRot = 0.0F;
    }

    private static final Map<Model<?>, java.util.function.Function<String, ModelPart>> PARTS = new java.util.WeakHashMap<>();

    /**
     * Vanilla models without a pose mixin of their own (fox, wolf, cat, llama, camel, frog ...): legs found by their part
     * names sag like a quadruped's, or hang on the rack; a hung bat folds its wings as it does at roost. Called right after
     * setupAnim (ModelFeatureRendererMixin), so it also poses layers drawn with the same model shape.
     */
    public static void poseVanillaCarcass(Model<?> model, Object state) {
        if (!(state instanceof LivingEntityRenderState s) || !isCarcass(s) && !isHung(s)) return;
        if (s.getRenderData(GRIP) != null) gripLegs(model, s.getRenderData(GRIP));
        // their own mixins / the mod's models pose themselves
        if (model instanceof QuadrupedModel || model instanceof ChickenModel || !model.getClass().getName().startsWith("net.minecraft.")) return;
        var part = PARTS.computeIfAbsent(model, m -> m.root().createPartLookup());
        if (model instanceof BatModel) {
            if (!isHung(s)) return; // lying it keeps its wings spread on the ground
            foldWing(part.apply("right_wing"), part.apply("right_wing_tip"), -1);
            foldWing(part.apply("left_wing"), part.apply("left_wing_tip"), 1);
            return;
        }
        ModelPart[] hind = parts(part, "right_hind_leg", "left_hind_leg", "right_leg", "left_leg");
        ModelPart[] front = parts(part, "right_front_leg", "left_front_leg", "right_arm", "left_arm");
        if (isHung(s)) hangLegs(front, hind);
        if (isCarcass(s)) {
            for (ModelPart leg : hind) leg.zRot = LIMB_SAG;
            for (ModelPart leg : front) leg.zRot = LIMB_SAG;
        }
    }

    /** Every model (after its own carcass pose): the legs the pullers hold stretch out ahead, towards them. */
    private static void gripLegs(Model<?> model, int held) {
        var part = PARTS.computeIfAbsent(model, m -> m.root().createPartLookup());
        for (int i = 0; i < GRIP_LEGS.length; i++) {
            ModelPart leg = (held & 1 << i) != 0 ? part.apply(GRIP_LEGS[i]) : null;
            if (leg == null) continue;
            leg.xRot = GRIP_PITCH;
            leg.zRot = GRIP_SAG;
        }
    }

    /** As BatAnimation.BAT_RESTING: the wing close to the body, the tip folded right back. */
    private static void foldWing(ModelPart wing, ModelPart tip, int side) {
        if (wing != null) wing.yRot = side * 10.0F * Mth.DEG_TO_RAD;
        if (tip != null) tip.yRot = side * 120.0F * Mth.DEG_TO_RAD;
    }

    private static ModelPart[] parts(java.util.function.Function<String, ModelPart> lookup, String... names) {
        return java.util.Arrays.stream(names).map(lookup).filter(java.util.Objects::nonNull).toArray(ModelPart[]::new);
    }

    public static boolean isSkinned(EntityRenderState state) {
        return Boolean.TRUE.equals(state.getRenderData(SKINNED));
    }

    public static boolean isAged(EntityRenderState state) {
        return Boolean.TRUE.equals(state.getRenderData(AGED));
    }

    public static boolean isTurning(EntityRenderState state) {
        return Boolean.TRUE.equals(state.getRenderData(TURNING));
    }

    /** mechanical:textures/entity/skinned/cow/x.png -> mechanical:textures/entity/skinned_aged/cow/x.png, if that texture exists. */
    public static Identifier agedTexture(Identifier skinned) {
        return AGED_TEXTURES.computeIfAbsent(skinned, t -> {
            Identifier aged = Identifier.fromNamespaceAndPath(Mechanical.MODID, t.getPath().replace("textures/entity/skinned/", "textures/entity/skinned_aged/"));
            return t.getNamespace().equals(Mechanical.MODID) && Minecraft.getInstance().getResourceManager().getResource(aged).isPresent() ? aged : t;
        });
    }

    /** The texture a carcass's model draws with in place of the animal's own {@code texture}. */
    public static Identifier carcassTexture(LivingEntityRenderState state, Identifier texture) {
        if (!isSkinned(state)) return texture;
        Identifier skinned = skinnedTexture(texture);
        return isAged(state) ? agedTexture(skinned) : skinned;
    }

    /** Extra model tint: aged without a dried texture of its own (the hide is still on, or no flesh texture for it). */
    public static int carcassTint(LivingEntityRenderState state, Identifier texture) {
        int tint = -1;
        if (isAged(state) && !carcassTexture(state, texture).getPath().startsWith("textures/entity/skinned_aged/")) tint = AGED_TINT;
        if (isTurning(state)) tint = net.minecraft.util.ARGB.multiply(tint, TURNING_TINT);
        return tint;
    }

    /** minecraft:textures/entity/cow/x.png -> mechanical:textures/entity/skinned/cow/x.png, if that texture exists. */
    public static Identifier skinnedTexture(Identifier texture) {
        return SKINNED_TEXTURES.computeIfAbsent(texture, t -> {
            Identifier skinned = Identifier.fromNamespaceAndPath(Mechanical.MODID, t.getPath().replace("textures/entity/", "textures/entity/skinned/"));
            return Minecraft.getInstance().getResourceManager().getResource(skinned).isPresent() ? skinned : t;
        });
    }

    private static boolean hasSkinnedTexture(EntityRenderer<Entity, EntityRenderState> renderer, EntityRenderState state) {
        if (!(renderer instanceof LivingEntityRenderer<?, ?, ?> living) || !(state instanceof LivingEntityRenderState ls)) return false;
        Identifier texture = ((LivingEntityRenderer) living).getTextureLocation(ls);
        return !skinnedTexture(texture).equals(texture);
    }

    /**
     * SLUNG: an edible bird carried by its feet over the shoulder, hanging down the back (drawn like HUNG). HORSE: hanging
     * head down on a horse's flank (CarcassEntity#slungHorse), drawn like HUNG from the horse's own position, a little smaller.
     */
    public enum Pose { LYING, CARRIED, HUNG, SLUNG, HORSE }
    /** On a horse: the rump this high above the hanging point (CarcassEntity#SIDE_UP: about the horse's back), and the scale. */
    private static final float HORSE_HOOK = 0.6F, HORSE_SCALE = 0.75F;

    public static class State extends EntityRenderState {
        EntityRenderState animal;
        EntityRenderer<Entity, EntityRenderState> renderer;
        float bodyYaw, width, height, hangLength;
        /** On a travois or cart: its bed's tilt (degrees, front up). */
        float tilt;
        boolean flat;
        /** An upright model (the bat): lies on its belly, wings spread; hangs head down from its feet. */
        boolean upright;
        /** Two-legged ({@link #ON_BACK}): standing tall along +y, its face to -z. */
        boolean onBack;
        Pose pose = Pose.LYING;
        /** Carried: from the carcass to the hauler's feet, and the shoulder height above them. */
        double dx, dy, dz;
        float shoulder;
        /** A caravan's trade goods, and the scale that fits it under the canopy. */
        boolean prepared;
        float fit = 1.0F;
    }

    /** Carried by the camera's player in first person: it would fill the screen. */
    private static boolean carriedByViewer(CarcassEntity carcass) {
        Minecraft mc = Minecraft.getInstance();
        return !carcass.isLarge() && carcass.haulerId() >= 0 && mc.getCameraEntity() != null && carcass.haulerId() == mc.getCameraEntity().getId()
                && mc.options.getCameraType().isFirstPerson();
    }

    @Override
    public boolean shouldRender(CarcassEntity carcass, net.minecraft.client.renderer.culling.Frustum frustum, double camX, double camY, double camZ, float partialTicks) {
        if (carriedByViewer(carcass)) return false;
        return carcass.haulerId() >= 0 || super.shouldRender(carcass, frustum, camX, camY, camZ, partialTicks);
    }

    private final Lashing lashing;

    public CarcassRenderer(EntityRendererProvider.Context context) {
        super(context);
        lashing = new Lashing(context.bakeLayer(Lashing.LAYER));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void extractRenderState(CarcassEntity carcass, State state, float partialTicks) {
        super.extractRenderState(carcass, state, partialTicks);
        state.animal = null;
        if (carcass.tickCount < CarcassEntity.HIDDEN_TICKS) return; // the dying animal is still playing its death animation
        if (carcass.renderDummy == null) {
            carcass.renderDummy = carcass.animal().create(carcass.level(), EntitySpawnReason.LOAD);
            // never added to the world, but vanilla renderers read the id; negative ids can't clash with real entities
            if (carcass.renderDummy != null) {
                carcass.renderDummy.setId(-1 - carcass.getId());
                // same coat / colour / variant as the animal had
                carcass.renderDummy.load(TagValueInput.create(ProblemReporter.DISCARDING, carcass.registryAccess(), carcass.looks()));
            }
        }
        Entity dummy = carcass.renderDummy;
        if (dummy == null) return;
        dummy.setPos(carcass.getX(), carcass.getY(), carcass.getZ());
        dummy.xo = dummy.getX();
        dummy.yo = dummy.getY();
        dummy.zo = dummy.getZ();
        if (dummy instanceof LivingEntity living) {
            // body faces "south" inside our own rotation (vanilla rotates by 180 - bodyRot)
            living.yBodyRot = living.yBodyRotO = living.yHeadRot = living.yHeadRotO = 180.0F;
            living.setYRot(180.0F);
        }
        Entity hauler = carcass.isLarge() ? null : carcass.hauler();
        LivingEntity horse = carcass.slungHorse();
        state.pose = horse != null ? Pose.HORSE : carcass.hungAt() != null ? Pose.HUNG : hauler != null ? (com.mechanical.Birds.isBird(carcass.animal()) || carcass.animal() == net.minecraft.world.entity.EntityTypes.BAT ? Pose.SLUNG : Pose.CARRIED) : Pose.LYING;
        boolean skinned = carcass.stage() > 0;
        if (dummy instanceof Sheep sheep) sheep.setSheared(skinned);
        state.renderer = (EntityRenderer<Entity, EntityRenderState>) Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(dummy);
        state.animal = state.renderer.createRenderState(dummy, partialTicks);
        // skinned/plucked: raw red flesh look (the vanilla hurt tint); a sheared sheep already looks bare
        // carried the legs hang stiff from the body; hung they hang from the hook (HUNG) instead of sagging sideways
        if (state.pose == Pose.LYING) state.animal.setRenderData(CARCASS, true);
        if (state.pose == Pose.LYING && carcass.isLarge() && carcass.haulerId() >= 0) {
            int held = 0;
            for (int i = 0; i < CarcassEntity.MAX_HAULERS; i++) if (carcass.haulerId(i) >= 0) held |= 1 << i;
            state.animal.setRenderData(GRIP, held);
        }
        if (state.pose == Pose.HUNG || state.pose == Pose.SLUNG || state.pose == Pose.HORSE) state.animal.setRenderData(HUNG, true);
        if (state.animal instanceof LivingEntityRenderState living) {
            // models use yRot as head yaw; lying on the side that points the head at the ground
            living.yRot = state.pose == Pose.LYING ? HEAD_SAG : 0.0F;
            living.xRot = 0.0F;
            if (skinned && !(dummy instanceof Sheep)) {
                // flesh texture if we have one for this animal, otherwise the red hurt tint
                if (hasSkinnedTexture(state.renderer, living)) living.setRenderData(SKINNED, true);
                else living.hasRedOverlay = true;
            }
            if (carcass.isAged()) living.setRenderData(AGED, true);
            if (carcass.isTurning()) living.setRenderData(TURNING, true);
        }
        // a dead phantom: wings spread limp on the ground instead of flapping
        if (state.animal instanceof PhantomRenderState phantom) phantom.flapTime = LIMP_FLAP;
        state.flat = carcass.liesFlat();
        state.upright = state.animal instanceof BatRenderState;
        state.onBack = ON_BACK.contains(carcass.animal());
        state.bodyYaw = carcass.getViewYRot(partialTicks);
        state.width = carcass.animal().getWidth();
        state.height = carcass.animal().getHeight();
        // slung: as long as it would hang on the rack (its own hitbox is the lying one)
        state.hangLength = state.pose == Pose.SLUNG || state.pose == Pose.HORSE ? Math.max(state.width, state.height) : carcass.getBbHeight();
        state.tilt = carcass.getVehicle() instanceof TravoisEntity bed ? bed.pitch(partialTicks) : 0.0F;
        state.prepared = carcass.isPrepared() && state.pose == Pose.LYING && !state.flat && !state.upright;
        // the legs lie across the bed, the body along it (a hitbox is about two thirds of the body's length)
        state.fit = state.prepared ? Math.min(1.0F, Math.min(FIT_ACROSS / state.height, FIT_ALONG / (state.width * 1.4F))) : 1.0F;
        if (state.pose == Pose.CARRIED || state.pose == Pose.SLUNG) {
            Vec3 at = hauler.getPosition(partialTicks).subtract(carcass.getPosition(partialTicks));
            state.dx = at.x;
            state.dy = at.y;
            state.dz = at.z;
            state.shoulder = hauler.isCrouching() ? 1.15F : 1.45F;
            if (hauler instanceof LivingEntity living) state.bodyYaw = Mth.rotLerp(partialTicks, living.yBodyRotO, living.yBodyRot);
        }
        if (state.pose == Pose.HORSE) {
            // from the horse's position at the frame, so it never lags behind a galloping horse; belly out from the flank
            float yaw = Mth.rotLerp(partialTicks, horse.yBodyRotO, horse.yBodyRot), out = carcass.slungSide() == 0 ? -90.0F : 90.0F;
            Vec3 at = horse.getPosition(partialTicks).add(Vec3.directionFromRotation(0, yaw + out).scale(CarcassEntity.SIDE_OUT))
                    .add(0, CarcassEntity.SIDE_UP, 0).subtract(carcass.getPosition(partialTicks));
            state.dx = at.x;
            state.dy = at.y;
            state.dz = at.z;
            state.bodyYaw = yaw - out;
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.animal == null) return;
        poseStack.pushPose();
        if (state.pose == Pose.CARRIED || state.pose == Pose.SLUNG) poseStack.translate(state.dx, state.dy + state.shoulder, state.dz);
        if (state.pose == Pose.HORSE) {
            poseStack.translate(state.dx, state.dy + HORSE_HOOK - state.hangLength * HORSE_SCALE, state.dz);
            poseStack.scale(HORSE_SCALE, HORSE_SCALE, HORSE_SCALE);
        }
        poseStack.rotate(Axis.YP.rotationDegrees(180.0F - state.bodyYaw));
        // in this frame the animal's head points to -z, its feet to -y (the copy is rendered facing yaw 180)
        if (state.pose == Pose.CARRIED) {
            // across the shoulders: body along x, back to the neck, legs hanging forward over the chest
            poseStack.translate(0.0F, 0.0F, 0.1F + state.width * 0.25F);
            poseStack.rotate(Axis.XP.rotationDegrees(90.0F));
            poseStack.rotate(Axis.YP.rotationDegrees(90.0F));
            poseStack.translate(0.0F, -state.height * 0.6F, 0.0F);
            state.renderer.submit(state.animal, poseStack, collector, camera);
            poseStack.popPose();
            return;
        }
        if (state.pose == Pose.SLUNG) {
            // the feet in the raised right hand above the shoulder (where the hook would be), the body down the back
            // (+x is the hauler's right, +z behind him here; HaulingClient raises the arm)
            poseStack.translate(0.3F, 0.55F - state.hangLength, 0.3F);
        }
        if ((state.pose == Pose.HUNG || state.pose == Pose.SLUNG || state.pose == Pose.HORSE) && (state.upright || state.onBack)) {
            // a bat hangs by its feet as at roost: just upside down, the feet at the hook
            poseStack.translate(0.0F, state.hangLength, 0.0F);
            poseStack.rotate(Axis.ZP.rotationDegrees(180.0F));
            state.renderer.submit(state.animal, poseStack, collector, camera);
            poseStack.popPose();
            return;
        }
        if (state.pose == Pose.HUNG || state.pose == Pose.SLUNG || state.pose == Pose.HORSE) {
            // head down from the hook: the rump (z = +rear) goes up to the top of the hitbox, the body centred under it.
            // The hind legs point up past the rump (hangLegs), so drop the body until the hook is at about the hocks,
            // less when a tall animal leaves little room under the hitbox (a moose's head already nears the ground)
            float drop = state.pose != Pose.HUNG ? 0.0F : Math.min(state.height * 0.15F, (com.mechanical.HangingRackBlock.HOOK - state.hangLength) * 0.3F);
            float rear = state.width * 0.6F + Math.max(0.0F, drop);
            poseStack.translate(0.0F, state.hangLength - rear, state.height * 0.55F);
            poseStack.rotate(Axis.XP.rotationDegrees(-90.0F));
            state.renderer.submit(state.animal, poseStack, collector, camera);
            poseStack.popPose();
            return;
        }
        // lying on a travois or cart: along its tilted bed (the body faces the way it does)
        poseStack.rotate(Axis.XP.rotationDegrees(state.tilt));
        if (state.upright) {
            // on its belly, head forward, wings flat on the ground (the model stands 10 px tall, its face to -z)
            poseStack.translate(0.0F, 0.07F, 0.31F);
            poseStack.rotate(Axis.XP.rotationDegrees(-90.0F));
            state.renderer.submit(state.animal, poseStack, collector, camera);
            poseStack.popPose();
            return;
        }
        if (state.onBack) {
            // on its back, face up, head along +z: the body centred on the hitbox, resting on its back
            poseStack.translate(0.0F, state.width * 0.2F, -state.height / 2.0F);
            poseStack.rotate(Axis.XP.rotationDegrees(90.0F));
            state.renderer.submit(state.animal, poseStack, collector, camera);
            poseStack.popPose();
            return;
        }
        if (state.flat) {
            state.renderer.submit(state.animal, poseStack, collector, camera);
            poseStack.popPose();
            return;
        }
        // lift so the flank rests on the ground, and shift so the body (not the feet) is centred on the hitbox
        // the hitbox width overstates the body thickness for long animals (horse), so estimate the flank height and cap it
        float lift = Math.clamp(state.width * 0.42F, 0.1F, 0.38F);
        if (state.prepared) {
            // the hide round the torso (the body is centred here), the pole on top along the bed, then the body shrunk to fit
            float s = state.fit, thick = 2 * lift * s;
            submitLashing(state, poseStack, collector, 0, s * 0.45F * Math.min(state.height, state.width * 1.4F), thick + 0.08F, s * state.width * 0.6F, thick / 2);
            submitLashing(state, poseStack, collector, 1, 1.0F, 1.0F, POLE_LENGTH, thick + 0.04F + 0.0625F);
            poseStack.scale(s, s, s);
        }
        poseStack.translate(state.height / 2.0F, lift, 0.0F);
        poseStack.rotate(Axis.ZP.rotationDegrees(90.0F));
        state.renderer.submit(state.animal, poseStack, collector, camera);
        poseStack.popPose();
    }

    /** One {@link Lashing} part scaled to x by y by z blocks, centred at height {@code y0}. */
    private void submitLashing(State state, PoseStack poseStack, SubmitNodeCollector collector, int part, float x, float y, float z, float y0) {
        poseStack.pushPose();
        poseStack.translate(0.0F, y0, 0.0F);
        poseStack.scale(-x, -y, z); // model y points down
        collector.submitModel(lashing, part, poseStack, LASHING_TEXTURE, state.lightCoords, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
    }
}
