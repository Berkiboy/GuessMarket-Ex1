package ui;

import engine.IGuessMarketEngine;
import engine.dto.EventStateSummary;
import engine.dto.EventSummary;
import engine.dto.OptionHoldingInfo;
import engine.dto.OptionInfo;
import engine.dto.OrderBookEntryInfo;
import engine.dto.OrderBookEventStateSummary;
import engine.dto.OrderBookOptionView;
import engine.dto.ParticipantInfo;
import engine.dto.Side;
import engine.dto.TradeRecordInfo;
import engine.dto.UserSummary;
import engine.exceptions.InvalidOperationException;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class EventsController {

    private IGuessMarketEngine engine;

    @FXML
    private ListView<EventSummary> eventList;
    @FXML
    private VBox detailBox;
    @FXML
    private ToggleButton lmsrToggle;
    @FXML
    private ToggleButton orderBookToggle;
    @FXML
    private ToggleButton notStartedToggle;
    @FXML
    private ToggleButton activeToggle;
    @FXML
    private ToggleButton closedToggle;
    @FXML
    private ToggleButton onPurchaseToggle;
    @FXML
    private ToggleButton onCloseToggle;

    @FXML
    private void initialize() {
        for (ToggleButton button : List.of(lmsrToggle, orderBookToggle, notStartedToggle, activeToggle, closedToggle,
                onPurchaseToggle, onCloseToggle)) {
            button.selectedProperty().addListener((obs, old, selected) -> applyFilters());
        }

        eventList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(EventSummary event, boolean empty) {
                super.updateItem(event, empty);
                setText(empty || event == null ? null : summaryLine(event));
            }
        });
        eventList.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                showDetail(selected.getId());
            }
        });
    }

    public void setEngine(IGuessMarketEngine engine) {
        this.engine = engine;
        refresh();
    }

    private String summaryLine(EventSummary event) {
        return event.getName() + "  [" + event.getTradingMethodType() + "]  "
                + Fmt.statusLabel(event.getStatus());
    }

    public void refresh() {
        if (engine == null) {
            return;
        }
        Integer previouslySelectedId = eventList.getSelectionModel().getSelectedItem() == null ? null
                : eventList.getSelectionModel().getSelectedItem().getId();
        applyFilters();
        if (previouslySelectedId != null) {
            eventList.getItems().stream()
                    .filter(e -> e.getId() == previouslySelectedId)
                    .findFirst()
                    .ifPresentOrElse(e -> {
                        eventList.getSelectionModel().select(e);
                        showDetail(e.getId());
                    }, () -> detailBox.getChildren().clear());
        }
    }

    private void applyFilters() {
        Set<String> types = selectedValues(lmsrToggle, "LMSR", orderBookToggle, "ORDER_BOOK");
        Set<String> statuses = selectedValues(notStartedToggle, "NOT_STARTED", activeToggle, "ACTIVE",
                closedToggle, "CLOSED");
        Set<String> commissionTypes = selectedValues(onPurchaseToggle, "ON_PURCHASE", onCloseToggle, "ON_CLOSE");

        List<EventSummary> filtered = engine.getAllEvents().stream()
                .filter(e -> types.contains(e.getTradingMethodType()))
                .filter(e -> statuses.contains(e.getStatus()))
                .filter(e -> commissionTypes.contains(e.getCommissionType()))
                .collect(Collectors.toList());
        eventList.setItems(FXCollections.observableArrayList(filtered));
    }

    private Set<String> selectedValues(Object... toggleAndValuePairs) {
        Set<String> values = new HashSet<>();
        for (int i = 0; i < toggleAndValuePairs.length; i += 2) {
            ToggleButton toggle = (ToggleButton) toggleAndValuePairs[i];
            String value = (String) toggleAndValuePairs[i + 1];
            if (toggle.isSelected()) {
                values.add(value);
            }
        }
        return values;
    }

    private void showDetail(int eventId) {
        EventSummary summary = engine.getAllEvents().stream().filter(e -> e.getId() == eventId).findFirst()
                .orElse(null);
        if (summary == null) {
            return;
        }

        detailBox.getChildren().clear();
        Label header = new Label(summary.getName());
        header.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        detailBox.getChildren().add(header);
        detailBox.getChildren().add(new Label(summary.getDescription()));
        detailBox.getChildren().add(new Label("Market maker: " + summary.getMarketMakerName()
                + "  |  Commission: " + summary.getCommissionPercent() + "% ("
                + Fmt.commissionLabel(summary.getCommissionType()) + ")"));
        detailBox.getChildren().add(new Label("Status: " + Fmt.statusLabel(summary.getStatus())
                + "  |  Contract balance: " + Fmt.money(summary.getContractBalance())));

        HBox actions = new HBox(10);
        if ("NOT_STARTED".equals(summary.getStatus())) {
            Button openButton = new Button("Open event");
            openButton.setOnAction(e -> onOpenEvent(summary));
            actions.getChildren().add(openButton);
        } else if ("ACTIVE".equals(summary.getStatus())) {
            if ("LMSR".equals(summary.getTradingMethodType())) {
                Button buyButton = new Button("Buy shares");
                buyButton.setOnAction(e -> onBuyLmsr(summary));
                actions.getChildren().add(buyButton);
            } else {
                Button orderButton = new Button("Submit order");
                orderButton.setOnAction(e -> onSubmitOrder(summary));
                actions.getChildren().add(orderButton);
            }
            Button closeButton = new Button("Close event");
            closeButton.setOnAction(e -> onCloseEvent(summary));
            actions.getChildren().add(closeButton);
        }
        if (!actions.getChildren().isEmpty()) {
            detailBox.getChildren().add(actions);
        }

        if ("LMSR".equals(summary.getTradingMethodType())) {
            detailBox.getChildren().add(buildLmsrDetail(eventId));
        } else {
            detailBox.getChildren().add(buildOrderBookDetail(eventId));
        }
    }

    private VBox buildLmsrDetail(int eventId) {
        EventStateSummary state = engine.getLmsrEventState(eventId);
        VBox box = new VBox(8);

        box.getChildren().add(new Label("Options:"));
        for (OptionInfo option : state.getOptions()) {
            box.getChildren().add(new Label(String.format("  %s: price %s, total shares %d",
                    option.getName(), Fmt.money(option.getCurrentPrice()), option.getTotalSharesBought())));
        }
        box.getChildren().add(new Label("Total commission collected: " + Fmt.money(state.getTotalCommissionCollected())));
        if (state.getWinningOptionName() != null) {
            box.getChildren().add(new Label("Winning option: " + state.getWinningOptionName()));
        }

        box.getChildren().add(new Label("Trade history (most recent first):"));
        List<TradeRecordInfo> history = state.getHistoryMostRecentFirst();
        if (history.isEmpty()) {
            box.getChildren().add(new Label("  (no trades yet)"));
        }
        for (TradeRecordInfo trade : history) {
            box.getChildren().add(new Label(String.format("  %s bought %d %s, paid %s, commission %s",
                    trade.getBuyerName(), trade.getQuantity(), trade.getOptionName(), Fmt.money(trade.getPricePaid()),
                    Fmt.money(trade.getCommissionPaid()))));
        }
        return box;
    }

    private VBox buildOrderBookDetail(int eventId) {
        OrderBookEventStateSummary state = engine.getOrderBookEventState(eventId);
        VBox box = new VBox(10);

        box.getChildren().add(new Label("Total commission collected: " + Fmt.money(state.getTotalCommissionCollected())));
        if (state.getWinningOptionName() != null) {
            box.getChildren().add(new Label("Winning option: " + state.getWinningOptionName()));
        }

        for (OrderBookOptionView view : state.getOptionViews()) {
            box.getChildren().add(buildOptionBookPane(view));
        }

        VBox participantsBox = new VBox(4);
        participantsBox.getChildren().add(new Label("Participants:"));
        if (state.getParticipants().isEmpty()) {
            participantsBox.getChildren().add(new Label("  (no participants yet)"));
        }
        for (ParticipantInfo participant : state.getParticipants()) {
            StringBuilder line = new StringBuilder("  " + participant.getUserName() + ": ");
            List<String> parts = new ArrayList<>();
            for (OptionHoldingInfo holding : participant.getHoldings()) {
                parts.add(holding.getQuantity() + " " + holding.getOptionName()
                        + " (paid " + Fmt.money(holding.getValue()) + ")");
            }
            line.append(String.join(", ", parts));
            participantsBox.getChildren().add(new Label(line.toString()));
        }
        box.getChildren().add(participantsBox);

        return box;
    }

    private TitledPane buildOptionBookPane(OrderBookOptionView view) {
        VBox content = new VBox(6);
        content.setPadding(new Insets(8));
        content.getChildren().add(new Label(String.format("LAST %s   BID %s   ASK %s   MID %s   SPREAD %s",
                Fmt.price(view.getStats().getLast()), Fmt.price(view.getStats().getBid()),
                Fmt.price(view.getStats().getAsk()), Fmt.price(view.getStats().getMid()),
                Fmt.price(view.getStats().getSpread()))));

        content.getChildren().add(new Label("Bids:"));
        if (view.getBids().isEmpty()) {
            content.getChildren().add(new Label("  (none)"));
        }
        for (OrderBookEntryInfo entry : view.getBids()) {
            content.getChildren().add(new Label(String.format("  %s: %d @ %s", entry.getUserName(),
                    entry.getQuantity(), Fmt.money(entry.getPrice()))));
        }
        content.getChildren().add(new Label("Asks:"));
        if (view.getAsks().isEmpty()) {
            content.getChildren().add(new Label("  (none)"));
        }
        for (OrderBookEntryInfo entry : view.getAsks()) {
            content.getChildren().add(new Label(String.format("  %s: %d @ %s", entry.getUserName(),
                    entry.getQuantity(), Fmt.money(entry.getPrice()))));
        }

        TitledPane pane = new TitledPane(view.getOptionName(), content);
        pane.setExpanded(true);
        return pane;
    }

    private void onOpenEvent(EventSummary summary) {
        pickUser("Open event", "Only the market maker (" + summary.getMarketMakerName()
                + ") can open this event.").ifPresent(userName -> runAction(() -> {
            engine.openEvent(userName, summary.getId());
            return "Event opened.";
        }));
    }

    private void onCloseEvent(EventSummary summary) {
        Optional<String> userName = pickUser("Close event", "Only the market maker ("
                + summary.getMarketMakerName() + ") can close this event.");
        if (userName.isEmpty()) {
            return;
        }
        ChoiceDialog<String> optionDialog = new ChoiceDialog<>(summary.getOptionNames().get(0),
                summary.getOptionNames());
        optionDialog.setTitle("Close event");
        optionDialog.setHeaderText("Which option won?");
        Optional<String> winningOption = optionDialog.showAndWait();
        winningOption.ifPresent(option -> runAction(() -> {
            int index = summary.getOptionNames().indexOf(option);
            engine.closeEvent(userName.get(), summary.getId(), index);
            return "Event closed. Winning option: " + option;
        }));
    }

    private void onBuyLmsr(EventSummary summary) {
        List<String> userNames = engine.getAllUsers().stream().map(UserSummary::getName).collect(Collectors.toList());
        BuyForm form = new BuyForm(userNames, summary.getOptionNames());
        Optional<ButtonType> result = form.dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }
        runAction(() -> {
            String userName = form.userCombo.getValue();
            int optionIndex = summary.getOptionNames().indexOf(form.optionCombo.getValue());
            int quantity = Integer.parseInt(form.quantityField.getText().trim());
            var purchase = engine.buyLmsrShares(userName, summary.getId(), optionIndex, quantity);
            return String.format("Bought %d shares for %s (commission %s).", quantity,
                    Fmt.money(purchase.getSharesCost()), Fmt.money(purchase.getCommissionPaid()));
        });
    }

    private void onSubmitOrder(EventSummary summary) {
        List<String> userNames = engine.getAllUsers().stream().map(UserSummary::getName).collect(Collectors.toList());
        OrderForm form = new OrderForm(userNames, summary.getOptionNames());
        Optional<ButtonType> result = form.dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }
        runAction(() -> {
            String userName = form.userCombo.getValue();
            int optionIndex = summary.getOptionNames().indexOf(form.optionCombo.getValue());
            Side side = Side.valueOf(form.sideCombo.getValue());
            int quantity = Integer.parseInt(form.quantityField.getText().trim());
            double price = Double.parseDouble(form.priceField.getText().trim());
            var submitResult = engine.submitOrder(userName, summary.getId(), optionIndex, side, quantity, price);
            return submitResult.getFills().isEmpty()
                    ? "Order resting in the book (no immediate match)."
                    : submitResult.getFills().size() + " fill(s) executed.";
        });
    }

    private Optional<String> pickUser(String title, String header) {
        List<String> names = engine.getAllUsers().stream().map(UserSummary::getName).collect(Collectors.toList());
        if (names.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "No users loaded.").showAndWait();
            return Optional.empty();
        }
        ChoiceDialog<String> dialog = new ChoiceDialog<>(names.get(0), names);
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText("Acting as:");
        return dialog.showAndWait();
    }

    private interface ActionCall {
        String run();
    }

    private void runAction(ActionCall action) {
        try {
            String message = action.run();
            refresh();
            new Alert(Alert.AlertType.INFORMATION, message).showAndWait();
        } catch (InvalidOperationException | NumberFormatException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage() != null ? e.getMessage() : "Invalid input.").showAndWait();
        }
    }

    private static class BuyForm {
        final Dialog<ButtonType> dialog = new Dialog<>();
        final ComboBox<String> userCombo;
        final ComboBox<String> optionCombo;
        final TextField quantityField = new TextField();

        BuyForm(List<String> userNames, List<String> optionNames) {
            dialog.setTitle("Buy shares");
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            userCombo = new ComboBox<>(FXCollections.observableArrayList(userNames));
            userCombo.getSelectionModel().selectFirst();
            optionCombo = new ComboBox<>(FXCollections.observableArrayList(optionNames));
            optionCombo.getSelectionModel().selectFirst();

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(15));
            grid.addRow(0, new Label("Acting as:"), userCombo);
            grid.addRow(1, new Label("Option:"), optionCombo);
            grid.addRow(2, new Label("Quantity:"), quantityField);
            dialog.getDialogPane().setContent(grid);
        }
    }

    private static class OrderForm {
        final Dialog<ButtonType> dialog = new Dialog<>();
        final ComboBox<String> userCombo;
        final ComboBox<String> optionCombo;
        final ComboBox<String> sideCombo = new ComboBox<>(FXCollections.observableArrayList("BUY", "SELL"));
        final TextField quantityField = new TextField();
        final TextField priceField = new TextField();

        OrderForm(List<String> userNames, List<String> optionNames) {
            dialog.setTitle("Submit order");
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            userCombo = new ComboBox<>(FXCollections.observableArrayList(userNames));
            userCombo.getSelectionModel().selectFirst();
            optionCombo = new ComboBox<>(FXCollections.observableArrayList(optionNames));
            optionCombo.getSelectionModel().selectFirst();
            sideCombo.getSelectionModel().selectFirst();

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(15));
            grid.addRow(0, new Label("Acting as:"), userCombo);
            grid.addRow(1, new Label("Option:"), optionCombo);
            grid.addRow(2, new Label("Side:"), sideCombo);
            grid.addRow(3, new Label("Quantity:"), quantityField);
            grid.addRow(4, new Label("Price per share:"), priceField);
            dialog.getDialogPane().setContent(grid);
        }
    }
}
