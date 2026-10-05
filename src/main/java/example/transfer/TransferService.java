package example.transfer;

import java.io.IOException;
import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Stream;

/** Copies elements from a {@link Source} to a {@link Target}, one element at a time. */
public final class TransferService {
    /**
     * Copies every element of the source to the target in encounter order.
     *
     * <p>The source stream is closed when the transfer ends, whether it succeeds or fails. The
     * transfer stops at the first failure, and writes that already succeeded are not rolled back.
     *
     * @param transferSpec the source and target to transfer between
     * @param <T> the element type
     * @return the number of elements written to the target
     * @throws NullPointerException if {@code transferSpec} is null
     * @throws TransferException if opening, reading, writing or closing fails with an {@link
     *     IOException} or {@link RuntimeException}, including when the source returns a null
     *     stream; {@link TransferException#transferredCount()} gives the number of writes that
     *     succeeded before the failure
     */
    public <T> long execute(TransferSpec<T> transferSpec) throws TransferException {
        Objects.requireNonNull(transferSpec, "transferSpec must not be null");

        long transferredCount = 0;
        try {
            Stream<? extends T> elements = transferSpec.source().open();
            Objects.requireNonNull(elements, () -> "Source '" + transferSpec.source().name() + "' returned a null stream");

            try (elements) {
                Iterator<? extends T> iterator = elements.iterator();
                while (iterator.hasNext()) {
                    transferSpec.target().write(iterator.next());
                    transferredCount++;
                }
                return transferredCount;
            }
        } catch (IOException | RuntimeException failure) {
            throw new TransferException(transferSpec, transferredCount, failure);
        }
    }
}
