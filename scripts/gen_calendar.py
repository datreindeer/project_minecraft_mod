"""Calendar: lang lines for the date in the HUD and /calendar (Calendar.java, client/CalendarHud.java).
Generator order: ... gen_equipment.py, gen_calendar.py, gen_block_items.py, ... gen_registry.py.
Run: python scripts/gen_calendar.py"""
import json
from pathlib import Path

A = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical"

SEASONS = {"spring": "Spring", "summer": "Summer", "autumn": "Autumn", "winter": "Winter"}
MONTHS = {"thaw": "Thaw", "sowing": "Sowing", "blossom": "Blossom", "midsun": "Midsun", "haymonth": "Haymonth",
          "reaping": "Reaping", "leaffall": "Leaffall", "hunting": "Hunting", "frostfall": "Frostfall",
          "deepwinter": "Deepwinter", "wolfmoon": "Wolfmoon", "snowmelt": "Snowmelt"}


def merge(path, obj):
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
    old.update(obj)
    path.write_text(json.dumps(old, indent=2) + "\n", encoding="utf-8")


LANG = {
    "calendar.mechanical.date": "%1$s · %2$s %3$s, Year %4$s · %5$s",
    "calendar.mechanical.day_length": "(day %s min, night %s min)",
    "message.mechanical.no_mating_in_winter": "Too cold: animals don't mate in winter",
}
LANG.update({f"calendar.mechanical.season.{k}": v for k, v in SEASONS.items()})
LANG.update({f"calendar.mechanical.month.{k}": v for k, v in MONTHS.items()})
merge(A / "lang/en_us.json", LANG)
print("calendar ok")
