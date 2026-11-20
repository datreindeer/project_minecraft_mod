package com.mechanical.client.clan;

import java.util.UUID;

import com.mechanical.clan.ClanNetwork;
import com.mechanical.clan.ClanNetwork.Entry;
import com.mechanical.clan.ClanNetwork.Row;
import com.mechanical.clan.ClanNetwork.View;
import com.mechanical.clan.Clans;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * The tribe page (G). Without a tribe: found one by a free Fire Pit, or answer invitations. In a tribe: the members with
 * their rank and buttons for what your rank allows, inviting, the running vote or starting one, leaving. Every button
 * sends a {@link ClanNetwork.Action}; the server checks it and sends back a fresh {@link View}, which rebuilds the page.
 */
public class ClanScreen extends Screen {
    private static final int W = 320, PAD = 10, ROW = 20, ROWS = 7, BTN = 20;
    private static final int PANEL = 0xE0302418, BORDER = 0xFF8A6A44, TITLE = 0xFFF0D8A8, TEXT = 0xFFE8E0D0, DIM = 0xFF9A8A70, ONLINE = 0xFFB8E08A;
    private View view;
    private int top, panelH, page, invitePick;
    private String typedName = "";
    private EditBox nameBox;

    public ClanScreen(View view) {
        super(Component.translatable("gui.mechanical.clan.title"));
        this.view = view;
    }

    void update(View view) {
        this.view = view;
        rebuildWidgets();
    }

    private void send(int action, UUID target, String text) {
        ClientPacketDistributor.sendToServer(new ClanNetwork.Action(action, target, text));
    }

    private void send(int action, UUID target) {
        send(action, target, "");
    }

    private Button button(String key, int x, int y, int w, Button.OnPress press) {
        Button b = Button.builder(Component.translatable(key), press).bounds(x, y, w, BTN).build();
        addRenderableWidget(b);
        return b;
    }

    /** A small one-symbol button with a tooltip. */
    private void icon(String symbol, String tip, int x, int y, Button.OnPress press) {
        addRenderableWidget(Button.builder(Component.literal(symbol), press).bounds(x, y, BTN, BTN)
                .tooltip(Tooltip.create(Component.translatable(tip))).build());
    }

