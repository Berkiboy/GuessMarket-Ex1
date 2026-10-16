package engine;

import engine.dto.AccountTransactionInfo;
import engine.dto.EventStateSummary;
import engine.dto.EventSummary;
import engine.dto.FillInfo;
import engine.dto.LoadResult;
import engine.dto.OptionHoldingInfo;
import engine.dto.OptionInfo;
import engine.dto.OrderBookEntryInfo;
import engine.dto.OrderBookEventStateSummary;
import engine.dto.OrderBookOptionView;
import engine.dto.OrderBookStatsInfo;
import engine.dto.OrderSubmitResult;
import engine.dto.ParticipantInfo;
import engine.dto.PurchaseResult;
import engine.dto.Side;
import engine.dto.TradeRecordInfo;
import engine.dto.TransactionsDelta;
import engine.dto.UserDetail;
import engine.dto.UserEventParticipation;
import engine.dto.UserSummary;
import engine.exceptions.FileValidationException;
import engine.exceptions.InvalidOperationException;
import engine.io.EventDefinition;
import engine.io.EventsFileLoader;
import engine.model.AccountTransaction;
import engine.model.Event;
import engine.model.EventStatus;
import engine.model.Option;
import engine.model.TradeRecord;
import engine.model.User;
import engine.model.trading.LmsrTradingMethod;
import engine.model.trading.OrderBook;
import engine.model.trading.OrderBookTradingMethod;
import engine.model.trading.OrderFill;
import engine.model.trading.OrderSide;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class GuessMarketEngine implements IGuessMarketEngine {

    private final EventsFileLoader fileLoader;
    private final Map<String, User> usersByKey = new LinkedHashMap<>();
    private final List<Event> events = new ArrayList<>();
    private long dataVersion;

    public GuessMarketEngine(EventsFileLoader fileLoader) {
        this.fileLoader = fileLoader;
    }

    @Override
    public synchronized void login(String userName) {
        String name = userName == null ? "" : userName.trim();
        if (name.isEmpty()) {
            throw new InvalidOperationException("Please enter a user name.");
        }
        User user = usersByKey.get(key(name));
        if (user != null && user.isLoggedIn()) {
            throw new InvalidOperationException("The user name \"" + user.getName()
                    + "\" is already taken by a connected user. Please choose a different name.");
        }
        if (user == null) {
            user = new User(name);
            usersByKey.put(key(name), user);
        }
        user.setLoggedIn(true);
        dataVersion++;
    }

    @Override
    public synchronized void logout(String userName) {
        User user = usersByKey.get(key(userName));
        if (user != null && user.isLoggedIn()) {
            user.setLoggedIn(false);
            dataVersion++;
        }
    }

    @Override
    public synchronized boolean isLoggedIn(String userName) {
        User user = usersByKey.get(key(userName));
        return user != null && user.isLoggedIn();
    }

    @Override
    public synchronized LoadResult uploadEventsFile(String uploaderName, String fileName, InputStream content) {
        User uploader = findUser(uploaderName);
        List<EventDefinition> definitions;
        try {
            definitions = fileLoader.load(fileName, content);
        } catch (FileValidationException e) {
            return LoadResult.failure("The file \"" + fileName + "\" was not loaded:\n" + e.getMessage());
        }

        List<String> clashes = new ArrayList<>();
        for (EventDefinition definition : definitions) {
            events.stream()
                    .filter(e -> key(e.getName()).equals(key(definition.name())))
                    .findFirst()
                    .ifPresent(e -> clashes.add("An event named \"" + e.getName() + "\" already exists in the system."));
        }
        if (!clashes.isEmpty()) {
            return LoadResult.failure("The file \"" + fileName + "\" was not loaded:\n" + String.join("\n", clashes));
        }

        for (EventDefinition definition : definitions) {
            List<Option> options = definition.optionNames().stream().map(Option::new).collect(Collectors.toList());
            events.add(new Event(events.size() + 1, definition.name(), definition.description(),
                    definition.commissionPercent(), definition.commissionType(), options,
                    definition.tradingMethod(), uploader));
        }
        dataVersion++;
        return LoadResult.success("The file \"" + fileName + "\" was loaded successfully: " + definitions.size()
                + " event(s) added. You are the market maker of all of them.", definitions.size());
    }

    @Override
    public synchronized void depositFunds(String userName, double amount) {
        User user = findUser(userName);
        if (!(amount > 0) || Double.isInfinite(amount)) {
            throw new InvalidOperationException("The amount to deposit must be a positive number.");
        }
        user.getAccount().deposit(amount, "Deposit");
        user.unblockIfNonNegative();
        dataVersion++;
    }

    @Override
    public synchronized List<UserSummary> getAllUsers() {
        return usersByKey.values().stream()
                .map(u -> new UserSummary(u.getName(), u.getAccount().getBalance(), u.isBlocked(),
                        isMarketMaker(u), u.isLoggedIn()))
                .collect(Collectors.toList());
    }

    @Override
    public synchronized UserDetail getUserDetail(String userName) {
        User user = findUser(userName);
        List<UserEventParticipation> participations = new ArrayList<>();
        for (Event event : events) {
            if (event.isParticipant(user)) {
                participations.add(toParticipation(event, user));
            }
        }
        return new UserDetail(user.getName(), user.getAccount().getBalance(), user.isBlocked(), participations);
    }

    @Override
    public synchronized TransactionsDelta getTransactions(String userName, int fromVersion) {
        List<AccountTransaction> all = findUser(userName).getAccount().getTransactions();
        int from = Math.max(0, Math.min(fromVersion, all.size()));
        List<AccountTransactionInfo> entries = all.subList(from, all.size()).stream()
                .map(t -> new AccountTransactionInfo(t.serial(), t.time(), t.description(), t.amount(),
                        t.balanceAfter()))
                .collect(Collectors.toList());
        return new TransactionsDelta(all.size(), entries);
    }

    @Override
    public synchronized List<EventSummary> getAllEvents() {
        return events.stream().map(this::toSummary).collect(Collectors.toList());
    }

    @Override
    public synchronized EventStateSummary getLmsrEventState(int eventId) {
        Event event = findEvent(eventId);
        if (!(event.getTradingMethod() instanceof LmsrTradingMethod lmsr)) {
            throw new InvalidOperationException("Event \"" + event.getName() + "\" is not an LMSR event.");
        }

        List<OptionInfo> optionInfos = new ArrayList<>();
        boolean closed = event.getStatus() == EventStatus.CLOSED;
        for (int i = 0; i < event.getOptions().size(); i++) {
            Option option = event.getOptions().get(i);
            double price = closed
                    ? (i == event.getWinningOptionIndex() ? 1.0 : 0.0)
                    : lmsr.getPrice(i);
            optionInfos.add(new OptionInfo(option.getName(), price, option.getTotalSharesBought()));
        }

        List<TradeRecordInfo> history = event.getHistoryMostRecentFirst().stream()
                .map(t -> new TradeRecordInfo(t.getBuyer().getName(), t.getOptionName(), t.getQuantity(),
                        t.getPricePaid(), t.getCommissionPaid()))
                .collect(Collectors.toList());

        return new EventStateSummary(event.getId(), event.getName(), optionInfos, event.getAccount().getBalance(),
                event.getTotalCommissionCollected(), history, event.getStatus().name(), winningOptionName(event));
    }

    @Override
    public synchronized OrderBookEventStateSummary getOrderBookEventState(int eventId) {
        Event event = findEvent(eventId);
        if (!(event.getTradingMethod() instanceof OrderBookTradingMethod orderBook)) {
            throw new InvalidOperationException("Event \"" + event.getName() + "\" is not an Order Book event.");
        }

        List<OrderBookOptionView> optionViews = new ArrayList<>();
        for (int i = 0; i < event.getOptions().size(); i++) {
            OrderBook book = orderBook.getBook(i);
            OrderBookStatsInfo stats = new OrderBookStatsInfo(book.getLastTradePrice(), book.getBestBid(),
                    book.getBestAsk(), book.getMid(), book.getSpread());
            List<OrderBookEntryInfo> bids = book.getBids().stream()
                    .map(o -> new OrderBookEntryInfo(o.getUser().getName(), o.getQuantity(), o.getPrice()))
                    .collect(Collectors.toList());
            List<OrderBookEntryInfo> asks = book.getAsks().stream()
                    .map(o -> new OrderBookEntryInfo(o.getUser().getName(), o.getQuantity(), o.getPrice()))
                    .collect(Collectors.toList());
            optionViews.add(new OrderBookOptionView(event.getOptions().get(i).getName(), stats, bids, asks));
        }

        List<ParticipantInfo> participants = new ArrayList<>();
        for (User participant : orderBook.getParticipants()) {
            participants.add(new ParticipantInfo(participant.getName(), holdingsFor(event, orderBook, participant)));
        }

        return new OrderBookEventStateSummary(event.getId(), event.getName(), event.getAccount().getBalance(),
                event.getTotalCommissionCollected(), event.getStatus().name(), winningOptionName(event), optionViews,
                participants);
    }

    @Override
    public synchronized void openEvent(String actingUserName, int eventId) {
        User user = findUser(actingUserName);
        Event event = findEvent(eventId);
        try {
            event.open(user);
            dataVersion++;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new InvalidOperationException(e.getMessage());
        }
    }

    @Override
    public synchronized PurchaseResult buyLmsrShares(String buyerUserName, int eventId, int optionIndex,
                                                     int quantity) {
        User buyer = findUser(buyerUserName);
        Event event = findEvent(eventId);
        try {
            TradeRecord record = event.buyLmsr(buyer, optionIndex, quantity);
            dataVersion++;
            return new PurchaseResult(record.getPricePaid(), record.getCommissionPaid(), getLmsrEventState(eventId));
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new InvalidOperationException(e.getMessage());
        }
    }

    @Override
    public synchronized OrderSubmitResult submitOrder(String userName, int eventId, int optionIndex, Side side,
                                                      int quantity, double price) {
        User user = findUser(userName);
        Event event = findEvent(eventId);
        try {
            List<OrderFill> fills = event.submitOrder(user, optionIndex, OrderSide.valueOf(side.name()), quantity,
                    price);
            dataVersion++;
            List<FillInfo> fillInfos = fills.stream()
                    .map(f -> new FillInfo(f.getBuyer().getName(), f.getSeller() == null ? null : f.getSeller().getName(),
                            event.getOptions().get(f.getOptionIndex()).getName(), f.getQuantity(), f.getPrice()))
                    .collect(Collectors.toList());
            return new OrderSubmitResult(fillInfos, getOrderBookEventState(eventId));
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new InvalidOperationException(e.getMessage());
        }
    }

    @Override
    public synchronized void closeEvent(String actingUserName, int eventId, int winningOptionIndex) {
        User user = findUser(actingUserName);
        Event event = findEvent(eventId);
        try {
            event.close(user, winningOptionIndex);
            dataVersion++;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new InvalidOperationException(e.getMessage());
        }
    }

    @Override
    public synchronized long getDataVersion() {
        return dataVersion;
    }

    private List<OptionHoldingInfo> holdingsFor(Event event, OrderBookTradingMethod orderBook, User user) {
        int[] quantities = orderBook.getAllHoldings().getOrDefault(user, new int[event.getOptions().size()]);
        boolean closed = event.getStatus() == EventStatus.CLOSED;
        List<OptionHoldingInfo> holdings = new ArrayList<>();
        for (int i = 0; i < quantities.length; i++) {
            if (quantities[i] == 0) {
                continue;
            }
            int optionIndex = i;
            double totalPaid = orderBook.getFillLog().stream()
                    .filter(f -> f.getBuyer() == user && f.getOptionIndex() == optionIndex)
                    .mapToDouble(f -> f.getQuantity() * f.getPrice())
                    .sum();
            double price = closed
                    ? (i == event.getWinningOptionIndex() ? orderBook.getBaseValue() : 0.0)
                    : orderBook.getPrice(i);
            holdings.add(new OptionHoldingInfo(event.getOptions().get(i).getName(), quantities[i], totalPaid,
                    quantities[i] * price));
        }
        return holdings;
    }

    private UserEventParticipation toParticipation(Event event, User user) {
        boolean isOrderBook = event.getTradingMethod() instanceof OrderBookTradingMethod;
        boolean closed = event.getStatus() == EventStatus.CLOSED;

        List<TradeRecordInfo> lmsrHistory = new ArrayList<>();
        List<OptionHoldingInfo> obHoldings = new ArrayList<>();
        List<OptionInfo> optionTotals = new ArrayList<>();
        if (closed) {
            for (Option option : event.getOptions()) {
                optionTotals.add(new OptionInfo(option.getName(), 0, option.getTotalSharesBought()));
            }
        }
        Double profitLoss = closed ? event.getNetCashFlow(user) : null;

        if (isOrderBook) {
            obHoldings = holdingsFor(event, (OrderBookTradingMethod) event.getTradingMethod(), user);
        } else {
            for (TradeRecord record : event.getHistoryMostRecentFirst()) {
                if (record.getBuyer() == user) {
                    lmsrHistory.add(new TradeRecordInfo(record.getBuyer().getName(), record.getOptionName(),
                            record.getQuantity(), record.getPricePaid(), record.getCommissionPaid()));
                }
            }
        }

        return new UserEventParticipation(event.getId(), event.getName(), isOrderBook ? "ORDER_BOOK" : "LMSR",
                event.getStatus().name(), winningOptionName(event), event.getCommissionPaidBy(user), lmsrHistory,
                obHoldings, optionTotals, profitLoss);
    }

    private EventSummary toSummary(Event event) {
        List<String> optionNames = event.getOptions().stream().map(Option::getName).collect(Collectors.toList());
        String methodDetails;
        double maxOrderPrice = 0;
        if (event.getTradingMethod() instanceof OrderBookTradingMethod orderBook) {
            methodDetails = String.format("d = %.2f, initial = %.2f, mint %s", orderBook.getBaseValue(),
                    orderBook.getInitial(), orderBook.isMintAllowed() ? "allowed" : "not allowed");
            maxOrderPrice = orderBook.getMaxPrice();
        } else {
            LmsrTradingMethod lmsr = (LmsrTradingMethod) event.getTradingMethod();
            methodDetails = String.format("b = %d, opening subsidy = %.2f", lmsr.getB(), lmsr.openingCost());
        }
        boolean isOrderBook = event.getTradingMethod() instanceof OrderBookTradingMethod;
        return new EventSummary(event.getId(), event.getName(), event.getDescription(),
                event.getCommissionPercent(), event.getCommissionType().name(), optionNames,
                event.getStatus().name(), isOrderBook ? "ORDER_BOOK" : "LMSR", event.getMarketMaker().getName(),
                event.getAccount().getBalance(), methodDetails, maxOrderPrice);
    }

    private String winningOptionName(Event event) {
        return event.getStatus() == EventStatus.CLOSED
                ? event.getOptions().get(event.getWinningOptionIndex()).getName()
                : null;
    }

    private boolean isMarketMaker(User user) {
        return events.stream().anyMatch(e -> e.getMarketMaker() == user);
    }

    private User findUser(String userName) {
        User user = userName == null ? null : usersByKey.get(key(userName));
        if (user == null) {
            throw new InvalidOperationException("No user found with name \"" + userName + "\".");
        }
        return user;
    }

    private Event findEvent(int eventId) {
        return events.stream()
                .filter(e -> e.getId() == eventId)
                .findFirst()
                .orElseThrow(() -> new InvalidOperationException("No event found with number " + eventId + "."));
    }

    private static String key(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }
}
