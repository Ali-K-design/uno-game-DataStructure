package game;

import dataStructures.CustomStack;
import dataStructures.DynamicArray;
import dataStructures.Node;
import dataStructures.TurnManager;
import model.Card;
import model.CardType;
import model.Color;
import model.Player;
import model.RoundState;

import java.util.Random;

/** One hand of UNO, from the deal until a player's hand is empty. */
public class Round {
    private static final Color[] REAL_COLORS = {Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW};

    private final Deck deck;
    private final DiscardPile discardPile = new DiscardPile();
    private final TurnManager turnManager = new TurnManager();
    private final RuleManager ruleManager = new RuleManager();
    private final CustomStack<Integer> penaltyStack = new CustomStack<>();
    private final DrawMachine drawMachine;
    private final Random random;

    private Color activeColor = Color.NONE;
    private Player winner;
    private Card drawnCard;              // set after a draw that produced a playable card
    private int pendingPenalty;          // running total of penaltyStack (for display)
    private CardType penaltyTopType;     // type of the latest +2/+4 in the chain

    public Round() { this(new Random()); }

    public Round(Random random) {
        this.random = random;
        this.deck = new Deck(random);
        this.drawMachine = new DrawMachine(random);
    }

    public void start(DynamicArray<Player> players) {
        deck.createDeck();
        for (int i = 0; i < players.getSize(); i++) {
            players.get(i).clearHand();
            turnManager.addPlayer(players.get(i));
        }
        deck.dealCards(players, 7, discardPile);
        Card first = deck.drawCard(discardPile);
        while (first != null && first.getCardType() == CardType.WILD_DRAW_FOUR) {
            deck.putBackAndShuffle(first);
            first = deck.drawCard(discardPile);
        }
        discardPile.addCard(first);
        activeColor = first.getColor();
        applyFirstCard(first);
    }

    private void applyFirstCard(Card first) {
        switch (first.getCardType()) {
            case WILD:
                activeColor = REAL_COLORS[random.nextInt(REAL_COLORS.length)];
                break;
            case SKIP:
                turnManager.advanceTurn(1);
                break;
            case REVERSE:
                turnManager.handleReverseCard();
                break;
            case DRAW_TWO:
                addPenalty(2, CardType.DRAW_TWO);
                break;
            default:
                break;
        }
    }

    private void addPenalty(int amount, CardType type) {
        penaltyStack.push(amount);
        pendingPenalty += amount;
        penaltyTopType = type;
    }

    // ------------------------------------------------------------------ moves
    /** Why a play would be refused, or null when it is legal. */
    public String validatePlay(Player player, Card card, Color chosenColor) {
        if (isFinished()) return "The round is already over.";
        if (player != turnManager.getCurrentPlayer()) return "It is not your turn.";
        if (card == null) return "There is no such card.";
        if (drawnCard != null && card != drawnCard) return "Play the card you just drew, or pass.";
        if (!ruleManager.isValidPlay(card, discardPile.getTopCard(), activeColor, player.getHand(), penaltyStack)) {
            if (pendingPenalty > 0) return "A penalty is pending: stack a +2/+4 or draw.";
            if (card.getCardType() == CardType.WILD_DRAW_FOUR) return "+4 is only allowed when you hold no card of the active color.";
            return "That card matches neither the active color nor the top card.";
        }
        boolean wild = card.getCardType() == CardType.WILD || card.getCardType() == CardType.WILD_DRAW_FOUR;
        if (wild && (chosenColor == null || chosenColor == Color.NONE)) return "Choose a color for the wild card.";
        return null;
    }

    /** Plays the card and applies its effect. Returns null on success, otherwise the reason it was refused. */
    public String tryPlay(Player player, Card card, Color chosenColor) {
        String error = validatePlay(player, card, chosenColor);
        if (error != null) return error;
        if (!player.removeCard(card)) return "You do not hold that card.";
        discardPile.addCard(card);
        drawnCard = null;
        boolean wild = card.getCardType() == CardType.WILD || card.getCardType() == CardType.WILD_DRAW_FOUR;
        activeColor = wild ? chosenColor : card.getColor();
        if (player.getHand().isEmpty()) {
            winner = player;
            return null;
        }
        switch (card.getCardType()) {
            case SKIP:
                turnManager.skipTurn();
                break;
            case REVERSE:
                turnManager.handleReverseCard();
                break;
            case DRAW_TWO:
                addPenalty(2, CardType.DRAW_TWO);
                turnManager.advanceTurn(1);
                break;
            case WILD_DRAW_FOUR:
                addPenalty(4, CardType.WILD_DRAW_FOUR);
                turnManager.advanceTurn(1);
                break;
            default:
                turnManager.advanceTurn(1);
                break;
        }
        return null;
    }

