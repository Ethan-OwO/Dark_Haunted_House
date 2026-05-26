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

public class VictoryScene {
    public Scene build() {
        Text title = new Text("恭喜畢業！");
        title.setFont(Font.font(52));
        title.setFill(Color.GOLD);

        Text msg = new Text("你成功逃離中央大學！");
        msg.setFont(Font.font(28));
        msg.setFill(Color.WHITE);

        Text score = new Text("最終分數：" + GameState.score);
        score.setFont(Font.font(22));
        score.setFill(Color.LIGHTGRAY);

        Button menuBtn = new Button("回主選單");
        menuBtn.setFont(Font.font(20));
        menuBtn.setPrefWidth(220);
        menuBtn.setOnAction(e -> SceneManager.showMainMenu());

        VBox root = new VBox(22, title, msg, score, menuBtn);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #0a0a1a;");

        return new Scene(root, GameApp.WIDTH, GameApp.HEIGHT);
    }
}
