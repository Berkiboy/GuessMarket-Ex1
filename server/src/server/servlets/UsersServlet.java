package server.servlets;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** GET /api/users. All users with their balance and whether they are a market maker. */
@WebServlet(name = "UsersServlet", urlPatterns = "/api/users")
public class UsersServlet extends ApiServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        respond(request, response, (engine, username) -> engine.getAllUsers());
    }
}
