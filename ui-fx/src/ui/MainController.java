package ui;

import engine.GuessMarketEngine;
import engine.IGuessMarketEngine;
import engine.dto.LoadResult;
import engine.io.impl.XmlEventsFileLoaderV2;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class MainController {

    private final IGuessMarketEngine engine = new GuessMarketEngine(new XmlEventsFileLoaderV2());

    @FXML
    private Button loadButton;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label statusLabel;
    @FXML
    private UsersController usersPaneController;
    @FXML
    private EventsController eventsPaneController;

    @FXML
    private void initialize() {
        usersPaneController.setEngine(engine);
        eventsPaneController.setEngine(engine);
    }

    @FXML
    private void onLoadFile() {
        Stage stage = (Stage) loadButton.getScene().getWindow();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose an events XML file");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML files", "*.xml"));
        File file = chooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }

        Task<LoadResult> task = new Task<>() {
            @Override
            protected LoadResult call() throws InterruptedException {
                for (int step = 0; step <= 10; step++) {
                    Thread.sleep(120);
                    updateProgress(step, 10);
                }
                return engine.loadEventsFile(file.getAbsolutePath());
            }
        };

        progressBar.setVisible(true);
        progressBar.progressProperty().bind(task.progressProperty());
        task.setOnSucceeded(e -> {
            progressBar.progressProperty().unbind();
            progressBar.setVisible(false);
            statusLabel.setText(task.getValue().getMessage());
            usersPaneController.refresh();
            eventsPaneController.refresh();
        });
        task.setOnFailed(e -> {
            progressBar.progressProperty().unbind();
            progressBar.setVisible(false);
            statusLabel.setText("Load failed: " + task.getException());
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
}
