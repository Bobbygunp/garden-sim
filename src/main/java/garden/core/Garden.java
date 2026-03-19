package garden.core;

import garden.logging.GardenLogger;
import garden.model.insects.*;
import garden.model.plants.*;
import garden.model.sensors.*;
import garden.modules.*;
import garden.util.Position;

import java.util.*;

/**
 * The Garden represents the entire garden world.
 * It holds all plants, insects, sensors, modules, and environmental state.
 */
public class Garden {

    public enum Season { SPRING, SUMMER, AUTUMN, WINTER }

    private final String name;
    private final int rows;
    private final int cols;

    // Collections of garden entities
    private final List<Plant> plants;
    private final List<Insect> insects;
    private final List<Sensor> sensors;
    private final List<GardenModule> modules;

    // Efficient Spatial Map: Quick O(1) lookup for occupied grid cells
    private final boolean[][] occupancyGrid;

    // Environmental state
    private double currentTemperature;  // °F
    private double currentLightLevel;   // 0-100
    private double currentHumidity;     // 0-100%
    private int currentTick;
    private int dayNightCycle;          // ticks per full day
    
    // Season & Weather State
    private Season currentSeason = Season.SPRING;
    private boolean isRaining = false;
    private double rainIntensity = 0.0; // 0.0 to 5.0 water per tick
    private final int ticksPerSeason = 1000; // 5 days per season (200 * 5)

    // Modules (typed references for direct access)
    private WateringSystem wateringSystem;
    private HeatingSystem heatingSystem;
    private PestControl pestControl;
    private LightingSystem lightingSystem;

    // API overrides — set by GardenSimulationAPI for the duration of one simulated day
    private boolean temperatureOverrideActive = false;
    private double temperatureOverrideValue = 65.0;
    private boolean rainDisabled = false;

    private final Random random = new Random();

    public Garden(String name, int rows, int cols) {
        this.name = name;
        this.rows = rows;
        this.cols = cols;
        this.plants = Collections.synchronizedList(new ArrayList<>());
        this.insects = Collections.synchronizedList(new ArrayList<>());
        this.sensors = Collections.synchronizedList(new ArrayList<>());
        this.modules = Collections.synchronizedList(new ArrayList<>());
        this.occupancyGrid = new boolean[rows][cols];
        this.currentTemperature = 72.0;
        this.currentLightLevel = 60.0;
        this.currentHumidity = 50.0;
        this.currentTick = 0;
        this.dayNightCycle = 200; // 200 ticks = 1 simulated day

        GardenLogger.getInstance().log("GARDEN",
                String.format("Garden '%s' created: %dx%d grid", name, rows, cols));
    }

