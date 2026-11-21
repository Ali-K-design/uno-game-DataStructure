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

    public MenuScreen(AppActions app, Router router) {
        getStyleClass().add("screen");

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
        Button start = Ui.button("Start game", "btn-primary", () -> { });
        start.setMaxWidth(Double.MAX_VALUE);
        start.disableProperty().bind(humans.add(bots).lessThan(2).or(humans.add(bots).greaterThan(10)));
        start.setOnAction(e -> {
            int extra = humans.get() - 1;
            String[] others = new String[extra];
            for (int i = 0; i < extra; i++) others[i] = nameFields[i].getText().trim();
            TableScreen table = new TableScreen(router::showMenu);
            try {
                GameActions ga = app.startGame(humans.get(), bots.get(), scoreMode ? "score" : "rounds", limit.get(), others,
                        noMercy ? "nomercy" : "classic", table);
                table.bind(ga);
                router.show(table);
            } catch (IllegalArgumentException ex) {
                hint.setText(ex.getMessage());
            }
        });

        rebuildLimit();
        rebuildNames();
        VBox panel = new VBox(18,
                Ui.label("New game", "h1"),
                field("HUMAN PLAYERS", stepper(humans, 1, 10)),
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

        HBox center = new HBox(80, panel, Ui.heroCards());
        center.setAlignment(Pos.CENTER);
        BorderPane root = new BorderPane();
        root.setTop(top); root.setCenter(center);
        root.setPadding(new Insets(28, 44, 28, 44));
        getChildren().add(root);
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
        for (int i = 0; i < humans.get() - 1; i++) {
            if (nameFields[i] == null) { nameFields[i] = new TextField(); }
            nameFields[i].setPromptText("username of human player " + (i + 2));
            namesBox.getChildren().add(nameFields[i]);
        }
    }
}
