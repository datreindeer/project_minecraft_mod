package com.mechanical.people;

import com.mechanical.Mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Dev only, for client.ui.GuiShots: a trading elder or a Trader Caravan before the player, and its trade screen. */
public final class GuiShotHooks {
    public static void clear(ServerPlayer p) {
        for (Entity e : p.level().getEntitiesOfClass(net.minecraft.world.entity.Mob.class, new AABB(p.blockPosition()).inflate(48))) e.discard();
    }

    public static void elder(ServerPlayer p, Vec3 stand) {
        clear(p);
        java.util.UUID traders = null;
        for (int i = 0; traders == null; i++) {
            java.util.UUID id = java.util.UUID.nameUUIDFromBytes(("tribe_trade" + i).getBytes());
            if (Tribe.of(id).temperament == Tribe.Temperament.TRADERS) traders = id;
        }
        Tribesperson elder = People.TRIBESPERSON.get().create(p.level(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        elder.setPos(stand.x, stand.y, stand.z + 2);
        elder.setNoAi(true);
        p.level().addFreshEntity(elder);
        elder.setup(TribeRole.ELDER, null, traders, false);
        var offer = elder.getOffers().get(0);
        p.getInventory().add(offer.getCostA().copy());
        p.getInventory().add(offer.getCostB().copy());
        // the client must know the entity before its portrait can be drawn
        p.level().getServer().schedule(new net.minecraft.server.TickTask(p.level().getServer().getTickCount() + 10, () -> CaravanTrade.open(p, elder)));
    }

    public static void caravan(ServerPlayer p, Vec3 stand) {
        clear(p);
        Tribesperson leader = Caravans.spawn(p.level(), BlockPos.containing(stand.x, stand.y, stand.z + 4),
                Identifier.fromNamespaceAndPath(Mechanical.MODID, "jewelry"), true);
        for (Tribesperson t : p.level().getEntitiesOfClass(Tribesperson.class, new AABB(p.blockPosition()).inflate(24))) t.setNoAi(true);
        leader.setPos(stand.x, stand.y, stand.z + 2);
        p.level().getServer().schedule(new net.minecraft.server.TickTask(p.level().getServer().getTickCount() + 10, () -> CaravanTrade.open(p, leader)));
    }

    private GuiShotHooks() {}
}
