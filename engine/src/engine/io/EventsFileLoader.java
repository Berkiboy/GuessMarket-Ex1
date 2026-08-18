package engine.io;

import engine.exceptions.FileValidationException;
import engine.model.Event;

import java.util.List;

public interface EventsFileLoader {

    List<Event> load(String path) throws FileValidationException;
}
