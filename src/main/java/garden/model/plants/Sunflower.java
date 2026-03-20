package garden.model.plants;

import garden.util.Position;

/** Sunflower - loves sun and warmth. */
public class Sunflower extends Plant {
    public Sunflower(Position position) {
        super("Sunflower", "Helianthus annuus", position);
        this.idealTemperatureMin = 55;
        this.idealTemperatureMax = 91;
        this.waterNeedPerTick = 1.5;
        this.nutrientNeedPerTick = 0.5;
        this.lightNeedHours = 10;
        this.ticksToNextStage = 40;
        this.pestResistance = 0.5;
        this.waterTolerance = 0.7;
        this.droughtThreshold   = 15.0;
        this.overwaterThreshold = 90.0;
        this.idealWaterMin      = 45.0;
        this.idealWaterMax      = 75.0;
        this.drainageRate       = 1.0;
        this.temperatureSensitivity = 0.20;
    }
}
