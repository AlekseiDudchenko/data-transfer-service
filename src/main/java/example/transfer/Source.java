package example.transfer;

import java.io.IOException;
import java.util.stream.Stream;

public interface Source<T> extends DataStore<T> {
    Stream<T> open() throws IOException;
}
