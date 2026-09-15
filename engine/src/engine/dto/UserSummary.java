package engine.dto;

public class UserSummary {
    private final String name;
    private final double balance;
    private final boolean blocked;

    public UserSummary(String name, double balance, boolean blocked) {
        this.name = name;
        this.balance = balance;
        this.blocked = blocked;
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
}
