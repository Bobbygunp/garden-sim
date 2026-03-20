package garden.model.plants;

import garden.util.Position;

/** Tomato plant - moderate water needs, warm temperatures. */
public class Tomato extends Plant {
    public Tomato(Position position) {
        super("Tomato", "Solanum lycopersicum", position);
        this.idealTemperatureMin = 60;
        this.idealTemperatureMax = 85;
        this.waterNeedPerTick = 1.2;
        this.nutrientNeedPerTick = 0.5;
        this.lightNeedHours = 8;
        this.ticksToNextStage = 50;
        this.pestResistance = 0.3;
        this.waterTolerance = 0.7;
        this.droughtThreshold   = 15.0;
        this.overwaterThreshold = 90.0;
        this.idealWaterMin      = 45.0;
        this.idealWaterMax      = 75.0;
        this.drainageRate       = 1.0;
        this.temperatureSensitivity = 0.55;
    }
}
