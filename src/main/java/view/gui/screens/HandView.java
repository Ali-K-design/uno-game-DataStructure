package view.gui.screens;

import javafx.scene.layout.Pane;
import view.gui.CardNode;

/** Lays cards out as a fan: overlap adapts to the card count, outer cards rotate and sink. */
final class HandView extends Pane {
    private CardNode[] cards = new CardNode[0];
    private final double cardWidth;

    HandView(double cardWidth) {
        this.cardWidth = cardWidth;
        setMinHeight(cardWidth * CardNode.RATIO + 70);
        setPrefHeight(cardWidth * CardNode.RATIO + 70);
    }

    void setCards(CardNode[] newCards) {
        getChildren().clear();
        cards = newCards;
        getChildren().addAll(newCards);
        requestLayout();
    }

    @Override
    protected void layoutChildren() {
        int n = cards.length;
        if (n == 0) return;
        double avail = getWidth() - 60;
        double step = n == 1 ? 0 : Math.min(cardWidth * 0.80, (avail - cardWidth) / (n - 1));
        double total = step * (n - 1) + cardWidth;
        double x0 = (getWidth() - total) / 2;
        double mid = (n - 1) / 2.0;
        double angleStep = Math.min(4.0, 26.0 / Math.max(1, n));
        for (int i = 0; i < n; i++) {
            double off = i - mid;
            cards[i].relocate(x0 + i * step, 46 + off * off * 1.4);
            cards[i].setRotate(off * angleStep);
        }
    }
}
