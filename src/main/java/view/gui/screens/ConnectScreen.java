package view.gui.screens;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import network.Protocol;
import network.RemoteApp;
import view.gui.Router;
import view.gui.Ui;

import java.io.IOException;

public final class ConnectScreen extends StackPane {
    public ConnectScreen(Router router, String message) {
        getStyleClass().add("screen");

        VBox hero = new VBox(26, Ui.heroCards(), Ui.label("UNO", "brand-logo"),
                Ui.label("Play with friends over the network, or alone against bots.", "muted"));
        hero.setAlignment(Pos.CENTER);
        hero.setPadding(new Insets(40));
        HBox.setHgrow(hero, Priority.ALWAYS);

        TextField host = new TextField("localhost");
        TextField port = new TextField(String.valueOf(Protocol.DEFAULT_PORT));
        Label error = Ui.label(message == null ? "" : message, "form-error");
        error.setWrapText(true);
        Button connect = Ui.button("Connect", "btn-primary", () -> { });
        connect.setMaxWidth(Double.MAX_VALUE);
        Button offline = Ui.button("Play offline", null, router::showAuth);
        offline.setMaxWidth(Double.MAX_VALUE);

        Runnable go = () -> {
            int portNumber;
            try {
                portNumber = Integer.parseInt(port.getText().trim());
            } catch (NumberFormatException e) {
                error.setText("The port must be a number.");
                return;
            }
            final String hostName = host.getText().trim();
            connect.setDisable(true);
            error.setText("Connecting...");
            Thread worker = new Thread(() -> {
                try {
                    RemoteApp app = RemoteApp.connect(hostName, portNumber, Platform::runLater);
                    app.getClient().setOnDisconnect(() -> router.showConnect("The connection to the server was lost."));
                    Platform.runLater(() -> {
                        router.setApp(app);
                        router.showAuth();
                    });
                } catch (IOException e) {
                    Platform.runLater(() -> {
                        error.setText("Could not connect: " + e.getMessage());
                        connect.setDisable(false);
                    });
                }
            }, "uno-connect");
            worker.setDaemon(true);
            worker.start();
        };
        connect.setOnAction(e -> go.run());
        port.setOnAction(e -> go.run());

        VBox form = new VBox(14, Ui.label("Online play", "h1"),
                new VBox(6, Ui.label("SERVER ADDRESS", "field-label"), host),
                new VBox(6, Ui.label("PORT", "field-label"), port),
                error, connect, Ui.label("or", "muted"), offline);
        form.getStyleClass().add("glass");
        form.setPadding(new Insets(34));
        form.setPrefWidth(400); form.setMaxWidth(400); form.setMaxHeight(Region.USE_PREF_SIZE);
        form.setAlignment(Pos.CENTER);

        HBox root = new HBox(hero, form);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(0, 70, 0, 0));
        getChildren().add(root);
    }
}
