package server.servlets;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;
import server.constants.Constants;

/** POST /api/event/open with parameter "id". Only the event's market maker may open it. */
@WebServlet(name = "OpenEventServlet", urlPatterns = "/api/event/open")
public class OpenEventServlet extends ApiServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        respond(request, response, (engine, username) -> {
            engine.openEvent(username, intParameter(request, Constants.EVENT_ID));
            return Map.of("message", "The event is now active.");
        });
    }
}
