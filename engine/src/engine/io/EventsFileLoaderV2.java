package engine.io;

import engine.exceptions.FileValidationException;

public interface EventsFileLoaderV2 {

    LoadedGameState load(String path) throws FileValidationException;
}
