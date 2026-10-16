package engine.io;

import engine.exceptions.FileValidationException;

import java.io.InputStream;
import java.util.List;

public interface EventsFileLoader {

    /**
     * Parses and validates an events file. The content is read straight from the stream and is never stored.
     *
     * @param fileName the original name of the uploaded file (used for the extension check and messages)
     */
    List<EventDefinition> load(String fileName, InputStream content) throws FileValidationException;
}
