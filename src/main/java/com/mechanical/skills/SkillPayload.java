package com.mechanical.skills;

import com.mechanical.Mechanical;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Server -> client: XP gained and, when the level went up, the new level (else 0). The client handler comes with Task 5; until then the packet is ignored. */
@EventBusSubscriber(modid = Mechanical.MODID)
public record SkillPayload(Skill skill, int amount, int newLevel) implements CustomPacketPayload {
    public static final Type<SkillPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "skill_xp"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SkillPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(i -> Skill.values()[i], Skill::ordinal), SkillPayload::skill,
            ByteBufCodecs.VAR_INT, SkillPayload::amount,
            ByteBufCodecs.VAR_INT, SkillPayload::newLevel, SkillPayload::new);

    @Override
    public Type<SkillPayload> type() {
        return TYPE;
    }

    @SubscribeEvent
    static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(TYPE, CODEC);
    }
}
