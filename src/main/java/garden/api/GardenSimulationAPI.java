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
import java.util.stream.Collectors;

/**
 * Public API for the automated garden simulation.
 * Implements the interface described in "Gardening System APIs.pdf".
 *
 * Usage:
 *   GardenSimulationAPI api = new GardenSimulationAPI();
 *   api.initializeGarden();
 *   api.rain(10);
 *   api.sleepOneHour();          // end of day 1
 *   api.temperature(85);
 *   api.parasite("aphid");
 *   api.sleepOneHour();          // end of day 2
 *   // ... 24 days total ...
 *   api.getState();
 */
public class GardenSimulationAPI {

    private static final int    TICKS_PER_DAY  = 200;
    private static final String CONFIG_PATH    = "config/garden_config.json";
    private static final String LOG_PATH       = "logs/log.txt";

    private Garden garden;
    private int    currentDay   = 0;

    // Track the last event each day for getState() logging
    private String lastEvent      = "none";
    private String lastEventValue = "none";

    // Pending rain for current day — applied per-tick inside sleepOneHour()
    private double rainWaterPerTick = 0.0;
    private boolean rainActive      = false;

    // -------------------------------------------------------------------------
    // Public API Methods
    // -------------------------------------------------------------------------

    /**
     * Initialises the garden from the config file and starts the simulation clock.
     * Must be called first. Guarantees >= 10 alive plants covering all varieties.
     */
    public void initializeGarden() {
        garden     = new Garden("API Garden", 20, 20);
        currentDay = 0;
        lastEvent  = "init";
        lastEventValue = "none";
        rainActive = false;
        rainWaterPerTick = 0;

        // Clear log.txt so each new run starts with a fresh log
        new File("logs").mkdirs();
        try (FileWriter fw = new FileWriter(LOG_PATH, false)) {
            // truncate only — no content written yet
        } catch (IOException e) {
            System.err.println("[API] Warning: could not clear " + LOG_PATH + ": " + e.getMessage());
        }

        garden.initializeDefaultGarden();

        // Load and parse config
        Map<String, Integer> plantCounts = loadConfig();

        // Place plants in their designated zone rows
        for (Map.Entry<String, Integer> entry : plantCounts.entrySet()) {
            String species = entry.getKey().toLowerCase();
            int    amount  = entry.getValue();
            placePlants(species, amount);
        }

        long alive = garden.getPlants().stream().filter(Plant::isAlive).count();
        GardenLogger.getInstance().log("API",
                String.format("Garden initialised: %d alive plants across %d species.",
                        alive, plantCounts.size()));

        writeApiLog("init", "none", alive);
        System.out.println("[API] Garden initialised — " + alive + " plants alive.");
    }

