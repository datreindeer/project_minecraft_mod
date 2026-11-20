package com.mechanical.client.wildlife.warm;

import com.mechanical.client.wildlife.WildAnimalRenderState;
import com.mechanical.client.wildlife.WildAnimalRenderer;
import com.mechanical.wildlife.WildAnimal;
import com.mechanical.wildlife.warm.DesertLizard;
import com.mechanical.wildlife.warm.Peafowl;
import com.mechanical.wildlife.warm.Tortoise;
import com.mechanical.wildlife.warm.WaterBuffalo;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** The wildlife renderer with {@link WarmRenderState}: fills in the tortoise's shell, the lizard's tail, the buffalo's wallow, the peacock's fan. */
public class WarmAnimalRenderer<T extends WildAnimal> extends WildAnimalRenderer<T> {
    public WarmAnimalRenderer(EntityRendererProvider.Context context, EntityModel<WildAnimalRenderState> adult,
            EntityModel<WildAnimalRenderState> baby, float babyScale, float shadow) {
        super(context, adult, baby, babyScale, shadow);
    }

    @Override
    public WarmRenderState createRenderState() {
        return new WarmRenderState();
    }

    @Override
    public void extractRenderState(T entity, WildAnimalRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        WarmRenderState s = (WarmRenderState) state;
        s.shell = entity instanceof Tortoise t ? t.shellAmount(partialTicks) : 0.0F;
        s.tailless = entity instanceof DesertLizard l && l.isTailless();
        s.basking = entity instanceof DesertLizard l && l.isBasking();
        s.wallowing = entity instanceof WaterBuffalo b && b.isWallowing();
        s.display = entity instanceof Peafowl p ? p.displayAmount(partialTicks) : 0.0F;
    }
}
