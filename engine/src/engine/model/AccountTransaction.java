package engine.model;

public record AccountTransaction(int serial, String time, String description, double amount, double balanceAfter) {
}