    /**
     * Initialize the garden with a realistic, industrial-grade layout.
     * Uses a 'Master Planting Plan' where infrastructure (pipes/sensors) 
     * and plant rows are organized into structured zones.
     */
    public void initializeDefaultGarden() {
        GardenLogger.getInstance().log("GARDEN", "=== Initializing Professional Garden Layout ===");

        // 1. INFRASTRUCTURE LAYER (Sprinklers & Sensors)
        wateringSystem = new WateringSystem();
        
        // --- Zone 1: Tomatoes (High Moisture) ---
        // Thresholds: 40% to 80%
        // Hardware moved to Row 3 (Plants are in Rows 1-2)
        MoistureSensor ms1 = new MoistureSensor(new Position(3, 7));
        ms1.setSensingRange(1, 2, 0, cols - 1);
        addSensor(ms1);
        Position spr1 = new Position(3, 4);
        Position spr2 = new Position(3, 10);
        Position spr3 = new Position(3, 16);
        wateringSystem.addSprinkler(spr1, 8.0, 1.5, 30.0, 70.0).linkSensor(ms1);
        wateringSystem.addSprinkler(spr2, 8.0, 1.5, 30.0, 70.0).linkSensor(ms1);
        wateringSystem.addSprinkler(spr3, 8.0, 1.5, 30.0, 70.0).linkSensor(ms1);
        markOccupied(spr1); markOccupied(spr2); markOccupied(spr3);

        // --- Zone 2: Roses (Very High Moisture) ---
        // Thresholds: 50% to 90%
        // Hardware moved to Row 7 (Plants are in Rows 5-6)
        MoistureSensor ms2 = new MoistureSensor(new Position(7, 8));
        ms2.setSensingRange(5, 6, 0, cols - 1);
        addSensor(ms2);
        Position spr4 = new Position(7, 4);
        Position spr5 = new Position(7, 12);
        wateringSystem.addSprinkler(spr4, 8.0, 1.0, 25.0, 65.0).linkSensor(ms2);
        wateringSystem.addSprinkler(spr5, 8.0, 1.0, 25.0, 65.0).linkSensor(ms2);
        markOccupied(spr4); markOccupied(spr5);

        // --- Zone 3: Sunflowers (Moderate Moisture) ---
        // Thresholds: 30% to 70%
        // Hardware moved to Row 10 (Plants are in Row 9)
        MoistureSensor ms3 = new MoistureSensor(new Position(10, 11));
        ms3.setSensingRange(9, 9, 0, cols - 1);
        addSensor(ms3);
        Position spr6 = new Position(10, 7);
        Position spr7 = new Position(10, 14);
        wateringSystem.addSprinkler(spr6, 8.0, 2.5, 30.0, 70.0).linkSensor(ms3);
        wateringSystem.addSprinkler(spr7, 8.0, 2.5, 30.0, 70.0).linkSensor(ms3);
        markOccupied(spr6); markOccupied(spr7);

        // --- Zone 4a: Carrots (Row 13) — Low water demand, moderate thresholds ---
        // Sensor in row 12 (empty infrastructure row above carrot bed).
        // Two gentle sprinklers (1.0 flow each) → 2.0/tick delivered to each carrot.
        // Net gain when active: 2.0 - 0.8 (carrot need) = 1.2/tick.
        // Max 4-tick sensor-lag overshoot: ~4.8 units above 60% → peaks ~64.8% (well below 90%).
        MoistureSensor ms4_carrot = new MoistureSensor(new Position(12, 10));
        ms4_carrot.setSensingRange(13, 13, 0, cols - 1);
        addSensor(ms4_carrot);
        Position spr8 = new Position(13, 4);
        Position spr9 = new Position(13, 10);
        wateringSystem.addSprinkler(spr8, 4.0, 1.0, 25.0, 60.0).linkSensor(ms4_carrot);
        wateringSystem.addSprinkler(spr9, 4.0, 1.0, 25.0, 60.0).linkSensor(ms4_carrot);
        markOccupied(spr8); markOccupied(spr9);

        // --- Zone 4b: Lettuce (Row 14) — High water demand, higher thresholds ---
        // Sensor in row 15 (empty infrastructure row below lettuce bed).
        // Single sprinkler (3.0 flow) → 3.0/tick per plant; lettuce needs 1.1/tick.
        // Net gain when active: 1.9/tick; max overshoot: ~7.6 → peaks ~82.6% (below 90%).
        // Lettuce waterTolerance=0.8 means brief high moisture causes minimal penalty.
        MoistureSensor ms4_lettuce = new MoistureSensor(new Position(15, 10));
        ms4_lettuce.setSensingRange(14, 14, 0, cols - 1);
        addSensor(ms4_lettuce);
        Position spr10 = new Position(13, 16);
        wateringSystem.addSprinkler(spr10, 8.0, 3.0, 40.0, 75.0).linkSensor(ms4_lettuce);
        markOccupied(spr10);

        // --- Zone 5: Cacti (Arid/Dry Zone) ---
        // Thresholds: 25% to 50% (drought-tolerant, low flow)
        // Two drip emitters cover the full row in two halves:
        //   Left  emitter (col 4, r=4): reaches cacti at cols 1 and 6
        //   Right emitter (col 15, r=4): reaches cacti at cols 11 and 16
        // Single moisture sensor at col 9 (center) monitors all cacti.
        MoistureSensor ms5 = new MoistureSensor(new Position(18, 9));
        ms5.setSensingRange(18, 18, 0, cols - 1);
        addSensor(ms5);
        Position spr11 = new Position(18, 4);
        Position spr12 = new Position(18, 15);
        wateringSystem.addSprinkler(spr11, 4.0, 1.0, 10.0, 35.0).linkSensor(ms5);
        wateringSystem.addSprinkler(spr12, 4.0, 1.0, 10.0, 35.0).linkSensor(ms5);
        markOccupied(spr11); markOccupied(spr12);

        modules.add(wateringSystem);

        // Ambient Temperature Monitoring (corners + center)
        addSensor(new TemperatureSensor(new Position(0, 0)));
        addSensor(new TemperatureSensor(new Position(0, cols - 1)));
        addSensor(new TemperatureSensor(new Position(rows - 1, 0)));
        addSensor(new TemperatureSensor(new Position(rows - 1, cols - 1)));
        addSensor(new TemperatureSensor(new Position(rows / 2, cols / 2)));

        // Zone Temperature Sensors — one per plant zone, placed in infrastructure rows.
        // Each sensor is linked to a HeatingZone so the heater reacts to the local reading.
        TemperatureSensor ts1 = new TemperatureSensor(new Position(3, 13));   // Tomato zone
        ts1.setSensingRange(1, 2, 0, cols - 1);
        addSensor(ts1);

        TemperatureSensor ts2 = new TemperatureSensor(new Position(7, 1));    // Rose zone
        ts2.setSensingRange(5, 6, 0, cols - 1);
        addSensor(ts2);

        TemperatureSensor ts3 = new TemperatureSensor(new Position(10, 1));   // Sunflower zone
        ts3.setSensingRange(9, 9, 0, cols - 1);
        addSensor(ts3);

        TemperatureSensor ts4 = new TemperatureSensor(new Position(12, 1));   // Carrot zone
        ts4.setSensingRange(13, 13, 0, cols - 1);
        addSensor(ts4);

        TemperatureSensor ts5 = new TemperatureSensor(new Position(15, 1));   // Lettuce zone
        ts5.setSensingRange(14, 14, 0, cols - 1);
        addSensor(ts5);

        TemperatureSensor ts6 = new TemperatureSensor(new Position(18, 2));   // Cactus zone
        ts6.setSensingRange(18, 18, 0, cols - 1);
        addSensor(ts6);
        
        addSensor(new LightSensor(new Position(0, 5)));
        addSensor(new LightSensor(new Position(0, cols / 2)));
        addSensor(new LightSensor(new Position(0, cols - 6)));
        addSensor(new LightSensor(new Position(rows - 1, 5)));
        addSensor(new LightSensor(new Position(rows - 1, cols / 2)));
        addSensor(new LightSensor(new Position(rows - 1, cols - 6)));
        addSensor(new LightSensor(new Position(rows / 2, 13)));

        // 2. PLANTING LAYER — commented out so the garden starts empty.
        //    Plants are placed interactively via the PvZ-style seed tray in the UI.
        //    Zone rows: Tomato=1-2, Rose=5-6, Sunflower=9, Carrot=13, Lettuce=14, Cactus=18.
        //
        // Uncomment below to restore pre-planted beds:
        //
        // // Bed 1: Tomatoes (Rows 1-2)
        // for (int c = 1; c < cols - 1; c += 3) {
        //     addPlant(new Tomato(new Position(1, c)));
        //     addPlant(new Tomato(new Position(2, c)));
        // }
        // // Bed 2: Roses (Rows 5-6)
        // for (int c = 1; c < cols - 1; c += 3) {
        //     addPlant(new Rose(new Position(5, c)));
        //     addPlant(new Rose(new Position(6, c)));
        // }
        // // Bed 3: Sunflowers (Row 9)
        // for (int c = 2; c < cols - 1; c += 4) {
        //     addPlant(new Sunflower(new Position(9, c)));
        // }
        // // Bed 4: Carrots & Lettuce (Rows 13-14)
        // for (int c = 1; c < cols - 1; c += 2) {
        //     addPlant(new Carrot(new Position(13, c)));
        //     addPlant(new Lettuce(new Position(14, c)));
        // }
        // // Bed 5: Cacti (Row 18)
        // for (int c = 1; c < cols; c += 5) {
        //     Cactus cactus = new Cactus(new Position(18, c));
        //     cactus.water(30);
        //     addPlant(cactus);
        // }

        // --- Add Insects ---
        addInsect(new Bee(new Position(4, 4)));
        addInsect(new Bee(new Position(8, 8)));
        addInsect(new Bee(new Position(6, 16)));
        addInsect(new Ladybug(new Position(6, 6)));
        addInsect(new Aphid(new Position(3, 5)));
        addInsect(new Caterpillar(new Position(11, 3)));

        // --- Heating System (Zonal) ---
        // Each plant zone has its own heater linked to its zone temperature sensor.
        // Targets are set near the midpoint of each species' ideal range.
        // Per-zone rate 0.05°F/tick → up to 0.30°F/tick when all 6 zones agree.
        heatingSystem = new HeatingSystem();
        // 0.15°F/tick per zone × 6 zones = 0.90°F/tick max HVAC power.
        // Natural drift rate is 0.2°F/tick, so HVAC can always win against any
        // external temperature (40-120°F) and hold internal temp in the safe range.
        heatingSystem.setTemperatureAdjustRate(0.5);
        heatingSystem.addZone(new Position(3, 13),  70.0).linkSensor(ts1);  // Tomatoes: ideal 60-85°F
        heatingSystem.addZone(new Position(7, 1),   67.0).linkSensor(ts2);  // Roses: ideal 55-80°F
        heatingSystem.addZone(new Position(10, 1),  70.0).linkSensor(ts3);  // Sunflowers: ideal 55-91°F
        heatingSystem.addZone(new Position(12, 1),  62.0).linkSensor(ts4);  // Carrots: ideal 45-75°F
        heatingSystem.addZone(new Position(15, 1),  58.0).linkSensor(ts5);  // Lettuce: ideal 40-70°F
        heatingSystem.addZone(new Position(18, 2),  72.0).linkSensor(ts6);  // Cacti: ideal 50-100°F
        modules.add(heatingSystem);

        pestControl = new PestControl();
        modules.add(pestControl);

        lightingSystem = new LightingSystem();
        modules.add(lightingSystem);

        // Zone-specific fertigation: injection rates are calibrated to each species'
        // nutrientNeedPerTick so all zones stay in the healthy 40-80% range.
        // Thresholds match the moisture sensor zone rectangles for consistency.
        FertigationSystem fertigationSystem = new FertigationSystem();
        fertigationSystem.addZone(new FertigationSystem.FertigationZone(
                "Tomatoes",       1,  2,  0, cols - 1, 40.0, 85.0, 10.0)); // 0.5/tick — heavy feeder
        fertigationSystem.addZone(new FertigationSystem.FertigationZone(
                "Roses",          5,  6,  0, cols - 1, 40.0, 85.0,  8.0)); // 0.4/tick — moderate feeder
        fertigationSystem.addZone(new FertigationSystem.FertigationZone(
                "Sunflowers",     9,  9,  0, cols - 1, 40.0, 85.0, 10.0)); // 0.5/tick — heavy feeder
        fertigationSystem.addZone(new FertigationSystem.FertigationZone(
                "Carrots",        13, 13, 0, cols - 1, 40.0, 85.0,  6.0)); // 0.3/tick — light feeder
        fertigationSystem.addZone(new FertigationSystem.FertigationZone(
                "Lettuce",        14, 14, 0, cols - 1, 40.0, 85.0,  5.0)); // 0.25/tick — light feeder
        fertigationSystem.addZone(new FertigationSystem.FertigationZone(
                "Cacti",         18, 18,  0, cols - 1, 20.0, 55.0,  2.0)); // 0.05/tick — minimal needs
        modules.add(fertigationSystem);

        GardenLogger.getInstance().log("GARDEN",
                String.format("Garden initialized: %d plants, %d insects, %d sensors, %d modules",
                        plants.size(), insects.size(), sensors.size(), modules.size()));
        GardenLogger.getInstance().log("GARDEN", "=== Garden Ready ===");
    }

