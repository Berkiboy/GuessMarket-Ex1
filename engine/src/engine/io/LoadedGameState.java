package engine.io;

import engine.model.Event;
import engine.model.User;

import java.io.Serializable;
import java.util.List;

public class LoadedGameState implements Serializable {

    private final List<User> users;
    private final List<Event> events;

    public LoadedGameState(List<User> users, List<Event> events) {
        this.users = users;
        this.events = events;
    }

    public List<User> getUsers() {
        return users;
    }

    public List<Event> getEvents() {
        return events;
    }
}
