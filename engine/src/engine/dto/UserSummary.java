package engine.dto;

public class UserSummary {
    private final String name;
    private final double balance;
    private final boolean blocked;
    private final boolean marketMaker;
    private final boolean online;

    public UserSummary(String name, double balance, boolean blocked, boolean marketMaker, boolean online) {
        this.name = name;
        this.balance = balance;
        this.blocked = blocked;
        this.marketMaker = marketMaker;
        this.online = online;
    }

    public String getName() {
        return name;
    }

    public double getBalance() {
        return balance;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public boolean isMarketMaker() {
        return marketMaker;
    }

    public boolean isOnline() {
        return online;
    }
}
