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
        Position spr1 = new Position(3, 4);
        Position spr2 = new Position(3, 10);
        Position spr3 = new Position(3, 16);
        wateringSystem.addSprinkler(spr1, 8.0, 8.0, 40.0, 80.0);
        wateringSystem.addSprinkler(spr2, 8.0, 8.0, 40.0, 80.0);
        wateringSystem.addSprinkler(spr3, 8.0, 8.0, 40.0, 80.0);
        markOccupied(spr1); markOccupied(spr2); markOccupied(spr3);
        
        // Single powerful sensor covering exactly the Tomato Zone (Rows 1-2)
        MoistureSensor ms1 = new MoistureSensor(new Position(3, 7));
        ms1.setSensingRange(1, 2, 0, cols - 1);
        addSensor(ms1);

        // --- Zone 2: Roses (Very High Moisture) ---
        // Thresholds: 50% to 90%
        // Hardware moved to Row 7 (Plants are in Rows 5-6)
        Position spr4 = new Position(7, 4);
        Position spr5 = new Position(7, 12);
        wateringSystem.addSprinkler(spr4, 8.0, 8.0, 50.0, 90.0);
        wateringSystem.addSprinkler(spr5, 8.0, 8.0, 50.0, 90.0);
        markOccupied(spr4); markOccupied(spr5);
        MoistureSensor ms2 = new MoistureSensor(new Position(7, 8));
        ms2.setSensingRange(5, 6, 0, cols - 1);
        addSensor(ms2);

        // --- Zone 3: Sunflowers (Moderate Moisture) ---
        // Thresholds: 30% to 70%
        // Hardware moved to Row 10 (Plants are in Row 9)
        Position spr6 = new Position(10, 7);
        Position spr7 = new Position(10, 14);
        wateringSystem.addSprinkler(spr6, 8.0, 8.0, 30.0, 70.0);
        wateringSystem.addSprinkler(spr7, 8.0, 8.0, 30.0, 70.0);
        markOccupied(spr6); markOccupied(spr7);
        MoistureSensor ms3 = new MoistureSensor(new Position(10, 11));
        ms3.setSensingRange(9, 9, 0, cols - 1);
        addSensor(ms3);

        // --- Zone 4: Carrots & Lettuce (Consistent Moisture) ---
        // Thresholds: 45% to 75%
        // Hardware moved to Row 15 (Plants are in Rows 13-14)
        Position spr8 = new Position(15, 4);
        Position spr9 = new Position(15, 10);
        Position spr10 = new Position(15, 16);
        wateringSystem.addSprinkler(spr8, 8.0, 8.0, 45.0, 75.0);
        wateringSystem.addSprinkler(spr9, 8.0, 8.0, 45.0, 75.0);
        wateringSystem.addSprinkler(spr10, 8.0, 8.0, 45.0, 75.0);
        markOccupied(spr8); markOccupied(spr9); markOccupied(spr10);
        MoistureSensor ms4 = new MoistureSensor(new Position(15, 7));
        ms4.setSensingRange(13, 14, 0, cols - 1);
        addSensor(ms4);

        // --- Zone 5: Cacti (Arid/Dry Zone) ---
        // Thresholds: 5% to 15% (Extremely low to prevent overwatering)
        // Hardware moved to Row 19 (Plants are in Row 18)
        Position spr11 = new Position(19, 10);
        wateringSystem.addSprinkler(spr11, 8.0, 2.0, 5.0, 15.0);
        markOccupied(spr11);
        MoistureSensor ms5 = new MoistureSensor(new Position(19, 13));
        ms5.setSensingRange(18, 18, 0, cols - 1);
        addSensor(ms5);

        modules.add(wateringSystem);

        // Environmental Monitoring (Empty Rows 0 and 19)
        addSensor(new TemperatureSensor(new Position(0, 0)));
        addSensor(new TemperatureSensor(new Position(0, cols - 1)));
        addSensor(new TemperatureSensor(new Position(rows - 1, 0)));
        addSensor(new TemperatureSensor(new Position(rows - 1, cols - 1)));
        addSensor(new TemperatureSensor(new Position(rows / 2, cols / 2)));
        
        addSensor(new LightSensor(new Position(0, 5)));
        addSensor(new LightSensor(new Position(0, cols / 2)));
        addSensor(new LightSensor(new Position(0, cols - 6)));
        addSensor(new LightSensor(new Position(rows - 1, 5)));
        addSensor(new LightSensor(new Position(rows - 1, cols / 2)));
        addSensor(new LightSensor(new Position(rows - 1, cols - 6)));
        addSensor(new LightSensor(new Position(rows / 2, 13)));

        // 2. PLANTING LAYER (Structured Crop Rows)
        
        // Bed 1: Tomatoes (Rows 1-2)
        for (int c = 1; c < cols - 1; c += 3) {
            addPlant(new Tomato(new Position(1, c)));
            addPlant(new Tomato(new Position(2, c)));
        }

        // Bed 2: Roses (Rows 5-6)
        for (int c = 1; c < cols - 1; c += 3) {
            addPlant(new Rose(new Position(5, c)));
            addPlant(new Rose(new Position(6, c)));
        }

        // Bed 3: Sunflowers (Rows 8-10)
        for (int c = 2; c < cols - 1; c += 4) {
            addPlant(new Sunflower(new Position(9, c)));
        }

        // Bed 4: Carrots & Lettuce (Rows 12-15)
        for (int c = 1; c < cols - 1; c += 2) {
            addPlant(new Carrot(new Position(13, c)));
            addPlant(new Lettuce(new Position(14, c)));
        }

        // Bed 5: Cacti (The 'Dry Row' at the edge)
        for (int c = 1; c < cols; c += 5) {
            addPlant(new Cactus(new Position(18, c)));
        }

        // --- Add Insects ---
        addInsect(new Bee(new Position(4, 4)));
        addInsect(new Bee(new Position(8, 8)));
        addInsect(new Bee(new Position(6, 16)));
        addInsect(new Ladybug(new Position(6, 6)));
        addInsect(new Aphid(new Position(3, 5)));
        addInsect(new Caterpillar(new Position(11, 3)));

        // --- Other Modules ---
        heatingSystem = new HeatingSystem();
        heatingSystem.setTargetTemperature(65.0);
        heatingSystem.setTemperatureAdjustRate(2.0);
        modules.add(heatingSystem);

        pestControl = new PestControl();
        modules.add(pestControl);

        lightingSystem = new LightingSystem();
        modules.add(lightingSystem);

        FertigationSystem fertigationSystem = new FertigationSystem();
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
            if (isRaining) {
                for (Plant plant : plants) {
                    if (plant.isAlive()) {
                        // Rain intensity is 0.0 to 5.0; we apply a portion each tick
                        plant.water(rainIntensity * 0.5, false);
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

            // 4. Update all plants (with temperature, light, and humidity)
            for (Plant plant : plants) {
                plant.update(currentTemperature, currentLightLevel, currentHumidity);
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
            if (currentTick >= 2000) {
                GardenLogger.getInstance().log("GARDEN", "Simulation complete at 2000 ticks. Exiting simulation.");
                System.exit(0);
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

        if (!isRaining && random.nextDouble() < rainChance) {
            isRaining = true;
            rainIntensity = 1.0 + random.nextDouble() * 4.0;
            GardenLogger.getInstance().log("WEATHER", String.format("It started raining! Intensity: %.1f", rainIntensity));
        } else if (isRaining && random.nextDouble() < 0.02) { // 2% chance per tick to stop
            isRaining = false;
            GardenLogger.getInstance().log("WEATHER", "The rain has stopped.");
        }

        // --- 3. Light & Temperature Calculations ---
        double dayProgress = (currentTick % dayNightCycle) / (double) dayNightCycle;

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

        // Day/Night Variation (Sine wave)
        double sunEffect = Math.sin(dayProgress * 2 * Math.PI - Math.PI / 2);
        
        // Light: Daytime peak, nighttime zero. Reduced by 50% if raining.
        double naturalLight = Math.max(0, maxLight * sunEffect);
        if (isRaining) naturalLight *= 0.5;
        currentLightLevel = naturalLight;

        // Temperature: Seasonal base + day/night swing + random drift
        double tempSwing = 10.0 * sunEffect;
        currentTemperature = baseTemp + tempSwing + (random.nextGaussian() * 2.0);

        // Humidity: Higher at night, higher when raining
        double baseHumidity = isRaining ? 85.0 : 40.0;
        double humidSwing = 20.0 * (-sunEffect); // Humidity rises at night
        currentHumidity = Math.max(20, Math.min(100, baseHumidity + humidSwing + (random.nextGaussian() * 5.0)));
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

    public WateringSystem getWateringSystem() { return wateringSystem; }
    public HeatingSystem getHeatingSystem() { return heatingSystem; }
    public PestControl getPestControl() { return pestControl; }
    public LightingSystem getLightingSystem() { return lightingSystem; }
}
