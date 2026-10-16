package server.servlets;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import server.constants.Constants;

/** GET /api/event/order-book?id=N. The books, statistics and participants of an Order Book event. */
@WebServlet(name = "OrderBookStateServlet", urlPatterns = "/api/event/order-book")
public class OrderBookStateServlet extends ApiServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        respond(request, response,
                (engine, username) -> engine.getOrderBookEventState(intParameter(request, Constants.EVENT_ID)));
    }
}
