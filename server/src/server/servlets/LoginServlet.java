package server.servlets;

import engine.IGuessMarketEngine;
import engine.exceptions.InvalidOperationException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import server.constants.Constants;
import server.utils.ServletUtils;
import server.utils.SessionUtils;

import java.io.IOException;
import java.util.Map;

/** POST /login with parameter "username". Starts a session for the user; fails if the name is taken. */
@WebServlet(name = "LoginServlet", urlPatterns = "/login")
public class LoginServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        String username = request.getParameter(Constants.USERNAME);
        IGuessMarketEngine engine = ServletUtils.getEngine(getServletContext());

        String current = SessionUtils.getUsername(request);
        if (current != null && current.equalsIgnoreCase(username == null ? "" : username.trim())
                && engine.isLoggedIn(current)) {
            ServletUtils.writeJson(response, HttpServletResponse.SC_OK, Map.of("username", current));
            return;
        }

        try {
            engine.login(username);
        } catch (InvalidOperationException e) {
            ServletUtils.writeError(response, HttpServletResponse.SC_CONFLICT, e.getMessage());
            return;
        }
        if (current != null) {
            engine.logout(current);
        }
        String name = engine.getAllUsers().stream()
                .filter(u -> u.getName().equalsIgnoreCase(username.trim()))
                .findFirst().orElseThrow().getName();
        HttpSession session = request.getSession(true);
        session.setAttribute(Constants.USERNAME, name);
        session.setMaxInactiveInterval(Constants.SESSION_TIMEOUT_SECONDS);
        ServletUtils.writeJson(response, HttpServletResponse.SC_OK, Map.of("username", name));
    }
}
