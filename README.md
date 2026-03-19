# Computerized Garden Simulation — User Manual

## Overview

This is an automated gardening simulation system built in Java with JavaFX. A 20×20 grid
garden contains 36 plants across 6 species, a population of insects (pests and beneficials),
24 sensors, and 12 sprinklers. Five standalone modules work autonomously to keep every
plant alive — watering, climate control, pest management, lighting, and fertigation.

The TA/professor can run the system, observe the garden surviving on its own, and
interact with it as a human gardener via the manual controls.

---

## Project Structure

```
garden-sim/
├── README.md                          # This file — user manual
├── LOG_GUIDE.md                       # Log navigation guide
├── logs/                              # Generated log files (created at runtime)
│   ├── garden_YYYYMMDD_HHmmss.log     # Full simulation log (one per run)
│   └── log.txt                        # API monitoring log (overwritten each run)
└── src/main/java/garden/
    ├── GardenApp.java                 # Main entry point — launches JavaFX + splash screen
    ├── Launcher.java                  # Thin launcher wrapper
    ├── core/
    │   ├── Garden.java                # Garden world: grid, plants, insects, sensors, environment
    │   └── SimulationEngine.java      # AnimationTimer tick loop, speed control
    ├── model/
    │   ├── plants/
    │   │   ├── Plant.java             # Abstract base: health, water, nutrients, growth stage
    │   │   ├── Tomato.java
    │   │   ├── Rose.java
    │   │   ├── Sunflower.java
    │   │   ├── Carrot.java
    │   │   ├── Lettuce.java
    │   │   └── Cactus.java
    │   ├── insects/
    │   │   ├── Insect.java            # Abstract base: movement, lifespan, type
    │   │   ├── Aphid.java             # Pest — damages nearby plants
    │   │   ├── Caterpillar.java       # Pest — heavier damage than aphid
    │   │   ├── Bee.java               # Pollinator — beneficial
    │   │   └── Ladybug.java           # Beneficial — hunts pests
    │   └── sensors/
    │       ├── Sensor.java            # Abstract base: threshold alerts
    │       ├── TemperatureSensor.java # Reads ambient °F; triggers heating/cooling
    │       ├── MoistureSensor.java    # Reads zone soil moisture; triggers sprinklers
    │       └── LightSensor.java       # Reads light level; triggers grow lights
    ├── modules/
    │   ├── GardenModule.java          # Interface all modules implement
    │   ├── WateringSystem.java        # Module 1: zone-based auto irrigation
    │   ├── HeatingSystem.java         # Module 2: zonal temperature control
    │   ├── PestControl.java           # Module 3: automated pest elimination
    │   ├── LightingSystem.java        # Module 4: supplemental grow lights
    │   └── FertigationSystem.java     # Module 5: zone-aware nutrient injection
    ├── api/
    │   ├── GardenSimulationAPI.java   # External monitoring/grading API
    │   └── GardenSimulator.java       # Simulation runner for API mode
    ├── ui/
    │   ├── GardenDashboard.fxml       # FXML layout for the dashboard
    │   ├── GardenController.java      # Handles all UI events and canvas rendering
    │   └── ImageManager.java          # Image caching for plant/insect/sensor icons
    ├── util/
    │   └── Position.java              # Immutable (row, col) grid coordinate
    └── logging/
        └── GardenLogger.java          # Singleton logger — file + in-memory storage
```

---

## How to Compile and Run

### Recommended: Maven (IntelliJ)

```bash
mvn javafx:run
```

> JDK 18 is required. In IntelliJ, set the project SDK to:
> `C:\Users\gpred\Downloads\openjdk-18.0.2_windows-x64_bin\jdk-18.0.2`

### Command Line (manual)

