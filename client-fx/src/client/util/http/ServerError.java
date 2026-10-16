package client.util.http;

/** A request the server rejected (the message is the server's explanation) or a connection problem. */
public class ServerError {

    private final int status;
    private final String message;

    public ServerError(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public boolean isUnauthorized() {
        return status == 401;
    }

    public String getMessage() {
        return message;
    }
}
