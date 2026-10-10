package engine.model.trading;

import engine.model.Account;
import engine.model.User;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        return d / 2.0;
    }

    @Override
    public double openingCost() {
        return initial;
    }

    @Override
    public double payoutPerShare() {
        return d;
    }

    public double getBaseValue() {
        return d;
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

    public void allocateInitialShares(User marketMaker) {
        int pairs = (int) Math.round(initial / d);
        int[] arr = holdingsFor(marketMaker);
        for (int i = 0; i < books.length; i++) {
            arr[i] += pairs;
        }
    }

    public List<OrderFill> submitOrder(User user, int optionIndex, OrderSide side, int quantity, double price,
                                        Account eventAccount) {
        if (price <= 0 || price > d - 0.01) {
            throw new IllegalArgumentException(
                    "Price must be positive and no more than " + String.format("%.2f", d - 0.01) + ".");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be a positive number.");
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
            remaining = matchMint(user, optionIndex, remaining, price, eventAccount, fills);
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

            executeTransfer(buyer, seller, optionIndex, fillQty, fillPrice);
            fills.add(new OrderFill(optionIndex, buyer, seller, fillQty, fillPrice, false));

            resting.reduceQuantity(fillQty);
            book.removeIfExhausted(resting);
            book.setLastTradePrice(fillPrice);
            remaining -= fillQty;
        }
        return remaining;
    }

    private int matchMint(User incomingUser, int optionIndex, int quantity, double price, Account eventAccount,
                           List<OrderFill> fills) {
        int otherIndex = complementOf(optionIndex);
        OrderBook otherBook = books[otherIndex];
        int remaining = quantity;

        while (remaining > 0) {
            Order restingBuy = bestMintCandidate(otherBook, price);
            if (restingBuy == null) {
                break;
            }
            int mintQty = Math.min(remaining, restingBuy.getQuantity());
            double completingPrice = d - restingBuy.getPrice();

            incomingUser.getAccount().withdraw(completingPrice * mintQty);
            restingBuy.getUser().getAccount().withdraw(restingBuy.getPrice() * mintQty);
            eventAccount.deposit((completingPrice + restingBuy.getPrice()) * mintQty);

            holdingsFor(incomingUser)[optionIndex] += mintQty;
            holdingsFor(restingBuy.getUser())[otherIndex] += mintQty;

            fills.add(new OrderFill(optionIndex, incomingUser, null, mintQty, completingPrice, true));
            fills.add(new OrderFill(otherIndex, restingBuy.getUser(), null, mintQty, restingBuy.getPrice(), true));

            restingBuy.reduceQuantity(mintQty);
            otherBook.removeIfExhausted(restingBuy);
            remaining -= mintQty;
        }
        return remaining;
    }

    private Order bestMintCandidate(OrderBook otherBook, double incomingPrice) {
        Order best = null;
        for (Order candidate : otherBook.getBids()) {
            if (candidate.getQuantity() <= 0 || candidate.getPrice() + incomingPrice < d) {
                continue;
            }
            if (best == null || candidate.getPrice() > best.getPrice()) {
                best = candidate;
            }
        }
        return best;
    }

    private void executeTransfer(User buyer, User seller, int optionIndex, int quantity, double price) {
        double cost = quantity * price;
        buyer.getAccount().withdraw(cost);
        seller.getAccount().deposit(cost);
        holdingsFor(buyer)[optionIndex] += quantity;
        holdingsFor(seller)[optionIndex] -= quantity;
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

    private int complementOf(int optionIndex) {
        return optionIndex == 0 ? 1 : 0;
    }
}
