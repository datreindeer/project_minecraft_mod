package com.mechanical.client;

import java.util.HashMap;
import java.util.Map;

import com.mechanical.CarcassEntity;
import com.mechanical.Mechanical;
import com.mechanical.TravoisEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.PhantomRenderState;
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
 * shortcut: the copy has default looks (white sheep, default cow variant); sync the dead animal's data if that matters.
 */
public class CarcassRenderer extends EntityRenderer<CarcassEntity, CarcassRenderer.State> {
    /** Marks the animal's render state as a carcass so the model mixins let the limbs sag. */
    private static final ContextKey<Boolean> CARCASS = new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "carcass"));
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

    public static boolean isCarcass(EntityRenderState state) {
        return Boolean.TRUE.equals(state.getRenderData(CARCASS));
    }

    public static boolean isSkinned(EntityRenderState state) {
        return Boolean.TRUE.equals(state.getRenderData(SKINNED));
    }

    public static boolean isAged(EntityRenderState state) {
        return Boolean.TRUE.equals(state.getRenderData(AGED));
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
        if (!isAged(state)) return -1;
        Identifier drawn = carcassTexture(state, texture);
        return drawn.getPath().startsWith("textures/entity/skinned_aged/") ? -1 : AGED_TINT;
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

    public enum Pose { LYING, CARRIED, HUNG }

    public static class State extends EntityRenderState {
        EntityRenderState animal;
        EntityRenderer<Entity, EntityRenderState> renderer;
        float bodyYaw, width, height, hangLength;
        /** On a travois or cart: its bed's tilt (degrees, front up). */
        float tilt;
        boolean flat;
        Pose pose = Pose.LYING;
        /** Carried: from the carcass to the hauler's feet, and the shoulder height above them. */
        double dx, dy, dz;
        float shoulder;
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

    public CarcassRenderer(EntityRendererProvider.Context context) {
        super(context);
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
        state.pose = carcass.hungAt() != null ? Pose.HUNG : hauler != null ? Pose.CARRIED : Pose.LYING;
        boolean skinned = carcass.stage() > 0;
        if (dummy instanceof Sheep sheep) sheep.setSheared(skinned);
        state.renderer = (EntityRenderer<Entity, EntityRenderState>) Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(dummy);
        state.animal = state.renderer.createRenderState(dummy, partialTicks);
        // skinned/plucked: raw red flesh look (the vanilla hurt tint); a sheared sheep already looks bare
        // hung or carried the legs hang stiff from the body instead of sagging sideways to the ground
        if (state.pose == Pose.LYING) state.animal.setRenderData(CARCASS, true);
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
        }
        // a dead phantom: wings spread limp on the ground instead of flapping
        if (state.animal instanceof PhantomRenderState phantom) phantom.flapTime = LIMP_FLAP;
        state.flat = carcass.liesFlat();
        state.bodyYaw = carcass.getViewYRot(partialTicks);
        state.width = carcass.animal().getWidth();
        state.height = carcass.animal().getHeight();
        state.hangLength = carcass.getBbHeight();
        state.tilt = carcass.getVehicle() instanceof TravoisEntity bed ? bed.pitch(partialTicks) : 0.0F;
        if (state.pose == Pose.CARRIED) {
            Vec3 at = hauler.getPosition(partialTicks).subtract(carcass.getPosition(partialTicks));
            state.dx = at.x;
            state.dy = at.y;
            state.dz = at.z;
            state.shoulder = hauler.isCrouching() ? 1.15F : 1.45F;
            if (hauler instanceof LivingEntity living) state.bodyYaw = Mth.rotLerp(partialTicks, living.yBodyRotO, living.yBodyRot);
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.animal == null) return;
        poseStack.pushPose();
        if (state.pose == Pose.CARRIED) poseStack.translate(state.dx, state.dy + state.shoulder, state.dz);
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
        if (state.pose == Pose.HUNG) {
            // head down from the hook: the rump (z = +rear) goes up to the top of the hitbox, the body centred under it
            float rear = state.width * 0.6F;
            poseStack.translate(0.0F, state.hangLength - rear, state.height * 0.55F);
            poseStack.rotate(Axis.XP.rotationDegrees(-90.0F));
            state.renderer.submit(state.animal, poseStack, collector, camera);
            poseStack.popPose();
            return;
        }
        // lying on a travois or cart: along its tilted bed (the body faces the way it does)
        poseStack.rotate(Axis.XP.rotationDegrees(state.tilt));
        if (state.flat) {
            state.renderer.submit(state.animal, poseStack, collector, camera);
            poseStack.popPose();
            return;
        }
        // lift so the flank rests on the ground, and shift so the body (not the feet) is centred on the hitbox
        // the hitbox width overstates the body thickness for long animals (horse), so estimate the flank height and cap it
        float lift = Math.clamp(state.width * 0.42F, 0.1F, 0.38F);
        poseStack.translate(state.height / 2.0F, lift, 0.0F);
        poseStack.rotate(Axis.ZP.rotationDegrees(90.0F));
        state.renderer.submit(state.animal, poseStack, collector, camera);
        poseStack.popPose();
    }
}
