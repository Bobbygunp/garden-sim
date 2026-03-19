package garden.api;

import java.util.HashMap;
import java.util.Random;

/**
 * Mirrors the TA's grading script from "Gardening System APIs.pdf".
 *
 * The garden runs continuously in a background thread (started by initializeGarden()).
 * sleepOneHour() here is the TA's own local helper — it simply sleeps 1 real hour.
 * The garden keeps ticking autonomously during that sleep.
 *
 * Run modes:
 *   mvn exec:java                        — quick test (no real sleep, uses short sleep)
 *   mvn exec:java -Dexec.args="--real-time" — full 24-hour TA simulation
 */
public class GardenSimulator {

    private static boolean              realTime  = false;
    private static GardenSimulationAPI  gardenAPI = null;

    public static void main(String[] args) {
        for (String arg : args) {
            if (arg.equals("--real-time")) realTime = true;
        }
        if (realTime) {
            System.out.println("[Simulator] REAL-TIME mode: 1 real hour per simulated day (24 hours total).");
        } else {
            System.out.println("[Simulator] QUICK mode: 10-second sleep per day (runs in ~4 minutes).");
        }

        gardenAPI = new GardenSimulationAPI();
        if (!realTime) gardenAPI.setQuickMode(true); // 50ms/tick → ~10s per day

        // Beginning of the simulation
        gardenAPI.initializeGarden();
        HashMap<String, Object> initialPlantDetails = (HashMap<String, Object>) gardenAPI.getPlants();
        System.out.println("\n[Initial plant details]");
        System.out.println("  plants:           " + initialPlantDetails.get("plants"));
        System.out.println("  waterRequirement: " + initialPlantDetails.get("waterRequirement"));
        System.out.println("  parasites:        " + initialPlantDetails.get("parasites"));
        System.out.println();

        // Day 1 — rain only
        gardenAPI.rain(25);
        sleepOneHour();

        // Days 2–23 — random events each day (seeded for reproducibility)
        Random rng = new Random(42);
        String[] parasiteTypes = {"aphid", "caterpillar", "insects"};
        int[]    temps         = {45, 60, 72, 85, 95, 110};

        for (int day = 2; day <= 23; day++) {
            switch (rng.nextInt(3)) {
                case 0 -> gardenAPI.rain(5 + rng.nextInt(30));
                case 1 -> gardenAPI.temperature(temps[rng.nextInt(temps.length)]);
                case 2 -> gardenAPI.parasite(parasiteTypes[rng.nextInt(parasiteTypes.length)]);
            }
            sleepOneHour();
        }

        // Day 24 — temperature + parasite (worst case)
        gardenAPI.temperature(60);
        gardenAPI.parasite("insects");
        sleepOneHour();

        // After 24 days — assess performance
        gardenAPI.getState();
    }

    /**
     * The TA's local sleepOneHour() helper.
     * Real-time mode : sleeps 1 real hour — garden runs autonomously in background.
     * Quick mode     : waits until the background thread finishes the current day,
     *                  so events are never injected before the previous day completes.
     */
    private static void sleepOneHour() {
        if (realTime) {
            try { Thread.sleep(3_600_000L); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        } else {
            gardenAPI.awaitDayEnd();
        }
    }
}
