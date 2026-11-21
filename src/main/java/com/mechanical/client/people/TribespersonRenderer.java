package com.mechanical.client.people;

import java.util.HashMap;
import java.util.Map;

import com.mechanical.people.Tribesperson;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;

/**
 * Tribespeople use the player model and 64x64 player-format skins: wide arms for men, slim for women.
 * Vanilla draws every AvatarRenderState with the player renderer picked by state.skin, so this renderer only
 * fills the state; the skin (texture + arm width) goes into state.skin. The skin is the person's climate outfit, dyed in
 * their tribe's colour (TintedSkin). The Miner walks hunched (the sneaking pose) under his ore sack (MinerSackModel, drawn
 * by EquipmentLayer).
 */
public class TribespersonRenderer extends MobRenderer<Tribesperson, AvatarRenderState, PlayerModel> {
    private static final Map<Identifier, PlayerSkin> SKINS = new HashMap<>();

    public TribespersonRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        // EquipmentClient only adds this layer to the player renderers; the Miner's sack needs it here too
        addLayer(new com.mechanical.client.EquipmentLayer(this, context.getModelSet(), context.getEquipmentRenderer()));
        addLayer(new com.mechanical.client.HoldPoses.GripLayer(this)); // a hot piece held in tongs
    }

    @Override
    public AvatarRenderState createRenderState() {
        return new AvatarRenderState();
    }

    @Override
    public void extractRenderState(Tribesperson entity, AvatarRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        HumanoidMobRenderer.extractHumanoidRenderState(entity, state, partialTicks, itemModelResolver);
        state.leftArmPose = armPose(entity, HumanoidArm.LEFT);
        state.rightArmPose = armPose(entity, HumanoidArm.RIGHT);
        com.mechanical.client.HoldPoses.extract(entity, state);
        state.arrowCount = entity.getArrowCount();
        boolean slim = entity.isWoman();
        state.skin = SKINS.computeIfAbsent(TintedSkin.of(entity.texture(), entity.colour()), t -> PlayerSkin.insecure(
                new ClientAsset.ResourceTexture(t, t), null, null, slim ? PlayerModelType.SLIM : PlayerModelType.WIDE));
        if (entity.role() == com.mechanical.people.TribeRole.MINER && entity.getPose() == net.minecraft.world.entity.Pose.STANDING) {
            state.isCrouching = true; // hunched under his sack (the sneaking pose)
            com.mechanical.client.EquipmentClient.wearMinerSack(state);
        }
    }

    private static HumanoidModel.ArmPose armPose(Tribesperson mob, HumanoidArm arm) {
        ItemStack held = mob.getItemHeldByArm(arm);
        if (held.isEmpty()) return HumanoidModel.ArmPose.EMPTY;
        // item poses from IClientItemExtensions (client/HoldPoses: staffs ...), as vanilla does for players
        HumanoidModel.ArmPose custom = net.neoforged.neoforge.client.extensions.common.IClientItemExtensions.of(held).getArmPose(mob,
                arm == mob.getMainArm() ? net.minecraft.world.InteractionHand.MAIN_HAND : net.minecraft.world.InteractionHand.OFF_HAND, held);
        if (custom != null) return custom;
        if (held.getItem() instanceof BowItem && mob.isAggressive() && mob.getMainArm() == arm) return HumanoidModel.ArmPose.BOW_AND_ARROW;
        if (HumanoidMobRenderer.usesSpearPose(held, arm, mob)) return HumanoidModel.ArmPose.SPEAR;
        return HumanoidModel.ArmPose.ITEM;
    }

    @Override
    public Identifier getTextureLocation(AvatarRenderState state) {
        return state.skin.body().texturePath();
    }
}
