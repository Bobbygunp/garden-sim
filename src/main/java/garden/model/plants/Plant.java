package garden.model.plants;

import garden.logging.GardenLogger;
import garden.util.Position;

/**
 * Abstract base class for all plants in the garden.
 * Each plant has health, water level, growth stage, and environmental needs.
 */
public abstract class Plant {

    public enum GrowthStage {
        SEED, SPROUT, VEGETATIVE, FLOWERING, FRUITING, MATURE, WILTING, DEAD
    }

    // Identity
    private final String id;
    private final String name;
    private final String species;
    private Position position;

    // Health & Status
    private double health;          // 0.0 - 100.0
    private double waterLevel;      // 0.0 - 100.0
    private double nutrientLevel;   // 0.0 - 100.0
    private GrowthStage growthStage;
    private int ageTicks;           // simulation ticks since planted
    private boolean alive;

    // Daily Light Integral (DLI) tracking — accumulates light each tick,
    // evaluates satisfaction once per simulated day (200 ticks).
    private double lightAccumulator;    // sum of light values this day cycle
    private int lightTickCounter;       // ticks since last daily evaluation
    private double lastLightSatisfaction = 1.0; // ratio of received vs needed (0.0-2.0+)
    private static final int DAY_CYCLE_TICKS = 200;

    // Environmental preferences (subclasses set these)
    protected double idealTemperatureMin;
    protected double idealTemperatureMax;
    protected double waterNeedPerTick;      // how much water consumed per tick
    protected double nutrientNeedPerTick;   // how many nutrients consumed per tick
    protected double lightNeedHours;        // ideal light hours per day
    protected int ticksToNextStage;         // ticks needed to advance growth stage
    protected double pestResistance;        // 0.0 - 1.0

    private static int idCounter = 0;

    public Plant(String name, String species, Position position) {
        this.id = "PLANT-" + (++idCounter);
        this.name = name;
        this.species = species;
        this.position = position;
        this.health = 100.0;
        this.waterLevel = 50.0;
        this.nutrientLevel = 50.0;
        this.growthStage = GrowthStage.SEED;
        this.ageTicks = 0;
        this.alive = true;

        GardenLogger.getInstance().log("PLANT",
                String.format("%s [%s] (%s) planted at %s", name, id, species, position));
    }

    /**
     * Called each simulation tick. Updates plant state based on environmental conditions.
     *
     * @param currentTemperature air temperature in °F
     * @param currentLight       light intensity 0-100
     * @param currentHumidity    relative humidity 0-100%
     */
    public void update(double currentTemperature, double currentLight, double currentHumidity) {
        if (!alive) return;

        try {
            ageTicks++;

            // Consume water and nutrients
            waterLevel = Math.max(0, waterLevel - waterNeedPerTick);
            nutrientLevel = Math.max(0, nutrientLevel - nutrientNeedPerTick);

            // Calculate health effects
            double healthDelta = 0;

            // Water stress
            if (waterLevel < 10) {
                healthDelta -= 2.5;
                if (waterLevel <= 0) {
                    GardenLogger.getInstance().logWarning("PLANT",
                            String.format("%s [%s] is critically dehydrated!", name, id));
                }
            } else if (waterLevel > 90) {
                healthDelta -= 0.5; // overwatering
            } else if (waterLevel > 40 && waterLevel < 70) {
                healthDelta += 1.2; // IDEAL RANGE: Improved healing
            } else {
                healthDelta += 0.5; // Acceptable range
            }

            // Temperature stress
            if (currentTemperature < idealTemperatureMin || currentTemperature > idealTemperatureMax) {
                double tempStress = 0;
                if (currentTemperature < idealTemperatureMin) {
                    tempStress = (idealTemperatureMin - currentTemperature) * 0.35;
                } else {
                    tempStress = (currentTemperature - idealTemperatureMax) * 0.35;
                }
                healthDelta -= tempStress;
            } else {
                healthDelta += 0.6; // Good temp healing
            }

            // Nutrient stress
            if (nutrientLevel < 10) {
                healthDelta -= 1.5;
            } else if (nutrientLevel > 40) {
                healthDelta += 0.5; // Nutrient boost
            }

            // ---- DAILY LIGHT INTEGRAL (DLI) ----
            // Accumulate light over the day cycle, then evaluate once per "day".
            // lightNeedHours maps to how many hours of good light the plant needs.
            // We convert the accumulated average into equivalent "light hours" and
            // compare against lightNeedHours to derive a satisfaction ratio.
            lightAccumulator += currentLight;
            lightTickCounter++;

            if (lightTickCounter >= DAY_CYCLE_TICKS) {
                double avgLight = lightAccumulator / DAY_CYCLE_TICKS;
                // Convert average light to equivalent "good light hours" per day.
                // If avgLight==50 (half-strength all day), that's ~12 hours equivalent.
                // Full daylight (100) all day would be 24 hours equivalent.
                double lightHoursReceived = (avgLight / 100.0) * 24.0;
                lastLightSatisfaction = Math.min(2.0, lightHoursReceived / lightNeedHours);

                // Log extreme light stress
                if (lastLightSatisfaction < 0.5) {
                    GardenLogger.getInstance().logWarning("PLANT",
                            String.format("%s [%s] severe light deficit: received %.1fh, needs %.0fh",
                                    name, id, lightHoursReceived, lightNeedHours));
                }
                lightAccumulator = 0;
                lightTickCounter = 0;
            }

            // Apply light satisfaction effect each tick (based on last daily evaluation)
            if (lastLightSatisfaction < 0.7) {
                // Insufficient light — stress proportional to deficit
                healthDelta -= (0.7 - lastLightSatisfaction) * 1.5;
            } else if (lastLightSatisfaction >= 0.9) {
                // Good light — small health boost
                healthDelta += 0.2;
            }

            // ---- HUMIDITY EFFECTS ----
            // High humidity (>85%) promotes fungal disease — health drain.
            // Low humidity (<30%) causes transpiration stress — mild penalty.
            // Optimal range (40-70%) gives a small bonus.
            if (currentHumidity > 85) {
                double fungalRisk = (currentHumidity - 85) / 15.0; // 0.0 at 85%, 1.0 at 100%
                healthDelta -= fungalRisk * 0.8;
                if (currentHumidity > 90 && ageTicks % 100 == 0) {
                    GardenLogger.getInstance().logWarning("PLANT",
                            String.format("%s [%s] at risk of fungal disease (humidity: %.0f%%)",
                                    name, id, currentHumidity));
                }
            } else if (currentHumidity < 30) {
                double dryStress = (30 - currentHumidity) / 30.0; // 0.0 at 30%, 1.0 at 0%
                healthDelta -= dryStress * 0.6;
            } else if (currentHumidity >= 40 && currentHumidity <= 70) {
                healthDelta += 0.15; // Optimal humidity bonus
            }

            // Apply health change
            health = Math.max(0, Math.min(100, health + healthDelta));

            // Check for death
            if (health <= 0) {
                die("Health reached zero");
                return;
            }

            // Growth stage advancement (ensure at least one full cycle has passed)
            if (ageTicks > 0 && ageTicks % ticksToNextStage == 0 && health > 30) {
                advanceGrowthStage();
            }

            // Wilting check
            if (health < 20 && growthStage != GrowthStage.DEAD) {
                if (growthStage != GrowthStage.WILTING) {
                    growthStage = GrowthStage.WILTING;
                    GardenLogger.getInstance().logWarning("PLANT",
                            String.format("%s [%s] is wilting! Health: %.1f%%", name, id, health));
                }
            }

        } catch (Exception e) {
            GardenLogger.getInstance().logError("PLANT",
                    "Error updating plant " + id, e);
        }
    }

