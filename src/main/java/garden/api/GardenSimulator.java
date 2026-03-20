package garden.api;

import java.util.HashMap;
import java.util.Random;

/**
 * Runs the API simulation in quick mode or real-time mode.
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
        if (!realTime) gardenAPI.setQuickMode(true);

        gardenAPI.initializeGarden();
        HashMap<String, Object> initialPlantDetails = (HashMap<String, Object>) gardenAPI.getPlants();
        System.out.println("\n[Initial plant details]");
        System.out.println("  plants:           " + initialPlantDetails.get("plants"));
        System.out.println("  waterRequirement: " + initialPlantDetails.get("waterRequirement"));
        System.out.println("  parasites:        " + initialPlantDetails.get("parasites"));
        System.out.println();

        gardenAPI.rain(25);
        sleepOneHour();

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

        gardenAPI.temperature(60);
        gardenAPI.parasite("insects");
        sleepOneHour();

        gardenAPI.getState();
    }

    /** Sleep helper for simulation day progression. */
    private static void sleepOneHour() {
        if (realTime) {
            try { Thread.sleep(3_600_000L); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        } else {
            gardenAPI.awaitDayEnd();
        }
    }
}