    /**
     * Checks if a specific grid cell is already occupied by infrastructure 
     * or other plants to prevent overlapping.
     */
    public boolean isPositionOccupied(Position pos) {
        if (pos.getRow() < 0 || pos.getRow() >= rows || pos.getCol() < 0 || pos.getCol() >= cols) {
            return true;
        }
        return occupancyGrid[pos.getRow()][pos.getCol()];
    }

    private void markOccupied(Position pos) {
        if (pos.getRow() >= 0 && pos.getRow() < rows && pos.getCol() >= 0 && pos.getCol() < cols) {
            occupancyGrid[pos.getRow()][pos.getCol()] = true;
        }
    }

    /**
     * Called every simulation tick. Updates environment, sensors, plants,
     * insects, and modules.
     */
    public void tick() {
        try {
            currentTick++;

            // 1. Update environment (Seasons & Weather)
            updateEnvironment();

            // 1b. Apply Rain Effect: Rain increases water level for all plants
            if (isRaining && !rainDisabled) {
                for (Plant plant : plants) {
                    if (plant.isAlive()) {
                        // Rain intensity is 1.0 to 5.0; we apply a smaller portion 
                        // to prevent instant overwatering (0.05 to 0.25 units/tick)
                        plant.water(rainIntensity * 0.05, true);
                    }
                }
            }

            // 2. Update all sensors
            List<Plant> alivePlants = plants.stream().filter(Plant::isAlive).toList();
            for (Sensor sensor : sensors) {
                if (sensor instanceof TemperatureSensor) {
                    sensor.update(currentTemperature, currentTick);
                } else if (sensor instanceof MoistureSensor ms) {
                    ms.update(alivePlants, currentTick); // Realistic local measurement
                } else if (sensor instanceof LightSensor) {
                    sensor.update(currentLightLevel, currentTick);
                }
            }

            // 3. Update all modules
            for (GardenModule module : modules) {
                module.update(this);
            }

            // 4. Update all plants — each plant receives its zone's temperature
            for (Plant plant : plants) {
                double plantTemp = (heatingSystem != null)
                        ? heatingSystem.getZoneTemperatureAt(plant.getPosition(), currentTemperature)
                        : currentTemperature;
                plant.update(plantTemp, currentLightLevel, currentHumidity);
            }

            // 5. Update all insects (optimization: only pass alive plants)
            List<Plant> alivePlantsList = plants.stream()
                    .filter(Plant::isAlive)
                    .toList();
            for (Insect insect : insects) {
                insect.update(alivePlantsList, rows, cols);
            }

            // 5b. Biological control: beneficial insects (ladybugs) hunt pests
            //     Hunt radius 2.0 cells, 30% chance per tick — models real ladybug
            //     behavior (~50 aphids/day, but not instant kills).
            List<Insect> aliveInsects = insects.stream()
                    .filter(Insect::isAlive).toList();
            for (Insect insect : aliveInsects) {
                insect.predateInsects(aliveInsects, 2.0, 0.3);
            }

            // 6. Ecologically-driven insect spawning — each species has its own
            //    spawn interval and probability, modelling real population dynamics.
            spawnInsectsEcological();

            // 6b. Clean up dead entities every 200 ticks to prevent list growth
            if (currentTick % 200 == 0) {
                insects.removeIf(i -> !i.isAlive());
                // Note: Dead plants are kept in the list so they remain visible on the grid
            }

            // 7. Periodic status log
            if (currentTick % 50 == 0) {
                logPeriodicStatus();
            }

        } catch (Exception e) {
            GardenLogger.getInstance().logError("GARDEN",
                    "Error during tick " + currentTick, e);
            // Garden must not crash - continue operating
        }
    }

