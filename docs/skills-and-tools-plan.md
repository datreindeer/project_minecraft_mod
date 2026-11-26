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
- **Byggt (v0.225.0), angreppssätt A:** verktygens item-typer finns kvar (huvudets material = itemet), delarna är en
  komponent `tool_parts` (binding, rod, Smithing-nivå) och vanillas komponenter (`MAX_DAMAGE`, verktygsfart, attackskada)
  räknas om när verktyget tas ut ur Tool Station. Ett verktyg utan `tool_parts` har receptets standarddelar och ändrar
  aldrig stats vid laddning, så gamla verktyg är orörda. Delar och faktorer: *Delar och nivåer* i `docs/ages/01-stone-age.md`.
- **Receptvisaren:** det blev inte ett recept per verktygstyp. `ToolStationRecipes.Recipe` har kvar ett recept per
  verktyg, men varje slot är en lista alternativ (alla tillåtna bindings och rods, filtrerade på nivå mot headet, bara
  sedda visas) och receptsidan visar samma stats-ruta som Tool Station. Upplåsning är fortfarande nyckeldelen (headet),
  så `UNLOCKED_TOOLS`-id:n och `SeenItems.backfill` behövde inte ändra form.
- **Tool Station-skärmen:** vänster verktygslista, mitten tre slots med spökbild, höger färdigt verktyg med stats och
  vad varje del bidrar med. Quest: A Better Haft (metallskaft).
