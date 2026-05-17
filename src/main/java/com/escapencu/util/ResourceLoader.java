package com.escapencu.util;

import javafx.scene.image.Image;
import java.util.HashMap;
import java.util.Map;

public class ResourceLoader {
    private static final Map<String, Image> cache = new HashMap<>();

    /**
     * Load an image from the resources folder.
     * Path example: "/images/player.png"
     */
    public static Image getImage(String path) {
        return cache.computeIfAbsent(path, p -> {
            var stream = ResourceLoader.class.getResourceAsStream(p);
            if (stream == null) {
                System.err.println("[ResourceLoader] Image not found: " + p);
                return null;
            }
            return new Image(stream);
        });
    }
}
