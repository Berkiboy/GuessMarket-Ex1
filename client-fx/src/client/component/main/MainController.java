package client.component.main;

import client.GuessMarketClientApp;
import client.component.events.EventsController;
import client.component.user.UserController;
import client.util.Constants;
import client.util.Fmt;
import client.util.http.ServerApi;
import client.util.http.ServerError;
import engine.dto.UserDetail;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;

import java.util.Timer;

public class MainController {

    private GuessMarketClientApp app;
    private ServerApi api;
    private Timer timer;
    private DataRefresher dataRefresher;
    private boolean sessionEnded;

    // The user's balance and blocked state, shown both here and on the User tab (bound to these properties).
    private final StringProperty balanceText = new SimpleStringProperty("Balance: -");
    private final BooleanProperty blocked = new SimpleBooleanProperty(false);

    @FXML
    private Label userLabel;
    @FXML
    private Label balanceLabel;
    @FXML
    private Label connectionLabel;
    @FXML
    private EventsController eventsPaneController;
    @FXML
    private UserController userPaneController;

    @FXML
    private void initialize() {
        balanceLabel.textProperty().bind(balanceText);
        blocked.addListener((obs, wasBlocked, isBlocked) -> {
            balanceLabel.getStyleClass().remove("blocked");
            if (isBlocked) {
                balanceLabel.getStyleClass().add("blocked");
                new Alert(Alert.AlertType.WARNING, "Your balance is negative. You are blocked and cannot trade "
                        + "or open events until you deposit enough funds.").show();
            }
        });
    }

    public void init(GuessMarketClientApp app, ServerApi api, String username) {
        this.app = app;
        this.api = api;
        userLabel.setText("Logged in as: " + username);

        dataRefresher = new DataRefresher(api, this::onData, () -> connectionLabel.setText(""), this::onServerError);
        eventsPaneController.init(api, username, dataRefresher::requestRefresh);
        userPaneController.init(api, username, balanceText, blocked, dataRefresher::requestRefresh,
                this::onServerError);

        timer = new Timer("data-refresher", true);
        timer.schedule(dataRefresher, 0, Constants.REFRESH_RATE);
        userPaneController.startRefreshers();
    }

    private void onData(DataRefresher.Snapshot snapshot) {
        UserDetail me = snapshot.me();
        balanceText.set("Balance: " + Fmt.money(me.getBalance()) + (me.isBlocked() ? "  (BLOCKED)" : ""));
        blocked.set(me.isBlocked());
        eventsPaneController.update(snapshot.events());
        userPaneController.updateMyDetails(me);
    }

    private void onServerError(ServerError error) {
        if (error.isUnauthorized()) {
            if (!sessionEnded) {
                sessionEnded = true;
                stopRefreshers();
                app.showLogin("Your session has ended. Please log in again.");
            }
        } else {
            connectionLabel.setText(error.getMessage());
        }
    }

    @FXML
    private void onLogout() {
        sessionEnded = true;
        stopRefreshers();
        api.logout(() -> app.showLogin(null));
    }

    /** Called when the window closes: stop pulling and free the user name on the server. */
    public void shutdown() {
        stopRefreshers();
        if (!sessionEnded) {
            api.logoutSync();
        }
    }

    private void stopRefreshers() {
        if (timer != null) {
            timer.cancel();
        }
        userPaneController.stopRefreshers();
    }
}
