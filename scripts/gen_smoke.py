"""Smoke indoors (Smoke.java): the smoke damage type (armour doesn't help), the cough sound (vanilla drowning gasps and a
low panda sneeze) with its subtitle, and the death messages. Generator order: ... gen_farming.py, gen_wind.py,
gen_smoke.py, gen_soot.py, gen_block_items.py, gen_registry.py. Run: python scripts/gen_smoke.py"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources"
A, D = ROOT / "assets/mechanical", ROOT / "data"


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n")


def merge(path, obj):
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
    old.update(obj)
    path.write_text(json.dumps(old, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


w(D / "mechanical/damage_type/smoke.json", {"exhaustion": 0.0, "message_id": "mechanical.smoke", "scaling": "never"})
w(D / "minecraft/tags/damage_type/bypasses_armor.json", {"values": ["mechanical:smoke"]})
merge(A / "sounds.json", {"smoke.cough": {"subtitle": "subtitles.mechanical.smoke.cough", "sounds": [
    {"name": "entity.player.hurt_drown", "type": "event", "pitch": 1.3, "volume": 0.7, "weight": 2},
    {"name": "entity.panda.sneeze", "type": "event", "pitch": 0.6}]}})
merge(A / "lang/en_us.json", {
    "death.attack.mechanical.smoke": "%1$s choked on smoke",
    "death.attack.mechanical.smoke.player": "%1$s choked on smoke while fighting %2$s",
    "subtitles.mechanical.smoke.cough": "Coughing",
})
print("smoke ok")
