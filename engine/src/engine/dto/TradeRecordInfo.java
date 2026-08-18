package engine.dto;

public class TradeRecordInfo {
    private final String optionName;
    private final int quantity;
    private final double pricePaid;

    public TradeRecordInfo(String optionName, int quantity, double pricePaid) {
        this.optionName = optionName;
        this.quantity = quantity;
        this.pricePaid = pricePaid;
    }

    public String getOptionName() {
        return optionName;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getPricePaid() {
        return pricePaid;
    }
}
