package com.mechanical.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mechanical.client.clan.ServerListDetails;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;

/**
 * A server entry on the Multiplayer screen (client.clan.ServerListDetails): for a server running the mod the content box
 * moves inside the bark card's frame (vanilla draws and handles clicks from it, so both follow), the card is drawn under
 * vanilla's content and the title is the server's own name; a server that answered without the mod is dimmed and marked.
 */
@Mixin(ServerSelectionList.OnlineServerEntry.class)
public abstract class OnlineServerEntryMixin extends ServerSelectionList.Entry {
    @Shadow @Final private ServerData serverData;

    @Override
    public int getContentX() {
        return getX() + 2 + ServerListDetails.inset(serverData);
    }

    @Override
    public int getContentY() {
        return getY() + 2 + ServerListDetails.inset(serverData);
    }

    @Override
    public int getContentWidth() {
        return getWidth() - 4 - 2 * ServerListDetails.inset(serverData);
    }

    @Override
    public int getContentHeight() {
        return getHeight() - 4 - 2 * ServerListDetails.inset(serverData);
    }

    @Inject(method = "extractContent", at = @At("HEAD"))
    private void mechanical$card(GuiGraphicsExtractor g, int mouseX, int mouseY, boolean hovered, float a, CallbackInfo ci) {
        ServerListDetails.drawCard(g, (ServerSelectionList.OnlineServerEntry) (Object) this);
    }

    @Inject(method = "extractContent", at = @At("TAIL"))
    private void mechanical$markVanilla(GuiGraphicsExtractor g, int mouseX, int mouseY, boolean hovered, float a, CallbackInfo ci) {
        ServerListDetails.markVanilla(g, (ServerSelectionList.OnlineServerEntry) (Object) this);
    }

    @ModifyArg(method = "extractContent", index = 1, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V"))
    private String mechanical$title(String typed) {
        return ServerListDetails.title(serverData, typed);
    }
}
