package engine.io;

import engine.model.CommissionType;
import engine.model.trading.TradingMethod;

import java.util.List;

/** One validated GM-event from an uploaded file, before it is assigned a number and a market maker. */
public record EventDefinition(String name, String description, int commissionPercent, CommissionType commissionType,
                              List<String> optionNames, TradingMethod tradingMethod) {
}
