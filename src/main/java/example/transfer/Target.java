package example.transfer;

import java.io.IOException;

public interface Target<T> extends DataStore<T> {
    void write(T element) throws IOException;
}
