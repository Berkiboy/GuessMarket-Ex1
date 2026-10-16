package server.constants;

/** Names shared by the servlets: session attribute and request parameter names. */
public final class Constants {

    public static final String USERNAME = "username";
    public static final String EVENT_ID = "id";
    public static final String OPTION = "option";
    public static final String WINNER = "winner";
    public static final String QUANTITY = "quantity";
    public static final String PRICE = "price";
    public static final String SIDE = "side";
    public static final String AMOUNT = "amount";
    public static final String FROM_VERSION = "from";

    /** Seconds without any request after which a session (and its login) expires; clients poll far more often. */
    public static final int SESSION_TIMEOUT_SECONDS = 60;

    private Constants() {
    }
}
