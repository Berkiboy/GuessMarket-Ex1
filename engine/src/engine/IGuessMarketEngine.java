package engine;

import engine.dto.EventStateSummary;
import engine.dto.EventSummary;
import engine.dto.LoadResult;
import engine.dto.OrderBookEventStateSummary;
import engine.dto.OrderSubmitResult;
import engine.dto.PurchaseResult;
import engine.dto.Side;
import engine.dto.TransactionsDelta;
import engine.dto.UserDetail;
import engine.dto.UserSummary;

import java.io.InputStream;
import java.util.List;

/**
 * The Guess Market engine. It is passive: it does not know who calls it (a servlet, a test, a UI) and every
 * operation names the acting user explicitly. All operations are thread safe.
 */
public interface IGuessMarketEngine {

    /** Logs a user in, creating the user on first login. Fails if the name is already logged in. */
    void login(String userName);

    void logout(String userName);

    boolean isLoggedIn(String userName);

    /** Validates an events file and adds all its events, with the uploader as their market maker. */
    LoadResult uploadEventsFile(String uploaderName, String fileName, InputStream content);

    void depositFunds(String userName, double amount);

    List<UserSummary> getAllUsers();

    UserDetail getUserDetail(String userName);

    /** Delta fetching: the user's account lines after line number fromVersion (0 = all of them). */
    TransactionsDelta getTransactions(String userName, int fromVersion);

    List<EventSummary> getAllEvents();

    EventStateSummary getLmsrEventState(int eventId);

    OrderBookEventStateSummary getOrderBookEventState(int eventId);

    void openEvent(String actingUserName, int eventId);

    PurchaseResult buyLmsrShares(String buyerUserName, int eventId, int optionIndex, int quantity);

    OrderSubmitResult submitOrder(String userName, int eventId, int optionIndex, Side side, int quantity,
                                  double price);

    void closeEvent(String actingUserName, int eventId, int winningOptionIndex);

    /** Increases on every change to the system's data; clients poll it to know when to refresh. */
    long getDataVersion();
}
