package client.component.user;

import client.util.Constants;
import client.util.Fmt;
import client.util.http.ServerApi;
import client.util.http.ServerError;
import engine.dto.AccountTransactionInfo;
import engine.dto.OptionHoldingInfo;
import engine.dto.OptionInfo;
import engine.dto.TradeRecordInfo;
import engine.dto.UserDetail;
import engine.dto.UserEventParticipation;
import engine.dto.UserSummary;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.function.Consumer;
import java.util.function.Function;

public class UserController {

    private ServerApi api;
    private String username;
    private Runnable requestRefresh = () -> { };
    private Consumer<ServerError> onServerError = error -> { };
    private Timer timer;
    private final Set<String> expandedParticipations = new HashSet<>();

    @FXML
    private ScrollPane myScroll;
    @FXML
    private Label nameLabel;
    @FXML
    private Button uploadButton;
    @FXML
    private ProgressBar uploadProgress;
    @FXML
    private Label uploadStatusLabel;
    @FXML
    private Label balanceLabel;
    @FXML
    private TextField depositField;
    @FXML
    private Button depositButton;
    @FXML
    private Label depositStatusLabel;
    @FXML
    private TableView<AccountTransactionInfo> transactionsTable;
    @FXML
    private Label participationsLabel;
    @FXML
    private VBox participationsBox;
    @FXML
    private TableView<UserSummary> usersTable;

    @FXML
    private void initialize() {
        transactionsTable.getColumns().add(column("No.", 45, t -> String.valueOf(t.getSerial())));
        transactionsTable.getColumns().add(column("Time", 70, AccountTransactionInfo::getTime));
        transactionsTable.getColumns().add(column("Description", 470, AccountTransactionInfo::getDescription));
        transactionsTable.getColumns().add(column("Amount", 80, t -> Fmt.signedMoney(t.getAmount())));
        transactionsTable.getColumns().add(column("Balance", 80, t -> Fmt.money(t.getBalanceAfter())));

        usersTable.getColumns().add(column("Name", 110,
                u -> u.getName() + (u.getName().equalsIgnoreCase(username) ? " (you)" : "")));
        usersTable.getColumns().add(column("Balance", 80,
                u -> Fmt.money(u.getBalance()) + (u.isBlocked() ? " (blocked)" : "")));
        usersTable.getColumns().add(column("Market maker", 90, u -> u.isMarketMaker() ? "Yes" : "No"));
        usersTable.getColumns().add(column("Status", 70, u -> u.isOnline() ? "Online" : "Offline"));
    }

