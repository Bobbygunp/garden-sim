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
        // === PLANTS — adult images ===
        loadImage("tomato",     "/garden/images/plants/tomato.png");
        loadImage("rose",       "/garden/images/plants/rose.png");
        loadImage("sunflower",  "/garden/images/plants/sunflower.png");
        loadImage("carrot",     "/garden/images/plants/carrot.png");
        loadImage("lettuce",    "/garden/images/plants/lettuce.png");
        loadImage("cactus",     "/garden/images/plants/cactus.png");

        // === PLANTS — sprout (sapling) images ===
        loadImage("sprout_tomato",    "/garden/images/plants/sprout_tomato.png");
        loadImage("sprout_rose",      "/garden/images/plants/sprout_rose.png");
        loadImage("sprout_sunflower", "/garden/images/plants/sprout_sunflower.png");
        loadImage("sprout_carrot",    "/garden/images/plants/sprout_carrot.png");
        loadImage("sprout_lettuce",   "/garden/images/plants/sprout_lettuce.png");
        loadImage("sprout_cactus",    "/garden/images/plants/sprout_cactus.png");

        // === PLANTS — dead images (per species) ===
        loadImage("dead_tomato",    "/garden/images/plants/dead_tomato.png");
        loadImage("dead_rose",      "/garden/images/plants/dead_rose.png");
        loadImage("dead_sunflower", "/garden/images/plants/dead_sunflower.png");
        loadImage("dead_carrot",    "/garden/images/plants/dead_carrot.png");
        loadImage("dead_lettuce",   "/garden/images/plants/dead_lettuce.png");
        loadImage("dead_cactus",    "/garden/images/plants/dead_cactus.png");

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
        String species = plantName.toLowerCase();

        // Dead (or explicitly in DEAD stage) → per-species dead image
        if (!alive || "DEAD".equals(growthStage)) return "dead_" + species;

        // SEED stage → drawn in code as a colored seed shape (no image)
        if ("SEED".equals(growthStage)) return null;

        // SPROUT → per-species sapling image
        if ("SPROUT".equals(growthStage)) return "sprout_" + species;

        // VEGETATIVE, FLOWERING, FRUITING, MATURE, WILTING → adult image
        return switch (species) {
            case "tomato", "rose", "sunflower", "carrot", "lettuce", "cactus" -> species;
            default -> null;
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