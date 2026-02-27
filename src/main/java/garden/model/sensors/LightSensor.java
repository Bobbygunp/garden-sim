package garden.model.sensors;

import garden.util.Position;

/** Measures light intensity (0-100 arbitrary units). */
public class LightSensor extends Sensor {
    public LightSensor(Position position) {
        super("Light Sensor", "lux", position, 20.0, 90.0);
    }

    @Override
    public double measure(double environmentalValue) {
        return environmentalValue + (Math.random() - 0.5) * 3.0;
    }
}
