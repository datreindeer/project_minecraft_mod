"""Fear of the dark (Sanity.java, client/SanityClient.java): the stage messages and the restless Straw Bed message.
No sounds.json entries: the tricks reuse vanilla sound events (so a fake groan even shows the real subtitle).
Textures: scripts/gen_sanity_textures.py.
Generator order: ... gen_soot.py, gen_phantom.py, gen_sanity.py, gen_block_items.py, gen_registry.py.
Run: python scripts/gen_sanity.py"""
import json
from pathlib import Path

LANG = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/lang/en_us.json"


def merge(path, obj):
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
    old.update(obj)
    path.write_text(json.dumps(old, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


merge(LANG, {
    "message.mechanical.sanity.uneasy": "You feel watched...",
    "message.mechanical.sanity.afraid": "The dark presses in. Find a fire.",
    "message.mechanical.sanity.mad": "Your mind is slipping. Was that real?",
    "message.mechanical.sanity.restless": "You toss and turn. The shadows won't let you rest.",
})
print("sanity ok")
