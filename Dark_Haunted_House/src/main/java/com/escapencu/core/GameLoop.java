package com.escapencu.core;

import com.escapencu.ui.GameScene;
import javafx.animation.AnimationTimer;

public class GameLoop extends AnimationTimer {
    private final GameScene gameScene;
    private long lastTime = 0;

    public GameLoop(GameScene gameScene) {
        this.gameScene = gameScene;
    }

    @Override
    public void handle(long now) {
        if (lastTime == 0) { lastTime = now; return; }

        double deltaTime = (now - lastTime) / 1_000_000_000.0;
        lastTime = now;

        // Cap to 50ms to avoid huge jumps when window is minimised
        if (deltaTime > 0.05) deltaTime = 0.05;

        gameScene.update(deltaTime);
        gameScene.render();
    }
}
