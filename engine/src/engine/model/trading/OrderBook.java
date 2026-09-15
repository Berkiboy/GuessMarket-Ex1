package engine.model.trading;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class OrderBook implements Serializable {

    private final List<Order> bids = new ArrayList<>();
    private final List<Order> asks = new ArrayList<>();
    private Double lastTradePrice;

    List<Order> restingOrders(OrderSide side) {
        return side == OrderSide.BUY ? bids : asks;
    }

    void addResting(Order order) {
        restingOrders(order.getSide()).add(order);
    }

    Order bestOpposing(OrderSide side, double price) {
        List<Order> opposite = restingOrders(side == OrderSide.BUY ? OrderSide.SELL : OrderSide.BUY);
        Order best = null;
        for (Order candidate : opposite) {
            if (candidate.getQuantity() <= 0) {
                continue;
            }
            boolean matches = side == OrderSide.BUY
                    ? candidate.getPrice() <= price
                    : candidate.getPrice() >= price;
            if (!matches) {
                continue;
            }
            if (best == null
                    || (side == OrderSide.BUY && candidate.getPrice() < best.getPrice())
                    || (side == OrderSide.SELL && candidate.getPrice() > best.getPrice())) {
                best = candidate;
            }
        }
        return best;
    }

    void removeIfExhausted(Order order) {
        if (order.getQuantity() <= 0) {
            restingOrders(order.getSide()).remove(order);
        }
    }

    void setLastTradePrice(double price) {
        lastTradePrice = price;
    }

    public Double getLastTradePrice() {
        return lastTradePrice;
    }

    public Double getBestBid() {
        return bestPrice(bids, true);
    }

    public Double getBestAsk() {
        return bestPrice(asks, false);
    }

    public Double getMid() {
        Double bid = getBestBid();
        Double ask = getBestAsk();
        if (bid == null || ask == null) {
            return null;
        }
        return (bid + ask) / 2.0;
    }

    public Double getSpread() {
        Double bid = getBestBid();
        Double ask = getBestAsk();
        if (bid == null || ask == null) {
            return null;
        }
        return ask - bid;
    }

    public List<Order> getBids() {
        return new ArrayList<>(bids);
    }

    public List<Order> getAsks() {
        return new ArrayList<>(asks);
    }

    private Double bestPrice(List<Order> orders, boolean highest) {
        Double best = null;
        for (Order order : orders) {
            if (order.getQuantity() <= 0) {
                continue;
            }
            if (best == null || (highest && order.getPrice() > best) || (!highest && order.getPrice() < best)) {
                best = order.getPrice();
            }
        }
        return best;
    }
}