    private void updateEnvironment() {
        // --- 1. Season Transition Logic ---
        Season oldSeason = currentSeason;
        int seasonIndex = (currentTick / ticksPerSeason) % 4;
        currentSeason = Season.values()[seasonIndex];

        if (currentSeason != oldSeason) {
            GardenLogger.getInstance().log("GARDEN", "=== SEASON CHANGE: Welcome to " + currentSeason + " ===");
        }

        // --- 2. Rain/Weather Engine ---
        // Rain probability varies by season: Spring(0.3%), Summer(0.05%), Autumn(0.2%), Winter(0.15%)
        double rainChance = switch (currentSeason) {
            case SPRING -> 0.003; 
            case SUMMER -> 0.0005;
            case AUTUMN -> 0.002;
            case WINTER -> 0.0015;
        };

        if (!rainDisabled) {
            if (!isRaining && random.nextDouble() < rainChance) {
                isRaining = true;
                rainIntensity = 1.0 + random.nextDouble() * 4.0;
                GardenLogger.getInstance().log("WEATHER", String.format("It started raining! Intensity: %.1f", rainIntensity));
            } else if (isRaining && random.nextDouble() < 0.02) {
                isRaining = false;
                GardenLogger.getInstance().log("WEATHER", "The rain has stopped.");
            }
        } else {
            isRaining = false;
            rainIntensity = 0;
        }

        // --- 3. Light, Temperature & Humidity Calculations ---
        double dayProgress = (currentTick % dayNightCycle) / (double) dayNightCycle;
        double sunEffect = Math.sin(dayProgress * 2 * Math.PI - Math.PI / 2);

        // Seasonal Baselines
        double baseTemp = switch (currentSeason) {
            case SPRING -> 65.0;
            case SUMMER -> 85.0;
            case AUTUMN -> 55.0;
            case WINTER -> 35.0;
        };

        double maxLight = switch (currentSeason) {
            case SPRING -> 90.0;
            case SUMMER -> 100.0;
            case AUTUMN -> 70.0;
            case WINTER -> 40.0;
        };

        // Light: Immediate reset is fine (light doesn't have "inertia" like temp)
        double naturalLight = Math.max(0, maxLight * sunEffect);
        if (isRaining) naturalLight *= 0.5;
        currentLightLevel = naturalLight;

        // Temperature: Target = Seasonal base + day/night swing
        double targetAmbientTemp = baseTemp + (10.0 * sunEffect);

        // When API sets a temperature, use it as the external ambient target the
        // environment drifts toward — the HVAC module then actively fights it.
        // This models a real greenhouse: outside temp is extreme, HVAC maintains
        // safe internal conditions. Hard-setting would bypass the HVAC entirely.
        if (temperatureOverrideActive) {
            targetAmbientTemp = temperatureOverrideValue;
        }

        // Drifts towards the target ambient temperature naturally (0.2 degrees per tick)
        if (currentTemperature < targetAmbientTemp) {
            currentTemperature = Math.min(targetAmbientTemp, currentTemperature + 0.2);
        } else if (currentTemperature > targetAmbientTemp) {
            currentTemperature = Math.max(targetAmbientTemp, currentTemperature - 0.2);
        }
        // Add a small amount of random drift
        currentTemperature += (random.nextGaussian() * 0.1);

        // Humidity: Drifts towards target (higher at night, higher when raining)
        double targetHumidity = isRaining ? 85.0 : 40.0;
        targetHumidity += 20.0 * (-sunEffect); 
        targetHumidity = Math.max(20, Math.min(100, targetHumidity));

        if (currentHumidity < targetHumidity) {
            currentHumidity = Math.min(targetHumidity, currentHumidity + 0.5);
        } else if (currentHumidity > targetHumidity) {
            currentHumidity = Math.max(targetHumidity, currentHumidity - 0.5);
        }
    }

