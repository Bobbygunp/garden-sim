package garden.core;

import garden.logging.GardenLogger;
import javafx.animation.AnimationTimer;
import javafx.beans.property.*;

/**
 * The SimulationEngine drives the garden simulation forward.
 * It uses a JavaFX AnimationTimer to tick the garden at a configurable speed.
 */
public class SimulationEngine {

    private final Garden garden;
    private AnimationTimer timer;

    // Simulation state
    private final BooleanProperty running = new SimpleBooleanProperty(false);
    private final BooleanProperty paused = new SimpleBooleanProperty(false);
    private final IntegerProperty tickCount = new SimpleIntegerProperty(0);
    private final DoubleProperty simulationSpeed = new SimpleDoubleProperty(1.0);

    // Timing
    private long lastTickTime = 0;
    private long nextTickExpectedTime = 0;
    private static final long BASE_TICK_INTERVAL_NS = 500_000_000L; // 0.5 seconds base

    // Callbacks for UI updates
    private Runnable onTickCallback;

    public SimulationEngine(Garden garden) {
        this.garden = garden;

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (paused.get()) return;

                long interval = (long) (BASE_TICK_INTERVAL_NS / simulationSpeed.get());
                if (now >= nextTickExpectedTime) {
                    lastTickTime = now;
                    nextTickExpectedTime = now + interval;
                    performTick();
                }
            }
        };

        GardenLogger.getInstance().log("APPLICATION", "Simulation Engine initialized.");
    }

    /** 
     * Returns the progress towards the next tick (0.0 to 1.0).
     * Used for smooth UI interpolation.
     */
    public double getTickProgress() {
        if (!running.get() || paused.get()) return 0;
        long now = System.nanoTime();
        long interval = nextTickExpectedTime - lastTickTime;
        if (interval <= 0) return 1.0;
        double progress = (double)(now - lastTickTime) / interval;
        return Math.min(1.0, Math.max(0.0, progress));
    }

    private void performTick() {
        try {
            garden.tick();
            tickCount.set(garden.getCurrentTick());

            if (onTickCallback != null) {
                onTickCallback.run();
            }
        } catch (Exception e) {
            // CRITICAL: The simulation must never crash
            GardenLogger.getInstance().logError("APPLICATION",
                    "Exception during simulation tick - recovering", e);
        }
    }

    /** Start the simulation. */
    public void start() {
        if (!running.get()) {
            running.set(true);
            paused.set(false);
            lastTickTime = System.nanoTime();
            timer.start();
            GardenLogger.getInstance().log("APPLICATION", "Simulation STARTED.");
        }
    }

    /** Stop the simulation completely. */
    public void stop() {
        running.set(false);
        timer.stop();
        GardenLogger.getInstance().log("APPLICATION", "Simulation STOPPED.");
    }

    /** Pause/resume the simulation. */
    public void togglePause() {
        paused.set(!paused.get());
        GardenLogger.getInstance().log("APPLICATION",
                "Simulation " + (paused.get() ? "PAUSED" : "RESUMED"));
    }

    /** Set callback that fires every tick (for UI updates). */
    public void setOnTickCallback(Runnable callback) {
        this.onTickCallback = callback;
    }

    // --- Properties for JavaFX binding ---
    public BooleanProperty runningProperty() { return running; }
    public BooleanProperty pausedProperty() { return paused; }
    public IntegerProperty tickCountProperty() { return tickCount; }
    public DoubleProperty simulationSpeedProperty() { return simulationSpeed; }

    public boolean isRunning() { return running.get(); }
    public boolean isPaused() { return paused.get(); }

    public void setSimulationSpeed(double speed) {
        simulationSpeed.set(Math.max(0.1, Math.min(10.0, speed)));
        GardenLogger.getInstance().log("APPLICATION",
                String.format("Simulation speed set to %.1fx", simulationSpeed.get()));
    }
}
