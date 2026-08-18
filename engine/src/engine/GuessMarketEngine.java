package engine;

import engine.dto.*;
import engine.exceptions.FileValidationException;
import engine.exceptions.InvalidOperationException;
import engine.io.EventsFileLoader;
import engine.model.Event;
import engine.model.Option;
import engine.model.TradeRecord;

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

    private final EventsFileLoader fileLoader;
    private List<Event> events;

    public GuessMarketEngine(EventsFileLoader fileLoader) {
        this.fileLoader = fileLoader;
        this.events = new ArrayList<>();
    }

    @Override
    public LoadResult loadEventsFile(String path) {
        List<Event> loadedEvents;
        try {
            loadedEvents = fileLoader.load(path);
        } catch (FileValidationException e) {
            // a failed load never touches the currently loaded events
            return LoadResult.failure(e.getMessage());
        }

        // valid file fully replaces the previous one; fund each event's subsidy
        for (Event event : loadedEvents) {
            event.applyInitialSubsidy();
        }
        this.events = loadedEvents;

        return LoadResult.success(events.size());
    }

    @Override
    public List<EventSummary> getAllEvents() {
        return events.stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    @Override
    public EventStateSummary getEventState(int eventId) {
        return toStateSummary(findEvent(eventId));
    }

    @Override
    public PurchaseResult buyShares(int eventId, int optionIndex, int quantity) {
        Event event = findEvent(eventId);
        try {
            TradeRecord record = event.buy(optionIndex, quantity);
            return new PurchaseResult(record.getPricePaid(), record.getCommissionPaid(), toStateSummary(event));
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new InvalidOperationException(e.getMessage());
        }
    }

    @Override
    public EventStateSummary closeEvent(int eventId, int winningOptionIndex) {
        Event event = findEvent(eventId);
        try {
            event.close(winningOptionIndex);
            return toStateSummary(event);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new InvalidOperationException(e.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public LoadResult saveState(String path) {
        File file = new File(path + STATE_FILE_EXTENSION);
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
            out.writeObject(events);
            return LoadResult.success("State saved successfully to \"" + file.getPath() + "\".", events.size());
        } catch (IOException e) {
            return LoadResult.failure("Could not save state to \"" + file.getPath() + "\": " + e.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public LoadResult loadState(String path) {
        File file = new File(path + STATE_FILE_EXTENSION);
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            List<Event> loadedEvents = (List<Event>) in.readObject();
            this.events = loadedEvents;
            return LoadResult.success("State loaded successfully from \"" + file.getPath() + "\".", events.size());
        } catch (IOException | ClassNotFoundException e) {
            return LoadResult.failure("Could not load state from \"" + file.getPath() + "\": " + e.getMessage());
        }
    }

    private Event findEvent(int eventId) {
        return events.stream()
                .filter(e -> e.getId() == eventId)
                .findFirst()
                .orElseThrow(() -> new InvalidOperationException("No event found with id " + eventId));
    }

    private EventSummary toSummary(Event event) {
        List<String> optionNames = event.getOptions().stream()
                .map(Option::getName)
                .collect(Collectors.toList());

        return new EventSummary(
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.getCommissionPercent(),
                event.getCommissionType().name(),
                optionNames,
                event.getStatus() == engine.model.EventStatus.CLOSED
        );
    }

    private EventStateSummary toStateSummary(Event event) {
        List<OptionInfo> optionInfos = new ArrayList<>();
        for (int i = 0; i < event.getOptions().size(); i++) {
            Option option = event.getOptions().get(i);
            double price = event.getStatus() == engine.model.EventStatus.CLOSED
                    ? (i == event.getWinningOptionIndex() ? 1.0 : 0.0)
                    : event.getTradingMethod().getPrice(i);
            optionInfos.add(new OptionInfo(option.getName(), price, option.getTotalSharesBought()));
        }

        List<TradeRecordInfo> historyInfos = event.getHistoryMostRecentFirst().stream()
                .map(t -> new TradeRecordInfo(t.getOptionName(), t.getQuantity(), t.getPricePaid()))
                .collect(Collectors.toList());

        boolean closed = event.getStatus() == engine.model.EventStatus.CLOSED;
        String winningOptionName = closed ? event.getOptions().get(event.getWinningOptionIndex()).getName() : null;

        return new EventStateSummary(
                event.getId(),
                event.getName(),
                optionInfos,
                event.getAccount().getBalance(),
                event.getTotalCommissionCollected(),
                historyInfos,
                closed,
                winningOptionName
        );
    }
}
