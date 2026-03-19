package garden.api;

import garden.core.Garden;
import garden.logging.GardenLogger;
import garden.model.insects.*;
import garden.model.plants.*;
import garden.util.Position;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Public API for the automated garden simulation.
 * Implements the interface described in "Gardening System APIs.pdf".
 *
 * The garden runs continuously in a background thread after initializeGarden() is called.
 * The TA's script injects events (rain/temperature/parasite) and sleeps 1 real hour
 * between each event. The garden processes ticks autonomously during that hour.
 *
 * Tick rate: 18 seconds per tick  →  200 ticks = 1 real hour = 1 simulated day.
 *
 * Usage (mirrors TA pseudo-code):
 *   GardenSimulationAPI api = new GardenSimulationAPI();
 *   api.initializeGarden();           // starts background tick thread
 *   api.rain(10);
 *   Thread.sleep(3_600_000);          // TA's sleepOneHour() — garden runs on its own
 *   api.temperature(85);
 *   Thread.sleep(3_600_000);
 *   // ... 24 hours total ...
 *   api.getState();
 */
public class GardenSimulationAPI {

    private static final int  TICKS_PER_DAY       = 200;
    private static final long REAL_TIME_TICK_MS   = 18_000L; // 18s/tick → 200 ticks = 1 real hour
    private static final long QUICK_MODE_TICK_MS  = 50L;     // 50ms/tick → 200 ticks = 10 seconds

    private long tickIntervalMs = REAL_TIME_TICK_MS; // default: real-time

    /** Call before initializeGarden() to run at 50ms/tick (200 ticks = ~10 seconds per day). */
    public void setQuickMode(boolean quick) {
        this.tickIntervalMs = quick ? QUICK_MODE_TICK_MS : REAL_TIME_TICK_MS;
    }

    /**
     * Blocks until the background thread completes the current simulated day.
     * Used by GardenSimulator in quick mode so events are never injected mid-day.
     */
    public void awaitDayEnd() {
        int targetDay = currentDay + 1;
        while (running && currentDay < targetDay) {
            try { Thread.sleep(50); } catch (InterruptedException e) { break; }
        }
    }
    private static final String CONFIG_PATH = "config/garden_config.json";
    private static final String LOG_PATH    = "logs/log.txt";

    private Garden  garden;
    private int     currentDay       = 0;
    private int     dayTickCounter   = 0;   // ticks elapsed in the current simulated day

    // Background simulation thread
    private Thread  simThread;
    private volatile boolean running = false;

    // Track the last event each day for getState() logging
    private volatile String lastEvent      = "none";
    private volatile String lastEventValue = "none";

    // Rain state — applied per-tick, auto-resets after TICKS_PER_DAY ticks
    private volatile double  rainWaterPerTick = 0.0;
    private volatile boolean rainActive       = false;

    // -------------------------------------------------------------------------
    // Public API Methods
    // -------------------------------------------------------------------------

    /**
     * Initialises the garden from the config file and starts the background
     * simulation thread. Must be called first.
     * Guarantees >= 10 alive plants covering all varieties.
     */
    public void initializeGarden() {
        // Stop any previously running simulation
        stopSimulation();

        garden       = new Garden("API Garden", 20, 20);
        currentDay   = 0;
        dayTickCounter = 0;
        lastEvent    = "init";
        lastEventValue = "none";
        rainActive   = false;
        rainWaterPerTick = 0;

        // Clear log.txt so each new run starts with a fresh log
        new File("logs").mkdirs();
        try (FileWriter fw = new FileWriter(LOG_PATH, StandardCharsets.UTF_8, false)) {
            // truncate only
        } catch (IOException e) {
            System.err.println("[API] Warning: could not clear " + LOG_PATH + ": " + e.getMessage());
        }

        garden.initializeDefaultGarden();

        // Load config and place plants
        Map<String, Integer> plantCounts = loadConfig();
        for (Map.Entry<String, Integer> entry : plantCounts.entrySet()) {
            placePlants(entry.getKey().toLowerCase(), entry.getValue());
        }

        long alive = garden.getPlants().stream().filter(Plant::isAlive).count();
        GardenLogger.getInstance().log("API",
                String.format("Garden initialised: %d alive plants across %d species.",
                        alive, plantCounts.size()));
        writeApiLog(0, "init", "none", alive);
        System.out.println("[API] Garden initialised — " + alive + " plants alive.");

        // Start background simulation thread
        startSimulation();
    }