    private void advanceGrowthStage() {
        GrowthStage previous = growthStage;
        switch (growthStage) {
            case SEED -> growthStage = GrowthStage.SPROUT;
            case SPROUT -> growthStage = GrowthStage.VEGETATIVE;
            case VEGETATIVE -> growthStage = GrowthStage.FLOWERING;
            case FLOWERING -> growthStage = GrowthStage.FRUITING;
            case FRUITING -> growthStage = GrowthStage.MATURE;
            default -> { return; }
        }
        GardenLogger.getInstance().log("PLANT",
                String.format("%s [%s] grew from %s to %s (health: %.1f%%)",
                        name, id, previous, growthStage, health));
    }

    /** Water this plant by the given amount. */
    public void water(double amount) {
        water(amount, false);
    }

    /** 
     * Water this plant. 
     * @param silent If true, suppresses log output (useful for automated systems).
     */
    public void water(double amount, boolean silent) {
        if (!alive) return;
        double before = waterLevel;
        waterLevel = Math.min(100, waterLevel + amount);
        
        if (!silent) {
            GardenLogger.getInstance().log("PLANT",
                    String.format("%s [%s] watered: %.1f -> %.1f", name, id, before, waterLevel));
        }
    }

    /** Fertilize this plant (logs the event). */
    public void fertilize(double amount) {
        if (!alive) return;
        double before = nutrientLevel;
        nutrientLevel = Math.min(100, nutrientLevel + amount);
        GardenLogger.getInstance().log("PLANT",
                String.format("%s [%s] fertilized: %.1f -> %.1f", name, id, before, nutrientLevel));
    }

    /** Add nutrients silently (used by fertigation during watering — avoids log spam). */
    public void addNutrients(double amount) {
        if (!alive) return;
        nutrientLevel = Math.min(100, nutrientLevel + amount);
    }

    /** Apply pest damage to this plant. */
    public void applyPestDamage(double damage) {
        if (!alive) return;
        double effectiveDamage = damage * (1.0 - pestResistance);
        health = Math.max(0, health - effectiveDamage);
        GardenLogger.getInstance().log("PLANT",
                String.format("%s [%s] took %.1f pest damage (resistance: %.0f%%). Health: %.1f%%",
                        name, id, effectiveDamage, pestResistance * 100, health));
        if (health <= 0) {
            die("Killed by pests");
        }
    }

    protected void die(String reason) {
        alive = false;
        growthStage = GrowthStage.DEAD;
        GardenLogger.getInstance().logWarning("PLANT",
                String.format("%s [%s] DIED. Reason: %s", name, id, reason));
    }

    // --- Getters ---
    public String getId() { return id; }
    public String getName() { return name; }
    public String getSpecies() { return species; }
    public Position getPosition() { return position; }
    public double getHealth() { return health; }
    public double getWaterLevel() { return waterLevel; }
    public double getNutrientLevel() { return nutrientLevel; }
    public GrowthStage getGrowthStage() { return growthStage; }
    public int getAgeTicks() { return ageTicks; }
    public boolean isAlive() { return alive; }
    public double getPestResistance() { return pestResistance; }

    /** Returns a display-friendly status summary. */
    public String getStatusSummary() {
        return String.format("%s (%s) | Stage: %s | HP: %.0f%% | Water: %.0f%% | Nutrients: %.0f%%",
                name, species, growthStage, health, waterLevel, nutrientLevel);
    }

    @Override
    public String toString() {
        return String.format("Plant[%s, %s, %s, hp=%.0f, stage=%s]",
                id, name, species, health, growthStage);
    }
}
