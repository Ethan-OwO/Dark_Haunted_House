package com.escapencu.ui;

import com.escapencu.application.GameApp;
import com.escapencu.core.SceneManager;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class MainMenuScene {
    public Scene build() {
        Text title = new Text("逃離中央大學");
        title.setFont(Font.font(52));
        title.setFill(Color.WHITE);

        Text subtitle = new Text("Escape from NCU");
        subtitle.setFont(Font.font(20));
        subtitle.setFill(Color.LIGHTGRAY);

        Button startBtn = new Button("開始遊戲");
        startBtn.setFont(Font.font(24));
        startBtn.setPrefWidth(220);
        startBtn.setOnAction(e -> SceneManager.showTalentSelect());

        Button exitBtn = new Button("離開");
        exitBtn.setFont(Font.font(18));
        exitBtn.setPrefWidth(220);
        exitBtn.setOnAction(e -> System.exit(0));

        VBox root = new VBox(22, title, subtitle, startBtn, exitBtn);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #1a1a2e;");

        return new Scene(root, GameApp.WIDTH, GameApp.HEIGHT);
    }
}
