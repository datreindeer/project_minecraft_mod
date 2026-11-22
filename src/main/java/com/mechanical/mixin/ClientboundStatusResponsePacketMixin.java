package com.mechanical.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.mechanical.clan.ServerListInfo;
import com.mojang.serialization.Codec;

import net.minecraft.network.protocol.status.ClientboundStatusResponsePacket;
import net.minecraft.network.protocol.status.ServerStatus;

/** The client reads a server's status with a codec that also picks up the mod's server list field (ServerListInfo). */
@Mixin(ClientboundStatusResponsePacket.class)
public abstract class ClientboundStatusResponsePacketMixin {
    @ModifyArg(method = "<clinit>", index = 1, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/network/codec/ByteBufCodecs;fromCodec(Lcom/mojang/serialization/DynamicOps;Lcom/mojang/serialization/Codec;)Lnet/minecraft/network/codec/StreamCodec$CodecOperation;"))
    private static Codec<ServerStatus> mechanical$readServerListInfo(Codec<ServerStatus> codec) {
        return ServerListInfo.reading(codec);
    }
}
