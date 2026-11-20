package view.gui.screens;

import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

/** Places opponents on the upper arc of an ellipse; the hub (piles) sits in the middle. */
final class Arena extends Pane {
    private final Region felt = new Region();
    private final Node hub;
    private Node[] seats = new Node[0];

    Arena(Node hub) {
        this.hub = hub;
        getStyleClass().add("arena");
        felt.getStyleClass().add("felt");
        getChildren().addAll(felt, hub);
    }

    void setSeats(Node[] newSeats) {
        getChildren().removeAll(seats);
        seats = newSeats;
        getChildren().addAll(newSeats);
        requestLayout();
    }

    @Override
    protected void layoutChildren() {
        double w = getWidth(), h = getHeight();
        double cx = w / 2, cy = h * 0.60;
        double rx = Math.max(200, w / 2 - 110), ry = Math.max(120, cy - 70);
        double fw = rx * 1.85, fh = Math.min(ry * 1.85, (h - cy - 6) * 2);
        felt.resizeRelocate(cx - fw / 2, cy - fh / 2, fw, fh);
        place(hub, cx, cy);
        int n = seats.length;
        for (int i = 0; i < n; i++) {
            double t = (i + 1.0) / (n + 1.0);
            double a = Math.PI + t * Math.PI;            // left -> top -> right (clockwise from the viewer)
            place(seats[i], cx + rx * Math.cos(a), cy + ry * Math.sin(a));
        }
    }

    private static void place(Node node, double x, double y) {
        double pw = node.prefWidth(-1), ph = node.prefHeight(-1);
        node.resize(pw, ph);
        node.relocate(x - pw / 2, y - ph / 2);
    }
}
