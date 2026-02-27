package garden.model.insects;

import garden.util.Position;

/** Bee - beneficial pollinator. */
public class Bee extends Insect {
    public Bee(Position position) {
        super("Bee", InsectType.POLLINATOR, position,
                0.0,   // no damage
                2.0,   // faster movement
                300);  // longer lifespan
    }
}
