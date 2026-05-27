package com.escapencu.level;

import com.escapencu.entity.Bullet;
import com.escapencu.entity.Coin;
import com.escapencu.entity.Enemy;
import com.escapencu.entity.Entity;
import com.escapencu.entity.Player;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * One room in the dungeon world.
 * World position is derived from grid position using DungeonFloor constants.
 *
 * Corridor indices:  0=NORTH  1=SOUTH  2=EAST  3=WEST
 */
public abstract class Room {
    public enum Type { START, NORMAL, REWARD, EXIT, BOSS }

    // ── World geometry ─────────────────────────────────────────────────────
    public final double worldX, worldY, worldW, worldH;
    public static final double WALL = 24; // visual wall thickness

    // ── Grid / meta ────────────────────────────────────────────────────────
    public final int  gridX, gridY;
    public final Type type;

    // ── State ──────────────────────────────────────────────────────────────
    private boolean activated = false; // enemies spawned?
    private boolean cleared;           // all enemies dead?

    // Which sides connect to a corridor (used for wall gap drawing)
    // 0=N, 1=S, 2=E, 3=W
    private final boolean[] hasCorridor = new boolean[4];

    protected final List<Entity> enemies = new ArrayList<>();
    protected final List<Coin>   coins   = new ArrayList<>();

    // ── Constructor ────────────────────────────────────────────────────────
    protected Room(int gridX, int gridY, Type type) {
        this.gridX  = gridX;
        this.gridY  = gridY;
        this.type   = type;
        this.worldX = DungeonFloor.roomWorldX(gridX);
        this.worldY = DungeonFloor.roomWorldY(gridY);
        this.worldW = DungeonFloor.ROOM_W;
        this.worldH = DungeonFloor.ROOM_H;
        // START, EXIT, REWARD have no enemies → start cleared
        this.cleared = (type == Type.START || type == Type.EXIT || type == Type.REWARD);
    }

    // ── Activation (called when player first enters) ───────────────────────
    public void activate() {
        if (activated) return;
        activated = true;
        spawnEnemies();
    }

    protected abstract void spawnEnemies();

    // ── Update ─────────────────────────────────────────────────────────────
    public void update(double deltaTime, Player player) {
        // Drain spawn-on-death lists before removing dead entities
        List<Entity> pending = new ArrayList<>();
        for (Entity e : enemies)
            if (e instanceof Enemy en) pending.addAll(en.getPendingSpawns());
        enemies.addAll(pending);

        // Spawn coins for newly-dead enemies (checked before removal)
        for (Entity e : enemies) {
            if (!e.isAlive() && e instanceof Enemy en && !en.hasCoinDropped()) {
                en.markCoinDropped();
                spawnCoins(en);
            }
        }
        enemies.removeIf(e -> !e.isAlive());

        // Build live-enemy list for steering separation (one allocation per frame)
        List<Enemy> liveEnemies = new ArrayList<>();
        for (Entity e : enemies)
            if (e instanceof Enemy en) liveEnemies.add(en);

        for (Entity e : enemies) {
            if (e instanceof Enemy en) {
                // ▼▼▼ 修改點 1：攔截正在生成的怪物 ▼▼▼
                if (en.isSpawning()) {
                    en.decrementSpawnTimer(deltaTime);
                    continue; // 生成中不執行尋路、射擊等 AI 邏輯
                }
                // ▲▲▲ 修改結束 ▲▲▲
                en.setSteeringNeighbors(liveEnemies);
                en.update(deltaTime, player);
            } else {
                e.update(deltaTime);
            }
        }
        // Cleared when no alive entity that counts toward room-clear remains
        // (permanent floor objects like Wing are excluded via countsForRoomClear())
        if (!cleared && enemies.stream().noneMatch(e -> e.isAlive() && e.countsForRoomClear())) {
            cleared = true;
        }

        // Update and collect coins — clamp each coin to inner room bounds
        coins.removeIf(Coin::isCollected);
        double innerMinX = worldX + WALL + 4;
        double innerMinY = worldY + WALL + 4;
        double innerMaxX = worldX + worldW - WALL - 4;
        double innerMaxY = worldY + worldH - WALL - 4;
        for (Coin c : coins) {
            c.update(deltaTime, player);
            c.clampToBounds(innerMinX, innerMinY, innerMaxX, innerMaxY);
        }
    }

