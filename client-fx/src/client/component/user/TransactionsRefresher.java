package client.component.user;

import client.util.http.ServerApi;
import client.util.http.ServerError;
import engine.dto.AccountTransactionInfo;

import java.util.List;
import java.util.TimerTask;
import java.util.function.Consumer;

/**
 * Pulls the user's account lines with delta fetching: each request sends the number of the last line already
 * shown and receives only the newer lines (like the chat lines of the course's chat example).
 */
public class TransactionsRefresher extends TimerTask {

    private final ServerApi api;
    private final Consumer<List<AccountTransactionInfo>> newLinesConsumer;
    private final Consumer<ServerError> onError;
    private boolean requestInProgress;
    private int lastVersion;

    public TransactionsRefresher(ServerApi api, Consumer<List<AccountTransactionInfo>> newLinesConsumer,
                                 Consumer<ServerError> onError) {
        this.api = api;
        this.newLinesConsumer = newLinesConsumer;
        this.onError = onError;
    }

    @Override
    public synchronized void run() {
        if (requestInProgress) {
            return;
        }
        requestInProgress = true;
        api.transactions(lastVersion, delta -> {
            synchronized (this) {
                lastVersion = delta.getVersion();
                requestInProgress = false;
            }
            if (!delta.getEntries().isEmpty()) {
                newLinesConsumer.accept(delta.getEntries());
            }
        }, error -> {
            synchronized (this) {
                requestInProgress = false;
            }
            onError.accept(error);
        });
    }
}
