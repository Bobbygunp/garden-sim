package garden.model.sensors;

import garden.logging.GardenLogger;
import garden.util.Position;

/**
 * Abstract base class for garden sensors.
 * Sensors monitor environmental conditions and report readings.
 */
public abstract class Sensor {

    private final String id;
    private final String name;
    private final String measurementUnit;
    private Position position;
    private double currentReading;
    private double minThreshold;
    private double maxThreshold;
    private boolean alertTriggered;

    private static int idCounter = 0;

    public Sensor(String name, String unit, Position position,
                  double minThreshold, double maxThreshold) {
        this.id = "SENSOR-" + (++idCounter);
        this.name = name;
        this.measurementUnit = unit;
        this.position = position;
        this.minThreshold = minThreshold;
        this.maxThreshold = maxThreshold;
        this.currentReading = 0;
        this.alertTriggered = false;

        GardenLogger.getInstance().log("SENSOR",
                String.format("%s [%s] installed at %s (range: %.1f-%.1f %s)",
                        name, id, position, minThreshold, maxThreshold, unit));
    }

    /** Take a new reading. Subclasses implement the actual measurement logic. */
    public abstract double measure(double environmentalValue);

    /** Update the sensor with a new measurement. */
    public void update(double environmentalValue) {
        try {
            currentReading = measure(environmentalValue);

            boolean wasAlert = alertTriggered;
            alertTriggered = currentReading < minThreshold || currentReading > maxThreshold;

            if (alertTriggered && !wasAlert) {
                GardenLogger.getInstance().logWarning("SENSOR",
                        String.format("ALERT: %s [%s] at %s reading %.1f %s (threshold: %.1f-%.1f)",
                                name, id, position, currentReading, measurementUnit,
                                minThreshold, maxThreshold));
            } else if (!alertTriggered && wasAlert) {
                GardenLogger.getInstance().log("SENSOR",
                        String.format("%s [%s] returned to normal: %.1f %s",
                                name, id, currentReading, measurementUnit));
            }
        } catch (Exception e) {
            GardenLogger.getInstance().logError("SENSOR", "Error reading " + id, e);
        }
    }

    // --- Getters ---
    public String getId() { return id; }
    public String getName() { return name; }
    public Position getPosition() { return position; }
    public double getCurrentReading() { return currentReading; }
    public boolean isAlertTriggered() { return alertTriggered; }
    public String getMeasurementUnit() { return measurementUnit; }
    public double getMinThreshold() { return minThreshold; }
    public double getMaxThreshold() { return maxThreshold; }

    // --- Setters for thresholds ---
    public void setMinThreshold(double min) { this.minThreshold = min; }
    public void setMaxThreshold(double max) { this.maxThreshold = max; }
}
