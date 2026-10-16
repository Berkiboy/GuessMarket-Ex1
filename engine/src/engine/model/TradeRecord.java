package engine.model;

public class TradeRecord {

    private final User buyer;
    private final int optionIndex;
    private final String optionName;
    private final int quantity;
    private final double pricePaid;
    private final double commissionPaid;

    public TradeRecord(User buyer, int optionIndex, String optionName, int quantity, double pricePaid,
                        double commissionPaid) {
        this.buyer = buyer;
        this.optionIndex = optionIndex;
        this.optionName = optionName;
        this.quantity = quantity;
        this.pricePaid = pricePaid;
        this.commissionPaid = commissionPaid;
    }

    public User getBuyer() {
        return buyer;
    }

    public int getOptionIndex() {
        return optionIndex;
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
