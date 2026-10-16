package server.servlets;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import server.constants.Constants;

/** GET /api/event/lmsr?id=N. Prices, totals and trade history of an LMSR event. */
@WebServlet(name = "LmsrStateServlet", urlPatterns = "/api/event/lmsr")
public class LmsrStateServlet extends ApiServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        respond(request, response,
                (engine, username) -> engine.getLmsrEventState(intParameter(request, Constants.EVENT_ID)));
    }
}
