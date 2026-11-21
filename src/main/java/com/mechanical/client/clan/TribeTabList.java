package com.mechanical.client.clan;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.mechanical.Calendar;
import com.mechanical.clan.ClanNetwork;
import com.mechanical.clan.Clans;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.DisplaySlot;
import net.neoforged.fml.util.ObfuscationReflectionHelper;

/**
 * The Tab player list, grouped by tribe (docs/seasons-and-tribes.md): a framed panel with the server's name, the world's
 * age and the date; then each tribe under a banner bar in its colour (name, members online of all, age), its online
 * members by rank (★ Leader, ✦ Elder, • Member, ◦ Newcomer) with face, name, age and ping; your tribe first, then the
 * bigger ones; players without a tribe last as the Wanderers. Long lists flow into balanced columns (a tribe split over
 * two columns repeats its bar) and the panel shrinks to fit the screen. Built from the server's
 * {@link ClanNetwork.Roster} plus the client's player info (faces, names, ping). Wraps vanilla's layer
 * (MechanicalClient) and leaves the list to vanilla until a roster has come, or when the server shows a scoreboard
 * objective in the list (scores and hearts are vanilla's). Unlike vanilla it also shows in singleplayer alone.
 */
public final class TribeTabList {
    /** The latest roster from the server (ClanClient); null on a server without it. */
    static ClanNetwork.Roster roster;

    private static final int PANEL = 0xE8302418, BORDER = 0xFF8A6A44, CORNER = 0xFFC49A62, SHINE = 0x30FFE0B0, POLE = 0xFF6B4A2A;
    private static final int TITLE = 0xFFF0D8A8, TEXT = 0xFFE8E0D0, DIM = 0xFF9A8A70, WANDER = 0xFFC4B8A4, BANNER_TEXT = 0xFFFFF4E0;
    private static final int ROW = 10, HEAD = 12, GAP = 4, PAD = 6, COL_GAP = 8, TOP = 6, MAX_PLAYERS = 80;
    private static final String[] RANK_GLYPHS = {"★", "✦", "•", "◦"};
    private static final int[] RANK_COLOURS = {0xFFFFD34A, 0xFFBFD4E8, 0xFFB0A088, 0xFF8FBF6A};
    private static final int[] AGE_COLOURS = {0xFF9A968C, 0xFFC8843E, 0xFFB4C0CA, 0xFF8A6E58, 0xFF6AAEE0};
    private static final Identifier[] PING = {Identifier.withDefaultNamespace("icon/ping_unknown"), Identifier.withDefaultNamespace("icon/ping_1"),
            Identifier.withDefaultNamespace("icon/ping_2"), Identifier.withDefaultNamespace("icon/ping_3"),
            Identifier.withDefaultNamespace("icon/ping_4"), Identifier.withDefaultNamespace("icon/ping_5")};
    // shortcut: vanilla's header/footer have no getters; read by reflection, shown without them if that ever fails
    private static final Field HEADER = field("header"), FOOTER = field("footer");

    /** A group on the list: a tribe, or the Wanderers ({@code tribe} null). */
    private record Group(ClanNetwork.Group tribe, List<Entry> members) {}

    /** {@code rank} -1 for a Wanderer, {@code age} -1 when unknown. */
    private record Entry(PlayerInfo info, Component name, int rank, int age) {}

    /** A line in a column: a banner bar ({@code entry} null) or a member ({@code index} in the group, for the shading). */
    private record Line(Group group, Entry entry, int index) {
        int height() {
            return entry == null ? HEAD : ROW;
        }
    }

    /** Draw the list if we have a roster and the Tab key is down; false leaves it to vanilla. */
    public static boolean render(GuiGraphicsExtractor g) {
        Minecraft mc = Minecraft.getInstance();
        if (roster == null || mc.level == null || mc.player == null || mc.getConnection() == null || !mc.options.keyPlayerList.isDown()
                || mc.level.getScoreboard().getDisplayObjective(DisplaySlot.LIST) != null) return false;
        PlayerTabOverlay tab = mc.gui.hud.getTabList();
        tab.setVisible(true);
        g.nextStratum();
        draw(g, mc, tab, groups(mc, tab, roster));
        return true;
    }

