package garden;

import garden.core.Garden;
import garden.model.plants.Plant;
import garden.model.plants.Cactus;
import garden.modules.WateringSystem;
import java.util.List;

public class CactusDiagnosticTest {
    public static void main(String[] args) {
        System.out.println("=== Starting Cactus Diagnostic Survival Test (600 Ticks) ===");
        Garden garden = new Garden("Cactus Diagnostic Garden", 20, 20);
        garden.initializeDefaultGarden();

        // Find a Cactus to track
        Plant target = null;
        for (Plant p : garden.getPlants()) {
            if (p instanceof Cactus) {
                target = p;
                break;
            }
        }

        if (target == null) {
            System.err.println("No Cactus found in the garden!");
            return;
        }

        System.out.println("Tracking Plant: " + target.getName() + " [" + target.getId() + "] at " + target.getPosition());

        for (int i = 1; i <= 600; i++) {
            garden.tick();
            
            boolean sprinklerInZoneActive = false;
            double sprinklerFlow = 0;
            for (WateringSystem.Sprinkler s : garden.getWateringSystem().getSprinklers()) {
                if (s.getPosition().distanceTo(target.getPosition()) <= s.getRadius()) {
                    if (s.isActive()) {
                        sprinklerInZoneActive = true;
                        sprinklerFlow = s.getFlowRate();
                    }
                }
            }

            if (i % 20 == 0 || !target.isAlive()) {
                System.out.printf("Tick %d | Water: %.1f | Health: %.1f | SprinklerActive: %b | Flow: %.1f | Humidity: %.1f%n",
                    i, target.getWaterLevel(), target.getHealth(), sprinklerInZoneActive, sprinklerFlow, garden.getCurrentHumidity());
            }

            if (!target.isAlive()) {
                System.out.println("CACTUS DIED at tick " + i + "! Final Stats: " + target.getStatusSummary());
                break;
            }
        }
    }
}
