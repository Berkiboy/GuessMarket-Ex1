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

    public UserEventParticipation(int eventId, String eventName, String tradingMethodType, String status,
                                   String winningOptionName, double totalCommissionPaid,
                                   List<TradeRecordInfo> lmsrHistory, List<OptionHoldingInfo> orderBookHoldings) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.tradingMethodType = tradingMethodType;
        this.status = status;
        this.winningOptionName = winningOptionName;
        this.totalCommissionPaid = totalCommissionPaid;
        this.lmsrHistory = lmsrHistory;
        this.orderBookHoldings = orderBookHoldings;
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
}
