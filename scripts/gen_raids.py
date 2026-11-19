"""Camp age and night raids: lang lines and the war drum sound (CampAge.java, people/Raids.java, people/RaidGoal.java).
The drum is a sound event built from vanilla's bass drum, merged into the existing assets/mechanical/sounds.json.
Generator order: ... gen_soot.py, gen_phantom.py, gen_raids.py, gen_block_items.py, gen_registry.py.
Run: python scripts/gen_raids.py"""
import json
from pathlib import Path

A = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical"


def merge(path, obj):
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
    old.update(obj)
    path.write_text(json.dumps(old, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


merge(A / "sounds.json", {"raid.drums": {"subtitle": "subtitles.mechanical.raid.drums", "sounds": [
    {"name": "block.note_block.basedrum", "type": "event", "pitch": 0.5, "volume": 1.0},
    {"name": "block.note_block.basedrum", "type": "event", "pitch": 0.6, "volume": 1.0},
    {"name": "block.note_block.basedrum", "type": "event", "pitch": 0.55, "volume": 0.8}]}})

LANG = {
    "subtitles.mechanical.raid.drums": "War drums",
    "message.mechanical.camp.duration": "%s days %s h",
    "message.mechanical.camp.none": "No camp here yet: keep a fire burning",
    "message.mechanical.camp.age": "This camp has burned for %s",
    "message.mechanical.camp.age_noticed": "This camp has burned for %s. Its smoke is seen far and wide...",
    "message.mechanical.raid.drums": "You hear drums in the dark, from the %s...",
    "message.mechanical.raid.coming": "War cries! Raiders of the %s come out of the dark from the %s, with torches!",
    "message.mechanical.raid.band": "Wandering Band",
    "message.mechanical.raid.stolen": "%s is carrying off your food!",
    "message.mechanical.raid.defended": "The raiders flee! They will think twice before coming back.",
}
LANG.update({f"message.mechanical.raid.dir.{d}": d for d in ("north", "south", "east", "west", "up", "down")})
merge(A / "lang/en_us.json", LANG)
print("raids ok")
