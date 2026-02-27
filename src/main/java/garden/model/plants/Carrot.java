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
    }
}
