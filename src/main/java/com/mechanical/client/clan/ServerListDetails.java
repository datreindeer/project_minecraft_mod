package com.mechanical.client.clan;

import com.mechanical.Calendar;
import com.mechanical.Mechanical;
import com.mechanical.clan.ServerListInfo;
import com.mechanical.client.CalendarHud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;

/**
 * Servers on the Multiplayer screen (docs/seasons-and-tribes.md). One running the mod becomes a single card in the Tab
 * list's bark style (TribeTabList): vanilla's icon, name, MOTD, players and ping sit inside the frame, inset by INSET
 * (mixin.OnlineServerEntryMixin moves the entry's content box, so drawing and clicks move together), the title is the
 * server's own name when it sends one, and under a divider: the world's age with its icon and the server's date in the
 * season's colour, the number of tribes, then a chip per tribe (pennant in its colour, name, online/all, age icon), the
 * Wanderers and "+N more". The data comes with the server's status (ServerListInfo). A server that answered without it
 * keeps vanilla's row, dimmed and marked "Not running <mod>"; one that has not answered (or is offline) stays vanilla.
 */
public final class ServerListDetails {
    /** The card's frame to content; the body starts under vanilla's 32 px icon; chips are CHIP high on a ROW pitch, two columns GAP apart. */
    static final int INSET = 5, TOP = 34, LINE = 12, LABEL = 11, ROW = 14, CHIP = 13, GAP = 4;
    private static final int NOT_MOD = 0xFFB07060;
    private static String modName;

    /** The entry height for this server: a taller card, else vanilla's. */
    public static int height(ServerData data, int vanilla) {
        ServerListInfo info = info(data);
        return info == null ? vanilla : 4 + 2 * INSET + TOP + body(info);
    }

    /** How far the entry's content sits inside its frame (0 for vanilla rows). */
    public static int inset(ServerData data) {
        return info(data) == null ? 0 : INSET;
    }

    /** The entry's title: the server's own name if it sends one, else the one typed in when adding it. */
    public static String title(ServerData data, String typed) {
        ServerListInfo info = info(data);
        return info == null || info.name().isBlank() ? typed : info.name();
    }

    private static ServerListInfo info(ServerData data) {
        return data.state() == ServerData.State.SUCCESSFUL ? ServerListInfo.of(data.players) : null;
    }

    private static int chips(ServerListInfo info) {
        return info.shown().size() + (info.wanderers() > 0 ? 1 : 0) + (info.tribes() > info.shown().size() ? 1 : 0);
    }

    /** The divider, the age and date line, the tribe count and the chip rows. */
    private static int body(ServerListInfo info) {
        int rows = (chips(info) + 1) / 2;
        return 4 + LINE + (rows == 0 ? 9 : LABEL + rows * ROW - 1);
    }

