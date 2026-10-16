package client.util.http;

import client.util.Constants;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import engine.dto.EventStateSummary;
import engine.dto.EventSummary;
import engine.dto.LoadResult;
import engine.dto.OrderBookEventStateSummary;
import engine.dto.OrderSubmitResult;
import engine.dto.PurchaseResult;
import engine.dto.Side;
import engine.dto.TransactionsDelta;
import engine.dto.UserDetail;
import engine.dto.UserSummary;
import javafx.application.Platform;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.function.Consumer;

/**
 * The client's gateway to the Guess Market server. Every engine call of Exercise 2 is an HTTP request here,
 * executed asynchronously by OkHttp. The JSON answer is read back into the engine's DTO classes and handed to
 * the given consumer on the JavaFX application thread (Platform.runLater), so callers may update the UI directly.
 */
public class ServerApi {

    private static final MediaType XML = MediaType.parse("text/xml");

    public void login(String username, Consumer<String> onSuccess, Consumer<ServerError> onError) {
        RequestBody body = new FormBody.Builder().add("username", username).build();
        this.<JsonObject>post(Constants.LOGIN_PAGE, body, JsonObject.class,
                json -> onSuccess.accept(json.get("username").getAsString()), onError);
    }

    public void logout(Runnable onDone) {
        this.<JsonObject>post(Constants.LOGOUT, new FormBody.Builder().build(), JsonObject.class,
                json -> onDone.run(), error -> onDone.run());
    }

    /** Used while the application closes, when there is no time left for an asynchronous answer. */
    public void logoutSync() {
        Request request = new Request.Builder().url(Constants.LOGOUT).post(new FormBody.Builder().build()).build();
        try (Response ignored = HttpClientUtil.runSync(request)) {
            // nothing to read
        } catch (IOException ignored) {
            // The server may already be down; the session will expire on its own.
        }
    }

    public void dataVersion(Consumer<Long> onSuccess, Consumer<ServerError> onError) {
        this.<JsonObject>get(url(Constants.DATA_VERSION), JsonObject.class,
                json -> onSuccess.accept(json.get("version").getAsLong()), onError);
    }

    public void events(Consumer<List<EventSummary>> onSuccess, Consumer<ServerError> onError) {
        get(url(Constants.EVENTS_LIST), new TypeToken<List<EventSummary>>() { }.getType(), onSuccess, onError);
    }

    public void users(Consumer<List<UserSummary>> onSuccess, Consumer<ServerError> onError) {
        get(url(Constants.USERS_LIST), new TypeToken<List<UserSummary>>() { }.getType(), onSuccess, onError);
    }

    public void myAccount(Consumer<UserDetail> onSuccess, Consumer<ServerError> onError) {
        get(url(Constants.MY_ACCOUNT), UserDetail.class, onSuccess, onError);
    }

    public void transactions(int fromVersion, Consumer<TransactionsDelta> onSuccess, Consumer<ServerError> onError) {
        HttpUrl url = url(Constants.TRANSACTIONS).newBuilder()
                .addQueryParameter("from", String.valueOf(fromVersion))
                .build();
        get(url, TransactionsDelta.class, onSuccess, onError);
    }

    public void deposit(double amount, Consumer<UserDetail> onSuccess, Consumer<ServerError> onError) {
        RequestBody body = new FormBody.Builder().add("amount", String.valueOf(amount)).build();
        post(Constants.DEPOSIT, body, UserDetail.class, onSuccess, onError);
    }

    public void lmsrState(int eventId, Consumer<EventStateSummary> onSuccess, Consumer<ServerError> onError) {
        get(eventUrl(Constants.LMSR_STATE, eventId), EventStateSummary.class, onSuccess, onError);
    }

    public void orderBookState(int eventId, Consumer<OrderBookEventStateSummary> onSuccess,
                               Consumer<ServerError> onError) {
        get(eventUrl(Constants.ORDER_BOOK_STATE, eventId), OrderBookEventStateSummary.class, onSuccess, onError);
    }

    public void openEvent(int eventId, Runnable onSuccess, Consumer<ServerError> onError) {
        RequestBody body = new FormBody.Builder().add("id", String.valueOf(eventId)).build();
        this.<JsonObject>post(Constants.OPEN_EVENT, body, JsonObject.class, json -> onSuccess.run(), onError);
    }