    /**
     * Ecologically-driven insect spawning. Each species has a unique spawn
     * interval and probability that reflects real-world population dynamics:
     *
     *   Aphids      — every  60 ticks, 40% chance (prolific asexual reproducers)
     *   Caterpillars — every 100 ticks, 15% chance (moths lay eggs periodically)
     *   Bees        — every 150 ticks, 25% chance (stable hive-based population)
     *   Ladybugs    — every 100 ticks, conditional on pest presence
     *                  (attracted by aphid/caterpillar pheromones in real life)
     */
    private void spawnInsectsEcological() {
        // --- Aphids: fast reproducers, most common garden pest ---
        if (currentTick % 60 == 0 && random.nextDouble() < 0.40) {
            Position pos = new Position(random.nextInt(rows), random.nextInt(cols));
            addInsect(new Aphid(pos));
        }

        // --- Caterpillars: periodic moth egg-laying ---
        if (currentTick % 100 == 0 && random.nextDouble() < 0.15) {
            Position pos = new Position(random.nextInt(rows), random.nextInt(cols));
            addInsect(new Caterpillar(pos));
        }

        // --- Bees: stable pollinator population, slow arrival ---
        if (currentTick % 150 == 0 && random.nextDouble() < 0.25) {
            Position pos = new Position(random.nextInt(rows), random.nextInt(cols));
            addInsect(new Bee(pos));
        }

        // --- Ladybugs: biological control, attracted by pest presence ---
        // Only spawn when pests exist — models real pheromone-based attraction.
        // Higher pest count increases spawn probability (more food = more attractive).
        if (currentTick % 100 == 0) {
            long pestCount = insects.stream()
                    .filter(Insect::isAlive)
                    .filter(i -> i.getType() == Insect.InsectType.PEST)
                    .count();

            if (pestCount > 0) {
                // Base 20% chance, scaling up to 60% when heavily infested (10+ pests)
                double spawnChance = Math.min(0.60, 0.20 + pestCount * 0.04);
                if (random.nextDouble() < spawnChance) {
                    Position pos = new Position(random.nextInt(rows), random.nextInt(cols));
                    addInsect(new Ladybug(pos));
                    GardenLogger.getInstance().log("INSECT",
                            String.format("Ladybug attracted to garden by %d pest(s) (spawn chance: %.0f%%)",
                                    pestCount, spawnChance * 100));
                }
            }
        }
    }

