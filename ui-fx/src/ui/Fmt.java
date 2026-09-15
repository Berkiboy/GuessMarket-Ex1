package ui;

public final class Fmt {

    private Fmt() {
    }

    public static String money(double value) {
        return String.format("%.2f", value);
    }

    public static String price(Double value) {
        return value == null ? "-" : String.format("%.2f", value);
    }

    public static String statusLabel(String status) {
        return switch (status) {
            case "NOT_STARTED" -> "Not started";
            case "ACTIVE" -> "Active";
            case "CLOSED" -> "Closed";
            default -> status;
        };
    }

    public static String commissionLabel(String commissionType) {
        return commissionType.toLowerCase().replace('_', '-');
    }
}
