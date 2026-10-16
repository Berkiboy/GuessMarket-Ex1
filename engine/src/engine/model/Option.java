package engine.model;

public class Option {

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
