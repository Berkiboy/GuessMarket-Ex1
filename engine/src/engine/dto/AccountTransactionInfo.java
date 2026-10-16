package engine.dto;

public class AccountTransactionInfo {
    private final int serial;
    private final String time;
    private final String description;
    private final double amount;
    private final double balanceAfter;

    public AccountTransactionInfo(int serial, String time, String description, double amount, double balanceAfter) {
        this.serial = serial;
        this.time = time;
        this.description = description;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
    }

    public int getSerial() {
        return serial;
    }

    public String getTime() {
        return time;
    }

    public String getDescription() {
        return description;
    }

    public double getAmount() {
        return amount;
    }

    public double getBalanceAfter() {
        return balanceAfter;
    }
}
