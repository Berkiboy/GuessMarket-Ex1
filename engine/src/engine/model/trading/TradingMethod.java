package engine.model.trading;

import java.io.Serializable;

public interface TradingMethod extends Serializable {

    double getPrice(int optionIndex);

    double openingCost();
}
