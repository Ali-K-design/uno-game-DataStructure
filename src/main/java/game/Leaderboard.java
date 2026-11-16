package game;

import dataStructures.DynamicArray;
import model.Account;

public class Leaderboard {
    private DynamicArray<Account> accounts;
    public Leaderboard() {

        accounts = new DynamicArray<>();
    }

    public void addAccount(Account account) {

        accounts.add(account);
    }

    public void sortByScore() {

        for (int i = 0; i < accounts.getSize() - 1; i++) {
            for (int j = 0; j < accounts.getSize() - i - 1; j++) {
                Account first = accounts.get(j);
                Account second = accounts.get(j + 1);
                if (first.getTotalScore() < second.getTotalScore()) {
                    accounts.swap(j, j + 1);
                }
            }
        }
    }

    public Account getRankedAccount(int index) {

        return accounts.get(index);
    }

    public int size() {

        return accounts.getSize();
    }
}
