package server.servlets;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import server.constants.Constants;

import java.io.IOException;

/**
 * GET /api/transactions?from=N. Delta fetching of the logged-in user's account lines: only the lines after line N
 * are returned, together with the new last line number for the client's next request.
 */
@WebServlet(name = "TransactionsServlet", urlPatterns = "/api/transactions")
public class TransactionsServlet extends ApiServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        respond(request, response, (engine, username) -> {
            String from = request.getParameter(Constants.FROM_VERSION);
            return engine.getTransactions(username, from == null ? 0 : intParameter(request, Constants.FROM_VERSION));
        });
    }
}
