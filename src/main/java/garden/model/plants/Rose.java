package garden.model.plants;

import garden.util.Position;

/** Rose - needs good drainage, moderate temperatures. */
public class Rose extends Plant {
    public Rose(Position position) {
        super("Rose", "Rosa", position);
        this.idealTemperatureMin = 55;
        this.idealTemperatureMax = 80;
        this.waterNeedPerTick = 1.0;
        this.nutrientNeedPerTick = 0.4;
        this.lightNeedHours = 6;
        this.ticksToNextStage = 60;
        this.pestResistance = 0.2;
        this.waterTolerance = 0.6;
        this.droughtThreshold   = 10.0;
        this.overwaterThreshold = 85.0;
        this.idealWaterMin      = 40.0;
        this.idealWaterMax      = 70.0;
        this.drainageRate       = 1.2;
        this.temperatureSensitivity = 0.30;
    }
}
