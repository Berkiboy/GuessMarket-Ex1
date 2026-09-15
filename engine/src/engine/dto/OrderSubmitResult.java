package engine.dto;

import java.util.List;

public class OrderSubmitResult {
    private final List<FillInfo> fills;
    private final OrderBookEventStateSummary updatedState;

    public OrderSubmitResult(List<FillInfo> fills, OrderBookEventStateSummary updatedState) {
        this.fills = fills;
        this.updatedState = updatedState;
    }

    public List<FillInfo> getFills() {
        return fills;
    }

    public OrderBookEventStateSummary getUpdatedState() {
        return updatedState;
    }
}