```bash
# Compile
javac --module-path /path/to/javafx-sdk/lib \
  --add-modules javafx.controls,javafx.fxml \
  -d out $(find src -name "*.java")

# Copy FXML
mkdir -p out/garden/ui
cp src/main/resources/garden/ui/GardenDashboard.fxml out/garden/ui/

# Run
java --module-path /path/to/javafx-sdk/lib \
  --add-modules javafx.controls,javafx.fxml \
  -cp out garden.Launcher
```

---

## UI Guide

### Layout Overview

```
┌─────────────────────────────────────────────────────┐
│  CONTROL BAR (top)                                  │
├──────────────────────────┬──────────────────────────┤
│                          │  TAB PANEL (right)        │
│   GARDEN CANVAS          │  [Seed Tray]              │
│   (left)                 │  [Plants]                 │
│                          │  [Sensors]                │
│                          │  [Modules]                │
├──────────────────────────┴──────────────────────────┤
│  LOG VIEWER (bottom)                                │
└─────────────────────────────────────────────────────┘
```

---

### Control Bar (Top)

| Control | Description |
|---------|-------------|
| **Pause / Resume** | Freezes or resumes the simulation tick loop |
| **Speed Slider** | Adjusts simulation speed — 0.1x (slow) to 5.0x (fast) |
| **Manual Water** | Immediately activates all sprinklers for one watering cycle |
| **Pest Control** | Immediately triggers the pest control module |
| **Fertilize All** | Injects nutrients into every living plant right now |
| **+  /  −  /  Fit** | Zoom in, zoom out, or auto-fit the canvas to the window |
| **Tick** | Shows the current simulation tick number |
| **Day** | Shows the current simulated day (1 day = 200 ticks) |

#### Keyboard Shortcuts

| Key | Action |
|-----|--------|
| `SPACE` | Pause / Resume |
| `↑` Arrow | Increase speed by 10% |
| `↓` Arrow | Decrease speed by 10% |
| `HOME` | Reset zoom to fit window |

---

### Garden Canvas (Left)

The canvas displays the full 20×20 grid. Each cell can contain one entity. The canvas
rescales automatically when the window is resized or the divider is dragged.

| Symbol | Meaning |
|--------|---------|
| Colored circle (large) | Living plant — color indicates species, size indicates growth stage |
| Gray circle | Dead plant |
| Letter inside plant | Species initial: **T** omato, **R** ose, **S** unflower, **C** arrot, **L** ettuce, **Ca** ctus |
| Blue square | Sprinkler (inactive) |
| Blue square with glow | Sprinkler (actively watering) |
| Yellow dot | Sensor (normal reading) |
| Red dot (sensor) | Sensor alert — reading outside its safe threshold |
| Red dot (moving) | Pest insect (Aphid or Caterpillar) |
| Gold dot | Pollinator (Bee) |
| Orange dot | Beneficial insect (Ladybug) |

**Plant color by species:**

| Color | Species |
|-------|---------|
| Red / orange | Tomato |
| Pink / purple | Rose |
| Yellow | Sunflower |
| Orange-brown | Carrot |
| Light green | Lettuce |
| Olive / dark green | Cactus |

---

### Tab Panel (Right)

#### Seed Tray Tab
Use this to manually place new plants in the garden.

1. Click a species button (Tomato, Rose, Sunflower, Carrot, Lettuce, Cactus)
2. Click any empty cell on the garden canvas to plant it
3. You can also drag an existing plant to a new empty cell to relocate it

The occupancy grid prevents planting on a cell that already has a plant, sprinkler, or sensor.

#### Plants Tab
Displays a live table of every plant:
- **ID** — unique identifier (e.g., PLANT-1)
- **Species** — plant type
- **Health** — HP out of 100 (green ≥ 60, orange ≥ 30, red < 30, gray = dead)
- **Water** — current water level %
- **Nutrients** — current nutrient level %
- **Stage** — growth stage (SEED → SPROUT → VEGETATIVE → FLOWERING → FRUITING → MATURE → WILTING → DEAD)
- **Position** — (row, col) on the grid

