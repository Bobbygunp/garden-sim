package garden.ui;

import javafx.scene.image.Image;
import java.util.HashMap;
import java.util.Map;

/** Loads and caches garden image resources. */
public class ImageManager {

    private final Map<String, Image> images = new HashMap<>();
    private final int defaultSize;

    public ImageManager(int defaultSize) {
        this.defaultSize = defaultSize;
        loadAllImages();
    }

    private void loadAllImages() {
        loadImage("tomato",     "/garden/images/plants/tomato.png");
        loadImage("rose",       "/garden/images/plants/rose.png");
        loadImage("sunflower",  "/garden/images/plants/sunflower.png");
        loadImage("carrot",     "/garden/images/plants/carrot.png");
        loadImage("lettuce",    "/garden/images/plants/lettuce.png");
        loadImage("cactus",     "/garden/images/plants/cactus.png");

        loadImage("sprout_tomato",    "/garden/images/plants/sprout_tomato.png");
        loadImage("sprout_rose",      "/garden/images/plants/sprout_rose.png");
        loadImage("sprout_sunflower", "/garden/images/plants/sprout_sunflower.png");
        loadImage("sprout_carrot",    "/garden/images/plants/sprout_carrot.png");
        loadImage("sprout_lettuce",   "/garden/images/plants/sprout_lettuce.png");
        loadImage("sprout_cactus",    "/garden/images/plants/sprout_cactus.png");

        loadImage("dead_tomato",    "/garden/images/plants/dead_tomato.png");
        loadImage("dead_rose",      "/garden/images/plants/dead_rose.png");
        loadImage("dead_sunflower", "/garden/images/plants/dead_sunflower.png");
        loadImage("dead_carrot",    "/garden/images/plants/dead_carrot.png");
        loadImage("dead_lettuce",   "/garden/images/plants/dead_lettuce.png");
        loadImage("dead_cactus",    "/garden/images/plants/dead_cactus.png");

        loadImage("bee",         "/garden/images/insects/bee.png");
        loadImage("aphid",       "/garden/images/insects/aphid.png");
        loadImage("caterpillar", "/garden/images/insects/caterpillar.png");
        loadImage("ladybug",     "/garden/images/insects/ladybug.png");

        loadImage("sprinkler",    "/garden/images/equipment/sprinkler.png");
        loadImage("sprinkler_on", "/garden/images/equipment/sprinkler_on.png");
        loadImage("sensor",       "/garden/images/equipment/sensor.png");
    }

    private void loadImage(String name, String resourcePath) {
        try {
            var url = getClass().getResource(resourcePath);
            if (url != null) {
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

    public Image getImage(String name) {
        return images.get(name);
    }

    public boolean hasImage(String name) {
        return images.containsKey(name);
    }

    /** Returns the image key for plant display state. */
    public String getPlantImageKey(String plantName, String growthStage, boolean alive) {
        String species = plantName.toLowerCase();

        if (!alive || "DEAD".equals(growthStage)) return "dead_" + species;

        if ("SEED".equals(growthStage)) return null;

        if ("SPROUT".equals(growthStage)) return "sprout_" + species;

        return switch (species) {
            case "tomato", "rose", "sunflower", "carrot", "lettuce", "cactus" -> species;
            default -> null;
        };
    }

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