    /** Before vanilla's content: the card's frame and body (nothing for other servers). */
    public static void drawCard(GuiGraphicsExtractor g, ServerSelectionList.OnlineServerEntry entry) {
        ServerListInfo info = info(entry.getServerData());
        if (info == null) return;
        Font font = Minecraft.getInstance().font;
        TribeTabList.frame(g, entry.getX() + 2, entry.getY() + 2, entry.getX() + entry.getWidth() - 2, entry.getY() + entry.getHeight() - 2);
        int x = entry.getContentX(), inner = entry.getContentWidth(), y = entry.getContentY() + TOP;
        g.fill(x, y, x + inner, y + 1, TribeTabList.BORDER);
        y += 4;

        // the world's age on the left, the date on the right
        int age = info.age();
        TribeTabList.ageIcon(g, x, y, age);
        g.text(font, TribeTabList.ageLong(age), x + TribeTabList.ICON + 3, y + 1,
                TribeTabList.AGE_COLOURS[Math.clamp(age, 0, TribeTabList.AGE_COLOURS.length - 1)], true);
        Component date = Calendar.describe(info.clock());
        g.text(font, date, x + inner - font.width(date), y + 1, CalendarHud.SEASON_COLOURS[Calendar.season(info.clock())], true);
        y += LINE;

        // "3 tribes" on a divider
        int n = info.tribes();
        Component label = n == 0 ? Component.translatable("gui.mechanical.servers.no_tribes")
                : Component.translatable(n == 1 ? "gui.mechanical.servers.tribe" : "gui.mechanical.servers.tribes", n);
        int mid = x + inner / 2, half = font.width(label) / 2;
        g.fill(x, y + 4, mid - half - 4, y + 5, TribeTabList.BORDER);
        g.fill(mid + half + 4, y + 4, x + inner, y + 5, TribeTabList.BORDER);
        g.text(font, label, mid - half, y, TribeTabList.DIM, false);
        y += LABEL;

        int colW = (inner - GAP) / 2, i = 0;
        for (ServerListInfo.Tribe t : info.shown()) tribe(g, font, t, x + i % 2 * (colW + GAP), y + i++ / 2 * ROW, colW);
        if (info.wanderers() > 0) {
            int cx = x + i % 2 * (colW + GAP), cy = y + i++ / 2 * ROW;
            g.fill(cx, cy, cx + colW, cy + CHIP, 0xB0403830);
            g.fill(cx, cy + CHIP - 1, cx + colW, cy + CHIP, 0x60000000);
            g.text(font, Component.translatable("gui.mechanical.tab.wanderers"), cx + 5, cy + 3, TribeTabList.WANDER, true);
            String count = String.valueOf(info.wanderers());
            g.text(font, count, cx + colW - 4 - font.width(count), cy + 3, 0xFFE0D0B8, true);
        }
        if (n > info.shown().size()) {
            int cx = x + i % 2 * (colW + GAP), cy = y + i / 2 * ROW;
            Component more = Component.translatable("gui.mechanical.servers.more", n - info.shown().size());
            g.fill(cx, cy, cx + colW, cy + CHIP, 0x40000000);
            g.text(font, more, cx + (colW - font.width(more)) / 2, cy + 3, TribeTabList.DIM, false);
        }
    }

    /** After vanilla's content: a server that answered without the mod is dimmed and marked at the bottom right. */
    public static void markVanilla(GuiGraphicsExtractor g, ServerSelectionList.OnlineServerEntry entry) {
        ServerData data = entry.getServerData();
        if (data.state() != ServerData.State.SUCCESSFUL || info(data) != null) return;
        Font font = Minecraft.getInstance().font;
        g.fill(entry.getContentX(), entry.getContentY(), entry.getContentRight(), entry.getContentBottom(), 0x80000000);
        if (modName == null) modName = ModList.get().getModContainerById(Mechanical.MODID).map(c -> c.getModInfo().getDisplayName()).orElse(Mechanical.MODID);
        Component label = Component.translatable("gui.mechanical.servers.not_modded", modName);
        int w = font.width(label), lx = entry.getContentRight() - 2 - w, ly = entry.getContentBottom() - 9;
        g.fill(lx - 3, ly - 2, entry.getContentRight(), entry.getContentBottom(), 0xC0201010);
        g.text(font, label, lx, ly, NOT_MOD, false);
    }

    /** A tribe's chip: like its banner bar on the Tab list, with members online of all and the age icon on the right. */
    private static void tribe(GuiGraphicsExtractor g, Font font, ServerListInfo.Tribe t, int x, int y, int w) {
        int dye = TribeTabList.dye(t.colour());
        g.fill(x, y, x + w, y + CHIP, 0xD8000000 | TribeTabList.mix(dye, 0x201810, 0.5F));
        g.fill(x, y, x + w, y + 1, 0x90000000 | dye & 0xFFFFFF);
        g.fill(x, y + CHIP - 1, x + w, y + CHIP, 0x60000000);
        TribeTabList.pennant(g, x + 2, y, dye);
        int ageX = x + w - 3 - TribeTabList.ICON;
        TribeTabList.ageIcon(g, ageX, y + (CHIP - TribeTabList.ICON) / 2, t.age());
        String count = t.online() + "/" + t.members();
        int countX = ageX - 4 - font.width(count);
        g.text(font, count, countX, y + 3, t.online() > 0 ? 0xFFE0D0B8 : TribeTabList.DIM, true);
        g.text(font, font.plainSubstrByWidth(t.name(), countX - 4 - (x + 14)), x + 14, y + 3, TribeTabList.BANNER_TEXT, true);
    }

    private ServerListDetails() {}
}
