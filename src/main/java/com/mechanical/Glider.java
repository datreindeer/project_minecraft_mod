package com.mechanical;

import java.util.function.Supplier;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Glider Cloak (worn in the Cloak slot): hold jump while falling (more than 2 blocks) to glide. A weak early glider: the fall slows to a fixed sink
 * rate and the body drifts in the look direction, far slower than an elytra and never upwards. Sneak or let go of jump
 * to drop. Landing from a glide does no fall damage. Costs 1 durability per second of gliding (server side).
 * The motion itself runs on the client (players move client-side), see client.GliderClient.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class Glider {
    /** Fall distance before the cloak catches the air, and what it is held at while gliding (fall damage starts above 3). */
    private static final double START_FALL = 2.0, HELD_FALL = 2.5;
    /** Sink speed (blocks/tick, ~2.4 m/s), forward push per tick and top horizontal speed (~6 m/s; elytra ~30). */
    private static final double SINK = -0.12, DRIFT = 0.025, MAX_SPEED = 0.3;
    /** Gliding right now (server's view, synced so everyone sees the cloak spread, client.EquipmentLayer). Not saved. */
    public static final Supplier<AttachmentType<Boolean>> GLIDING = Mechanical.ATTACHMENTS.register("gliding",
            () -> AttachmentType.builder(() -> false).sync(ByteBufCodecs.BOOL).build());

    public static void init() {}

    public static boolean canGlide(Player player, boolean jump, boolean sneak) {
        return jump && !sneak && com.mechanical.equipment.Equipment.get(player, com.mechanical.equipment.Accessory.CLOAK).is(Mechanical.GLIDER_CLOAK.get())
                && !player.onGround() && !player.isFallFlying() && !player.getAbilities().flying && !player.isInWater()
                && !player.isInLava() && !player.onClimbable() && !player.isPassenger() && player.fallDistance > START_FALL;
    }

    /** Client: one tick of gliding for the local player, after its normal movement. */
    public static void glide(Player player) {
        player.fallDistance = Math.min(player.fallDistance, HELD_FALL);
        Vec3 v = player.getDeltaMovement();
        Vec3 look = player.getLookAngle().multiply(1, 0, 1);
        Vec3 flat = new Vec3(v.x, 0, v.z);
        if (look.lengthSqr() > 1.0E-4) flat = flat.add(look.normalize().scale(DRIFT));
        if (flat.length() > MAX_SPEED) flat = flat.normalize().scale(MAX_SPEED);
        player.setDeltaMovement(flat.x, Math.max(v.y, SINK), flat.z);
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Input input = player.getLastClientInput();
        boolean gliding = canGlide(player, input.jump(), input.shift());
        if (player.getData(GLIDING) != gliding) player.setData(GLIDING, gliding);
        if (!gliding) return;
        // the cloak takes the fall: the server's fall distance (from the client's moves) stays below the damage threshold
        player.fallDistance = Math.min(player.fallDistance, HELD_FALL);
        if (player.tickCount % 20 == 0) com.mechanical.equipment.Equipment.damageCloak(player, player.level(), 1);
    }

    private Glider() {}
}
