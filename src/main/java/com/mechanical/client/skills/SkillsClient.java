package com.mechanical.client.skills;

import com.mechanical.Mechanical;
import com.mechanical.skills.SkillPayload;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

/** Client side of the skills: K opens {@link SkillsScreen}; each XP gain shows {@link SkillToast}, a new level also rings. */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class SkillsClient {
    public static final KeyMapping KEY = new KeyMapping("key.mechanical.skills", KeyConflictContext.IN_GAME, InputConstants.Type.KEYBOARD,
            InputConstants.KEY_K, KeyMapping.Category.INVENTORY);

    private SkillsClient() {}

    @SubscribeEvent
    static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(KEY);
    }

    @SubscribeEvent
    static void registerHud(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, SkillToast.ID, SkillToast::render);
    }

    @SubscribeEvent
    static void registerHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(SkillPayload.TYPE, (payload, context) -> {
            SkillToast.add(payload.skill(), payload.amount(), payload.newLevel());
            if (payload.newLevel() > 0) Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.0F, 0.6F));
        });
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (KEY.consumeClick()) {
            if (mc.player != null && mc.gui.screen() == null) mc.gui.setScreen(new SkillsScreen(SkillsScreen.last));
        }
    }
}
