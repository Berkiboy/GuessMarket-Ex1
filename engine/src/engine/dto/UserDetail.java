package engine.dto;

import java.util.List;

public class UserDetail {
    private final String name;
    private final double balance;
    private final boolean blocked;
    private final List<UserEventParticipation> participations;

    public UserDetail(String name, double balance, boolean blocked, List<UserEventParticipation> participations) {
        this.name = name;
        this.balance = balance;
        this.blocked = blocked;
        this.participations = participations;
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

    public List<UserEventParticipation> getParticipations() {
        return participations;
    }
}
