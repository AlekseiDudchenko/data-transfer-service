package example.transfer.demo;

import example.transfer.Target;

import java.io.IOException;
import java.util.Objects;

/** A sample target that fails once it has accepted a fixed number of elements. */
final class FailingTarget<T> implements Target<T> {
    private final String name;
    private final int failAfter;
    private int written;

    FailingTarget(String name, int failAfter) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.failAfter = failAfter;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public void write(T element) throws IOException {
        if (written == failAfter) {
            throw new IOException("Target '" + name + "' is unavailable");
        }
        written++;
    }
}
