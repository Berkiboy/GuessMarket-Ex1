package ui;

import engine.IGuessMarketEngine;
import engine.dto.EventStateSummary;
import engine.dto.EventSummary;
import engine.dto.LoadResult;
import engine.dto.OptionInfo;
import engine.dto.PurchaseResult;
import engine.dto.TradeRecordInfo;
import engine.exceptions.InvalidOperationException;

import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

// only place touching System.in/out; never clears the screen or colors output (per spec)
public class ConsoleApp {

    private final IGuessMarketEngine engine;
    private final Scanner scanner = new Scanner(System.in);

    public ConsoleApp(IGuessMarketEngine engine) {
        this.engine = engine;
    }

    public void run() {
        System.out.println("Welcome to Guess Market!");
        boolean running = true;
        while (running) {
            printMenu();
            int choice = readMenuChoice(1, 8);
            switch (choice) {
                case 1 -> loadEventsFile();
                case 2 -> showAllEvents();
                case 3 -> showEventState();
                case 4 -> buyShares();
                case 5 -> closeEvent();
                case 6 -> saveState();
                case 7 -> loadState();
                case 8 -> {
                    System.out.println("Goodbye!");
                    running = false;
                }
            }
        }
    }

    private void printMenu() {
        System.out.println();
        System.out.println("==== Guess Market - Main Menu ====");
        System.out.println("1. Load events file");
        System.out.println("2. Show all events");
        System.out.println("3. Show event trading status");
        System.out.println("4. Buy shares (participate in an event)");
        System.out.println("5. Close an event");
        System.out.println("6. Save current state to a file (bonus)");
        System.out.println("7. Load state from a file (bonus)");
        System.out.println("8. Exit");
    }

    private void loadEventsFile() {
        System.out.print("Enter the full path to the XML events file: ");
        String path = scanner.nextLine().trim();
        LoadResult result = engine.loadEventsFile(path);
        System.out.println(result.getMessage());
    }

    private void showAllEvents() {
        List<EventSummary> all = engine.getAllEvents();
        if (all.isEmpty()) {
            System.out.println("No events are currently loaded. Use option 1 to load an events file.");
            return;
        }
        System.out.println();
        for (EventSummary event : all) {
            printEventSummary(event);
        }
    }

    private void printEventSummary(EventSummary event) {
        System.out.println("Event #" + event.getId() + ": " + event.getName());
        System.out.println("  Description: " + event.getDescription());
        System.out.println("  Commission: " + event.getCommissionPercent() + "% ("
                + displayCommissionType(event.getCommissionType()) + ")");
        System.out.println("  Options: " + String.join(", ", event.getOptionNames()));
        System.out.println("  Status: " + (event.isClosed() ? "CLOSED" : "ACTIVE"));
        System.out.println();
    }

    private void showEventState() {
        EventSummary picked = pickEvent(false, "view");
        if (picked == null) {
            return;
        }
        printEventState(engine.getEventState(picked.getId()));
    }

    private void buyShares() {
        EventSummary picked = pickEvent(true, "buy shares in");
        if (picked == null) {
            return;
        }
        printEventState(engine.getEventState(picked.getId()));

        List<String> optionNames = picked.getOptionNames();
        System.out.println("Choose an option to buy:");
        for (int i = 0; i < optionNames.size(); i++) {
            System.out.println((i + 1) + ") " + optionNames.get(i));
        }
        int optionChoice = readMenuChoice(1, optionNames.size());
        int quantity = readPositiveInt("Enter the quantity of shares to buy: ");

        try {
            PurchaseResult result = engine.buyShares(picked.getId(), optionChoice - 1, quantity);
            System.out.println();
            System.out.printf("Purchase successful. Shares cost: %.2f, commission: %.2f, total paid: %.2f%n",
                    result.getSharesCost(), result.getCommissionPaid(), result.getTotalPaid());
            printEventState(result.getUpdatedState());
        } catch (InvalidOperationException e) {
            System.out.println("Could not complete the purchase: " + e.getMessage());
        }
    }

