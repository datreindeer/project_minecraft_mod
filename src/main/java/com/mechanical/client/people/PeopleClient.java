package com.mechanical.client.people;

import com.mechanical.Mechanical;
import com.mechanical.people.People;
import com.mechanical.people.TribeDialog;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class PeopleClient {
    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(People.TRIBESPERSON.get(), TribespersonRenderer::new);
    }

    /** An envoy speaks: show the talk screen (payload handlers run on the main thread). */
    @SubscribeEvent
    static void registerHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(TribeDialog.Open.TYPE, (payload, context) -> Minecraft.getInstance().gui.setScreen(new TribeDialogScreen(payload)));
    }

    private PeopleClient() {}
}