    /** Scatter coins from a dead enemy — each coin is worth 1 point. */
    private void spawnCoins(Enemy en) {
        int count = Math.min(en.getCoinValue(), 60); // cap at 60 per kill
        for (int i = 0; i < count; i++)
            coins.add(new Coin(en.getCenterX(), en.getCenterY(), 1));
    }

    // ── Draw (world coordinates) ───────────────────────────────────────────
    public final void draw(GraphicsContext gc) {
        drawFloor(gc);
        drawWalls(gc);
        drawGates(gc);
        for (Entity e : enemies) {
            // ▼▼▼ 修改點 2：根據生成狀態決定畫出警告光圈，還是怪物實體 ▼▼▼
            if (e instanceof Enemy en && en.isSpawning()) {
                en.drawSpawnWarning(gc); // 畫紅色閃爍光圈
            } else {
                e.draw(gc);              // 畫出真正的怪物
            }
        }
        // ▲▲▲ 修改結束 ▲▲▲

        // ── Dropped coins (drawn on top of floor, below player) ───────────
        for (Coin c : coins) c.draw(gc);
    }

    /** Subclass draws the floor/background of the room. */
    protected abstract void drawFloor(GraphicsContext gc);

    private void drawWalls(GraphicsContext gc) {
        gc.setFill(Color.rgb(35, 35, 55));
        double ct = DungeonFloor.CORRIDOR_THICK; // corridor thickness = gap width

        // Top wall
        if (!hasCorridor[0]) {
            gc.fillRect(worldX, worldY, worldW, WALL);
        } else {
            double mx = worldX + worldW / 2;
            gc.fillRect(worldX, worldY, mx - worldX - ct / 2, WALL);
            gc.fillRect(mx + ct / 2, worldY, worldX + worldW - mx - ct / 2, WALL);
        }
        // Bottom wall
        double by = worldY + worldH - WALL;
        if (!hasCorridor[1]) {
            gc.fillRect(worldX, by, worldW, WALL);
        } else {
            double mx = worldX + worldW / 2;
            gc.fillRect(worldX, by, mx - worldX - ct / 2, WALL);
            gc.fillRect(mx + ct / 2, by, worldX + worldW - mx - ct / 2, WALL);
        }
        // Left wall
        if (!hasCorridor[3]) {
            gc.fillRect(worldX, worldY, WALL, worldH);
        } else {
            double my = worldY + worldH / 2;
            gc.fillRect(worldX, worldY, WALL, my - worldY - ct / 2);
            gc.fillRect(worldX, my + ct / 2, WALL, worldY + worldH - my - ct / 2);
        }
        // Right wall
        double rx = worldX + worldW - WALL;
        if (!hasCorridor[2]) {
            gc.fillRect(rx, worldY, WALL, worldH);
        } else {
            double my = worldY + worldH / 2;
            gc.fillRect(rx, worldY, WALL, my - worldY - ct / 2);
            gc.fillRect(rx, my + ct / 2, WALL, worldY + worldH - my - ct / 2);
        }
    }

