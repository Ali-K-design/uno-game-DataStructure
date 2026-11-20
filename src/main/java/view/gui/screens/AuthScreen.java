package view.gui.screens;

import controller.AppActions;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import view.gui.Ui;

public final class AuthScreen extends StackPane {
    public AuthScreen(AppActions app, Runnable onSuccess) {
        getStyleClass().add("screen");

        VBox hero = new VBox(26, Ui.heroCards(), Ui.label("UNO", "brand-logo"),
                Ui.label("The classic card game, powered by hand-made data structures.", "muted"));
        hero.setAlignment(Pos.CENTER);
        hero.setPadding(new Insets(40));
        HBox.setHgrow(hero, Priority.ALWAYS);

        ToggleGroup tabs = new ToggleGroup();
        ToggleButton loginTab = new ToggleButton("Login");
        ToggleButton regTab = new ToggleButton("Register");
        for (ToggleButton t : new ToggleButton[]{loginTab, regTab}) { t.getStyleClass().add("seg"); t.setToggleGroup(tabs); }
        loginTab.setSelected(true);

        TextField user = new TextField(); user.setPromptText("username");
        PasswordField pass = new PasswordField(); pass.setPromptText("password (min 4 chars)");
        TextField display = new TextField(); display.setPromptText("display name");
        VBox displayBox = new VBox(6, Ui.label("DISPLAY NAME", "field-label"), display);
        displayBox.setVisible(false); displayBox.setManaged(false);
        Label error = Ui.label("", "form-error");
        error.setWrapText(true);
        Button submit = Ui.button("Login", "btn-primary", () -> { });
        submit.setMaxWidth(Double.MAX_VALUE);

        tabs.selectedToggleProperty().addListener((o, a, b) -> {
            if (b == null) { a.setSelected(true); return; }
            boolean reg = b == regTab;
            displayBox.setVisible(reg); displayBox.setManaged(reg);
            submit.setText(reg ? "Create account" : "Login");
            error.setText("");
        });
        Runnable go = () -> {
            boolean reg = regTab.isSelected();
            String err = null;
            if (reg) err = app.register(user.getText().trim(), pass.getText(), display.getText().trim());
            if (err == null) err = app.login(user.getText().trim(), pass.getText());
            if (err == null) onSuccess.run(); else error.setText(err);
        };
        submit.setOnAction(e -> go.run());
        pass.setOnAction(e -> go.run());

        VBox form = new VBox(14, new HBox(8, loginTab, regTab),
                Ui.label("Welcome back", "h1"),
                new VBox(6, Ui.label("USERNAME", "field-label"), user),
                new VBox(6, Ui.label("PASSWORD", "field-label"), pass),
                displayBox, error, submit);
        form.getStyleClass().add("glass");
        form.setPadding(new Insets(34));
        form.setPrefWidth(400); form.setMaxWidth(400); form.setMaxHeight(Region.USE_PREF_SIZE);

        HBox root = new HBox(hero, form);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(0, 70, 0, 0));
        getChildren().add(root);
    }
}
