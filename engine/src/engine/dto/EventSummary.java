package engine.dto;

import java.util.List;

public class EventSummary {
    private final int id;
    private final String name;
    private final String description;
    private final int commissionPercent;
    private final String commissionType;
    private final List<String> optionNames;
    private final String status;
    private final String tradingMethodType;
    private final String marketMakerName;
    private final double contractBalance;
    private final String methodDetails;
    private final double maxOrderPrice;

    public EventSummary(int id, String name, String description, int commissionPercent, String commissionType,
                         List<String> optionNames, String status, String tradingMethodType, String marketMakerName,
                         double contractBalance, String methodDetails, double maxOrderPrice) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.commissionPercent = commissionPercent;
        this.commissionType = commissionType;
        this.optionNames = optionNames;
        this.status = status;
        this.tradingMethodType = tradingMethodType;
        this.marketMakerName = marketMakerName;
        this.contractBalance = contractBalance;
        this.methodDetails = methodDetails;
        this.maxOrderPrice = maxOrderPrice;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getCommissionPercent() {
        return commissionPercent;
    }

    public String getCommissionType() {
        return commissionType;
    }

    public List<String> getOptionNames() {
        return optionNames;
    }

    public String getStatus() {
        return status;
    }

    public String getTradingMethodType() {
        return tradingMethodType;
    }

    public String getMarketMakerName() {
        return marketMakerName;
    }

    public double getContractBalance() {
        return contractBalance;
    }

    public String getMethodDetails() {
        return methodDetails;
    }

    /** Highest price allowed in an Order Book order (d - 0.01); 0 for LMSR events. */
    public double getMaxOrderPrice() {
        return maxOrderPrice;
    }
}
