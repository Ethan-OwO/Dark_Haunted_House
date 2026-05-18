package com.escapencu.util;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.util.HashMap;
import java.util.Map;

public class ResourceLoader {
    private static final Map<String, Image> cache = new HashMap<>();

    /** Load an image. Path example: "/images/player/idle_s.jpg" */
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

    /**
     * Load an image and replace near-white pixels with transparent ones.
     * Use this for sprites with white backgrounds (e.g. AI-generated JPGs).
     */
    public static Image getImage(String path, boolean removeWhiteBg) {
        if (!removeWhiteBg) return getImage(path);

        String key = path + "#nowbg";
        return cache.computeIfAbsent(key, k -> {
            var stream = ResourceLoader.class.getResourceAsStream(path);
            if (stream == null) {
                System.err.println("[ResourceLoader] Image not found: " + path);
                return null;
            }
            Image src = new Image(stream);
            int w = (int) src.getWidth();
            int h = (int) src.getHeight();
            WritableImage out    = new WritableImage(w, h);
            PixelReader   reader = src.getPixelReader();
            PixelWriter   writer = out.getPixelWriter();
            for (int py = 0; py < h; py++) {
                for (int px = 0; px < w; px++) {
                    Color c = reader.getColor(px, py);
                    // Remove near-white (accounts for JPEG compression artefacts)
                    if (c.getRed() > 0.85 && c.getGreen() > 0.85 && c.getBlue() > 0.85) {
                        writer.setColor(px, py, Color.TRANSPARENT);
                    } else {
                        writer.setColor(px, py, c);
                    }
                }
            }
            return out;
        });
    }
}
