package com.mechanical;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.mechanical.equipment.Accessory;
import com.mechanical.equipment.Equipment;
import com.mechanical.equipment.PackMenu;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Dev only: with {@code gearshots.flag} in the game directory (first line: output folder) the client makes a flat world
 * "GearShots" and shoots gear_*.png: the worn models with both Hip slots in use (two pouches; pouch and quiver; quiver and
 * pouch over armour under a cloak) from behind, the sides and the front, the survival and creative inventories with the
 * second Hip slot, and the B screen with satchel, pouch and quiver; then quits. Nothing happens without the flag file.
 * Pattern: PackShots.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class GearShots {
    private static final String WORLD = "GearShots";
    private static int stage = -1, ticks, step;
    private static File out;
    private static ArmorStand camera;
    private static Vec3 body;

    /** One shot: what to set up on the server, then (client) what to show, then the file name. */
    private record Shot(String name, Consumer<ServerPlayer> server, Runnable client) {}

    private static final List<Shot> SHOTS = new ArrayList<>();

    private static void wear(ServerPlayer p, ItemStack back, ItemStack hip, ItemStack hip2, ItemStack chest, ItemStack cloak) {
        Equipment.of(p).set(Accessory.BACK, back);
        Equipment.of(p).set(Accessory.HIP, hip);
        Equipment.of(p).set(Accessory.HIP_2, hip2);
        Equipment.of(p).set(Accessory.CLOAK, cloak);
        p.setItemSlot(EquipmentSlot.CHEST, chest);
    }

    private static ItemStack quiver(int arrows) {
        ItemStack q = new ItemStack(Mechanical.QUIVER.get());
        NonNullList<ItemStack> items = NonNullList.withSize(4, ItemStack.EMPTY);
        for (int i = 0; i < arrows; i++) items.set(i, new ItemStack(Items.ARROW, 16));
        q.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
        return q;
    }

    static {
        ItemStack none = ItemStack.EMPTY;
        // A: two pouches and the satchel
        Consumer<ServerPlayer> a = p -> wear(p, new ItemStack(Mechanical.LEATHER_SATCHEL.get()), new ItemStack(Mechanical.LEATHER_POUCH.get()),
                new ItemStack(Mechanical.LEATHER_POUCH.get()), none, none);
        for (String view : new String[] {"back", "left", "front", "right", "back_left"}) SHOTS.add(new Shot("two_pouches_" + view, a, () -> look(view)));
        // B: pouch in the first Hip slot, quiver in the second, satchel
        Consumer<ServerPlayer> b = p -> wear(p, new ItemStack(Mechanical.LEATHER_SATCHEL.get()), new ItemStack(Mechanical.LEATHER_POUCH.get()), quiver(3), none, none);
        for (String view : new String[] {"back", "left", "back_left", "front"}) SHOTS.add(new Shot("pouch_quiver_" + view, b, () -> look(view)));
        // C: quiver first, pouch second, over an iron chestplate and under a cloak
        Consumer<ServerPlayer> c = p -> wear(p, none, quiver(4), new ItemStack(Mechanical.LEATHER_POUCH.get()), new ItemStack(Items.IRON_CHESTPLATE),
                new ItemStack(com.mechanical.wildlife.river.RiverFauna.OTTER_FUR_CAPE.get()));
        for (String view : new String[] {"back", "right", "back_right"}) SHOTS.add(new Shot("quiver_pouch_armored_" + view, c, () -> look(view)));
        // the screens
        Consumer<ServerPlayer> gear = p -> {
            wear(p, new ItemStack(Mechanical.LEATHER_SATCHEL.get()), new ItemStack(Mechanical.LEATHER_POUCH.get()), quiver(2), none,
                    new ItemStack(com.mechanical.wildlife.river.RiverFauna.OTTER_FUR_CAPE.get()));
            Equipment.of(p).set(Accessory.RING_1, Jewelry.make(Jewelry.SILVER_RING.get(), new Jewelry.Jewel("ruby", false)));
            Equipment.of(p).set(Accessory.NECK, Jewelry.make(Jewelry.SILVER_NECKLACE.get(), new Jewelry.Jewel("sapphire", true)));
            p.getInventory().setItem(0, new ItemStack(Jewelry.SILVER_RING_BLANK.get(), 2));
            p.getInventory().setItem(1, new ItemStack(Jewelry.SILVER_CHAIN.get()));
            p.getInventory().setItem(2, Jewelry.make(Jewelry.SILVER_RING.get(), new Jewelry.Jewel("emerald", false)));
            p.getInventory().setItem(3, Jewelry.make(Jewelry.GOLD_RING.get(), new Jewelry.Jewel("emerald", false)));
            p.getInventory().setItem(4, new ItemStack(Smithing.STEEL_KNIFE.get()));
            p.setGameMode(GameType.SURVIVAL);
        };
        SHOTS.add(new Shot("inventory", gear, () -> Minecraft.getInstance().gui.setScreen(new InventoryScreen(Minecraft.getInstance().player))));
        SHOTS.add(new Shot("creative_inventory", p -> p.setGameMode(GameType.CREATIVE), () -> {
            Minecraft mc = Minecraft.getInstance();
            CreativeModeInventoryScreen screen = new CreativeModeInventoryScreen(mc.player, mc.player.connection.enabledFeatures(), true);
            mc.gui.setScreen(screen);
            try { // the inventory tab (selectTab is private)
                var m = CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab", net.minecraft.world.item.CreativeModeTab.class);
                m.setAccessible(true);
                m.invoke(screen, net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(CreativeModeTabs.INVENTORY));
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }));
        SHOTS.add(new Shot("pack_screen", p -> {
            p.setGameMode(GameType.SURVIVAL);
            PackMenu.openWorn(p);
            var menu = p.containerMenu;
            menu.getSlot(0).set(new ItemStack(Items.BREAD, 12));
            menu.getSlot(4).set(new ItemStack(Items.TORCH, 32));
            menu.getSlot(9).set(new ItemStack(Smithing.STEEL_KNIFE.get()));
            menu.getSlot(10).set(new ItemStack(Items.FLINT, 5));
        }, () -> {}));
        SHOTS.add(new Shot("pack_screen_pack", p -> {
            p.closeContainer();
            Equipment.of(p).set(Accessory.BACK, new ItemStack(Satchels.STEEL_FRAME_PACK.get()));
            PackMenu.openWorn(p);
            p.containerMenu.getSlot(0).set(new ItemStack(Items.COBBLESTONE, 64));
            p.containerMenu.getSlot(25).set(new ItemStack(Smithing.STEEL_SAW.get()));
        }, () -> {}));
    }

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (stage == -1) {
                File flag = new File(mc.gameDirectory, "gearshots.flag");
                if (!flag.exists()) {
                    stage = -2;
                    return;
                }
                out = new File(Files.readAllLines(flag.toPath()).get(0).trim());
                out.mkdirs();
                stage = 0;
            }
            if (stage < 0) return;
            if (stage == 0) {
                if (mc.gui.screen() instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen) mc.gui.setScreen(new TitleScreen());
                if (mc.gui.screen() instanceof TitleScreen) {
                    mc.options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);
                    mc.createWorldOpenFlows().createFreshLevel(WORLD,
                            new LevelSettings(WORLD, GameType.CREATIVE, LevelSettings.DifficultySettings.DEFAULT, true, WorldDataConfiguration.DEFAULT),
                            new WorldOptions(77L, false, false),
                            reg -> reg.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                            new TitleScreen());
                    stage = 1;
                    ticks = 0;
                }
                return;
            }
            if (mc.player == null || mc.getSingleplayerServer() == null) return;
            ticks++;
            var server = mc.getSingleplayerServer();
            if (stage == 1) {
                mc.options.pauseOnLostFocus = false;
                if (mc.gui.screen() != null) mc.gui.setScreen(null);
                if (ticks < 80) return;
                server.execute(() -> {
                    for (String cmd : List.of("time set noon", "gamerule advance_time false", "weather clear")) {
                        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), cmd);
                    }
                    ServerPlayer p = server.getPlayerList().getPlayers().get(0);
                    int ground = p.level().getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0);
                    p.teleportTo(p.level(), 0.5, ground, 0.5, java.util.Set.of(), 0, 0, true);
                    body = new Vec3(0.5, ground + 1.0, 0.5);
                });
                stage = 2;
                ticks = 0;
                return;
            }
            if (step >= SHOTS.size()) {
                mc.stop();
                return;
            }
            Shot shot = SHOTS.get(step);
            if (ticks == 1) {
                server.execute(() -> shot.server().accept(server.getPlayerList().getPlayers().get(0)));
            } else if (ticks == 15) {
                boolean screen = shot.name().contains("inventory") || shot.name().contains("screen");
                if (screen) {
                    mc.setCameraEntity(mc.player);
                    if (mc.gui.hud.isHidden()) mc.gui.hud.toggle();
                } else if (!mc.gui.hud.isHidden()) {
                    mc.gui.hud.toggle();
                }
                shot.client().run();
            } else if (ticks == 45) {
                Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
                    try (image) {
                        image.writeToFile(new File(out, "gear_" + shot.name() + ".png"));
                    } catch (Exception ignored) {}
                });
            } else if (ticks == 50) {
                if (!shot.name().startsWith("pack_screen") && !shot.name().equals("inventory")) mc.gui.setScreen(null);
                if (shot.name().equals("inventory")) mc.gui.setScreen(null);
                step++;
                ticks = 0;
            }
        } catch (Throwable e) {
            e.printStackTrace();
            mc.stop();
        }
    }

    /** The camera round the player: from behind, the left or right side, the front, or a three-quarter view from behind. */
    private static void look(String view) {
        double r = 1.7;
        double[] d = switch (view) {
            case "back" -> new double[] {0, -r};
            case "front" -> new double[] {0, r};
            case "left" -> new double[] {r, 0};     // the player faces +z (yaw 0): his left is +x
            case "right" -> new double[] {-r, 0};
            case "back_left" -> new double[] {r * 0.7, -r * 0.7};
            default -> new double[] {-r * 0.7, -r * 0.7}; // back_right
        };
        Vec3 target = body.add(0, -0.15, 0);
        view(body.add(d[0], 0.25, d[1]), target);
    }

    private static void view(Vec3 at, Vec3 target) {
        Minecraft mc = Minecraft.getInstance();
        if (camera == null) {
            camera = new ArmorStand(EntityTypes.ARMOR_STAND, mc.level);
            camera.setInvisible(true);
        }
        Vec3 d = target.subtract(at);
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z)), pitch = (float) -Math.toDegrees(Math.atan2(d.y, d.horizontalDistance()));
        camera.setPos(at.x, at.y - camera.getEyeHeight(), at.z);
        camera.setYRot(yaw);
        camera.setXRot(pitch);
        camera.setYHeadRot(yaw);
        camera.setOldPosAndRot();
        mc.setCameraEntity(camera);
        mc.options.setCameraType(CameraType.FIRST_PERSON);
    }

    private GearShots() {}
}
