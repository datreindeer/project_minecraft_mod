package com.mechanical.client;

import com.mechanical.Mechanical;
import com.mechanical.Temperature;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.gui.GuiLayer;

/** Ten thermometers above the hunger bar, filled right to left like food: warm, cold or freezing. */
public class TemperatureHud implements GuiLayer {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(Mechanical.MODID, "temperature");
    private static final Identifier BACKGROUND = sprite("background");

    private static Identifier sprite(String name) {
        return Identifier.fromNamespaceAndPath(Mechanical.MODID, "hud/temperature/" + name);
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.gameMode == null || !mc.gameMode.canHurtPlayer()) return;
        var hud = mc.gui.hud;
        float body = Temperature.of(player);
        String kind = body >= 10 ? "warm" : body >= Temperature.FREEZING_BELOW ? "cold" : "freezing";
        int points = Math.round(body); // 0..20, two per icon
        int xRight = graphics.guiWidth() / 2 + 91;
        int y = graphics.guiHeight() - hud.rightHeight;
        for (int i = 0; i < 10; i++) {
            int x = xRight - i * 8 - 9;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, x, y, 9, 9);
            if (i * 2 + 1 < points) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite(kind + "_full"), x, y, 9, 9);
            else if (i * 2 + 1 == points) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite(kind + "_half"), x, y, 9, 9);
        }
        hud.rightHeight += 10;
    }
}
