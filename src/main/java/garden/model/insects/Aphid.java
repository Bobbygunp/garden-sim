package garden.model.insects;

import garden.util.Position;

/** Aphid - common garden pest that damages plants. */
public class Aphid extends Insect {
    public Aphid(Position position) {
        super("Aphid", InsectType.PEST, position, 0.8, 1.0, 200);
    }
}