    /**
     * Returns a map of all alive plant data:
     *   "plants"           -> List<String>       — plant names
     *   "waterRequirement" -> List<Integer>       — daily water need per plant
     *   "parasites"        -> List<List<String>>  — parasites each plant is vulnerable to
     */
    public Map<String, Object> getPlants() {
        List<String>       names     = new ArrayList<>();
        List<Integer>      water     = new ArrayList<>();
        List<List<String>> parasites = new ArrayList<>();

        for (Plant p : garden.getPlants()) {
            if (!p.isAlive()) continue;
            names.add(p.getName());
            water.add((int) Math.round(p.getWaterNeedPerTick() * 10));
            parasites.add(vulnerableTo(p));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("plants",           names);
        result.put("waterRequirement", water);
        result.put("parasites",        parasites);
        return result;
    }

    /**
     * Simulates rainfall for the current simulated day.
     * Water is applied per-tick by the background thread.
     * Resets automatically after TICKS_PER_DAY ticks (end of current day).
     *
     * @param amount total water units to distribute over the day
     */
    public void rain(int amount) {
        if (amount <= 0) return;
        rainWaterPerTick = amount / (double) TICKS_PER_DAY;
        rainActive       = true;
        lastEvent        = "rain";
        lastEventValue   = String.valueOf(amount);

        long alive = garden.getPlants().stream().filter(Plant::isAlive).count();
        GardenLogger.getInstance().log("API",
                String.format("Day %d — Rain event: %d units (%.3f/tick)", currentDay + 1, amount, rainWaterPerTick));
        writeApiLog(currentDay + 1, "rain", String.valueOf(amount), alive);
        System.out.printf("[API] Day %d — rain(%d)%n", currentDay + 1, amount);
    }

    /**
     * Sets the garden temperature for the current simulated day.
     * Overrides the natural seasonal cycle; resets at end of current day.
     *
     * @param tempF temperature in Fahrenheit (valid range: 40–120)
     */
    public void temperature(int tempF) {
        int clamped = Math.max(40, Math.min(120, tempF));
        garden.setTemperatureOverride(clamped);
        lastEvent      = "temperature";
        lastEventValue = String.valueOf(clamped);

        long alive = garden.getPlants().stream().filter(Plant::isAlive).count();
        GardenLogger.getInstance().log("API",
                String.format("Day %d — Temperature override: %dF", currentDay + 1, clamped));
        writeApiLog(currentDay + 1, "temperature", String.valueOf(clamped), alive);
        System.out.printf("[API] Day %d — temperature(%dF)%n", currentDay + 1, clamped);
    }

    /**
     * Triggers a parasite infestation. Spawns 4 insects of the named type.
     * PestControl responds autonomously; plants heal gradually over time.
     *
     * Accepted names (case-insensitive): aphid, caterpillar, bee, ladybug, insects
     * "insects" spawns a mixed aphid + caterpillar infestation.
     */
    public void parasite(String type) {
        if (type == null || type.isBlank()) return;
        String key = type.strip().toLowerCase();
        lastEvent      = "parasite";
        lastEventValue = key;

        int    spawned = 0;
        Random rng     = new Random();
        List<String> types = key.equals("insects")
                ? List.of("aphid", "caterpillar", "aphid", "caterpillar")
                : Collections.nCopies(4, key);

        for (String t : types) {
            Insect insect = createInsect(t, new Position(1 + rng.nextInt(18), 1 + rng.nextInt(18)));
            if (insect != null) { garden.addInsect(insect); spawned++; }
        }

        long alive = garden.getPlants().stream().filter(Plant::isAlive).count();
        GardenLogger.getInstance().log("API",
                String.format("Day %d — Parasite event: %s (%d insects spawned)", currentDay + 1, key, spawned));
        writeApiLog(currentDay + 1, "parasite", key, alive);
        System.out.printf("[API] Day %d — parasite(\"%s\") — %d insects spawned%n", currentDay + 1, key, spawned);
    }

    /**
     * Logs the current garden state to logs/log.txt.
     * Format: DAY, EVENT, EVENT_VALUE, PLANTS_ALIVE
     * Should be called after 24 simulated days (24 real hours).
     */
    public void getState() {
        stopSimulation();

        long alive = garden.getPlants().stream().filter(Plant::isAlive).count();
        long dead  = garden.getPlants().stream().filter(p -> !p.isAlive()).count();

        writeApiLog(currentDay, lastEvent, lastEventValue, alive);

        StringBuilder sb = new StringBuilder();
        sb.append("\n=== GARDEN STATE — DAY ").append(currentDay).append(" ===\n");
        sb.append(String.format("PLANTS_ALIVE: %d | PLANTS_DEAD: %d%n", alive, dead));
        sb.append(String.format("TEMPERATURE:  %.1fF%n", garden.getCurrentTemperature()));
        sb.append(String.format("HUMIDITY:     %.1f%%%n", garden.getCurrentHumidity()));
        sb.append("PLANT DETAILS:\n");
        for (Plant p : garden.getPlants()) {
            sb.append(String.format("  %-10s | HP: %5.1f | Water: %5.1f | Stage: %-10s | alive=%b%n",
                    p.getName(), p.getHealth(), p.getWaterLevel(),
                    p.getGrowthStage(), p.isAlive()));
        }
        sb.append("=".repeat(40)).append("\n");

        appendToLogFile(sb.toString());
        System.out.println(sb.toString().trim());
    }

    /** Alias — spec pseudo-code uses both spellings. */
    public void getStatus() { getState(); }

    /** Alias — spec pseudo-code uses both spellings. */
    public void parasites(String type) { parasite(type); }

    /** Expose garden for module failure testing. */
    public Garden getGarden() { return garden; }

    /** Public stop for testing. */
    public void stopSimulationPublic() { stopSimulation(); }

    // -------------------------------------------------------------------------
    // Background simulation thread
    // -------------------------------------------------------------------------

    private void startSimulation() {
        running   = true;
        simThread = new Thread(this::simulationLoop, "garden-sim-thread");
        simThread.setDaemon(true);   // JVM exits when main thread finishes
        simThread.start();
        System.out.println("[API] Background simulation thread started " +
                "(tick interval: " + tickIntervalMs + "ms, " +
                TICKS_PER_DAY + " ticks/day = " +
                (tickIntervalMs * TICKS_PER_DAY / 1000) + "s/day).");
    }

    private void stopSimulation() {
        running = false;
        if (simThread != null) {
            simThread.interrupt();
            try { simThread.join(5_000); } catch (InterruptedException ignored) {}
            simThread = null;
        }
    }

    /**
     * Main loop: one tick every TICK_INTERVAL_MS.
     * Rain is applied each tick while active.
     * Rain and temperature overrides reset at the end of each simulated day.
     */
    private void simulationLoop() {
        while (running) {
            try {
                // Apply rain water before tick (cap at 85% to prevent overwatering)
                if (rainActive) {
                    for (Plant p : garden.getPlants()) {
                        if (p.isAlive()) {
                            p.water(rainWaterPerTick, true);
                        }
                    }
                }

                garden.tick();
                dayTickCounter++;

                // End of simulated day: reset daily overrides and log status
                if (dayTickCounter >= TICKS_PER_DAY) {
                    dayTickCounter = 0;
                    currentDay++;
                    rainActive       = false;
                    rainWaterPerTick = 0;
                    garden.clearTemperatureOverride();

                    long alive = garden.getPlants().stream().filter(Plant::isAlive).count();
                    GardenLogger.getInstance().log("API",
                            String.format("=== Day %d complete — %d plants alive ===", currentDay, alive));
                    writeApiLog(currentDay, "day_end", String.valueOf(currentDay), alive);
                    writeDailySummary(currentDay);
                    System.out.printf("[API] Day %d complete — %d/%d plants alive%n",
                            currentDay, alive, garden.getPlants().size());
                }

                Thread.sleep(tickIntervalMs);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                GardenLogger.getInstance().logError("API", "Simulation thread error", e);
                // Continue running — garden must not crash
            }
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Map<String, Integer> loadConfig() {
        String json = null;
        File configFile = new File(CONFIG_PATH);
        if (configFile.exists()) {
            try { json = Files.readString(configFile.toPath(), StandardCharsets.UTF_8); }
            catch (IOException e) { System.err.println("[API] Warning: could not read config: " + e.getMessage()); }
        }
        if (json == null) {
            try (InputStream is = getClass().getResourceAsStream("/garden_config.json")) {
                if (is != null) json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) { /* fall through */ }
        }
        return json == null ? defaultConfig() : parseConfig(json);
    }

    private Map<String, Integer> parseConfig(String json) {
        Map<String, Integer> result = new LinkedHashMap<>();
        Pattern p = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"amount\"\\s*:\\s*(\\d+)");
        Matcher m = p.matcher(json);
        while (m.find()) result.merge(m.group(1), Integer.parseInt(m.group(2)), Integer::sum);
        return result.isEmpty() ? defaultConfig() : result;
    }

    private Map<String, Integer> defaultConfig() {
        Map<String, Integer> d = new LinkedHashMap<>();
        d.put("Tomato",    3); d.put("Rose",      3); d.put("Sunflower", 2);
        d.put("Carrot",    3); d.put("Lettuce",   3); d.put("Cactus",    2);
        return d;
    }

    private void placePlants(String species, int amount) {
        int[] zoneRows = zoneRowsFor(species);
        int placed = 0;
        outer:
        for (int col = 1; col < 19 && placed < amount; col++) {
            for (int row : zoneRows) {
                if (placed >= amount) break outer;
                Position pos = new Position(row, col);
                if (!garden.isPositionOccupied(pos)) {
                    Plant plant = garden.createPlant(species, pos);
                    if (plant != null) { garden.addPlant(plant); placed++; }
                }
            }
        }
        if (placed < amount)
            System.err.printf("[API] Warning: only placed %d/%d %s plants%n", placed, amount, species);
    }

    private int[] zoneRowsFor(String species) {
        return switch (species.toLowerCase()) {
            case "tomato"    -> new int[]{1, 2};
            case "rose"      -> new int[]{5, 6};
            case "sunflower" -> new int[]{9};
            case "carrot"    -> new int[]{13};
            case "lettuce"   -> new int[]{14};
            case "cactus"    -> new int[]{18};
            default          -> new int[]{0};
        };
    }

    private List<String> vulnerableTo(Plant p) {
        return switch (p.getSpecies()) {
            case "Lactuca sativa", "Rosa",
                 "Solanum lycopersicum", "Daucus carota" -> List.of("Aphid", "Caterpillar");
            case "Helianthus annuus"                     -> List.of("Caterpillar");
            case "Cactaceae"                             -> List.of();
            default                                      -> List.of("Aphid", "Caterpillar");
        };
    }

    private Insect createInsect(String type, Position pos) {
        return switch (type.toLowerCase()) {
            case "aphid"       -> new Aphid(pos);
            case "caterpillar" -> new Caterpillar(pos);
            case "bee"         -> new Bee(pos);
            case "ladybug"     -> new Ladybug(pos);
            default -> { GardenLogger.getInstance().log("API", "Unknown parasite: " + type); yield null; }
        };
    }

    private void writeApiLog(int day, String event, String eventValue, long plantsAlive) {
        String line = String.format("DAY=%d,EVENT=%s,EVENT_VALUE=%s,PLANTS_ALIVE=%d",
                day, event, eventValue, plantsAlive);
        appendToLogFile(line + System.lineSeparator());
    }

    /**
     * Writes a daily end-of-day summary to log.txt:
     *   - Garden-wide stats (temperature, humidity, alive count)
     *   - Per-plant snapshot (HP, water, nutrients, growth stage)
     * This runs after every simulated day so the TA can track garden health day-by-day.
     */
    private void writeDailySummary(int day) {
        long alive = garden.getPlants().stream().filter(Plant::isAlive).count();
        long dead  = garden.getPlants().stream().filter(p -> !p.isAlive()).count();
        long activePests = garden.getInsects().stream()
                .filter(Insect::isAlive)
                .filter(i -> i.getType() == Insect.InsectType.PEST)
                .count();

        StringBuilder sb = new StringBuilder();

        // Header
        sb.append(String.format(
                "--- DAY %d SUMMARY | Temp: %.1fF | Humidity: %.1f%% | Plants: %d alive, %d dead ---%n",
                day, garden.getCurrentTemperature(), garden.getCurrentHumidity(), alive, dead));

        // Today's injected event
        sb.append(String.format("  EVENT:    %s(%s)%n", lastEvent, lastEventValue));

        // One line per module — shows the TA each subsystem responded
        sb.append(String.format("  WATERING: %s%n", garden.getWateringSystem().getStatusSummary()));
        sb.append(String.format("  HEATING:  %s%n", garden.getHeatingSystem().getStatusSummary()));
        sb.append(String.format("  PESTS:    %s | Active pests: %d%n",
                garden.getPestControl().getStatusSummary(), activePests));

        // Per-plant snapshot
        for (Plant p : garden.getPlants()) {
            sb.append(String.format(
                    "  DAY=%d | %-10s | HP=%5.1f | Water=%5.1f | Stage=%-10s | alive=%b%n",
                    day, p.getName(), p.getHealth(), p.getWaterLevel(),
                    p.getGrowthStage(), p.isAlive()));
        }

        appendToLogFile(sb.toString());
    }

    private void appendToLogFile(String text) {
        new File("logs").mkdirs();
        try (FileWriter fw = new FileWriter(LOG_PATH, StandardCharsets.UTF_8, true)) {
            fw.write(text);
        } catch (IOException e) {
            System.err.println("[API] Warning: could not write to " + LOG_PATH + ": " + e.getMessage());
        }
    }
}
