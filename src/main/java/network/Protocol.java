package network;

import controller.AppActions;
import controller.TableSnapshot;
import model.Card;
import model.CardType;
import model.Color;

public final class Protocol {
    public static final int VERSION = 1;
    public static final int MAX_LINE_CHARS = 64 * 1024;
    public static final int DEFAULT_PORT = 5555;

    private Protocol() { }

    public static JsonObject encodeCard(Card card) {
        if (card == null) return null;
        return new JsonObject().put("color", card.getColor().name()).put("type", card.getCardType().name()).put("value", card.getValue());
    }

    public static Card decodeCard(JsonObject o) {
        if (o == null) return null;
        try {
            return new Card(Color.valueOf(o.getString("color")), CardType.valueOf(o.getString("type")), o.getInt("value", 0));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new JsonException("Invalid card");
        }
    }

    public static JsonObject encodeSnapshot(TableSnapshot s) {
        JsonArray seats = new JsonArray();
        for (TableSnapshot.Seat seat : s.seats()) {
            seats.add(new JsonObject().put("name", seat.name()).put("cards", seat.cards()).put("score", seat.score())
                    .put("bot", seat.bot()).put("eliminated", seat.eliminated()));
        }
        JsonArray hand = new JsonArray();
        for (Card c : s.hand()) hand.add(encodeCard(c));
        JsonObject o = new JsonObject()
                .put("roundTitle", s.roundTitle()).put("winCondition", s.winCondition()).put("clockwise", s.clockwise())
                .put("seats", seats).put("currentSeat", s.currentSeat()).put("deckCount", s.deckCount())
                .put("topCard", encodeCard(s.topCard())).put("activeColor", s.activeColor().name())
                .put("pendingPenalty", s.pendingPenalty()).put("hand", hand)
                .put("playable", JsonArray.ofBooleans(s.playable())).put("drawnCardPending", s.drawnCardPending())
                .put("log", JsonArray.ofStrings(s.log())).put("variant", s.variant()).put("mercyLimit", s.mercyLimit());
        TableSnapshot.RoundSummary r = s.summary();
        if (r != null) {
            o.put("summary", new JsonObject().put("winner", r.winner()).put("names", JsonArray.ofStrings(r.names()))
                    .put("leftovers", JsonArray.ofStrings(r.leftovers())).put("points", JsonArray.ofInts(r.points()))
                    .put("gained", r.gained()).put("gameOver", r.gameOver()).put("champion", r.champion()));
        } else {
            o.put("summary", null);
        }
        return o;
    }

    public static TableSnapshot decodeSnapshot(JsonObject o) {
        try {
            JsonArray seatArray = require(o.getArray("seats"), "seats");
            TableSnapshot.Seat[] seats = new TableSnapshot.Seat[seatArray.size()];
            for (int i = 0; i < seats.length; i++) {
                JsonObject so = require(seatArray.getObject(i), "seat");
                seats[i] = new TableSnapshot.Seat(so.getString("name"), so.getInt("cards", 0), so.getInt("score", 0),
                        so.getBool("bot", false), so.getBool("eliminated", false));
            }
            JsonArray handArray = require(o.getArray("hand"), "hand");
            Card[] hand = new Card[handArray.size()];
            for (int i = 0; i < hand.length; i++) hand[i] = decodeCard(handArray.getObject(i));
            TableSnapshot.RoundSummary summary = null;
            JsonObject so = o.getObject("summary");
            if (so != null) {
                summary = new TableSnapshot.RoundSummary(so.getString("winner"), require(so.getArray("names"), "names").toStringArray(),
                        require(so.getArray("leftovers"), "leftovers").toStringArray(), require(so.getArray("points"), "points").toIntArray(),
                        so.getInt("gained", 0), so.getBool("gameOver", false), so.getString("champion"));
            }
            if (seats.length == 0) throw new JsonException("No seats");
            return new TableSnapshot(o.getString("roundTitle"), o.getString("winCondition"), o.getBool("clockwise", true),
                    seats, o.getInt("currentSeat", 0), o.getInt("deckCount", 0), decodeCard(o.getObject("topCard")),
                    Color.valueOf(o.getString("activeColor")), o.getInt("pendingPenalty", 0), hand,
                    require(o.getArray("playable"), "playable").toBooleanArray(), o.getBool("drawnCardPending", false),
                    require(o.getArray("log"), "log").toStringArray(), summary, o.getString("variant"), o.getInt("mercyLimit", 0));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new JsonException("Invalid snapshot: " + e.getMessage());
        }
    }

    private static <T> T require(T value, String what) {
        if (value == null) throw new JsonException("Missing " + what);
        return value;
    }

    public static JsonObject encodeRoom(AppActions.RoomInfo r) {
        return new JsonObject().put("id", r.id()).put("host", r.host()).put("humans", r.humans()).put("joined", r.joined())
                .put("bots", r.bots()).put("mode", r.mode()).put("limit", r.limit()).put("variant", r.variant());
    }

    public static AppActions.RoomInfo decodeRoom(JsonObject o) {
        return new AppActions.RoomInfo(o.getInt("id", 0), o.getString("host"), o.getInt("humans", 0), o.getInt("joined", 0),
                o.getInt("bots", 0), o.getString("mode"), o.getInt("limit", 0), o.getString("variant"));
    }

    public static JsonObject encodeRow(AppActions.LeaderboardRow r) {
        return new JsonObject().put("rank", r.rank()).put("displayName", r.displayName()).put("username", r.username())
                .put("totalScore", r.totalScore()).put("games", r.games()).put("wins", r.wins());
    }

    public static AppActions.LeaderboardRow decodeRow(JsonObject o) {
        return new AppActions.LeaderboardRow(o.getInt("rank", 0), o.getString("displayName"), o.getString("username"),
                o.getInt("totalScore", 0), o.getInt("games", 0), o.getInt("wins", 0));
    }

    public static JsonObject push(String type) { return new JsonObject().put("push", type); }

    public static JsonObject toast(String text) { return push("toast").put("text", text); }
}
