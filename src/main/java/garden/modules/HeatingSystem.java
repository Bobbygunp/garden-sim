package garden.modules;

import garden.core.Garden;
import garden.logging.GardenLogger;
import garden.model.sensors.TemperatureSensor;
import garden.util.Position;

import java.util.ArrayList;
import java.util.List;

/**
 * Module 2: Heating / Climate Control System
 * Manages per-zone heaters across the garden. Each zone has a dedicated
 * TemperatureSensor and target temperature, and activates independently —
 * mirroring how the WateringSystem operates per moisture zone.
 */
public class HeatingSystem implements GardenModule {

    public enum Mode {
        OFF, HEATING, COOLING, AUTO
    }

    /** Represents a single climate control zone (heater/cooler unit). */
    public static class HeatingZone {
        private final String id;
        private final Position position;
        private double targetTemperature;
        private boolean heatingActive;
        private boolean coolingActive;
        private TemperatureSensor linkedSensor;
        private int heatingActivations;
        private int coolingActivations;

        private static int counter = 0;

        public HeatingZone(Position position, double targetTemperature) {
            this.id = "HZONE-" + (++counter);
            this.position = position;
            this.targetTemperature = targetTemperature;
            this.heatingActive = false;
            this.coolingActive = false;
            this.heatingActivations = 0;
            this.coolingActivations = 0;
        }

        public String getId() { return id; }
        public Position getPosition() { return position; }
        public double getTargetTemperature() { return targetTemperature; }
        public void setTargetTemperature(double t) { this.targetTemperature = t; }
        public boolean isHeatingActive() { return heatingActive; }
        public boolean isCoolingActive() { return coolingActive; }
        public void setHeatingActive(boolean v) { this.heatingActive = v; }
        public void setCoolingActive(boolean v) { this.coolingActive = v; }
        public int getHeatingActivations() { return heatingActivations; }
        public int getCoolingActivations() { return coolingActivations; }
        public void incrementHeatingActivations() { heatingActivations++; }
        public void incrementCoolingActivations() { coolingActivations++; }
        public void linkSensor(TemperatureSensor sensor) { this.linkedSensor = sensor; }
        public TemperatureSensor getLinkedSensor() { return linkedSensor; }
    }

    private boolean enabled;
    private Mode mode;
    private double temperatureAdjustRate; // degrees per tick per active zone
    private double currentAdjustment;     // net adjustment this tick (used by UI glow)
    private int heatingActivations;       // total across all zones
    private int coolingActivations;

    private final List<HeatingZone> zones;

    public HeatingSystem() {
        this.enabled = true;
        this.mode = Mode.AUTO;
        this.temperatureAdjustRate = 0.05; // per zone per tick; 6 zones = 0.30 max total
        this.currentAdjustment = 0;
        this.heatingActivations = 0;
        this.coolingActivations = 0;
        this.zones = new ArrayList<>();

        GardenLogger.getInstance().log("HEATING", "Heating System initialized (zonal mode).");
    }

    /** Add a zone heater at the given position with the given target temperature. */
    public HeatingZone addZone(Position position, double targetTemperature) {
        HeatingZone zone = new HeatingZone(position, targetTemperature);
        zones.add(zone);
        GardenLogger.getInstance().log("HEATING",
                String.format("Zone %s added at %s (target: %.1f°F)", zone.getId(), position, targetTemperature));
        return zone;
    }

