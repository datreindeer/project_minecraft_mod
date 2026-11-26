# Placera strukturer med kommando, och markörer på webbkartan

> Status: godkänd 2026-11-26.

## Varför
Admin vill kunna sätta ut en tribe village eller en Ancient Oak på valda koordinater, och se på webbkartan var Ancient Oaks, waystones och tribe villages finns.

## Mål
- `/structure tribe_village <x> <z>` och `/structure ancient_oak <x> <z>` (gamemaster, bara Overworld).
  - Laddar området, bygger, och sparar strukturstarten och referenserna i chunkarna, så att den fungerar som en naturlig (landtitel, besök, age-ombyggnad). Vanillas `/locate` hittar den inte, den söker bara på världsgenereringens egna platser.
  - Nekar med förklaring vid vatten, för kuperad mark, spelarbyggda block i vägen eller en struktur som redan står där.
  - Biomkravet ignoreras.
- Kartan visar Ancient Oaks, waystones och tribe villages med egna ikoner. De registreras när deras chunk laddas, och direkt vid placering. Bara utforskad mark.

## Inte nu
- Ingen force-flagga som skriver över byggen.
- Ingen snygg markutjämning vid placering (beard_box körs bara vid världsgenerering).
- Outforskade waystones visas inte. Ingen läsning av gamla regionfiler.

## Vägar
- **Eget kommando (vald):** sparar strukturstarten.
- **Vanillas `/place structure`:** inget att bygga, men sparar inte strukturen.
- **Kartan, alla waystones från seeden:** avslöjar outforskad mark.