    private static List<Group> groups(Minecraft mc, PlayerTabOverlay tab, ClanNetwork.Roster roster) {
        Map<UUID, PlayerInfo> byId = new HashMap<>();
        List<PlayerInfo> infos = mc.player.connection.getListedOnlinePlayers().stream()
                .sorted(Comparator.comparing(p -> p.getProfile().name(), String::compareToIgnoreCase)).limit(MAX_PLAYERS).toList();
        for (PlayerInfo info : infos) byId.put(info.getProfile().id(), info);
        Set<UUID> placed = new HashSet<>();
        List<Group> groups = new ArrayList<>();
        for (ClanNetwork.Group t : roster.tribes()) {
            List<Entry> members = new ArrayList<>();
            for (ClanNetwork.Row r : t.members()) {
                PlayerInfo info = byId.get(r.id());
                if (info != null && placed.add(r.id())) members.add(new Entry(info, tab.getNameForDisplay(info), r.rank(), t.age()));
            }
            if (!members.isEmpty()) groups.add(new Group(t, members));
        }
        UUID me = mc.player.getUUID();
        groups.sort(Comparator.<Group>comparingInt(gr -> gr.members.stream().anyMatch(e -> e.info.getProfile().id().equals(me)) ? 0 : 1)
                .thenComparingInt(gr -> -gr.members.size()).thenComparing(gr -> gr.tribe.name(), String::compareToIgnoreCase));
        List<Entry> wanderers = new ArrayList<>();
        for (PlayerInfo info : infos) {
            UUID id = info.getProfile().id();
            if (!placed.contains(id)) wanderers.add(new Entry(info, tab.getNameForDisplay(info), -1, roster.solo().getOrDefault(id, -1)));
        }
        if (!wanderers.isEmpty()) groups.add(new Group(null, wanderers));
        return groups;
    }