    @Override
    protected void init() {
        panelH = view.inClan() ? PAD + 14 + ROWS * ROW + 4 + BTN + 4 + 12 + BTN + 4 + BTN + PAD : PAD + 14 + 24 + BTN + 8 + Math.max(1, view.invites().size()) * ROW + 12 + PAD;
        top = (this.height - panelH) / 2;
        int left = (width - W) / 2, x = left + PAD, inner = W - 2 * PAD;
        if (!view.inClan()) {
            int y = top + PAD + 14 + 12;
            if (view.canPit()) {
                nameBox = new EditBox(font, x, y, inner - 84, BTN, Component.translatable("gui.mechanical.clan.name"));
                nameBox.setMaxLength(Clans.NAME_MAX);
                nameBox.setValue(typedName);
                nameBox.setResponder(s -> typedName = s);
                addRenderableWidget(nameBox);
                button("gui.mechanical.clan.found", x + inner - 80, y, 80, b -> send(ClanNetwork.FOUND, ClanNetwork.NONE, typedName));
            }
            y += BTN + 8 + 12;
            for (Entry invite : view.invites()) {
                button("gui.mechanical.clan.accept", x + inner - 124, y, 60, b -> send(ClanNetwork.ACCEPT, invite.id()));
                button("gui.mechanical.clan.decline", x + inner - 60, y, 60, b -> send(ClanNetwork.DECLINE, invite.id()));
                y += ROW;
            }
            return;
        }
        boolean leader = view.myRank() == Clans.LEADER, elder = view.myRank() == Clans.ELDER;
        int pages = Math.max(1, (view.members().size() + ROWS - 1) / ROWS);
        page = Math.min(page, pages - 1);
        int y = top + PAD + 14;
        for (int i = page * ROWS; i < Math.min(view.members().size(), (page + 1) * ROWS); i++) {
            Row r = view.members().get(i);
            int bx = x + inner - BTN;
            if (leader && r.rank() != Clans.LEADER) {
                icon("✕", "gui.mechanical.clan.kick", bx, y, b -> send(ClanNetwork.KICK, r.id()));
                bx -= BTN + 2;
                icon("★", "gui.mechanical.clan.hand_over", bx, y, b -> send(ClanNetwork.HAND_OVER, r.id()));
                bx -= BTN + 2;
                if (r.rank() != Clans.NEWCOMER) icon("-", "gui.mechanical.clan.demote", bx, y, b -> send(ClanNetwork.DEMOTE, r.id()));
                bx -= BTN + 2;
                if (r.rank() != Clans.ELDER) icon("+", "gui.mechanical.clan.promote", bx, y, b -> send(ClanNetwork.PROMOTE, r.id()));
            } else if (elder && r.rank() == Clans.NEWCOMER) {
                icon("✕", "gui.mechanical.clan.kick", bx, y, b -> send(ClanNetwork.KICK, r.id()));
            }
            if (view.canLeaderVote() && view.voteKind() < 0 && r.rank() != Clans.LEADER)
                icon("⚑", "gui.mechanical.clan.propose_leader", x + inner - 5 * (BTN + 2) + 2, y, b -> send(ClanNetwork.VOTE_LEADER, r.id()));
            y += ROW;
        }
        if (pages > 1) {
            icon("▲", "gui.mechanical.clan.page_up", left - BTN - 2, top + PAD + 14, b -> { page = Math.max(0, page - 1); rebuildWidgets(); });
            icon("▼", "gui.mechanical.clan.page_down", left - BTN - 2, top + PAD + 14 + BTN + 2, b -> { page = Math.min(pages - 1, page + 1); rebuildWidgets(); });
        }
        y = top + PAD + 14 + ROWS * ROW + 4;
        // invite: pick an online player without a tribe, then invite
        if ((leader || elder) && !view.invitable().isEmpty()) {
            invitePick = Math.floorMod(invitePick, view.invitable().size());
            Entry pick = view.invitable().get(invitePick);
            addRenderableWidget(Button.builder(Component.literal(pick.name()), b -> { invitePick++; rebuildWidgets(); })
                    .bounds(x, y, inner - 84, BTN).tooltip(Tooltip.create(Component.translatable("gui.mechanical.clan.pick"))).build());
            button("gui.mechanical.clan.invite", x + inner - 80, y, 80, b -> send(ClanNetwork.INVITE, pick.id()));
        }
        y += BTN + 4 + 12;
        if (view.voteKind() >= 0) {
            boolean mayVote = !(view.voteKind() == Clans.VOTE_LEADER && leader);
            if (mayVote) {
                button(view.myBallot() == 1 ? "gui.mechanical.clan.yes_chosen" : "gui.mechanical.clan.yes", x, y, inner / 2 - 2, b -> send(ClanNetwork.YES, ClanNetwork.NONE));
                button(view.myBallot() == 0 ? "gui.mechanical.clan.no_chosen" : "gui.mechanical.clan.no", x + inner / 2 + 2, y, inner / 2 - 2, b -> send(ClanNetwork.NO, ClanNetwork.NONE));
            }
        } else {
            if (view.hasPit()) button("gui.mechanical.clan.vote_move", x, y, inner / 2 - 2, b -> send(ClanNetwork.VOTE_MOVE, ClanNetwork.NONE));
            button("gui.mechanical.clan.vote_disband", x + inner / 2 + 2, y, inner / 2 - 2, b -> send(ClanNetwork.VOTE_DISBAND, ClanNetwork.NONE));
        }
        y += BTN + 4;
        if (view.canPit()) button("gui.mechanical.clan.claim_pit", x, y, inner / 2 - 2, b -> send(ClanNetwork.CLAIM_PIT, ClanNetwork.NONE));
        Button leave = button("gui.mechanical.clan.leave", x + inner / 2 + 2, y, inner / 2 - 2, b -> send(ClanNetwork.LEAVE, ClanNetwork.NONE));
        if (leader) {
            leave.active = false;
            leave.setTooltip(Tooltip.create(Component.translatable("gui.mechanical.clan.leader_cant_leave")));
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int left = (width - W) / 2;
        graphics.fill(left - 1, top - 1, left + W + 1, top + panelH + 1, BORDER);
        graphics.fill(left, top, left + W, top + panelH, PANEL);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        int left = (width - W) / 2, x = left + PAD, inner = W - 2 * PAD;
        if (!view.inClan()) {
            graphics.text(font, Component.translatable("gui.mechanical.clan.none"), x, top + PAD, TITLE);
            graphics.text(font, Component.translatable(view.canPit() ? "gui.mechanical.clan.name" : "gui.mechanical.clan.need_pit"), x, top + PAD + 14, TEXT);
            int y = top + PAD + 14 + 12 + BTN + 8;
            graphics.text(font, Component.translatable(view.invites().isEmpty() ? "gui.mechanical.clan.no_invites" : "gui.mechanical.clan.invites"), x, y, TEXT);
            y += 12;
            for (Entry invite : view.invites()) {
                graphics.text(font, Component.literal(invite.name()), x, y + 6, TITLE);
                y += ROW;
            }
        } else {
            graphics.text(font, Component.translatable("gui.mechanical.clan.header", view.name(),
                    Component.translatable("gui.mechanical.clan.age." + Clans.AGES[view.age()])), x, top + PAD, TITLE);
            int y = top + PAD + 14;
            for (int i = page * ROWS; i < Math.min(view.members().size(), (page + 1) * ROWS); i++) {
                Row r = view.members().get(i);
                graphics.text(font, Component.literal(r.name()), x, y + 6, r.online() ? ONLINE : DIM);
                graphics.text(font, Component.translatable("gui.mechanical.clan.rank." + Clans.RANKS[r.rank()]), x + 110, y + 6, TEXT);
                y += ROW;
            }
            y = top + PAD + 14 + ROWS * ROW + 4 + BTN + 4;
            Component status;
            if (view.voteKind() >= 0) {
                status = Component.translatable("gui.mechanical.clan.vote_status",
                        Component.translatable("gui.mechanical.clan.vote." + Clans.VOTES[view.voteKind()], view.voteWho()),
                        view.yes(), view.no(), view.voters(), view.daysLeft());
            } else if (!view.hasPit()) {
                status = Component.translatable(view.myRank() == Clans.LEADER ? "gui.mechanical.clan.no_pit_leader" : "gui.mechanical.clan.no_pit");
            } else {
                status = Component.translatable("gui.mechanical.clan.no_vote");
            }
            graphics.text(font, status, x, y, TEXT);
        }
        super.extractRenderState(graphics, mouseX, mouseY, a);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
