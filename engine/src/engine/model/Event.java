package engine.model;

import engine.model.trading.LmsrTradingMethod;
import engine.model.trading.OrderBookTradingMethod;
import engine.model.trading.OrderFill;
import engine.model.trading.OrderSide;
import engine.model.trading.TradingMethod;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Event implements Serializable {

    private final int id;
    private final String name;
    private final String description;
    private final int commissionPercent;
    private final CommissionType commissionType;
    private final List<Option> options;
    private final TradingMethod tradingMethod;
    private final User marketMaker;
    private final Account account = new Account();
    private final List<TradeRecord> history = new ArrayList<>();
    private final Map<User, Double> commissionPaidByUser = new LinkedHashMap<>();

    private EventStatus status = EventStatus.NOT_STARTED;
    private int winningOptionIndex = -1;
    private double totalCommissionCollected;

    public Event(int id, String name, String description, int commissionPercent, CommissionType commissionType,
                 List<Option> options, TradingMethod tradingMethod, User marketMaker) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.commissionPercent = commissionPercent;
        this.commissionType = commissionType;
        this.options = options;
        this.tradingMethod = tradingMethod;
        this.marketMaker = marketMaker;
    }

    public void open(User actingUser) {
        if (status != EventStatus.NOT_STARTED) {
            throw new IllegalStateException("Event \"" + name + "\" has already been opened.");
        }
        if (actingUser != marketMaker) {
            throw new IllegalStateException("Only the event's market maker can open it.");
        }
        double cost = tradingMethod.openingCost();
        if (marketMaker.getAccount().getBalance() < cost) {
            throw new IllegalStateException("Market maker does not have enough funds to open this event.");
        }

        marketMaker.getAccount().withdraw(cost);
        account.deposit(cost);
        if (tradingMethod instanceof OrderBookTradingMethod orderBook) {
            orderBook.allocateInitialShares(marketMaker);
        }
        status = EventStatus.ACTIVE;
    }

    public TradeRecord buyLmsr(User buyer, int optionIndex, int quantity) {
        if (status != EventStatus.ACTIVE) {
            throw new IllegalStateException("Event \"" + name + "\" is not currently active.");
        }
        if (!(tradingMethod instanceof LmsrTradingMethod lmsr)) {
            throw new IllegalStateException("Event \"" + name + "\" is not an LMSR event.");
        }
        if (buyer.isBlocked()) {
            throw new IllegalStateException("User \"" + buyer.getName() + "\" is blocked (negative balance).");
        }
        if (optionIndex < 0 || optionIndex >= options.size()) {
            throw new IllegalArgumentException("Invalid option index: " + optionIndex);
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be a positive number.");
        }

        double sharesCost = lmsr.costOfPurchase(optionIndex, quantity);
        double commission = commissionType == CommissionType.ON_PURCHASE
                ? sharesCost * commissionPercent / 100.0
                : 0.0;

        lmsr.commitPurchase(optionIndex, quantity);
        options.get(optionIndex).addShares(quantity);
        buyer.getAccount().withdraw(sharesCost + commission);
        account.deposit(sharesCost);
        if (commission > 0) {
            marketMaker.getAccount().deposit(commission);
            totalCommissionCollected += commission;
            commissionPaidByUser.merge(buyer, commission, Double::sum);
        }
        buyer.blockIfNegative();

        TradeRecord record = new TradeRecord(buyer, optionIndex, options.get(optionIndex).getName(), quantity,
                sharesCost, commission);
        history.add(record);
        return record;
    }

    public List<OrderFill> submitOrder(User user, int optionIndex, OrderSide side, int quantity, double price) {
        if (status != EventStatus.ACTIVE) {
            throw new IllegalStateException("Event \"" + name + "\" is not currently active.");
        }
        if (!(tradingMethod instanceof OrderBookTradingMethod orderBook)) {
            throw new IllegalStateException("Event \"" + name + "\" is not an Order Book event.");
        }
        if (user.isBlocked()) {
            throw new IllegalStateException("User \"" + user.getName() + "\" is blocked (negative balance).");
        }
        if (optionIndex < 0 || optionIndex >= options.size()) {
            throw new IllegalArgumentException("Invalid option index: " + optionIndex);
        }

        List<OrderFill> fills = orderBook.submitOrder(user, optionIndex, side, quantity, price, account);

        if (commissionType == CommissionType.ON_PURCHASE) {
            for (OrderFill fill : fills) {
                double commission = fill.getQuantity() * fill.getPrice() * commissionPercent / 100.0;
                fill.getBuyer().getAccount().withdraw(commission);
                marketMaker.getAccount().deposit(commission);
                totalCommissionCollected += commission;
                commissionPaidByUser.merge(fill.getBuyer(), commission, Double::sum);
            }
        }
        for (OrderFill fill : fills) {
            fill.getBuyer().blockIfNegative();
        }
        return fills;
    }

    public void close(User actingUser, int winningOptionIndex) {
        if (status != EventStatus.ACTIVE) {
            throw new IllegalStateException("Event \"" + name + "\" is not currently active.");
        }
        if (actingUser != marketMaker) {
            throw new IllegalStateException("Only the event's market maker can close it.");
        }
        if (winningOptionIndex < 0 || winningOptionIndex >= options.size()) {
            throw new IllegalArgumentException("Invalid option index: " + winningOptionIndex);
        }

        status = EventStatus.CLOSED;
        this.winningOptionIndex = winningOptionIndex;

        Map<User, Integer> winnerHoldings = tradingMethod instanceof OrderBookTradingMethod orderBook
                ? orderBookWinnerHoldings(orderBook, winningOptionIndex)
                : lmsrWinnerHoldings(winningOptionIndex);

        settleWinners(winnerHoldings);
    }

    private Map<User, Integer> lmsrWinnerHoldings(int winningOptionIndex) {
        Map<User, Integer> holdings = new LinkedHashMap<>();
        for (TradeRecord record : history) {
            if (record.getOptionIndex() == winningOptionIndex) {
                holdings.merge(record.getBuyer(), record.getQuantity(), Integer::sum);
            }
        }
        return holdings;
    }

    private Map<User, Integer> orderBookWinnerHoldings(OrderBookTradingMethod orderBook, int winningOptionIndex) {
        Map<User, Integer> holdings = new LinkedHashMap<>();
        for (Map.Entry<User, int[]> entry : orderBook.getAllHoldings().entrySet()) {
            int quantity = entry.getValue()[winningOptionIndex];
            if (quantity > 0) {
                holdings.put(entry.getKey(), quantity);
            }
        }
        return holdings;
    }

    private void settleWinners(Map<User, Integer> winnerHoldings) {
        double totalWinningShares = winnerHoldings.values().stream().mapToInt(Integer::intValue).sum();
        double pricePerShare = 1.0;

        if (commissionType == CommissionType.ON_CLOSE && totalWinningShares > 0) {
            double commission = totalWinningShares * pricePerShare * commissionPercent / 100.0;
            totalCommissionCollected += commission;
            pricePerShare -= pricePerShare * commissionPercent / 100.0;
        }

        for (Map.Entry<User, Integer> entry : winnerHoldings.entrySet()) {
            double payout = entry.getValue() * pricePerShare;
            entry.getKey().getAccount().deposit(payout);
            account.withdraw(payout);
        }

        double remainder = account.getBalance();
        marketMaker.getAccount().deposit(remainder);
        account.withdraw(remainder);
    }

    public List<TradeRecord> getHistoryMostRecentFirst() {
        List<TradeRecord> reversed = new ArrayList<>(history);
        Collections.reverse(reversed);
        return reversed;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getCommissionPercent() {
        return commissionPercent;
    }

    public CommissionType getCommissionType() {
        return commissionType;
    }

    public List<Option> getOptions() {
        return options;
    }

    public TradingMethod getTradingMethod() {
        return tradingMethod;
    }

    public User getMarketMaker() {
        return marketMaker;
    }

    public Account getAccount() {
        return account;
    }

    public EventStatus getStatus() {
        return status;
    }

    public int getWinningOptionIndex() {
        return winningOptionIndex;
    }

    public double getTotalCommissionCollected() {
        return totalCommissionCollected;
    }

    public double getCommissionPaidBy(User user) {
        return commissionPaidByUser.getOrDefault(user, 0.0);
    }

    public boolean isParticipant(User user) {
        for (TradeRecord record : history) {
            if (record.getBuyer() == user) {
                return true;
            }
        }
        if (tradingMethod instanceof OrderBookTradingMethod orderBook) {
            int[] holdings = orderBook.getAllHoldings().get(user);
            if (holdings != null) {
                for (int qty : holdings) {
                    if (qty != 0) {
                        return true;
                    }
                }
            }
            for (int i = 0; i < options.size(); i++) {
                for (var order : orderBook.getBook(i).getBids()) {
                    if (order.getUser() == user) {
                        return true;
                    }
                }
                for (var order : orderBook.getBook(i).getAsks()) {
                    if (order.getUser() == user) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
