package garden.modules;

import garden.core.Garden;
import garden.logging.GardenLogger;
import garden.model.plants.Plant;
import garden.model.sensors.MoistureSensor;
import garden.model.sensors.Sensor;
import garden.util.Position;

import java.util.ArrayList;
import java.util.Comparator;
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
        private final double radius;
        private final double flowRate;
        private boolean active;
        private double thresholdLow;
        private double thresholdHigh;
        private MoistureSensor linkedSensor;

        private static int counter = 0;

        public Sprinkler(Position position, double radius, double flowRate) {
            this.id = "SPR-" + (++counter);
            this.position = position;
            this.radius = radius;
            this.flowRate = flowRate;
            this.active = false;
        }

        public Sprinkler(Position position, double radius, double flowRate, double thresholdLow, double thresholdHigh) {
            this(position, radius, flowRate);
            this.thresholdLow = thresholdLow;
            this.thresholdHigh = thresholdHigh;
        }

        public String getId() { return id; }
        public Position getPosition() { return position; }
        public double getRadius() { return radius; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        public double getFlowRate() { return flowRate; }
        public double getThresholdLow() { return thresholdLow; }
        public double getThresholdHigh() { return thresholdHigh; }
        public void linkSensor(MoistureSensor sensor) { this.linkedSensor = sensor; }
        public MoistureSensor getLinkedSensor() { return linkedSensor; }
    }

    private boolean enabled;
    private final List<Sprinkler> sprinklers;
    private double moistureThresholdLow;
    private double moistureThresholdHigh;
    private int totalWateringEvents;

    public WateringSystem() {
        this.enabled = true;
        this.sprinklers = new ArrayList<>();
        this.moistureThresholdLow = 25.0;
        this.moistureThresholdHigh = 65.0;
        this.totalWateringEvents = 0;

        GardenLogger.getInstance().log("WATERING", "Watering System initialized.");
    }

    public WateringSystem(double moistureThresholdLow, double moistureThresholdHigh) {
        this.enabled = true;
        this.sprinklers = new ArrayList<>();
        this.moistureThresholdLow = moistureThresholdLow;
        this.moistureThresholdHigh = moistureThresholdHigh;
        this.totalWateringEvents = 0;
    }

    /** Add a sprinkler with default thresholds (25% to 65%). */
    public Sprinkler addSprinkler(Position position, double radius, double flowRate) {
        return addSprinkler(position, radius, flowRate, 25.0, 65.0);
    }

    /** Add a sprinkler with custom thresholds for specific plant needs. */
    public Sprinkler addSprinkler(Position position, double radius, double flowRate, double thresholdLow, double thresholdHigh) {
        Sprinkler s = new Sprinkler(position, radius, flowRate, thresholdLow, thresholdHigh);
        sprinklers.add(s);
        GardenLogger.getInstance().log("WATERING",
                String.format("Sprinkler %s added at %s (radius: %.1f, flow: %.1f, range: %.0f-%.0f%%)",
                        s.getId(), position, radius, flowRate, thresholdLow, thresholdHigh));
        return s;
    }

    @Override
    public void update(Garden garden) {
        if (!enabled) return;

        try {
            List<Plant> alivePlants = garden.getPlants().stream()
                    .filter(Plant::isAlive)
                    .collect(Collectors.toList());

            for (Sprinkler sprinkler : sprinklers) {
                boolean zoneNeedsWater = sprinkler.isActive();

                MoistureSensor ms = sprinkler.getLinkedSensor();

                if (ms == null) {
                    ms = garden.getSensors().stream()
                            .filter(s -> s instanceof MoistureSensor)
                            .map(s -> (MoistureSensor) s)
                            .min(Comparator.comparingDouble(s -> s.getPosition().distanceTo(sprinkler.getPosition())))
                            .orElse(null);
                    
                    if (ms != null && ms.getPosition().distanceTo(sprinkler.getPosition()) <= 5.0) {
                        sprinkler.linkSensor(ms);
                        GardenLogger.getInstance().log("WATERING", 
                            "Auto-linked Sprinkler " + sprinkler.getId() + " to nearest sensor at " + ms.getPosition());
                    }
                }

                if (ms != null) {
                    if (!sprinkler.isActive() && ms.getMinReading() < sprinkler.getThresholdLow()) {
                        zoneNeedsWater = true;
                    } else if (sprinkler.isActive() && ms.getAvgReading() >= sprinkler.getThresholdHigh()) {
                        zoneNeedsWater = false;
                    }
                }

                /* 
                    if (!zoneNeedsWater) {
                        List<Plant> plantsInZone = alivePlants.stream()
                                .filter(p -> p.getPosition().distanceTo(sprinkler.getPosition()) <= sprinkler.getRadius())
                                .toList();
                        
                        if (!plantsInZone.isEmpty()) {
                            if (sprinkler.isActive()) {
                                zoneNeedsWater = plantsInZone.stream().anyMatch(p -> p.getWaterLevel() < 75.0);
                            } else {
                                zoneNeedsWater = plantsInZone.stream().anyMatch(p -> p.getWaterLevel() < moistureThresholdLow);
                            }
                        }
                    }
                */

                if (zoneNeedsWater) {
                    if (!sprinkler.isActive()) {
                        sprinkler.setActive(true);
                        totalWateringEvents++;
                        GardenLogger.getInstance().log("WATERING",
                            "Zone " + sprinkler.getId() + " activated at " + sprinkler.getPosition());
                    }
                    for (Plant plant : alivePlants) {
                        boolean inZone;
                        if (ms != null && ms.isZoned()) {
                            Position p = plant.getPosition();
                            inZone = p.getRow() >= ms.getZoneMinRow()
                                  && p.getRow() <= ms.getZoneMaxRow()
                                  && p.getCol() >= ms.getZoneMinCol()
                                  && p.getCol() <= ms.getZoneMaxCol();
                        } else {
                            inZone = plant.getPosition().distanceTo(sprinkler.getPosition()) <= sprinkler.getRadius();
                        }
                        if (inZone) {
                            plant.water(sprinkler.getFlowRate(), true);
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
                }
            }
        }
        GardenLogger.getInstance().log("WATERING",
                String.format("Manual watering complete: %d sprinklers activated, %d plants watered.",
                        sprinklers.size(), alivePlants.size()));
    }

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
        if (!enabled) {
            sprinklers.forEach(s -> s.setActive(false));
        }
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
