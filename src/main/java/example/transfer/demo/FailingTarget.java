package example.transfer.demo;

import example.transfer.Target;

import java.io.IOException;
import java.util.Objects;

/**
 * A sample target decorator that passes a fixed number of elements to another target and then
 * fails as if that target became unavailable. Elements passed before the failure stay there.
 */
final class FailingTarget<T> implements Target<T> {
    private final Target<? super T> delegate;
    private final int failAfter;
    private int written;

    FailingTarget(Target<? super T> delegate, int failAfter) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        if (failAfter < 0) {
            throw new IllegalArgumentException("failAfter must not be negative");
        }
        this.failAfter = failAfter;
    }

    @Override
    public String name() {
        return delegate.name();
    }

    @Override
    public void write(T element) throws IOException {
        if (written == failAfter) {
            throw new IOException("Target '" + name() + "' is unavailable");
        }
        delegate.write(element);
        written++;
    }
}
