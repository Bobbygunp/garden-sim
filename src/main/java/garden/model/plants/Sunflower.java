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
    }
}
