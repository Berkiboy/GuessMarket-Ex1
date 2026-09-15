package engine.dto;

public class OptionHoldingInfo {
    private final String optionName;
    private final int quantity;
    private final double value;

    public OptionHoldingInfo(String optionName, int quantity, double value) {
        this.optionName = optionName;
        this.quantity = quantity;
        this.value = value;
    }

    public String getOptionName() {
        return optionName;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getValue() {
        return value;
    }
}
