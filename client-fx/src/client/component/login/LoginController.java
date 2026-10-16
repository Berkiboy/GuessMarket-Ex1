package client.component.login;

import client.GuessMarketClientApp;
import client.util.http.ServerApi;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class LoginController {

    private GuessMarketClientApp app;
    private ServerApi api;

    @FXML
    private TextField usernameField;
    @FXML
    private Button loginButton;
    @FXML
    private Label messageLabel;

    public void init(GuessMarketClientApp app, ServerApi api, String message) {
        this.app = app;
        this.api = api;
        messageLabel.setText(message == null ? "" : message);
    }

    @FXML
    private void onLogin() {
        String name = usernameField.getText() == null ? "" : usernameField.getText().trim();
        if (name.isEmpty()) {
            messageLabel.setText("Please enter a user name.");
            return;
        }
        loginButton.setDisable(true);
        messageLabel.setText("Connecting...");
        api.login(name, app::showMain, error -> {
            loginButton.setDisable(false);
            messageLabel.setText(error.getMessage());
            usernameField.requestFocus();
            usernameField.selectAll();
        });
    }
}
