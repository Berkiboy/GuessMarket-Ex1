package engine.model;

import java.io.Serializable;

public class TradeRecord implements Serializable {

    private final String optionName;
    private final int quantity;
    private final double pricePaid; // shares cost only, excludes commission
    private final double commissionPaid; // 0 for on-close events, settled later at close

    public TradeRecord(String optionName, int quantity, double pricePaid, double commissionPaid) {
        this.optionName = optionName;
        this.quantity = quantity;
        this.pricePaid = pricePaid;
        this.commissionPaid = commissionPaid;
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
