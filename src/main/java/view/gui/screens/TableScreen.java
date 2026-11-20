package view.gui.screens;

import controller.GameActions;
import controller.TableSnapshot;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.util.Duration;
import model.Card;
import model.CardType;
import model.Color;
import view.GameView;
import view.gui.CardNode;
import view.gui.Ui;

import java.util.function.Consumer;

/** The game table. Pure View: renders TableSnapshot, forwards clicks to GameActions. */
public final class TableScreen extends StackPane implements GameView {
    private static final double HAND_W = 112, PILE_W = 104;

    private GameActions actions;
    private TableSnapshot last;
    private boolean summaryShown;
    private Card lastTop;

    private final Runnable onExit;
    private final Label roundChip = Ui.label("", "chip", "chip-accent"), winChip = Ui.label("", "chip");
    private final Label dirLabel = Ui.label("CLOCKWISE", "chip");
    private final Label turnChip = Ui.label("", "chip");
    private final Label deckCount = Ui.label("", "pile-count");
    private final Label colorLabel = Ui.label("", "h2");
    private final Label penalty = Ui.label("", "penalty-badge");
    private final Region ring = new Region(), colorDot = new Region();
    private final StackPane drawPile = new StackPane(), discard = new StackPane();
    private final Arc spinner = new Arc(0, 0, 14, 14, 0, 300);
    private RotateTransition spin;
    private final HandView hand = new HandView(HAND_W);
    private final Arena arena;
    private final Button drawBtn = Ui.button("Draw", "btn-draw", () -> { });
    private final Button playDrawnBtn = Ui.button("Play drawn card", "btn-primary", () -> { });
    private final Button passBtn = Ui.button("Pass", null, () -> { });
    private final Button luckyBtn = Ui.button("Lucky draw", null, () -> { });
    private final VBox logBox = new VBox(5);
    private final VBox logPanel = new VBox(10, Ui.label("MOVE LOG", "table-head"));
    private final StackPane overlay = new StackPane();
    private final Label toast = Ui.label("", "toast");
    private final HBox debugBar = new HBox(10);

    public TableScreen(Runnable onExit) {
        this.onExit = onExit;
        getStyleClass().add("screen");

        // --- hub (piles + active colour)
        ring.getStyleClass().add("felt-ring");
        ring.setMinSize(340, 340); ring.setPrefSize(340, 340); ring.setMaxSize(340, 340);
        for (int i = 0; i < 3; i++) {
            CardNode b = CardNode.back(PILE_W);
            b.setTranslateX(i * 2); b.setTranslateY(-i * 2);
            drawPile.getChildren().add(b);
        }
        drawPile.getChildren().add(deckCount);
        StackPane.setAlignment(deckCount, Pos.BOTTOM_CENTER);
        StackPane.setMargin(deckCount, new Insets(0, 0, 8, 0));
        drawPile.setCursor(Cursor.HAND);
        drawPile.setOnMouseClicked(e -> { if (actions != null && last != null && last.myTurn()) actions.draw(); });
        HBox piles = new HBox(36, drawPile, discard);
        piles.setAlignment(Pos.CENTER);
        colorDot.getStyleClass().add("color-dot");
        HBox colorRow = new HBox(8, colorDot, Ui.label("ACTIVE COLOR", "field-label"), colorLabel);
        colorRow.setAlignment(Pos.CENTER);
        VBox hubCol = new VBox(16, piles, colorRow, penalty);
        hubCol.setAlignment(Pos.CENTER);
        arena = new Arena(new StackPane(ring, hubCol));

        // --- top bar
        spinner.setType(ArcType.OPEN);
        spinner.getStyleClass().add("spinner");
        spin = new RotateTransition(Duration.seconds(3), spinner);
        spin.setByAngle(360); spin.setInterpolator(Interpolator.LINEAR); spin.setCycleCount(Animation.INDEFINITE);
        spin.play();
        StackPane dir = new StackPane(spinner);
        dir.setMinSize(36, 36);
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        HBox top = new HBox(12, Ui.label("UNO", "h2"), roundChip, winChip, sp, dir, dirLabel,
                Ui.button("Scores", null, this::showScores),
                Ui.button("Log", null, () -> { boolean v = !logPanel.isVisible(); logPanel.setVisible(v); logPanel.setManaged(v); }),
                Ui.button("Quit", "btn-danger", () -> { if (actions != null) actions.quit(); onExit.run(); }));
        top.setAlignment(Pos.CENTER_LEFT);
        top.getStyleClass().add("topbar");
        top.setPadding(new Insets(12, 24, 12, 24));

        // --- bottom
        drawBtn.setOnAction(e -> { if (actions != null) actions.draw(); });
        passBtn.setOnAction(e -> { if (actions != null) actions.pass(); });
        playDrawnBtn.setOnAction(e -> {
            if (last == null) return;
            Card drawn = last.hand()[last.hand().length - 1];
            if (isWild(drawn)) pickColor(c -> actions.playDrawn(c)); else actions.playDrawn(null);
        });
        luckyBtn.setOnAction(e -> { if (actions != null) actions.drawMachine(); });
        HBox actionBar = new HBox(14, turnChip, drawBtn, luckyBtn, playDrawnBtn, passBtn);
        actionBar.setAlignment(Pos.CENTER);
        TextField cheat = new TextField();
        cheat.setPromptText("cheat_give -t NUMBER -c RED -v 7   |   cheat_top   |   cheat_empty_deck   |   cheat_win_round");
        HBox.setHgrow(cheat, Priority.ALWAYS);
        cheat.setOnAction(e -> { if (actions != null) actions.cheat(cheat.getText().trim()); cheat.clear(); });
        debugBar.getChildren().addAll(Ui.label("DEBUG", "chip"), cheat);
        debugBar.setPadding(new Insets(0, 40, 0, 40));
        debugBar.setVisible(false); debugBar.setManaged(false);
        VBox bottom = new VBox(8, debugBar, actionBar, hand);
        bottom.setPadding(new Insets(0, 0, 0, 0));

        // --- log panel
        logPanel.getStyleClass().add("log-panel");
        logPanel.setPrefWidth(250); logPanel.setMinWidth(250);
        logPanel.getChildren().add(logBox);

        BorderPane layout = new BorderPane();
        layout.setTop(top); layout.setCenter(arena); layout.setBottom(bottom); layout.setRight(logPanel);

        toast.setMouseTransparent(true);
        toast.setOpacity(0);
        StackPane.setAlignment(toast, Pos.TOP_CENTER);
        StackPane.setMargin(toast, new Insets(96, 0, 0, 0));
        overlay.setPickOnBounds(false);
        getChildren().addAll(layout, toast, overlay);

        // F9 toggles the cheat bar (needed for the in-person evaluation)
        sceneProperty().addListener((o, oldS, newS) -> {
            if (newS != null) newS.addEventFilter(KeyEvent.KEY_PRESSED, ev -> {
                if (ev.getCode() == KeyCode.F9) { boolean v = !debugBar.isVisible(); debugBar.setVisible(v); debugBar.setManaged(v); }
            });
        });
    }

