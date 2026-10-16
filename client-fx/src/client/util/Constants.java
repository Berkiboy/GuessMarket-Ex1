package client.util;

import com.google.gson.Gson;

public final class Constants {

    // Server resources locations. The client assumes the WAR GuessMarket.war is deployed on localhost:8080;
    // another location can be given with -Dgm.server=http://host:port/GuessMarket
    public static final String BASE_DOMAIN = "localhost";
    private static final String BASE_URL = "http://" + BASE_DOMAIN + ":8080";
    private static final String CONTEXT_PATH = "/GuessMarket";
    public static final String FULL_SERVER_PATH = System.getProperty("gm.server", BASE_URL + CONTEXT_PATH);

    public static final String LOGIN_PAGE = FULL_SERVER_PATH + "/login";
    public static final String LOGOUT = FULL_SERVER_PATH + "/logout";
    public static final String DATA_VERSION = FULL_SERVER_PATH + "/api/version";
    public static final String EVENTS_LIST = FULL_SERVER_PATH + "/api/events";
    public static final String USERS_LIST = FULL_SERVER_PATH + "/api/users";
    public static final String MY_ACCOUNT = FULL_SERVER_PATH + "/api/me";
    public static final String TRANSACTIONS = FULL_SERVER_PATH + "/api/transactions";
    public static final String DEPOSIT = FULL_SERVER_PATH + "/api/deposit";
    public static final String UPLOAD_FILE = FULL_SERVER_PATH + "/api/upload";
    public static final String LMSR_STATE = FULL_SERVER_PATH + "/api/event/lmsr";
    public static final String ORDER_BOOK_STATE = FULL_SERVER_PATH + "/api/event/order-book";
    public static final String OPEN_EVENT = FULL_SERVER_PATH + "/api/event/open";
    public static final String CLOSE_EVENT = FULL_SERVER_PATH + "/api/event/close";
    public static final String BUY_SHARES = FULL_SERVER_PATH + "/api/event/buy";
    public static final String SUBMIT_ORDER = FULL_SERVER_PATH + "/api/event/order";

    // Pull rate of the refreshers (milliseconds)
    public static final int REFRESH_RATE = 500;

    public static final Gson GSON_INSTANCE = new Gson();

    private Constants() {
    }
}
