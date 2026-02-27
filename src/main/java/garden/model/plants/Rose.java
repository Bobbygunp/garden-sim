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
    }
}
