package client.component.user;

import client.util.http.ServerApi;
import client.util.http.ServerError;
import engine.dto.UserSummary;

import java.util.List;
import java.util.TimerTask;
import java.util.function.Consumer;

/** Pulls the list of all users with the all-the-information-always technique. */
public class UsersListRefresher extends TimerTask {

    private final ServerApi api;
    private final Consumer<List<UserSummary>> usersListConsumer;
    private final Consumer<ServerError> onError;
    private boolean requestInProgress;

    public UsersListRefresher(ServerApi api, Consumer<List<UserSummary>> usersListConsumer,
                              Consumer<ServerError> onError) {
        this.api = api;
        this.usersListConsumer = usersListConsumer;
        this.onError = onError;
    }

    @Override
    public synchronized void run() {
        if (requestInProgress) {
            return;
        }
        requestInProgress = true;
        api.users(users -> {
            finished();
            usersListConsumer.accept(users);
        }, error -> {
            finished();
            onError.accept(error);
        });
    }

    private synchronized void finished() {
        requestInProgress = false;
    }
}
