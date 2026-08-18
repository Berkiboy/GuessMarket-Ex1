package engine;

import engine.dto.EventStateSummary;
import engine.dto.EventSummary;
import engine.dto.LoadResult;
import engine.dto.PurchaseResult;

import java.util.List;

public interface IGuessMarketEngine {

    // Command 1
    LoadResult loadEventsFile(String path);

    // Command 2
    List<EventSummary> getAllEvents();

    // Command 3
    EventStateSummary getEventState(int eventId);

    // Command 4
    PurchaseResult buyShares(int eventId, int optionIndex, int quantity);

    // Command 5
    EventStateSummary closeEvent(int eventId, int winningOptionIndex);

    // Bonus
    LoadResult saveState(String path);

    // Bonus
    LoadResult loadState(String path);
}
