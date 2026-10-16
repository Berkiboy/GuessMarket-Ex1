package server.servlets;

import engine.dto.Side;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Locale;
import server.constants.Constants;

/**
 * POST /api/event/order with parameters "id", "option" (0-based), "side" (BUY/SELL), "quantity" and "price".
 * Submits an order to an Order Book event.
 */
@WebServlet(name = "SubmitOrderServlet", urlPatterns = "/api/event/order")
public class SubmitOrderServlet extends ApiServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        respond(request, response, (engine, username) -> {
            Side side;
            try {
                side = Side.valueOf(requiredParameter(request, Constants.SIDE).toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("\"side\" must be BUY or SELL.");
            }
            return engine.submitOrder(username, intParameter(request, Constants.EVENT_ID),
                    intParameter(request, Constants.OPTION), side,
                    intParameter(request, Constants.QUANTITY), doubleParameter(request, Constants.PRICE));
        });
    }
}
