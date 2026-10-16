package engine.model;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Account {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private double balance;
    private final List<AccountTransaction> transactions = new ArrayList<>();

    public void deposit(double amount, String description) {
        apply(amount, description);
    }

    public void withdraw(double amount, String description) {
        apply(-amount, description);
    }

    public double getBalance() {
        return balance;
    }

    public List<AccountTransaction> getTransactions() {
        return transactions;
    }

    private void apply(double signedAmount, String description) {
        if (signedAmount == 0) {
            return;
        }
        balance += signedAmount;
        transactions.add(new AccountTransaction(transactions.size() + 1, LocalTime.now().format(TIME_FORMAT),
                description, signedAmount, balance));
    }
}
