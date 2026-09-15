package engine.dto;

import java.util.List;

public class EventStateSummary {
    private final int id;
    private final String name;
    private final List<OptionInfo> options;
    private final double accountBalance;
    private final double totalCommissionCollected;
    private final List<TradeRecordInfo> historyMostRecentFirst;
    private final String status;
    private final String winningOptionName;

    public EventStateSummary(int id, String name, List<OptionInfo> options, double accountBalance,
                              double totalCommissionCollected, List<TradeRecordInfo> historyMostRecentFirst,
                              String status, String winningOptionName) {
        this.id = id;
        this.name = name;
        this.options = options;
        this.accountBalance = accountBalance;
        this.totalCommissionCollected = totalCommissionCollected;
        this.historyMostRecentFirst = historyMostRecentFirst;
        this.status = status;
        this.winningOptionName = winningOptionName;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<OptionInfo> getOptions() {
        return options;
    }

    public double getAccountBalance() {
        return accountBalance;
    }

    public double getTotalCommissionCollected() {
        return totalCommissionCollected;
    }

    public List<TradeRecordInfo> getHistoryMostRecentFirst() {
        return historyMostRecentFirst;
    }

    public String getStatus() {
        return status;
    }

    public String getWinningOptionName() {
        return winningOptionName;
    }
}
