package garden.model.insects;

import garden.util.Position;

/** Caterpillar - slow-moving pest that eats leaves. */
public class Caterpillar extends Insect {
    public Caterpillar(Position position) {
        super("Caterpillar", InsectType.PEST, position,
                1.5,   // damage per tick (reduced from 3.5)
                0.5,   // slow movement
                150);  // shorter lifespan
    }
}