    private void logPeriodicStatus() {
        long alivePlants = plants.stream().filter(Plant::isAlive).count();
        long aliveInsects = insects.stream().filter(Insect::isAlive).count();
        long alivePests = insects.stream()
                .filter(Insect::isAlive)
                .filter(i -> i.getType() == Insect.InsectType.PEST)
                .count();

        GardenLogger.getInstance().log("GARDEN",
                String.format("--- TICK %d STATUS | Temp: %.1f°F | Light: %.0f | Humidity: %.0f%% | " +
                                "Plants: %d/%d alive | Insects: %d (%d pests) ---",
                        currentTick, currentTemperature, currentLightLevel, currentHumidity,
                        alivePlants, plants.size(), aliveInsects, alivePests));
    }

    private double getAveragePlantWaterLevel() {
        return plants.stream()
                .filter(Plant::isAlive)
                .mapToDouble(Plant::getWaterLevel)
                .average()
                .orElse(50.0);
    }

    // --- Entity Management ---
    public void addPlant(Plant plant) {
        if (!isPositionOccupied(plant.getPosition())) {
            plants.add(plant);
            markOccupied(plant.getPosition());
        } else {
            GardenLogger.getInstance().logWarning("GARDEN", 
                "Failed to add plant at " + plant.getPosition() + " - Position already occupied.");
        }
    }

