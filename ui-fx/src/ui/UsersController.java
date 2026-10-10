package ui;

import engine.IGuessMarketEngine;
import engine.dto.OptionHoldingInfo;
import engine.dto.OptionInfo;
import engine.dto.TradeRecordInfo;
import engine.dto.UserDetail;
import engine.dto.UserEventParticipation;
import engine.dto.UserSummary;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;

import java.util.List;

public class UsersController {

    private IGuessMarketEngine engine;

    @FXML
    private ListView<UserSummary> userList;
    @FXML
    private VBox detailBox;

    @FXML
    private void initialize() {
        userList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(UserSummary user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                } else {
                    setText(user.getName() + "  (" + Fmt.money(user.getBalance()) + ")"
                            + (user.isBlocked() ? "  [BLOCKED]" : ""));
                }
            }
        });
        userList.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                showDetail(selected.getName());
            }
        });
    }

    public void setEngine(IGuessMarketEngine engine) {
        this.engine = engine;
        refresh();
    }

    public void refresh() {
        if (engine == null) {
            return;
        }
        String previouslySelected = userList.getSelectionModel().getSelectedItem() == null ? null
                : userList.getSelectionModel().getSelectedItem().getName();

        userList.getItems().setAll(engine.getAllUsers());

        if (previouslySelected != null) {
            userList.getItems().stream()
                    .filter(u -> u.getName().equals(previouslySelected))
                    .findFirst()
                    .ifPresentOrElse(u -> {
                        userList.getSelectionModel().select(u);
                        showDetail(u.getName());
                    }, () -> detailBox.getChildren().clear());
        }
    }

    private void showDetail(String userName) {
        UserDetail detail = engine.getUserDetail(userName);

        detailBox.getChildren().clear();
        Label header = new Label(detail.getName());
        header.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        Label balanceLabel = new Label("Balance: " + Fmt.money(detail.getBalance())
                + (detail.isBlocked() ? "  (BLOCKED - negative balance)" : ""));
        detailBox.getChildren().addAll(header, balanceLabel, new Label("Participating in "
                + detail.getParticipations().size() + " event(s):"));

        for (UserEventParticipation participation : detail.getParticipations()) {
            detailBox.getChildren().add(buildParticipationPane(participation));
        }
    }

    private TitledPane buildParticipationPane(UserEventParticipation participation) {
        VBox content = new VBox(6);
        content.setPadding(new Insets(8));
        content.getChildren().add(new Label("Status: " + Fmt.statusLabel(participation.getStatus())
                + (participation.getWinningOptionName() != null
                ? "  |  Winner: " + participation.getWinningOptionName() : "")));
        content.getChildren().add(new Label("Commission paid: " + Fmt.money(participation.getTotalCommissionPaid())));

        if ("LMSR".equals(participation.getTradingMethodType())) {
            content.getChildren().add(new Label("Trade history (most recent first):"));
            List<TradeRecordInfo> history = participation.getLmsrHistory();
            if (history.isEmpty()) {
                content.getChildren().add(new Label("  (no trades yet)"));
            }
            for (TradeRecordInfo trade : history) {
                content.getChildren().add(new Label(String.format("  %s: %d shares, paid %s, commission %s",
                        trade.getOptionName(), trade.getQuantity(), Fmt.money(trade.getPricePaid()),
                        Fmt.money(trade.getCommissionPaid()))));
            }
            if (!participation.getOptionTotals().isEmpty()) {
                content.getChildren().add(new Label("Total shares bought per option:"));
                for (OptionInfo option : participation.getOptionTotals()) {
                    boolean winner = option.getName().equals(participation.getWinningOptionName());
                    content.getChildren().add(new Label(String.format("  %s: %d shares%s", option.getName(),
                            option.getTotalSharesBought(), winner ? "  (WINNER)" : "")));
                }
            }
        } else {
            content.getChildren().add(new Label("Holdings:"));
            List<OptionHoldingInfo> holdings = participation.getOrderBookHoldings();
            if (holdings.isEmpty()) {
                content.getChildren().add(new Label("  (no holdings)"));
            }
            for (OptionHoldingInfo holding : holdings) {
                content.getChildren().add(new Label(String.format("  %s: %d shares, paid %s",
                        holding.getOptionName(), holding.getQuantity(), Fmt.money(holding.getValue()))));
            }
            if (participation.getProfitLoss() != null) {
                double profitLoss = participation.getProfitLoss();
                content.getChildren().add(new Label("Total " + (profitLoss >= 0 ? "profit" : "loss")
                        + " from this event: " + Fmt.money(Math.abs(profitLoss))));
            }
        }

        TitledPane pane = new TitledPane(participation.getEventName() + "  ["
                + participation.getTradingMethodType() + "]", content);
        pane.setExpanded(false);
        return pane;
    }
}
