package com.mechanical.client.wildlife.river;

import com.mechanical.wildlife.river.MosquitoSwarm;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/** Draws the swarm's specks (MosquitoSwarmModel); no shadow. */
public class MosquitoSwarmRenderer extends MobRenderer<MosquitoSwarm, LivingEntityRenderState, MosquitoSwarmModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/mosquito_swarm.png");

    public MosquitoSwarmRenderer(EntityRendererProvider.Context context) {
        super(context, new MosquitoSwarmModel(context.bakeLayer(MosquitoSwarmModel.LAYER)), 0.0F);
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }
}