    /**
     * Moves a plant to a new position, updating the occupancy grid.
     * Returns false if the target cell is already occupied by something else.
     */
    public boolean movePlant(Plant plant, Position newPos) {
        if (newPos.equals(plant.getPosition())) return false;
        if (isPositionOccupied(newPos)) return false;
        Position oldPos = plant.getPosition();
        occupancyGrid[oldPos.getRow()][oldPos.getCol()] = false;
        plant.setPosition(newPos);
        occupancyGrid[newPos.getRow()][newPos.getCol()] = true;
        GardenLogger.getInstance().log("GARDEN",
                String.format("%s moved from %s to %s", plant.getName(), oldPos, newPos));
        return true;
    }

    /**
     * Factory: creates a new plant of the given type at the given position.
     * Used by the UI seed tray to instantiate plants on demand.
     * Returns null if the type is unrecognised.
     */
    public Plant createPlant(String type, Position pos) {
        return switch (type.toLowerCase()) {
            case "tomato"    -> new Tomato(pos);
            case "rose"      -> new Rose(pos);
            case "sunflower" -> new Sunflower(pos);
            case "carrot"    -> new Carrot(pos);
            case "lettuce"   -> new Lettuce(pos);
            case "cactus"    -> {
                Cactus c = new Cactus(pos);
                c.water(30); // pre-hydrate like the original setup
                yield c;
            }
            default -> null;
        };
    }

