package garden;

import garden.core.Garden;
import garden.model.plants.Plant;
import garden.model.sensors.MoistureSensor;
import garden.modules.FertigationSystem;
import garden.modules.WateringSystem;
import garden.util.Position;

public class SystemTest {
    public static void main(String[] args) {
        System.out.println("=== Starting Advanced Ecosystem Test ===");
        
        // 1. Create a 20x20 Garden
        Garden garden = new Garden("Survival Test", 20, 20);
        garden.initializeDefaultGarden();
        
        // 2. Identify a test plant (e.g., a Tomato in the first row)
        Plant testPlant = garden.getPlants().stream()
                .filter(p -> p instanceof garden.model.plants.Tomato)
                .findFirst()
                .orElseThrow();
        
        System.out.println("\nInitial State:");
        printStatus(garden, testPlant);
        
        // 3. Simulation Loop (Run for 15 ticks to see cycles)
        // This will cover:
        // - Multiple moisture sensor checks (every 4 ticks)
        // - At least one Fertigation check (every 12 ticks)
        // - Potential rain events
        for (int i = 1; i <= 15; i++) {
            garden.tick();
            System.out.println("\n--- TICK " + i + " ---");
            
            // Log Season & Weather
            System.out.printf("Season: %s | Raining: %b%n", 
                    getGardenSeason(garden), isGardenRaining(garden));
            
            printStatus(garden, testPlant);
            
            // Manually trigger a "Drought" if it's not raining to see the sprinkler kick in
            if (i == 5 && testPlant.getWaterLevel() > 30) {
                System.out.println("[Manual Override] Simulating a sudden dry spell...");
                setPlantWaterLevel(testPlant, 20.0);
            }
        }
        
        System.out.println("\n=== Test Completed Successfully ===");
    }
    
    private static void printStatus(Garden garden, Plant plant) {
        System.out.printf("Plant: %s at %s | Health: %.1f | Water: %.1f | Nutrients: %.1f%n",
                plant.getName(), plant.getPosition(), plant.getHealth(), 
                plant.getWaterLevel(), plant.getNutrientLevel());
        
        // Check if any sprinkler is active in the garden
        boolean anySprinkler = garden.getWateringSystem().getSprinklers().stream()
                .anyMatch(s -> s.isActive());
        System.out.printf("System Status -> Sprinklers Active: %b | Temp: %.1f°F%n", 
                anySprinkler, garden.getCurrentTemperature());
    }

    private static String getGardenSeason(Garden g) {
        try {
            java.lang.reflect.Field field = Garden.class.getDeclaredField("currentSeason");
            field.setAccessible(true);
            return field.get(g).toString();
        } catch (Exception e) { return "Unknown"; }
    }

    private static boolean isGardenRaining(Garden g) {
        try {
            java.lang.reflect.Field field = Garden.class.getDeclaredField("isRaining");
            field.setAccessible(true);
            return (boolean) field.get(g);
        } catch (Exception e) { return false; }
    }
    
    private static void setPlantWaterLevel(Plant plant, double level) {
        try {
            java.lang.reflect.Field field = Plant.class.getDeclaredField("waterLevel");
            field.setAccessible(true);
            field.set(plant, level);
        } catch (Exception e) { e.printStackTrace(); }
    }
}
