"""Felled logs (FelledLogEntity.java, docs/living-world-plan.md F1): lang for the entity and its messages, and the
Wagon's log messages (WagonEntity.loadLog). Advancements: gen_advancements.py (stone_age haul_log, iron_age log_wagon).
Generator order: ... gen_wagon.py, gen_plough.py, gen_weaving.py, gen_felled_logs.py, ... gen_advancements.py, gen_registry.py.
Run: python scripts/gen_felled_logs.py"""
import json
from pathlib import Path

LANG = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/lang/en_us.json"

old = json.loads(LANG.read_text(encoding="utf-8"))
old.update({
    "entity.mechanical.felled_log": "Felled Log",
    "message.mechanical.felled_log.too_heavy": "Too heavy – more people needed",
    "message.mechanical.felled_log.root_end": "Tie the lead to the root end: the thick end that stood on the stump",
    "message.mechanical.felled_log.tied": "Lead tied on. Walk off to drag the log; right-click it with an empty hand to untie",
    "message.mechanical.felled_log.untied": "Lead untied",
    "message.mechanical.felled_log.too_big_to_lift": "Too big to lift by hand: tie a Lead to its root end (more people, or a horse)",
    "message.mechanical.felled_log.lift_by_hand": "Small enough to lift by hand: right-click an end with an empty hand",
    "message.mechanical.felled_log.grab_end": "Grab it by an end",
    "message.mechanical.felled_log.end_taken": "Someone already holds that end",
    "message.mechanical.felled_log.no_room": "No room to straighten the log here",
    "message.mechanical.felled_log.lifted": "End lifted. Walk to carry it; right-click it again or sneak to put it down",
    "message.mechanical.wagon.log_no_room": "Logs need an empty bed: take out the modules, chest and loads first",
    "message.mechanical.wagon.log_too_long": "Too long for this bed: cut it to %s blocks or less",
    "message.mechanical.wagon.log_loaded": "The log is loaded. Sneak + right-click with an empty hand to roll the last one off",
    "message.mechanical.wagon.log_full": "No room for another log",
    "message.mechanical.wagon.log_first": "Unload the log first: sneak + right-click with an empty hand",
})
LANG.write_text(json.dumps(old, indent=2) + "\n", encoding="utf-8")
print("felled logs ok")
