package garden;

import garden.core.Garden;
import garden.model.plants.Plant;
import garden.modules.WateringSystem;
import java.util.List;

public class LongTermSurvivalTest {
    public static void main(String[] args) {
        System.out.println("=== Starting Diagnostic Survival Test ===");
        Garden garden = new Garden("Diagnostic Garden", 20, 20);
        garden.initializeDefaultGarden();

        // Focus on a specific plant: Tomato [PLANT-11] at (1, 16)
        Plant target = garden.getPlants().get(10); 
        System.out.println("Tracking Plant: " + target.getName() + " at " + target.getPosition());

        for (int i = 1; i <= 200; i++) {
            garden.tick();
            
            boolean sprinklerActive = false;
            for (WateringSystem.Sprinkler s : garden.getWateringSystem().getSprinklers()) {
                if (s.getPosition().distanceTo(target.getPosition()) <= s.getRadius()) {
                    if (s.isActive()) sprinklerActive = true;
                }
            }

            if (i % 10 == 0 || target.getWaterLevel() < 15 || !target.isAlive()) {
                System.out.printf("Tick %d | Water: %.1f | Nutrients: %.1f | HP: %.1f | SprinklerInZoneActive: %b%n",
                    i, target.getWaterLevel(), target.getNutrientLevel(), target.getHealth(), sprinklerActive);
            }

            if (!target.isAlive()) {
                System.out.println("PLANT DIED at tick " + i);
                break;
            }
        }
    }
}
