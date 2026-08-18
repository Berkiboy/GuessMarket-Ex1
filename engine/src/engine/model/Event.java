package engine.model;

import engine.model.trading.TradingMethod;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Event implements Serializable {

    private final int id;
    private final String name;
    private final String description;
    private final int commissionPercent;
    private final CommissionType commissionType;
    private final List<Option> options;
    private final TradingMethod tradingMethod;
    private final Account account = new Account();
    private final List<TradeRecord> history = new ArrayList<>();

    private EventStatus status = EventStatus.ACTIVE;
    private int winningOptionIndex = -1;
    private double totalCommissionCollected;

    public Event(int id, String name, String description, int commissionPercent, CommissionType commissionType,
                 List<Option> options, TradingMethod tradingMethod) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.commissionPercent = commissionPercent;
        this.commissionType = commissionType;
        this.options = options;
        this.tradingMethod = tradingMethod;
    }

    public void applyInitialSubsidy() {
        account.withdraw(tradingMethod.initialSubsidy()); // MM funds the pot up front
    }

    public TradeRecord buy(int optionIndex, int quantity) {
        if (status != EventStatus.ACTIVE) {
            throw new IllegalStateException("Event \"" + name + "\" is closed and no longer accepts trades.");
        }
        if (optionIndex < 0 || optionIndex >= options.size()) {
            throw new IllegalArgumentException("Invalid option index: " + optionIndex);
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be a positive number.");
        }

        double sharesCost = tradingMethod.costOfPurchase(optionIndex, quantity);
        double commission = commissionType == CommissionType.ON_PURCHASE
                ? sharesCost * commissionPercent / 100.0
                : 0.0;

        tradingMethod.commitPurchase(optionIndex, quantity);
        options.get(optionIndex).addShares(quantity);
        account.deposit(sharesCost + commission);
        totalCommissionCollected += commission;

        TradeRecord record = new TradeRecord(options.get(optionIndex).getName(), quantity, sharesCost, commission);
        history.add(record);
        return record;
    }

    public void close(int winningOptionIndex) {
        if (status != EventStatus.ACTIVE) {
            throw new IllegalStateException("Event \"" + name + "\" is already closed.");
        }
        if (winningOptionIndex < 0 || winningOptionIndex >= options.size()) {
            throw new IllegalArgumentException("Invalid option index: " + winningOptionIndex);
        }

        status = EventStatus.CLOSED;
        this.winningOptionIndex = winningOptionIndex;

        double payoutPool = options.get(winningOptionIndex).getTotalSharesBought(); // $1 per winning share
        if (commissionType == CommissionType.ON_CLOSE) {
            double commission = payoutPool * commissionPercent / 100.0;
            totalCommissionCollected += commission;
            account.withdraw(payoutPool - commission); // commission withheld from the payout
        } else {
            account.withdraw(payoutPool); // commission was already collected at purchase time
        }
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
}
