package engine.model.trading;

import java.io.Serializable;

// kept as an interface so exercise 2 can add Order Book alongside LMSR
public interface TradingMethod extends Serializable {

    double getPrice(int optionIndex);

    double costOfPurchase(int optionIndex, int quantity);

    void commitPurchase(int optionIndex, int quantity);

    double initialSubsidy();
}
