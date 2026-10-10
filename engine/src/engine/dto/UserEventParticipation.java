package engine.dto;

import java.util.List;

public class UserEventParticipation {
    private final int eventId;
    private final String eventName;
    private final String tradingMethodType;
    private final String status;
    private final String winningOptionName;
    private final double totalCommissionPaid;
    private final List<TradeRecordInfo> lmsrHistory;
    private final List<OptionHoldingInfo> orderBookHoldings;
    private final List<OptionInfo> optionTotals;
    private final Double profitLoss;

    public UserEventParticipation(int eventId, String eventName, String tradingMethodType, String status,
                                   String winningOptionName, double totalCommissionPaid,
                                   List<TradeRecordInfo> lmsrHistory, List<OptionHoldingInfo> orderBookHoldings,
                                   List<OptionInfo> optionTotals, Double profitLoss) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.tradingMethodType = tradingMethodType;
        this.status = status;
        this.winningOptionName = winningOptionName;
        this.totalCommissionPaid = totalCommissionPaid;
        this.lmsrHistory = lmsrHistory;
        this.orderBookHoldings = orderBookHoldings;
        this.optionTotals = optionTotals;
        this.profitLoss = profitLoss;
    }

    public int getEventId() {
        return eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public String getTradingMethodType() {
        return tradingMethodType;
    }

    public String getStatus() {
        return status;
    }

    public String getWinningOptionName() {
        return winningOptionName;
    }

    public double getTotalCommissionPaid() {
        return totalCommissionPaid;
    }

    public List<TradeRecordInfo> getLmsrHistory() {
        return lmsrHistory;
    }

    public List<OptionHoldingInfo> getOrderBookHoldings() {
        return orderBookHoldings;
    }

    public List<OptionInfo> getOptionTotals() {
        return optionTotals;
    }

    public Double getProfitLoss() {
        return profitLoss;
    }
}