    private static <T> TableColumn<T, String> column(String title, double width, Function<T, String> value) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
        return column;
    }

    public void init(ServerApi api, String username, StringProperty balanceText, BooleanProperty blocked,
                     Runnable requestRefresh, Consumer<ServerError> onServerError) {
        this.api = api;
        this.username = username;
        this.requestRefresh = requestRefresh;
        this.onServerError = onServerError;
        nameLabel.setText(username);

        balanceLabel.textProperty().bind(Bindings.concat("Current ", balanceText,
                Bindings.when(blocked).then(" - deposit funds to unblock").otherwise("")));
        blocked.addListener((obs, wasBlocked, isBlocked) -> {
            balanceLabel.getStyleClass().remove("blocked");
            if (isBlocked) {
                balanceLabel.getStyleClass().add("blocked");
            }
        });
    }

    /** Starts pulling the users list (all the information, always) and the account lines (delta fetching). */
    public void startRefreshers() {
        timer = new Timer("user-refreshers", true);
        timer.schedule(new UsersListRefresher(api, this::updateUsersList, onServerError), 0, Constants.REFRESH_RATE);
        timer.schedule(new TransactionsRefresher(api, this::addTransactions, onServerError), 0,
                Constants.REFRESH_RATE);
    }

    public void stopRefreshers() {
        if (timer != null) {
            timer.cancel();
        }
    }

    private void updateUsersList(List<UserSummary> users) {
        usersTable.setItems(FXCollections.observableArrayList(users));
    }

    private void addTransactions(List<AccountTransactionInfo> newLines) {
        transactionsTable.getItems().addAll(newLines);
        transactionsTable.scrollTo(transactionsTable.getItems().size() - 1);
    }

    /** Called by the main data refresher whenever the server's data changed. */
    public void updateMyDetails(UserDetail me) {
        double scroll = myScroll.getVvalue();
        participationsLabel.setText("My events (" + me.getParticipations().size() + "):");
        participationsBox.getChildren().clear();
        if (me.getParticipations().isEmpty()) {
            participationsBox.getChildren().add(new Label("You are not participating in any event yet."));
        }
        for (UserEventParticipation participation : me.getParticipations()) {
            participationsBox.getChildren().add(buildParticipationPane(participation));
        }
        Platform.runLater(() -> myScroll.setVvalue(scroll));
    }

    private TitledPane buildParticipationPane(UserEventParticipation participation) {
        VBox content = new VBox(6);
        content.setPadding(new Insets(8));
        content.getChildren().add(label("Status: " + Fmt.statusLabel(participation.getStatus())
                + (participation.getWinningOptionName() != null
                ? "  |  Winner: " + participation.getWinningOptionName() : "")));
        content.getChildren().add(label("Commission paid: " + Fmt.money(participation.getTotalCommissionPaid())));

        if ("LMSR".equals(participation.getTradingMethodType())) {
            content.getChildren().add(label("My trades (most recent first):"));
            List<TradeRecordInfo> history = participation.getLmsrHistory();
            if (history.isEmpty()) {
                content.getChildren().add(label("  (no trades yet)"));
            }
            for (TradeRecordInfo trade : history) {
                content.getChildren().add(label(String.format("  %s: %d shares, paid %s, commission %s",
                        trade.getOptionName(), trade.getQuantity(), Fmt.money(trade.getPricePaid()),
                        Fmt.money(trade.getCommissionPaid()))));
            }
            if (!participation.getOptionTotals().isEmpty()) {
                content.getChildren().add(label("Total shares bought per option (all users):"));
                for (OptionInfo option : participation.getOptionTotals()) {
                    boolean winner = option.getName().equals(participation.getWinningOptionName());
                    content.getChildren().add(label(String.format("  %s: %d shares%s", option.getName(),
                            option.getTotalSharesBought(), winner ? "  (WINNER)" : "")));
                }
            }
        } else {
            content.getChildren().add(label("My holdings:"));
            List<OptionHoldingInfo> holdings = participation.getOrderBookHoldings();
            if (holdings.isEmpty()) {
                content.getChildren().add(label("  (no holdings - pending orders only)"));
            }
            for (OptionHoldingInfo holding : holdings) {
                content.getChildren().add(label(String.format("  %s: %d shares, paid %s, current value %s",
                        holding.getOptionName(), holding.getQuantity(), Fmt.money(holding.getTotalPaid()),
                        Fmt.money(holding.getMarketValue()))));
            }
        }
        if (participation.getProfitLoss() != null) {
            double profitLoss = participation.getProfitLoss();
            content.getChildren().add(label("Total " + (profitLoss >= 0 ? "profit" : "loss")
                    + " from this event: " + Fmt.money(Math.abs(profitLoss))));
        }

        String title = participation.getEventName() + "  [" + Fmt.typeLabel(participation.getTradingMethodType())
                + ", " + Fmt.statusLabel(participation.getStatus()) + "]";
        TitledPane pane = new TitledPane(title, content);
        String key = participation.getEventName();
        pane.setExpanded(expandedParticipations.contains(key));
        pane.expandedProperty().addListener((obs, old, expanded) -> {
            if (expanded) {
                expandedParticipations.add(key);
            } else {
                expandedParticipations.remove(key);
            }
        });
        return pane;
    }

    private static Label label(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        return label;
    }

    @FXML
    private void onUpload() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose an events XML file");
        chooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("XML files", "*.xml"),
                new FileChooser.ExtensionFilter("All files", "*.*"));
        File file = chooser.showOpenDialog(uploadButton.getScene().getWindow());
        if (file == null) {
            return;
        }

        uploadButton.setDisable(true);
        uploadProgress.setVisible(true);
        uploadProgress.setManaged(true);
        uploadProgress.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        setStatus(uploadStatusLabel, null, "Uploading \"" + file.getName() + "\"...");

        api.uploadFile(file, result -> {
            finishUpload();
            showUploadResult(result.isSuccess(), result.getMessage());
            if (result.isSuccess()) {
                requestRefresh.run();
            }
        }, error -> {
            finishUpload();
            showUploadResult(false, error.getMessage());
        });
    }

    private void finishUpload() {
        uploadButton.setDisable(false);
        uploadProgress.setVisible(false);
        uploadProgress.setManaged(false);
    }

    private void showUploadResult(boolean success, String message) {
        setStatus(uploadStatusLabel, success ? "success-text" : "error-text", message);
        Alert alert = new Alert(success ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR, message);
        alert.setHeaderText(success ? "File loaded" : "The file was not loaded");
        alert.show();
    }

    private static void setStatus(Label label, String styleClass, String text) {
        label.getStyleClass().removeAll("success-text", "error-text");
        if (styleClass != null) {
            label.getStyleClass().add(styleClass);
        }
        label.setText(text);
    }

    @FXML
    private void onDeposit() {
        double amount;
        try {
            amount = Double.parseDouble(depositField.getText().trim());
        } catch (NumberFormatException e) {
            amount = -1;
        }
        if (!(amount > 0) || Double.isInfinite(amount)) {
            setStatus(depositStatusLabel, "error-text", "Please enter a positive number.");
            return;
        }
        double deposited = amount;
        depositButton.setDisable(true);
        api.deposit(deposited, detail -> {
            depositButton.setDisable(false);
            depositField.clear();
            setStatus(depositStatusLabel, "success-text", "Deposited " + Fmt.money(deposited) + ".");
            requestRefresh.run();
        }, error -> {
            depositButton.setDisable(false);
            setStatus(depositStatusLabel, "error-text", error.getMessage());
        });
    }
}
