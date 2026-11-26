package com.mechanical.people;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.mechanical.Mechanical;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Dev only (like LogShots): with a file {@code oakshots.flag} in the game directory (its first line: an output folder),
 * the client opens the world "OakShots", finds the nearest Ancient Oak, goes there (its altar settles the Oak Folk) and
 * takes screenshots (oak_*.png): the group at the oak, the keeper, a praying worshipper, the light-bearers at night and
 * the keeper's quest screen; then it quits. Does nothing without the flag file.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class OakShots {
    private static int stage = -1, ticks;
    private static File out;
    private static final List<String> result = new ArrayList<>();
    private static BlockPos oak;
    /** Second line of the flag "quest": only the quest screen, in each of its states (oak_quest_*.png). */
    private static boolean questOnly;
    private static final String[] QUEST_SHOTS = {"current", "focus_done", "first", "two_rewards", "finished", "closed"};

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (stage == -1) {
                File flag = new File(mc.gameDirectory, "oakshots.flag");
                if (!flag.exists()) {
                    stage = -2;
                    return;
                }
                List<String> lines = Files.readAllLines(flag.toPath());
                out = new File(lines.get(0).trim());
                questOnly = lines.size() > 1 && lines.get(1).trim().equals("quest");
                out.mkdirs();
                stage = 0;
            }
            if (stage < 0) return;
            if (stage == 0) {
                if (mc.gui.screen() instanceof TitleScreen) {
                    mc.createWorldOpenFlows().openWorld("OakShots", () -> {});
                    stage = 1;
                    ticks = 0;
                }
                return;
            }
            if (mc.player == null || mc.getSingleplayerServer() == null) return;
            ticks++;
            var server = mc.getSingleplayerServer();
            switch (stage) {
                case 1 -> {
                    mc.options.pauseOnLostFocus = false;
                    if (mc.player.isDeadOrDying()) mc.player.respawn();
                    if (mc.gui.screen() != null) mc.gui.setScreen(null);
                    if (ticks < 100) return;
                    mc.options.setCameraType(CameraType.FIRST_PERSON);
                    server.execute(() -> {
                        cmd("gamemode spectator @a");
                        cmd("time set 5000");
                        cmd("weather clear");
                        cmd("gamerule advance_time false");
                        ServerLevel level = player().level();
                        var structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE)
                                .getOrThrow(ResourceKey.create(Registries.STRUCTURE, Identifier.fromNamespaceAndPath(Mechanical.MODID, "ancient_oak")));
                        var found = level.getChunkSource().getGenerator().findNearestMapStructure(level, HolderSet.direct(structure), player().blockPosition(), 200, false);
                        if (found == null) throw new IllegalStateException("no Ancient Oak found");
                        oak = found.getFirst();
                        result.add("oak structure at " + oak);
                        look(Vec3.atCenterOf(oak).add(0, 60, 0), 0, 90);
                    });
                    next();
                    if (questOnly) stage = 20;
                }
                case 2 -> { // the oak's chunks generate, the altar ticks and settles its people
                    if (ticks < 300) return;
                    server.execute(() -> {
                        List<Tribesperson> folk = folk();
                        result.add("oak folk: " + folk.size());
                        for (Tribesperson t : folk) result.add("  " + t.role() + " " + t.tribeName() + " at " + t.blockPosition() + " oak " + t.oakFoot + " idle " + t.idle());
                        if (!folk.isEmpty()) oak = folk.get(0).oakFoot;
                        stageGroup(folk);
                    });
                    next();
                }
                case 3 -> {
                    if (ticks < 80) return;
                    shot(mc, "group");
                    server.execute(() -> front(role(TribeRole.OAK_KEEPER), 3.0, 0.4));
                    next();
                }
                case 4 -> {
                    if (ticks < 40) return;
                    shot(mc, "keeper");
                    server.execute(() -> front(role(TribeRole.OAK_WORSHIPPER), 2.6, 0.6));
                    next();
                }
                case 5 -> {
                    if (ticks < 40) return;
                    shot(mc, "worshipper");
                    server.execute(() -> front(role(TribeRole.OAK_GATHERER), 2.8, 0.5));
                    next();
                }
                case 6 -> {
                    if (ticks < 40) return;
                    shot(mc, "gatherer");
                    server.execute(() -> {
                        cmd("time set 18000");
                        front(role(TribeRole.OAK_LIGHTBEARER), 3.0, 0.4);
                    });
                    next();
                }
                case 7 -> {
                    if (ticks < 60) return;
                    shot(mc, "lightbearer_night");
                    server.execute(() -> {
                        cmd("time set 5000");
                        folk().forEach(t -> t.setNoAi(false));
                        front(role(TribeRole.OAK_KEEPER), 3.0, 0.2);
                    });
                    next();
                }
                case 8 -> {
                    if (ticks < 40) return;
                    Tribesperson keeper = mc.level.getEntitiesOfClass(Tribesperson.class, mc.player.getBoundingBox().inflate(32), t -> t.role() == TribeRole.OAK_KEEPER)
                            .stream().findFirst().orElse(null);
                    if (keeper != null)
                        mc.gui.setScreen(new com.mechanical.client.people.OakQuestScreen(new OakQuests.Open(keeper.getId(), keeper.getDisplayName(), 6, 1, false, 34)));
                    next();
                }
                case 9 -> {
                    if (ticks < 20) return;
                    shot(mc, "quest_gui");
                    mc.gui.setScreen(null);
                    next();
                }
                case 10 -> {
                    if (ticks < 20) return;
                    Files.write(new File(out, "oak_result.txt").toPath(), result);
                    stage = -2;
                    mc.stop();
                }
                default -> { // quest screen states: 20 + 2n opens one, 21 + 2n photographs it
                    int n = (stage - 20) / 2;
                    if (n >= QUEST_SHOTS.length) {
                        stage = 10;
                        return;
                    }
                    if (ticks < 40) return;
                    if ((stage - 20) % 2 == 0) {
                        Component name = Component.literal("Ealdwyn, Keeper of the Oak");
                        int id = mc.player.getId();
                        mc.gui.setScreen(new com.mechanical.client.people.OakQuestScreen(switch (QUEST_SHOTS[n]) {
                            case "first" -> new OakQuests.Open(id, name, 0, 5, false, 6);
                            case "two_rewards" -> new OakQuests.Open(id, name, 7, 2, false, 41);
                            case "finished" -> new OakQuests.Open(id, name, 12, 0, false, 80);
                            case "closed" -> new OakQuests.Open(id, name, 4, 0, true, -45);
                            default -> new OakQuests.Open(id, name, 6, 1, false, 34);
                        }));
                        if (QUEST_SHOTS[n].equals("focus_done")) { // click the third token (the screen's layout: 12 tokens 18 wide, 4 apart)
                            int left = (mc.getWindow().getGuiScaledWidth() - 340) / 2 + 40, top = Math.max(2, (mc.getWindow().getGuiScaledHeight() - 208) / 2);
                            mc.gui.screen().mouseClicked(new net.minecraft.client.input.MouseButtonEvent(left + 2 * 22 + 9, top + 158 + 9,
                                    new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0)), false);
                        }
                    } else {
                        shot(mc, "quest_" + QUEST_SHOTS[n]);
                    }
                    next();
                }
            }
        } catch (Exception e) {
            result.add("error: " + e);
            try {
                Files.write(new File(out, "oak_result.txt").toPath(), result);
            } catch (Exception ignored) {}
            stage = -2;
            mc.stop();
        }
    }

    private static void next() {
        stage++;
        ticks = 0;
    }

    private static ServerPlayer player() {
        return Minecraft.getInstance().getSingleplayerServer().getPlayerList().getPlayers().get(0);
    }

    private static List<Tribesperson> folk() {
        return player().level().getEntitiesOfClass(Tribesperson.class, new AABB(oak).inflate(48, 320, 48), t -> t.role().oak());
    }

    private static Tribesperson role(TribeRole r) {
        return folk().stream().filter(t -> t.role() == r).min(Comparator.comparingInt(Tribesperson::getId)).orElse(null);
    }

    /**
     * For the group picture (staged): the oak's people in a half ring just outside its door, each in their pose (keeper
     * raising the staff, worshippers praying, light-bearers with candles, gatherers tending), facing the camera 24 out.
     */
    private static void stageGroup(List<Tribesperson> folk) {
        Tribesperson keeper = role(TribeRole.OAK_KEEPER);
        if (keeper == null) return;
        Vec3 c = Vec3.atBottomCenterOf(oak);
        Vec3 dir = new Vec3(keeper.getX() - c.x, 0, keeper.getZ() - c.z).normalize(), side = new Vec3(-dir.z, 0, dir.x);
        Vec3 eye = c.add(dir.scale(22)).add(0, 4, 0);
        for (int i = 0; i < folk.size(); i++) {
            Tribesperson t = folk.get(i);
            double off = (i - (folk.size() - 1) / 2.0) * 2.6, out = 14 + Math.abs(off) * 0.25 + (t == keeper ? -1.5 : 0);
            Vec3 at = c.add(dir.scale(out)).add(side.scale(off));
            BlockPos g = OakFolk.ground(t.level(), BlockPos.containing(at.x, oak.getY(), at.z));
            for (BlockPos q : BlockPos.betweenClosed(g.offset(-1, 0, -1), g.offset(1, 1, 1))) // tall grass would hide them
                if (t.level().getBlockState(q).canBeReplaced() && t.level().getFluidState(q).isEmpty()) t.level().setBlockAndUpdate(q, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            float yaw = (float) (Math.atan2(eye.z - at.z, eye.x - at.x) * 180 / Math.PI) - 90;
            t.setNoAi(true);
            t.snapTo(at.x, g.getY(), at.z, yaw, 0);
            t.yBodyRot = t.yHeadRot = yaw;
            t.setIdle(switch (t.role()) {
                case OAK_KEEPER -> CampLife.STAFF;
                case OAK_WORSHIPPER -> CampLife.PRAY | CampLife.SITTING;
                case OAK_LIGHTBEARER -> CampLife.CANDLES;
                default -> CampLife.TEND;
            });
        }
        Vec3 target = c.add(dir.scale(14)).add(0, 1.5, 0), d = target.subtract(eye);
        look(eye.add(0, -player().getEyeHeight(), 0), (float) (Math.atan2(d.z, d.x) * 180 / Math.PI) - 90,
                (float) (-Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * 180 / Math.PI));
    }

    /** Stands {@code dist} in front of {@code t} (along its body's facing), {@code up} above its eyes, looking back at it. */
    private static void front(Tribesperson t, double dist, double up) {
        if (t == null) return;
        float yaw = t.yBodyRot * ((float) Math.PI / 180F);
        Vec3 target = t.position().add(0, t.idle() >= CampLife.SITTING ? 0.6 : 1.2, 0);
        Vec3 eye = target.add(-Math.sin(yaw) * dist, up, Math.cos(yaw) * dist), d = target.subtract(eye);
        look(eye.add(0, -player().getEyeHeight(), 0), (float) (Math.atan2(d.z, d.x) * 180 / Math.PI) - 90,
                (float) (-Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * 180 / Math.PI));
    }

    /** Stands {@code dist} away from {@code target}, on the side away from the trunk, {@code up} higher, looking at it. */
    private static void lookFrom(Vec3 target, double dist, double up) {
        Vec3 away = new Vec3(target.x - oak.getX() - 0.5, 0, target.z - oak.getZ() - 0.5).normalize();
        Vec3 eye = target.add(away.scale(dist)).add(0, up, 0);
        Vec3 d = target.subtract(eye);
        float yaw = (float) (Math.atan2(d.z, d.x) * 180 / Math.PI) - 90;
        float pitch = (float) (-Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * 180 / Math.PI);
        look(eye.add(0, -player().getEyeHeight(), 0), yaw, pitch);
    }

    private static void look(Vec3 at, float yaw, float pitch) {
        ServerPlayer p = player();
        p.teleportTo(p.level(), at.x, at.y, at.z, java.util.Set.of(), yaw, pitch, true);
    }

    private static void cmd(String c) {
        ServerPlayer p = player();
        p.level().getServer().getCommands().performPrefixedCommand(p.level().getServer().createCommandSourceStack().withSuppressedOutput(), c);
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
            try (image) {
                image.writeToFile(new File(out, "oak_" + name + ".png"));
            } catch (Exception e) {
                result.add("screenshot " + name + " failed: " + e);
            }
        });
    }

    private OakShots() {}
}
