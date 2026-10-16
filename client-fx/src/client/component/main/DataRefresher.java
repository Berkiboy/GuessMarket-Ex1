package client.component.main;

import client.util.http.ServerApi;
import client.util.http.ServerError;
import engine.dto.EventSummary;
import engine.dto.UserDetail;

import java.util.List;
import java.util.TimerTask;
import java.util.function.Consumer;

/**
 * Pulls the events and the user's own details. Each run asks the server for its data version first (a tiny
 * request); only when the version changed are the events and the account fetched and handed to the screens.
 */
public class DataRefresher extends TimerTask {

    public record Snapshot(List<EventSummary> events, UserDetail me) {
    }

    private final ServerApi api;
    private final Consumer<Snapshot> onData;
    private final Runnable onConnected;
    private final Consumer<ServerError> onError;
    private boolean requestInProgress;
    private boolean refreshRequested;
    private long lastVersion = -1;

    public DataRefresher(ServerApi api, Consumer<Snapshot> onData, Runnable onConnected,
                         Consumer<ServerError> onError) {
        this.api = api;
        this.onData = onData;
        this.onConnected = onConnected;
        this.onError = onError;
    }

    /** Makes the next run fetch everything even if the version did not change (e.g. after this user's action). */
    public synchronized void requestRefresh() {
        refreshRequested = true;
    }

    @Override
    public synchronized void run() {
        if (requestInProgress) {
            return;
        }
        requestInProgress = true;
        api.dataVersion(this::onVersion, this::failed);
    }

    private void onVersion(long version) {
        onConnected.run();
        boolean changed;
        synchronized (this) {
            changed = version != lastVersion || refreshRequested;
            refreshRequested = false;
        }
        if (!changed) {
            finished();
            return;
        }
        api.events(events -> api.myAccount(me -> {
            synchronized (this) {
                lastVersion = version;
            }
            finished();
            onData.accept(new Snapshot(events, me));
        }, this::failed), this::failed);
    }

    private synchronized void finished() {
        requestInProgress = false;
    }

    private void failed(ServerError error) {
        finished();
        onError.accept(error);
    }
}
