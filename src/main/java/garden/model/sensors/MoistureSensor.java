package garden.model.sensors;

import garden.util.Position;

/** Measures soil moisture level (0-100%). */
public class MoistureSensor extends Sensor {
    private double minReading;
    private double avgReading;
    
    // Zoned Sensing: Precise rectangular coverage
    private int minRow, maxRow, minCol, maxCol;
    private boolean useZonedSensing = false;

    public MoistureSensor(Position position) {
        super("Moisture Sensor", "%", position, 20.0, 80.0);
        setUpdateInterval(4); // Sense every 4 ticks (~30 minutes)
        this.minReading = 50.0;
        this.avgReading = 50.0;
    }

    /** Configure the sensor to cover a specific rectangular zone. */
    public void setSensingRange(int minRow, int maxRow, int minCol, int maxCol) {
        this.minRow = minRow;
        this.maxRow = maxRow;
        this.minCol = minCol;
        this.maxCol = maxCol;
        this.useZonedSensing = true;
    }

    public boolean isZoned() { return useZonedSensing; }
    public int getZoneMinRow() { return minRow; }
    public int getZoneMaxRow() { return maxRow; }
    public int getZoneMinCol() { return minCol; }
    public int getZoneMaxCol() { return maxCol; }

    @Override
    public double measure(double environmentalValue) {
        return environmentalValue;
    }

    public double getMinReading() { return minReading; }
    public double getAvgReading() { return avgReading; }

    /** 
     * Smart Zoned Sensing: tracks both the minimum and average water levels
     * of plants strictly within its configured zone boundaries.
     */
    public void update(java.util.List<garden.model.plants.Plant> nearbyPlants, int currentTick) {
        if (currentTick % getUpdateInterval() != 0) return;

        java.util.List<garden.model.plants.Plant> targets = nearbyPlants.stream()
                .filter(p -> p.isAlive())
                .filter(p -> {
                    if (useZonedSensing) {
                        Position pos = p.getPosition();
                        return pos.getRow() >= minRow && pos.getRow() <= maxRow &&
                               pos.getCol() >= minCol && pos.getCol() <= maxCol;
                    } else {
                        return p.getPosition().distanceTo(getPosition()) <= 3.0;
                    }
                })
                .toList();

        if (targets.isEmpty()) {
            this.minReading = 50.0;
            this.avgReading = 50.0;
        } else {
            // Fail-Safe: minReading is the thirstiest plant in the specific zone.
            this.minReading = targets.stream().mapToDouble(p -> p.getWaterLevel()).min().orElse(50.0);
            this.avgReading = targets.stream().mapToDouble(p -> p.getWaterLevel()).average().orElse(50.0);
        }
        
        // We report the MINIMUM as the primary reading to trigger the watering system.
        super.update(minReading, currentTick);
    }
}
