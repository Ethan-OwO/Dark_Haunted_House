package com.escapencu.level;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/** A narrow passable passage connecting two rooms in world space. */
public class Corridor {
    public final double worldX, worldY, worldW, worldH;
    private final boolean horizontal; // true = East-West, false = North-South

    public Corridor(double worldX, double worldY, double worldW, double worldH) {
        this.worldX      = worldX;
        this.worldY      = worldY;
        this.worldW      = worldW;
        this.worldH      = worldH;
        this.horizontal  = worldW >= worldH;
    }

    public void draw(GraphicsContext gc) {
        // Floor
        gc.setFill(Color.rgb(48, 44, 40));
        gc.fillRect(worldX, worldY, worldW, worldH);
        // Side walls (thin dark lines along the long edges)
        gc.setFill(Color.rgb(30, 28, 26));
        if (horizontal) {
            gc.fillRect(worldX, worldY,              worldW, 6); // top wall strip
            gc.fillRect(worldX, worldY + worldH - 6, worldW, 6); // bottom wall strip
        } else {
            gc.fillRect(worldX,              worldY, 6, worldH); // left wall strip
            gc.fillRect(worldX + worldW - 6, worldY, 6, worldH); // right wall strip
        }
    }

    /** True if the given rectangle overlaps this corridor's passable area. */
    public boolean overlaps(double px, double py, double pw, double ph) {
        return px < worldX + worldW && px + pw > worldX
            && py < worldY + worldH && py + ph > worldY;
    }
}
