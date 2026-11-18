package com.mechanical.client;

import java.util.HashMap;
import java.util.Map;

import com.mechanical.CarcassEntity;
import com.mechanical.Mechanical;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;

/**
 * Draws the carcass as the animal itself lying on its side: a never-added copy of the animal is rendered with its own
 * vanilla renderer inside a rotated pose.
 * shortcut: the copy has default looks (white sheep, default cow variant); sync the dead animal's data if that matters.
 */
public class CarcassRenderer extends EntityRenderer<CarcassEntity, CarcassRenderer.State> {
    /** Marks the animal's render state as a carcass so the model mixins let the limbs sag. */
    private static final ContextKey<Boolean> CARCASS = new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "carcass"));
    /** Limb roll (model z rotation, radians) towards the ground for an animal lying on its side. */
    public static final float LIMB_SAG = -0.7F;
    /** Head yaw (degrees) that turns the head down to the ground. */
    private static final float HEAD_SAG = -50.0F;

    /** Skinned/plucked carcass: rendered with the flesh texture (see LivingEntityRendererMixin). */
    private static final ContextKey<Boolean> SKINNED = new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "skinned"));
    private static final Map<Identifier, Identifier> SKINNED_TEXTURES = new HashMap<>();

    public static boolean isCarcass(EntityRenderState state) {
        return Boolean.TRUE.equals(state.getRenderData(CARCASS));
    }

    public static boolean isSkinned(EntityRenderState state) {
        return Boolean.TRUE.equals(state.getRenderData(SKINNED));
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

    public static class State extends EntityRenderState {
        EntityRenderState animal;
        EntityRenderer<Entity, EntityRenderState> renderer;
        float bodyYaw, width, height;
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
        boolean skinned = carcass.stage() > 0;
        if (dummy instanceof Sheep sheep) sheep.setSheared(skinned);
        state.renderer = (EntityRenderer<Entity, EntityRenderState>) Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(dummy);
        state.animal = state.renderer.createRenderState(dummy, partialTicks);
        // skinned/plucked: raw red flesh look (the vanilla hurt tint); a sheared sheep already looks bare
        state.animal.setRenderData(CARCASS, true);
        if (state.animal instanceof LivingEntityRenderState living) {
            living.yRot = HEAD_SAG; // models use yRot as head yaw; lying on the side that points the head at the ground
            living.xRot = 0.0F;
            if (skinned && !(dummy instanceof Sheep)) {
                // flesh texture if we have one for this animal, otherwise the red hurt tint
                if (hasSkinnedTexture(state.renderer, living)) living.setRenderData(SKINNED, true);
                else living.hasRedOverlay = true;
            }
        }
        state.bodyYaw = carcass.bodyYaw();
        state.width = carcass.animal().getWidth();
        state.height = carcass.animal().getHeight();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.animal == null) return;
        poseStack.pushPose();
        poseStack.rotate(Axis.YP.rotationDegrees(180.0F - state.bodyYaw));
        // lift so the flank rests on the ground, and shift so the body (not the feet) is centred on the hitbox
        // the hitbox width overstates the body thickness for long animals (horse), so estimate the flank height and cap it
        float lift = Math.clamp(state.width * 0.42F, 0.1F, 0.38F);
        poseStack.translate(state.height / 2.0F, lift, 0.0F);
        poseStack.rotate(Axis.ZP.rotationDegrees(90.0F));
        state.renderer.submit(state.animal, poseStack, collector, camera);
        poseStack.popPose();
    }
}
