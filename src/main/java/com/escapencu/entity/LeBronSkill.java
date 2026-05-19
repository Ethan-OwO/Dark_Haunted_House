package com.escapencu.entity;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LeBronSkill {

    private enum Phase { RUN_IN, SLAM, DONE }

    private Phase  phase      = Phase.RUN_IN;
    private double x;
    private final double targetX, targetY;
    private static final double RUN_SPEED     = 600.0;
    private static final double SLAM_DURATION = 0.6;
    private double slamTimer = 0;
    private boolean alive    = true;

    private int    frameIndex = 0;        // 0 = FRAME_A, 1 = FRAME_B
    private double frameTimer = 0;
    private static final double FRAME_INTERVAL = 0.28; // 280ms per frame

    private final List<FirePatch> pendingFire = new ArrayList<>();

    // ── Pixel art color map ──────────────────────────────────────────────────
    private static final Map<Character, Color> COLORS = Map.ofEntries(
        Map.entry('K', Color.web("#000000")),
        Map.entry('H', Color.web("#171010")),
        Map.entry('h', Color.web("#2a1a14")),
        Map.entry('B', Color.web("#FDB927")),
        Map.entry('b', Color.web("#C99124")),
        Map.entry('S', Color.web("#a96b41")),
        Map.entry('D', Color.web("#6e3f23")),
        Map.entry('E', Color.web("#FFFFFF")),
        Map.entry('J', Color.web("#552583")),
        Map.entry('j', Color.web("#3a1660")),
        Map.entry('P', Color.web("#FDB927")),
        Map.entry('p', Color.web("#C99124")),
        Map.entry('W', Color.web("#f3f3f3")),
        Map.entry('w', Color.web("#bdbdbd")),
        Map.entry('R', Color.web("#c41e3a")),
        Map.entry('O', Color.web("#e07a23")),
        Map.entry('o', Color.web("#7a3a10")),
        Map.entry('F', Color.web("#FFE03D")),
        Map.entry('f', Color.web("#FF8C1A")),
        Map.entry('r', Color.web("#DB2A1F")),
        Map.entry('d', Color.web("#7A1500")),
        Map.entry('G', Color.web("#36A0FF")),
        Map.entry('g', Color.web("#1056b8"))
    );

    // ── Sprite frames (32 cols × 40 rows) ───────────────────────────────────
    public static final String[] FRAME_A = {
        "................................",
        "................................",
        "................................",
        "..............KKKKKK............",
        ".............KHHHHHHHK..........",
        "............KHHHHHHHHHK.........",
        "............KHHHHHHHHHK.........",
        "............KBBBBBBBBBK.........",
        "............KBbBBBBBbBK.........",
        "............KSSDDSDDSSK.........",
        "............KSKSSSSKSSK.........",
        "............KSSSSSSSSSK.........",
        "............KSDDSDDDDSK.........",
        ".............KDDDDDDDK..........",
        "..............KKDDDKK...........",
        "..............KSSSSK............",
        "...........KKJJJJJJJJKK.........",
        "..........KJJJJJJJJJJJJK........",
        ".........KSSKJJJJJJJJKKSSK......",
        ".........KSSKJBBBBBBJKSSK.......",
        ".........KSSKJBJ.B.BJKSSK.......",
        ".........KSSKJBBB.BBJKSSK.......",
        ".........KSSKJB...BBJKSSK.......",
        ".........KSSKJBBB.BBJKSSK.......",
        ".........KSSKJJJJJJJKKSSK.......",
        ".........KSSKKJJJJJJK.KSSK......",
        ".........KSSK.KPPPPPK..KSSK.....",
        ".........KSSK.KPpPpPK..KSSK.....",
        ".........KSSK.KPPPPPK..KSSK.....",
        "..........KKK.KKKKKKK..KSSK.....",
        "..............KSS.KSK..KSSK.....",
        "..............KSS.KSK..KKKK.....",
        "..............KSS.KSK...........",
        "..............KSS.KSK...KKKK....",
        "..............KSS.KSK...KOOK....",
        ".............KWWWWWWWK..KOoOK...",
        ".............KKWRWKWRWKKKOOoK...",
        "..............KKKKKKKK...KOOK...",
        ".........................KKKK...",
        "................................",
    };

    public static final String[] FRAME_B = {
        "................................",
        "................................",
        "................................",
        "................................",
        "..............KKKKKK............",
        ".............KHHHHHHHK..........",
        "............KHHHHHHHHHK.........",
        "............KHHHHHHHHHK.........",
        "............KBBBBBBBBBK.........",
        "............KBbBBBBBbBK.........",
        "............KSSDDSDDSSK.........",
        "............KSKSSSSKSSK.........",
        "............KSSSSSSSSSK.........",
        "............KSDDSDDDDSK.........",
        ".............KDDDDDDDK..........",
        "..............KKDDDKK...........",
        "..............KSSSSK............",
        "...........KKJJJJJJJJKK.........",
        "..........KJJJJJJJJJJJJK........",
        ".........KSSKJJJJJJJJKKSSK......",
        ".........KSSKJBBBBBBJKSSK.......",
        ".........KSSKJBJ.B.BJKSSK.......",
        "........KKSKJBBB.BBJKSSK........",
        "........KOSKJB...BBJKSSK........",
        "........KOoKJBBB.BBJKSSK........",
        "........KOOKJJJJJJJKKSSK........",
        "........KKKKKJJJJJJK.KSSK.......",
        "..............KPPPPPK..KSSK.....",
        "..............KPpPpPK..KSSK.....",
        "..............KPPPPPK..KSSK.....",
        "..............KKKKKKK..KSSK.....",
        "..............KSS.KSK..KKKK.....",
        "..............KSS.KSK...........",
        "..............KSS.KSK...........",
        "..............KSS.KSK...........",
        ".............KWWWWWWWK..........",
        ".............KKWRWKWRWKK........",
        "..............KKKKKKKK..........",
        "................................",
        "................................",
    };

    // ── Constructor ──────────────────────────────────────────────────────────
    public LeBronSkill(double startX, double targetX, double targetY) {
        this.x       = startX;
        this.targetX = targetX;
        this.targetY = targetY;
    }

    // ── Update ───────────────────────────────────────────────────────────────
    public void update(double dt, List<Entity> enemies) {
        switch (phase) {
            case RUN_IN -> {
                frameTimer += dt;
                if (frameTimer >= FRAME_INTERVAL) {
                    frameTimer -= FRAME_INTERVAL;
                    frameIndex  = 1 - frameIndex;
                }
                x += RUN_SPEED * dt;
                if (x >= targetX) {
                    x = targetX;
                    phase = Phase.SLAM;
                    for (Entity e : enemies) {
                        if (e.isAlive()) e.takeDamage(25);
                    }
                    spawnFire();
                }
            }
            case SLAM -> {
                slamTimer += dt;
                if (slamTimer >= SLAM_DURATION) phase = Phase.DONE;
            }
            case DONE -> alive = false;
        }
    }

    private void spawnFire() {
        for (int i = 0; i < 6; i++) {
            double angle  = Math.random() * Math.PI * 2;
            double dist   = Math.random() * 90;
            double radius = 40 + Math.random() * 20;
            pendingFire.add(new FirePatch(
                targetX + Math.cos(angle) * dist,
                targetY + Math.sin(angle) * dist,
                radius
            ));
        }
        pendingFire.add(new FirePatch(targetX, targetY, 55));
    }

    // ── Draw ─────────────────────────────────────────────────────────────────
    public void draw(GraphicsContext gc) {
        if (phase == Phase.RUN_IN) {
            String[] grid = (frameIndex == 0) ? FRAME_A : FRAME_B;
            // 每格 4px，sprite 寬 32*4=128，高 40*4=160
            // 以 (x, targetY) 為 sprite 底部中心對齊
            int cell = 4;
            double originX = x - (32 * cell) / 2.0;
            double originY = targetY - 40 * cell;
            drawPixelSprite(gc, grid, cell, originX, originY);
        } else if (phase == Phase.SLAM) {
            // SLAM 定格 Frame A
            int cell = 4;
            double originX = x - (32 * cell) / 2.0;
            double originY = targetY - 40 * cell;
            drawPixelSprite(gc, FRAME_A, cell, originX, originY);
            drawSlamEffect(gc);
        }
    }

    /** 把 pixel-art grid 畫到 (originX, originY) 位置，每格 cell px。 */
    public static void drawPixelSprite(GraphicsContext gc, String[] grid, int cell, double originX, double originY) {
        for (int row = 0; row < grid.length; row++) {
            String line = grid[row];
            for (int col = 0; col < line.length(); col++) {
                char ch = line.charAt(col);
                Color c = COLORS.get(ch);
                if (c == null) continue;
                gc.setFill(c);
                gc.fillRect(originX + col * cell, originY + row * cell, cell, cell);
            }
        }
    }

    private void drawSlamEffect(GraphicsContext gc) {
        double progress = slamTimer / SLAM_DURATION;
        double r = progress * 140;
        gc.setGlobalAlpha(Math.max(0, 1.0 - progress));
        gc.setStroke(Color.web("#FDB927"));
        gc.setLineWidth(5);
        gc.strokeOval(targetX - r, targetY - r, r * 2, r * 2);
        double r2 = progress * 80;
        gc.setStroke(Color.ORANGERED);
        gc.setLineWidth(3);
        gc.strokeOval(targetX - r2, targetY - r2, r2 * 2, r2 * 2);
        gc.setGlobalAlpha(1.0);
        gc.setLineWidth(1);
    }

    public boolean isAlive()                { return alive; }
    public List<FirePatch> getPendingFire() { return pendingFire; }
}
