package view.gui.screens;

import controller.TableSnapshot;
import javafx.animation.Animation;
import javafx.animation.ScaleTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import view.gui.CardNode;
import view.gui.Ui;

/** One opponent: avatar, name, mini card fan, count and a UNO! tag. */
final class SeatNode extends VBox {
    SeatNode(TableSnapshot.Seat seat, boolean active) {
        super(6);
        setAlignment(Pos.CENTER);
        getStyleClass().add("seat");
        if (active) getStyleClass().add("active");

        String initial = seat.name().isEmpty() ? "?" : seat.name().substring(0, 1).toUpperCase();
        StackPane avatar = new StackPane(Ui.label(initial, "avatar-initial"));
        avatar.getStyleClass().addAll("avatar", seat.bot() ? "bot" : "human");
        if (active) {
            ScaleTransition pulse = new ScaleTransition(Duration.millis(650), avatar);
            pulse.setToX(1.12); pulse.setToY(1.12);
            pulse.setAutoReverse(true);
            pulse.setCycleCount(Animation.INDEFINITE);
            pulse.play();
            sceneProperty().addListener((o, a, b) -> { if (b == null) pulse.stop(); });
        }

        HBox minis = new HBox(-14);
        minis.setAlignment(Pos.CENTER);
        for (int i = 0; i < Math.min(seat.cards(), 7); i++) minis.getChildren().add(CardNode.back(24));

        String cards = seat.cards() + (seat.cards() == 1 ? " card" : " cards");
        getChildren().addAll(avatar, Ui.label(seat.name(), "seat-name"), minis, Ui.label(cards + "  \u00B7  " + seat.score() + " pts", "count-chip"));
        if (seat.cards() == 1) getChildren().add(Ui.label("UNO!", "uno-tag"));
    }
}
