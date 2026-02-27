package garden.model.insects;

import garden.logging.GardenLogger;
import garden.model.plants.Plant;
import garden.util.Position;

import java.util.List;
import java.util.Random;

/**
 * Represents an insect in the garden. Insects can be beneficial (pollinators)
 * or harmful (pests that damage plants).
 */
public abstract class Insect {

    public enum InsectType {
        PEST, POLLINATOR, NEUTRAL, BENEFICIAL
    }

    private final String id;
    private final String name;
    private final InsectType type;
    private Position position;
    private Position previousPosition; // for smooth interpolation
    private boolean alive;
    private double damagePerTick;    // damage to plants per tick (pests only)
    private double movementRange;    // how far it can move per tick
    private int lifespanTicks;
    private int ageTicks;

    protected static final Random random = new Random();
    private static int idCounter = 0;

    public Insect(String name, InsectType type, Position position,
                  double damagePerTick, double movementRange, int lifespanTicks) {
        this.id = "INSECT-" + (++idCounter);
        this.name = name;
        this.type = type;
        this.position = position;
        this.previousPosition = position;
        this.damagePerTick = damagePerTick;
        this.movementRange = movementRange;
        this.lifespanTicks = lifespanTicks;
        this.ageTicks = 0;
        this.alive = true;

        GardenLogger.getInstance().log("INSECT",
                String.format("%s [%s] (%s) appeared at %s", name, id, type, position));
    }

    /**
     * Update insect behavior each tick. Moves around and interacts with plants.
     */
    public void update(List<Plant> nearbyPlants, int gridRows, int gridCols) {
        if (!alive) return;

        try {
            ageTicks++;

            // Update interpolation start point
            previousPosition = position;

            // Natural death
            if (ageTicks >= lifespanTicks) {
                die("Reached end of lifespan");
                return;
            }

            // Move randomly
            move(gridRows, gridCols);

            // Interact with nearby plants (throttled logging every 50 ticks to avoid spam)
            boolean shouldLog = (ageTicks % 50 == 0);

            if (type == InsectType.PEST) {
                for (Plant plant : nearbyPlants) {
                    if (plant.isAlive() && plant.getPosition().distanceTo(position) <= 2.0) {
                        plant.applyPestDamage(damagePerTick);
                        if (shouldLog) {
                            GardenLogger.getInstance().log("INSECT",
                                    String.format("%s [%s] is feeding on %s at %s",
                                            name, id, plant.getName(), plant.getPosition()));
                        }
                    }
                }
            } else if (type == InsectType.POLLINATOR) {
                for (Plant plant : nearbyPlants) {
                    if (plant.isAlive() && plant.getPosition().distanceTo(position) <= 1.5
                            && plant.getGrowthStage() == Plant.GrowthStage.FLOWERING) {
                        if (shouldLog) {
                            GardenLogger.getInstance().log("INSECT",
                                    String.format("%s [%s] is pollinating %s at %s",
                                            name, id, plant.getName(), plant.getPosition()));
                        }
                    }
                }
            }

        } catch (Exception e) {
            GardenLogger.getInstance().logError("INSECT",
                    "Error updating insect " + id, e);
        }
    }

    private void move(int gridRows, int gridCols) {
        // Calculate potential delta
        double dr = (random.nextInt(3) - 1) * movementRange;
        double dc = (random.nextInt(3) - 1) * movementRange;

        // If movementRange < 1.0, treat it as a probability of moving 1 full cell
        int moveR = (int) dr;
        if (moveR == 0 && dr != 0 && random.nextDouble() < Math.abs(dr)) {
            moveR = (dr > 0) ? 1 : -1;
        }

        int moveC = (int) dc;
        if (moveC == 0 && dc != 0 && random.nextDouble() < Math.abs(dc)) {
            moveC = (dc > 0) ? 1 : -1;
        }

        int newRow = Math.max(0, Math.min(gridRows - 1, position.getRow() + moveR));
        int newCol = Math.max(0, Math.min(gridCols - 1, position.getCol() + moveC));
        
        if (newRow != position.getRow() || newCol != position.getCol()) {
            position = new Position(newRow, newCol);
        }
    }

    /**
     * Biological control: BENEFICIAL insects hunt and eat nearby pests.
     * Called from Garden.tick() after normal insect updates, passing all live insects.
     * Only BENEFICIAL-type insects (e.g., Ladybugs) do anything here.
     *
     * @param allInsects the full list of live insects in the garden
     * @param huntRadius how close a pest must be to get eaten
     * @param killChance probability (0.0-1.0) of killing a pest per tick when in range
     */
    public void predateInsects(List<Insect> allInsects, double huntRadius, double killChance) {
        if (!alive || type != InsectType.BENEFICIAL) return;

        for (Insect target : allInsects) {
            if (target == this || !target.isAlive() || target.getType() != InsectType.PEST) continue;

            if (target.getPosition().distanceTo(this.position) <= huntRadius) {
                if (random.nextDouble() < killChance) {
                    target.kill("Eaten by " + name + " [" + id + "]");
                    // Only eat one pest per tick (realistic — ladybugs eat ~50 aphids/day)
                    if (ageTicks % 50 == 0) {
                        GardenLogger.getInstance().log("INSECT",
                                String.format("%s [%s] ate %s [%s] at %s (biological control)",
                                        name, id, target.getName(), target.getId(), position));
                    }
                    return;
                }
            }
        }
    }

    /** Kill this insect (e.g., via pest control). */
    public void kill(String reason) {
        die(reason);
    }

    private void die(String reason) {
        alive = false;
        GardenLogger.getInstance().log("INSECT",
                String.format("%s [%s] died. Reason: %s", name, id, reason));
    }

    // --- Getters ---
    public String getId() { return id; }
    public String getName() { return name; }
    public InsectType getType() { return type; }
    public Position getPosition() { return position; }
    public Position getPreviousPosition() { return previousPosition; }
    public boolean isAlive() { return alive; }
    public int getAgeTicks() { return ageTicks; }
    public double getDamagePerTick() { return damagePerTick; }

    @Override
    public String toString() {
        return String.format("Insect[%s, %s, %s, %s]", id, name, type, alive ? "alive" : "dead");
    }
}
