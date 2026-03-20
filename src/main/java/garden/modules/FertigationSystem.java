package garden.modules;

import garden.core.Garden;
import garden.logging.GardenLogger;
import garden.model.plants.Plant;
import garden.util.Position;

import java.util.ArrayList;
import java.util.List;

/** Module 5: Fertigation & Soil Nutrition System - zone-aware per-crop nutrient control. */
public class FertigationSystem implements GardenModule {

    /** A rectangular planting zone with its own fertigation parameters. */
    public static class FertigationZone {
        private final String name;
        private final int minRow, maxRow, minCol, maxCol;
        private final double thresholdLow;
        private final double thresholdHigh;
        private final double injectionRate;

        public FertigationZone(String name, int minRow, int maxRow, int minCol, int maxCol,
                                double thresholdLow, double thresholdHigh, double injectionRate) {
            this.name = name;
            this.minRow = minRow;
            this.maxRow = maxRow;
            this.minCol = minCol;
            this.maxCol = maxCol;
            this.thresholdLow = thresholdLow;
            this.thresholdHigh = thresholdHigh;
            this.injectionRate = injectionRate;
        }

        public boolean contains(Position pos) {
            return pos.getRow() >= minRow && pos.getRow() <= maxRow
                && pos.getCol() >= minCol && pos.getCol() <= maxCol;
        }

        public String getName() { return name; }
        public double getThresholdLow() { return thresholdLow; }
        public double getThresholdHigh() { return thresholdHigh; }
        public double getInjectionRate() { return injectionRate; }
    }

    private boolean enabled = true;
    private final List<FertigationZone> zones = new ArrayList<>();
    private int fertilizationEvents = 0;
    private final int checkInterval = 12;

    public FertigationSystem() {
        GardenLogger.getInstance().log("FERTIGATION", "Soil Nutrition System initialized. Interval: 12 ticks.");
    }

    /** Register a planting zone with its own fertigation parameters. */
    public void addZone(FertigationZone zone) {
        zones.add(zone);
        GardenLogger.getInstance().log("FERTIGATION",
            String.format("Zone '%s' configured: rows %d-%d | fertilize below %.0f%%, cap at %.0f%%, rate %.1f",
                zone.getName(), zone.minRow, zone.maxRow,
                zone.thresholdLow, zone.thresholdHigh, zone.injectionRate));
    }

    @Override
    public void update(Garden garden) {
        if (!enabled) return;
        if (garden.getCurrentTick() % checkInterval != 0) return;

        try {
            List<Plant> plants = garden.getPlants();
            boolean anyFertilized = false;

            for (Plant plant : plants) {
                if (!plant.isAlive()) continue;

                FertigationZone zone = findZone(plant);
                double level = plant.getNutrientLevel();

                if (zone != null) {
                    if (level < zone.getThresholdLow() && level < zone.getThresholdHigh()) {
                        plant.addNutrients(zone.getInjectionRate());
                        anyFertilized = true;
                    }
                } else {
                    if (level < 20.0) {
                        plant.addNutrients(5.0);
                        anyFertilized = true;
                    }
                }
            }

            if (anyFertilized && garden.getCurrentTick() % 50 == 0) {
                fertilizationEvents++;
                GardenLogger.getInstance().log("FERTIGATION", "Nutrient injection active across zones.");
            }

        } catch (Exception e) {
            GardenLogger.getInstance().logError("FERTIGATION", "Error in fertigation update", e);
        }
    }

    /** Find the zone that contains the given plant, or null if unconfigured. */
    private FertigationZone findZone(Plant plant) {
        Position pos = plant.getPosition();
        for (FertigationZone zone : zones) {
            if (zone.contains(pos)) return zone;
        }
        return null;
    }

    public List<FertigationZone> getZones() { return zones; }
    public int getFertilizationEvents() { return fertilizationEvents; }

    @Override
    public String getModuleName() { return "Soil Nutrition (Fertigation) System"; }

    @Override
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    @Override
    public boolean isEnabled() { return enabled; }

    @Override
    public String getStatusSummary() {
        return String.format("Fertigation [%s] | Zones: %d | Events: %d",
                enabled ? "ON" : "OFF", zones.size(), fertilizationEvents);
    }
}
