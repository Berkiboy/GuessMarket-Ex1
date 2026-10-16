package server.servlets;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** GET /api/events. A summary of every event in the system. */
@WebServlet(name = "EventsServlet", urlPatterns = "/api/events")
public class EventsServlet extends ApiServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        respond(request, response, (engine, username) -> engine.getAllEvents());
    }
}
