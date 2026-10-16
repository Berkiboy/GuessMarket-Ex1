package engine.model.trading;

import engine.model.User;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Order Book matching for an event with any number of options. This class only moves shares and maintains the
 * books; the owning Event turns the returned fills into money transfers and commissions.
 */
public class OrderBookTradingMethod implements TradingMethod {

    private final double initial;
    private final double d;
    private final boolean allowMint;
    private final OrderBook[] books;
    private final Map<User, int[]> holdings = new LinkedHashMap<>();
    private final List<OrderFill> fillLog = new ArrayList<>();

    public OrderBookTradingMethod(double initial, double d, boolean allowMint, int optionCount) {
        this.initial = initial;
        this.d = d;
        this.allowMint = allowMint;
        this.books = new OrderBook[optionCount];
        for (int i = 0; i < optionCount; i++) {
            books[i] = new OrderBook();
        }
    }

    @Override
    public double getPrice(int optionIndex) {
        Double last = books[optionIndex].getLastTradePrice();
        if (last != null) {
            return last;
        }
        Double mid = books[optionIndex].getMid();
        if (mid != null) {
            return mid;
        }
        return d / books.length;
    }

    @Override
    public double openingCost() {
        return initialSetCount() * d;
    }

    @Override
    public double payoutPerShare() {
        return d;
    }

    public double getInitial() {
        return initial;
    }

    public double getBaseValue() {
        return d;
    }

    public double getMaxPrice() {
        return d - 0.01;
    }

    public boolean isMintAllowed() {
        return allowMint;
    }

    public OrderBook getBook(int optionIndex) {
        return books[optionIndex];
    }

    public int getHoldings(User user, int optionIndex) {
        int[] arr = holdings.get(user);
        return arr == null ? 0 : arr[optionIndex];
    }

    public Map<User, int[]> getAllHoldings() {
        return holdings;
    }

    public List<OrderFill> getFillLog() {
        return fillLog;
    }

    /** Number of full option sets (one share of every option, worth d together) the MM receives on opening. */
    public int initialSetCount() {
        return (int) Math.round(initial / d);
    }

    public void allocateInitialShares(User marketMaker) {
        int sets = initialSetCount();
        int[] arr = holdingsFor(marketMaker);
        for (int i = 0; i < books.length; i++) {
            arr[i] += sets;
        }
    }

    public List<OrderFill> submitOrder(User user, int optionIndex, OrderSide side, int quantity, double price) {
        if (price <= 0 || price > getMaxPrice() + 1e-9) {
            throw new IllegalArgumentException("Price must be positive and no more than "
                    + String.format("%.2f", getMaxPrice()) + " (the base value d minus 0.01).");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be a positive whole number.");
        }
        if (side == OrderSide.SELL) {
            int available = getHoldings(user, optionIndex) - restingQuantity(user, books[optionIndex].getAsks());
            if (quantity > available) {
                throw new IllegalArgumentException("Cannot sell " + quantity + " shares: only " + available
                        + " shares are available to sell (holdings not already offered in the book).");
            }
        }

        List<OrderFill> fills = new ArrayList<>();
        OrderBook book = books[optionIndex];
        int remaining = matchSameOption(user, optionIndex, side, quantity, price, book, fills);

        if (remaining > 0 && side == OrderSide.BUY && allowMint) {
            remaining = matchMint(user, optionIndex, remaining, price, fills);
        }

        if (remaining > 0) {
            book.addResting(new Order(user, side, remaining, price));
        }

        fillLog.addAll(fills);
        return fills;
    }

    private int matchSameOption(User user, int optionIndex, OrderSide side, int quantity, double price,
                                 OrderBook book, List<OrderFill> fills) {
        int remaining = quantity;
        while (remaining > 0) {
            Order resting = book.bestOpposing(side, price);
            if (resting == null) {
                break;
            }
            int fillQty = Math.min(remaining, resting.getQuantity());
            double fillPrice = resting.getPrice();

            User buyer = side == OrderSide.BUY ? user : resting.getUser();
            User seller = side == OrderSide.BUY ? resting.getUser() : user;

            holdingsFor(buyer)[optionIndex] += fillQty;
            holdingsFor(seller)[optionIndex] -= fillQty;
            fills.add(new OrderFill(optionIndex, buyer, seller, fillQty, fillPrice, false));

            resting.reduceQuantity(fillQty);
            book.removeIfExhausted(resting);
            book.setLastTradePrice(fillPrice);
            remaining -= fillQty;
        }
        return remaining;
    }

    /**
     * Mint: when every other option has a resting buy order and the prices of all of them together with the
     * incoming price reach d, a new set of shares is created. The resting orders execute at their own price and
     * the incoming order pays the completing price up to d, so the event account receives exactly d per set.
     * With two options this is the YES/NO pairing.
     */
    private int matchMint(User incomingUser, int optionIndex, int quantity, double price, List<OrderFill> fills) {
        int remaining = quantity;
        while (remaining > 0) {
            Order[] counterparts = new Order[books.length];
            double counterpartsTotal = 0;
            int mintQty = remaining;
            for (int i = 0; i < books.length; i++) {
                if (i == optionIndex) {
                    continue;
                }
                Order best = highestBid(books[i]);
                if (best == null) {
                    return remaining;
                }
                counterparts[i] = best;
                counterpartsTotal += best.getPrice();
                mintQty = Math.min(mintQty, best.getQuantity());
            }
            // Every minted set must cost exactly d: the incoming price has to reach d together with the resting bids,
            // and the resting bids alone must stay below d so the incoming order pays a positive completing price.
            if (counterpartsTotal + price < d - 1e-9 || counterpartsTotal > d - 0.01 + 1e-9) {
                return remaining;
            }

            double completingPrice = d - counterpartsTotal;
            holdingsFor(incomingUser)[optionIndex] += mintQty;
            fills.add(new OrderFill(optionIndex, incomingUser, null, mintQty, completingPrice, true));
            books[optionIndex].setLastTradePrice(completingPrice);

            for (int i = 0; i < books.length; i++) {
                Order resting = counterparts[i];
                if (resting == null) {
                    continue;
                }
                holdingsFor(resting.getUser())[i] += mintQty;
                fills.add(new OrderFill(i, resting.getUser(), null, mintQty, resting.getPrice(), true));
                resting.reduceQuantity(mintQty);
                books[i].removeIfExhausted(resting);
                books[i].setLastTradePrice(resting.getPrice());
            }
            remaining -= mintQty;
        }
        return remaining;
    }

    private Order highestBid(OrderBook book) {
        Order best = null;
        for (Order candidate : book.getBids()) {
            if (candidate.getQuantity() > 0 && (best == null || candidate.getPrice() > best.getPrice())) {
                best = candidate;
            }
        }
        return best;
    }

    public Set<User> getParticipants() {
        Set<User> participants = new LinkedHashSet<>(holdings.keySet());
        for (OrderBook book : books) {
            book.getBids().forEach(order -> participants.add(order.getUser()));
            book.getAsks().forEach(order -> participants.add(order.getUser()));
        }
        return participants;
    }

    private int restingQuantity(User user, List<Order> orders) {
        return orders.stream()
                .filter(order -> order.getUser() == user)
                .mapToInt(Order::getQuantity)
                .sum();
    }

    private int[] holdingsFor(User user) {
        return holdings.computeIfAbsent(user, u -> new int[books.length]);
    }
}