    public void bind(GameActions actions) { this.actions = actions; }

    // ------------------------------------------------------------------ GameView
    @Override
    public void render(TableSnapshot s) {
        last = s;
        roundChip.setText(s.roundTitle());
        winChip.setText(s.winCondition());
        dirLabel.setText(s.clockwise() ? "CLOCKWISE" : "COUNTER-CLOCKWISE");
        spin.setByAngle(s.clockwise() ? 360 : -360);
        spin.play();

        // seats
        Node[] seatNodes = new Node[s.seats().length - 1];
        for (int i = 1; i < s.seats().length; i++) seatNodes[i - 1] = new SeatNode(s.seats()[i], s.currentSeat() == i);
        arena.setSeats(seatNodes);

        // piles + colour
        deckCount.setText(s.deckCount() + " left");
        CardNode topNode = CardNode.of(s.topCard(), PILE_W);
        CardNode g1 = CardNode.back(PILE_W), g2 = CardNode.back(PILE_W);
        g1.setRotate(-9); g2.setRotate(7); g1.setOpacity(0.55); g2.setOpacity(0.7);
        discard.getChildren().setAll(g1, g2, topNode);
        if (s.topCard() != lastTop) topNode.popIn();
        lastTop = s.topCard();
        String c = s.activeColor().name().toLowerCase();
        ring.getStyleClass().removeAll("red", "blue", "green", "yellow", "none");
        ring.getStyleClass().add(c);
        colorDot.getStyleClass().removeAll("red", "blue", "green", "yellow", "none");
        colorDot.getStyleClass().add(c);
        colorLabel.setText(s.activeColor().name());
        boolean pen = s.pendingPenalty() > 0;
        penalty.setText("+" + s.pendingPenalty() + " PENALTY");
        penalty.setVisible(pen); penalty.setManaged(pen);

        // actions
        boolean mine = s.myTurn();
        turnChip.getStyleClass().removeAll("chip-turn");
        if (mine) turnChip.getStyleClass().add("chip-turn");
        turnChip.setText(mine ? "YOUR TURN" : "Waiting for " + s.seats()[s.currentSeat()].name() + "\u2026");
        drawBtn.setText(pen ? "Take +" + s.pendingPenalty() : "Draw");
        drawBtn.setDisable(!mine || s.drawnCardPending());
        luckyBtn.setVisible(mine && pen); luckyBtn.setManaged(mine && pen);
        playDrawnBtn.setVisible(mine && s.drawnCardPending()); playDrawnBtn.setManaged(mine && s.drawnCardPending());
        passBtn.setVisible(mine && s.drawnCardPending());      passBtn.setManaged(mine && s.drawnCardPending());

        // hand
        CardNode[] nodes = new CardNode[s.hand().length];
        for (int i = 0; i < nodes.length; i++) {
            final int idx = i;
            CardNode n = CardNode.of(s.hand()[i], HAND_W);
            boolean ok = mine && s.playable()[i];
            n.setPlayable(ok);
            n.enableHover();
            n.setOnMouseClicked(e -> onCardClicked(idx, n, ok));
            nodes[i] = n;
        }
        hand.setCards(nodes);

        // log (newest first)
        logBox.getChildren().clear();
        for (int i = s.log().length - 1, k = 0; i >= 0 && k < 14; i--, k++)
            logBox.getChildren().add(Ui.label(s.log()[i], k == 0 ? "log-line-new" : "log-line"));

        if (s.summary() != null && !summaryShown) { summaryShown = true; showSummary(s.summary()); }
        if (s.summary() == null) summaryShown = false;
    }

