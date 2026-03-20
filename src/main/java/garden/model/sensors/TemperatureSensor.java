package garden.model.sensors;

import garden.util.Position;

/** Measures air temperature in Fahrenheit. Supports optional zone bounds for zoned climate control. */
public class TemperatureSensor extends Sensor {

    private int minRow, maxRow, minCol, maxCol;
    private boolean useZonedSensing = false;

    public TemperatureSensor(Position position) {
        super("Temperature Sensor", "°F", position, 40.0, 95.0);
        setUpdateInterval(10);
    }

    /** Configure this sensor to represent a specific rectangular zone. */
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
        return environmentalValue + (Math.random() - 0.5) * 1.0;
    }
}
