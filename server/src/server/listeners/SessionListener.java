package server.listeners;

import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import server.constants.Constants;
import server.utils.ServletUtils;
import server.utils.SessionUtils;

/** When a client disappears without logging out, its session expires and the user name becomes free again. */
@WebListener
public class SessionListener implements HttpSessionListener {

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        Object username = event.getSession().getAttribute(Constants.USERNAME);
        if (username != null) {
            ServletUtils.getEngine(event.getSession().getServletContext()).logout((String) username);
        }
    }
}
