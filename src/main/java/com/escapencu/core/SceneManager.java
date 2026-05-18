package com.escapencu.core;

import com.escapencu.ui.GameOverScene;
import com.escapencu.ui.GameScene;
import com.escapencu.ui.MainMenuScene;
import com.escapencu.ui.TalentSelectScene;
import com.escapencu.ui.VictoryScene;
import javafx.stage.Stage;

public class SceneManager {
    private static Stage stage;

    public static void init(Stage primaryStage) {
        stage = primaryStage;
    }

    public static void showMainMenu() {
        stage.setScene(new MainMenuScene().build());
    }

    public static void showTalentSelect() {
        stage.setScene(new TalentSelectScene().build());
    }

    public static void showGame() {
        GameState.reset();
        stage.setScene(new GameScene().build());
    }

    public static void showGameOver() {
        stage.setScene(new GameOverScene().build());
    }

    public static void showVictory() {
        stage.setScene(new VictoryScene().build());
    }
}
