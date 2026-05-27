package com.escapencu.core;

import com.escapencu.application.GameApp;
import com.escapencu.ui.GameOverScene;
import com.escapencu.ui.GameScene;
import com.escapencu.ui.MainMenuScene;
import com.escapencu.ui.TalentSelectScene;
import com.escapencu.ui.VictoryScene;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;

public class SceneManager {
    private static Stage  stage;
    private static Pane   wrapper;   // 永遠不換，撐滿視窗
    private static Group  content;   // 永遠不換，放遊戲內容
    private static Scale     scaleT;
    private static Translate transT;

    public static void init(Stage primaryStage) {
        stage = primaryStage;

        // 建一次，之後只換 content 裡的子節點
        scaleT = new Scale(1, 1, 0, 0);
        transT = new Translate(0, 0);
        content = new Group();
        content.getTransforms().addAll(transT, scaleT);

        wrapper = new Pane(content);
        wrapper.setStyle("-fx-background-color: black;");

        Scene scene = new Scene(wrapper, GameApp.WIDTH, GameApp.HEIGHT);
        scene.setFill(Color.BLACK);

        // wrapper 寬高跟著 Scene 走
        wrapper.prefWidthProperty().bind(scene.widthProperty());
        wrapper.prefHeightProperty().bind(scene.heightProperty());

        // 監聽視窗大小，更新縮放
        scene.widthProperty().addListener((obs, o, w) ->
                applyTransform(w.doubleValue(), scene.getHeight()));
        scene.heightProperty().addListener((obs, o, h) ->
                applyTransform(scene.getWidth(), h.doubleValue()));

        primaryStage.setScene(scene);
    }

    /** 切換場景內容：只換 Group 裡的子節點，Scene/Stage 完全不動 */
    private static void setContent(Scene builtScene) {
        Node root = builtScene.getRoot();
        // 若 root 是 Region（VBox/StackPane 等），強制設成 960×640
        // 這樣 Group 的邊界才正確，置中計算才不會偏
        if (root instanceof javafx.scene.layout.Region r) {
            r.setPrefWidth(GameApp.WIDTH);
            r.setPrefHeight(GameApp.HEIGHT);
        }
        content.getChildren().setAll(root);
    }

    private static void applyTransform(double w, double h) {
        double s = Math.min(w / GameApp.WIDTH, h / GameApp.HEIGHT);
        scaleT.setX(s);
        scaleT.setY(s);
        transT.setX((w - GameApp.WIDTH  * s) / 2.0);
        transT.setY((h - GameApp.HEIGHT * s) / 2.0);
    }

    public static void showMainMenu() {
        setContent(new MainMenuScene().build());
    }

    public static void showTalentSelect() {
        setContent(new TalentSelectScene().build());
    }

    public static void showGame() {
        GameState.reset();
        setContent(new GameScene().build());
    }

    public static void showGameOver() {
        setContent(new GameOverScene().build());
    }

    public static void showVictory() {
        setContent(new VictoryScene().build());
    }
}
