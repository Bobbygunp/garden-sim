package garden.modules;

import garden.core.Garden;
import garden.logging.GardenLogger;

/**
 * Module 4: Lighting System
 * Provides supplemental lighting when natural light is insufficient.
 * Simulates day/night cycles and activates grow lights as needed.
 */
public class LightingSystem implements GardenModule {

    private boolean enabled;
    private boolean lightsOn;
    private double currentLightLevel;    // 0-100
    private double targetLightLevel;     // desired light level
    private int lightOnTicks;
    private int totalActivations;

    public LightingSystem() {
        this.enabled = true;
        this.lightsOn = false;
        this.currentLightLevel = 50;
        this.targetLightLevel = 60;
        this.lightOnTicks = 0;
        this.totalActivations = 0;

        GardenLogger.getInstance().log("LIGHTING",
                String.format("Lighting System initialized. Target light level: %.0f", targetLightLevel));
    }

    @Override
    public void update(Garden garden) {
        if (!enabled) return;

        try {
            currentLightLevel = garden.getCurrentLightLevel();

            if (currentLightLevel < targetLightLevel - 10) {
                if (!lightsOn) {
                    lightsOn = true;
                    totalActivations++;
                    GardenLogger.getInstance().log("LIGHTING",
                            String.format("Grow lights ON: Natural light %.0f below target %.0f",
                                    currentLightLevel, targetLightLevel));
                }
                lightOnTicks++;
                // Supplement light
                double supplement = targetLightLevel - currentLightLevel;
                garden.adjustLightLevel(supplement * 0.5);
            } else {
                if (lightsOn) {
                    lightsOn = false;
                    GardenLogger.getInstance().log("LIGHTING",
                            String.format("Grow lights OFF: Natural light %.0f sufficient", currentLightLevel));
                }
            }

        } catch (Exception e) {
            GardenLogger.getInstance().logError("LIGHTING", "Error in lighting system update", e);
        }
    }

    // --- Getters & Setters ---
    public boolean isLightsOn() { return lightsOn; }
    public double getCurrentLightLevel() { return currentLightLevel; }
    public double getTargetLightLevel() { return targetLightLevel; }
    public void setTargetLightLevel(double target) {
        this.targetLightLevel = target;
        GardenLogger.getInstance().log("LIGHTING", "Target light level set to: " + target);
    }

    @Override
    public String getModuleName() { return "Lighting System"; }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        GardenLogger.getInstance().log("LIGHTING",
                "Lighting System " + (enabled ? "ENABLED" : "DISABLED"));
    }

    @Override
    public boolean isEnabled() { return enabled; }

    @Override
    public String getStatusSummary() {
        return String.format("Lighting [%s] | Lights: %s | Level: %.0f/%.0f | Activations: %d | On-ticks: %d",
                enabled ? "ON" : "OFF", lightsOn ? "ON" : "OFF",
                currentLightLevel, targetLightLevel, totalActivations, lightOnTicks);
    }
}
