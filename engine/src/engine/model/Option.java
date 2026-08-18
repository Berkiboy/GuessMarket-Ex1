package engine.model;

import java.io.Serializable;

public class Option implements Serializable {

    private final String name;
    private int totalSharesBought;

    public Option(String name) {
        this.name = name;
    }

    void addShares(int quantity) {
        totalSharesBought += quantity;
    }

    public String getName() {
        return name;
    }

    public int getTotalSharesBought() {
        return totalSharesBought;
    }
}
