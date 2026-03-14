package garden.api;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Test harness that mirrors the pseudo-code in "Gardening System APIs.pdf".
 * Runs a 24-day simulation with random rain/temperature/parasite events each day.
 */
public class GardenSimulator {

    public static void main(String[] args) {
        GardenSimulationAPI gardenAPI = new GardenSimulationAPI();

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
        sleepOneHour(gardenAPI);

        // Days 2–23 — random events each day
        Random rng = new Random(42);
        String[] parasiteTypes = {"aphid", "caterpillar", "insects"};
        int[] temps = {45, 60, 72, 85, 95, 110};

        for (int day = 2; day <= 23; day++) {
            int event = rng.nextInt(3);
            switch (event) {
                case 0 -> {
                    int rainAmt = 5 + rng.nextInt(30);
                    gardenAPI.rain(rainAmt);
                }
                case 1 -> {
                    int temp = temps[rng.nextInt(temps.length)];
                    gardenAPI.temperature(temp);
                }
                case 2 -> {
                    String parasite = parasiteTypes[rng.nextInt(parasiteTypes.length)];
                    gardenAPI.parasite(parasite);
                }
            }
            sleepOneHour(gardenAPI);
        }

        // Day 24 — temperature + parasite (worst case)
        gardenAPI.temperature(60);
        gardenAPI.parasite("insects");
        sleepOneHour(gardenAPI);

        // After 24 days — assess performance
        gardenAPI.getState();
    }

    private static void sleepOneHour(GardenSimulationAPI api) {
        api.sleepOneHour();
    }
}
