# ArneSurv

Survival-Essentials für Minecraft **26.3** (Fabric). Rein clientseitig – funktioniert auf jedem Server, auch Paper/Spigot, ohne Plugin.

## Funktionen

**Karte & Orientierung**
- **Minimap** – eckig oder rund, dreht mit, Farben aus den echten Block-Texturen (auch mit Resource Packs), Höhlenmodus (automatisch, im Nether immer), Waypoints, Spieler und Strukturen; weicht Trank-Symbolen aus
- **Weltkarte** – zeigt alles, was du schon gesehen hast (pro Welt/Server gespeichert), Zoom, Verschieben, Spieler, Rechtsklick setzt Waypoints
- **Waypoints** – anlegen, bearbeiten, verstecken, löschen; automatischer Todes-Waypoint; Marker direkt in der Spielwelt; per Chat teilen (andere übernehmen sie mit einem Klick)
- **Overlays** – Slime-Chunks, Chunk-Gitter, automatisch erkannte Strukturen (Dörfer, Festungen, Spawner, Trial Chambers, Antike Städte, Bastionen, Netherfestungen, Endstädte, Ozeanmonumente)
- **Info-Anzeige & Kompass** – Koordinaten, Biom, Richtung; optional Portal-Koordinaten, Uhrzeit, Tag, FPS, Ping

**Komfort**
- **Zoom** – Taste halten, Mausrad ändert die Stärke
- **Auto-Tool** – wählt beim Abbauen das passende Werkzeug aus der Hotbar
- **Inventar/Kiste sortieren** und **Einlagern** (Quick-Stack)
- **Sprint/Sneak umschalten**, Sneak bleibt im Inventar erhalten
- **Chat** – Uhrzeit, Erwähnungen mit Ton, längerer Verlauf
- **Screenshot-Galerie** im Spiel
- **Statistiken** – Vanilla-Statistiken pro Session, heute, Woche, Monat; Aufzeichnung nach Zeitraum löschbar
- **Update-Hinweis** im Hauptmenü, wenn es eine neue Version gibt

Alles ist einzeln abschaltbar, das HUD frei verschiebbar (`O` → HUD anpassen).

## Tasten (änderbar in den Steuerungseinstellungen)

| Taste | Funktion |
|---|---|
| `O` | ArneSurv-Menü |
| `M` | Weltkarte |
| `B` | Waypoint-Liste |
| `N` | Neuer Waypoint an der aktuellen Position |
| `C` (halten) | Zoom |
| `=` / `-` | Minimap rein-/rauszoomen |

## Installation

1. [Fabric Loader](https://fabricmc.net/use/) für 26.3 installieren
2. [Fabric API](https://modrinth.com/mod/fabric-api) in den `mods`-Ordner
3. `ArneSurv-x.y.z.jar` aus den [Releases](../../releases) in den `mods`-Ordner

Läuft zusammen mit [Distant Horizons](https://modrinth.com/mod/distanthorizons) für mehr Sichtweite.

## Gut zu wissen

- Slime-Chunks brauchen den Welt-Seed. Im Einzelspieler wird er automatisch erkannt, auf Servern einmal unter `O` → Overlays eintragen.
- Strukturen werden an typischen Blöcken erkannt, sobald diese in Sichtweite geladen werden.
- Alle Daten liegen unter `config/arnesurv/` (Karten, Waypoints, Strukturen, Statistik-Aufzeichnung).

## Neue Version veröffentlichen

Einen Tag hochladen, GitHub baut und veröffentlicht automatisch:

```
git tag v1.0.1
git push origin v1.0.1
```

## Lizenz

All Rights Reserved.
