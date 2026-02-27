package garden.model.sensors;

import garden.util.Position;

/** Measures air temperature in Fahrenheit. */
public class TemperatureSensor extends Sensor {
    public TemperatureSensor(Position position) {
        super("Temperature Sensor", "°F", position, 40.0, 95.0);
    }

    @Override
    public double measure(double environmentalValue) {
        return environmentalValue + (Math.random() - 0.5) * 1.0;
    }
}