    private static void draw(GuiGraphicsExtractor g, Minecraft mc, PlayerTabOverlay tab, List<Group> groups) {
        Font font = mc.font;
        int screenW = g.guiWidth(), screenH = g.guiHeight();
        boolean heads = mc.getConnection().onlineMode();
        int players = 0;
        for (Group gr : groups) players += gr.members.size();

        // column width: the widest row or banner bar
        int tagW = 0, nameW = 0, barW = 0;
        for (int a = 0; a < Clans.AGES.length; a++) tagW = Math.max(tagW, font.width(ageShort(a)) + 6);
        for (Group gr : groups) {
            barW = Math.max(barW, 14 + font.width(groupName(gr)) + 6 + font.width(count(gr)) + 4);
            for (Entry e : gr.members) nameW = Math.max(nameW, font.width(e.name));
        }
        int right = 4 + tagW + 3 + 10 + 2;
        int colW = Math.max((heads ? 14 : 4) + 9 + nameW + 6, barW) + right;

        // the chrome around the columns
        Component title = title(mc);
        Component date = Calendar.describe(mc.level.getOverworldClockTime());
        Component worldAge = ageLong(roster.worldAge());
        int worldTagW = font.width(worldAge) + 6;
        Component legend = legend(players);
        List<FormattedCharSequence> header = lines(font, read(HEADER, tab), screenW - 50), footer = lines(font, read(FOOTER, tab), screenW - 50);
        int chromeTop = PAD + 10 + 10 + 5 + (header.isEmpty() ? 0 : header.size() * 9 + 3);
        int chromeBottom = 4 + (footer.isEmpty() ? 0 : footer.size() * 9 + 3) + 5 + 10 + PAD - 1;
        int maxH = Math.max(HEAD + 3 * ROW, screenH - 2 * TOP - chromeTop - chromeBottom);

        List<List<Line>> cols = columns(groups, maxH);
        int colsH = 0;
        for (List<Line> col : cols) colsH = Math.max(colsH, height(col));
        int inner = cols.size() * colW + (cols.size() - 1) * COL_GAP;
        inner = Math.max(inner, Math.max(font.width(title), worldTagW + 6 + font.width(date)));
        inner = Math.max(inner, font.width(legend));
        for (FormattedCharSequence l : header) inner = Math.max(inner, font.width(l));
        for (FormattedCharSequence l : footer) inner = Math.max(inner, font.width(l));
        int panelW = inner + 2 * PAD, panelH = chromeTop + colsH + chromeBottom;
        float scale = Math.min(1F, Math.min((screenW - 8F) / panelW, (screenH - 2F * TOP) / panelH));

        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(screenW / 2F, TOP);
        pose.scale(scale, scale);
        int left = -panelW / 2, x = left + PAD, mid = left + panelW / 2;
        frame(g, left, 0, left + panelW, panelH);

        int y = PAD;
        g.text(font, title, mid - font.width(title) / 2, y, TITLE, true);
        y += 10;
        int lineX = mid - (worldTagW + 6 + font.width(date)) / 2;
        tag(g, font, worldAge, lineX, y, worldTagW, roster.worldAge());
        g.text(font, date, lineX + worldTagW + 6, y + 1, DIM, false);
        y += 10 + 2;
        g.fill(x, y, x + inner, y + 1, BORDER);
        y += 3;
        for (FormattedCharSequence l : header) {
            g.text(font, l, mid - font.width(l) / 2, y, -1, true);
            y += 9;
        }
        if (!header.isEmpty()) y += 3;

        int colX = mid - (cols.size() * colW + (cols.size() - 1) * COL_GAP) / 2;
        UUID me = mc.player.getUUID();
        for (List<Line> col : cols) {
            int ly = y;
            for (int i = 0; i < col.size(); i++) {
                Line line = col.get(i);
                if (line.entry == null && i > 0) ly += GAP;
                if (line.entry == null) bar(g, font, line.group, colX, ly, colW, tagW);
                else row(g, mc, font, line, colX, ly, colW, tagW, heads, line.entry.info.getProfile().id().equals(me));
                ly += line.height();
            }
            colX += colW + COL_GAP;
        }
        y += colsH + 4;

        if (!footer.isEmpty()) {
            y += 3;
            for (FormattedCharSequence l : footer) {
                g.text(font, l, mid - font.width(l) / 2, y, -1, true);
                y += 9;
            }
        }
        g.fill(x, y, x + inner, y + 1, BORDER);
        y += 4;
        g.text(font, legend, mid - font.width(legend) / 2, y, DIM, false);
        pose.popMatrix();
    }

    /** Pack the lines into as few balanced columns as fit {@code maxH}; a tribe going on in the next column repeats its bar. */
    private static List<List<Line>> columns(List<Group> groups, int maxH) {
        int total = 0;
        for (Group gr : groups) total += HEAD + gr.members.size() * ROW + GAP;
        List<List<Line>> cols = List.of();
        for (int n = 1; n <= 16; n++) {
            int limit = Math.min(maxH, total / n + HEAD + ROW);
            cols = pack(groups, limit);
            if (cols.size() <= n || limit == maxH) break;
        }
        return cols;
    }

    private static List<List<Line>> pack(List<Group> groups, int limit) {
        List<List<Line>> cols = new ArrayList<>();
        List<Line> col = new ArrayList<>();
        cols.add(col);
        int h = 0;
        for (Group gr : groups) {
            // a bar only where its first member fits under it
            int need = (col.isEmpty() ? 0 : GAP) + HEAD + ROW;
            if (!col.isEmpty() && h + need > limit) {
                col = new ArrayList<>();
                cols.add(col);
                h = 0;
            }
            h += (col.isEmpty() ? 0 : GAP) + HEAD;
            col.add(new Line(gr, null, -1));
            for (int i = 0; i < gr.members.size(); i++) {
                if (h + ROW > limit && col.size() > 1) {
                    col = new ArrayList<>();
                    cols.add(col);
                    col.add(new Line(gr, null, -1));
                    h = HEAD;
                }
                col.add(new Line(gr, gr.members.get(i), i));
                h += ROW;
            }
        }
        return cols;
    }

