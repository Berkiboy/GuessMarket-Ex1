package server.servlets;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** GET /api/me. The logged-in user's balance, account transactions and event participations. */
@WebServlet(name = "MyAccountServlet", urlPatterns = "/api/me")
public class MyAccountServlet extends ApiServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        respond(request, response, (engine, username) -> engine.getUserDetail(username));
    }
}
