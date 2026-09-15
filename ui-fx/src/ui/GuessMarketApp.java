package ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class GuessMarketApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("main.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1050, 720);
        stage.setScene(scene);
        stage.setTitle("Guess Market");
        stage.setMinWidth(640);
        stage.setMinHeight(420);
        stage.show();
    }
}
