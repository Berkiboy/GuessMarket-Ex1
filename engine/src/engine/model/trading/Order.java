package engine.model.trading;

import engine.model.User;

public class Order {

    private final User user;
    private final OrderSide side;
    private final double price;
    private int quantity;

    public Order(User user, OrderSide side, int quantity, double price) {
        this.user = user;
        this.side = side;
        this.quantity = quantity;
        this.price = price;
    }

    void reduceQuantity(int amount) {
        quantity -= amount;
    }

    public User getUser() {
        return user;
    }

    public OrderSide getSide() {
        return side;
    }

    public double getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }
}
