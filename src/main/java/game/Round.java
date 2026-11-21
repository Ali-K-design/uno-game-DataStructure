package game;

import dataStructures.CustomStack;
import dataStructures.DynamicArray;
import dataStructures.MonotoneStack;
import dataStructures.Node;
import dataStructures.SinglyLinkedList;
import dataStructures.TurnManager;
import model.Card;
import model.CardType;
import model.Color;
import model.Player;
import model.RoundState;

import java.util.Random;

public class Round {
    private static final Color[] REAL_COLORS = {Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW};

    private final RuleSet rules;
    private final Deck deck;
    private final DiscardPile discardPile = new DiscardPile();
    private final TurnManager turnManager = new TurnManager();
    private final MonotoneStack penaltyStack;
    private final DrawMachine drawMachine;
    private final Random random;

    private DynamicArray<String> events = new DynamicArray<>();
    private DynamicArray<Player> players;
    private Color activeColor = Color.NONE;
    private Player winner;
    private Card drawnCard;
    private int pendingPenalty;
    private CardType penaltyTopType;
    private int eliminationBonus;

    public Round() { this(new ClassicRules(), new Random()); }

    public Round(Random random) { this(new ClassicRules(), random); }

    public Round(RuleSet rules) { this(rules, new Random()); }

    public Round(RuleSet rules, Random random) {
        this.rules = rules;
        this.random = random;
        this.deck = new Deck(random);
        this.drawMachine = new DrawMachine(random);
        this.penaltyStack = new MonotoneStack(rules.strictPenaltyStack());
    }

    public void start(DynamicArray<Player> players) {
        this.players = players;
        deck.createDeck(rules.buildCards());
        for (int i = 0; i < players.getSize(); i++) {
            players.get(i).clearHand();
            players.get(i).setEliminated(false);
            turnManager.addPlayer(players.get(i));
        }
        deck.dealCards(players, 7, discardPile);
        Card first = deck.drawCard(discardPile);
        while (first != null && first.getCardType().isWildFamily() && first.getCardType().penaltyValue() > 0) {
            deck.putBackAndShuffle(first);
            first = deck.drawCard(discardPile);
        }
        discardPile.addCard(first);
        activeColor = first.getColor();
        applyFirstCard(first);
    }

