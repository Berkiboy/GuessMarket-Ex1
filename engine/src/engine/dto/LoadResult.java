package engine.dto;

public class LoadResult {
    private final boolean success;
    private final String message;
    private final int eventsLoaded;

    private LoadResult(boolean success, String message, int eventsLoaded) {
        this.success = success;
        this.message = message;
        this.eventsLoaded = eventsLoaded;
    }

    public static LoadResult success(String message, int eventsLoaded) {
        return new LoadResult(true, message, eventsLoaded);
    }

    public static LoadResult failure(String reason) {
        return new LoadResult(false, reason, 0);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public int getEventsLoaded() {
        return eventsLoaded;
    }
}
