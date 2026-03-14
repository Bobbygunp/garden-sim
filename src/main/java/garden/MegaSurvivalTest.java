package garden;

import garden.core.Garden;
import garden.model.plants.Plant;
import java.util.List;

public class MegaSurvivalTest {
    public static void main(String[] args) {
        System.out.println("=== Starting 2000-Tick Mega Survival Test ===");
        Garden garden = new Garden("Mega Garden", 20, 20);
        garden.initializeDefaultGarden();

        for (int i = 1; i <= 2000; i++) {
            garden.tick();
            
            if (i % 200 == 0) {
                long alive = garden.getPlants().stream().filter(Plant::isAlive).count();
                System.out.printf("Tick %d | Season: %s | Alive: %d/%d | Temp: %.1f°F | Humidity: %.0f%%%n",
                    i, garden.getCurrentTick() < 1000 ? "SPRING" : "SUMMER", 
                    alive, garden.getPlants().size(), 
                    garden.getCurrentTemperature(), garden.getCurrentHumidity());
            }
        }
        
        System.out.println("=== Final Survival Report ===");
        for (Plant p : garden.getPlants()) {
            if (!p.isAlive()) {
                System.out.printf("DIED: %s [%s] at %s. Final Stats: %s%n", 
                    p.getName(), p.getId(), p.getPosition(), p.getStatusSummary());
            }
        }
    }
}
