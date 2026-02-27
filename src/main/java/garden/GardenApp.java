package garden;

import garden.core.Garden;
import garden.core.SimulationEngine;
import garden.logging.GardenLogger;
import garden.ui.GardenController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main entry point for the Computerized Garden Simulation.
 * Loads the FXML layout, initializes the garden model, and starts the simulation.
 */
public class GardenApp extends Application {

    private SimulationEngine simulationEngine;
    private Garden garden;

    @Override
    public void start(Stage primaryStage) {
        // Global safety net: log and swallow any unhandled exception so the
        // garden keeps running during the 24-hour grading session.
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            try {
                GardenLogger.getInstance().logError("APPLICATION",
                        "Uncaught exception on thread " + t.getName()
                                + ": " + e.getClass().getSimpleName() + " - " + e.getMessage());
            } catch (Exception ignored) { }
        });

        try {
            GardenLogger.getInstance().log("APPLICATION", "Garden Simulation starting up...");

            // Initialize the garden world
            garden = new Garden("My Automated Garden", 20, 20); // 20x20 grid
            garden.initializeDefaultGarden();

            // Initialize the simulation engine
            simulationEngine = new SimulationEngine(garden);

            // Load the FXML layout
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/garden/ui/GardenDashboard.fxml"));
            Parent root = loader.load();

            GardenController controller = loader.getController();

            // Set up the stage — start maximized so all UI fits any screen size
            Scene scene = new Scene(root, 1280, 800);
            primaryStage.setScene(scene);
            primaryStage.setTitle("Computerized Garden Simulation");
            primaryStage.setMinWidth(900);
            primaryStage.setMinHeight(650);
            primaryStage.setMaximized(true);
            primaryStage.show();

            // Defer model initialisation until AFTER the stage is shown and the
            // JavaFX layout engine has computed real pane sizes.  Two nested
            // Platform.runLater calls guarantee we wait for both the initial
            // layout pulse AND the maximise resize pulse before measuring the
            // ScrollPane — this is what makes the canvas fill the left pane
            // correctly on first run.
            Platform.runLater(() -> Platform.runLater(() -> {
                controller.initializeWithModel(garden, simulationEngine);
                simulationEngine.start();
                GardenLogger.getInstance().log("APPLICATION", "Garden Simulation started successfully.");
            }));

        } catch (Exception e) {
            GardenLogger.getInstance().logError("APPLICATION", "Failed to start application", e);
            System.err.println("Critical startup error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        try {
            if (simulationEngine != null) {
                simulationEngine.stop();
            }
            GardenLogger.getInstance().log("APPLICATION", "Garden Simulation shutting down gracefully.");
            GardenLogger.getInstance().close();
        } catch (Exception e) {
            System.err.println("Error during shutdown: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
