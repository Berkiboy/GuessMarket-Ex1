package engine.model;

import java.io.Serializable;

public class User implements Serializable {

    private final String name;
    private final Account account = new Account();
    private boolean blocked;

    public User(String name, double initialCash) {
        this.name = name;
        account.deposit(initialCash);
    }

    public String getName() {
        return name;
    }

    public Account getAccount() {
        return account;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void blockIfNegative() {
        if (account.getBalance() < 0) {
            blocked = true;
        }
    }
}
