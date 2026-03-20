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

    private final String id;
    private final String name;
    private final String species;
    private Position position;

    private double health;
    private double waterLevel;
    private double nutrientLevel;
    private GrowthStage growthStage;
    private int ageTicks;
    private boolean alive;

    private double lightAccumulator;
    private int lightTickCounter;
    private double lastLightSatisfaction = 1.0;
    private static final int DAY_CYCLE_TICKS = 200;

    protected double idealTemperatureMin;
    protected double idealTemperatureMax;
    protected double temperatureSensitivity = 0.35;
    protected double waterNeedPerTick;
    protected double nutrientNeedPerTick;
    protected double lightNeedHours;
    protected int ticksToNextStage;
    protected double pestResistance;
    protected double waterTolerance;

    protected double droughtThreshold   = 10.0;
    protected double overwaterThreshold = 90.0;
    protected double idealWaterMin      = 40.0;
    protected double idealWaterMax      = 70.0;
    protected double drainageRate       = 0.8;

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
        this.waterTolerance = 0.5; // Default tolerance

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

            waterLevel = Math.max(0, waterLevel - waterNeedPerTick);
            if (waterLevel > idealWaterMax) {
                waterLevel = Math.max(idealWaterMax, waterLevel - drainageRate);
            }
            nutrientLevel = Math.max(0, nutrientLevel - nutrientNeedPerTick);

            double healthDelta = 0;

            if (waterLevel < droughtThreshold) {
                healthDelta -= 2.5;
                if (waterLevel <= 0) {
                    GardenLogger.getInstance().logWarning("PLANT",
                            String.format("%s [%s] is critically dehydrated!", name, id));
                }
            } else if (waterLevel > overwaterThreshold) {
                double overwaterPenalty = 3.0 * (1.0 - waterTolerance);
                healthDelta -= Math.max(0.1, overwaterPenalty);
            } else if (waterLevel > idealWaterMin && waterLevel < idealWaterMax) {
                healthDelta += 1.2;
            } else {
                healthDelta += 0.5;
            }

            if (currentTemperature < idealTemperatureMin || currentTemperature > idealTemperatureMax) {
                double tempStress = 0;
                if (currentTemperature < idealTemperatureMin) {
                    tempStress = (idealTemperatureMin - currentTemperature) * temperatureSensitivity;
                } else {
                    tempStress = (currentTemperature - idealTemperatureMax) * temperatureSensitivity;
                }
                healthDelta -= tempStress;
            } else {
                healthDelta += 0.6;
            }

            if (nutrientLevel < 10) {
                healthDelta -= 1.5;
            } else if (nutrientLevel > 85) {
                healthDelta -= (nutrientLevel - 85) * 0.04;
            } else if (nutrientLevel > 40) {
                healthDelta += 0.5;
            }

            lightAccumulator += currentLight;
            lightTickCounter++;

            if (lightTickCounter >= DAY_CYCLE_TICKS) {
                double avgLight = lightAccumulator / DAY_CYCLE_TICKS;
                double lightHoursReceived = (avgLight / 100.0) * 24.0;
                lastLightSatisfaction = Math.min(2.0, lightHoursReceived / lightNeedHours);

                if (lastLightSatisfaction < 0.5) {
                    GardenLogger.getInstance().logWarning("PLANT",
                            String.format("%s [%s] severe light deficit: received %.1fh, needs %.0fh",
                                    name, id, lightHoursReceived, lightNeedHours));
                }
                lightAccumulator = 0;
                lightTickCounter = 0;
            }

            if (lastLightSatisfaction < 0.7) {
                healthDelta -= (0.7 - lastLightSatisfaction) * 1.5;
            } else if (lastLightSatisfaction >= 0.9) {
                healthDelta += 0.2;
            }

            if (currentHumidity > 85) {
                double fungalRisk = (currentHumidity - 85) / 15.0;
                healthDelta -= fungalRisk * 0.8;
                if (currentHumidity > 90 && ageTicks % 100 == 0) {
                    GardenLogger.getInstance().logWarning("PLANT",
                            String.format("%s [%s] at risk of fungal disease (humidity: %.0f%%)",
                                    name, id, currentHumidity));
                }
            } else if (currentHumidity < 30) {
                double dryStress = (30 - currentHumidity) / 30.0;
                healthDelta -= dryStress * 0.6;
            } else if (currentHumidity >= 40 && currentHumidity <= 70) {
                healthDelta += 0.15;
            }

            health = Math.max(0, Math.min(100, health + healthDelta));
            if (health <= 0) {
                die("Health reached zero");
                return;
            }

            if (ageTicks > 0 && ageTicks % ticksToNextStage == 0 && health > 30) {
                advanceGrowthStage();
            }

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

    public void setPosition(Position pos) { this.position = pos; }
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
    public double getWaterNeedPerTick() { return waterNeedPerTick; }
    public double getIdealWaterMax() { return idealWaterMax; }

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