    public boolean playCard(Player player, Card card, Color chosenColor) {
        return tryPlay(player, card, chosenColor) == null;
    }

    /**
     * Normal draw (no penalty pending). If the drawn card is playable the turn stays open
     * (see getDrawnCard); otherwise the turn passes. Returns the drawn card, or null if no card was left.
     */
    public Card drawOne(Player player) {
        Card card = deck.drawCard(discardPile);
        if (card == null) {
            turnManager.advanceTurn(1);
            return null;
        }
        player.addCard(card);
        if (ruleManager.isValidPlay(card, discardPile.getTopCard(), activeColor, player.getHand(), penaltyStack)) {
            drawnCard = card;
        } else {
            turnManager.advanceTurn(1);
        }
        return card;
    }

    /** Declines to play the just-drawn card. */
    public void pass() {
        drawnCard = null;
        turnManager.advanceTurn(1);
    }

    /** Takes the whole pending penalty (recursive drain) and ends the turn. Returns how many cards were drawn. */
    public int takePenalty(Player player) {
        int total = sumPenalty(penaltyStack);
        pendingPenalty = 0;
        drawCards(player, total);
        turnManager.advanceTurn(1);
        return total;
    }

    /** Lucky draw: discards the pending penalty and draws the machine's random count instead. */
    public int useDrawMachine(Player player) {
        int count = drawMachine.roll(penaltyTopType == CardType.WILD_DRAW_FOUR);
        sumPenalty(penaltyStack);
        pendingPenalty = 0;
        drawCards(player, count);
        turnManager.advanceTurn(1);
        return count;
    }

    /** Recursive: pops the entire stack and returns the sum (base case: empty stack = 0). */
    private int sumPenalty(CustomStack<Integer> stack) {
        if (stack.isEmpty()) return 0;
        return stack.pop() + sumPenalty(stack);
    }

    private void drawCards(Player player, int count) {
        for (int i = 0; i < count; i++) {
            Card card = deck.drawCard(discardPile);
            if (card != null) player.addCard(card);
        }
    }

    // ------------------------------------------------------------------ cheats (do not advance the turn)
    public void cheatGive(Player player, Card card) { player.addCard(card); }

    public void cheatTop(Card card) {
        discardPile.removeTopCard();
        discardPile.addCard(card);          // activeColor intentionally untouched
    }

    public void cheatEmptyDeck() { deck.reduceToOne(); }

    public void cheatWinRound(Player player) {
        while (player.getHand().getSize() > 1) {
            player.getHand().removeAt(0);
        }
        drawnCard = null;
        winner = player;
    }

    // ------------------------------------------------------------------ queries
    public RoundState stateFor(Player player) {
        boolean[] playable = new boolean[player.getHand().getSize()];
        int i = 0;
        for (Node<Card> n = player.getHand().getHead(); n != null; n = n.getNext(), i++) {
            playable[i] = isPlayable(player, n.getData());
        }
        return new RoundState(discardPile.getTopCard(), activeColor, pendingPenalty, playable,
                player == turnManager.getCurrentPlayer() ? drawnCard : null);
    }

    public boolean isPlayable(Player player, Card card) {
        return validatePlay(player, card, REAL_COLORS[0]) == null;
    }

    public Player getWinner() { return winner; }
    public boolean isFinished() { return winner != null; }
    public Player getCurrentPlayer() { return turnManager.getCurrentPlayer(); }
    public Card getTopCard() { return discardPile.getTopCard(); }
    public Color getActiveColor() { return activeColor; }
    public Card getDrawnCard() { return drawnCard; }
    public int getPendingPenalty() { return pendingPenalty; }
    public int getDeckSize() { return deck.getSize(); }
    public int getDiscardSize() { return discardPile.size(); }
    public boolean isClockwise() { return turnManager.isClockwise(); }
}
