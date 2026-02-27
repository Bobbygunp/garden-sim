package garden.modules;

import garden.core.Garden;
import garden.logging.GardenLogger;
import garden.model.insects.Insect;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Module 3: Pest Control System
 * Monitors insect populations and automatically applies pest control
 * when pest levels exceed thresholds. Avoids harming beneficial insects.
 */
public class PestControl implements GardenModule {

    public enum PestControlMethod {
        ORGANIC,     // slower but safe for beneficials
        CHEMICAL,    // fast but kills all insects
        TARGETED     // only kills pests, moderate speed
    }

    private boolean enabled;
    private PestControlMethod method;
    private int pestThreshold;           // number of pests before activating
    private int pestControlActivations;
    private int pestsEliminated;
    private int ticksBetweenChecks;
    private int tickCounter;
    private int lastActivationTick = -1; // track for UI effects

    public PestControl() {
        this.enabled = true;
        this.method = PestControlMethod.TARGETED;
        this.pestThreshold = 1; // Trigger immediately on discovery
        this.pestControlActivations = 0;
        this.pestsEliminated = 0;
        this.ticksBetweenChecks = 5; // Check twice as often
        this.tickCounter = 0;

        GardenLogger.getInstance().log("PEST_CONTROL",
                String.format("Pest Control initialized. Method: %s, Threshold: %d pests",
                        method, pestThreshold));
    }

    @Override
    public void update(Garden garden) {
        if (!enabled) return;

        try {
            tickCounter++;
            if (tickCounter < ticksBetweenChecks) return;
            tickCounter = 0;

            // Count alive pests
            List<Insect> alivePests = garden.getInsects().stream()
                    .filter(Insect::isAlive)
                    .filter(i -> i.getType() == Insect.InsectType.PEST)
                    .collect(Collectors.toList());

            if (alivePests.size() >= pestThreshold) {
                applyPestControl(garden, alivePests);
            }

        } catch (Exception e) {
            GardenLogger.getInstance().logError("PEST_CONTROL", "Error in pest control update", e);
        }
    }

    private void applyPestControl(Garden garden, List<Insect> pests) {
        pestControlActivations++;
        lastActivationTick = garden.getCurrentTick();

        GardenLogger.getInstance().log("PEST_CONTROL",
                String.format("Activating %s pest control! %d pests detected.",
                        method, pests.size()));

        switch (method) {
            case CHEMICAL:
                // Kills all insects, including beneficial ones
                for (Insect insect : garden.getInsects()) {
                    if (insect.isAlive()) {
                        insect.kill("Chemical pest control");
                        pestsEliminated++;
                    }
                }
                GardenLogger.getInstance().logWarning("PEST_CONTROL",
                        "Chemical method: ALL insects eliminated (including beneficials).");
                break;

            case TARGETED:
                // Only kills pest-type insects
                for (Insect pest : pests) {
                    if (pest.isAlive()) {
                        pest.kill("Targeted pest control");
                        pestsEliminated++;
                    }
                }
                GardenLogger.getInstance().log("PEST_CONTROL",
                        String.format("Targeted method: %d pests eliminated. Beneficials safe.", pests.size()));
                break;

            case ORGANIC:
                // Kills some pests (70% chance each), never harms beneficials
                int killed = 0;
                for (Insect pest : pests) {
                    if (pest.isAlive() && Math.random() < 0.7) {
                        pest.kill("Organic pest control");
                        killed++;
                        pestsEliminated++;
                    }
                }
                GardenLogger.getInstance().log("PEST_CONTROL",
                        String.format("Organic method: %d/%d pests eliminated.", killed, pests.size()));
                break;
        }
    }

    /** Manually trigger pest control. */
    public void manualPestControl(Garden garden) {
        GardenLogger.getInstance().log("USER_ACTION", "Manual pest control triggered.");
        List<Insect> pests = garden.getInsects().stream()
                .filter(Insect::isAlive)
                .filter(i -> i.getType() == Insect.InsectType.PEST)
                .collect(Collectors.toList());
        if (!pests.isEmpty()) {
            applyPestControl(garden, pests);
        } else {
            GardenLogger.getInstance().log("PEST_CONTROL", "No pests found to eliminate.");
        }
    }

    // --- Getters & Setters ---
    public PestControlMethod getMethod() { return method; }
    public void setMethod(PestControlMethod method) {
        this.method = method;
        GardenLogger.getInstance().log("PEST_CONTROL", "Method changed to: " + method);
    }

    public int getPestThreshold() { return pestThreshold; }
    public void setPestThreshold(int threshold) { this.pestThreshold = threshold; }
    public int getPestsEliminated() { return pestsEliminated; }
    public int getLastActivationTick() { return lastActivationTick; }

    @Override
    public String getModuleName() { return "Pest Control System"; }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        GardenLogger.getInstance().log("PEST_CONTROL",
                "Pest Control " + (enabled ? "ENABLED" : "DISABLED"));
    }

    @Override
    public boolean isEnabled() { return enabled; }

    @Override
    public String getStatusSummary() {
        return String.format("Pest Control [%s] | Method: %s | Activations: %d | Eliminated: %d",
                enabled ? "ON" : "OFF", method, pestControlActivations, pestsEliminated);
    }
}
