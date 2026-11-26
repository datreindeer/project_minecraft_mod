# Armborst – design (2026-11-26)

Bygger på `2026-11-26-dynamic-tools-design.md`. **(Claude)** = mina siffror, kan rättas.

## Användarens beslut
- Två armborst: **järn och stål**. Byggs på Tool Station av **prod + sträng + stock**; avtryckaren ingår i proddens recept.
- Bultar i **brons, järn och stål**; ett armborst skjuter alla bultar upp till sin nivå (järn: brons + järn; stål: alla).
- Vanillas Crossbow **blir järnarmborstet** (samma stats med standarddelar). Gamla armborst fungerar oförändrade.
  Crafting-receptet ersätts av Tool Station.

## Siffror (Claude)
- **Prod** (huvudet): Iron Crossbow Prod = 2 Iron Plates + 1 Iron Tool Rod (crafting, ersätter armborstreceptet).
  Steel Crossbow Prod = 2 Steel Plates + 1 Iron Tool Rod.
- **Sträng:** bågarnas strängtabell, standard Sinew. **Stock:** rod-tabellen upp till proddens nivå, standard Tool Rod.
- Delarna ändrar durability, laddningstid och bultens fart, relativt standarddelarna.
- **Järnarmborst:** som vanillas i dag. **Steel Crossbow:** dubbla durability, laddning 35 ticks (mot 25), bultfart ×1,15.
  Det ska se ut och kännas tyngre (vev), egen item-textur och laddnings-/laddad-bild som vanillas.
- **Bultar:** Bronze Bolt 3 skada, 25 % genom rustning; Iron Bolt oförändrad (4, 40 %); Steel Bolt 5, 55 %.
  Görs som Iron Bolt: 4 bolt heads per ingot (brons/stål som motsvarande delar görs) och fletchas i Fletching Table.
- Koger, ammunition från inventory/offhand, tooltips, receptvisaren (R/U, bara sett), skills (Smithing/Fletching-XP via
  befintliga krokar), LaterAgeGear (stålarmborst räknas som stålålderns), quest/advancement för Steel Crossbow.
- Mobs med armborst (pillagers) ska fungera som i dag.
- Utseende: stock och sträng syns på armborstet i inventoryt (gen_tool_parts.py), alla tre lägen (oladdad, spänner, laddad).
