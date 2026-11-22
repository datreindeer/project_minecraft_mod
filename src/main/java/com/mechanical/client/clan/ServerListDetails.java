package com.mechanical.client.clan;

import com.mechanical.Calendar;
import com.mechanical.clan.ServerListInfo;
import com.mechanical.client.CalendarHud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

/**
 * A server running the mod on the Multiplayer screen (docs/seasons-and-tribes.md): under vanilla's entry (icon, name, MOTD,
 * players, ping) a panel in the Tab list's style (TribeTabList): the world's age with its icon and the server's date in the
 * season's colour, the number of tribes, then a chip per tribe (pennant in its colour, name, online/all, age icon), the
 * Wanderers and "+N more". The data comes with the server's status (ServerListInfo); servers without it stay vanilla.
 */
public final class ServerListDetails {
    /** The panel starts under vanilla's 32 px icon; chips are CHIP high on a ROW pitch, two columns GAP apart. */
    static final int TOP = 35, PAD = 4, LINE = 12, LABEL = 11, ROW = 14, CHIP = 13, GAP = 4;

    /** The entry height for this server: taller with a panel, else vanilla's. */
    public static int height(ServerData data, int vanilla) {
        ServerListInfo info = info(data);
        return info == null ? vanilla : 4 + TOP + panelHeight(info) + 2;
    }

    private static ServerListInfo info(ServerData data) {
        return data.state() == ServerData.State.SUCCESSFUL ? ServerListInfo.of(data.players) : null;
    }

    private static int chips(ServerListInfo info) {
        return info.shown().size() + (info.wanderers() > 0 ? 1 : 0) + (info.tribes() > info.shown().size() ? 1 : 0);
    }

    private static int panelHeight(ServerListInfo info) {
        int rows = (chips(info) + 1) / 2;
        return 2 * PAD + LINE + (rows == 0 ? 9 : LABEL + rows * ROW - 1);
    }

    public static void draw(GuiGraphicsExtractor g, ServerSelectionList.OnlineServerEntry entry) {
        ServerListInfo info = info(entry.getServerData());
        if (info == null) return;
        Font font = Minecraft.getInstance().font;
        int x0 = entry.getContentX() + 1, x1 = entry.getContentRight() - 1, y0 = entry.getContentY() + TOP;
        TribeTabList.frame(g, x0, y0, x1, y0 + panelHeight(info));
        int x = x0 + PAD, inner = x1 - x0 - 2 * PAD, y = y0 + PAD;

        // the world's age on the left, the date on the right
        int age = info.age();
        TribeTabList.ageIcon(g, x, y, age);
        g.text(font, TribeTabList.ageLong(age), x + TribeTabList.ICON + 3, y + 1,
                TribeTabList.AGE_COLOURS[Math.clamp(age, 0, TribeTabList.AGE_COLOURS.length - 1)], true);
        Component date = Calendar.describe(info.clock());
        g.text(font, date, x + inner - font.width(date), y + 1, CalendarHud.SEASON_COLOURS[Calendar.season(info.clock())], true);
        y += LINE;

        // "3 tribes" on the divider
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
