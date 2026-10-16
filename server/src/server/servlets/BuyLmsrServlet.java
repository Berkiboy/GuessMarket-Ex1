package server.servlets;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import server.constants.Constants;

/** POST /api/event/buy with parameters "id", "option" (0-based) and "quantity". Buys LMSR shares. */
@WebServlet(name = "BuyLmsrServlet", urlPatterns = "/api/event/buy")
public class BuyLmsrServlet extends ApiServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        respond(request, response, (engine, username) -> engine.buyLmsrShares(username,
                intParameter(request, Constants.EVENT_ID), intParameter(request, Constants.OPTION),
                intParameter(request, Constants.QUANTITY)));
    }
}
