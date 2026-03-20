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

    private final List<Plant> plants;
    private final List<Insect> insects;
    private final List<Sensor> sensors;
    private final List<GardenModule> modules;

    private final boolean[][] occupancyGrid;

    private double currentTemperature;
    private double currentLightLevel;
    private double currentHumidity;
    private int currentTick;
    private int dayNightCycle;
    
    private Season currentSeason = Season.SPRING;
    private boolean isRaining = false;
    private double rainIntensity = 0.0;
    private final int ticksPerSeason = 1000;

    private WateringSystem wateringSystem;
    private HeatingSystem heatingSystem;
    private PestControl pestControl;
    private LightingSystem lightingSystem;

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
        this.dayNightCycle = 200;

        GardenLogger.getInstance().log("GARDEN",
                String.format("Garden '%s' created: %dx%d grid", name, rows, cols));
    }

    /**
     * Initialize the garden with a realistic, industrial-grade layout.
     */
    public void initializeDefaultGarden() {
        GardenLogger.getInstance().log("GARDEN", "=== Initializing Professional Garden Layout ===");

        wateringSystem = new WateringSystem();
        
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

        MoistureSensor ms2 = new MoistureSensor(new Position(7, 8));
        ms2.setSensingRange(5, 6, 0, cols - 1);
        addSensor(ms2);
        Position spr4 = new Position(7, 4);
        Position spr5 = new Position(7, 12);
        wateringSystem.addSprinkler(spr4, 8.0, 1.0, 25.0, 65.0).linkSensor(ms2);
        wateringSystem.addSprinkler(spr5, 8.0, 1.0, 25.0, 65.0).linkSensor(ms2);
        markOccupied(spr4); markOccupied(spr5);

        MoistureSensor ms3 = new MoistureSensor(new Position(10, 11));
        ms3.setSensingRange(9, 9, 0, cols - 1);
        addSensor(ms3);
        Position spr6 = new Position(10, 7);
        Position spr7 = new Position(10, 14);
        wateringSystem.addSprinkler(spr6, 8.0, 2.5, 30.0, 70.0).linkSensor(ms3);
        wateringSystem.addSprinkler(spr7, 8.0, 2.5, 30.0, 70.0).linkSensor(ms3);
        markOccupied(spr6); markOccupied(spr7);

        
        MoistureSensor ms4_carrot = new MoistureSensor(new Position(12, 10));
        ms4_carrot.setSensingRange(13, 13, 0, cols - 1);
        addSensor(ms4_carrot);
        Position spr8 = new Position(13, 4);
        Position spr9 = new Position(13, 10);
        wateringSystem.addSprinkler(spr8, 4.0, 1.0, 25.0, 60.0).linkSensor(ms4_carrot);
        wateringSystem.addSprinkler(spr9, 4.0, 1.0, 25.0, 60.0).linkSensor(ms4_carrot);
        markOccupied(spr8); markOccupied(spr9);

        MoistureSensor ms4_lettuce = new MoistureSensor(new Position(15, 10));
        ms4_lettuce.setSensingRange(14, 14, 0, cols - 1);
        addSensor(ms4_lettuce);
        Position spr10 = new Position(13, 16);
        wateringSystem.addSprinkler(spr10, 8.0, 3.0, 40.0, 75.0).linkSensor(ms4_lettuce);
        markOccupied(spr10);


        MoistureSensor ms5 = new MoistureSensor(new Position(18, 9));
        ms5.setSensingRange(18, 18, 0, cols - 1);
        addSensor(ms5);
        Position spr11 = new Position(18, 4);
        Position spr12 = new Position(18, 15);
        wateringSystem.addSprinkler(spr11, 4.0, 1.0, 10.0, 35.0).linkSensor(ms5);
        wateringSystem.addSprinkler(spr12, 4.0, 1.0, 10.0, 35.0).linkSensor(ms5);
        markOccupied(spr11); markOccupied(spr12);

        modules.add(wateringSystem);

        addSensor(new TemperatureSensor(new Position(0, 0)));
        addSensor(new TemperatureSensor(new Position(0, cols - 1)));
        addSensor(new TemperatureSensor(new Position(rows - 1, 0)));
        addSensor(new TemperatureSensor(new Position(rows - 1, cols - 1)));
        addSensor(new TemperatureSensor(new Position(rows / 2, cols / 2)));

        TemperatureSensor ts1 = new TemperatureSensor(new Position(3, 13));
        ts1.setSensingRange(1, 2, 0, cols - 1);
        addSensor(ts1);

        TemperatureSensor ts2 = new TemperatureSensor(new Position(7, 1));
        ts2.setSensingRange(5, 6, 0, cols - 1);
        addSensor(ts2);

        TemperatureSensor ts3 = new TemperatureSensor(new Position(10, 1));
        ts3.setSensingRange(9, 9, 0, cols - 1);
        addSensor(ts3);

        TemperatureSensor ts4 = new TemperatureSensor(new Position(12, 1));
        ts4.setSensingRange(13, 13, 0, cols - 1);
        addSensor(ts4);

        TemperatureSensor ts5 = new TemperatureSensor(new Position(15, 1));
        ts5.setSensingRange(14, 14, 0, cols - 1);
        addSensor(ts5);

        TemperatureSensor ts6 = new TemperatureSensor(new Position(18, 2));
        ts6.setSensingRange(18, 18, 0, cols - 1);
        addSensor(ts6);
        
        addSensor(new LightSensor(new Position(0, 5)));
        addSensor(new LightSensor(new Position(0, cols / 2)));
        addSensor(new LightSensor(new Position(0, cols - 6)));
        addSensor(new LightSensor(new Position(rows - 1, 5)));
        addSensor(new LightSensor(new Position(rows - 1, cols / 2)));
        addSensor(new LightSensor(new Position(rows - 1, cols - 6)));
        addSensor(new LightSensor(new Position(rows / 2, 13)));

        addInsect(new Bee(new Position(4, 4)));
        addInsect(new Bee(new Position(8, 8)));
        addInsect(new Bee(new Position(6, 16)));
        addInsect(new Ladybug(new Position(6, 6)));
        addInsect(new Aphid(new Position(3, 5)));
        addInsect(new Caterpillar(new Position(11, 3)));

        heatingSystem = new HeatingSystem();
        
        heatingSystem.setTemperatureAdjustRate(0.5);
        heatingSystem.addZone(new Position(3, 13), 70.0).linkSensor(ts1);
        heatingSystem.addZone(new Position(7, 1), 67.0).linkSensor(ts2);
        heatingSystem.addZone(new Position(10, 1), 70.0).linkSensor(ts3);
        heatingSystem.addZone(new Position(12, 1), 62.0).linkSensor(ts4);
        heatingSystem.addZone(new Position(15, 1), 58.0).linkSensor(ts5);
        heatingSystem.addZone(new Position(18, 2), 72.0).linkSensor(ts6);
        modules.add(heatingSystem);

        pestControl = new PestControl();
        modules.add(pestControl);

        lightingSystem = new LightingSystem();
        modules.add(lightingSystem);

       
        FertigationSystem fertigationSystem = new FertigationSystem();
        fertigationSystem.addZone(new FertigationSystem.FertigationZone(
            "Tomatoes", 1, 2, 0, cols - 1, 40.0, 85.0, 10.0));
        fertigationSystem.addZone(new FertigationSystem.FertigationZone(
            "Roses", 5, 6, 0, cols - 1, 40.0, 85.0, 8.0));
        fertigationSystem.addZone(new FertigationSystem.FertigationZone(
            "Sunflowers", 9, 9, 0, cols - 1, 40.0, 85.0, 10.0));
        fertigationSystem.addZone(new FertigationSystem.FertigationZone(
            "Carrots", 13, 13, 0, cols - 1, 40.0, 85.0, 6.0));
        fertigationSystem.addZone(new FertigationSystem.FertigationZone(
            "Lettuce", 14, 14, 0, cols - 1, 40.0, 85.0, 5.0));
        fertigationSystem.addZone(new FertigationSystem.FertigationZone(
            "Cacti", 18, 18, 0, cols - 1, 20.0, 55.0, 2.0));
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

            updateEnvironment();

            if (isRaining && !rainDisabled) {
                for (Plant plant : plants) {
                    if (plant.isAlive()) {
                        plant.water(rainIntensity * 0.05, true);
                    }
                }
            }

            List<Plant> alivePlants = plants.stream().filter(Plant::isAlive).toList();
            for (Sensor sensor : sensors) {
                if (sensor instanceof TemperatureSensor) {
                    sensor.update(currentTemperature, currentTick);
                } else if (sensor instanceof MoistureSensor ms) {
                    ms.update(alivePlants, currentTick);
                } else if (sensor instanceof LightSensor) {
                    sensor.update(currentLightLevel, currentTick);
                }
            }

            for (GardenModule module : modules) {
                module.update(this);
            }

            for (Plant plant : plants) {
                double plantTemp = (heatingSystem != null)
                        ? heatingSystem.getZoneTemperatureAt(plant.getPosition(), currentTemperature)
                        : currentTemperature;
                plant.update(plantTemp, currentLightLevel, currentHumidity);
            }

            List<Plant> alivePlantsList = plants.stream()
                    .filter(Plant::isAlive)
                    .toList();
            for (Insect insect : insects) {
                insect.update(alivePlantsList, rows, cols);
            }

    
            List<Insect> aliveInsects = insects.stream()
                    .filter(Insect::isAlive).toList();
            for (Insect insect : aliveInsects) {
                insect.predateInsects(aliveInsects, 2.0, 0.3);
            }

            spawnInsectsEcological();

            if (currentTick % 200 == 0) {
                insects.removeIf(i -> !i.isAlive());
            }

            if (currentTick % 50 == 0) {
                logPeriodicStatus();
            }

        } catch (Exception e) {
            GardenLogger.getInstance().logError("GARDEN",
                    "Error during tick " + currentTick, e);
        }
    }

    private void updateEnvironment() {
        Season oldSeason = currentSeason;
        int seasonIndex = (currentTick / ticksPerSeason) % 4;
        currentSeason = Season.values()[seasonIndex];

        if (currentSeason != oldSeason) {
            GardenLogger.getInstance().log("GARDEN", "=== SEASON CHANGE: Welcome to " + currentSeason + " ===");
        }

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

        double dayProgress = (currentTick % dayNightCycle) / (double) dayNightCycle;
        double sunEffect = Math.sin(dayProgress * 2 * Math.PI - Math.PI / 2);

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

        double naturalLight = Math.max(0, maxLight * sunEffect);
        if (isRaining) naturalLight *= 0.5;
        currentLightLevel = naturalLight;

        double targetAmbientTemp = baseTemp + (10.0 * sunEffect);


        if (temperatureOverrideActive) {
            targetAmbientTemp = temperatureOverrideValue;
        }

        if (currentTemperature < targetAmbientTemp) {
            currentTemperature = Math.min(targetAmbientTemp, currentTemperature + 0.2);
        } else if (currentTemperature > targetAmbientTemp) {
            currentTemperature = Math.max(targetAmbientTemp, currentTemperature - 0.2);
        }
        currentTemperature += (random.nextGaussian() * 0.1);

        double targetHumidity = isRaining ? 85.0 : 40.0;
        targetHumidity += 20.0 * (-sunEffect); 
        targetHumidity = Math.max(20, Math.min(100, targetHumidity));

        if (currentHumidity < targetHumidity) {
            currentHumidity = Math.min(targetHumidity, currentHumidity + 0.5);
        } else if (currentHumidity > targetHumidity) {
            currentHumidity = Math.max(targetHumidity, currentHumidity - 0.5);
        }
    }

    
    private void spawnInsectsEcological() {
        if (currentTick % 60 == 0 && random.nextDouble() < 0.40) {
            Position pos = new Position(random.nextInt(rows), random.nextInt(cols));
            addInsect(new Aphid(pos));
        }

        if (currentTick % 100 == 0 && random.nextDouble() < 0.15) {
            Position pos = new Position(random.nextInt(rows), random.nextInt(cols));
            addInsect(new Caterpillar(pos));
        }

        if (currentTick % 150 == 0 && random.nextDouble() < 0.25) {
            Position pos = new Position(random.nextInt(rows), random.nextInt(cols));
            addInsect(new Bee(pos));
        }

        if (currentTick % 100 == 0) {
            long pestCount = insects.stream()
                    .filter(Insect::isAlive)
                    .filter(i -> i.getType() == Insect.InsectType.PEST)
                    .count();

            if (pestCount > 0) {
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

    
    public Plant createPlant(String type, Position pos) {
        return switch (type.toLowerCase()) {
            case "tomato"    -> new Tomato(pos);
            case "rose"      -> new Rose(pos);
            case "sunflower" -> new Sunflower(pos);
            case "carrot"    -> new Carrot(pos);
            case "lettuce"   -> new Lettuce(pos);
            case "cactus"    -> {
                Cactus c = new Cactus(pos);
                c.water(30);
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
