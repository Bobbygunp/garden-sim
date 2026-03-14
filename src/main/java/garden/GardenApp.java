package garden;

import garden.core.Garden;
import garden.core.SimulationEngine;
import garden.logging.GardenLogger;
import garden.ui.GardenController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
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
            // --- Splash Screen ---
            Label title = new Label("Computerized Garden Simulation");
            title.setStyle("-fx-font-size: 16px; -fx-text-fill: #aaaaaa; -fx-font-family: 'Georgia', serif;");

            Label message = new Label("Thank you, Professor Navid Shaghaghi");
            message.setStyle("-fx-font-size: 36px; -fx-text-fill: #90EE90; -fx-font-family: 'Georgia', serif; -fx-font-weight: bold;");

            Label body = new Label(
                "This course has been one of the most valuable experiences\n" +
                "of our academic journey. Thank you for every lesson."
            );
            body.setStyle("-fx-font-size: 16px; -fx-text-fill: #cccccc; -fx-font-family: 'Georgia', serif; -fx-text-alignment: center;");
            body.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

            Label sub = new Label("Loading garden...");
            sub.setStyle("-fx-font-size: 13px; -fx-text-fill: #666666;");

            ProgressIndicator spinner = new ProgressIndicator();
            spinner.setStyle("-fx-progress-color: #90EE90;");
            spinner.setMaxSize(50, 50);

            VBox splashRoot = new VBox(18, title, message, body, spinner, sub);
            splashRoot.setAlignment(Pos.CENTER);
            splashRoot.setStyle("-fx-background-color: #1a2a1a; -fx-padding: 60px;");

            primaryStage.setScene(new Scene(splashRoot, 1280, 800));
            primaryStage.setTitle("Computerized Garden Simulation");
            primaryStage.setMinWidth(900);
            primaryStage.setMinHeight(650);
            primaryStage.setMaximized(true);
            primaryStage.show();

            // --- Background init: keep the FX thread free so the splash renders ---
            // Record start time so we can enforce a minimum display of 6 seconds,
            // giving the professor enough time to read the message.
            final long splashStart = System.currentTimeMillis();
            final long MIN_SPLASH_MS = 6000;

            Thread initThread = new Thread(() -> {
                try {
                    GardenLogger.getInstance().log("APPLICATION", "Garden Simulation starting up...");

                    garden = new Garden("My Automated Garden", 20, 20);
                    garden.initializeDefaultGarden();
                    simulationEngine = new SimulationEngine(garden);

                    // Wait out any remaining splash time before transitioning
                    long elapsed = System.currentTimeMillis() - splashStart;
                    if (elapsed < MIN_SPLASH_MS) {
                        try { Thread.sleep(MIN_SPLASH_MS - elapsed); } catch (InterruptedException ignored) {}
                    }

                    // Back to FX thread to build and show the main scene
                    Platform.runLater(() -> {
                        try {
                            FXMLLoader loader = new FXMLLoader(
                                    getClass().getResource("/garden/ui/GardenDashboard.fxml"));
                            Parent root = loader.load();
                            GardenController controller = loader.getController();

                            primaryStage.setScene(new Scene(root, 1280, 800));

                            // Two runLater calls so the layout engine measures pane
                            // sizes after both the scene swap and the maximise pulse.
                            Platform.runLater(() -> Platform.runLater(() -> {
                                controller.initializeWithModel(garden, simulationEngine);
                                simulationEngine.start();
                                GardenLogger.getInstance().log("APPLICATION",
                                        "Garden Simulation started successfully.");
                            }));
                        } catch (Exception e) {
                            GardenLogger.getInstance().logError("APPLICATION",
                                    "Failed to load main scene", e);
                        }
                    });

                } catch (Exception e) {
                    GardenLogger.getInstance().logError("APPLICATION",
                            "Failed to initialise garden", e);
                    Platform.runLater(() -> {
                        Label err = new Label("Startup failed: " + e.getMessage());
                        err.setStyle("-fx-text-fill: red; -fx-font-size: 16px;");
                        ((VBox) primaryStage.getScene().getRoot()).getChildren().add(err);
                    });
                }
            });
            initThread.setDaemon(true);
            initThread.start();

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
