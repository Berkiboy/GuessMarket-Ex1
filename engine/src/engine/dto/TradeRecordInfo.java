package engine.dto;

public class TradeRecordInfo {
    private final String buyerName;
    private final String optionName;
    private final int quantity;
    private final double pricePaid;
    private final double commissionPaid;

    public TradeRecordInfo(String buyerName, String optionName, int quantity, double pricePaid,
                            double commissionPaid) {
        this.buyerName = buyerName;
        this.optionName = optionName;
        this.quantity = quantity;
        this.pricePaid = pricePaid;
        this.commissionPaid = commissionPaid;
    }

    public String getBuyerName() {
        return buyerName;
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

    public double getCommissionPaid() {
        return commissionPaid;
    }
}
