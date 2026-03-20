package garden.model.insects;

import garden.util.Position;

/** Caterpillar - slow-moving pest that eats leaves. */
public class Caterpillar extends Insect {
    public Caterpillar(Position position) {
        super("Caterpillar", InsectType.PEST, position, 1.5, 0.5, 150);
    }
}
