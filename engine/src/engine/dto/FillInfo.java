package engine.dto;

public class FillInfo {
    private final String buyerName;
    private final String sellerName;
    private final String optionName;
    private final int quantity;
    private final double price;

    public FillInfo(String buyerName, String sellerName, String optionName, int quantity, double price) {
        this.buyerName = buyerName;
        this.sellerName = sellerName;
        this.optionName = optionName;
        this.quantity = quantity;
        this.price = price;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public String getSellerName() {
        return sellerName;
    }

    public String getOptionName() {
        return optionName;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getPrice() {
        return price;
    }
}