    @Override
    public void toast(String message) {
        toast.setText(message);
        FadeTransition in = new FadeTransition(Duration.millis(180), toast); in.setToValue(1);
        PauseTransition hold = new PauseTransition(Duration.millis(1300));
        FadeTransition out = new FadeTransition(Duration.millis(350), toast); out.setToValue(0);
        new SequentialTransition(in, hold, out).play();
    }

    // ------------------------------------------------------------------ interaction
    private void onCardClicked(int index, CardNode node, boolean playable) {
        if (actions == null) return;
        if (!playable) { node.shake(); return; }
        if (isWild(node.getCard())) pickColor(color -> actions.play(index, color));
        else actions.play(index, null);
    }

    private static boolean isWild(Card c) {
        return c.getCardType() == CardType.WILD || c.getCardType() == CardType.WILD_DRAW_FOUR;
    }

    private void pickColor(Consumer<Color> onPicked) {
        HBox row = new HBox(18);
        row.setAlignment(Pos.CENTER);
        Color[] colors = {Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW};
        for (Color col : colors) {
            Button b = new Button(col.name());
            b.getStyleClass().addAll("button", "color-pick", col.name().toLowerCase());
            b.setOnAction(e -> { overlay.getChildren().clear(); onPicked.accept(col); });
            row.getChildren().add(b);
        }
        VBox panel = new VBox(24, Ui.label("Choose a color", "h1"), row,
                Ui.button("Cancel", null, () -> overlay.getChildren().clear()));
        panel.setAlignment(Pos.CENTER);
        showOverlay(panel, true);
    }

    private void showScores() {
        if (last == null) return;
        VBox panel = new VBox(10, Ui.label("Scores", "h1"));
        for (TableSnapshot.Seat seat : last.seats())
            panel.getChildren().add(Ui.label(seat.name() + "   \u2014   " + seat.score() + " pts", "h2"));
        panel.getChildren().add(Ui.button("Close", null, () -> overlay.getChildren().clear()));
        panel.setAlignment(Pos.CENTER);
        showOverlay(panel, true);
    }

    private void showSummary(TableSnapshot.RoundSummary r) {
        GridPane grid = new GridPane();
        grid.setHgap(36); grid.setVgap(8);
        grid.addRow(0, Ui.label("PLAYER", "table-head"), Ui.label("REMAINING CARDS", "table-head"), Ui.label("POINTS", "table-head"));
        for (int i = 0; i < r.names().length; i++)
            grid.addRow(i + 1, Ui.label(r.names()[i], "h2"), Ui.label(r.leftovers()[i], "muted"), Ui.label(String.valueOf(r.points()[i]), "h2"));
        String title = r.gameOver() ? r.champion() + " wins the game!" : r.winner() + " wins the round";
        Button next = Ui.button(r.gameOver() ? "Back to menu" : "Next round", "btn-primary", () -> {
            overlay.getChildren().clear();
            if (r.gameOver()) { if (actions != null) actions.quit(); onExit.run(); }
            else if (actions != null) actions.nextRound();
        });
        VBox panel = new VBox(20, Ui.label(title, "victory"), grid, Ui.label("+" + r.gained() + " points for the winner", "h2"), next);
        panel.setAlignment(Pos.CENTER);
        showOverlay(panel, false);
    }

    private void showOverlay(VBox content, boolean closeOnScrimClick) {
        content.getStyleClass().add("glass");
        content.setPadding(new Insets(36, 48, 36, 48));
        content.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane scrim = new StackPane(content);
        scrim.getStyleClass().add("overlay-scrim");
        if (closeOnScrimClick) scrim.setOnMouseClicked(e -> { if (e.getTarget() == scrim) overlay.getChildren().clear(); });
        scrim.setOpacity(0);
        overlay.getChildren().setAll(scrim);
        FadeTransition ft = new FadeTransition(Duration.millis(200), scrim);
        ft.setToValue(1);
        ft.play();
    }
}