#### Sensors Tab
Displays all 24 sensors and their live readings:
- **ID** — sensor identifier (e.g., SENSOR-1)
- **Type** — Moisture, Temperature, or Light
- **Reading** — current measured value with unit
- **Alert** — highlighted red if reading is outside the configured threshold

#### Modules Tab
Displays the status summary of all 5 automated modules:
- Whether each module is enabled
- Current operational state (e.g., sprinklers active, lights on)
- Activation counts and other statistics

---

### Log Viewer (Bottom)

A scrollable, live display of recent simulation events (last 500 entries).

- Updates every tick automatically
- Entries are color-coded by level: INFO (white), WARN (yellow), ERROR (red)
- Use the **filter dropdown** to show only a specific category (PLANT, WATERING, etc.)
- The full log is also saved to `logs/garden_YYYYMMDD_HHmmss.log`

See **LOG_GUIDE.md** for a complete guide to reading and navigating the log files.

---

## The 5 Modules

All modules operate automatically every tick. They can also be triggered manually via
the control bar buttons.

### Module 1 — Watering System

Monitors zone moisture sensors and controls 12 sprinklers across 6 plant zones.

- Each sprinkler is linked to a dedicated moisture sensor for its zone
- When zone moisture drops below the low threshold → sprinkler activates
- When zone moisture rises above the high threshold → sprinkler deactivates
- Waters only plants within the zone rectangle (no cross-zone interference)
- Fertigation is applied with each watering cycle (nutrients added alongside water)

**Zones and thresholds:**

| Zone | Species | Activate below | Deactivate above |
|------|---------|---------------|-----------------|
| 1 | Tomatoes (rows 1–2) | 40% | 80% |
| 2 | Roses (rows 5–6) | 50% | 90% |
| 3 | Sunflowers (row 9) | 30% | 70% |
| 4a | Carrots (row 13) | 35% | 60% |
| 4b | Lettuce (row 14) | 45% | 75% |
| 5 | Cacti (row 18) | 25% | 50% |

### Module 2 — Heating / Climate Control

Maintains per-zone target temperatures using 6 heating zones linked to temperature sensors.

- If zone temperature < target → heating activates
- If zone temperature > target → cooling activates
- Adjusts the garden's ambient temperature by 0.15°F per active zone per tick

**Zone targets:**

| Zone | Species | Target |
|------|---------|--------|
| HZONE-1 | Tomatoes | 70°F |
| HZONE-2 | Roses | 67°F |
| HZONE-3 | Sunflowers | 70°F |
| HZONE-4 | Carrots | 62°F |
| HZONE-5 | Lettuce | 58°F |
| HZONE-6 | Cacti | 72°F |

### Module 3 — Pest Control

Monitors the pest population and eliminates threats automatically.

- Checks every 5 ticks
- Activates as soon as 1 or more live pests are detected (threshold = 1)
- Default method: **TARGETED** — kills only pest-type insects (Aphids, Caterpillars)
  - Does not harm Bees or Ladybugs
- Alternative methods (configurable): **ORGANIC** (slower), **CHEMICAL** (kills all insects)

### Module 4 — Lighting System

Supplements natural light with grow lights during low-light periods (nights, overcast).

- Activates when ambient light drops below 50 (out of 100)
- Turns off when natural light returns above 60
- Simulates a day/night cycle: light peaks midday, drops to near zero at night

### Module 5 — Fertigation System

Injects nutrients into each zone's plants on a scheduled interval.

- Checks every 12 ticks (~1.5 simulated hours)
- Per-zone nutrient thresholds calibrated to each species' consumption rate
- Stops injecting once nutrient level reaches the cap (prevents nutrient burn above 85%)

**Zone rates:**

