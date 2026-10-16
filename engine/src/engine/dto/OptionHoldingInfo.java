package engine.dto;

public class OptionHoldingInfo {
    private final String optionName;
    private final int quantity;
    private final double totalPaid;
    private final double marketValue;

    public OptionHoldingInfo(String optionName, int quantity, double totalPaid, double marketValue) {
        this.optionName = optionName;
        this.quantity = quantity;
        this.totalPaid = totalPaid;
        this.marketValue = marketValue;
    }

    public String getOptionName() {
        return optionName;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getTotalPaid() {
        return totalPaid;
    }

    /** Quantity times the option's current price (last trade, else mid, else d / number of options). */
    public double getMarketValue() {
        return marketValue;
    }
}
