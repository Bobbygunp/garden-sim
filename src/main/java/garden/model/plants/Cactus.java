package garden.model.plants;

import garden.util.Position;

/** Cactus - drought tolerant, heat loving. */
public class Cactus extends Plant {
    public Cactus(Position position) {
        super("Cactus", "Cactaceae", position);
        this.idealTemperatureMin = 50;
        this.idealTemperatureMax = 100;
        this.waterNeedPerTick = 0.15;
        this.nutrientNeedPerTick = 0.05;
        this.lightNeedHours = 10;
        this.ticksToNextStage = 80;
        this.pestResistance = 0.8;
        this.waterTolerance = 0.4;
        this.droughtThreshold   = 5.0;
        this.overwaterThreshold = 60.0;
        this.idealWaterMin      = 15.0;
        this.idealWaterMax      = 40.0;
        this.drainageRate       = 3.0;
        this.temperatureSensitivity = 0.10;
    }
}
