package client;

import client.component.login.LoginController;
import client.component.main.MainController;
import client.util.http.HttpClientUtil;
import client.util.http.ServerApi;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;

public class GuessMarketClientApp extends Application {

    private static final String LOGIN_FXML = "/client/component/login/login.fxml";
    private static final String MAIN_FXML = "/client/component/main/main.fxml";
    private static final String STYLESHEET = "/client/resources/guess-market.css";

    private final ServerApi api = new ServerApi();
    private Stage stage;
    private MainController mainController;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        stage.setTitle("Guess Market");
        stage.setOnCloseRequest(e -> shutdownMain());
        showLogin(null);
        stage.show();
    }

    @Override
    public void stop() {
        shutdownMain();
        HttpClientUtil.shutdown();
    }

    public void showLogin(String message) {
        mainController = null;
        FXMLLoader loader = loader(LOGIN_FXML);
        Parent root = load(loader);
        LoginController controller = loader.getController();
        controller.init(this, api, message);
        stage.setScene(scene(root, 520, 320));
        stage.setMinWidth(380);
        stage.setMinHeight(260);
        stage.setTitle("Guess Market - Login");
    }

    public void showMain(String username) {
        FXMLLoader loader = loader(MAIN_FXML);
        Parent root = load(loader);
        mainController = loader.getController();
        mainController.init(this, api, username);
        stage.setScene(scene(root, 1200, 780));
        stage.setMinWidth(700);
        stage.setMinHeight(480);
        stage.setTitle("Guess Market - " + username);
        stage.centerOnScreen();
    }

    private Scene scene(Parent root, double width, double height) {
        Scene scene = new Scene(root, width, height);
        scene.getStylesheets().add(getClass().getResource(STYLESHEET).toExternalForm());
        return scene;
    }

    private void shutdownMain() {
        if (mainController != null) {
            mainController.shutdown();
            mainController = null;
        }
    }

    private FXMLLoader loader(String fxml) {
        URL url = getClass().getResource(fxml);
        return new FXMLLoader(url);
    }

    private Parent load(FXMLLoader loader) {
        try {
            return loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
