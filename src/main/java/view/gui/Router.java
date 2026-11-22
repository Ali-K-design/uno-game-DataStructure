package view.gui;

import controller.AppActions;
import javafx.animation.FadeTransition;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import view.gui.screens.*;

public final class Router {
    private final StackPane host = new StackPane();
    private AppActions app;

    public Router(Stage stage, AppActions app) {
        this.app = app;
        Scene scene = new Scene(host, 1280, 800);
        scene.getStylesheets().add(Router.class.getResource("/css/uno.css").toExternalForm());
        stage.setTitle("UNO");
        stage.setMinWidth(1100);
        stage.setMinHeight(720);
        stage.setScene(scene);
    }

    public void show(Region screen) {
        screen.setOpacity(0);
        host.getChildren().setAll(screen);
        FadeTransition ft = new FadeTransition(Duration.millis(260), screen);
        ft.setToValue(1);
        ft.play();
    }

    public void setApp(AppActions app) { this.app = app; }

    public void showConnect(String message) { show(new ConnectScreen(this, message)); }

    public void showAuth() { show(new AuthScreen(app, this::showMenu)); }
    public void showMenu() { show(new MenuScreen(app, this)); }
    public void showLeaderboard() { show(new LeaderboardScreen(app, this::showMenu)); }
}