    public void addInsect(Insect insect) {
        insects.add(insect);
    }

    public void addSensor(Sensor sensor) {
        if (!isPositionOccupied(sensor.getPosition())) {
            sensors.add(sensor);
            markOccupied(sensor.getPosition());
        } else {
            GardenLogger.getInstance().logWarning("GARDEN", 
                "Failed to add sensor at " + sensor.getPosition() + " - Position already occupied.");
        }
    }

    // --- Environmental Adjustments (used by modules) ---
    public void adjustTemperature(double delta) {
        currentTemperature += delta;
    }

    /** Force temperature to a fixed value for the current day (used by GardenSimulationAPI). */
    public void setTemperatureOverride(double tempF) {
        this.temperatureOverrideValue = Math.max(40, Math.min(120, tempF));
        this.temperatureOverrideActive = true;
    }

    public void setRainDisabled(boolean disabled) { this.rainDisabled = disabled; }

    /** Remove temperature override and resume natural seasonal cycle. */
    public void clearTemperatureOverride() {
        this.temperatureOverrideActive = false;
    }

    public void adjustLightLevel(double delta) {
        currentLightLevel = Math.max(0, Math.min(100, currentLightLevel + delta));
    }

    // --- Getters ---
    public String getName() { return name; }
    public int getRows() { return rows; }
    public int getCols() { return cols; }
    public List<Plant> getPlants() { return plants; }
    public List<Insect> getInsects() { return insects; }
    public List<Sensor> getSensors() { return sensors; }
    public List<GardenModule> getModules() { return modules; }
    public double getCurrentTemperature() { return currentTemperature; }
    public double getCurrentLightLevel() { return currentLightLevel; }
    public double getCurrentHumidity() { return currentHumidity; }
    public int getCurrentTick() { return currentTick; }
    public boolean isRaining() { return isRaining; }
    public double getRainIntensity() { return rainIntensity; }

    public WateringSystem getWateringSystem() { return wateringSystem; }
    public HeatingSystem getHeatingSystem() { return heatingSystem; }
    public PestControl getPestControl() { return pestControl; }
    public LightingSystem getLightingSystem() { return lightingSystem; }
}
