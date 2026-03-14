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
        this.temperatureSensitivity = 0.55; // Tropical — rapid damage from cold or heat
    }
}
