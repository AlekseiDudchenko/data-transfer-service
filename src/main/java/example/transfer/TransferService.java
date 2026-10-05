package example.transfer;

import java.io.IOException;
import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Stream;

public final class TransferService {
    /** Copies in encounter order, failing immediately without rolling back writes. */
    public <T> long execute(TransferSpec<T> transferSpec) throws TransferException {
        Objects.requireNonNull(transferSpec, "transferSpec must not be null");

        long transferredCount = 0;
        try {
            Stream<? extends T> elements = transferSpec.source().open();
            Objects.requireNonNull(elements, "Source must return a non-null stream");

            try (elements) {
                Iterator<? extends T> iterator = elements.iterator();
                while (iterator.hasNext()) {
                    transferSpec.target().write(iterator.next());
                    transferredCount++;
                }
                return transferredCount;
            }
        } catch (IOException | RuntimeException failure) {
            throw new TransferException(transferredCount, failure);
        }
    }
}
