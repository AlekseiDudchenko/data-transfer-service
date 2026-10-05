package example.transfer;

import java.util.Objects;

/** A source and target plugged together with a compatible element type. */
public record TransferSpec<T>(Source<? extends T> source, Target<? super T> target) {
    public TransferSpec {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(target, "target must not be null");
    }
}
