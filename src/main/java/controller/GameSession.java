package controller;

import dataStructures.DynamicArray;
import dataStructures.Node;
import game.Game;
import game.Round;
import model.Action;
import model.Card;
import model.CardType;
import model.Color;
import model.Player;
import view.GameView;

/**
 * Runs one game for a View. Humans act through GameActions; bots are driven here
 * via Player.chooseAction. After every state change the View gets a fresh TableSnapshot.
 */
public class GameSession implements GameActions {
    private final Game game;
    private final GameView view;
    private final Scheduler scheduler;
    private final long botDelayMillis;
    private DynamicArray<String> log = new DynamicArray<>();
    private Player viewer;                       // whose hand the View shows (the human whose turn it is)
    private TableSnapshot.RoundSummary summary;
    private boolean closed;
    private int epoch;                           // invalidates bot tasks scheduled before a quit / new round

    public GameSession(Game game, GameView view, Scheduler scheduler, long botDelayMillis) {
        this.game = game;
        this.view = view;
        this.scheduler = scheduler;
        this.botDelayMillis = botDelayMillis;
        this.viewer = game.getPlayers().get(0);
        game.startRound();
        say("Round " + game.getRoundNumber() + " started. Top card: " + CardText.of(round().getTopCard()));
        publish();
    }

    private Round round() { return game.getCurrentRound(); }

    /** For tests in the same package. */
    Game getGame() { return game; }

    // ================================================================== GameActions
    @Override
    public void play(int cardIndex, Color chosenColor) {
        if (!isViewersTurn()) return;
        if (cardIndex < 0 || cardIndex >= viewer.getHand().getSize()) {
            view.toast("There is no card at index " + cardIndex + ".");
            return;
        }
        playCard(viewer, viewer.getHand().get(cardIndex), chosenColor);
    }

    @Override
    public void playDrawn(Color chosenColor) {
        if (!isViewersTurn()) return;
        Card drawn = round().getDrawnCard();
        if (drawn == null) {
            view.toast("You have no drawn card to play.");
            return;
        }
        playCard(viewer, drawn, chosenColor);
    }

    @Override
    public void draw() {
        if (!isViewersTurn()) return;
        Round r = round();
        if (r.getDrawnCard() != null) {
            view.toast("Play the card you drew, or pass.");
            return;
        }
        if (r.getPendingPenalty() > 0) {
            int count = r.takePenalty(viewer);
            say(viewer.getName() + " took the penalty (" + count + " cards).");
            publish("You drew " + count + " cards.");
            return;
        }
        Card card = r.drawOne(viewer);
        say(viewer.getName() + " drew a card.");
        if (card == null) publish("Both piles are empty; your turn is skipped.");
        else if (r.getDrawnCard() != null) publish("You drew " + CardText.of(card) + " - play it or pass.");
        else publish("You drew " + CardText.of(card) + " (not playable).");
    }

    @Override
    public void drawMachine() {
        if (!isViewersTurn()) return;
        Round r = round();
        if (r.getPendingPenalty() == 0 || r.getDrawnCard() != null) {
            view.toast("The lucky draw is only available while a penalty is pending.");
            return;
        }
        int pending = r.getPendingPenalty();
        int count = r.useDrawMachine(viewer);
        say(viewer.getName() + " tried the lucky draw against +" + pending + " and drew " + count + ".");
        publish("Lucky draw: you drew " + count + " card" + (count == 1 ? "" : "s") + ".");
    }

    @Override
    public void pass() {
        if (!isViewersTurn()) return;
        if (round().getDrawnCard() == null) {
            view.toast("You can only pass after drawing a playable card.");
            return;
        }
        round().pass();
        say(viewer.getName() + " passed.");
        publish();
    }

    @Override
    public void nextRound() {
        if (closed || summary == null || summary.gameOver()) return;
        summary = null;
        epoch++;
        log = new DynamicArray<>();
        game.startRound();
        say("Round " + game.getRoundNumber() + " started. Top card: " + CardText.of(round().getTopCard()));
        publish();
    }

    @Override
    public void quit() {
        closed = true;
        epoch++;
    }

