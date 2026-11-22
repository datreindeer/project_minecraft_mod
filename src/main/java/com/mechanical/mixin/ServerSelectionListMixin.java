package com.mechanical.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mechanical.client.clan.ServerListDetails;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;

/**
 * The Multiplayer screen's list (ServerSelectionList inherits its drawing from here): entries of servers running the mod
 * grow tall enough for their detail panel, which is drawn under vanilla's entry (client.clan.ServerListDetails).
 * Every other list, and servers without the mod, are left alone.
 */
@Mixin(AbstractSelectionList.class)
public abstract class ServerSelectionListMixin {
    @Shadow @Final private List<?> children;
    @Shadow @Final protected int defaultEntryHeight;

    @Shadow
    private void repositionEntries() {}

    @Inject(method = "extractListItems", at = @At("HEAD"))
    private void mechanical$fitServers(GuiGraphicsExtractor g, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (!((Object) this instanceof ServerSelectionList)) return;
        boolean moved = false;
        for (Object child : children) if (child instanceof ServerSelectionList.OnlineServerEntry entry) {
            int height = ServerListDetails.height(entry.getServerData(), defaultEntryHeight);
            if (entry.getHeight() != height) {
                entry.setHeight(height);
                moved = true;
            }
        }
        if (moved) repositionEntries();
    }

    @Inject(method = "extractListItems", at = @At("TAIL"))
    private void mechanical$serverDetails(GuiGraphicsExtractor g, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (!((Object) this instanceof ServerSelectionList list)) return;
        for (Object child : children) if (child instanceof ServerSelectionList.OnlineServerEntry entry
                && entry.getY() + entry.getHeight() >= list.getY() && entry.getY() <= list.getBottom()) ServerListDetails.draw(g, entry);
    }
}
