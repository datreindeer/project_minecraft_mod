# Skills och dynamiska verktyg – beslut (2026-11-26)

Projekt 2 och 3 efter receptvisaren (`docs/superpowers/specs/2026-11-26-recipe-viewer-design.md`). Varje projekt får en
egen design innan det byggs. Här står det användaren redan bestämt.

## Skills (projekt 2)

- Skills: Smithing, Mining, Farming, Fishing, Slaughter, Woodcutting, Gold Smithing, Cooking, Hunting, Masonry,
  Leatherworking, Fletching.
- Level **1–100**, **ingen skillcape**.
- XP fås genom att göra saken (bryta malm ger Mining-XP). Levels begränsar inget, de ger **förmåner**
  (t.ex. Mining 10: chans till extra malm).
- Vanillas XP-mätare finns kvar. Skill-XP visas på skill-sidan och i en liten popup.
- Inget tappas vid död.
- Förmånslistan: Claude föreslår per skill i skill-designen, användaren rättar.
- **Ringar och amuletter** ger +levels. De görs med Gold Smithing: en smidd ring/amulett med en sten från Lapidary
  Wheel. Stenen avgör skill, metallen hur många levels. Sitter i de befintliga ring-/halsplatserna (B).
- Skill-sidan: egen tangent **K**, egen skärm i quest-skärmens stil.

## Dynamiska verktyg (projekt 3)

- **Alla verktyg och vapen** med ett head blir dynamiska (pickaxe, axe, shovel, hoe, sickle, knife, spear, sword …),
  även vanillas iron/diamond ersätts.
- Delar: **head + binding + rod**, alla tre påverkar stats (durability, speed, damage …). Rod finns i trä, ben, koppar,
  brons, järn, stål. Rodens nivå får inte vara högre än headets (flint: bara trä; stål: allt upp till stål).
- Verktyget **ser ut som sina delar** (en textur per del och material).
- Gamla verktyg i AmandaTest **görs om automatiskt** till dynamiska med motsvarande delar och samma slitage.
- **Smithing-level** ger lite bonus (förslag: +0,5 % durability per level), visad som egen rad i stats.
- Tool Station byggs om: vänster = välj verktygstyp, mitten = input-slots som ändras efter typen, med bakgrundsbild i
  varje slot som visar vad som ska dit, höger = färdigt verktyg, dess stats och vad varje del bidrar med.
