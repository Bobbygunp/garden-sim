package garden.model.plants;

import garden.util.Position;

/** Carrot - cool weather crop, moderate water. */
public class Carrot extends Plant {
    public Carrot(Position position) {
        super("Carrot", "Daucus carota", position);
        this.idealTemperatureMin = 45;
        this.idealTemperatureMax = 75;
        this.waterNeedPerTick = 0.8;
        this.nutrientNeedPerTick = 0.3;
        this.lightNeedHours = 6;
        this.ticksToNextStage = 55;
        this.pestResistance = 0.4;
        this.waterTolerance = 0.5;
        this.droughtThreshold   = 10.0;
        this.overwaterThreshold = 85.0;
        this.idealWaterMin      = 35.0;
        this.idealWaterMax      = 65.0;
        this.drainageRate       = 1.5;  // Loose/sandy soil — roots need good aeration
        this.temperatureSensitivity = 0.25; // Hardy root veg — handles temperature swings well
    }
}