    /**
     * Draws gate bars at every corridor opening while this room is activated
     * but not yet cleared (NORMAL / BOSS only).
     * Gates are drawn AFTER walls so they visually fill the gap.
     */
    private void drawGates(GraphicsContext gc) {
        if (!activated || cleared) return;
        if (type != Type.NORMAL && type != Type.BOSS) return;

        double ct = DungeonFloor.CORRIDOR_THICK;

        // Main bar colour — dark red
        Color barColor  = Color.rgb(140, 25, 25);
        // Highlight stripe along the top/left edge
        Color highlight = Color.rgb(200, 60, 60);
        double stripe   = 4.0;

        if (hasCorridor[0]) {   // North
            double gx = worldX + worldW / 2 - ct / 2;
            double gy = worldY;
            gc.setFill(barColor);  gc.fillRect(gx, gy, ct, WALL);
            gc.setFill(highlight); gc.fillRect(gx, gy, ct, stripe);
        }
        if (hasCorridor[1]) {   // South
            double gx = worldX + worldW / 2 - ct / 2;
            double gy = worldY + worldH - WALL;
            gc.setFill(barColor);  gc.fillRect(gx, gy, ct, WALL);
            gc.setFill(highlight); gc.fillRect(gx, gy + WALL - stripe, ct, stripe);
        }
        if (hasCorridor[2]) {   // East
            double gx = worldX + worldW - WALL;
            double gy = worldY + worldH / 2 - ct / 2;
            gc.setFill(barColor);  gc.fillRect(gx, gy, WALL, ct);
            gc.setFill(highlight); gc.fillRect(gx + WALL - stripe, gy, stripe, ct);
        }
        if (hasCorridor[3]) {   // West
            double gx = worldX;
            double gy = worldY + worldH / 2 - ct / 2;
            gc.setFill(barColor);  gc.fillRect(gx, gy, WALL, ct);
            gc.setFill(highlight); gc.fillRect(gx, gy, stripe, ct);
        }
    }

    /**
     * Returns true when the given rectangle overlaps a gate of this room.
     * Gates only exist while the room is activated and uncleared (NORMAL / BOSS).
     * Used by DungeonFloor.canMoveTo() to physically block passage.
     */
    public boolean overlapsGate(double px, double py, double pw, double ph) {
        if (!activated || cleared) return false;
        if (type != Type.NORMAL && type != Type.BOSS) return false;

        double ct = DungeonFloor.CORRIDOR_THICK;

        if (hasCorridor[0]) {   // North
            double gx = worldX + worldW / 2 - ct / 2;
            if (rectsOverlap(px, py, pw, ph, gx, worldY, ct, WALL)) return true;
        }
        if (hasCorridor[1]) {   // South
            double gx = worldX + worldW / 2 - ct / 2;
            double gy = worldY + worldH - WALL;
            if (rectsOverlap(px, py, pw, ph, gx, gy, ct, WALL)) return true;
        }
        if (hasCorridor[2]) {   // East
            double gx = worldX + worldW - WALL;
            double gy = worldY + worldH / 2 - ct / 2;
            if (rectsOverlap(px, py, pw, ph, gx, gy, WALL, ct)) return true;
        }
        if (hasCorridor[3]) {   // West
            double gy = worldY + worldH / 2 - ct / 2;
            if (rectsOverlap(px, py, pw, ph, worldX, gy, WALL, ct)) return true;
        }
        return false;
    }

    private static boolean rectsOverlap(double ax, double ay, double aw, double ah,
                                         double bx, double by, double bw, double bh) {
        return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by;
    }

    // ── Geometry helpers ───────────────────────────────────────────────────
    public boolean containsPoint(double px, double py) {
        return px >= worldX && px < worldX + worldW
                && py >= worldY && py < worldY + worldH;
    }

    public boolean overlaps(double px, double py, double pw, double ph) {
        return px < worldX + worldW && px + pw > worldX
                && py < worldY + worldH && py + ph > worldY;
    }

    // ── Accessors ──────────────────────────────────────────────────────────
    public void    setHasCorridor(int dirIndex, boolean val) { hasCorridor[dirIndex] = val; }
    public boolean getHasCorridor(int dirIndex)             { return hasCorridor[dirIndex]; }
    public boolean isActivated() { return activated; }
    public boolean isCleared()   { return cleared; }

    public List<Entity> getEnemies() { return enemies; }

    public List<Bullet> getEnemyBullets() {
        List<Bullet> all = new ArrayList<>();
        for (Entity e : enemies)
            if (e instanceof Enemy en) all.addAll(en.getBullets());
        return all;
    }
}
