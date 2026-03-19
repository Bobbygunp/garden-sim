package garden.api;

import garden.core.Garden;
import garden.model.plants.Plant;

/**
 * Tests plant survival when individual modules are disabled (simulating failure).
 * Runs in quick mode — each "day" completes in ~10 seconds.
 */
public class ModuleFailureTest {

    private static final int TEST_DAYS = 10;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=======================================================");
        System.out.println(" MODULE FAILURE TEST");
        System.out.println(" Each scenario runs " + TEST_DAYS + " days in quick mode.");
        System.out.println("=======================================================\n");

        runScenario("WATERING SYSTEM DISABLED (no rain)", true);
    }

    private static void runScenario(String name, boolean disableWatering) throws InterruptedException {
        System.out.println("--- SCENARIO: " + name + " ---");

        GardenSimulationAPI api = new GardenSimulationAPI();
        api.setQuickMode(true);
        api.initializeGarden();

        Garden garden = api.getGarden();
        garden.setRainDisabled(true);
        if (disableWatering) garden.getWateringSystem().setEnabled(false);

        for (int day = 1; day <= TEST_DAYS; day++) {
            api.awaitDayEnd();
            long alive = garden.getPlants().stream().filter(Plant::isAlive).count();
            System.out.printf("  Day %2d: %d/%-2d alive%n", day, alive, garden.getPlants().size());
        }

        System.out.println();
        long alive = garden.getPlants().stream().filter(Plant::isAlive).count();
        long dead  = garden.getPlants().stream().filter(p -> !p.isAlive()).count();
        System.out.printf("Final: %d alive, %d dead%n", alive, dead);
        for (Plant p : garden.getPlants()) {
            System.out.printf("  %-10s | HP: %5.1f | Water: %5.1f | alive=%b%n",
                    p.getName(), p.getHealth(), p.getWaterLevel(), p.isAlive());
        }
        System.out.println();
        api.stopSimulationPublic();
    }
}
