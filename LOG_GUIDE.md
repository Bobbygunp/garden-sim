# Garden Simulation — Log Navigation Guide

This guide explains how to read, search, and interpret the log files produced by
the garden simulation. The logs are the primary way to verify that every subsystem
is working correctly during a grading run.

---

## The Two Log Files

The simulation produces two separate log files, each serving a different purpose.

### 1. `logs/garden_YYYYMMDD_HHmmss.log` — Full Simulation Log

The definitive record of everything that happened during a run. A new file is created
each time the application starts; the filename encodes the start time.

Example filename: `logs/garden_20260314_083556.log`
→ started on 2026-03-14 at 08:35:56

This file contains every sensor alert, every sprinkler activation, every plant health
change, every pest interaction, every module decision — logged in real time. For a
24-hour grading run at 1.0x speed this file will be large; the sections below explain
how to navigate it efficiently.

### 2. `logs/log.txt` — API Monitoring Log

A compact, human-readable daily summary written by the external monitoring API.
This is the file the grading system reads. It is overwritten at the start of each run.

**Format:**
```
DAY=0,EVENT=init,EVENT_VALUE=none,PLANTS_ALIVE=16
DAY=1,EVENT=rain,EVENT_VALUE=25,PLANTS_ALIVE=16
DAY=5,EVENT=temperature,EVENT_VALUE=110,PLANTS_ALIVE=16
DAY=11,EVENT=parasite,EVENT_VALUE=caterpillar,PLANTS_ALIVE=16
...
=== GARDEN STATE — DAY 24 ===
PLANTS_ALIVE: 16 | PLANTS_DEAD: 0
TEMPERATURE:  65.7°F
PLANT DETAILS:
  Tomato     | HP: 100.0 | Water:  89.1 | Stage: MATURE | alive=true
  ...
```

Each line records what external event was injected on that day and how many plants
were still alive. The state block at the end is a snapshot of every plant at hour 24.
To verify survival: every `alive=true` entry with `HP > 0` counts as a surviving plant.

---

## Full Log Entry Format

Every line in `garden_*.log` follows this exact format:

```
[YYYY-MM-DD HH:mm:ss.SSS] [LEVEL] [CATEGORY] message text
```

**Example:**
```
[2026-03-14 08:35:56.476] [INFO] [PLANT] Carrot [PLANT-10] took 0.9 pest damage (resistance: 40%). Health: 99.1%
```

| Field | Value in example | Meaning |
|-------|-----------------|---------|
| Timestamp | `2026-03-14 08:35:56.476` | Wall-clock time when event occurred |
| Level | `INFO` | Severity — INFO, WARN, or ERROR |
| Category | `PLANT` | Which subsystem produced this entry |
| Message | `Carrot [PLANT-10] took...` | Human-readable description of the event |

---

## Log Levels

| Level | Meaning | When it appears |
|-------|---------|-----------------|
| `INFO` | Normal operation | Everything routine — activations, deactivations, state changes |
| `WARN` | Threshold crossed | Sensor alert, plant health critical, nutrient burn risk |
| `WARN` | Unusual state | Unexpected reading, borderline survival condition |
| `ERROR` | Runtime exception | Java exception caught during a tick; garden continues running |

**To find all problems quickly:** search the file for `[WARN]` and `[ERROR]`.

---

## Log Categories

There are 12 categories. Each entry is tagged with exactly one.

---

### `[APPLICATION]`

Application startup and shutdown events.

```
[INFO] [APPLICATION] Garden Simulation starting...
[INFO] [APPLICATION] JavaFX application launched successfully
[INFO] [APPLICATION] Simulation engine stopped. Garden shutting down.
```

**When to check:** If the program crashes, the last `[APPLICATION]` line before
the crash point will tell you what state the app was in.

---

### `[GARDEN]`

Garden-level lifecycle events: initialization, season changes, weather, and tick summaries.

```
[INFO] [GARDEN] Garden 'API Garden' created: 20x20 grid
[INFO] [GARDEN] === Initializing Professional Garden Layout ===
[INFO] [GARDEN] Garden initialized: 0 plants, 6 insects, 24 sensors, 5 modules
[INFO] [GARDEN] === Garden Ready ===
```

**Season changes** appear here as the simulation progresses through Spring → Summer →
Autumn → Winter (every 1000 ticks).

**When to check:** To confirm the garden initialized correctly — look for
`Garden initialized:` and verify the counts (24 sensors, 5 modules, 12 sprinklers).

