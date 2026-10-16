package server.servlets;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/** GET /api/version. A cheap counter the clients pull to know whether anything changed since their last fetch. */
@WebServlet(name = "VersionServlet", urlPatterns = "/api/version")
public class VersionServlet extends ApiServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        respond(request, response, (engine, username) -> Map.of("version", engine.getDataVersion()));
    }
}
