package com.mechanical.client;

import com.mechanical.equipment.CloakItem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.object.equipment.ElytraModel;
import net.minecraft.client.model.player.PlayerCapeModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.resources.model.EquipmentClientInfo;

/**
 * What a player wears in the accessory slots, drawn on their model (data from EquipmentClient's render-state modifier):
 * <ul>
 * <li>a cloak ({@link CloakItem}): its humanoid equipment layer over body and shoulders (a size above chest armour),
 * then a cape like a skin cape (textures/entity/cloak/&lt;id&gt;.png, pushed out over chest armour) or, for the Glider
 * Cloak, its wings layer (folded, spread while gliding);</li>
 * <li>the Leather Satchel ({@link WornSatchelModel}), the Quiver ({@link WornQuiverModel}) and the Leather Pouch ({@link WornPouchModel});</li>
 * <li>the Miner's ore sack ({@link MinerSackModel}, people.TribespersonRenderer adds this layer too);</li>
 * <li>a tribesperson's gear: hood, mantle, cloak, necklace, braids, beard, feathers ({@link TribeGearModel}).</li>
 * </ul>
 */
public class EquipmentLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private final HumanoidModel<AvatarRenderState> cloakBody;
    private final HumanoidModel<AvatarRenderState> cape;
    private final ElytraModel wings;
    private final WornSatchelModel satchel;
    private final WornQuiverModel quiver;
    private final WornPouchModel pouch;
    private final MinerSackModel minerSack;
    private final TribeGearModel tribeGear;
    private final EquipmentLayerRenderer equipmentRenderer;

    public EquipmentLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer, EntityModelSet models, EquipmentLayerRenderer equipmentRenderer) {
        super(renderer);
        this.cloakBody = new HumanoidModel<>(models.bakeLayer(EquipmentClient.CLOAK_BODY));
        this.cape = new PlayerCapeModel(models.bakeLayer(ModelLayers.PLAYER_CAPE));
        this.wings = new ElytraModel(models.bakeLayer(ModelLayers.ELYTRA));
        this.satchel = new WornSatchelModel(models.bakeLayer(WornSatchelModel.LAYER));
        this.quiver = new WornQuiverModel(models.bakeLayer(WornQuiverModel.LAYER));
        this.pouch = new WornPouchModel(models.bakeLayer(WornPouchModel.LAYER));
        this.minerSack = new MinerSackModel(models.bakeLayer(MinerSackModel.LAYER));
        this.tribeGear = new TribeGearModel(models.bakeLayer(TribeGearModel.LAYER));
        this.equipmentRenderer = equipmentRenderer;
        // only body and arms, like a chest piece
        cloakBody.head.visible = cloakBody.hat.visible = cloakBody.rightLeg.visible = cloakBody.leftLeg.visible = false;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        if (state.isInvisible) return;
        ItemStack cloak = EquipmentClient.cloak(state);
        if (cloak != null && cloak.getItem() instanceof CloakItem item) {
            equipmentRenderer.renderLayers(EquipmentClientInfo.LayerType.HUMANOID, item.asset(), cloakBody, state, cloak, poseStack, collector, light, state.outlineColor);
            if (item.hasCape()) {
                poseStack.pushPose();
                if (EquipmentClient.chestArmored(state)) poseStack.translate(0.0F, -0.053125F, 0.06875F); // as vanilla's CapeLayer
                collector.submitModel(cape, state, poseStack, RenderTypes.entityCutout(capeTexture(cloak)), light, OverlayTexture.NO_OVERLAY, state.outlineColor);
                poseStack.popPose();
            } else {
                poseStack.pushPose();
                poseStack.translate(0.0F, 0.0F, 0.125F); // as vanilla's WingsLayer
                equipmentRenderer.renderLayers(EquipmentClientInfo.LayerType.WINGS, item.asset(), wings, state, cloak, poseStack, collector, light, state.outlineColor);
                poseStack.popPose();
            }
        }
        if (EquipmentClient.satchel(state)) {
            collector.submitModel(satchel, state, poseStack, RenderTypes.entityCutout(WornSatchelModel.TEXTURE), light, OverlayTexture.NO_OVERLAY, state.outlineColor);
        }
        if (EquipmentClient.pouch(state)) {
            collector.submitModel(pouch, state, poseStack, RenderTypes.entityCutout(WornPouchModel.TEXTURE), light, OverlayTexture.NO_OVERLAY, state.outlineColor);
        }
        Identifier gear = EquipmentClient.tribeGear(state);
        if (gear != null) {
            collector.submitModel(tribeGear, state, poseStack, RenderTypes.entityCutout(gear), light, OverlayTexture.NO_OVERLAY, state.outlineColor);
        }
        if (EquipmentClient.minerSack(state)) {
            collector.submitModel(minerSack, state, poseStack, RenderTypes.entityCutout(MinerSackModel.TEXTURE), light, OverlayTexture.NO_OVERLAY, state.outlineColor);
        }
        if (EquipmentClient.quiverArrows(state) >= 0) {
            collector.submitModel(quiver, state, poseStack, RenderTypes.entityCutout(WornQuiverModel.TEXTURE), light, OverlayTexture.NO_OVERLAY, state.outlineColor);
        }
    }

    private static Identifier capeTexture(ItemStack cloak) {
        Identifier id = BuiltInRegistries.ITEM.getKey(cloak.getItem());
        return id.withPath(p -> "textures/entity/cloak/" + p + ".png");
    }
}
