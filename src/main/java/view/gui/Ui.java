package view.gui;

import javafx.animation.Animation;
import javafx.animation.TranslateTransition;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import model.Card;
import model.CardType;
import model.Color;

/** Tiny helpers so the screens stay readable. */
public final class Ui {
    private Ui() { }

    public static Label label(String text, String... styleClasses) {
        Label l = new Label(text);
        l.getStyleClass().addAll(styleClasses);
        return l;
    }

    public static Button button(String text, String styleClass, Runnable action) {
        Button b = new Button(text);
        if (styleClass != null) b.getStyleClass().add(styleClass);
        b.setOnAction(e -> action.run());
        return b;
    }

    public static void floatLoop(javafx.scene.Node n, double dy, double millis) {
        TranslateTransition t = new TranslateTransition(Duration.millis(millis), n);
        t.setFromY(0); t.setToY(dy);
        t.setAutoReverse(true);
        t.setCycleCount(Animation.INDEFINITE);
        t.play();
    }

    /** Three floating cards used as decoration on the auth + menu screens. */
    public static StackPane heroCards() {
        CardNode a = CardNode.of(new Card(Color.RED, CardType.NUMBER, 7), 170);
        CardNode b = CardNode.of(new Card(Color.NONE, CardType.WILD_DRAW_FOUR, 0), 170);
        CardNode c = CardNode.of(new Card(Color.BLUE, CardType.DRAW_TWO, 0), 170);
        a.setRotate(-16); a.setTranslateX(-120);
        c.setRotate(16);  c.setTranslateX(120);
        floatLoop(a, -14, 2400); floatLoop(b, -20, 2900); floatLoop(c, -12, 2100);
        StackPane hero = new StackPane(a, c, b);
        hero.setPickOnBounds(false);
        return hero;
    }
}
