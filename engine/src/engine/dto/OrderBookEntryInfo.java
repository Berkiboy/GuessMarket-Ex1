package engine.dto;

public class OrderBookEntryInfo {
    private final String userName;
    private final int quantity;
    private final double price;

    public OrderBookEntryInfo(String userName, int quantity, double price) {
        this.userName = userName;
        this.quantity = quantity;
        this.price = price;
    }

    public String getUserName() {
        return userName;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getPrice() {
        return price;
    }
}
