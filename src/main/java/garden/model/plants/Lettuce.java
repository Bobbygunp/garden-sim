package garden.model.plants;

import garden.util.Position;

/** Lettuce - cool weather, lots of water. */
public class Lettuce extends Plant {
    public Lettuce(Position position) {
        super("Lettuce", "Lactuca sativa", position);
        this.idealTemperatureMin = 40;
        this.idealTemperatureMax = 70;
        this.waterNeedPerTick = 1.1;
        this.nutrientNeedPerTick = 0.25;
        this.lightNeedHours = 5;
        this.ticksToNextStage = 35;
        this.pestResistance = 0.15;
        this.waterTolerance = 0.8;
        this.droughtThreshold   = 20.0;
        this.overwaterThreshold = 95.0;
        this.idealWaterMin      = 55.0;
        this.idealWaterMax      = 80.0;
        this.drainageRate       = 0.4;  // Clay-rich soil — retains moisture, drains slowly
        this.temperatureSensitivity = 0.45; // Bolts quickly in heat, stunts in cold
    }
}
