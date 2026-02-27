package garden.ui;

import garden.core.Garden;
import garden.core.SimulationEngine;
import garden.logging.GardenLogger;
import garden.model.insects.Insect;
import garden.model.plants.Plant;
import garden.model.sensors.Sensor;
import garden.modules.*;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.image.Image;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * FXML Controller for the Garden Dashboard.
 * Handles all UI events and updates the view each simulation tick.
 */
public class GardenController implements Initializable {

    // --- FXML-injected UI components ---
    @FXML private Button pauseButton;
    @FXML private Slider speedSlider;
    @FXML private Label tickLabel;
    @FXML private Label dayLabel;
    @FXML private Canvas gardenCanvas;
    @FXML private StackPane canvasContainer;
    @FXML private ScrollPane canvasScrollPane;
    @FXML private Button zoomInButton;
    @FXML private Button zoomOutButton;
    @FXML private Label zoomLabel;

    // Environment panel labels & progress bars
    @FXML private Label tempLabel;
    @FXML private Label lightLabel;
    @FXML private Label humidityLabel;
    @FXML private Label plantsAliveLabel;
    @FXML private Label insectsLabel;
    @FXML private Label sprinklerLabel;
    @FXML private Label dayPhaseLabel;
    @FXML private ProgressBar tempProgressBar;
    @FXML private ProgressBar lightProgressBar;
    @FXML private ProgressBar humidityProgressBar;

    // Tab content VBoxes
    @FXML private VBox moduleStatusList;
    @FXML private VBox plantStatusList;
    @FXML private VBox insectStatusList;

    // Log viewer
    @FXML private ComboBox<String> logFilterCombo;
    @FXML private TextArea logArea;

    // --- Model references ---
    private Garden garden;
    private SimulationEngine engine;
    private ImageManager imageManager;

    /**
     * Cell width and height in pixels — computed separately so the canvas
     * fills the full left pane regardless of its aspect ratio.
     * cellW = viewportWidth  / cols
     * cellH = viewportHeight / rows
     * At zoom 1.0 the canvas covers the entire viewport with no black gaps.
     */
    private double cellW = 38.0;
    private double cellH = 38.0;

    /** Zoom multiplier: 1.0 = fill pane exactly, >1.0 = scrollable zoom-in. */
    private double zoomLevel = 1.0;

    /** Simulated ticks per day (must match Garden.dayNightCycle). */
    private static final int DAY_NIGHT_CYCLE = 200;

