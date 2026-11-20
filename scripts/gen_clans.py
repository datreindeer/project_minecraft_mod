"""Player tribes and the world age: lang lines for the tribe page (G) and its chat messages (clan/Clans.java, client/clan/ClanScreen.java).
Generator order: ... gen_equipment.py, gen_calendar.py, gen_clans.py, gen_block_items.py, ... gen_registry.py.
Run: python scripts/gen_clans.py"""
import json
from pathlib import Path

A = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical"


def merge(path, obj):
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
    old.update(obj)
    path.write_text(json.dumps(old, indent=2) + "\n", encoding="utf-8")


G = "gui.mechanical.clan."
M = "message.mechanical.clan."
LANG = {
    "key.mechanical.tribe": "Tribe",
    G + "title": "Tribe",
    G + "none": "You belong to no tribe",
    G + "name": "Name your tribe:",
    G + "need_pit": "Stand by a Fire Pit no tribe has to found a tribe",
    G + "found": "Found",
    G + "invites": "Invitations:",
    G + "no_invites": "No invitations",
    G + "accept": "Join",
    G + "decline": "Decline",
    G + "header": "%s · %s",
    G + "kick": "Kick out",
    G + "hand_over": "Make leader (you become an Elder)",
    G + "promote": "Promote",
    G + "demote": "Demote",
    G + "propose_leader": "The leader has been away long: call a vote to make this member leader",
    G + "page_up": "Previous page",
    G + "page_down": "Next page",
    G + "pick": "Click to pick another player",
    G + "invite": "Invite",
    G + "yes": "Yes",
    G + "no": "No",
    G + "yes_chosen": "Yes ✔",
    G + "no_chosen": "No ✔",
    G + "vote_move": "Vote: move Fire Pit",
    G + "vote_disband": "Vote: disband",
    G + "claim_pit": "Make this Fire Pit our centre",
    G + "leave": "Leave tribe",
    G + "leader_cant_leave": "Make someone else leader first",
    G + "vote_status": "Vote: %s. Yes %s, no %s of %s, %s days left",
    G + "no_pit": "The tribe has no Fire Pit: the leader picks a new one",
    G + "no_pit_leader": "Stand by a free Fire Pit to make it the tribe's centre",
    G + "no_vote": "No vote under way (2/3 of the tribe decides, 3 days)",
    G + "vote.disband": "disband the tribe",
    G + "vote.move": "move the Fire Pit",
    G + "vote.leader": "make %s leader",
    G + "rank.leader": "Leader",
    G + "rank.elder": "Elder",
    G + "rank.member": "Member",
    G + "rank.newcomer": "Newcomer",
    M + "pit_protected": "This Fire Pit is a tribe's centre: it can't be broken",
    M + "new_centre": "%s made a new Fire Pit the tribe's centre",
    M + "invited": "%s invites you to the tribe %s. Press G to answer",
    M + "invite_sent": "%s invited %s",
    M + "joined": "%s joined %s",
    M + "left": "%s left %s",
    M + "kicked": "%s was sent away from %s",
    M + "rank": "%s is now %s",
    M + "vote_started": "%s calls a vote to %s. Press G to vote",
    M + "vote_passed": "The tribe has decided to %s",
    M + "vote_failed": "The vote to %s failed",
    M + "vote_open": "Your tribe is voting. Press G to vote",
    M + "disbanded": "The tribe %s is no more",
    "message.mechanical.world_age.entered": "The world has entered the %s",
    "message.mechanical.world_age.is": "The world is in the %s",
}
for age in ("stone", "bronze", "iron", "steam", "electric"):
    LANG[G + "age." + age] = age.capitalize() + " Age"
merge(A / "lang/en_us.json", LANG)
print("clans ok")
