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
    }
}
