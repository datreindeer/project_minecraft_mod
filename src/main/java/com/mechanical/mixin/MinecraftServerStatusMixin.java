package com.mechanical.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.google.gson.JsonElement;
import com.mechanical.clan.ServerListInfo;

import net.minecraft.server.MinecraftServer;

/** The status JSON the server caches (every 5 s) and answers pings with gets the mod's server list field (ServerListInfo). */
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerStatusMixin {
    @ModifyArg(method = "resetStatusCache", at = @At(value = "INVOKE", target = "Lcom/google/gson/Gson;toJson(Lcom/google/gson/JsonElement;)Ljava/lang/String;"))
    private JsonElement mechanical$serverListInfo(JsonElement status) {
        ServerListInfo.addTo((MinecraftServer) (Object) this, status);
        return status;
    }
}
