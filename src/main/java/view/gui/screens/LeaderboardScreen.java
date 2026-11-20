package view.gui.screens;

import controller.AppActions;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import view.gui.Ui;

public final class LeaderboardScreen extends StackPane {
    public LeaderboardScreen(AppActions app, Runnable onBack) {
        getStyleClass().add("screen");
        VBox rows = new VBox(10);
        AppActions.LeaderboardRow[] data = app.leaderboard();
        rows.getChildren().add(row("#", "PLAYER", "USERNAME", "SCORE", "GAMES", "WINS", "table-head", null));
        if (data.length == 0) rows.getChildren().add(Ui.label("No players yet.", "muted"));
        for (int i = 0; i < data.length; i++) {
            AppActions.LeaderboardRow r = data[i];
            String medal = i == 0 ? "gold" : i == 1 ? "silver" : i == 2 ? "bronze" : null;
            HBox row = row(String.valueOf(r.rank()), r.displayName(), "@" + r.username(),
                    String.valueOf(r.totalScore()), String.valueOf(r.games()), String.valueOf(r.wins()), null, medal);
            rows.getChildren().add(row);
        }
        ScrollPane scroll = new ScrollPane(rows);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        HBox head = new HBox(16, Ui.button("\u2190 Back", null, onBack), Ui.label("Leaderboard", "h1"));
        head.setAlignment(Pos.CENTER_LEFT);
        VBox box = new VBox(22, head, scroll);
        box.setPadding(new Insets(40, 120, 40, 120));
        VBox.setVgrow(scroll, Priority.ALWAYS);
        getChildren().add(box);
    }

    private static HBox row(String rank, String name, String user, String score, String games, String wins, String textClass, String medal) {
        String[] cells = {rank, name, user, score, games, wins};
        double[] widths = {60, 260, 200, 120, 100, 100};
        HBox row = new HBox();
        if (textClass == null) { row.getStyleClass().add("lb-row"); if (medal != null) row.getStyleClass().add(medal); }
        row.setAlignment(Pos.CENTER_LEFT);
        for (int i = 0; i < cells.length; i++) {
            javafx.scene.control.Label l = Ui.label(cells[i], i == 0 && textClass == null ? "lb-rank" : (textClass == null ? "label" : textClass));
            l.setMinWidth(widths[i]);
            if (i == 2 && textClass == null) l.getStyleClass().add("muted");
            row.getChildren().add(l);
        }
        return row;
    }
}
