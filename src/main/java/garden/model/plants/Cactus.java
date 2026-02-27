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
    }
}
