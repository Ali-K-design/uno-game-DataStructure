package view.gui.screens;

import controller.AppActions;
import controller.GameActions;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import view.gui.Router;
import view.gui.Ui;

public final class MenuScreen extends StackPane {
    private final IntegerProperty humans = new SimpleIntegerProperty(1);
    private final IntegerProperty bots = new SimpleIntegerProperty(3);
    private final IntegerProperty limit = new SimpleIntegerProperty(500);
    private final HBox limitBox = new HBox(8);
    private final VBox namesBox = new VBox(8);
    private final TextField[] nameFields = new TextField[10];
    private boolean scoreMode = true;
    private boolean noMercy;
    private boolean online;
    private final VBox roomsBox = new VBox(10);

    public MenuScreen(AppActions app, Router router) {
        getStyleClass().add("screen");
        online = app.isOnline();
        humans.set(online ? 2 : 1);

        Label avatar = Ui.label(app.currentDisplayName().substring(0, 1).toUpperCase(), "avatar-initial");
        StackPane av = new StackPane(avatar);
        av.getStyleClass().addAll("avatar", "human");
        HBox user = new HBox(12, av, Ui.label(app.currentDisplayName(), "h2"));
        user.setAlignment(Pos.CENTER_LEFT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox top = new HBox(12, user, spacer,
                Ui.button("Leaderboard", null, router::showLeaderboard),
                Ui.button("Logout", "btn-danger", () -> { app.logout(); router.showAuth(); }));
        top.setAlignment(Pos.CENTER_LEFT);

        ToggleGroup modeGroup = new ToggleGroup();
        ToggleButton scoreBtn = seg("Score target", modeGroup), roundsBtn = seg("Number of rounds", modeGroup);
        scoreBtn.setSelected(true);
        modeGroup.selectedToggleProperty().addListener((o, a, b) -> {
            if (b == null) { a.setSelected(true); return; }
            scoreMode = b == scoreBtn;
            limit.set(scoreMode ? 500 : 5);
            rebuildLimit();
        });
        humans.addListener((o, a, b) -> rebuildNames());

        ToggleGroup variantGroup = new ToggleGroup();
        ToggleButton classicBtn = seg("Classic", variantGroup), mercyBtn = seg("No Mercy", variantGroup);
        classicBtn.setSelected(true);
        Label variantHint = Ui.label("", "muted");
        variantHint.setWrapText(true);
        variantGroup.selectedToggleProperty().addListener((o, a, b) -> {
            if (b == null) { a.setSelected(true); return; }
            noMercy = b == mercyBtn;
            variantHint.setText(noMercy
                    ? "25+ cards = eliminated. Penalties stack only upward (+2 < +4 < +6 < +10). Skip Everyone, Discard All, Color Roulette, Wild Draw 6/10. 7 swaps hands, 0 rotates them. Draw until you can play."
                    : "");
        });

        Label hint = Ui.label("", "form-error");
        Button start = Ui.button(online ? "Host online game" : "Start game", "btn-primary", () -> { });
        start.setMaxWidth(Double.MAX_VALUE);
        start.disableProperty().bind(humans.add(bots).lessThan(2).or(humans.add(bots).greaterThan(10)));
        start.setOnAction(e -> {
            int extra = humans.get() - 1;
            String[] others = new String[extra];
            for (int i = 0; i < extra; i++) others[i] = nameFields[i].getText().trim();
            TableScreen table = new TableScreen(router::showMenu);
            try {
                String mode = scoreMode ? "score" : "rounds";
                String variant = noMercy ? "nomercy" : "classic";
                GameActions ga = online
                        ? app.hostRoom(humans.get(), bots.get(), mode, limit.get(), variant, table)
                        : app.startGame(humans.get(), bots.get(), mode, limit.get(), others, variant, table);
                table.bind(ga);
                router.show(table);
            } catch (IllegalArgumentException ex) {
                hint.setText(ex.getMessage());
            }
        });

        rebuildLimit();
        rebuildNames();
        VBox panel = new VBox(18,
                Ui.label(online ? "Host an online game" : "New game", "h1"),
                field(online ? "HUMAN PLAYERS (including you)" : "HUMAN PLAYERS", stepper(humans, online ? 2 : 1, 10)),
                field("BOTS", stepper(bots, 0, 9)),
                namesBox,
                field("VARIANT", new HBox(8, classicBtn, mercyBtn)),
                variantHint,
                field("GAME ENDS BY", new HBox(8, scoreBtn, roundsBtn)),
                field("LIMIT", limitBox),
                hint, start);
        panel.getStyleClass().add("glass");
        panel.setPadding(new Insets(32));
        panel.setPrefWidth(480); panel.setMaxWidth(480); panel.setMaxHeight(Region.USE_PREF_SIZE);

        javafx.scene.Node right = online ? buildRoomsPanel(app, router, hint) : Ui.heroCards();
        HBox center = new HBox(60, panel, right);
        center.setAlignment(Pos.CENTER);
        BorderPane root = new BorderPane();
        root.setTop(top); root.setCenter(center);
        root.setPadding(new Insets(28, 44, 28, 44));
        getChildren().add(root);
    }

    private VBox buildRoomsPanel(AppActions app, Router router, Label hint) {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox head = new HBox(12, Ui.label("Open rooms", "h1"), spacer, Ui.button("Refresh", null, () -> refreshRooms(app, router, hint)));
        head.setAlignment(Pos.CENTER_LEFT);
        ScrollPane scroll = new ScrollPane(roomsBox);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(420);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox panel = new VBox(18, head, scroll);
        panel.getStyleClass().add("glass");
        panel.setPadding(new Insets(32));
        panel.setPrefWidth(540); panel.setMaxWidth(540); panel.setMaxHeight(Region.USE_PREF_SIZE);
        refreshRooms(app, router, hint);
        return panel;
    }

    private void refreshRooms(AppActions app, Router router, Label hint) {
        roomsBox.getChildren().clear();
        AppActions.RoomInfo[] rooms;
        try {
            rooms = app.listRooms();
        } catch (IllegalArgumentException e) {
            hint.setText(e.getMessage());
            return;
        }
        if (rooms.length == 0) roomsBox.getChildren().add(Ui.label("No open rooms yet - host one!", "muted"));
        for (AppActions.RoomInfo r : rooms) {
            String rule = ("score".equals(r.mode()) ? r.limit() + " pts" : r.limit() + " rounds")
                    + ("nomercy".equals(r.variant()) ? "  \u00B7  NO MERCY" : "");
            VBox info = new VBox(2, Ui.label("Room " + r.id() + "  \u00B7  " + r.host(), "h2"),
                    Ui.label(r.joined() + "/" + r.humans() + " humans, " + r.bots() + " bots  \u00B7  " + rule, "muted"));
            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);
            final int roomId = r.id();
            HBox row = new HBox(12, info, sp, Ui.button("Join", "btn-primary", () -> joinRoom(app, router, hint, roomId)));
            row.getStyleClass().add("lb-row");
            row.setAlignment(Pos.CENTER_LEFT);
            roomsBox.getChildren().add(row);
        }
    }

