package com.mechanical.people;

import java.util.Locale;

import com.mechanical.Carting;
import com.mechanical.CarcassHauling;
import com.mechanical.FelledLogEntity;
import com.mechanical.HangingRackBlock;
import com.mechanical.Mechanical;
import com.mechanical.TravoisEntity;
import com.mechanical.WaterPower;
import com.mechanical.WaterWheelBlock;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Dev only (multiplayer check, v0.195.0): {@code /mptest <scene>} builds a test scene beside the player on a dedicated
 * server for client.MpShots to play through with real clicks; {@code /mptest where} answers with the server's view of the
 * vehicles, logs, horses and pots near the player (chat lines starting "[mp]") so the client can compare it with its own.
 * Registered only when {@code mptest.flag} is in the server's game directory.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class MpTest {
    @SubscribeEvent
    static void registerCommands(RegisterCommandsEvent event) {
        if (!FMLPaths.GAMEDIR.get().resolve("mptest.flag").toFile().exists()) return;
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("mptest").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        for (String scene : new String[] {"wagon", "log", "wheel", "caravan", "quest", "kitchen", "bleed", "where"})
            root.then(Commands.literal(scene).executes(ctx -> {
                run(scene, ctx.getSource().getPlayerOrException());
                return 1;
            }));
        event.getDispatcher().register(root);
    }

    private static void run(String scene, ServerPlayer player) {
        ServerLevel level = player.level();
        BlockPos at = player.blockPosition();
        Vec3 p = player.position();
        switch (scene) {
            case "wagon" -> { // a wagon facing +x, its two horses unhitched ahead of it (the lead one saddled), a lead in hand
                var wagon = Carting.WAGON.get().create(level, EntitySpawnReason.COMMAND);
                Vec3 w = p.add(4, 0, 4);
                wagon.setPos(w);
                wagon.setYRot(-90);
                level.addFreshEntity(wagon);
                AbstractHorse lead = horse(level, w.add(com.mechanical.WagonEntity.LEAD, 0, -0.7)), partner = horse(level, w.add(com.mechanical.WagonEntity.LEAD, 0, 0.7));
                lead.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Carting.HIDE_SADDLE_BLANKET.get()));
                lead.addTag("mp_lead");
                partner.addTag("mp_partner");
                player.getInventory().add(new ItemStack(Items.LEAD));
            }
            case "log" -> FelledLogEntity.spawnStraight(level, p.add(2.5, 0, 0), 90, 4, Blocks.OAK_LOG.defaultBlockState());
            case "wheel" -> { // a hub 3 up, the wheel across x, a spring over its top corner running down through its cells
                BlockPos hub = at.offset(0, 3, 6);
                level.setBlockAndUpdate(hub, WaterPower.WATER_WHEEL.get().defaultBlockState().setValue(WaterWheelBlock.AXIS, Direction.Axis.X));
                level.setBlockAndUpdate(hub.offset(0, 3, 2), Blocks.WATER.defaultBlockState());
            }
            case "caravan" -> {
                Tribesperson leader = Caravans.spawn(level, at.offset(0, 0, 6), Identifier.fromNamespaceAndPath(Mechanical.MODID, "jewelry"), true);
                if (leader != null) {
                    leader.setNoAi(true);
                    leader.snapTo(p.x, p.y, p.z + 2.5, 180, 0);
                }
            }
            case "quest" -> {
                Tribesperson t = People.TRIBESPERSON.get().create(level, EntitySpawnReason.COMMAND);
                t.snapTo(p.x, p.y, p.z + 2.5, 180, 0);
                t.setup(TribeRole.OAK_KEEPER, null, Tribe.OAK, true, ClimateZone.TEMPERATE);
                t.setNoAi(true);
                t.setPersistenceRequired();
                level.addFreshEntity(t);
            }
            case "kitchen" -> { // two lit campfires (a tripod and a clay pot go on them by hand) and a hanging rack facing the player
                var fire = Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true);
                level.setBlockAndUpdate(at.offset(-1, 0, 3), fire);
                level.setBlockAndUpdate(at.offset(1, 0, 3), fire);
                var rack = CarcassHauling.HANGING_RACK.get().defaultBlockState().setValue(HangingRackBlock.FACING, Direction.NORTH);
                for (HangingRackBlock.Part part : HangingRackBlock.Part.values())
                    level.setBlockAndUpdate(at.offset(4, part.ordinal(), 3), rack.setValue(HangingRackBlock.PART, part));
                for (var item : new net.minecraft.world.item.Item[] {Items.WATER_BUCKET, Items.WATER_BUCKET, Items.BEEF, Items.CARROT, Items.POTATO, Items.BOWL, Items.BOWL, Items.BUCKET})
                    player.getInventory().add(new ItemStack(item));
            }
            case "bleed" -> {
                BlockPos base = at.offset(4, 0, 3);
                for (int i = 0; i < HangingRackBlock.FULL; i++) HangingRackBlock.bleedOnto(level, base);
            }
            case "where" -> {
                for (Entity e : level.getEntities((Entity) null, player.getBoundingBox().inflate(24),
                        e -> e instanceof TravoisEntity || e instanceof FelledLogEntity || e instanceof AbstractHorse || e instanceof com.mechanical.WaterWheelHitbox))
                    player.sendSystemMessage(Component.literal(String.format(Locale.ROOT, "[mp] %s %d %.3f %.3f %.3f %.1f%s", e.getType().toShortString(), e.getId(),
                            e.getX(), e.getY(), e.getZ(), e.getYRot(), e.getVehicle() != null ? " on " + e.getVehicle().getId() : "")));
                for (BlockPos b : BlockPos.betweenClosed(at.offset(-6, -2, -2), at.offset(6, 3, 8)))
                    if (level.getBlockEntity(b) instanceof com.mechanical.TripodBlockEntity pot)
                        player.sendSystemMessage(Component.literal("[mp] pot " + b.toShortString() + " " + pot.saveWithoutMetadata(level.registryAccess())));
            }
            default -> {}
        }
    }

    private static AbstractHorse horse(ServerLevel level, Vec3 at) {
        AbstractHorse h = com.mechanical.wildlife.Wildlife.TAME_HORSE.get().create(level, EntitySpawnReason.COMMAND);
        h.setPos(at);
        h.setYRot(-90);
        h.setYBodyRot(-90);
        h.setTamed(true);
        h.setPersistenceRequired();
        level.addFreshEntity(h);
        return h;
    }

    private MpTest() {}
}
