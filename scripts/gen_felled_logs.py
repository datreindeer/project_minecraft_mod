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
    "message.mechanical.wagon.log_no_room": "A log needs the whole bed: take out the modules and unload it first",
    "message.mechanical.wagon.log_too_long": "Too long for the wagon: cut it to %s blocks or less",
    "message.mechanical.wagon.log_loaded": "The log is on the wagon. Sneak + right-click the wagon with an empty hand to roll it off",
    "message.mechanical.wagon.log_first": "Unload the log first: sneak + right-click with an empty hand",
})
LANG.write_text(json.dumps(old, indent=2) + "\n", encoding="utf-8")
print("felled logs ok")
