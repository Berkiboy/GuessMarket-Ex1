package server.servlets;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import server.constants.Constants;

/** POST /api/deposit with parameter "amount". Adds funds to the logged-in user's account. */
@WebServlet(name = "DepositServlet", urlPatterns = "/api/deposit")
public class DepositServlet extends ApiServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        respond(request, response, (engine, username) -> {
            engine.depositFunds(username, doubleParameter(request, Constants.AMOUNT));
            return engine.getUserDetail(username);
        });
    }
}
