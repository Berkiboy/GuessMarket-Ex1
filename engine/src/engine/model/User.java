package engine.model;

public class User {

    private final String name;
    private final Account account = new Account();
    private boolean blocked;
    private boolean loggedIn;

    public User(String name) {
        this.name = name;
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

    public void unblockIfNonNegative() {
        if (account.getBalance() >= 0) {
            blocked = false;
        }
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }
}