---

### `[PLANT]`

Every event involving a plant's state. This is the most verbose category.

**Planted:**
```
[INFO] [PLANT] Tomato [PLANT-1] (Solanum lycopersicum) planted at (1, 1)
```

**Watered:**
```
[INFO] [PLANT] Cactus [PLANT-15] watered: 50.0 -> 80.0
```

**Pest damage:**
```
[INFO] [PLANT] Carrot [PLANT-10] took 0.9 pest damage (resistance: 40%). Health: 99.1%
```

**Growth stage advancement:**
```
[INFO] [PLANT] Tomato [PLANT-1] advanced to stage VEGETATIVE (tick 150)
```

**Health warning:**
```
[WARN] [PLANT] Rose [PLANT-4] health critical: 18.3%
```

**Death:**
```
[WARN] [PLANT] Lettuce [PLANT-12] died. Cause: dehydration. Final health: 0.0
```

**When to check:** If plants are dying, filter this category and look for `[WARN]`
entries. The cause of death (dehydration, temperature stress, pest damage, etc.)
is recorded in the death line.

---

### `[INSECT]`

Every insect appearance, movement, and death.

**Spawned:**
```
[INFO] [INSECT] Aphid [INSECT-5] (PEST) appeared at (3, 5)
[INFO] [INSECT] Bee [INSECT-1] (POLLINATOR) appeared at (4, 4)
[INFO] [INSECT] Ladybug [INSECT-4] (BENEFICIAL) appeared at (6, 6)
```

**Natural death:**
```
[INFO] [INSECT] Aphid [INSECT-5] died of old age (age: 200 ticks)
```

**Killed by pest control:**
```
[INFO] [INSECT] Aphid [INSECT-5] eliminated by TARGETED pest control
```

**When to check:** To confirm pest control is working, look for `eliminated by` lines
appearing after each `[PEST_CONTROL]` activation. If pest damage continues for many
lines without elimination entries following shortly after, pest control may not be
triggering.

---

### `[SENSOR]`

Sensor installation (startup) and threshold alerts (during the run).

**Installation (startup only):**
```
[INFO] [SENSOR] Moisture Sensor [SENSOR-1] installed at (3, 7) (range: 20.0-80.0 %)
[INFO] [SENSOR] Temperature Sensor [SENSOR-7] installed at (0, 0) (range: 40.0-95.0 °F)
[INFO] [SENSOR] Light Sensor [SENSOR-18] installed at (0, 5) (range: 20.0-90.0 lux)
```

**Alert (threshold crossed during run):**
```
[WARN] [SENSOR] ALERT: Moisture Sensor [SENSOR-6] at (18, 9) reading 80.0 % (threshold: 20.0-80.0)
[WARN] [SENSOR] ALERT: Light Sensor [SENSOR-18] at (0, 5) reading -0.1 lux (threshold: 20.0-90.0)
[WARN] [SENSOR] ALERT: Temperature Sensor [SENSOR-7] at (0, 0) reading 96.0°F (threshold: 40.0-95.0)
```

**When to check:** Light sensor alerts at night are normal (expected). Temperature
alerts during an injected extreme-temperature event are normal. Persistent moisture
alerts on a zone that has sprinklers suggest the watering system may not be covering
that zone correctly.

---

### `[WATERING]`

Every sprinkler activation, deactivation, and water delivery event.

**System startup:**
```
[INFO] [WATERING] Watering System initialized.
[INFO] [WATERING] Sprinkler SPR-1 added at (3, 4) (radius: 8.0, flow: 1.5, range: 40-80%)
```

**Activation:**
```
[INFO] [WATERING] Zone SPR-4 activated at (7, 4)
[INFO] [WATERING] Zone SPR-5 activated at (7, 12)
```

**Deactivation:**
```
[INFO] [WATERING] Zone SPR-4 deactivated at (7, 4) — moisture 81.2% above high threshold
```

**When to check:** If a plant zone is drying out (check `[PLANT]` for dehydration
deaths), verify that the corresponding sprinklers appear in `[WATERING]` activation
lines. If they never activate, the linked moisture sensor may not be reading correctly.

---

### `[HEATING]`

Zone heating and cooling state changes.

**System startup:**
```
[INFO] [HEATING] Heating System initialized (zonal mode).
[INFO] [HEATING] Zone HZONE-1 added at (3, 13) (target: 70.0°F)
```

