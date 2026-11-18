package com.mechanical.client.people;

import com.mechanical.people.Tribesperson;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;

/** Tribespeople use the player model and 64x64 player-format skins: wide arms for men, slim for women. */
public class TribespersonRenderer extends MobRenderer<Tribesperson, AvatarRenderState, PlayerModel> {
    private final PlayerModel wide, slim;

    public TribespersonRenderer(EntityRendererProvider.Context context) {
        this(context, new PlayerModel(context.bakeLayer(ModelLayers.PLAYER), false));
    }

    private TribespersonRenderer(EntityRendererProvider.Context context, PlayerModel wide) {
        super(context, wide, 0.5F);
        this.wide = wide;
        this.slim = new PlayerModel(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        addLayer(new ItemInHandLayer<>(this));
    }

    /** Render state with the skin texture and arm width. */
    static class State extends AvatarRenderState {
        Identifier texture;
        boolean slim;
    }

    @Override
    public AvatarRenderState createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(Tribesperson entity, AvatarRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        HumanoidMobRenderer.extractHumanoidRenderState(entity, state, partialTicks, itemModelResolver);
        state.leftArmPose = armPose(entity, HumanoidArm.LEFT);
        state.rightArmPose = armPose(entity, HumanoidArm.RIGHT);
        State s = (State) state;
        s.texture = entity.texture();
        s.slim = entity.isWoman();
    }

    private static HumanoidModel.ArmPose armPose(Tribesperson mob, HumanoidArm arm) {
        ItemStack held = mob.getItemHeldByArm(arm);
        if (held.isEmpty()) return HumanoidModel.ArmPose.EMPTY;
        if (held.getItem() instanceof BowItem && mob.isAggressive() && mob.getMainArm() == arm) return HumanoidModel.ArmPose.BOW_AND_ARROW;
        if (HumanoidMobRenderer.usesSpearPose(held, arm, mob)) return HumanoidModel.ArmPose.SPEAR;
        return HumanoidModel.ArmPose.ITEM;
    }

    @Override
    public void submit(AvatarRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        model = ((State) state).slim ? slim : wide;
        super.submit(state, poseStack, collector, camera);
    }

    @Override
    public Identifier getTextureLocation(AvatarRenderState state) {
        return ((State) state).texture;
    }
}
