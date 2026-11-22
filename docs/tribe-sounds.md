# Tribe sounds (living-world-plan B8)

Tribespeople no longer use villager (or player) sounds. They have their own sound events, registered in
`people/TribeSounds.java`, and **they are silent until the files below are added**: each event's entry in
`src/main/resources/assets/mechanical/sounds.json` has an empty `"sounds": []` list, which plays nothing and logs nothing.

| Event | Used for | Subtitle |
|---|---|---|
| `mechanical:entity.tribesperson.ambient` | the occasional murmur (about every 20 s, not while asleep) | Tribesperson murmurs |
| `mechanical:entity.tribesperson.hurt` | taking damage | Tribesperson hurts |
| `mechanical:entity.tribesperson.death` | dying | Tribesperson dies |
| `mechanical:entity.tribesperson.greet` | every chat line they say (greetings, warnings, thanks) | Tribesperson calls out |
| `mechanical:entity.tribesperson.trade` | a trade in the barter screen | Tribesperson agrees |

## Chosen sounds (all CC0, freesound.org)

Every page below was opened and its license box reads **Creative Commons 0**. The clips were picked from their title,
description and tags; nobody has listened to them yet. Long clips must be cut into short pieces.

| Event | Title | Author | URL | License | Length | Notes |
|---|---|---|---|---|---|---|
| ambient | Murmur 2.wav | jayfrosting | https://freesound.org/people/jayfrosting/sounds/333415/ | CC0 | 10.6 s | small crowd murmuring, wordless: cut into 2-3 s pieces |
| ambient | humming | AlexTerziyski | https://freesound.org/people/AlexTerziyski/sounds/754952/ | CC0 | 11.6 s | one person humming, no words: cut into 1-3 s pieces |
| ambient (rare) | CRWDSing_Mmm Hummed Near F_..._BIN-AmbeoVR.wav | ShangusBurger | https://freesound.org/people/ShangusBurger/sounds/764011/ | CC0 | 8.5 s | a group "mmm" hum, almost singing: a rare variant |
| hurt | Enemy hurt 1 | K27K_Mike | https://freesound.org/people/K27K_Mike/sounds/853659/ | CC0 | 1.4 s | short hurt groan, ready to use (may sound processed) |
| hurt | Male Grunts | abraksil | https://freesound.org/people/abraksil/sounds/727827/ | CC0 | 16.2 s | a sequence of pain grunts: cut into single grunts |
| hurt (alt) | voice_grunts | drummy | https://freesound.org/people/drummy/sounds/842336/ | CC0 | 20.7 s | pain grunts, lightly processed: cut |
| death | MaleDeathSound12.wav | Blankened | https://freesound.org/people/Blankened/sounds/577033/ | CC0 | 1.7 s | dying groan, ready to use |
| death | Zisa, Pain to Death, Groan Long, Whisper, Hoarse, Bizarre_1.wav | hisoul | https://freesound.org/people/hisoul/sounds/520274/ | CC0 | 4.3 s | a woman's hoarse dying groan (for the women) |
| greet | Yelling Eyoh in Empty Parking Garage | qubodup | https://freesound.org/people/qubodup/sounds/859347/ | CC0 | 3.6 s | wordless "eyoh!" call, primal; heavy echo, trim the tail |
| greet | dialogue_mumble_heh.ogg | dodrio | https://freesound.org/people/dodrio/sounds/554018/ | CC0 | 0.26 s | gibberish syllable from a game speech set |
| greet | dialogue_mumble_huuh.ogg | dodrio | https://freesound.org/people/dodrio/sounds/554023/ | CC0 | 0.21 s | same set (its siblings 554017, 554019, 554020 are probably CC0 too: check first) |
| greet (alt) | Sadiquecat MKE600 - Greetings - Hey | Sadiquecat | https://freesound.org/people/Sadiquecat/sounds/818146/ | CC0 | 0.44 s | a calm "hey": a real English word, so only if nothing else fits |
| trade | Hmm RTS Select Male NPC | VSE00 | https://freesound.org/people/VSE00/sounds/867879/ | CC0 | 0.74 s | short male "hmm" made for NPCs, ready to use |
| trade | AS016231_Aesthetic Appreciation.flac | SuddenDice | https://freesound.org/people/SuddenDice/sounds/558479/ | CC0 | 4.9 s | an appreciative "hmm": trim |
| trade (alt) | Guy saying mmhmm *5 takes* | NicolasLikesThisWebsite496 | https://freesound.org/people/NicolasLikesThisWebsite496/sounds/806888/ | CC0 | 9.4 s | five "mm-hmm" takes to cut apart (webcam mic) |

CC0 needs no credit, but keep this table as the record of where each file came from.

## Adding the files

1. Download the clips (a free freesound account is needed), cut them (e.g. Audacity) into short pieces, **mono** (mono
   sounds fade with distance; stereo ones don't), and export as **Ogg Vorbis** (.ogg).
2. Put them in `src/main/resources/assets/mechanical/sounds/tribesperson/` with these names:
   `murmur1.ogg`, `murmur2.ogg`, `murmur3.ogg`, `hum1.ogg`, `hurt1.ogg` ... `hurt3.ogg`, `death1.ogg`, `death_woman1.ogg`,
   `greet1.ogg` ... `greet3.ogg`, `trade1.ogg`, `trade2.ogg` (fewer or more is fine: the list in sounds.json decides).
3. Replace the five empty entries in `sounds.json` with these (names without `.ogg`; `pitch`/`volume` are starting points):

```json
  "entity.tribesperson.ambient": {
    "subtitle": "subtitles.mechanical.entity.tribesperson.ambient",
    "sounds": [
      {"name": "mechanical:tribesperson/murmur1", "volume": 0.6},
      {"name": "mechanical:tribesperson/murmur2", "volume": 0.6},
      {"name": "mechanical:tribesperson/murmur3", "volume": 0.6},
      {"name": "mechanical:tribesperson/hum1", "volume": 0.5, "weight": 1}
    ]
  },
  "entity.tribesperson.hurt": {
    "subtitle": "subtitles.mechanical.entity.tribesperson.hurt",
    "sounds": [
      "mechanical:tribesperson/hurt1",
      "mechanical:tribesperson/hurt2",
      "mechanical:tribesperson/hurt3"
    ]
  },
  "entity.tribesperson.death": {
    "subtitle": "subtitles.mechanical.entity.tribesperson.death",
    "sounds": [
      "mechanical:tribesperson/death1",
      "mechanical:tribesperson/death_woman1"
    ]
  },
  "entity.tribesperson.greet": {
    "subtitle": "subtitles.mechanical.entity.tribesperson.greet",
    "sounds": [
      {"name": "mechanical:tribesperson/greet1", "volume": 0.7},
      {"name": "mechanical:tribesperson/greet2", "volume": 0.7},
      {"name": "mechanical:tribesperson/greet3", "volume": 0.7}
    ]
  },
  "entity.tribesperson.trade": {
    "subtitle": "subtitles.mechanical.entity.tribesperson.trade",
    "sounds": [
      "mechanical:tribesperson/trade1",
      "mechanical:tribesperson/trade2"
    ]
  }
```

The game also pitches each voice by 0.9-1.1 (`Tribesperson.getVoicePitch`), so one recording gives a few voices.
Men and women share the same events for now; separate women's events would need a small code change.