| Zone | Species | Inject below | Cap at | Rate |
|------|---------|-------------|--------|------|
| Tomatoes | rows 1–2 | 40% | 85% | 10.0 |
| Roses | rows 5–6 | 40% | 85% | 8.0 |
| Sunflowers | row 9 | 40% | 85% | 10.0 |
| Carrots | row 13 | 40% | 85% | 6.0 |
| Lettuce | row 14 | 40% | 85% | 5.0 |
| Cacti | row 18 | 20% | 55% | 2.0 |

---

## Garden Layout

The garden is a 20×20 grid. Plants occupy the following rows:

```
Row  0  — (empty / border)
Rows 1–2  — Tomatoes (Zone 1)
Row  3    — Zone 1 infrastructure (sprinklers, sensors)
Row  4    — (empty)
Rows 5–6  — Roses (Zone 2)
Row  7    — Zone 2 infrastructure
Row  8    — (empty)
Row  9    — Sunflowers (Zone 3)
Row  10   — Zone 3 infrastructure
Rows 11–12 — (empty)
Row  13   — Carrots (Zone 4a)
Row  14   — Lettuce (Zone 4b)
Row  15   — Zones 4a/4b infrastructure
Rows 16–17 — (empty)
Row  18   — Cacti (Zone 5) with drip emitters
Row  19   — (empty / border)
```

---

## Plant Species Reference

| Species | Ideal Temp | Water/tick | Nutrient/tick | Light Need | Pest Resistance |
|---------|-----------|-----------|--------------|-----------|----------------|
| Tomato | 60–85°F | 1.2 | 0.5 | 8 hrs/day | 30% |
| Rose | 55–80°F | 1.0 | 0.4 | 6 hrs/day | 40% |
| Sunflower | 55–91°F | 1.5 | 0.5 | 10 hrs/day | 30% |
| Carrot | 45–75°F | 0.8 | 0.3 | 6 hrs/day | 40% |
| Lettuce | 40–70°F | 1.1 | 0.25 | 5 hrs/day | 15% |
| Cactus | 50–100°F | 0.15 | 0.05 | 10 hrs/day | 60% |

**Growth stages:** SEED → SPROUT → VEGETATIVE → FLOWERING → FRUITING → MATURE → WILTING → DEAD

---

## Time Scale Reference

| Unit | Value |
|------|-------|
| 1 tick | 500 ms real time (at 1.0x speed) |
| 1 simulated hour | ~8.33 ticks |
| 1 simulated day | 200 ticks (~100 seconds at 1.0x) |
| 24 real hours | ~2,000 simulated days |

---

## Extending the System

### Add a new plant species
1. Create a class in `model/plants/` extending `Plant`
2. Set species parameters in the constructor (temp range, water/nutrient needs, etc.)
3. Add instances in `Garden.initializeDefaultGarden()`
4. Add a button in `GardenDashboard.fxml` and wire it in `GardenController.java`

### Add a new module
1. Create a class in `modules/` implementing `GardenModule`
2. Register it in `Garden.initializeDefaultGarden()`
3. Add a status entry in the Modules tab of the FXML controller

### Add a new insect type
1. Create a class in `model/insects/` extending `Insect`
2. Add spawn logic in `Garden.spawnRandomInsect()`

### Add a new sensor type
1. Create a class in `model/sensors/` extending `Sensor`
2. Add instantiation in `Garden.initializeDefaultGarden()`
3. Handle the new type in the Sensors tab rendering in `GardenController.java`

---

## Exception Safety

All exceptions thrown during a tick are caught at the `SimulationEngine` level.
The garden continues running regardless. Errors are logged to the ERROR category
in the log file — search for `[ERROR]` to find any runtime issues.

---

## Log Files

Two log files are produced per run:

| File | Contents |
|------|----------|
| `logs/garden_YYYYMMDD_HHmmss.log` | Full simulation log — every event, every tick |
| `logs/log.txt` | API monitoring log — daily summaries and plant state snapshots |

See **LOG_GUIDE.md** for a complete guide to reading, filtering, and navigating the logs.