    private void closeEvent() {
        EventSummary picked = pickEvent(true, "close");
        if (picked == null) {
            return;
        }
        printEventState(engine.getEventState(picked.getId()));

        List<String> optionNames = picked.getOptionNames();
        System.out.println("Choose the winning option:");
        for (int i = 0; i < optionNames.size(); i++) {
            System.out.println((i + 1) + ") " + optionNames.get(i));
        }
        int winnerChoice = readMenuChoice(1, optionNames.size());

        try {
            EventStateSummary finalState = engine.closeEvent(picked.getId(), winnerChoice - 1);
            System.out.println();
            System.out.println("Event closed.");
            printEventState(finalState);
        } catch (InvalidOperationException e) {
            System.out.println("Could not close the event: " + e.getMessage());
        }
    }

    private void saveState() {
        System.out.print("Enter the full path (without extension) to save the state to: ");
        String path = scanner.nextLine().trim();
        LoadResult result = engine.saveState(path);
        System.out.println(result.getMessage());
    }

    private void loadState() {
        System.out.print("Enter the full path (without extension) to load the state from: ");
        String path = scanner.nextLine().trim();
        LoadResult result = engine.loadState(path);
        System.out.println(result.getMessage());
    }

    // returns null when there's nothing to pick from
    private EventSummary pickEvent(boolean activeOnly, String actionDescription) {
        List<EventSummary> all = engine.getAllEvents();
        List<EventSummary> candidates = activeOnly
                ? all.stream().filter(e -> !e.isClosed()).collect(Collectors.toList())
                : all;

        if (candidates.isEmpty()) {
            System.out.println(activeOnly
                    ? "There are no active events right now."
                    : "No events are currently loaded. Use option 1 to load an events file.");
            return null;
        }

        System.out.println();
        System.out.println("Choose an event to " + actionDescription + ":");
        for (int i = 0; i < candidates.size(); i++) {
            EventSummary event = candidates.get(i);
            System.out.println((i + 1) + ") " + event.getName() + " - " + event.getDescription());
        }
        int choice = readMenuChoice(1, candidates.size());
        return candidates.get(choice - 1);
    }

    private void printEventState(EventStateSummary state) {
        System.out.println();
        System.out.println("=== " + state.getName() + " (Event #" + state.getId() + ") ===");
        System.out.println("Options:");
        for (OptionInfo option : state.getOptions()) {
            System.out.printf("  %s: price %.2f, total shares bought %d%n",
                    option.getName(), option.getCurrentPrice(), option.getTotalSharesBought());
        }
        System.out.printf("Account balance: %.2f%n", state.getAccountBalance());
        System.out.printf("Total commission collected: %.2f%n", state.getTotalCommissionCollected());
        System.out.println(state.isClosed()
                ? "Status: CLOSED. Winning option: " + state.getWinningOptionName()
                : "Status: ACTIVE");

        System.out.println("Trade history (most recent first):");
        List<TradeRecordInfo> history = state.getHistoryMostRecentFirst();
        if (history.isEmpty()) {
            System.out.println("  (no trades yet)");
        } else {
            for (TradeRecordInfo trade : history) {
                System.out.printf("  %s: %d shares, paid %.2f%n",
                        trade.getOptionName(), trade.getQuantity(), trade.getPricePaid());
            }
        }
        System.out.println();
    }

    private String displayCommissionType(String enumName) {
        return enumName.toLowerCase().replace('_', '-');
    }

    private int readMenuChoice(int min, int max) {
        while (true) {
            System.out.print("Enter your choice (" + min + "-" + max + "): ");
            String line = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(line);
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.println("Please enter a number between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid whole number.");
            }
        }
    }

    private int readPositiveInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(line);
                if (value > 0) {
                    return value;
                }
                System.out.println("Please enter a positive whole number.");
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid whole number.");
            }
        }
    }
}
