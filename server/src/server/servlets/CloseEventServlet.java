package server.servlets;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;
import server.constants.Constants;

/** POST /api/event/close with parameters "id" and "winner" (0-based option index). Market maker only. */
@WebServlet(name = "CloseEventServlet", urlPatterns = "/api/event/close")
public class CloseEventServlet extends ApiServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        respond(request, response, (engine, username) -> {
            engine.closeEvent(username, intParameter(request, Constants.EVENT_ID),
                    intParameter(request, Constants.WINNER));
            return Map.of("message", "The event is closed and the winners were paid.");
        });
    }
}
