package engine.dto;

import java.util.List;

public class OrderBookOptionView {
    private final String optionName;
    private final OrderBookStatsInfo stats;
    private final List<OrderBookEntryInfo> bids;
    private final List<OrderBookEntryInfo> asks;

    public OrderBookOptionView(String optionName, OrderBookStatsInfo stats, List<OrderBookEntryInfo> bids,
                                List<OrderBookEntryInfo> asks) {
        this.optionName = optionName;
        this.stats = stats;
        this.bids = bids;
        this.asks = asks;
    }

    public String getOptionName() {
        return optionName;
    }

    public OrderBookStatsInfo getStats() {
        return stats;
    }

    public List<OrderBookEntryInfo> getBids() {
        return bids;
    }

    public List<OrderBookEntryInfo> getAsks() {
        return asks;
    }
}