    private static int height(List<Line> col) {
        int h = 0;
        for (int i = 0; i < col.size(); i++) h += col.get(i).height() + (col.get(i).entry == null && i > 0 ? GAP : 0);
        return h;
    }

    /** The panel: a bark-brown border with brighter corner pegs and a faint inner shine, like the tribe page. */
    private static void frame(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1) {
        g.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, BORDER);
        g.fill(x0, y0, x1, y1, PANEL);
        g.outline(x0 + 1, y0 + 1, x1 - x0 - 2, y1 - y0 - 2, SHINE);
        for (int[] c : new int[][] {{x0 - 1, y0 - 1}, {x1 - 2, y0 - 1}, {x0 - 1, y1 - 2}, {x1 - 2, y1 - 2}}) g.fill(c[0], c[1], c[0] + 3, c[1] + 3, CORNER);
    }

    /** A tribe's banner bar: a pennant in its colour on a pole, the name, members online of all, and its age. */
    private static void bar(GuiGraphicsExtractor g, Font font, Group gr, int x, int y, int w, int tagW) {
        int tagX = x + w - 2 - 10 - 3 - tagW;
        Component count = count(gr);
        if (gr.tribe == null) {
            g.fill(x, y, x + w, y + HEAD, 0xB0403830);
            g.fill(x, y + HEAD - 1, x + w, y + HEAD, 0x60000000);
            g.text(font, groupName(gr), x + 5, y + 2, WANDER, true);
        } else {
            int dye = dye(gr.tribe.colour());
            g.fill(x, y, x + w, y + HEAD, 0xD0000000 | mix(dye, 0x201810, 0.55F));
            g.fill(x, y, x + w, y + 1, 0x70000000 | dye & 0xFFFFFF);
            g.fill(x, y + HEAD - 1, x + w, y + HEAD, 0x60000000);
            pennant(g, x + 2, y, dye);
            g.text(font, groupName(gr), x + 14, y + 2, BANNER_TEXT, true);
            tag(g, font, ageShort(gr.tribe.age()), tagX, y + 1, tagW, gr.tribe.age());
        }
        g.text(font, count, tagX - 4 - font.width(count), y + 2, 0xFFE0D0B8, true);
    }

    /** A pennant on a pole, 9 by 12 from (x, y): used on the Tab list and the tribe page. */
    static void pennant(GuiGraphicsExtractor g, int x, int y, int dye) {
        g.fill(x, y, x + 1, y + 12, POLE);
        g.fill(x + 1, y + 1, x + 9, y + 9, dye);
        g.fill(x + 1, y + 9, x + 4, y + 11, dye);
        g.fill(x + 6, y + 9, x + 9, y + 11, dye);
        g.fill(x + 1, y + 1, x + 9, y + 2, 0x40FFFFFF);
    }

    private static void row(GuiGraphicsExtractor g, Minecraft mc, Font font, Line line, int x, int y, int w, int tagW, boolean heads, boolean mine) {
        Entry e = line.entry;
        boolean wanderer = line.group.tribe == null;
        g.fill(x, y, x + w, y + ROW, mine ? 0x34FFE8A0 : line.index % 2 == 0 ? 0x22FFFFFF : 0x0CFFFFFF);
        g.fill(x + 2, y, x + 3, y + ROW, wanderer ? 0x60FFFFFF & DIM : 0xA0000000 | dye(line.group.tribe.colour()) & 0xFFFFFF);
        int cx = x + 5;
        if (heads) {
            Player player = mc.level.getPlayerByUUID(e.info.getProfile().id());
            boolean flip = player != null && AvatarRenderer.isPlayerUpsideDown(player);
            PlayerFaceExtractor.extractRenderState(g, e.info.getSkin().body().texturePath(), cx, y + 1, 8, e.info.showHat(), flip, -1);
            cx += 10;
        }
        if (e.rank >= 0) g.text(font, RANK_GLYPHS[e.rank], cx + 4 - font.width(RANK_GLYPHS[e.rank]) / 2, y + 1, RANK_COLOURS[e.rank], true);
        cx += 9;
        boolean spectator = e.info.getGameMode() == GameType.SPECTATOR;
        g.text(font, e.name, cx, y + 1, spectator ? 0x90FFFFFF : wanderer ? WANDER : TEXT, true);
        int pingX = x + w - 2 - 10;
        if (e.age >= 0) tag(g, font, ageShort(e.age), pingX - 3 - tagW, y, tagW, e.age);
        int ms = e.info.getLatency();
        g.blitSprite(RenderPipelines.GUI_TEXTURED, PING[ms < 0 ? 0 : ms < 150 ? 5 : ms < 300 ? 4 : ms < 600 ? 3 : ms < 1000 ? 2 : 1], pingX, y + 1, 10, 8);
    }

    /** A small rounded tag in the age's colour. */
    private static void tag(GuiGraphicsExtractor g, Font font, Component text, int x, int y, int w, int age) {
        int c = AGE_COLOURS[Math.clamp(age, 0, AGE_COLOURS.length - 1)];
        g.fill(x + 1, y, x + w - 1, y + 9, c);
        g.fill(x, y + 1, x + w, y + 8, c);
        g.fill(x + 1, y + 8, x + w - 1, y + 9, mix(c, 0x000000, 0.7F) | 0xFF000000);
        g.text(font, text, x + (w - font.width(text) + 1) / 2, y + 1, 0xFF1E1610, false);
    }

    private static Component groupName(Group gr) {
        return gr.tribe == null ? Component.translatable("gui.mechanical.tab.wanderers") : Component.literal(gr.tribe.name());
    }

    /** "3/5" members online of all for a tribe, the head count for the Wanderers. */
    private static Component count(Group gr) {
        return Component.literal(gr.tribe == null ? String.valueOf(gr.members.size()) : gr.members.size() + "/" + gr.tribe.members().size());
    }

    private static Component ageShort(int age) {
        return Component.translatable("gui.mechanical.tab.age." + Clans.AGES[Math.clamp(age, 0, Clans.AGES.length - 1)]);
    }

    private static Component ageLong(int age) {
        return Component.translatable("gui.mechanical.clan.age." + Clans.AGES[Math.clamp(age, 0, Clans.AGES.length - 1)]);
    }

    private static Component title(Minecraft mc) {
        ServerData server = mc.getCurrentServer();
        if (server != null && !server.name.isBlank()) return Component.literal(server.name);
        if (mc.getSingleplayerServer() != null) return Component.literal(mc.getSingleplayerServer().getWorldData().getLevelName());
        return Component.translatable("gui.mechanical.tab.title");
    }

    /** "★ Leader ✦ Elder • Member ◦ Newcomer · 4 online". */
    private static Component legend(int players) {
        MutableComponent out = Component.empty();
        for (int r = 0; r < RANK_GLYPHS.length; r++) {
            out.append(Component.literal(RANK_GLYPHS[r] + " ").withColor(RANK_COLOURS[r] & 0xFFFFFF));
            out.append(Component.translatable("gui.mechanical.clan.rank." + Clans.RANKS[r]).withColor(DIM & 0xFFFFFF)).append("  ");
        }
        return out.append(Component.literal("· ").withColor(DIM & 0xFFFFFF))
                .append(Component.translatable("gui.mechanical.tab.online", players).withColor(TEXT & 0xFFFFFF));
    }

    static int dye(int colour) {
        return DyeColor.byId(Math.clamp(colour, 0, 15)).getTextureDiffuseColor();
    }

    /** {@code a} blended toward {@code b} by {@code t} (0 = a, 1 = b), RGB only. */
    private static int mix(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int gr = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return r << 16 | gr << 8 | bl;
    }

    private static List<FormattedCharSequence> lines(Font font, Component text, int width) {
        return text == null ? List.of() : font.split(text, width);
    }

    private static Field field(String name) {
        try {
            return ObfuscationReflectionHelper.findField(PlayerTabOverlay.class, name);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static Component read(Field f, PlayerTabOverlay tab) {
        try {
            return f == null ? null : (Component) f.get(tab);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    private TribeTabList() {}
}
