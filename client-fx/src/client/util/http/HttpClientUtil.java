package client.util.http;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/** Holds the single OkHttpClient of the application (shared thread pool, cookie manager and configuration). */
public final class HttpClientUtil {

    private static final SimpleCookieManager COOKIE_MANAGER = new SimpleCookieManager();
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .cookieJar(COOKIE_MANAGER)
            .followRedirects(false)
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();

    private HttpClientUtil() {
    }

    /** Asynchronous call: runs on OkHttp's thread pool and reports back through the callback. */
    public static void runAsync(Request request, Callback callback) {
        Call call = HTTP_CLIENT.newCall(request);
        call.enqueue(callback);
    }

    /** Synchronous call on the calling thread (used only when the application is closing). */
    public static Response runSync(Request request) throws IOException {
        return HTTP_CLIENT.newCall(request).execute();
    }

    public static void removeCookiesOf(String domain) {
        COOKIE_MANAGER.removeCookiesOf(domain);
    }

    public static void shutdown() {
        HTTP_CLIENT.dispatcher().executorService().shutdown();
        HTTP_CLIENT.connectionPool().evictAll();
    }
}