**Heating on:**
```
[INFO] [HEATING] HZONE-1 HEAT ON: 0.0°F < target 70.0°F
```

**Cooling on:**
```
[INFO] [HEATING] HZONE-3 COOL ON: 82.5°F > target 70.0°F
```

**Heat/cool off:**
```
[INFO] [HEATING] HZONE-1 HEAT OFF: temperature 70.2°F reached target
```

**When to check:** During an injected extreme-temperature event (e.g., 110°F from the
API), look for `COOL ON` entries. If plants are dying from temperature stress but no
cooling entries appear, the heating system is not linked to the correct sensor zone.

---

### `[PEST_CONTROL]`

Pest detection and elimination cycles.

**System startup:**
```
[INFO] [PEST_CONTROL] Pest Control initialized. Method: TARGETED, Threshold: 1 pests
```

**Detection and elimination:**
```
[INFO] [PEST_CONTROL] 2 active pests detected. Activating TARGETED control.
[INFO] [PEST_CONTROL] Eliminated 2 pests. Total eliminated: 47
```

**When to check:** Count the `detected` lines and the `Eliminated` lines together.
If pests are detected but the eliminated count does not go up, the control method
may not be functioning. Cross-reference with `[INSECT]` elimination lines.

---

### `[LIGHTING]`

Grow light on/off events.

**System startup:**
```
[INFO] [LIGHTING] Lighting System initialized. Target light level: 60
```

**Lights on:**
```
[INFO] [LIGHTING] Grow lights ON: Natural light 0 below target 60
```

**Lights off:**
```
[INFO] [LIGHTING] Grow lights OFF: Natural light 65 above threshold
```

**When to check:** Lights should turn on every simulated night and off every simulated
day. If `Grow lights ON` entries appear every ~100 ticks with `OFF` entries in between,
the day/night cycle is working correctly.

---

### `[FERTIGATION]`

Nutrient injection events per zone.

**System startup:**
```
[INFO] [FERTIGATION] Soil Nutrition System initialized. Interval: 12 ticks.
[INFO] [FERTIGATION] Zone 'Tomatoes' configured: rows 1-2 | fertilize below 40%, cap at 85%, rate 10.0
```

**Injection:**
```
[INFO] [FERTIGATION] Zone 'Roses': fertilized 3 plants (nutrients: 35.2% -> 43.2%)
```

**Cap reached (no injection):**
```
[INFO] [FERTIGATION] Zone 'Tomatoes': all plants above threshold (avg 72.4%), skipping
```

**When to check:** If plants are dying from nutrient deficiency, confirm that
fertigation injection lines appear for that zone at regular intervals (~every 12 ticks).

---

### `[WEATHER]`

Rain events, which supplement soil moisture across the garden.

```
[INFO] [WEATHER] Rain started: intensity 0.15 units/tick
[INFO] [WEATHER] Rain stopped after 200 ticks
```

**When to check:** During an API `rain` event, a weather entry confirms the system
received and applied it. Rain supplements the watering system and should be visible
as moisture level increases in `[PLANT]` watered lines.

---

### `[USER_ACTION]`

Every manual action taken via the UI control bar.

```
[INFO] [USER_ACTION] Manual watering triggered by user
[INFO] [USER_ACTION] Manual pest control triggered by user
[INFO] [USER_ACTION] Manual fertilize-all triggered by user
[INFO] [USER_ACTION] Simulation paused by user
[INFO] [USER_ACTION] Simulation speed set to 3.0x
```

**When to check:** Provides an audit trail of human-gardener interventions during a
grading session. Confirms that UI buttons are connected to the simulation.

---

### `[API]`

Events injected by the external monitoring/grading system.

```
[INFO] [API] Garden initialised: 16 alive plants across 6 species.
[INFO] [API] Day 1 — Rain event: 25 units (0.125/tick)
[INFO] [API] Day 5 — Temperature override: 110.0°F for this day
[INFO] [API] Day 11 — Parasite injected: caterpillar at (8, 14)
```

**When to check:** To understand what external stressors were applied and when.
The day number in these entries matches the `DAY=` values in `log.txt`.

---

## How to Navigate a Long Log File

For a 24-hour grading run the `garden_*.log` file can be tens of thousands of lines.
Use text search to jump to what matters.

### Find all problems (WARN + ERROR only)

**Windows (PowerShell):**
```powershell
Select-String -Path "logs\garden_*.log" -Pattern "\[WARN\]|\[ERROR\]"
```