    /**
     * Returns a map of all alive plant data:
     *   "plants"           -> List<String>        — plant names
     *   "waterRequirement" -> List<Integer>        — daily water need per plant
     *   "parasites"        -> List<List<String>>   — parasites each plant is vulnerable to
     */
    public Map<String, Object> getPlants() {
        List<String>        names    = new ArrayList<>();
        List<Integer>       water    = new ArrayList<>();
        List<List<String>>  parasites = new ArrayList<>();

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
     * Simulates rainfall for the current day.
     * Water is distributed evenly across the day's ticks inside sleepOneHour().
     * Resets automatically when sleepOneHour() ends.
     *
     * @param amount total water units to add to each plant over the day
     *               (scale: matches waterRequirement values from getPlants())
     */
    public void rain(int amount) {
        if (amount <= 0) return;
        rainWaterPerTick = amount / (double) TICKS_PER_DAY;
        rainActive = true;
        lastEvent      = "rain";
        lastEventValue = String.valueOf(amount);

        long alive = garden.getPlants().stream().filter(Plant::isAlive).count();
        GardenLogger.getInstance().log("API",
                String.format("Day %d — Rain event: %d units (%.3f/tick)", currentDay + 1, amount, rainWaterPerTick));
        writeApiLog("rain", String.valueOf(amount), alive);
        System.out.printf("[API] Day %d — rain(%d)%n", currentDay + 1, amount);
    }

    /**
     * Sets the garden temperature for the current day.
     * Overrides the natural seasonal cycle; resets when sleepOneHour() ends.
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
                String.format("Day %d — Temperature override: %d°F", currentDay + 1, clamped));
        writeApiLog("temperature", String.valueOf(clamped), alive);
        System.out.printf("[API] Day %d — temperature(%d°F)%n", currentDay + 1, clamped);
    }

    /**
     * Triggers a parasite infestation. Spawns 4 insects of the named type across
     * the garden. The existing PestControl module responds autonomously over time,
     * but does not instantly heal plants.
     *
     * Accepted names (case-insensitive): aphid, caterpillar, bee, ladybug, insects
     * "insects" spawns a mixed aphid + caterpillar infestation.
     *
     * @param type parasite type name
     */
    public void parasite(String type) {
        if (type == null || type.isBlank()) return;
        String key = type.strip().toLowerCase();
        lastEvent      = "parasite";
        lastEventValue = key;

        int spawned = 0;
        Random rng = new Random();

        List<String> types = key.equals("insects")
                ? List.of("aphid", "caterpillar", "aphid", "caterpillar")
                : Collections.nCopies(4, key);

        for (String t : types) {
            int row = 1 + rng.nextInt(18);
            int col = 1 + rng.nextInt(18);
            Insect insect = createInsect(t, new Position(row, col));
            if (insect != null) {
                garden.addInsect(insect);
                spawned++;
            }
        }

        long alive = garden.getPlants().stream().filter(Plant::isAlive).count();
        GardenLogger.getInstance().log("API",
                String.format("Day %d — Parasite event: %s (%d insects spawned)", currentDay + 1, key, spawned));
        writeApiLog("parasite", key, alive);
        System.out.printf("[API] Day %d — parasite(\"%s\") — %d insects spawned%n", currentDay + 1, key, spawned);
    }

    /**
     * Advances the simulation by one simulated day (200 ticks).
     * Applies any pending rain per-tick, then clears rain and temperature overrides.
     * Called by the external script to end the current day.
     */
    public void sleepOneHour() {
        currentDay++;

        for (int t = 0; t < TICKS_PER_DAY; t++) {
            // Apply rain water before the tick so sensors and modules see updated moisture.
            // Cap at 85% — excess drains away (prevents overwatering sensitive plants like Cactus).
            if (rainActive) {
                for (Plant p : garden.getPlants()) {
                    if (p.isAlive() && p.getWaterLevel() < 85.0) p.water(rainWaterPerTick, true);
                }
            }
            garden.tick();
        }

        // Reset daily overrides
        rainActive       = false;
        rainWaterPerTick = 0;
        garden.clearTemperatureOverride();

        long alive = garden.getPlants().stream().filter(Plant::isAlive).count();
        GardenLogger.getInstance().log("API",
                String.format("=== Day %d complete — %d plants alive ===", currentDay, alive));
        System.out.printf("[API] Day %d complete — %d/%d plants alive%n",
                currentDay, alive, garden.getPlants().size());
    }

    /**
     * Logs the current garden state to logs/log.txt.
     * Format: DAY, EVENT, EVENT_VALUE, PLANTS_ALIVE
     * Should be called after 24 simulated days.
     */
    public void getState() {
        long alive = garden.getPlants().stream().filter(Plant::isAlive).count();
        long dead  = garden.getPlants().stream().filter(p -> !p.isAlive()).count();

        writeApiLog(lastEvent, lastEventValue, alive);

        // Detailed state block to log.txt
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== GARDEN STATE — DAY ").append(currentDay).append(" ===\n");
        sb.append(String.format("PLANTS_ALIVE: %d | PLANTS_DEAD: %d%n", alive, dead));
        sb.append(String.format("TEMPERATURE:  %.1f°F%n", garden.getCurrentTemperature()));
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

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /** Load and parse config/garden_config.json (relative path, falls back to classpath). */
    private Map<String, Integer> loadConfig() {
        String json = null;

        // 1. Try relative path (working directory — no absolute paths per spec)
        File configFile = new File(CONFIG_PATH);
        if (configFile.exists()) {
            try {
                json = Files.readString(configFile.toPath(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                System.err.println("[API] Warning: could not read " + CONFIG_PATH + ": " + e.getMessage());
            }
        }

        // 2. Fallback: classpath (bundled in JAR/resources)
        if (json == null) {
            try (InputStream is = getClass().getResourceAsStream("/garden_config.json")) {
                if (is != null) json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                System.err.println("[API] Warning: could not load config from classpath: " + e.getMessage());
            }
        }

        if (json == null) {
            System.err.println("[API] Config not found — using built-in defaults.");
            return defaultConfig();
        }

        return parseConfig(json);
    }

    /**
     * Minimal JSON parser for the spec format:
     *   { "plants": [ { "name": "X", "amount": N }, ... ] }
     * No external library required.
     */
    private Map<String, Integer> parseConfig(String json) {
        Map<String, Integer> result = new LinkedHashMap<>();
        // Match each { "name": "...", "amount": N } pair
        Pattern p = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"amount\"\\s*:\\s*(\\d+)");
        Matcher m = p.matcher(json);
        while (m.find()) {
            result.merge(m.group(1), Integer.parseInt(m.group(2)), Integer::sum);
        }
        return result.isEmpty() ? defaultConfig() : result;
    }

    /** Built-in fallback config if no file is found. */
    private Map<String, Integer> defaultConfig() {
        Map<String, Integer> defaults = new LinkedHashMap<>();
        defaults.put("Tomato",    3);
        defaults.put("Rose",      3);
        defaults.put("Sunflower", 2);
        defaults.put("Carrot",    3);
        defaults.put("Lettuce",   3);
        defaults.put("Cactus",    2);
        return defaults;
    }

    /**
     * Places `amount` plants of the given species into their designated zone rows,
     * finding free positions systematically from left to right.
     */
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
                    if (plant != null) {
                        garden.addPlant(plant);
                        placed++;
                    }
                }
            }
        }
        if (placed < amount) {
            System.err.printf("[API] Warning: only placed %d/%d %s plants (zone full)%n",
                    placed, amount, species);
        }
    }

    /** Returns the designated row(s) for each species per the garden zone layout. */
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

    /** Returns the list of parasites the given plant species is vulnerable to. */
    private List<String> vulnerableTo(Plant p) {
        return switch (p.getSpecies()) {
            case "Lactuca sativa"       -> List.of("Aphid", "Caterpillar"); // Lettuce — most vulnerable
            case "Rosa"                 -> List.of("Aphid", "Caterpillar"); // Rose
            case "Solanum lycopersicum" -> List.of("Aphid", "Caterpillar"); // Tomato
            case "Daucus carota"        -> List.of("Aphid", "Caterpillar"); // Carrot
            case "Helianthus annuus"    -> List.of("Caterpillar");          // Sunflower
            case "Cactaceae"            -> List.of();                       // Cactus — highly resistant
            default                     -> List.of("Aphid", "Caterpillar");
        };
    }

    /** Creates an insect instance by type name. Returns null for unknown types. */
    private Insect createInsect(String type, Position pos) {
        return switch (type.toLowerCase()) {
            case "aphid"       -> new Aphid(pos);
            case "caterpillar" -> new Caterpillar(pos);
            case "bee"         -> new Bee(pos);
            case "ladybug"     -> new Ladybug(pos);
            default -> {
                GardenLogger.getInstance().log("API", "Unknown parasite type: " + type);
                yield null;
            }
        };
    }

    /**
     * Writes a structured summary line to logs/log.txt.
     * Format: DAY,EVENT,EVENT_VALUE,PLANTS_ALIVE
     */
    private void writeApiLog(String event, String eventValue, long plantsAlive) {
        String line = String.format("DAY=%d,EVENT=%s,EVENT_VALUE=%s,PLANTS_ALIVE=%d",
                currentDay, event, eventValue, plantsAlive);
        appendToLogFile(line + System.lineSeparator());
    }

    /** Appends text to logs/log.txt (relative path). */
    private void appendToLogFile(String text) {
        new File("logs").mkdirs();
        try (FileWriter fw = new FileWriter(LOG_PATH, true)) {
            fw.write(text);
        } catch (IOException e) {
            System.err.println("[API] Warning: could not write to " + LOG_PATH + ": " + e.getMessage());
        }
    }
}
