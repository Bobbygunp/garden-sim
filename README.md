# Computerized Garden Simulation - User Manual

## Overview
This is an automated gardening simulation system built in Java with JavaFX and FXML.
The garden contains various plants, insects, sensors, sprinklers, and 4 standalone
modules that work together to keep the garden alive autonomously.

## Project Structure
```
garden-sim/
├── README.md
├── logs/                                        # Generated log files (created at runtime)
└── src/main/java/garden/
    ├── GardenApp.java                           # Main entry point (loads FXML)
    ├── core/
    │   ├── Garden.java                          # Garden world model (grid, entities, environment)
    │   └── SimulationEngine.java                # Drives the simulation tick loop
    ├── model/
    │   ├── plants/
    │   │   ├── Plant.java                       # Abstract base class for all plants
    │   │   ├── Tomato.java
    │   │   ├── Rose.java
    │   │   ├── Sunflower.java
    │   │   ├── Carrot.java
    │   │   ├── Lettuce.java
    │   │   └── Cactus.java
    │   ├── insects/
    │   │   ├── Insect.java                      # Abstract base class for insects
    │   │   ├── Aphid.java                       # Pest
    │   │   ├── Caterpillar.java                 # Pest
    │   │   ├── Bee.java                         # Pollinator
    │   │   └── Ladybug.java                     # Beneficial/Neutral
    │   └── sensors/
    │       ├── Sensor.java                      # Abstract sensor base
    │       ├── TemperatureSensor.java
    │       ├── MoistureSensor.java
    │       └── LightSensor.java
    ├── modules/
    │   ├── GardenModule.java                    # Module interface
    │   ├── WateringSystem.java                  # Module 1: Sprinklers & auto-watering
    │   ├── HeatingSystem.java                   # Module 2: Temperature/climate control
    │   ├── PestControl.java                     # Module 3: Automated pest management
    │   └── LightingSystem.java                  # Module 4: Supplemental grow lights
    ├── ui/
    │   ├── GardenDashboard.fxml                 # FXML layout file
    │   └── GardenController.java                # FXML controller (handles UI logic)
    ├── util/
    │   └── Position.java                        # Grid position helper class
    └── logging/
        └── GardenLogger.java                    # Comprehensive logging system
```

## How to Compile and Run

### Using javac (command line):
```bash
# Compile (adjust JavaFX path for your system)
javac --module-path /path/to/javafx-sdk/lib \
  --add-modules javafx.controls,javafx.fxml \
  -d out $(find src -name "*.java")

# Copy FXML file to output (must match package path)
mkdir -p out/garden/ui
cp src/main/java/garden/ui/GardenDashboard.fxml out/garden/ui/

# Run
java --module-path /path/to/javafx-sdk/lib \
  --add-modules javafx.controls,javafx.fxml \
  -cp out garden.GardenApp
```

### Using an IDE (IntelliJ / Eclipse):
1. Import as a Java project
2. Add JavaFX SDK to the project libraries
3. Set VM options:
   ```
   --module-path /path/to/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml
   ```
4. Make sure the FXML file is in the build output alongside the compiled classes
   (IntelliJ: mark `src/main/java` as Sources Root and it handles this automatically)
5. Run `garden.GardenApp`

## UI Guide

### Control Bar (Top)
- **Pause / Resume**: Pauses or resumes the simulation
- **Speed Slider**: Adjusts simulation speed (0.1x - 5.0x)
- **Manual Water**: Triggers all sprinklers immediately
- **Pest Control**: Manually activates the pest control system
- **Fertilize All**: Adds nutrients to all living plants
- **Tick Counter**: Shows current simulation tick

### Garden View (Left)
- **Green circles**: Living plants (size = growth stage, brightness = health)
- **Gray circles**: Dead plants
- **Blue squares**: Sprinklers (blue halo = actively watering)
- **Yellow dots**: Sensors (red = alert triggered)
- **Red dots**: Pest insects
- **Gold dots**: Pollinators (bees)
- **Orange dots**: Neutral insects (ladybugs)
- **Letter inside plant**: First letter of plant type (T=Tomato, R=Rose, etc.)

### Info Panel (Right)
- **Environment**: Current temperature, light, humidity
- **Modules**: Status of all 4 automated modules
- **Plant Status**: Health, water, nutrient levels for each plant
  - Green = healthy, Orange = stressed, Red = critical, Gray = dead

### Log Viewer (Bottom)
- **Filter dropdown**: Filter logs by category (PLANT, WATERING, etc.)
- Shows timestamped entries of every event in the simulation
- Log files are saved to the `logs/` directory

## Modules

### 1. Watering System
- Monitors moisture sensors and plant water levels
- Activates sprinklers when moisture drops below 25%
- Each sprinkler has a coverage radius and flow rate
- Supports manual triggering

### 2. Heating / Climate Control
- Monitors temperature sensors
- AUTO mode: heats when cold, cools when hot (target: 72°F)
- Can be set to manual HEATING or COOLING mode
- Configurable target temperature

### 3. Pest Control
- Monitors pest insect population
- Activates when pest count exceeds threshold (default: 3)
- Three methods: ORGANIC (safe, 70% kill), TARGETED (pests only), CHEMICAL (kills all)
- Supports manual triggering

### 4. Lighting System
- Monitors natural light levels
- Activates grow lights when light drops below target
- Simulates day/night cycle

## FXML Architecture
The UI uses JavaFX FXML for layout separation:
- `GardenDashboard.fxml` — Declares the entire UI layout (controls, canvas, panels, log)
- `GardenController.java` — Handles all UI events and rendering logic
- `GardenApp.java` — Loads the FXML, creates the model, and wires everything together

## Logging System
All events are logged with format: `[TIMESTAMP] [LEVEL] [CATEGORY] MESSAGE`

Categories: APPLICATION, GARDEN, PLANT, INSECT, WATERING, HEATING,
PEST_CONTROL, LIGHTING, SENSOR, USER_ACTION

Levels: INFO, WARN, ERROR

Log files are saved in the `logs/` directory with timestamps in the filename.

## Extending This Base

### To add a new plant type:
1. Create a class in `model/plants/` extending `Plant`
2. Set the environmental parameters in the constructor
3. Add instances in `Garden.initializeDefaultGarden()`

### To add a new module:
1. Create a class in `modules/` implementing `GardenModule`
2. Register it in `Garden.initializeDefaultGarden()`
3. Add UI controls in the FXML and controller as needed

### To add a new insect type:
1. Create a class in `model/insects/` extending `Insect`
2. Add spawn logic in `Garden.spawnRandomInsect()`

### To add a new sensor type:
1. Create a class in `model/sensors/` extending `Sensor`
2. Handle it in `Garden.tick()` sensor update loop
