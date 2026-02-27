package garden.modules;

import garden.core.Garden;
import garden.logging.GardenLogger;
import garden.model.plants.Plant;
import garden.model.sensors.MoistureSensor;
import garden.model.sensors.Sensor;
import garden.util.Position;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Module 1: Watering System
 * Manages sprinklers across the garden. Monitors moisture sensors and
 * automatically waters plants when moisture drops below threshold.
 * Supports both automatic and manual watering modes.
 */
public class WateringSystem implements GardenModule {

    /** Represents a single sprinkler in the garden. */
    public static class Sprinkler {
        private final String id;
        private final Position position;
        private final double radius;    // coverage radius in grid units
        private final double flowRate;  // water amount per tick when active
        private boolean active;

        private static int counter = 0;

        public Sprinkler(Position position, double radius, double flowRate) {
            this.id = "SPR-" + (++counter);
            this.position = position;
            this.radius = radius;
            this.flowRate = flowRate;
            this.active = false;
        }

        public String getId() { return id; }
        public Position getPosition() { return position; }
        public double getRadius() { return radius; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        public double getFlowRate() { return flowRate; }
    }

    private boolean enabled;
    private final List<Sprinkler> sprinklers;
    private double moistureThresholdLow;    // below this -> start watering
    private double moistureThresholdHigh;   // above this -> stop watering
    private int totalWateringEvents;

    public WateringSystem() {
        this.enabled = true;
        this.sprinklers = new ArrayList<>();
        this.moistureThresholdLow = 25.0;
        this.moistureThresholdHigh = 65.0;
        this.totalWateringEvents = 0;

        GardenLogger.getInstance().log("WATERING", "Watering System initialized.");
    }

    /** Add a sprinkler to the system. */
    public void addSprinkler(Position position, double radius, double flowRate) {
        Sprinkler s = new Sprinkler(position, radius, flowRate);
        sprinklers.add(s);
        GardenLogger.getInstance().log("WATERING",
                String.format("Sprinkler %s added at %s (radius: %.1f, flow: %.1f)",
                        s.getId(), position, radius, flowRate));
    }

    @Override
    public void update(Garden garden) {
        if (!enabled) return;

        try {
            List<Plant> alivePlants = garden.getPlants().stream()
                    .filter(Plant::isAlive)
                    .collect(Collectors.toList());

            // Realistic Zoned Control: Check each sprinkler zone independently
            for (Sprinkler sprinkler : sprinklers) {
                // Use Hysteresis: If already active, check against HIGH threshold
                double threshold = sprinkler.isActive() ? moistureThresholdHigh : moistureThresholdLow;
                boolean zoneNeedsWater = false;

                // 1. Check sensors in this specific zone
                for (Sensor sensor : garden.getSensors()) {
                    if (sensor instanceof MoistureSensor && 
                        sensor.getPosition().distanceTo(sprinkler.getPosition()) <= sprinkler.getRadius() + 2) {
                        if (sensor.getCurrentReading() < threshold) {
                            zoneNeedsWater = true;
                            break;
                        }
                    }
                }

                // 2. Safety Net: Check individual plants in this zone
                if (!zoneNeedsWater) {
                    List<Plant> plantsInZone = alivePlants.stream()
                            .filter(p -> p.getPosition().distanceTo(sprinkler.getPosition()) <= sprinkler.getRadius())
                            .toList();
                    
                    if (!plantsInZone.isEmpty()) {
                        // If active, stay on until ALL plants in zone are above 75%
                        // If inactive, turn on if ANY plant is below low threshold
                        if (sprinkler.isActive()) {
                            zoneNeedsWater = plantsInZone.stream().anyMatch(p -> p.getWaterLevel() < 75.0);
                        } else {
                            zoneNeedsWater = plantsInZone.stream().anyMatch(p -> p.getWaterLevel() < moistureThresholdLow);
                        }
                    }
                }

                // 3. Actuate only this specific zone
                if (zoneNeedsWater) {
                    if (!sprinkler.isActive()) {
                        sprinkler.setActive(true);
                        totalWateringEvents++;
                        GardenLogger.getInstance().log("WATERING",
                            "Zone " + sprinkler.getId() + " activated at " + sprinkler.getPosition());
                    }
                    // Apply water to plants in this zone
                    for (Plant plant : alivePlants) {
                        if (plant.getPosition().distanceTo(sprinkler.getPosition()) <= sprinkler.getRadius()) {
                            plant.water(sprinkler.getFlowRate(), true);
                            plant.addNutrients(2.0);
                        }
                    }
                } else {
                    if (sprinkler.isActive()) {
                        sprinkler.setActive(false);
                        GardenLogger.getInstance().log("WATERING", "Zone " + sprinkler.getId() + " deactivated.");
                    }
                }
            }

        } catch (Exception e) {
            GardenLogger.getInstance().logError("WATERING", "Error in zoned watering update", e);
        }
    }

    /** Manually trigger all zones and immediately deliver water. */
    public void manualWater(Garden garden) {
        GardenLogger.getInstance().log("USER_ACTION", "Manual override: Activating all zones.");
        List<Plant> alivePlants = garden.getPlants().stream()
                .filter(Plant::isAlive)
                .collect(Collectors.toList());

        for (Sprinkler s : sprinklers) {
            s.setActive(true);
            totalWateringEvents++;
            for (Plant plant : alivePlants) {
                if (plant.getPosition().distanceTo(s.getPosition()) <= s.getRadius()) {
                    plant.water(s.getFlowRate(), true);
                    plant.addNutrients(2.0);
                }
            }
        }
        GardenLogger.getInstance().log("WATERING",
                String.format("Manual watering complete: %d sprinklers activated, %d plants watered.",
                        sprinklers.size(), alivePlants.size()));
    }

    // --- Getters & Setters ---
    public List<Sprinkler> getSprinklers() { return sprinklers; }
    public int getTotalWateringEvents() { return totalWateringEvents; }
    public double getMoistureThresholdLow() { return moistureThresholdLow; }
    public double getMoistureThresholdHigh() { return moistureThresholdHigh; }
    public void setMoistureThresholdLow(double val) { this.moistureThresholdLow = val; }
    public void setMoistureThresholdHigh(double val) { this.moistureThresholdHigh = val; }

    @Override
    public String getModuleName() { return "Watering System"; }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        GardenLogger.getInstance().log("WATERING",
                "Watering System " + (enabled ? "ENABLED" : "DISABLED"));
    }

    @Override
    public boolean isEnabled() { return enabled; }

    @Override
    public String getStatusSummary() {
        long activeSprinklers = sprinklers.stream().filter(Sprinkler::isActive).count();
        return String.format("Watering System [%s] | Sprinklers: %d/%d active | Events: %d",
                enabled ? "ON" : "OFF", activeSprinklers, sprinklers.size(), totalWateringEvents);
    }
}
