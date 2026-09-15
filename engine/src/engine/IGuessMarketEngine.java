package engine;

import engine.dto.EventStateSummary;
import engine.dto.EventSummary;
import engine.dto.LoadResult;
import engine.dto.OrderBookEventStateSummary;
import engine.dto.OrderSubmitResult;
import engine.dto.PurchaseResult;
import engine.dto.Side;
import engine.dto.UserDetail;
import engine.dto.UserSummary;

import java.util.List;

public interface IGuessMarketEngine {

    LoadResult loadEventsFile(String path);

    LoadResult saveState(String path);

    LoadResult loadState(String path);

    List<UserSummary> getAllUsers();

    UserDetail getUserDetail(String userName);

    List<EventSummary> getAllEvents();

    EventStateSummary getLmsrEventState(int eventId);

    OrderBookEventStateSummary getOrderBookEventState(int eventId);

    void openEvent(String actingUserName, int eventId);

    PurchaseResult buyLmsrShares(String buyerUserName, int eventId, int optionIndex, int quantity);

    OrderSubmitResult submitOrder(String userName, int eventId, int optionIndex, Side side, int quantity,
                                   double price);

    void closeEvent(String actingUserName, int eventId, int winningOptionIndex);
}
