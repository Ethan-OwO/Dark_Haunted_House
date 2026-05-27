package com.escapencu.ui;

import com.escapencu.application.GameApp;
import com.escapencu.core.GameState;
import com.escapencu.core.SceneManager;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class GameOverScene {

    // 載入支援中文的像素字型（Windows 也適用，只是換了字型外觀）
    private static Font pf(double size) {
        Font f = Font.loadFont(GameOverScene.class.getResourceAsStream("/fonts/Cubic_11.ttf"), size);
        return f != null ? f : Font.font(size);
    }

    public Scene build() {
        Text title = new Text("遊戲結束");
        title.setFont(pf(52));
        title.setFill(Color.RED);

        Text score = new Text("分數：" + GameState.score);
        score.setFont(pf(26));
        score.setFill(Color.WHITE);

        Button retryBtn = new Button("再試一次");
        retryBtn.setFont(pf(20));
        retryBtn.setPrefWidth(220);
        retryBtn.setOnAction(e -> SceneManager.showGame());

        Button menuBtn = new Button("回主選單");
        menuBtn.setFont(pf(20));
        menuBtn.setPrefWidth(220);
        menuBtn.setOnAction(e -> SceneManager.showMainMenu());

        VBox root = new VBox(22, title, score, retryBtn, menuBtn);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #1a0000;");

        return new Scene(root, GameApp.WIDTH, GameApp.HEIGHT);
    }
}