    @Override
    public void update(Garden garden) {
        if (!enabled || mode == Mode.OFF) return;

        try {
            double netAdjustment = 0;

            for (HeatingZone zone : zones) {
                // Read this zone's sensor; fall back to garden-wide temperature
                TemperatureSensor ts = zone.getLinkedSensor();
                double zoneTemp = (ts != null)
                        ? ts.getCurrentReading()
                        : garden.getCurrentTemperature();

                boolean wasHeating = zone.isHeatingActive();
                boolean wasCooling = zone.isCoolingActive();

                if ((mode == Mode.AUTO || mode == Mode.HEATING)
                        && zoneTemp < zone.getTargetTemperature() - 2.0) {
                    // Zone too cold — activate heater
                    zone.setHeatingActive(true);
                    zone.setCoolingActive(false);
                    if (!wasHeating) {
                        zone.incrementHeatingActivations();
                        heatingActivations++;
                        GardenLogger.getInstance().log("HEATING",
                                String.format("%s HEAT ON: %.1f°F < target %.1f°F",
                                        zone.getId(), zoneTemp, zone.getTargetTemperature()));
                    }
                    netAdjustment += temperatureAdjustRate;

                } else if ((mode == Mode.AUTO || mode == Mode.COOLING)
                        && zoneTemp > zone.getTargetTemperature() + 2.0) {
                    // Zone too hot — activate cooler
                    zone.setCoolingActive(true);
                    zone.setHeatingActive(false);
                    if (!wasCooling) {
                        zone.incrementCoolingActivations();
                        coolingActivations++;
                        GardenLogger.getInstance().log("HEATING",
                                String.format("%s COOL ON: %.1f°F > target %.1f°F",
                                        zone.getId(), zoneTemp, zone.getTargetTemperature()));
                    }
                    netAdjustment -= temperatureAdjustRate;

                } else {
                    // Zone within range — deactivate
                    if (wasHeating || wasCooling) {
                        GardenLogger.getInstance().log("HEATING",
                                String.format("%s stable at %.1f°F (target %.1f°F)",
                                        zone.getId(), zoneTemp, zone.getTargetTemperature()));
                    }
                    zone.setHeatingActive(false);
                    zone.setCoolingActive(false);
                }
            }

            // Apply net adjustment once — avoids multiple adjustTemperature() calls
            if (netAdjustment != 0) {
                garden.adjustTemperature(netAdjustment);
            }
            currentAdjustment = netAdjustment;

        } catch (Exception e) {
            GardenLogger.getInstance().logError("HEATING", "Error in zonal heating update", e);
        }
    }

    // --- Getters & Setters ---
    public List<HeatingZone> getZones() { return zones; }
    public Mode getMode() { return mode; }
    public void setMode(Mode mode) {
        this.mode = mode;
        GardenLogger.getInstance().log("HEATING", "Mode changed to: " + mode);
    }

    /** Set target temperature on all zones (convenience method). */
    public void setTargetTemperature(double target) {
        for (HeatingZone zone : zones) {
            zone.setTargetTemperature(target);
        }
        GardenLogger.getInstance().log("HEATING",
                String.format("All zone targets set to %.1f°F", target));
    }

    /** Returns the target of the first zone (or default) for display purposes. */
    public double getTargetTemperature() {
        return zones.isEmpty() ? 65.0 : zones.get(0).getTargetTemperature();
    }

    public double getCurrentAdjustment() { return currentAdjustment; }

    public void setTemperatureAdjustRate(double rate) {
        this.temperatureAdjustRate = rate;
        GardenLogger.getInstance().log("HEATING",
                String.format("Per-zone adjust rate set to %.2f°F/tick", rate));
    }

    @Override
    public String getModuleName() { return "Heating/Climate Control System"; }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        GardenLogger.getInstance().log("HEATING",
                "Heating System " + (enabled ? "ENABLED" : "DISABLED"));
    }

    @Override
    public boolean isEnabled() { return enabled; }

    @Override
    public String getStatusSummary() {
        long heatingZones = zones.stream().filter(HeatingZone::isHeatingActive).count();
        long coolingZones = zones.stream().filter(HeatingZone::isCoolingActive).count();
        return String.format("Climate Control [%s] | Mode: %s | Zones: %d | Heating: %d | Cooling: %d | Adj: %+.2f",
                enabled ? "ON" : "OFF", mode, zones.size(), heatingZones, coolingZones, currentAdjustment);
    }
}
