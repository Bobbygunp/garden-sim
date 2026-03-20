package garden.api;

import garden.core.Garden;
import garden.model.plants.Plant;


public class DroughtTest {

    private static final int TICKS_PER_DAY = 200;
    private static final int TEST_DAYS     = 10;

    public static void main(String[] args) {
        System.out.println("=======================================================");
        System.out.println(" DROUGHT TEST — Zero rain, Zero watering");
        System.out.println(" " + TEST_DAYS + " days (" + (TEST_DAYS * TICKS_PER_DAY) + " ticks)");
        System.out.println("=======================================================\n");

        Garden garden = new Garden("DroughtTest", 20, 20);
        garden.initializeDefaultGarden();

        garden.setRainDisabled(true);

        garden.getWateringSystem().setEnabled(false);

        System.out.println("[DroughtTest] Rain:     DISABLED");
        System.out.println("[DroughtTest] Watering: DISABLED\n");

        String[] species = {"tomato", "tomato", "tomato",
                            "rose",   "rose",   "rose",
                            "sunflower", "sunflower",
                            "carrot", "carrot", "carrot",
                            "lettuce","lettuce","lettuce",
                            "cactus", "cactus"};
        int[] rows = {1, 1, 2, 5, 5, 6, 9, 9, 13, 13, 13, 14, 14, 14, 18, 18};
        int[] cols = {1, 4, 1, 1, 4, 1,  2,  6,  1,  4,  7,  1,  4,  7,  1,  6};
        for (int i = 0; i < species.length; i++) {
            garden.addPlant(garden.createPlant(species[i],
                    new garden.util.Position(rows[i], cols[i])));
        }
        System.out.printf("Plants placed: %d%n%n", garden.getPlants().size());

        for (int day = 1; day <= TEST_DAYS; day++) {
            for (int t = 0; t < TICKS_PER_DAY; t++) {
                garden.tick();
            }

            long alive = garden.getPlants().stream().filter(Plant::isAlive).count();
            long dead  = garden.getPlants().stream().filter(p -> !p.isAlive()).count();
            System.out.printf("  Day %2d | %2d alive  %2d dead%n", day, alive, dead);

            if (alive == 0) {
                System.out.println("\n  All plants dead — stopping early.");
                break;
            }
        }


        System.out.println("\n--- FINAL PLANT STATE ---");
        for (Plant p : garden.getPlants()) {
            System.out.printf("  %-10s | HP: %5.1f | Water: %5.1f | Stage: %-10s | alive=%b%n",
                    p.getName(), p.getHealth(), p.getWaterLevel(),
                    p.getGrowthStage(), p.isAlive());
        }
    }
}