    @Override
    public void cheat(String rawCommand) {
        if (!isViewersTurn()) return;
        String[] t = rawCommand == null ? new String[0] : rawCommand.trim().split("\\s+");
        if (t.length == 0 || t[0].isEmpty()) return;
        Round r = round();
        try {
            switch (t[0].toLowerCase()) {
                case "cheat_give":
                    r.cheatGive(viewer, parseCheatCard(t));
                    say("[cheat] card added to " + viewer.getName() + "'s hand.");
                    break;
                case "cheat_top":
                    r.cheatTop(parseCheatCard(t));
                    say("[cheat] top card replaced.");
                    break;
                case "cheat_empty_deck":
                    r.cheatEmptyDeck();
                    say("[cheat] deck reduced to 1 card.");
                    break;
                case "cheat_win_round":
                    r.cheatWinRound(viewer);
                    say("[cheat] " + viewer.getName() + " wins the round immediately.");
                    endRound();
                    return;
                default:
                    view.toast("Unknown cheat command: " + t[0]);
                    return;
            }
        } catch (IllegalArgumentException e) {
            view.toast(e.getMessage());
            return;
        }
        publish();                       // cheats never advance the turn
    }

    @Override
    public String scoreboardText() {
        DynamicArray<Player> players = game.getPlayers();
        StringBuilder sb = new StringBuilder("SCOREBOARD (rounds played: " + game.getRoundsPlayed() + ")\n");
        for (int i = 0; i < players.getSize(); i++) {
            Player p = players.get(i);
            sb.append(String.format("  %-14s total %5d   rounds: ", p.getName(), p.getCurrentGameScore()));
            Node<Integer> n = p.getRoundScores().getHead();
            while (n != null) {
                sb.append(n.getData()).append(n.getNext() == null ? "" : ", ");
                n = n.getNext();
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    // ================================================================== turn flow
    private boolean isViewersTurn() {
        if (closed || summary != null || round().isFinished()) return false;
        if (round().getCurrentPlayer() != viewer) {
            view.toast("It is not your turn.");
            return false;
        }
        return true;
    }

    private void playCard(Player player, Card card, Color chosenColor) {
        String error = round().tryPlay(player, card, chosenColor);
        if (error != null) {
            view.toast(error);                 // invalid move: no turn consumed
            return;
        }
        afterPlay(player, card, chosenColor);
    }

    private void afterPlay(Player player, Card card, Color chosenColor) {
        boolean wild = card.getCardType() == CardType.WILD || card.getCardType() == CardType.WILD_DRAW_FOUR;
        say(player.getName() + " played " + CardText.of(card) + (wild ? " and chose " + chosenColor : "") + ".");
        if (round().isFinished()) {
            endRound();
            return;
        }
        String next = round().getCurrentPlayer().getName();
        String message = null;
        switch (card.getCardType()) {
            case SKIP: message = "Skip!"; break;
            case REVERSE: message = round().isClockwise() ? "Direction: clockwise" : "Reverse!"; break;
            case DRAW_TWO: message = "+2 penalty for " + next; break;
            case WILD_DRAW_FOUR: message = "+4 penalty for " + next + " (color " + chosenColor + ")"; break;
            case WILD: message = "New color: " + chosenColor; break;
            default: break;
        }
        publish(message);
    }

    private void endRound() {
        Round r = round();
        int gained = game.finishRound();
        DynamicArray<Player> players = game.getPlayers();
        int n = players.getSize();
        String[] names = new String[n];
        String[] leftovers = new String[n];
        int[] points = new int[n];
        for (int i = 0; i < n; i++) {
            Player p = players.get(i);
            names[i] = p.getName();
            boolean won = p == r.getWinner();
            leftovers[i] = won ? "-" : handText(p);
            points[i] = won ? gained : 0;
            if (!won) points[i] = game.getScoreManager().calculateHandPoints(p.getHand());
        }
        boolean over = game.isGameFinished();
        String champion = null;
        if (over) {
            champion = game.getChampion().getName();
            game.finalizeGame();
        }
        summary = new TableSnapshot.RoundSummary(r.getWinner().getName(), names, leftovers, points, gained, over, champion);
        say(r.getWinner().getName() + " wins the round (+" + gained + ").");
        epoch++;
        view.render(snapshot());
    }

    private String handText(Player p) {
        StringBuilder sb = new StringBuilder();
        for (Node<Card> c = p.getHand().getHead(); c != null; c = c.getNext()) {
            sb.append(CardText.of(c.getData())).append(c.getNext() == null ? "" : ", ");
        }
        return sb.length() == 0 ? "-" : sb.toString();
    }

    // ================================================================== bots
    private void publish() { publish(null); }

    /** Renders the table, then shows the optional announcement, then lets the next bot move. */
    private void publish(String announcement) {
        Player current = round().getCurrentPlayer();
        if (!current.isBot()) viewer = current;
        view.render(snapshot());
        if (announcement != null) view.toast(announcement);
        if (!closed && summary == null && !round().isFinished() && current.isBot()) {
            final int myEpoch = epoch;
            scheduler.later(botDelayMillis, () -> {
                if (!closed && myEpoch == epoch) botTurn();
            });
        }
    }

    private void botTurn() {
        Round r = round();
        Player bot = r.getCurrentPlayer();
        if (r.isFinished() || !bot.isBot()) return;
        Action action = bot.chooseAction(r.stateFor(bot));
        if (action.getType() == Action.Type.PLAY) {
            String error = r.tryPlay(bot, action.getCard(), action.getColor());
            if (error == null) {
                afterPlay(bot, action.getCard(), action.getColor());
                return;
            }
        }
        if (r.getPendingPenalty() > 0) {
            int count = r.takePenalty(bot);
            say(bot.getName() + " took the penalty (" + count + " cards).");
            publish(bot.getName() + " draws " + count);
            return;
        }
        Card drawn = r.drawOne(bot);
        say(bot.getName() + " drew a card.");
        if (drawn != null && r.getDrawnCard() != null) {
            Action second = bot.chooseAction(r.stateFor(bot));
            if (r.tryPlay(bot, second.getCard(), second.getColor()) == null) {
                afterPlay(bot, second.getCard(), second.getColor());
                return;
            }
            r.pass();
        }
        publish();
    }

    // ================================================================== snapshot
    private TableSnapshot snapshot() {
        Round r = round();
        DynamicArray<Player> players = game.getPlayers();
        int n = players.getSize();
        int viewerIndex = indexOf(viewer);
        TableSnapshot.Seat[] seats = new TableSnapshot.Seat[n];
        for (int k = 0; k < n; k++) {
            Player p = players.get((viewerIndex + k) % n);
            seats[k] = new TableSnapshot.Seat(p.getName(), p.getHand().getSize(), p.getCurrentGameScore(), p.isBot());
        }
        int currentSeat = (indexOf(r.getCurrentPlayer()) - viewerIndex + n) % n;

        int handSize = viewer.getHand().getSize();
        Card[] hand = new Card[handSize];
        boolean[] playable = new boolean[handSize];
        int i = 0;
        for (Node<Card> c = viewer.getHand().getHead(); c != null; c = c.getNext(), i++) {
            hand[i] = c.getData();
            playable[i] = summary == null && r.isPlayable(viewer, c.getData());
        }
        String[] logLines = new String[log.getSize()];
        for (int k = 0; k < logLines.length; k++) logLines[k] = log.get(k);

        String win = game.getMode() == Game.Mode.SCORE
                ? "Win Condition: " + game.getLimit() + " Pts"
                : "Win Condition: " + game.getLimit() + " Rounds";
        boolean drawnPending = r.getDrawnCard() != null && r.getCurrentPlayer() == viewer;
        return new TableSnapshot("ROUND #" + game.getRoundNumber(), win, r.isClockwise(), seats, currentSeat,
                r.getDeckSize(), r.getTopCard(), r.getActiveColor(), r.getPendingPenalty(),
                hand, playable, drawnPending, logLines, summary);
    }

    private int indexOf(Player player) {
        DynamicArray<Player> players = game.getPlayers();
        for (int i = 0; i < players.getSize(); i++) {
            if (players.get(i) == player) return i;
        }
        return 0;
    }

    private void say(String line) { log.add(line); }

    // ================================================================== cheat parsing
    /** Parses "-t TYPE -c COLOR [-v 0-9]" into a Card, throwing IllegalArgumentException with a clear message. */
    private Card parseCheatCard(String[] tokens) {
        String type = null, color = null, value = null;
        for (int i = 1; i < tokens.length - 1; i++) {
            if (tokens[i].equals("-t")) type = tokens[i + 1];
            else if (tokens[i].equals("-c")) color = tokens[i + 1];
            else if (tokens[i].equals("-v")) value = tokens[i + 1];
        }
        if (type == null || color == null) throw new IllegalArgumentException("Usage: " + tokens[0] + " -t <type> -c <color> [-v <0-9>]");
        CardType cardType;
        Color cardColor;
        try {
            cardType = CardType.valueOf(type.toUpperCase());
            cardColor = Color.valueOf(color.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown card type or color.");
        }
        boolean wild = cardType == CardType.WILD || cardType == CardType.WILD_DRAW_FOUR;
        if (wild && cardColor != Color.NONE) throw new IllegalArgumentException("Wild cards must use -c NONE.");
        if (!wild && cardColor == Color.NONE) throw new IllegalArgumentException("This card needs a real color.");
        int number = 0;
        if (cardType == CardType.NUMBER) {
            try {
                number = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("NUMBER cards need -v <0-9>.");
            }
            if (number < 0 || number > 9) throw new IllegalArgumentException("Value must be between 0 and 9.");
        }
        return new Card(cardColor, cardType, number);
    }
}
