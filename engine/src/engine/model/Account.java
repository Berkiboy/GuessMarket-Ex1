package engine.model;

import java.io.Serializable;

/**
 * A simple money balance. Event accounts are explicitly allowed to go
 * negative (it represents the Market Maker subsidizing the event).
 */
public class Account implements Serializable {

    private double balance;

    public void deposit(double amount) {
        balance += amount;
    }

    public void withdraw(double amount) {
        balance -= amount;
    }

    public double getBalance() {
        return balance;
    }
}
