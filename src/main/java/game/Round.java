package game;

import dataStructures.CustomStack;
import dataStructures.DynamicArray;
import dataStructures.TurnManager;
import model.Card;
import model.CardType;
import model.Color;
import model.Player;

public class Round {
    private Deck deck;
    private DiscardPile discardPile;
    private TurnManager turnManager;
    private RuleManager ruleManager;
    private CustomStack<Integer> penaltyStack;
    private Color activeColor;
    private Player winner;

    public Round() {

        deck = new Deck();
        discardPile = new DiscardPile();
        turnManager = new TurnManager();
        ruleManager = new RuleManager();
        penaltyStack = new CustomStack<>();
        activeColor = Color.NONE;
        winner = null;
    }

    public void start(DynamicArray<Player> players) {

        deck.createDeck();
        for (int i = 0; i < players.getSize(); i++) {
            turnManager.addPlayer(players.get(i));
        }
        deck.dealCards(players, 7, discardPile);
        Card firstCard = deck.drawCard(discardPile);
        if (firstCard != null) {
            discardPile.addCard(firstCard);
            activeColor = firstCard.getColor();
        }
    }

    public boolean playCard(Player player, Card card, Color chosenColor) {

            if (player != turnManager.getCurrentPlayer()) {
                return false;
            }
            if (!ruleManager.isValidPlay(card, discardPile.getTopCard(), activeColor, player.getHand(), penaltyStack)) {
                return false;
            }
            if ((card.getCardType() == CardType.WILD || card.getCardType() == CardType.WILD_DRAW_FOUR) && (chosenColor == null || chosenColor == Color.NONE)) {
                return false;
            }
            if (!player.removeCard(card)) {
                return false;
            }
            discardPile.addCard(card);
            if (card.getCardType() == CardType.WILD || card.getCardType() == CardType.WILD_DRAW_FOUR) {
                activeColor = chosenColor;
            } else {
                activeColor = card.getColor();
            }
            if (player.getHand().isEmpty()) {
                winner = player;
                return true;
            }
            switch (card.getCardType()) {
                case SKIP:
                    turnManager.skipTurn();
                    break;
                case REVERSE:
                    turnManager.handleReverseCard();
                    break;
                case DRAW_TWO:
                    penaltyStack.push(2);
                    turnManager.advanceTurn(1);
                    break;
                case WILD_DRAW_FOUR:
                    penaltyStack.push(4);
                    turnManager.advanceTurn(1);
                    break;
                default:
                    turnManager.advanceTurn(1);
                    break;
            }
            return true;
        }
    public void drawPenalty() {

        if (penaltyStack.isEmpty()) {
            return;
        }
        int totalPenalty = 0;
        while (!penaltyStack.isEmpty()) {
            totalPenalty += penaltyStack.pop();
        }
        Player currentPlayer = turnManager.getCurrentPlayer();
        for (int i = 0; i < totalPenalty; i++) {
            Card card = deck.drawCard(discardPile);
            if (card != null) {
                currentPlayer.addCard(card);
            }
        }
        turnManager.advanceTurn(1);
    }
    public Player getWinner() {

        return winner;
    }

    public Player getCurrentPlayer() {

        return turnManager.getCurrentPlayer();
    }

    public Card getTopCard() {

        return discardPile.getTopCard();
    }

    public Color getActiveColor() {

        return activeColor;
    }

    public boolean isFinished() {

        return winner != null;
    }
}