    private void applyFirstCard(Card first) {
        CardType type = first.getCardType();
        if (type.isWildFamily()) {
            activeColor = REAL_COLORS[random.nextInt(REAL_COLORS.length)];
            return;
        }
        switch (type) {
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

    public String validateCard(Player player, Card card) {
        if (isFinished()) return "The round is already over.";
        if (player.isEliminated()) return "You have been eliminated.";
        if (player != turnManager.getCurrentPlayer()) return "It is not your turn.";
        if (card == null) return "There is no such card.";
        if (drawnCard != null && card != drawnCard) return "Play the card you just drew, or pass.";
        if (!rules.isValidPlay(card, discardPile.getTopCard(), activeColor, player.getHand(), penaltyStack)) {
            if (pendingPenalty > 0) return rules.penaltyHint();
            if (card.getCardType() == CardType.WILD_DRAW_FOUR) return "+4 is only allowed when you hold no card of the active color.";
            return "That card matches neither the active color nor the top card.";
        }
        return null;
    }

    public String validatePlay(Player player, Card card, Color chosenColor, Player target) {
        String error = validateCard(player, card);
        if (error != null) return error;
        if (rules.needsColorChoice(card) && (chosenColor == null || chosenColor == Color.NONE)) {
            return "Choose a color for the wild card.";
        }
        if (rules.needsTarget(card)) {
            if (target == null || target == player) return "Choose another player to swap hands with.";
            if (target.isEliminated()) return "That player is already eliminated.";
        }
        return null;
    }

    public String validatePlay(Player player, Card card, Color chosenColor) {
        return validatePlay(player, card, chosenColor, null);
    }

    public boolean isPlayable(Player player, Card card) {
        return validateCard(player, card) == null;
    }

    public String tryPlay(Player player, Card card, Color chosenColor, Player target) {
        String error = validatePlay(player, card, chosenColor, target);
        if (error != null) return error;
        if (!player.removeCard(card)) return "You do not hold that card.";
        discardPile.addCard(card);
        drawnCard = null;
        activeColor = rules.needsColorChoice(card) ? chosenColor : card.getColor();
        if (player.getHand().isEmpty()) {
            winner = player;
            return null;
        }
        rules.applyEffect(this, player, card, chosenColor, target);
        return null;
    }

    public String tryPlay(Player player, Card card, Color chosenColor) {
        return tryPlay(player, card, chosenColor, null);
    }

    public boolean playCard(Player player, Card card, Color chosenColor) {
        return tryPlay(player, card, chosenColor) == null;
    }

    public Card drawOne(Player player) {
        Card card;
        while (true) {
            card = deck.drawCard(discardPile);
            if (card == null) break;
            player.addCard(card);
            if (reachesLimit(player)) {
                enforceMercy();
                return card;
            }
            if (rules.isValidPlay(card, discardPile.getTopCard(), activeColor, player.getHand(), penaltyStack)) {
                drawnCard = card;
                return card;
            }
            if (!rules.drawUntilPlayable()) break;
        }
        turnManager.advanceTurn(1);
        return card;
    }

    public void pass() {
        drawnCard = null;
        turnManager.advanceTurn(1);
    }

    public int takePenalty(Player player) {
        int total = sumPenalty(penaltyStack);
        pendingPenalty = 0;
        drawCards(player, total);
        finishDrawTurn();
        return total;
    }

    public int useDrawMachine(Player player) {
        int count = drawMachine.roll(penaltyTopType == CardType.WILD_DRAW_FOUR);
        sumPenalty(penaltyStack);
        pendingPenalty = 0;
        drawCards(player, count);
        finishDrawTurn();
        return count;
    }

    private void finishDrawTurn() {
        boolean currentGone = enforceMercy();
        if (!currentGone && !isFinished()) turnManager.advanceTurn(1);
    }

    private int sumPenalty(CustomStack<Integer> stack) {
        if (stack.isEmpty()) return 0;
        return stack.pop() + sumPenalty(stack);
    }

    private void drawCards(Player player, int count) {
        for (int i = 0; i < count; i++) {
            Card card = deck.drawCard(discardPile);
            if (card == null) return;
            player.addCard(card);
            if (reachesLimit(player)) return;
        }
    }

    private boolean reachesLimit(Player player) {
        int limit = rules.eliminationLimit();
        return limit > 0 && player.getHand().getSize() >= limit;
    }

    boolean enforceMercy() {
        if (rules.eliminationLimit() <= 0 || winner != null) return false;
        Player current = turnManager.getCurrentPlayer();
        boolean currentGone = false;
        for (int i = 0; i < players.getSize() && winner == null; i++) {
            Player p = players.get(i);
            if (!p.isEliminated() && reachesLimit(p)) {
                if (p == current) currentGone = true;
                eliminate(p);
            }
        }
        return currentGone;
    }

    private void eliminate(Player player) {
        int count = player.getHand().getSize();
        eliminationBonus += ScoreManager.handPoints(player.getHand());
        deck.appendToEnd(player.releaseHand());
        player.setEliminated(true);
        if (player == turnManager.getCurrentPlayer()) drawnCard = null;
        turnManager.removePlayer(player);
        event(player.getName() + " reached " + count + " cards and is ELIMINATED. Their cards go to the bottom of the deck.");
        if (turnManager.getPlayerCount() == 1) {
            winner = turnManager.getCurrentPlayer();
        }
    }

    // ------------------------------------------------------------------ primitives used by RuleSets
    void advance(int steps) { turnManager.advanceTurn(steps); }
    void reverse() { turnManager.handleReverseCard(); }
    void skipNext() { turnManager.skipTurn(); }
    Player peekNextPlayer() { return turnManager.peekNextPlayer(); }
    int activePlayerCount() { return turnManager.getPlayerCount(); }
    void declareWinner(Player player) { winner = player; }
    void event(String text) { events.add(text); }

    void addPenalty(int amount, CardType type) {
        penaltyStack.push(amount);
        pendingPenalty += amount;
        penaltyTopType = type;
    }

    void swapHands(Player a, Player b) {
        a.swapHandWith(b);
        event(a.getName() + " swapped hands with " + b.getName() + ".");
    }

    void rotateHands() {
        DynamicArray<Player> order = turnManager.getPlayersInOrder();
        for (int i = 1; i < order.getSize(); i++) {
            order.get(0).swapHandWith(order.get(i));
        }
        event("All hands moved one seat in the direction of play.");
    }

    int drawUntilColor(Player victim, Color color) {
        int count = 0;
        while (true) {
            Card card = deck.drawCard(discardPile);
            if (card == null) break;
            victim.addCard(card);
            count++;
            if (card.getColor() == color || reachesLimit(victim)) break;
        }
        return count;
    }

    void slipUnderTop(SinglyLinkedList<Card> cards) {
        Card top = discardPile.removeTopCard();
        for (Node<Card> n = cards.getHead(); n != null; n = n.getNext()) {
            discardPile.addCard(n.getData());
        }
        discardPile.addCard(top);
    }

    public void cheatGive(Player player, Card card) { player.addCard(card); }

    public void cheatTop(Card card) {
        discardPile.removeTopCard();
        discardPile.addCard(card);
    }

    public void cheatEmptyDeck() { deck.reduceToOne(); }

    public void cheatWinRound(Player player) {
        while (player.getHand().getSize() > 1) {
            player.getHand().removeAt(0);
        }
        drawnCard = null;
        winner = player;
    }

    public boolean checkMercyAfterCheat() { return enforceMercy(); }

    public RoundState stateFor(Player player) {
        boolean[] playable = new boolean[player.getHand().getSize()];
        int i = 0;
        for (Node<Card> n = player.getHand().getHead(); n != null; n = n.getNext(), i++) {
            playable[i] = isPlayable(player, n.getData());
        }
        DynamicArray<Player> order = turnManager.getPlayersInOrder();
        Player[] opponents = new Player[Math.max(0, order.getSize() - 1)];
        int k = 0;
        for (int j = 0; j < order.getSize(); j++) {
            if (order.get(j) != player) opponents[k++] = order.get(j);
        }
        boolean sevenSwaps = rules.needsTarget(new Card(Color.RED, CardType.NUMBER, 7));
        return new RoundState(discardPile.getTopCard(), activeColor, pendingPenalty, playable,
                player == turnManager.getCurrentPlayer() ? drawnCard : null, opponents, sevenSwaps);
    }

    public String[] popEvents() {
        String[] out = new String[events.getSize()];
        for (int i = 0; i < out.length; i++) out[i] = events.get(i);
        events = new DynamicArray<>();
        return out;
    }

    public RuleSet getRules() { return rules; }
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
    public int getEliminationBonus() { return eliminationBonus; }
}
