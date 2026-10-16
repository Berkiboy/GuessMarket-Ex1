package engine.model.trading;

public interface TradingMethod {

    double getPrice(int optionIndex);

    double openingCost();

    double payoutPerShare();
}
