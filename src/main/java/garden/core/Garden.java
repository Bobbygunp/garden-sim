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

    private final String name;
    private final int rows;
    private final int cols;

    // Collections of garden entities
    private final List<Plant> plants;
    private final List<Insect> insects;
    private final List<Sensor> sensors;
    private final List<GardenModule> modules;

    // Environmental state
    private double currentTemperature;  // °F
    private double currentLightLevel;   // 0-100
    private double currentHumidity;     // 0-100%
    private int currentTick;
    private int dayNightCycle;          // ticks per full day

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
        // Placed in 'Lanes' at intervals to allow maintenance access and clear coverage
        wateringSystem = new WateringSystem();
        
        // 3 Irrigation Lanes (Columns 4, 10, 16)
        int[] lanes = {4, 10, 16};
        for (int col : lanes) {
            for (int row = 3; row < rows; row += 6) {
                Position sprPos = new Position(row, col);
                wateringSystem.addSprinkler(sprPos, 7.5, 8.0);
                
                // Place a moisture sensor adjacent to each sprinkler for localized feedback
                addSensor(new MoistureSensor(new Position(row + 1, col)));
            }
        }
        modules.add(wateringSystem);

        // Environmental Monitoring (Corners and Center)
        addSensor(new TemperatureSensor(new Position(0, 0)));
        addSensor(new TemperatureSensor(new Position(rows - 1, cols - 1)));
        addSensor(new TemperatureSensor(new Position(rows / 2, cols / 2)));
        addSensor(new LightSensor(new Position(0, cols / 2)));
        addSensor(new LightSensor(new Position(rows - 1, 0)));

        // 2. PLANTING LAYER (Structured Crop Rows)
        // Plants are organized by species into dedicated 'Beds'
        
        // Bed 1: Tomatoes (Rows 1-2)
        for (int c = 1; c < cols - 1; c += 3) {
            if (c != 4 && c != 10 && c != 16) { // Skip irrigation lanes
                addPlant(new Tomato(new Position(1, c)));
                addPlant(new Tomato(new Position(2, c)));
            }
        }

        // Bed 2: Roses (Rows 5-6)
        for (int c = 1; c < cols - 1; c += 3) {
            if (c != 4 && c != 10 && c != 16) {
                addPlant(new Rose(new Position(5, c)));
                addPlant(new Rose(new Position(6, c)));
            }
        }

        // Bed 3: Sunflowers (Rows 8-10)
        for (int c = 2; c < cols - 1; c += 4) {
            if (c != 4 && c != 10 && c != 16) {
                addPlant(new Sunflower(new Position(9, c)));
            }
        }

        // Bed 4: Carrots & Lettuce (Rows 12-15)
        for (int c = 1; c < cols - 1; c += 2) {
            if (c != 4 && c != 10 && c != 16) {
                addPlant(new Carrot(new Position(13, c)));
                addPlant(new Lettuce(new Position(14, c)));
            }
        }

        // Bed 5: Cacti (The 'Dry Row' at the edge)
        for (int c = 1; c < cols; c += 5) {
            if (c != 4 && c != 10 && c != 16) {
                addPlant(new Cactus(new Position(18, c)));
            }
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

        GardenLogger.getInstance().log("GARDEN",
                String.format("Garden initialized: %d plants, %d insects, %d sensors, %d modules",
                        plants.size(), insects.size(), sensors.size(), modules.size()));
        GardenLogger.getInstance().log("GARDEN", "=== Garden Ready ===");
    }

    /**
     * Called every simulation tick. Updates environment, sensors, plants,
     * insects, and modules.
     */
    public void tick() {
        try {
            currentTick++;

            // 1. Update environment (day/night cycle, weather variations)
            updateEnvironment();

            // 2. Update all sensors
            List<Plant> alivePlants = plants.stream().filter(Plant::isAlive).toList();
            for (Sensor sensor : sensors) {
                if (sensor instanceof TemperatureSensor) {
                    sensor.update(currentTemperature);
                } else if (sensor instanceof MoistureSensor ms) {
                    ms.update(alivePlants); // Realistic local measurement
                } else if (sensor instanceof LightSensor) {
                    sensor.update(currentLightLevel);
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

        } catch (Exception e) {
            GardenLogger.getInstance().logError("GARDEN",
                    "Error during tick " + currentTick, e);
            // Garden must not crash - continue operating
        }
    }

    // Slow-moving weather pattern that shifts over multiple days.
    // Creates realistic multi-day wet/dry and cloudy/clear spells.
    private double weatherHumidityBias = 0;  // -15 (dry spell) to +15 (wet spell)
    private double weatherCloudCover = 0;    // 0 (clear) to 0.5 (overcast)

    private void updateEnvironment() {
        double dayProgress = (currentTick % dayNightCycle) / (double) dayNightCycle;

        // --- Slowly-drifting weather patterns (change over ~5 days / 1000 ticks) ---
        if (currentTick % 50 == 0) {
            weatherHumidityBias += (random.nextGaussian() * 1.5);
            weatherHumidityBias = Math.max(-15, Math.min(15, weatherHumidityBias));
            weatherCloudCover += (random.nextGaussian() * 0.04);
            weatherCloudCover = Math.max(0, Math.min(0.5, weatherCloudCover));
        }

        // --- Light: day/night cycle, reduced by cloud cover ---
        double naturalLight = 50 + 45 * Math.sin(dayProgress * 2 * Math.PI - Math.PI / 2);
        naturalLight *= (1.0 - weatherCloudCover); // clouds reduce light
        currentLightLevel = Math.max(0, Math.min(100, naturalLight));

        // --- Temperature: day/night cycle with small random drift ---
        double tempVariation = 7 * Math.sin(dayProgress * 2 * Math.PI - Math.PI / 2);
        double baseTemp = 65 + (random.nextGaussian() * 0.5);
        currentTemperature = baseTemp + tempVariation;

        // --- Humidity: realistic day/night swing + weather patterns ---
        // Real gardens: nighttime humidity rises sharply (dew/fog can reach 90-95%),
        // daytime drops as temperature rises (can hit 25-35% on hot dry days).
        // The day/night swing is large (~30 percentage points) and is further
        // shifted by the multi-day weather bias.
        double dayNightHumiditySwing = 18 * Math.sin(dayProgress * 2 * Math.PI + Math.PI / 2);
        // Positive at night (high humidity), negative during day (low humidity)
        double baseHumidity = 58 + dayNightHumiditySwing + weatherHumidityBias;
        currentHumidity = Math.max(15, Math.min(98,
                baseHumidity + random.nextGaussian() * 3));
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
        plants.add(plant);
    }

    public void addInsect(Insect insect) {
        insects.add(insect);
    }

    public void addSensor(Sensor sensor) {
        sensors.add(sensor);
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
