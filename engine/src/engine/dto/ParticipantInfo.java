package engine.dto;

import java.util.List;

public class ParticipantInfo {
    private final String userName;
    private final List<OptionHoldingInfo> holdings;

    public ParticipantInfo(String userName, List<OptionHoldingInfo> holdings) {
        this.userName = userName;
        this.holdings = holdings;
    }

    public String getUserName() {
        return userName;
    }

    public List<OptionHoldingInfo> getHoldings() {
        return holdings;
    }
}
