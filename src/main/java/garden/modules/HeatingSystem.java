package garden.modules;

import garden.core.Garden;
import garden.logging.GardenLogger;
import garden.model.sensors.Sensor;
import garden.model.sensors.TemperatureSensor;

/**
 * Module 2: Heating / Climate Control System
 * Monitors temperature sensors and adjusts the garden's temperature
 * by activating heaters or coolers as needed.
 */
public class HeatingSystem implements GardenModule {

    public enum Mode {
        OFF, HEATING, COOLING, AUTO
    }

    private boolean enabled;
    private Mode mode;
    private double targetTemperature;
    private double temperatureAdjustRate; // degrees per tick adjustment
    private double currentAdjustment;     // current temp adjustment being applied
    private int heatingActivations;
    private int coolingActivations;

    public HeatingSystem() {
        this.enabled = true;
        this.mode = Mode.AUTO;
        this.targetTemperature = 65.0; // ideal default in °F
        this.temperatureAdjustRate = 2.0;
        this.currentAdjustment = 0;
        this.heatingActivations = 0;
        this.coolingActivations = 0;

        GardenLogger.getInstance().log("HEATING",
                String.format("Heating System initialized. Target: %.1f°F, Mode: %s",
                        targetTemperature, mode));
    }

    @Override
    public void update(Garden garden) {
        if (!enabled) return;

        try {
            double currentTemp = garden.getCurrentTemperature();

            if (mode == Mode.AUTO) {
                if (currentTemp < targetTemperature - 2) {
                    // Too cold - heat up
                    currentAdjustment = temperatureAdjustRate;
                    garden.adjustTemperature(currentAdjustment);
                    heatingActivations++;
                    GardenLogger.getInstance().log("HEATING",
                            String.format("Heater ON: Current %.1f°F → adjusting +%.1f°F (target: %.1f°F)",
                                    currentTemp, currentAdjustment, targetTemperature));
                } else if (currentTemp > targetTemperature + 2) {
                    // Too hot - cool down
                    currentAdjustment = -temperatureAdjustRate;
                    garden.adjustTemperature(currentAdjustment);
                    coolingActivations++;
                    GardenLogger.getInstance().log("HEATING",
                            String.format("Cooler ON: Current %.1f°F → adjusting %.1f°F (target: %.1f°F)",
                                    currentTemp, currentAdjustment, targetTemperature));
                } else {
                    if (currentAdjustment != 0) {
                        GardenLogger.getInstance().log("HEATING",
                                String.format("Climate control OFF: %.1f°F is within target range", currentTemp));
                    }
                    currentAdjustment = 0;
                }
            } else if (mode == Mode.HEATING) {
                currentAdjustment = temperatureAdjustRate;
                garden.adjustTemperature(currentAdjustment);
            } else if (mode == Mode.COOLING) {
                currentAdjustment = -temperatureAdjustRate;
                garden.adjustTemperature(currentAdjustment);
            }

        } catch (Exception e) {
            GardenLogger.getInstance().logError("HEATING", "Error in heating system update", e);
        }
    }

    // --- Getters & Setters ---
    public Mode getMode() { return mode; }
    public void setMode(Mode mode) {
        this.mode = mode;
        GardenLogger.getInstance().log("HEATING", "Mode changed to: " + mode);
    }

    public double getTargetTemperature() { return targetTemperature; }
    public void setTargetTemperature(double target) {
        this.targetTemperature = target;
        GardenLogger.getInstance().log("HEATING",
                String.format("Target temperature set to %.1f°F", target));
    }

    public double getCurrentAdjustment() { return currentAdjustment; }

    public void setTemperatureAdjustRate(double rate) {
        this.temperatureAdjustRate = rate;
        GardenLogger.getInstance().log("HEATING",
                String.format("Temperature adjust rate set to %.1f°F/tick", rate));
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
        return String.format("Climate Control [%s] | Mode: %s | Target: %.1f°F | Adj: %+.1f | Heat: %d | Cool: %d",
                enabled ? "ON" : "OFF", mode, targetTemperature, currentAdjustment,
                heatingActivations, coolingActivations);
    }
}