**Unix / Git Bash:**
```bash
grep -E "\[WARN\]|\[ERROR\]" logs/garden_*.log
```

---

### Filter by a single category

Show only pest control events:
```bash
grep "\[PEST_CONTROL\]" logs/garden_*.log
```

Show only plant deaths:
```bash
grep "died\." logs/garden_*.log
```

Show only sprinkler activations:
```bash
grep "activated at" logs/garden_*.log
```

---

### Check a specific plant

Track everything that happened to PLANT-4 (a Rose):
```bash
grep "PLANT-4" logs/garden_*.log
```

---

### Check a specific time window

Events between 08:35 and 08:40:
```bash
grep "08:3[5-9]" logs/garden_*.log
```

---

### Count events

How many times did pest control fire?
```bash
grep -c "active pests detected" logs/garden_*.log
```

How many plants died?
```bash
grep -c "died\." logs/garden_*.log
```

---

## Reading the Startup Block

The first ~90 lines of every `garden_*.log` are the initialization sequence. They
confirm the garden was set up correctly before the simulation started.

**Checklist to verify at startup:**

| What to look for | Expected |
|-----------------|---------|
| `[GARDEN] Garden initialized:` | 24 sensors, 5 modules |
| `[WATERING] Sprinkler SPR-` lines | 12 entries (SPR-1 through SPR-12) |
| `[SENSOR] Moisture Sensor` lines | 6 entries (SENSOR-1 through SENSOR-6) |
| `[SENSOR] Temperature Sensor` lines | 11 entries (SENSOR-7 through SENSOR-17) |
| `[SENSOR] Light Sensor` lines | 7 entries (SENSOR-18 through SENSOR-24) |
| `[HEATING] Zone HZONE-` lines | 6 entries (HZONE-1 through HZONE-6) |
| `[PLANT] ... planted at` lines | 16 entries (PLANT-1 through PLANT-16) |
| `[API] Garden initialised:` | `16 alive plants across 6 species` |

If any of these are missing the garden did not initialize fully. Look for `[ERROR]`
entries in the startup block for the cause.

---

## Common Scenarios and What to Look For

### "Are plants being watered?"

1. Search for `[WATERING]` — find `activated at` entries
2. Cross-check with `[PLANT]` — find `watered:` entries showing water level increases
3. If watering entries exist but plants still dehydrate, the flow rate may be too low
   for the zone's water consumption

### "Is pest control working?"

1. Search `[PEST_CONTROL]` for `active pests detected`
2. Search `[INSECT]` for `eliminated by TARGETED`
3. Search `[PLANT]` for `took ... pest damage` — frequency should drop after eliminations
4. If pests keep reappearing, ecological respawning is normal (aphids respawn every
   60 ticks, caterpillars every 100 ticks)

### "Why did a plant die?"

1. Search `[PLANT]` for the plant's ID and `died.` — the cause is on that line
2. Common causes:
   - `dehydration` — watering system not covering the zone
   - `temperature stress` — heating/cooling not activated; check `[HEATING]`
   - `nutrient deficiency` — fertigation not firing; check `[FERTIGATION]`
   - `pest damage` — pest control slow to react; check `[PEST_CONTROL]`
3. Look at the lines *before* the death for health warnings (`health critical`)
   to see how long the plant was stressed before dying

### "Did the garden survive the temperature spike?"

1. Find the `[API]` entry for the temperature override event
2. Look at the `[HEATING]` entries that follow — expect `COOL ON` for all 6 zones
3. Check `[PLANT]` entries in that time window — look for temperature stress damage lines
4. If plants survived with health above 0 the subsystems handled the event

### "Is the lighting system cycling correctly?"

1. Search `[LIGHTING]` — expect alternating `Grow lights ON` / `Grow lights OFF` entries
2. The pattern should repeat roughly every 200 ticks (one simulated day cycle)
3. If lights never turn off, the day/night cycle in the environment may have stopped

---

## Log File Tips

- **Do not delete old log files** — each run creates a new timestamped file, so
  old files are harmless and serve as a run history
- **The log is flushed to disk every 50 entries** — if the program is force-quit
  the last few entries may not appear in the file
- **The in-memory log (shown in the UI) holds the last 500 entries** — the file
  holds the complete history with no entry limit (up to 50,000 entries in memory
  to prevent RAM issues during 24+ hour runs; the file always has everything)
- **Timestamps are wall-clock time**, not simulation time — use tick numbers in
  message text (where present) to correlate with simulation days
