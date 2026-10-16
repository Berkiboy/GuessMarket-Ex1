package server.servlets;

import engine.IGuessMarketEngine;
import engine.exceptions.InvalidOperationException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import server.utils.ServletUtils;
import server.utils.SessionUtils;

import java.io.IOException;

/**
 * Base class for the API servlets: resolves the logged-in user from the session, turns engine errors and bad
 * parameters into JSON error responses, and writes the handler's result as JSON.
 */
public abstract class ApiServlet extends HttpServlet {

    @FunctionalInterface
    protected interface Handler {
        Object handle(IGuessMarketEngine engine, String username) throws IOException, jakarta.servlet.ServletException;
    }

    protected void respond(HttpServletRequest request, HttpServletResponse response, Handler handler)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        String username = SessionUtils.getUsername(request);
        IGuessMarketEngine engine = ServletUtils.getEngine(getServletContext());
        if (username == null || !engine.isLoggedIn(username)) {
            ServletUtils.writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "You are not logged in. Please log in again.");
            return;
        }
        try {
            Object result = handler.handle(engine, username);
            ServletUtils.writeJson(response, HttpServletResponse.SC_OK, result);
        } catch (InvalidOperationException | IllegalArgumentException e) {
            ServletUtils.writeError(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (jakarta.servlet.ServletException e) {
            ServletUtils.writeError(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    protected static String requiredParameter(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required parameter \"" + name + "\".");
        }
        return value.trim();
    }

    protected static int intParameter(HttpServletRequest request, String name) {
        String value = requiredParameter(request, name);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("\"" + name + "\" must be a whole number, got \"" + value + "\".");
        }
    }

    protected static double doubleParameter(HttpServletRequest request, String name) {
        String value = requiredParameter(request, name);
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("\"" + name + "\" must be a number, got \"" + value + "\".");
        }
    }
}