    private AnimationTimer renderTimer;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logFilterCombo.getItems().addAll(
                "ALL", "PLANT", "INSECT", "WATERING", "HEATING",
                "PEST_CONTROL", "LIGHTING", "SENSOR", "GARDEN",
                "USER_ACTION", "APPLICATION", "ERRORS ONLY"
        );
        logFilterCombo.setValue("ALL");
    }

    /**
     * Called by GardenApp after FXML loading to inject the model and start updates.
     */
    public void initializeWithModel(Garden garden, SimulationEngine engine) {
        this.garden = garden;
        this.engine = engine;

        imageManager = new ImageManager((int) Math.round(Math.min(cellW, cellH)));
        gardenCanvas.setWidth(garden.getCols() * cellW);
        gardenCanvas.setHeight(garden.getRows() * cellH);

        // Recompute canvas size whenever the viewport changes (divider drag / window resize).
        // viewportBoundsProperty fires on every layout pass and reports the true visible area.
        canvasScrollPane.viewportBoundsProperty().addListener((obs, o, n) -> updateCellSize());
        Platform.runLater(this::updateCellSize);

        // Bind speed slider to simulation engine
        speedSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                engine.setSimulationSpeed(newVal.doubleValue()));

        // Register tick callback for logic-based UI refresh (text labels, tabs)
        engine.setOnTickCallback(() -> Platform.runLater(this::updateLabelsAndTabs));

        // Dedicated 60fps loop for smooth canvas rendering and interpolation
        renderTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                renderGarden(engine.getTickProgress());
            }
        };
        renderTimer.start();

        updateLabelsAndTabs();
    }

    /**
     * Recalculates cellW/cellH from the viewport bounds and zoom level.
     *
     * cellW = vpWidth  / cols * zoom  → canvas width  == vpWidth  * zoom
     * cellH = vpHeight / rows * zoom  → canvas height == vpHeight * zoom
     *
     * At zoom 1.0 the canvas covers the viewport exactly (no black borders).
     * At zoom > 1.0 the canvas is larger than the viewport and scrollbars appear.
     */
    private void updateCellSize() {
        if (garden == null) return;

        double vpW = canvasScrollPane.getViewportBounds().getWidth();
        double vpH = canvasScrollPane.getViewportBounds().getHeight();
        if (vpW <= 10 || vpH <= 10) return;

        double newCellW = Math.max(10.0, Math.min((vpW / garden.getCols()) * zoomLevel, 150.0));
        double newCellH = Math.max(10.0, Math.min((vpH / garden.getRows()) * zoomLevel, 150.0));

        if (Math.abs(newCellW - cellW) > 0.3 || Math.abs(newCellH - cellH) > 0.3) {
            cellW = newCellW;
            cellH = newCellH;
            gardenCanvas.setWidth(garden.getCols() * cellW);
            gardenCanvas.setHeight(garden.getRows() * cellH);
            imageManager = new ImageManager((int) Math.round(Math.min(cellW, cellH)));
            renderGarden(0.0);
        }
    }

    // =========================================================================
    // FXML EVENT HANDLERS
    // =========================================================================

    @FXML
    private void handlePause() {
        engine.togglePause();
        pauseButton.setText(engine.isPaused() ? "Resume" : "Pause");
    }

    @FXML
    private void handleManualWater() {
        if (garden.getWateringSystem() != null) {
            garden.getWateringSystem().manualWater(garden);
        }
    }

    @FXML
    private void handlePestControl() {
        if (garden.getPestControl() != null) {
            garden.getPestControl().manualPestControl(garden);
        }
    }

    @FXML
    private void handleFertilize() {
        GardenLogger.getInstance().log("USER_ACTION", "Manual fertilize all plants triggered.");
        for (Plant p : garden.getPlants()) {
            p.fertilize(20);
        }
    }

    @FXML
    private void handleLogFilter() {
        refreshLog();
    }

    @FXML
    private void handleZoomIn() {
        zoomLevel = Math.min(zoomLevel + 0.5, 4.0);
        zoomLabel.setText(zoomLevel == 1.0 ? "Fit" : String.format("%.1f\u00d7", zoomLevel));
        updateCellSize();
    }

    @FXML
    private void handleZoomOut() {
        zoomLevel = Math.max(zoomLevel - 0.5, 1.0);
        zoomLabel.setText(zoomLevel == 1.0 ? "Fit" : String.format("%.1f\u00d7", zoomLevel));
        updateCellSize();
    }

    // =========================================================================
    // UI UPDATE METHODS
    // =========================================================================

    private long lastTabUpdateTime = 0;

    private void updateLabelsAndTabs() {
        try {
            int tick = garden.getCurrentTick();
            int day = tick / DAY_NIGHT_CYCLE + 1;
            double dayProgress = (tick % DAY_NIGHT_CYCLE) / (double) DAY_NIGHT_CYCLE;
            String phase = dayProgress < 0.5 ? "Daytime" : "Nighttime";

            // --- Toolbar labels ---
            tickLabel.setText(String.format("Tick: %d", tick));
            dayLabel.setText(String.format("Day %d  -  %s", day, phase));

            // --- Environment cards ---
            double temp = garden.getCurrentTemperature();
            tempLabel.setText(String.format("%.1f F", temp));
            tempProgressBar.setProgress(Math.max(0, Math.min(1.0, (temp - 40.0) / 55.0)));

            double light = garden.getCurrentLightLevel();
            lightLabel.setText(String.format("%.0f / 100", light));
            lightProgressBar.setProgress(light / 100.0);

            double humidity = garden.getCurrentHumidity();
            humidityLabel.setText(String.format("%.0f%%", humidity));
            humidityProgressBar.setProgress(humidity / 100.0);

            // --- Garden stats ---
            long alivePlants = garden.getPlants().stream().filter(Plant::isAlive).count();
            plantsAliveLabel.setText(String.format("Plants alive:   %d / %d", alivePlants, garden.getPlants().size()));

            long aliveInsects = garden.getInsects().stream().filter(Insect::isAlive).count();
            long pests = garden.getInsects().stream()
                    .filter(Insect::isAlive)
                    .filter(i -> i.getType() == Insect.InsectType.PEST).count();
            insectsLabel.setText(String.format("Insects:   %d total  (%d pests)", aliveInsects, pests));

            if (garden.getWateringSystem() != null) {
                long activeSpr = garden.getWateringSystem().getSprinklers().stream()
                        .filter(WateringSystem.Sprinkler::isActive).count();
                int totalSpr = garden.getWateringSystem().getSprinklers().size();
                sprinklerLabel.setText(String.format("Sprinklers:   %d / %d active", activeSpr, totalSpr));
            }
            dayPhaseLabel.setText(String.format("Phase:   %s  (day %d)", phase, day));

            // --- Throttled Tab Updates ---
            long now = System.currentTimeMillis();
            if (now - lastTabUpdateTime >= 500) { // Only update tabs twice per second to prevent lag
                lastTabUpdateTime = now;
                updateTabs();
            }

            // --- Log (every 5 ticks to reduce load) ---
            if (tick % 5 == 0) {
                refreshLog();
            }

        } catch (Exception e) {
            GardenLogger.getInstance().logError("APPLICATION", "UI update error", e);
        }
    }

    private void updateTabs() {
        // --- Module tab ---
        moduleStatusList.getChildren().clear();
        for (GardenModule mod : garden.getModules()) {
            moduleStatusList.getChildren().add(createModuleCard(mod));
        }

        // --- Plants tab ---
        plantStatusList.getChildren().clear();
        for (Plant plant : garden.getPlants()) {
            plantStatusList.getChildren().add(createPlantCard(plant));
        }

        // --- Insects tab ---
        insectStatusList.getChildren().clear();
        long aliveCount = garden.getInsects().stream().filter(Insect::isAlive).count();
        if (aliveCount == 0) {
            Label empty = new Label("No insects currently in the garden.");
            empty.setTextFill(Color.LIGHTGRAY);
            empty.setFont(Font.font("System", 11));
            insectStatusList.getChildren().add(empty);
        } else {
            for (Insect insect : garden.getInsects()) {
                if (insect.isAlive()) {
                    insectStatusList.getChildren().add(createInsectCard(insect));
                }
            }
        }
    }

    // =========================================================================
    // CARD BUILDERS
    // =========================================================================

    private VBox createModuleCard(GardenModule mod) {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");

        HBox titleRow = new HBox(10);
        titleRow.setStyle("-fx-alignment: CENTER_LEFT;");
        Label nameLabel = new Label(mod.getModuleName());
        nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label badge = new Label(mod.isEnabled() ? "ONLINE" : "OFFLINE");
        badge.setFont(Font.font("System", FontWeight.BOLD, 10));
        badge.setTextFill(mod.isEnabled() ? Color.WHITE : Color.LIGHTGRAY);
        badge.setStyle((mod.isEnabled()
                ? "-fx-background-color: #238636;"
                : "-fx-background-color: #21262d;")
                + " -fx-padding: 3 8; -fx-background-radius: 12;");

        titleRow.getChildren().addAll(nameLabel, badge);

        Label summary = new Label(mod.getStatusSummary());
        summary.setWrapText(true);
        summary.setStyle("-fx-font-family: Monospaced; -fx-font-size: 11px; -fx-text-fill: #8b949e;");

        card.getChildren().addAll(titleRow, summary);
        return card;
    }

    /**
     * Returns a species-specific accent colour used for the card left-border
     * and name tinting.  Gives each species a unique visual identity.
     */
    private String getSpeciesColor(String species) {
        String s = species.toLowerCase();
        if (s.contains("tomato"))    return "#e74c3c"; // red
        if (s.contains("rose"))      return "#e91e8c"; // pink
        if (s.contains("sunflower")) return "#f1c40f"; // gold
        if (s.contains("carrot"))    return "#e67e22"; // orange
        if (s.contains("lettuce"))   return "#2ecc71"; // green
        if (s.contains("cactus"))    return "#1abc9c"; // teal
        return "#8b949e";                              // default grey
    }

    /** Maps a growth stage to a short, human-friendly label + colour. */
    private String[] getStageBadge(Plant.GrowthStage stage) {
        return switch (stage) {
            case SEED       -> new String[]{"SEED",      "#6e7681"};
            case SPROUT     -> new String[]{"SPROUT",    "#7ee787"};
            case VEGETATIVE -> new String[]{"GROWING",   "#3fb950"};
            case FLOWERING  -> new String[]{"FLOWERING", "#d2a8ff"};
            case FRUITING   -> new String[]{"FRUITING",  "#f0883e"};
            case MATURE     -> new String[]{"MATURE",    "#e3b341"};
            case WILTING    -> new String[]{"WILTING",    "#f85149"};
            case DEAD       -> new String[]{"DEAD",      "#484f58"};
        };
    }

    private VBox createPlantCard(Plant plant) {
        boolean alive = plant.isAlive();
        String speciesColor = getSpeciesColor(plant.getName());
        String[] stageBadge = getStageBadge(plant.getGrowthStage());

        // Outer card with species-coloured left accent border
        VBox card = new VBox(8);
        card.getStyleClass().add("card");
        String borderStyle = alive
                ? String.format("-fx-border-color: %s transparent transparent transparent; "
                        + "-fx-border-width: 3 0 0 0; -fx-border-radius: 8 8 0 0;", speciesColor)
                : "-fx-opacity: 0.45;";
        card.setStyle(card.getStyle() + borderStyle);

        // --- Row 1: Name + Stage badge ---
        HBox titleRow = new HBox(8);
        titleRow.setStyle("-fx-alignment: CENTER_LEFT;");

        Label nameLabel = new Label(plant.getName());
        nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        nameLabel.setTextFill(alive ? Color.web(speciesColor) : Color.DARKGRAY);

        Label speciesLabel = new Label(plant.getSpecies());
        speciesLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #6e7681; -fx-font-style: italic;");

        // Stage badge (coloured pill)
        Label stageLbl = new Label(stageBadge[0]);
        stageLbl.setFont(Font.font("System", FontWeight.BOLD, 9));
        stageLbl.setTextFill(Color.WHITE);
        stageLbl.setStyle(String.format(
                "-fx-background-color: %s; -fx-padding: 2 8; -fx-background-radius: 10;", stageBadge[1]));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        titleRow.getChildren().addAll(nameLabel, speciesLabel, spacer, stageLbl);

        card.getChildren().add(titleRow);

        // --- Dead plants: just the skull row ---
        if (!alive) {
            HBox deadRow = new HBox(6);
            deadRow.setStyle("-fx-alignment: CENTER;");
            Label deadLabel = new Label("DEAD");
            deadLabel.setStyle("-fx-text-fill: #f85149; -fx-font-weight: 900; -fx-font-size: 13px;");
            deadRow.getChildren().add(deadLabel);
            card.getChildren().add(deadRow);
            return card;
        }

        // --- Row 2: Position + age ---
        Label posAge = new Label(String.format("Pos %s  |  Age: %d ticks",
                plant.getPosition(), plant.getAgeTicks()));
        posAge.setStyle("-fx-font-family: Monospaced; -fx-font-size: 10px; -fx-text-fill: #6e7681;");
        card.getChildren().add(posAge);

        // --- Row 3-5: Stat bars ---
        card.getChildren().add(buildStatBar("HP",
                plant.getHealth(), "#3fb950", "#f0883e", "#f85149", "#2ea043"));
        card.getChildren().add(buildStatBar("WATER",
                plant.getWaterLevel(), "#58a6ff", "#58a6ff", "#1f6feb", "#1971c2"));
        card.getChildren().add(buildStatBar("NUTRI",
                plant.getNutrientLevel(), "#d2a8ff", "#bc8cff", "#8957e5", "#6e40c9"));

        return card;
    }

    /**
     * Builds a richly styled stat bar row with a label, coloured track,
     * percentage text, and glow effect when the value is critically low.
     */
    private HBox buildStatBar(String label, double value,
                              String highColor, String midColor, String lowColor,
                              String trackBg) {
        HBox row = new HBox(6);
        row.setStyle("-fx-alignment: CENTER_LEFT;");

        // Stat label with fixed width
        Label lbl = new Label(label);
        lbl.setPrefWidth(42);
        lbl.setStyle("-fx-font-family: Monospaced; -fx-font-size: 10px; -fx-font-weight: bold; "
                + "-fx-text-fill: #8b949e;");

        // Progress bar
        ProgressBar bar = new ProgressBar(value / 100.0);
        bar.setPrefHeight(10);
        bar.setMinHeight(10);
        bar.setMaxHeight(10);
        HBox.setHgrow(bar, Priority.ALWAYS);

        String accent;
        if (value > 60)       accent = highColor;
        else if (value > 30)  accent = midColor;
        else                  accent = lowColor;

        bar.setStyle(String.format(
                "-fx-accent: %s; -fx-control-inner-background: %s; "
              + "-fx-background-color: transparent; -fx-background-radius: 4; ",
                accent, trackBg + "33")); // 33 = 20% alpha hex suffix for track

        // Percentage with colour that matches the bar
        Label pct = new Label(String.format("%.0f%%", value));
        pct.setPrefWidth(38);
        pct.setStyle(String.format(
                "-fx-font-family: Monospaced; -fx-font-size: 11px; -fx-font-weight: bold; "
              + "-fx-text-fill: %s;", accent));

        // Critical glow: if value < 15%, make the row pulse visually
        if (value < 15) {
            row.setStyle(row.getStyle()
                    + String.format("-fx-background-color: %s22; -fx-background-radius: 4;", lowColor));
        }

        row.getChildren().addAll(lbl, bar, pct);
        return row;
    }

    private VBox createInsectCard(Insect insect) {
        VBox card = new VBox(6);
        card.getStyleClass().add("card");

        Color typeColor = switch (insect.getType()) {
            case PEST -> Color.web("#ff7b72");
            case POLLINATOR -> Color.web("#e3b341");
            case BENEFICIAL -> Color.web("#3fb950");
            case NEUTRAL -> Color.web("#d2a8ff");
        };

        String typeBadge = insect.getType().name();

        HBox titleRow = new HBox(10);
        titleRow.setStyle("-fx-alignment: CENTER_LEFT;");
        Label nameLabel = new Label(insect.getName());
        nameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
        nameLabel.setTextFill(typeColor);

        Label typeLbl = new Label(typeBadge);
        typeLbl.setFont(Font.font("System", FontWeight.BOLD, 9));
        typeLbl.setTextFill(Color.WHITE);
        
        String badgeBg = switch(insect.getType()) {
            case PEST -> "#da3633";
            case POLLINATOR -> "#bf8700";
            case BENEFICIAL -> "#238636";
            case NEUTRAL -> "#8957e5";
        };
        typeLbl.setStyle("-fx-background-color: " + badgeBg + "; -fx-padding: 2 6; -fx-background-radius: 10;");
        
        titleRow.getChildren().addAll(nameLabel, typeLbl);

        Label detailLabel = new Label(String.format("Position: %s  |  Age: %d ticks", insect.getPosition(), insect.getAgeTicks()));
        detailLabel.setStyle("-fx-font-family: Monospaced; -fx-font-size: 11px; -fx-text-fill: #8b949e;");

        card.getChildren().addAll(titleRow, detailLabel);
        return card;
    }

    // =========================================================================
    // GARDEN CANVAS RENDERING
    // =========================================================================

    private void renderGarden(double progress) {
        GraphicsContext gc = gardenCanvas.getGraphicsContext2D();
        double w = gardenCanvas.getWidth();
        double h = gardenCanvas.getHeight();

        // ---- Clear background ----
        gc.setFill(Color.web("#1a1a1a"));
        gc.fillRect(0, 0, w, h);

        // ---- PvZ Styled Lawn (Textured grass) ----
        for (int r = 0; r < garden.getRows(); r++) {
            for (int c = 0; c < garden.getCols(); c++) {
                boolean isAlt = (r + c) % 2 == 0;
                Color grassColor = isAlt ? Color.web("#4c9900") : Color.web("#5cb300");
                gc.setFill(grassColor);
                gc.fillRect(c * cellW, r * cellH, cellW, cellH);

                // Add grass "texture" (small darker blades)
                gc.setFill(grassColor.darker());
                gc.setGlobalAlpha(0.2);
                for(int i=0; i<3; i++) {
                    double gx = c * cellW + (i * cellW/3.0) + (r%2 * 5);
                    double gy = r * cellH + (i * cellH/4.0);
                    gc.fillRect(gx + 5, gy + 5, 2, 4);
                }
                gc.setGlobalAlpha(1.0);

                // Tile highlight (Sun-kissed edge)
                gc.setStroke(Color.rgb(255, 255, 255, 0.1));
                gc.setLineWidth(1);
                gc.strokeRect(c * cellW + 1, r * cellH + 1, cellW - 2, cellH - 2);
            }
        }

        // ---- Soil patches beneath plants ----
        for (Plant plant : garden.getPlants()) {
            double px = plant.getPosition().getCol() * cellW;
            double py = plant.getPosition().getRow() * cellH;
            gc.setFill(Color.rgb(45, 30, 21, 0.4));
            gc.fillOval(px + 4, py + cellH - 12, cellW - 8, 8);
        }

        // ---- Grid lines ----
        gc.setStroke(Color.rgb(63, 185, 80, 0.15));
        gc.setLineWidth(1.0);
        for (int r = 0; r <= garden.getRows(); r++) {
            gc.strokeLine(0, r * cellH, w, r * cellH);
        }
        for (int c = 0; c <= garden.getCols(); c++) {
            gc.strokeLine(c * cellW, 0, c * cellW, h);
        }

        // ---- Draw layers ----
        drawSprinklers(gc, progress);
        drawSensors(gc);
        drawPlants(gc, progress);
        drawInsects(gc, progress);

        // ---- NEW: SYSTEM VISUAL EFFECTS ----
        drawSystemEffects(gc);

        // ---- Day/Night overlay (PvZ Night Style - Intense) ----
        double lightLevel = garden.getCurrentLightLevel();
        if (lightLevel < 65) { 
            // Calculate darkness intensity: reaches up to 85% opacity at lowest light
            double darkness = (65 - lightLevel) / 65.0 * 0.85; 
            
            // Rich midnight blue/black mix for that PvZ feel
            gc.setFill(Color.rgb(4, 2, 24, darkness)); 
            gc.fillRect(0, 0, w, h);
            
            // Add a subtle "vignette" effect during the deepest part of night
            if (darkness > 0.6) {
                double vignette = (darkness - 0.6) / 0.25 * 0.2;
                gc.setStroke(Color.rgb(0, 0, 0, vignette));
                gc.setLineWidth(100);
                gc.strokeRect(0, 0, w, h);
            }
        }

        // ---- Garden border (Wooden frame) ----
        gc.setStroke(Color.web("#4d3319"));
        gc.setLineWidth(8);
        gc.strokeRect(4, 4, w - 8, h - 8);
        gc.setStroke(Color.web("#734d26"));
        gc.setLineWidth(2);
        gc.strokeRect(2, 2, w - 4, h - 4);
    }

    private void drawSystemEffects(GraphicsContext gc) {
        double w = gardenCanvas.getWidth();
        double h = gardenCanvas.getHeight();
        long time = System.currentTimeMillis();
        int currentTick = garden.getCurrentTick();

        // 1. HEATING / COOLING GLOW
        HeatingSystem heating = garden.getHeatingSystem();
        if (heating != null && heating.isEnabled()) {
            double adj = heating.getCurrentAdjustment();
            if (adj != 0) {
                // Pulse intensity
                double pulse = 0.1 + 0.05 * Math.sin(time * 0.003);
                gc.setFill(adj > 0 ? Color.rgb(255, 69, 0, pulse) : Color.rgb(0, 191, 255, pulse));
                gc.fillRect(0, 0, w, h);
            }
        }

        // 2. LIGHTING SYSTEM (GOLDEN RAYS)
        LightingSystem lighting = garden.getLightingSystem();
        if (lighting != null && lighting.isLightsOn()) {
            gc.setGlobalAlpha(0.15);
            gc.setFill(Color.web("#ffd700")); // Gold
            // Draw diagonal "rays" across the screen
            for (int i = 0; i < 10; i++) {
                double rayX = (time * 0.05 + i * 200) % (w + 400) - 200;
                gc.fillPolygon(
                    new double[]{rayX, rayX + 100, rayX + 150, rayX + 50},
                    new double[]{0, 0, h, h}, 4
                );
            }
            gc.setGlobalAlpha(1.0);
        }

        // 3. PEST CONTROL MISTING (REALISTIC)
        PestControl pc = garden.getPestControl();
        if (pc != null && pc.getLastActivationTick() != -1) {
            int ticksSince = currentTick - pc.getLastActivationTick();
            if (ticksSince >= 0 && ticksSince < 40) { // Mist lasts longer (40 ticks)
                // Smooth fade in and out
                double alpha;
                if (ticksSince < 10) alpha = ticksSince / 10.0 * 0.35; // Fade in
                else alpha = (40 - ticksSince) / 30.0 * 0.35; // Slow fade out
                
                gc.setGlobalAlpha(alpha);
                gc.setFill(Color.web("#f0f8ff")); // Cloud White
                
                // Draw multiple layers of "clouds" for a thick, realistic look
                for(int i=0; i<5; i++) {
                    double offset = (time * 0.02 + i * 100) % w;
                    gc.fillOval(offset - 200, -100, 400, h + 200);
                    gc.fillOval(w - offset - 200, -100, 400, h + 200);
                }
                
                gc.setGlobalAlpha(1.0);
            }
        }
    }

    private void drawSprinklers(GraphicsContext gc, double progress) {
        if (garden.getWateringSystem() == null) return;

        for (WateringSystem.Sprinkler spr : garden.getWateringSystem().getSprinklers()) {
            double x  = spr.getPosition().getCol() * cellW;
            double y  = spr.getPosition().getRow() * cellH;
            double cx = x + cellW / 2.0;
            double cy = y + cellH / 2.0;
            double cellMin = Math.min(cellW, cellH);

            if (spr.isActive()) {
                long time = System.currentTimeMillis();
                
                // 1. Energetic Pulsing Glow
                double pulse = 1.0 + 0.08 * Math.sin(time * 0.01);
                double radius = spr.getRadius() * cellMin * pulse;
                
                gc.setFill(new RadialGradient(0, 0, cx, cy, radius, false, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.rgb(100, 200, 255, 0.3)),
                    new Stop(1, Color.rgb(52, 152, 219, 0.0))
                ));
                gc.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);

                // 2. Spinning Water Jets
                gc.setStroke(Color.web("#79c0ff", 0.6));
                gc.setLineWidth(3);
                for (int j = 0; j < 4; j++) {
                    double angle = (time * 0.005) + (j * Math.PI / 2);
                    double startX = cx + Math.cos(angle) * 5;
                    double startY = cy + Math.sin(angle) * 5;
                    double endX = cx + Math.cos(angle) * (radius * 0.8);
                    double endY = cy + Math.sin(angle) * (radius * 0.8);
                    gc.strokeLine(startX, startY, endX, endY);
                }

                // 3. High-Speed Water Drops
                gc.setFill(Color.WHITE);
                for (int i = 0; i < 12; i++) {
                    double angle    = (time * 0.002 * 4 + i * 0.52);
                    double dropDist = (time % 500) * radius / 500.0;
                    double dx = cx + Math.cos(angle) * dropDist;
                    double dy = cy + Math.sin(angle) * dropDist;
                    gc.fillOval(dx - 1.5, dy - 1.5, 3, 3);
                }
            }

            // Sprinkler image or fallback shape
            String sprKey = spr.isActive() ? "sprinkler_on" : "sprinkler";
            Image sprImg = imageManager.getImage(sprKey);
            if (sprImg != null) {
                gc.drawImage(sprImg, x, y, cellW, cellH);
            } else {
                double sz = cellMin * 0.45;
                gc.setFill(spr.isActive() ? Color.web("#99ff33") : Color.web("#4d3319"));
                gc.fillRoundRect(cx - sz / 2, cy - sz / 2, sz, sz, 4, 4);
                gc.setFill(Color.WHITE);
                gc.setFont(Font.font("System", FontWeight.BOLD, Math.max(8, (int)(cellMin * 0.24))));
                gc.fillText("S", cx - cellW * 0.07, cy + cellH * 0.1);
            }
        }
    }

    private void drawSensors(GraphicsContext gc) {
        for (Sensor sensor : garden.getSensors()) {
            double x  = sensor.getPosition().getCol() * cellW;
            double y  = sensor.getPosition().getRow() * cellH;
            double cx = x + cellW / 2.0;
            double cy = y + cellH / 2.0;
            double cellMin = Math.min(cellW, cellH);

            if (sensor.isAlertTriggered()) {
                int tick = garden.getCurrentTick();
                double pulse = 16 + 6 * Math.sin(tick * 0.3);
                gc.setFill(Color.rgb(255, 51, 0, 0.3));
                gc.fillOval(cx - pulse, cy - pulse, pulse * 2, pulse * 2);
            }

            Image sensorImg = imageManager.getImage("sensor");
            if (sensorImg != null) {
                gc.drawImage(sensorImg, x + 4, y + 4, cellMin * 0.65, cellMin * 0.65);
            } else {
                gc.setFill(sensor.isAlertTriggered() ? Color.web("#ff3300") : Color.web("#d9b38c"));
                gc.fillOval(cx - 5, cy - 5, 10, 10);
            }
        }
    }

    private void drawPlants(GraphicsContext gc, double progress) {
        long time = System.currentTimeMillis();
        for (Plant plant : garden.getPlants()) {
            double x  = plant.getPosition().getCol() * cellW;
            double y  = plant.getPosition().getRow() * cellH;
            double cx = x + cellW / 2.0;
            double cy = y + cellH / 2.0;
            double cellMin = Math.min(cellW, cellH);

            // --- UNIQUE SPECIES-BASED ANIMATIONS ---
            double bob = 0, tilt = 0, stretchY = 1.0, squashX = 1.0;
            String species = plant.getName().toLowerCase();

            if (species.contains("sunflower")) {
                // Wide, slow rhythmic swaying (tracking light)
                bob = Math.sin(time * 0.002 + (x * 0.05)) * 4;
                tilt = Math.sin(time * 0.0015 + (x * 0.05)) * 15;
                stretchY = 1.0 + Math.sin(time * 0.003) * 0.05;
            } else if (species.contains("cactus")) {
                // Very stiff, just a tiny periodic "shiver"
                tilt = Math.sin(time * 0.02) * 0.8;
                stretchY = 1.0;
            } else if (species.contains("carrot")) {
                // Fast, nervous wiggling of the greens
                bob = Math.sin(time * 0.01) * 1.5;
                tilt = Math.sin(time * 0.012) * 5;
            } else if (species.contains("tomato")) {
                // Heavy fruit weight - slow, deep squash/stretch
                bob = Math.sin(time * 0.003) * 2;
                stretchY = 1.0 + Math.sin(time * 0.004) * 0.08;
                tilt = Math.sin(time * 0.002) * 3;
            } else if (species.contains("rose")) {
                // Elegant, graceful bobbing
                bob = Math.sin(time * 0.004) * 5;
                tilt = Math.sin(time * 0.003) * 8;
            } else {
                // Default: rhythmic "breathing"
                stretchY = 1.0 + Math.sin(time * 0.005 + (x + y)) * 0.06;
                tilt = Math.sin(time * 0.003 + (y * 0.1)) * 6;
            }
            squashX = 1.0 / stretchY; 

            String imgKey  = imageManager.getPlantImageKey(
                    plant.getName(), plant.getGrowthStage().name(), plant.isAlive());
            Image plantImg = (imgKey != null) ? imageManager.getImage(imgKey) : null;

            gc.save(); // Save state for transformation
            gc.translate(cx, cy + bob); // Move to center of plant (with bobbing)
            gc.rotate(tilt); // Apply rhythmic tilt

            if (plantImg != null) {
                double baseScale = switch (plant.getGrowthStage()) {
                    case SEED       -> 0.30;
                    case SPROUT     -> 0.50;
                    case VEGETATIVE -> 0.70;
                    case FLOWERING  -> 0.85;
                    case FRUITING   -> 0.92;
                    case MATURE     -> 1.00;
                    case WILTING    -> 0.60;
                    case DEAD       -> 0.45;
                };

                double imgW = cellW * baseScale * squashX;
                double imgH = cellH * baseScale * stretchY;

                // Draw centered
                gc.drawImage(plantImg, -imgW / 2.0, -imgH / 2.0, imgW, imgH);
            } else {
                // Fallback Shape
                if (!plant.isAlive()) {
                    gc.setFill(Color.web("#261a0d"));
                    gc.fillOval(-8, cellH/2 - 12, 16, 8);
                } else {
                    double hp = plant.getHealth() / 100.0;
                    gc.setFill(Color.color(0.1, 0.8 * hp, 0.1));
                    double size = cellMin * 0.6;
                    gc.fillOval(-size/2 * squashX, -size/2 * stretchY, size * squashX, size * stretchY);
                }
            }
            gc.restore(); // Restore state

            // Health bar stays fixed at the bottom
            if (plant.isAlive()) {
                double barW = cellW * 0.7;
                double barX = x + (cellW - barW)/2;
                double barY = y + cellH - 6;
                gc.setFill(Color.BLACK);
                gc.fillRect(barX, barY, barW, 4);
                gc.setFill(plant.getHealth() > 50 ? Color.web("#99ff33") : Color.web("#ff3300"));
                gc.fillRect(barX, barY, barW * (plant.getHealth()/100.0), 4);
            }
        }
    }

    private void drawInsects(GraphicsContext gc, double progress) {
        long time = System.currentTimeMillis();
        for (Insect insect : garden.getInsects()) {
            if (!insect.isAlive()) continue;

            // SMOOTH POSITION INTERPOLATION
            double startX = insect.getPreviousPosition().getCol() * cellW;
            double startY = insect.getPreviousPosition().getRow() * cellH;
            double targetX = insect.getPosition().getCol() * cellW;
            double targetY = insect.getPosition().getRow() * cellH;
            
            double x = startX + (targetX - startX) * progress;
            double y = startY + (targetY - startY) * progress;
            
            // --- UNIQUE INSECT LOCOMOTION ---
            double buzzX = 0, buzzY = 0, hoverTilt = 0, scaleX = 1.0, scaleY = 1.0;
            String name = insect.getName().toLowerCase();

            if (name.contains("bee")) {
                // High-frequency erratic buzzing
                buzzX = Math.sin(time * 0.045) * 4.5;
                buzzY = Math.cos(time * 0.04) * 4.5;
                hoverTilt = Math.sin(time * 0.02) * 15;
            } else if (name.contains("caterpillar")) {
                // "Inchworm" effect: stretch and contract during movement
                // We use simulation 'progress' to drive the physical stretch
                double cycle = Math.sin(progress * Math.PI);
                scaleX = 1.0 + cycle * 0.4;
                scaleY = 1.0 - cycle * 0.2;
                hoverTilt = Math.sin(time * 0.005) * 3; // slow wobble
            } else if (name.contains("aphid")) {
                // Heavy, slow drunken wobble
                buzzX = Math.sin(time * 0.008) * 2;
                hoverTilt = Math.sin(time * 0.01) * 12;
                scaleY = 1.0 + Math.sin(time * 0.015) * 0.1;
            } else if (name.contains("ladybug")) {
                // Smooth circular scouting hover
                buzzX = Math.sin(time * 0.01) * 6;
                buzzY = Math.cos(time * 0.01) * 3;
                hoverTilt = Math.sin(time * 0.01) * 10;
            } else {
                // Default buzzing
                buzzX = Math.sin(time * 0.025) * 2.5;
                buzzY = Math.cos(time * 0.02) * 2.5;
                hoverTilt = Math.sin(time * 0.01) * 8;
            }

            double cx = x + cellW / 2.0 + buzzX;
            double cy = y + cellH / 2.0 + buzzY;
            double cellMin = Math.min(cellW, cellH);

            gc.save();
            gc.translate(cx, cy);
            gc.rotate(hoverTilt);
            gc.scale(scaleX, scaleY);

            String imgKey   = imageManager.getInsectImageKey(insect.getName());
            Image insectImg = (imgKey != null) ? imageManager.getImage(imgKey) : null;

            if (insectImg != null) {
                double insectSize = cellMin * 0.55;
                gc.setFill(Color.rgb(0, 0, 0, 0.3)); // Shadow
                gc.fillOval(-insectSize/2 + 2, insectSize/2 - 2, insectSize - 4, 4);
                gc.drawImage(insectImg, -insectSize / 2.0, -insectSize / 2.0, insectSize, insectSize);
            } else {
                Color dotColor = switch (insect.getType()) {
                    case PEST       -> Color.web("#ff7b72");
                    case POLLINATOR -> Color.web("#e3b341");
                    case BENEFICIAL -> Color.web("#3fb950");
                    case NEUTRAL    -> Color.web("#d2a8ff");
                };
                gc.setFill(Color.rgb(0, 0, 0, 0.6));
                gc.fillOval(-7, -7, 14, 14);
                gc.setFill(dotColor);
                gc.fillOval(-5, -5, 10, 10);
            }
            gc.restore();
        }
    }

    // =========================================================================
    // LOG VIEWER
    // =========================================================================

    private void refreshLog() {
        String filter = logFilterCombo.getValue();
        if (filter == null) filter = "ALL";

        List<GardenLogger.LogEntry> entries;
        if ("ERRORS ONLY".equals(filter)) {
            entries = GardenLogger.getInstance().getEntriesByLevel("ERROR");
        } else if ("ALL".equals(filter)) {
            entries = GardenLogger.getInstance().getRecentEntries();
        } else {
            entries = GardenLogger.getInstance().getEntriesByCategory(filter);
        }

        StringBuilder sb = new StringBuilder();
        int start = Math.max(0, entries.size() - 100);
        for (int i = start; i < entries.size(); i++) {
            sb.append(entries.get(i).toString()).append("\n");
        }
        logArea.setText(sb.toString());
        logArea.setScrollTop(Double.MAX_VALUE);
    }
}
