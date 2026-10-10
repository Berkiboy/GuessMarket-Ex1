package engine;

import engine.dto.EventStateSummary;
import engine.dto.EventSummary;
import engine.dto.FillInfo;
import engine.dto.LoadResult;
import engine.dto.OptionHoldingInfo;
import engine.dto.OrderBookEntryInfo;
import engine.dto.OrderBookEventStateSummary;
import engine.dto.OrderBookOptionView;
import engine.dto.OrderBookStatsInfo;
import engine.dto.OrderSubmitResult;
import engine.dto.OptionInfo;
import engine.dto.ParticipantInfo;
import engine.dto.PurchaseResult;
import engine.dto.Side;
import engine.dto.TradeRecordInfo;
import engine.dto.UserDetail;
import engine.dto.UserEventParticipation;
import engine.dto.UserSummary;
import engine.exceptions.FileValidationException;
import engine.exceptions.InvalidOperationException;
import engine.io.EventsFileLoaderV2;
import engine.io.LoadedGameState;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GuessMarketEngine implements IGuessMarketEngine {

    private static final String STATE_FILE_EXTENSION = ".gmdata";

    private final EventsFileLoaderV2 fileLoader;
    private List<User> users = new ArrayList<>();
    private List<Event> events = new ArrayList<>();

    public GuessMarketEngine(EventsFileLoaderV2 fileLoader) {
        this.fileLoader = fileLoader;
    }

    @Override
    public LoadResult loadEventsFile(String path) {
        try {
            LoadedGameState state = fileLoader.load(path);
            this.users = state.getUsers();
            this.events = state.getEvents();
            return LoadResult.success(events.size());
        } catch (FileValidationException e) {
            return LoadResult.failure(e.getMessage());
        }
    }

    @Override
    public LoadResult saveState(String path) {
        File file = new File(path + STATE_FILE_EXTENSION);
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
            out.writeObject(new LoadedGameState(users, events));
            return LoadResult.success("State saved successfully to \"" + file.getPath() + "\".", events.size());
        } catch (IOException e) {
            return LoadResult.failure("Could not save state to \"" + file.getPath() + "\": " + e.getMessage());
        }
    }

    @Override
    public LoadResult loadState(String path) {
        File file = new File(path + STATE_FILE_EXTENSION);
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            LoadedGameState state = (LoadedGameState) in.readObject();
            this.users = state.getUsers();
            this.events = state.getEvents();
            return LoadResult.success("State loaded successfully from \"" + file.getPath() + "\".", events.size());
        } catch (IOException | ClassNotFoundException e) {
            return LoadResult.failure("Could not load state from \"" + file.getPath() + "\": " + e.getMessage());
        }
    }

    @Override
    public List<UserSummary> getAllUsers() {
        return users.stream()
                .map(u -> new UserSummary(u.getName(), u.getAccount().getBalance(), u.isBlocked()))
                .collect(Collectors.toList());
    }

    @Override
    public UserDetail getUserDetail(String userName) {
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
    public List<EventSummary> getAllEvents() {
        return events.stream().map(this::toSummary).collect(Collectors.toList());
    }

    @Override
    public EventStateSummary getLmsrEventState(int eventId) {
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

        String winningOptionName = closed ? event.getOptions().get(event.getWinningOptionIndex()).getName() : null;

        return new EventStateSummary(event.getId(), event.getName(), optionInfos, event.getAccount().getBalance(),
                event.getTotalCommissionCollected(), history, event.getStatus().name(), winningOptionName);
    }

    @Override
    public OrderBookEventStateSummary getOrderBookEventState(int eventId) {
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
            int[] quantities = orderBook.getAllHoldings().getOrDefault(participant, new int[event.getOptions().size()]);
            participants.add(new ParticipantInfo(participant.getName(),
                    holdingsFor(event, orderBook, participant, quantities)));
        }

        boolean closed = event.getStatus() == EventStatus.CLOSED;
        String winningOptionName = closed ? event.getOptions().get(event.getWinningOptionIndex()).getName() : null;

        return new OrderBookEventStateSummary(event.getId(), event.getName(), event.getAccount().getBalance(),
                event.getTotalCommissionCollected(), event.getStatus().name(), winningOptionName, optionViews,
                participants);
    }

    @Override
    public void openEvent(String actingUserName, int eventId) {
        User user = findUser(actingUserName);
        Event event = findEvent(eventId);
        try {
            event.open(user);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new InvalidOperationException(e.getMessage());
        }
    }

    @Override
    public PurchaseResult buyLmsrShares(String buyerUserName, int eventId, int optionIndex, int quantity) {
        User buyer = findUser(buyerUserName);
        Event event = findEvent(eventId);
        try {
            TradeRecord record = event.buyLmsr(buyer, optionIndex, quantity);
            return new PurchaseResult(record.getPricePaid(), record.getCommissionPaid(), getLmsrEventState(eventId));
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new InvalidOperationException(e.getMessage());
        }
    }

    @Override
    public OrderSubmitResult submitOrder(String userName, int eventId, int optionIndex, Side side, int quantity,
                                          double price) {
        User user = findUser(userName);
        Event event = findEvent(eventId);
        try {
            List<OrderFill> fills = event.submitOrder(user, optionIndex, OrderSide.valueOf(side.name()), quantity,
                    price);
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
    public void closeEvent(String actingUserName, int eventId, int winningOptionIndex) {
        User user = findUser(actingUserName);
        Event event = findEvent(eventId);
        try {
            event.close(user, winningOptionIndex);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new InvalidOperationException(e.getMessage());
        }
    }

    private List<OptionHoldingInfo> holdingsFor(Event event, OrderBookTradingMethod orderBook, User user,
                                                 int[] quantities) {
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
            holdings.add(new OptionHoldingInfo(event.getOptions().get(i).getName(), quantities[i], totalPaid));
        }
        return holdings;
    }

    private UserEventParticipation toParticipation(Event event, User user) {
        boolean isOrderBook = event.getTradingMethod() instanceof OrderBookTradingMethod;
        boolean closed = event.getStatus() == EventStatus.CLOSED;
        String winningOptionName = closed ? event.getOptions().get(event.getWinningOptionIndex()).getName() : null;

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
            OrderBookTradingMethod orderBook = (OrderBookTradingMethod) event.getTradingMethod();
            int[] quantities = orderBook.getAllHoldings().getOrDefault(user, new int[event.getOptions().size()]);
            obHoldings = holdingsFor(event, orderBook, user, quantities);
        } else {
            for (TradeRecord record : event.getHistoryMostRecentFirst()) {
                if (record.getBuyer() == user) {
                    lmsrHistory.add(new TradeRecordInfo(record.getBuyer().getName(), record.getOptionName(),
                            record.getQuantity(), record.getPricePaid(), record.getCommissionPaid()));
                }
            }
        }

        return new UserEventParticipation(event.getId(), event.getName(), isOrderBook ? "ORDER_BOOK" : "LMSR",
                event.getStatus().name(), winningOptionName, event.getCommissionPaidBy(user), lmsrHistory,
                obHoldings, optionTotals, profitLoss);
    }

    private EventSummary toSummary(Event event) {
        List<String> optionNames = event.getOptions().stream().map(Option::getName).collect(Collectors.toList());
        boolean isOrderBook = event.getTradingMethod() instanceof OrderBookTradingMethod;
        return new EventSummary(event.getId(), event.getName(), event.getDescription(),
                event.getCommissionPercent(), event.getCommissionType().name(), optionNames,
                event.getStatus().name(), isOrderBook ? "ORDER_BOOK" : "LMSR", event.getMarketMaker().getName(),
                event.getAccount().getBalance());
    }

    private User findUser(String userName) {
        return users.stream()
                .filter(u -> u.getName().equals(userName))
                .findFirst()
                .orElseThrow(() -> new InvalidOperationException("No user found with name \"" + userName + "\"."));
    }

    private Event findEvent(int eventId) {
        return events.stream()
                .filter(e -> e.getId() == eventId)
                .findFirst()
                .orElseThrow(() -> new InvalidOperationException("No event found with id " + eventId));
    }
}
