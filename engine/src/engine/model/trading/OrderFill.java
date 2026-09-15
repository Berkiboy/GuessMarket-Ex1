package engine.model.trading;

import engine.model.User;

public class OrderFill {

    private final int optionIndex;
    private final User buyer;
    private final User seller;
    private final int quantity;
    private final double price;
    private final boolean minted;

    public OrderFill(int optionIndex, User buyer, User seller, int quantity, double price, boolean minted) {
        this.optionIndex = optionIndex;
        this.buyer = buyer;
        this.seller = seller;
        this.quantity = quantity;
        this.price = price;
        this.minted = minted;
    }

    public int getOptionIndex() {
        return optionIndex;
    }

    public User getBuyer() {
        return buyer;
    }

    public User getSeller() {
        return seller;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getPrice() {
        return price;
    }

    public boolean isMinted() {
        return minted;
    }
}
