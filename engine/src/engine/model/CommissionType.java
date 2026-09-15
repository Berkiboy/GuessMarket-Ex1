package engine.model;

public enum CommissionType {
    ON_PURCHASE,
    ON_CLOSE;

    public static CommissionType fromXmlValue(String value) {
        if ("on-purchase".equalsIgnoreCase(value)) {
            return ON_PURCHASE;
        }
        if ("on-close".equalsIgnoreCase(value)) {
            return ON_CLOSE;
        }
        throw new IllegalArgumentException("Unknown commission type: " + value);
    }
}
