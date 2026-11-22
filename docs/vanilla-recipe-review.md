# Genomgång av vanilla-recept (F8)

Gjord 2026-11-22 mot den patchade 26.3-jaren (2 042 recept: 1 202 crafting, 351 stonecutting, 73 smelting, 25 blasting,
9 smoking, 9 campfire, 30 smithing, 279 brewing). Regeln: ett recept spärras bara när det **tydligt** låter spelaren få
något tidigare eller billigare än moddets egen väg. Är det osäkert lämnas receptet och listas nedan. Spärrningen görs i
`scripts/gen_age_recipes.py` (never-villkor på receptet och dess unlock-advancement), som de tidigare spärrarna.

Saker som avgör mycket:

- **Furnace, Smoker, Blast Furnace, Stonecutter, Smithing Table, Brewing Stand, Anvil och Grindstone** går inte att
  använda (taggen `mechanical:too_advanced`, även de som står i byar). Därför går inga smelting-, blasting-, smoking-,
  stonecutting-, smithing- eller brewing-recept att göra just nu.
- **Vanillas Iron Ingot** finns inte förrän ångåldern (järnåldern har Wrought Iron Ingot), och metaller, nuggets,
  redstone och ädelstenar är bortfiltrerade ur all struktur-, fiske- och mob-loot. Alla recept som behöver Iron Ingot,
  Iron Nugget, Redstone, Quartz eller Diamond är alltså redan oåtkomliga i stenåldern.
- **Campfire-recepten** används av Fire Pit och Campfire och ska finnas kvar.

## Spärrat

| Recept | Varför |
|---|---|
| White Wool av 4 String (`white_wool_from_string`) | String från spindlar → Wool → 3 Wool Thread hoppar över klippningen. Beslutat med användaren. |
| Golden Pickaxe, Axe, Shovel, Hoe, Sword, Spear | Alla andra vanilla-verktyg är borttagna, guldet hade glömts. Guld finns redan i stenåldern (Lindworm-skatten: Block of Gold → 9 Gold Ingot), och en Golden Pickaxe bryter Coal Ore utan Bronze Pickaxe eftersom `incorrect_for_gold_tool` saknar `needs_bronze_tool`. |

Spärrat sedan tidigare (oförändrat): se tabellerna i `docs/ages/01-stone-age.md` ("Borttaget och ersatt" och "Recept
från senare åldrar") och `docs/ages/03-iron-age.md` ("Borttaget eller spärrat från vanilla").

## Lämnat med flit

| Recept | Varför det får vara kvar |
|---|---|
| Byggblock (trappor, slabs, murar, bricks, polished, chiseled, sandstone, mud bricks, mossy cobblestone, dyed wool/terracotta/carpet, banners, cushions) | Kosmetiska. Moddets egna stenfamiljer använder samma sorts recept redan i stenåldern. |
| Trävaror (Crafting Table, Chest, Barrel, Bowl, Ladder, Stick, dörrar, staket, skyltar, pressure plates, Composter, Fletching Table) | Plankor ger redan bara 2 per stock; inget av dem kringgår ett eget steg. |
| Loom, Scaffolding (String) | Banner-mönster och byggställning; inget i moddets kedja. Warp-Weighted Loom är ett eget block. |
| Item Frame, Painting, Book, Name Tag, Brush | Tar saker som redan är spärrade till sin ålder (Paper, kopparnuggets), eller är rent dekorativa. |
| Hay Bale ↔ 9 Wheat Sheaf, Melon ↔ Melon Slice, Dried Kelp Block, Bone Block, Coal Block, Snow, Ice-varianter | Lagring utan vinst. Hay Bale som bryts ger fortfarande bara Plant Fibre. |
| Bone Meal av Bone | Moddet gör själv Bone Meal av Fish Bones. |
| Torch (Coal/Charcoal + Stick) | Moddets facklor blåser ut och skyddar inte mot monster ändå; kol kräver Bronze Pickaxe eller kolning. |
| Pumpkin Pie, Mushroom Stew, Beetroot Soup, Suspicious Stew, Rabbit Stew | Inget mjöl i dem; potatis är sällsynt (bara tribe-handel). |
| Dyes av blommor, Sugar | Inget steg att hoppa över. |
| Kopparsaker (Copper Block/Bars/Chain/Door/Trapdoor/Grate/Torch/Lantern, nuggets) | Kräver gjuten Copper Ingot, alltså bronsåldern. Copper Ingot av Block of Copper är redan spärrat. |
| Allt med Iron Ingot/Nugget, Redstone, Quartz, Diamond, Emerald, Netherite, Blaze Rod, Ender Pearl, Shulker Shell, Echo Shard (rails, minecarts, hopper, piston, compass, clock, shield, saddle, anvil, beacon, enchanting table m.m.) | Oåtkomligt nu (se ovan). Ses över när vanillas Iron Ingot kommer i ångåldern. |
| Smelting, Blasting, Smoking, Stonecutting, Smithing, Brewing | Blocken är `too_advanced`. Guld- och järnsmältningen är ändå spärrad sedan tidigare. |
| Campfire Cooking | Används av Fire Pit och Campfire. |
| Leather Armor dyed, Shield/Banner-dekorationer, Map/Book cloning, Shulker Box-färgning, Tipped Arrow | Kräver något som redan är spärrat eller är kosmetiskt. |

## Osäkert (lämnat tills vidare)

| Recept | Tveksamheten |
|---|---|
| Leather av 4 Rabbit Hide | Hoppar över Scraped Hide och Drying Rack, men kostar 4 hudar (30–35 % drop) och Leatherworker byter i samma kurs. |
| Glowstone (4 Glowstone Dust från häxor) | Ett ljus som inte blåser ut, redan i stenåldern. Spectral Arrow och Respawn Anchor är spärrade av samma skäl, men själva blocket är ett vanligt byggblock. |
| Sea Lantern (Prismarine från guardians) | Samma sak som Glowstone, men kräver ett ocean monument. |
| Soul Campfire, Soul Torch | Soul Campfire görs av Logs utan Firewood och följer inte Campfire-ändringarna (släckt placering, drop). Soul Sand/Soil finns nästan bara i Nether. |
| Gold Ingot av Block of Gold, Golden Apple, Golden Carrot, Glistering Melon Slice, Golden Dandelion | Lindworm-skatten ger Block of Gold i stenåldern. Guldet ska egentligen komma med guldgjutning (F10), men skatten är en avsiktlig belöning. |
| Copper Ingot av Raw Copper/Copper Ore i Furnace/Blast Furnace (6 recept) och nuggets av koppar-/guld-/järnsaker | Oåtkomligt nu (Furnace är `too_advanced`). Guld och järn spärrades ändå i förväg; koppar borde nog också spärras innan ugnen kommer tillbaka. |
| Repair Item (två slitna verktyg → ett) | Gör moddets verktyg billigare (två halvslitna Flint-verktyg blir ett nästan nytt), men är inte en genväg förbi en ålder. |
| Rails och Minecarts | Kommer med vanillas Iron Ingot i ångåldern; om järnvägen ska höra till en senare ålder får det bestämmas då. |