    private void joinRoom(AppActions app, Router router, Label hint, int roomId) {
        TableScreen table = new TableScreen(router::showMenu);
        try {
            GameActions ga = app.joinRoom(roomId, table);
            table.bind(ga);
            router.show(table);
        } catch (IllegalArgumentException ex) {
            hint.setText(ex.getMessage());
            refreshRooms(app, router, hint);
        }
    }

    private static ToggleButton seg(String text, ToggleGroup g) {
        ToggleButton t = new ToggleButton(text);
        t.getStyleClass().add("seg");
        t.setToggleGroup(g);
        return t;
    }

    private static VBox field(String title, javafx.scene.Node content) {
        return new VBox(8, Ui.label(title, "field-label"), content);
    }

    private static HBox stepper(IntegerProperty value, int min, int max) {
        Button minus = Ui.button("\u2013", "stepper-btn", () -> { if (value.get() > min) value.set(value.get() - 1); });
        Button plus = Ui.button("+", "stepper-btn", () -> { if (value.get() < max) value.set(value.get() + 1); });
        Label v = new Label();
        v.getStyleClass().add("stepper-value");
        v.textProperty().bind(value.asString());
        HBox box = new HBox(12, minus, v, plus);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private void rebuildLimit() {
        int[] options = scoreMode ? new int[]{100, 500, 1000} : new int[]{3, 5, 10};
        ToggleGroup g = new ToggleGroup();
        limitBox.getChildren().clear();
        for (int o : options) {
            ToggleButton t = seg(String.valueOf(o), g);
            t.setSelected(o == limit.get());
            t.setOnAction(e -> limit.set(o));
            limitBox.getChildren().add(t);
        }
        g.selectedToggleProperty().addListener((obs, a, b) -> { if (b == null && a != null) a.setSelected(true); });
    }

    private void rebuildNames() {
        namesBox.getChildren().clear();
        if (online) return;
        for (int i = 0; i < humans.get() - 1; i++) {
            if (nameFields[i] == null) { nameFields[i] = new TextField(); }
            nameFields[i].setPromptText("username of human player " + (i + 2));
            namesBox.getChildren().add(nameFields[i]);
        }
    }
}
