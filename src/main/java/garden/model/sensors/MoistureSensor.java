package garden.model.sensors;

import garden.util.Position;

/** Measures soil moisture level (0-100%). */
public class MoistureSensor extends Sensor {
    public MoistureSensor(Position position) {
        super("Moisture Sensor", "%", position, 20.0, 80.0);
    }

    @Override
    public double measure(double environmentalValue) {
        // Environmental value here is irrelevant; we measure the plants near the sensor
        // This is much more realistic for a physical soil sensor.
        return environmentalValue; // Default fallback
    }

    /** 
     * Realistic measure: average the water levels of living plants 
     * physically near the sensor (within radius 3).
     */
    public void update(java.util.List<garden.model.plants.Plant> nearbyPlants) {
        double avg = nearbyPlants.stream()
                .filter(p -> p.isAlive() && p.getPosition().distanceTo(getPosition()) <= 3.0)
                .mapToDouble(p -> p.getWaterLevel())
                .average()
                .orElse(50.0);
        
        // Add noise for realism
        super.update(avg + (Math.random() - 0.5) * 4.0);
    }
}
