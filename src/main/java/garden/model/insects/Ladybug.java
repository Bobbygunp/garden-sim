package garden.model.insects;

import garden.util.Position;

/** Ladybug - beneficial predator that hunts aphids and other pests. */
public class Ladybug extends Insect {
    public Ladybug(Position position) {
        super("Ladybug", InsectType.BENEFICIAL, position, 0.0, 1.5, 400);
    }
}
