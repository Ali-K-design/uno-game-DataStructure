package view.gui;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.*;
import javafx.util.Duration;
import model.Card;
import model.CardType;

/** A UNO card drawn entirely with nodes + CSS (no images needed). */
public final class CardNode extends StackPane {
    public static final double RATIO = 1.5;
    private final Card card;
    private double restRotate;
    private Timeline running;

    private CardNode(Card card, double w) {
        this.card = card;
        double h = w * RATIO;
        setMinSize(w, h); setPrefSize(w, h); setMaxSize(w, h);
        getStyleClass().add("uno-card");
        double r = w * 0.13;
        setStyle("-fx-border-width:" + Math.max(3, w * 0.055) + ";-fx-background-radius:" + r + ";-fx-border-radius:" + r + ";");

        Region oval = new Region();
        oval.getStyleClass().add("card-oval");
        oval.setMinSize(w * 0.70, h * 0.74); oval.setPrefSize(w * 0.70, h * 0.74); oval.setMaxSize(w * 0.70, h * 0.74);
        oval.setStyle("-fx-background-radius:" + w + ";");
        oval.setRotate(-24);
        getChildren().add(oval);

        if (card == null) {
            getStyleClass().add("back");
            Label logo = new Label("UNO");
            logo.getStyleClass().add("back-logo");
            logo.setStyle("-fx-font-size:" + (w * 0.28) + "px;");
            logo.setRotate(-24);
            getChildren().add(logo);
            return;
        }
        boolean wild = card.getCardType() == CardType.WILD || card.getCardType() == CardType.WILD_DRAW_FOUR;
        getStyleClass().add(wild ? "wild" : card.getColor().name().toLowerCase());
        getChildren().add(buildCenter(card, w));

        String corner = cornerText(card);
        Label tl = cornerLabel(corner, w), br = cornerLabel(corner, w);
        br.setRotate(180);
        StackPane.setAlignment(tl, Pos.TOP_LEFT);     StackPane.setMargin(tl, new Insets(0, 0, 0, w * 0.02));
        StackPane.setAlignment(br, Pos.BOTTOM_RIGHT); StackPane.setMargin(br, new Insets(0, w * 0.02, 0, 0));
        getChildren().addAll(tl, br);
    }

    public static CardNode of(Card card, double width) { return new CardNode(card, width); }
    public static CardNode back(double width) { return new CardNode(null, width); }
    public Card getCard() { return card; }

    // ---------- pieces ----------
    private static Label cornerLabel(String text, double w) {
        Label l = new Label(text);
        l.getStyleClass().add("card-corner");
        l.setStyle("-fx-font-size:" + (w * 0.17) + "px;");
        return l;
    }

    private static String cornerText(Card c) {
        switch (c.getCardType()) {
            case NUMBER: return String.valueOf(c.getValue());
            case SKIP: return "\u2298";
            case REVERSE: return "\u21C4";
            case DRAW_TWO: return "+2";
            case WILD: return "W";
            default: return "+4";
        }
    }

    private static Node buildCenter(Card c, double w) {
        switch (c.getCardType()) {
            case NUMBER: return symbol(String.valueOf(c.getValue()), w * 0.46);
            case DRAW_TWO: return symbol("+2", w * 0.36);
            case SKIP: return skipIcon(w);
            case REVERSE: return reverseIcon(w);
            case WILD: return wildEmblem(w);
            default:
                Label four = symbol("+4", w * 0.30);
                four.getStyleClass().add("on-wild");
                return new StackPane(wildEmblem(w), four);
        }
    }

    private static Label symbol(String text, double size) {
        Label l = new Label(text);
        l.getStyleClass().add("card-symbol");
        l.setStyle("-fx-font-size:" + size + "px;");
        return l;
    }

    private static Node skipIcon(double w) {
        double r = w * 0.20;
        Circle ring = new Circle(r);
        ring.getStyleClass().add("icon-ring");
        ring.setStrokeWidth(w * 0.07);
        Line slash = new Line(-r * 0.7, r * 0.7, r * 0.7, -r * 0.7);
        slash.getStyleClass().add("icon-line");
        slash.setStrokeWidth(w * 0.07);
        return new Group(ring, slash);
    }

    private static Node reverseIcon(double w) {
        double s = w / 60.0;
        Polygon right = poly(s, 0, 0, 20, 0, 20, -7, 34, 6, 20, 19, 20, 12, 0, 12);
        Polygon left = poly(s, 34, 24, 14, 24, 14, 17, 0, 30, 14, 43, 14, 36, 34, 36);
        right.getStyleClass().add("icon-fill");
        left.getStyleClass().add("icon-fill");
        Group g = new Group(right, left);
        g.setRotate(-24);
        return g;
    }

    private static Polygon poly(double s, double... pts) {
        double[] p = new double[pts.length];
        for (int i = 0; i < pts.length; i++) p[i] = pts[i] * s;
        return new Polygon(p);
    }

    private static Node wildEmblem(double w) {
        String[] cls = {"red", "blue", "green", "yellow"};
        Group g = new Group();
        for (int i = 0; i < 4; i++) {
            Arc a = new Arc(0, 0, w * 0.27, w * 0.36, i * 90, 90);
            a.setType(ArcType.ROUND);
            a.getStyleClass().addAll("emblem", cls[i]);
            g.getChildren().add(a);
        }
        g.setRotate(-24);
        return g;
    }

    // ---------- state & animation ----------
    public void setPlayable(boolean playable) {
        getStyleClass().removeAll("playable", "dim");
        getStyleClass().add(playable ? "playable" : "dim");
    }

    public void enableHover() {
        setOnMouseEntered(e -> hover(true));
        setOnMouseExited(e -> hover(false));
    }

    private void hover(boolean on) {
        if (on) { restRotate = getRotate(); setViewOrder(-1); } else { setViewOrder(0); }
        Timeline t = new Timeline(new KeyFrame(Duration.millis(140),
                new KeyValue(translateYProperty(), on ? -42.0 : 0.0, Interpolator.EASE_OUT),
                new KeyValue(scaleXProperty(), on ? 1.10 : 1.0),
                new KeyValue(scaleYProperty(), on ? 1.10 : 1.0),
                new KeyValue(rotateProperty(), on ? 0.0 : restRotate)));
        if (running != null) running.stop();
        running = t;
        t.play();
    }

    public void popIn() {
        setScaleX(1.45); setScaleY(1.45); setOpacity(0);
        new Timeline(new KeyFrame(Duration.millis(260),
                new KeyValue(scaleXProperty(), 1.0, Interpolator.EASE_OUT),
                new KeyValue(scaleYProperty(), 1.0, Interpolator.EASE_OUT),
                new KeyValue(opacityProperty(), 1.0))).play();
    }

    public void shake() {
        Timeline t = new Timeline();
        double[] xs = {-9, 9, -6, 6, 0};
        for (int i = 0; i < xs.length; i++)
            t.getKeyFrames().add(new KeyFrame(Duration.millis(50 * (i + 1)), new KeyValue(translateXProperty(), xs[i])));
        t.play();
    }
}
