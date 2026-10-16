package server.utils;

import com.google.gson.Gson;
import engine.GuessMarketEngine;
import engine.IGuessMarketEngine;
import engine.io.impl.XmlEventsFileLoader;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

public final class ServletUtils {

    private static final String ENGINE_ATTRIBUTE = "guessMarketEngine";
    private static final Object ENGINE_LOCK = new Object();

    public static final Gson GSON = new Gson();

    private ServletUtils() {
    }

    /** One engine per web application, created on first use and shared by all servlets. */
    public static IGuessMarketEngine getEngine(ServletContext context) {
        synchronized (ENGINE_LOCK) {
            Object engine = context.getAttribute(ENGINE_ATTRIBUTE);
            if (engine == null) {
                engine = new GuessMarketEngine(new XmlEventsFileLoader());
                context.setAttribute(ENGINE_ATTRIBUTE, engine);
            }
            return (IGuessMarketEngine) engine;
        }
    }

    public static void writeJson(HttpServletResponse response, int status, Object body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().print(GSON.toJson(body));
    }

    public static void writeError(HttpServletResponse response, int status, String message) throws IOException {
        writeJson(response, status, Map.of("error", message == null ? "Unexpected error." : message));
    }
}