    public void closeEvent(int eventId, int winningOptionIndex, Runnable onSuccess, Consumer<ServerError> onError) {
        RequestBody body = new FormBody.Builder()
                .add("id", String.valueOf(eventId))
                .add("winner", String.valueOf(winningOptionIndex))
                .build();
        this.<JsonObject>post(Constants.CLOSE_EVENT, body, JsonObject.class, json -> onSuccess.run(), onError);
    }

    public void buyLmsr(int eventId, int optionIndex, int quantity, Consumer<PurchaseResult> onSuccess,
                        Consumer<ServerError> onError) {
        RequestBody body = new FormBody.Builder()
                .add("id", String.valueOf(eventId))
                .add("option", String.valueOf(optionIndex))
                .add("quantity", String.valueOf(quantity))
                .build();
        post(Constants.BUY_SHARES, body, PurchaseResult.class, onSuccess, onError);
    }

    public void submitOrder(int eventId, int optionIndex, Side side, int quantity, double price,
                            Consumer<OrderSubmitResult> onSuccess, Consumer<ServerError> onError) {
        RequestBody body = new FormBody.Builder()
                .add("id", String.valueOf(eventId))
                .add("option", String.valueOf(optionIndex))
                .add("side", side.name())
                .add("quantity", String.valueOf(quantity))
                .add("price", String.valueOf(price))
                .build();
        post(Constants.SUBMIT_ORDER, body, OrderSubmitResult.class, onSuccess, onError);
    }

    /** Uploads an events file as a multipart body (part name "file"). */
    public void uploadFile(File file, Consumer<LoadResult> onSuccess, Consumer<ServerError> onError) {
        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(), RequestBody.create(file, XML))
                .build();
        post(Constants.UPLOAD_FILE, body, LoadResult.class, onSuccess, onError);
    }

    private static HttpUrl url(String url) {
        return HttpUrl.parse(url);
    }

    private static HttpUrl eventUrl(String url, int eventId) {
        return url(url).newBuilder().addQueryParameter("id", String.valueOf(eventId)).build();
    }

    private <T> void get(HttpUrl url, Type type, Consumer<T> onSuccess, Consumer<ServerError> onError) {
        send(new Request.Builder().url(url).get().build(), type, onSuccess, onError);
    }

    private <T> void post(String url, RequestBody body, Type type, Consumer<T> onSuccess,
                          Consumer<ServerError> onError) {
        send(new Request.Builder().url(url).post(body).build(), type, onSuccess, onError);
    }

    private <T> void send(Request request, Type type, Consumer<T> onSuccess, Consumer<ServerError> onError) {
        HttpClientUtil.runAsync(request, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Platform.runLater(() -> onError.accept(new ServerError(0, "Cannot reach the Guess Market server at "
                        + Constants.FULL_SERVER_PATH + ". Make sure Tomcat is running and the application is deployed.")));
            }

            @Override
            public void onResponse(Call call, Response response) {
                try (ResponseBody responseBody = response.body()) {
                    String json = responseBody == null ? "" : responseBody.string();
                    if (response.code() != 200) {
                        ServerError error = new ServerError(response.code(), errorMessage(response.code(), json));
                        Platform.runLater(() -> onError.accept(error));
                        return;
                    }
                    T value = Constants.GSON_INSTANCE.fromJson(json, type);
                    Platform.runLater(() -> onSuccess.accept(value));
                } catch (IOException | JsonParseException e) {
                    Platform.runLater(() -> onError.accept(
                            new ServerError(response.code(), "The server sent an unexpected answer.")));
                }
            }
        });
    }

    private String errorMessage(int status, String body) {
        try {
            JsonObject json = Constants.GSON_INSTANCE.fromJson(body, JsonObject.class);
            if (json != null && json.has("error")) {
                return json.get("error").getAsString();
            }
        } catch (JsonParseException ignored) {
            // Not our JSON (for example Tomcat's HTML 404 page when the WAR is not deployed).
        }
        if (status == 404) {
            return "The Guess Market application was not found at " + Constants.FULL_SERVER_PATH
                    + ". Is the WAR deployed?";
        }
        return "The server answered with HTTP status " + status + ".";
    }
}
