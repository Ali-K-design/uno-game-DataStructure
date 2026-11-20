package game;

import dataStructures.DynamicArray;
import model.Account;

public class Leaderboard {
    private DynamicArray<Account> accounts = new DynamicArray<>();

    public void addAccount(Account account) { accounts.add(account); }

    public Account findByUsername(String username) {
        for (int i = 0; i < accounts.getSize(); i++) {
            if (accounts.get(i).getUsername().equals(username)) return accounts.get(i);
        }
        return null;
    }

    /** Recursive merge sort, O(U log U): higher total score first, then more wins. Stable. */
    public void sortByScore() {
        accounts = mergeSort(accounts);
    }

    private DynamicArray<Account> mergeSort(DynamicArray<Account> list) {
        if (list.getSize() <= 1) return list;
        int middle = list.getSize() / 2;
        DynamicArray<Account> left = mergeSort(slice(list, 0, middle, new DynamicArray<>()));
        DynamicArray<Account> right = mergeSort(slice(list, middle, list.getSize(), new DynamicArray<>()));
        return merge(left, right, 0, 0, new DynamicArray<>());
    }

    private DynamicArray<Account> slice(DynamicArray<Account> source, int from, int to, DynamicArray<Account> out) {
        if (from >= to) return out;
        out.add(source.get(from));
        return slice(source, from + 1, to, out);
    }

    private DynamicArray<Account> merge(DynamicArray<Account> left, DynamicArray<Account> right,
                                        int i, int j, DynamicArray<Account> out) {
        if (i == left.getSize() && j == right.getSize()) return out;
        boolean takeLeft = j == right.getSize() || (i < left.getSize() && ranksBeforeOrEqual(left.get(i), right.get(j)));
        if (takeLeft) {
            out.add(left.get(i));
            return merge(left, right, i + 1, j, out);
        }
        out.add(right.get(j));
        return merge(left, right, i, j + 1, out);
    }

    private boolean ranksBeforeOrEqual(Account a, Account b) {
        if (a.getTotalScore() != b.getTotalScore()) return a.getTotalScore() > b.getTotalScore();
        return a.getWins() >= b.getWins();
    }

    public Account getRankedAccount(int index) { return accounts.get(index); }
    public int size() { return accounts.getSize(); }
}
