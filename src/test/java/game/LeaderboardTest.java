package game;

import model.Account;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LeaderboardTest {
    private Account account(String name, int score, int wins) {
        Account a = new Account(name, "pass", name);
        a.setTotalScore(score);
        a.setWins(wins);
        return a;
    }

    @Test
    void sortsByScoreThenWins() {
        Leaderboard lb = new Leaderboard();
        lb.addAccount(account("low", 10, 5));
        lb.addAccount(account("tieFewWins", 100, 1));
        lb.addAccount(account("top", 300, 0));
        lb.addAccount(account("tieMoreWins", 100, 4));
        lb.sortByScore();
        assertEquals("top", lb.getRankedAccount(0).getUsername());
        assertEquals("tieMoreWins", lb.getRankedAccount(1).getUsername());
        assertEquals("tieFewWins", lb.getRankedAccount(2).getUsername());
        assertEquals("low", lb.getRankedAccount(3).getUsername());
    }

    @Test
    void handlesEmptyAndSingleElement() {
        Leaderboard lb = new Leaderboard();
        lb.sortByScore();
        assertEquals(0, lb.size());
        lb.addAccount(account("only", 1, 0));
        lb.sortByScore();
        assertEquals("only", lb.getRankedAccount(0).getUsername());
    }

    @Test
    void sortsALargeBoardCorrectly() {
        Leaderboard lb = new Leaderboard();
        java.util.Random random = new java.util.Random(5);
        for (int i = 0; i < 1500; i++) lb.addAccount(account("u" + i, random.nextInt(500), random.nextInt(20)));
        lb.sortByScore();
        for (int i = 1; i < lb.size(); i++) {
            Account a = lb.getRankedAccount(i - 1), b = lb.getRankedAccount(i);
            assertTrue(a.getTotalScore() > b.getTotalScore() || (a.getTotalScore() == b.getTotalScore() && a.getWins() >= b.getWins()));
        }
    }

    @Test
    void registrationRules() {
        Leaderboard lb = new Leaderboard();
        AccountManager m = new AccountManager(lb);
        assertNull(m.register("ali", "1234", "Ali"));
        assertNotNull(m.register("ali", "9999", "Other"));      // duplicate
        assertNotNull(m.register("a b", "1234", "X"));           // space
        assertNotNull(m.register("bob", "123", "Bob"));          // short password
        assertNotNull(m.authenticate("ali", "1234"));
        assertNull(m.authenticate("ali", "nope"));
    }
}
