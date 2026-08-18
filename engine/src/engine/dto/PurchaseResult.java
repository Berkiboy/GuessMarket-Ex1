package engine.dto;

public class PurchaseResult {
    private final double sharesCost;
    private final double commissionPaid;
    private final EventStateSummary updatedState;

    public PurchaseResult(double sharesCost, double commissionPaid, EventStateSummary updatedState) {
        this.sharesCost = sharesCost;
        this.commissionPaid = commissionPaid;
        this.updatedState = updatedState;
    }

    public double getSharesCost() {
        return sharesCost;
    }

    public double getCommissionPaid() {
        return commissionPaid;
    }

    public double getTotalPaid() {
        return sharesCost + commissionPaid;
    }

    public EventStateSummary getUpdatedState() {
        return updatedState;
    }
}
