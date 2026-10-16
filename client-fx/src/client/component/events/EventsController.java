package client.component.events;

import client.util.Fmt;
import client.util.http.ServerApi;
import client.util.http.ServerError;
import engine.dto.EventStateSummary;
import engine.dto.EventSummary;
import engine.dto.FillInfo;
import engine.dto.OptionHoldingInfo;
import engine.dto.OptionInfo;
import engine.dto.OrderBookEntryInfo;
import engine.dto.OrderBookEventStateSummary;
import engine.dto.OrderBookOptionView;
import engine.dto.OrderSubmitResult;
import engine.dto.ParticipantInfo;
import engine.dto.Side;
import engine.dto.TradeRecordInfo;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
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
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class EventsController {

    private ServerApi api;
    private String username;
    private Runnable requestRefresh = () -> { };
    private List<EventSummary> allEvents = new ArrayList<>();
    private Integer selectedEventId;
    private boolean updatingTable;
    private int detailRequestSeq;
    private final Set<String> collapsedPanes = new HashSet<>();

    @FXML
    private TableView<EventSummary> eventTable;
    @FXML
    private ScrollPane detailScroll;
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

        eventTable.getColumns().add(column("No.", 45, e -> String.valueOf(e.getId())));
        eventTable.getColumns().add(column("Name", 170, EventSummary::getName));
        eventTable.getColumns().add(column("Type", 85, e -> Fmt.typeLabel(e.getTradingMethodType())));
        eventTable.getColumns().add(column("Status", 85, e -> Fmt.statusLabel(e.getStatus())));
        eventTable.getColumns().add(column("Commission", 125,
                e -> e.getCommissionPercent() + "% " + Fmt.commissionLabel(e.getCommissionType())));
        eventTable.getColumns().add(column("Contract", 80, e -> Fmt.money(e.getContractBalance())));
        eventTable.getColumns().add(column("Market maker", 100, EventSummary::getMarketMakerName));

        eventTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (updatingTable) {
                return;
            }
            selectedEventId = selected == null ? null : selected.getId();
            collapsedPanes.clear();
            detailScroll.setVvalue(0);
            loadDetail();
        });
    }

    private static TableColumn<EventSummary, String> column(String title, double width,
                                                           Function<EventSummary, String> value) {
        TableColumn<EventSummary, String> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
        return column;
    }

    public void init(ServerApi api, String username, Runnable requestRefresh) {
        this.api = api;
        this.username = username;
        this.requestRefresh = requestRefresh;
    }

    /** Called by the poller whenever the server's data changed. */
    public void update(List<EventSummary> events) {
        allEvents = events;
        applyFilters();
        loadDetail();
    }

    private void applyFilters() {
        Set<String> types = selectedValues(lmsrToggle, "LMSR", orderBookToggle, "ORDER_BOOK");
        Set<String> statuses = selectedValues(notStartedToggle, "NOT_STARTED", activeToggle, "ACTIVE",
                closedToggle, "CLOSED");
        Set<String> commissionTypes = selectedValues(onPurchaseToggle, "ON_PURCHASE", onCloseToggle, "ON_CLOSE");

        List<EventSummary> filtered = allEvents.stream()
                .filter(e -> types.contains(e.getTradingMethodType()))
                .filter(e -> statuses.contains(e.getStatus()))
                .filter(e -> commissionTypes.contains(e.getCommissionType()))
                .collect(Collectors.toList());

        updatingTable = true;
        try {
            eventTable.setItems(FXCollections.observableArrayList(filtered));
            filtered.stream()
                    .filter(e -> selectedEventId != null && e.getId() == selectedEventId)
                    .findFirst()
                    .ifPresent(e -> eventTable.getSelectionModel().select(e));
        } finally {
            updatingTable = false;
        }
    }

    private Set<String> selectedValues(Object... toggleAndValuePairs) {
        Set<String> values = new HashSet<>();
        for (int i = 0; i < toggleAndValuePairs.length; i += 2) {
            ToggleButton toggle = (ToggleButton) toggleAndValuePairs[i];
            if (toggle.isSelected()) {
                values.add((String) toggleAndValuePairs[i + 1]);
            }
        }
        return values;
    }

    private EventSummary selectedSummary() {
        return allEvents.stream()
                .filter(e -> selectedEventId != null && e.getId() == selectedEventId)
                .findFirst()
                .orElse(null);
    }

    // ----- detail -----

    private void loadDetail() {
        EventSummary summary = selectedSummary();
        int requestSeq = ++detailRequestSeq;
        if (summary == null) {
            detailBox.getChildren().setAll(label("Select an event to see its details."));
            return;
        }
        Consumer<Object> onState = state -> {
            if (requestSeq == detailRequestSeq) {
                showDetail(summary, state);
            }
        };
        Consumer<ServerError> onError = error -> {
            if (requestSeq == detailRequestSeq) {
                detailBox.getChildren().setAll(label("Could not load the event details: " + error.getMessage()));
            }
        };
        if ("LMSR".equals(summary.getTradingMethodType())) {
            api.lmsrState(summary.getId(), onState::accept, onError);
        } else {
            api.orderBookState(summary.getId(), onState::accept, onError);
        }
    }

    private void showDetail(EventSummary summary, Object state) {
        double scroll = detailScroll.getVvalue();
        List<javafx.scene.Node> nodes = new ArrayList<>();

        Label header = label(summary.getName());
        header.getStyleClass().add("page-title");
        nodes.add(header);
        nodes.add(label(summary.getDescription()));
        nodes.add(label("Event no. " + summary.getId() + "  |  " + Fmt.typeLabel(summary.getTradingMethodType())
                + " (" + summary.getMethodDetails() + ")"));
        nodes.add(label("Market maker: " + summary.getMarketMakerName()
                + (isMine(summary) ? " (you)" : "")
                + "  |  Commission: " + summary.getCommissionPercent() + "% ("
                + Fmt.commissionLabel(summary.getCommissionType()) + ")"));
        nodes.add(label("Status: " + Fmt.statusLabel(summary.getStatus())
                + "  |  Contract balance: " + Fmt.money(summary.getContractBalance())));

        FlowPane actions = buildActions(summary);
        if (!actions.getChildren().isEmpty()) {
            nodes.add(actions);
        }

        if (state instanceof EventStateSummary lmsrState) {
            nodes.add(buildLmsrDetail(lmsrState));
        } else if (state instanceof OrderBookEventStateSummary orderBookState) {
            nodes.add(buildOrderBookDetail(orderBookState));
        }

        detailBox.getChildren().setAll(nodes);
        Platform.runLater(() -> detailScroll.setVvalue(scroll));
    }

    private boolean isMine(EventSummary summary) {
        return summary.getMarketMakerName().equalsIgnoreCase(username);
    }

    private FlowPane buildActions(EventSummary summary) {
        FlowPane actions = new FlowPane(10, 6);
        if ("NOT_STARTED".equals(summary.getStatus())) {
            if (isMine(summary)) {
                Button openButton = new Button("Open event");
                openButton.setOnAction(e -> onOpenEvent(summary));
                actions.getChildren().add(openButton);
            } else {
                actions.getChildren().add(label("Waiting for the market maker to open this event."));
            }
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
            if (isMine(summary)) {
                Button closeButton = new Button("Close event");
                closeButton.setOnAction(e -> onCloseEvent(summary));
                actions.getChildren().add(closeButton);
            }
        }
        return actions;
    }

    private VBox buildLmsrDetail(EventStateSummary state) {
        VBox box = new VBox(8);

        box.getChildren().add(label("Options:"));
        for (OptionInfo option : state.getOptions()) {
            boolean winner = option.getName().equals(state.getWinningOptionName());
            box.getChildren().add(label(String.format("  %s: price %s, total shares bought %d%s", option.getName(),
                    Fmt.money(option.getCurrentPrice()), option.getTotalSharesBought(), winner ? "  (WINNER)" : "")));
        }
        box.getChildren().add(label("Total commission collected: "
                + Fmt.money(state.getTotalCommissionCollected())));
        if (state.getWinningOptionName() != null) {
            box.getChildren().add(label("Winning option: " + state.getWinningOptionName()));
        }

        box.getChildren().add(label("Trade history (most recent first):"));
        List<TradeRecordInfo> history = state.getHistoryMostRecentFirst();
        if (history.isEmpty()) {
            box.getChildren().add(label("  (no trades yet)"));
        }
        for (TradeRecordInfo trade : history) {
            box.getChildren().add(label(String.format("  %s bought %d %s, paid %s, commission %s",
                    trade.getBuyerName(), trade.getQuantity(), trade.getOptionName(), Fmt.money(trade.getPricePaid()),
                    Fmt.money(trade.getCommissionPaid()))));
        }
        return box;
    }

    private VBox buildOrderBookDetail(OrderBookEventStateSummary state) {
        VBox box = new VBox(10);

        box.getChildren().add(label("Total commission collected: "
                + Fmt.money(state.getTotalCommissionCollected())));
        if (state.getWinningOptionName() != null) {
            box.getChildren().add(label("Winning option: " + state.getWinningOptionName()));
        }

        for (OrderBookOptionView view : state.getOptionViews()) {
            box.getChildren().add(buildOptionBookPane(view));
        }

        VBox participantsBox = new VBox(4);
        participantsBox.setPadding(new Insets(8));
        if (state.getParticipants().isEmpty()) {
            participantsBox.getChildren().add(label("(no participants yet)"));
        }
        for (ParticipantInfo participant : state.getParticipants()) {
            List<String> parts = new ArrayList<>();
            for (OptionHoldingInfo holding : participant.getHoldings()) {
                parts.add(holding.getQuantity() + " " + holding.getOptionName() + " (value "
                        + Fmt.money(holding.getMarketValue()) + ", paid " + Fmt.money(holding.getTotalPaid()) + ")");
            }
            participantsBox.getChildren().add(label(participant.getUserName() + ": "
                    + (parts.isEmpty() ? "no shares held (pending orders only)" : String.join(", ", parts))));
        }
        box.getChildren().add(titledPane("Participants", participantsBox));
        return box;
    }

    private TitledPane buildOptionBookPane(OrderBookOptionView view) {
        VBox content = new VBox(6);
        content.setPadding(new Insets(8));
        content.getChildren().add(label(String.format("LAST %s   BID %s   ASK %s   MID %s   SPREAD %s",
                Fmt.price(view.getStats().getLast()), Fmt.price(view.getStats().getBid()),
                Fmt.price(view.getStats().getAsk()), Fmt.price(view.getStats().getMid()),
                Fmt.price(view.getStats().getSpread()))));

        content.getChildren().add(label("Buy orders (bids):"));
        if (view.getBids().isEmpty()) {
            content.getChildren().add(label("  (none)"));
        }
        for (OrderBookEntryInfo entry : view.getBids()) {
            content.getChildren().add(label(String.format("  %s: %d @ %s", entry.getUserName(),
                    entry.getQuantity(), Fmt.money(entry.getPrice()))));
        }
        content.getChildren().add(label("Sell orders (asks):"));
        if (view.getAsks().isEmpty()) {
            content.getChildren().add(label("  (none)"));
        }
        for (OrderBookEntryInfo entry : view.getAsks()) {
            content.getChildren().add(label(String.format("  %s: %d @ %s", entry.getUserName(),
                    entry.getQuantity(), Fmt.money(entry.getPrice()))));
        }
        return titledPane("Order book: " + view.getOptionName(), content);
    }

    /** A pane that remembers being collapsed across the automatic refreshes. */
    private TitledPane titledPane(String title, VBox content) {
        TitledPane pane = new TitledPane(title, content);
        pane.setExpanded(!collapsedPanes.contains(title));
        pane.expandedProperty().addListener((obs, old, expanded) -> {
            if (expanded) {
                collapsedPanes.remove(title);
            } else {
                collapsedPanes.add(title);
            }
        });
        return pane;
    }

    private static Label label(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        return label;
    }

    // ----- actions (always performed as the logged-in user) -----

    private void onOpenEvent(EventSummary summary) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Opening the event charges your account with its opening cost (" + summary.getMethodDetails()
                        + ").\nOpen \"" + summary.getName() + "\" now?", ButtonType.OK, ButtonType.CANCEL);
        confirm.setHeaderText("Open event");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        api.openEvent(summary.getId(),
                () -> actionDone("The event \"" + summary.getName() + "\" is now active."), this::actionFailed);
    }

    private void onCloseEvent(EventSummary summary) {
        ChoiceDialog<String> optionDialog = new ChoiceDialog<>(summary.getOptionNames().get(0),
                summary.getOptionNames());
        optionDialog.setTitle("Close event");
        optionDialog.setHeaderText("Close \"" + summary.getName() + "\" and decide the winning option.");
        optionDialog.setContentText("Winning option:");
        Optional<String> winningOption = optionDialog.showAndWait();
        winningOption.ifPresent(option -> api.closeEvent(summary.getId(), summary.getOptionNames().indexOf(option),
                () -> actionDone("The event is closed. Winning option: " + option + "."), this::actionFailed));
    }

    private void onBuyLmsr(EventSummary summary) {
        ComboBox<String> optionCombo = optionCombo(summary);
        TextField quantityField = new TextField();
        Dialog<ButtonType> dialog = formDialog("Buy shares in \"" + summary.getName() + "\"",
                new Label("Option:"), optionCombo, new Label("Quantity:"), quantityField);
        if (dialog.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        Integer quantity = positiveInt(quantityField.getText(), "Quantity");
        if (quantity == null) {
            return;
        }
        int optionIndex = optionCombo.getSelectionModel().getSelectedIndex();
        api.buyLmsr(summary.getId(), optionIndex, quantity, purchase -> actionDone(String.format(
                "Bought %d \"%s\" shares.%nShares: %s%nCommission: %s%nTotal paid: %s", quantity,
                summary.getOptionNames().get(optionIndex), Fmt.money(purchase.getSharesCost()),
                Fmt.money(purchase.getCommissionPaid()), Fmt.money(purchase.getTotalPaid()))), this::actionFailed);
    }

    private void onSubmitOrder(EventSummary summary) {
        ComboBox<String> optionCombo = optionCombo(summary);
        ComboBox<String> sideCombo = new ComboBox<>(FXCollections.observableArrayList("BUY", "SELL"));
        sideCombo.getSelectionModel().selectFirst();
        TextField quantityField = new TextField();
        TextField priceField = new TextField();
        priceField.setPromptText("0.01 - " + Fmt.money(summary.getMaxOrderPrice()));
        Dialog<ButtonType> dialog = formDialog("Submit order to \"" + summary.getName() + "\"",
                new Label("Option:"), optionCombo, new Label("Side:"), sideCombo,
                new Label("Quantity:"), quantityField, new Label("Price per share:"), priceField);
        if (dialog.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        Integer quantity = positiveInt(quantityField.getText(), "Quantity");
        if (quantity == null) {
            return;
        }
        double price;
        try {
            price = Double.parseDouble(priceField.getText().trim());
        } catch (NumberFormatException e) {
            showError("Price must be a number, for example 0.45.");
            return;
        }
        if (price <= 0 || price > summary.getMaxOrderPrice() + 1e-9) {
            showError("Price must be positive and no more than " + Fmt.money(summary.getMaxOrderPrice())
                    + " (the base value d minus 0.01).");
            return;
        }
        int optionIndex = optionCombo.getSelectionModel().getSelectedIndex();
        Side side = Side.valueOf(sideCombo.getValue());
        api.submitOrder(summary.getId(), optionIndex, side, quantity, price,
                result -> actionDone(describeOrderResult(result, side, quantity)), this::actionFailed);
    }

    private String describeOrderResult(OrderSubmitResult result, Side side, int quantity) {
        List<FillInfo> mine = result.getFills().stream()
                .filter(f -> username.equalsIgnoreCase(side == Side.BUY ? f.getBuyerName() : f.getSellerName()))
                .collect(Collectors.toList());
        int filled = mine.stream().mapToInt(FillInfo::getQuantity).sum();
        StringBuilder text = new StringBuilder();
        if (filled == 0) {
            text.append("No matching order right now. Your order is waiting in the book.");
        } else {
            text.append(filled).append(" of ").append(quantity).append(" share(s) executed:\n");
            for (FillInfo fill : mine) {
                String counterparty = fill.getSellerName() == null ? "minted"
                        : side == Side.BUY ? "from " + fill.getSellerName() : "to " + fill.getBuyerName();
                text.append(String.format("  %d %s @ %s (%s)%n", fill.getQuantity(), fill.getOptionName(),
                        Fmt.money(fill.getPrice()), counterparty));
            }
            if (filled < quantity) {
                text.append("The remaining ").append(quantity - filled).append(" share(s) wait in the book.");
            }
        }
        return text.toString();
    }

    private ComboBox<String> optionCombo(EventSummary summary) {
        ComboBox<String> combo = new ComboBox<>(FXCollections.observableArrayList(summary.getOptionNames()));
        combo.getSelectionModel().selectFirst();
        return combo;
    }

    private Dialog<ButtonType> formDialog(String title, javafx.scene.Node... labelAndFieldPairs) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(title);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(15));
        for (int i = 0; i < labelAndFieldPairs.length; i += 2) {
            grid.addRow(i / 2, labelAndFieldPairs[i], labelAndFieldPairs[i + 1]);
        }
        dialog.getDialogPane().setContent(grid);
        return dialog;
    }

    private Integer positiveInt(String text, String fieldName) {
        try {
            int value = Integer.parseInt(text.trim());
            if (value > 0) {
                return value;
            }
        } catch (NumberFormatException ignored) {
            // reported below
        }
        showError(fieldName + " must be a positive whole number.");
        return null;
    }

    private void actionDone(String message) {
        requestRefresh.run();
        new Alert(Alert.AlertType.INFORMATION, message).show();
    }

    private void actionFailed(ServerError error) {
        showError(error.getMessage());
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText("The action could not be completed");
        alert.show();
    }
}
