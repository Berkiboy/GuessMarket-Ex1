package client.util.http;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Keeps the cookies (the server's JSESSIONID) per domain and sends them back on every request to that domain. */
public class SimpleCookieManager implements CookieJar {

    private final Map<String, Map<String, Cookie>> cookiesByDomain = new HashMap<>();

    @Override
    public synchronized List<Cookie> loadForRequest(HttpUrl httpUrl) {
        Map<String, Cookie> cookies = cookiesByDomain.get(httpUrl.host());
        return cookies == null ? List.of() : new ArrayList<>(cookies.values());
    }

    @Override
    public synchronized void saveFromResponse(HttpUrl httpUrl, List<Cookie> responseCookies) {
        Map<String, Cookie> cookies = cookiesByDomain.computeIfAbsent(httpUrl.host(), host -> new HashMap<>());
        for (Cookie cookie : responseCookies) {
            cookies.put(cookie.name(), cookie);
        }
    }

    public synchronized void removeCookiesOf(String domain) {
        cookiesByDomain.remove(domain);
    }
}
