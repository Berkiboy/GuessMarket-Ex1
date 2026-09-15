package engine.dto;

import java.util.List;

public class OrderBookEventStateSummary {
    private final int id;
    private final String name;
    private final double accountBalance;
    private final double totalCommissionCollected;
    private final String status;
    private final String winningOptionName;
    private final List<OrderBookOptionView> optionViews;
    private final List<ParticipantInfo> participants;

    public OrderBookEventStateSummary(int id, String name, double accountBalance, double totalCommissionCollected,
                                       String status, String winningOptionName, List<OrderBookOptionView> optionViews,
                                       List<ParticipantInfo> participants) {
        this.id = id;
        this.name = name;
        this.accountBalance = accountBalance;
        this.totalCommissionCollected = totalCommissionCollected;
        this.status = status;
        this.winningOptionName = winningOptionName;
        this.optionViews = optionViews;
        this.participants = participants;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getAccountBalance() {
        return accountBalance;
    }

    public double getTotalCommissionCollected() {
        return totalCommissionCollected;
    }

    public String getStatus() {
        return status;
    }

    public String getWinningOptionName() {
        return winningOptionName;
    }

    public List<OrderBookOptionView> getOptionViews() {
        return optionViews;
    }

    public List<ParticipantInfo> getParticipants() {
        return participants;
    }
}
