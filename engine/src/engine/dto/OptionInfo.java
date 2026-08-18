package engine.dto;

public class OptionInfo {
    private final String name;
    private final double currentPrice;
    private final int totalSharesBought;

    public OptionInfo(String name, double currentPrice, int totalSharesBought) {
        this.name = name;
        this.currentPrice = currentPrice;
        this.totalSharesBought = totalSharesBought;
    }

    public String getName() {
        return name;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public int getTotalSharesBought() {
        return totalSharesBought;
    }
}
