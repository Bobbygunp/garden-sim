package garden.ui;

import javafx.scene.image.Image;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads and caches all garden images from the resources folder.
 * Images are loaded once at startup and reused for rendering.
 *
 * HOW TO ADD IMAGES:
 * 1. Place PNG files in src/main/resources/garden/images/
 * 2. Add a loadImage() call in the constructor below
 * 3. Use getImage("name") to retrieve it for drawing
 */
public class ImageManager {

    private final Map<String, Image> images = new HashMap<>();
    private final int defaultSize;

    /**
     * @param defaultSize The size (width & height) to scale images to (e.g., 30 for CELL_SIZE)
     */
    public ImageManager(int defaultSize) {
        this.defaultSize = defaultSize;
        loadAllImages();
    }

    private void loadAllImages() {
        // === PLANTS ===
        loadImage("tomato",     "/garden/images/plants/tomato.png");
        loadImage("rose",       "/garden/images/plants/rose.png");
        loadImage("sunflower",  "/garden/images/plants/sunflower.png");
        loadImage("carrot",     "/garden/images/plants/carrot.png");
        loadImage("lettuce",    "/garden/images/plants/lettuce.png");
        loadImage("cactus",     "/garden/images/plants/cactus.png");
        loadImage("dead_plant", "/garden/images/plants/dead_plant.png");
        loadImage("seed",       "/garden/images/plants/seed.png");

        // === INSECTS ===
        loadImage("bee",         "/garden/images/insects/bee.png");
        loadImage("aphid",       "/garden/images/insects/aphid.png");
        loadImage("caterpillar", "/garden/images/insects/caterpillar.png");
        loadImage("ladybug",     "/garden/images/insects/ladybug.png");

        // === EQUIPMENT ===
        loadImage("sprinkler",    "/garden/images/equipment/sprinkler.png");
        loadImage("sprinkler_on", "/garden/images/equipment/sprinkler_on.png");
        loadImage("sensor",       "/garden/images/equipment/sensor.png");
    }

    /**
     * Load a single image from the resources folder.
     * If the image file is not found, it logs a warning but does NOT crash.
     */
    private void loadImage(String name, String resourcePath) {
        try {
            var url = getClass().getResource(resourcePath);
            if (url != null) {
                // Load with requested size, preserve aspect ratio, smooth scaling
                Image img = new Image(url.toExternalForm(), defaultSize, defaultSize, true, true);
                images.put(name, img);
            } else {
                System.out.println("WARNING: Image not found: " + resourcePath +
                        " (will use fallback shapes)");
            }
        } catch (Exception e) {
            System.out.println("WARNING: Failed to load image " + resourcePath + ": " + e.getMessage());
        }
    }

    /**
     * Get a loaded image by name. Returns null if not found.
     */
    public Image getImage(String name) {
        return images.get(name);
    }

    /**
     * Check if an image was loaded successfully.
     */
    public boolean hasImage(String name) {
        return images.containsKey(name);
    }

    /**
     * Get the image key for a plant species name.
     * Maps the plant's getName() to the correct image key.
     */
    public String getPlantImageKey(String plantName, String growthStage, boolean alive) {
        if (!alive) return "dead_plant";
        if ("SEED".equals(growthStage)) return "seed";

        // Map plant name to image key
        return switch (plantName.toLowerCase()) {
            case "tomato" -> "tomato";
            case "rose" -> "rose";
            case "sunflower" -> "sunflower";
            case "carrot" -> "carrot";
            case "lettuce" -> "lettuce";
            case "cactus" -> "cactus";
            default -> null; // fallback to shapes
        };
    }

    /**
     * Get the image key for an insect name.
     */
    public String getInsectImageKey(String insectName) {
        return switch (insectName.toLowerCase()) {
            case "bee" -> "bee";
            case "aphid" -> "aphid";
            case "caterpillar" -> "caterpillar";
            case "ladybug" -> "ladybug";
            default -> null;
        };
    }
}