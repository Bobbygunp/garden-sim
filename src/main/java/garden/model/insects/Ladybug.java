package garden.model.insects;

import garden.util.Position;

/** Ladybug - beneficial predator that hunts and eats aphids and other pests.
 *  Real ladybugs consume ~50 aphids per day and are the most well-known
 *  biological control agent in agriculture. */
public class Ladybug extends Insect {
    public Ladybug(Position position) {
        super("Ladybug", InsectType.BENEFICIAL, position,
                0.0,   // no damage to plants
                1.5,   // moderate movement
                400);  // long lifespan
    }
}
