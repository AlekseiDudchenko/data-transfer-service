package example.transfer;

import example.transfer.connector.InMemoryStore;
import org.junit.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class TransferServiceTest {
    private final TransferService service = new TransferService();

    @Test
    public void copiesElementsInOrderAndCountsWrites() throws TransferException {
        InMemoryStore<String> source = new InMemoryStore<>("source", List.of("a", "b", "a"));
        InMemoryStore<String> target = new InMemoryStore<>("target", List.of("existing"));

        assertEquals(3, service.execute(new TransferSpec<>(source, target)));
        assertEquals(List.of("existing", "a", "b", "a"), target.snapshot());
        assertEquals(List.of("a", "b", "a"), source.snapshot());
    }

    @Test
    public void emptySourceDoesNotWrite() throws TransferException {
        InMemoryStore<String> source = new InMemoryStore<>("source", List.of());
        Target<String> target = target(element -> fail("Empty source must not cause a write"));

        assertEquals(0, service.execute(new TransferSpec<>(source, target)));
    }

    @Test
    public void acceptsSourceSubtypeAndTargetSupertype() throws TransferException {
        InMemoryStore<Integer> source = new InMemoryStore<>("source", List.of(1, 2));
        InMemoryStore<Object> target = new InMemoryStore<>("target", List.of());
        TransferSpec<Number> transferSpec = new TransferSpec<>(source, target);

        assertEquals(2, service.execute(transferSpec));
        assertEquals(List.of(1, 2), target.snapshot());
    }

    @Test
    public void closesSourceAfterSuccess() throws TransferException {
        AtomicBoolean closed = new AtomicBoolean();
        Source<String> source = source(() -> Stream.of("a").onClose(() -> closed.set(true)));

        assertEquals(1, service.execute(new TransferSpec<>(source, target(element -> {}))));
        assertTrue(closed.get());
    }

    @Test
    public void writeFailureStopsTransferPreservesCauseAndClosesSource() {
        AtomicBoolean closed = new AtomicBoolean();
        IOException cause = new IOException("Target unavailable");
        Source<String> source = source(() -> Stream.of("a", "b", "c").onClose(() -> closed.set(true)));
        InMemoryStore<String> written = new InMemoryStore<>("written", List.of());
        Target<String> target = target(element -> {
            if (element.equals("b")) throw cause;
            written.write(element);
        });

        TransferException failure = assertThrows(TransferException.class,
                () -> service.execute(new TransferSpec<>(source, target)));

        assertEquals(1, failure.transferredCount());
        assertSame(cause, failure.getCause());
        assertEquals("Transfer from 'test-source' to 'test-target' failed, elements written: 1",
                failure.getMessage());
        assertEquals(List.of("a"), written.snapshot());
        assertTrue(closed.get());
    }

    @Test
    public void sourceOpenFailureDoesNotWrite() {
        IOException cause = new IOException("Source unavailable");
        Source<String> source = source(() -> { throw cause; });
        Target<String> target = target(element -> fail("Failed source must not cause a write"));

        TransferException failure = assertThrows(TransferException.class,
                () -> service.execute(new TransferSpec<>(source, target)));

        assertEquals(0, failure.transferredCount());
        assertSame(cause, failure.getCause());
    }

    @Test
    public void nullSourceStreamIsReportedWithSourceName() {
        Source<String> source = source(() -> null);
        Target<String> target = target(element -> fail("Null stream must not cause a write"));

        TransferException failure = assertThrows(TransferException.class,
                () -> service.execute(new TransferSpec<>(source, target)));

        assertEquals(0, failure.transferredCount());
        assertEquals("Source 'test-source' returned a null stream", failure.getCause().getMessage());
    }

    @Test
    public void readFailureReportsPartialCountAndClosesSource() {
        AtomicBoolean closed = new AtomicBoolean();
        UncheckedIOException cause = new UncheckedIOException(new IOException("Read failed"));
        Source<String> source = source(() -> Stream.of("a", "b", "c")
                .map(element -> {
                    if (element.equals("b")) throw cause;
                    return element;
                }).onClose(() -> closed.set(true)));
        InMemoryStore<String> target = new InMemoryStore<>("target", List.of());

        TransferException failure = assertThrows(TransferException.class,
                () -> service.execute(new TransferSpec<>(source, target)));

        assertEquals(1, failure.transferredCount());
        assertSame(cause, failure.getCause());
        assertEquals(List.of("a"), target.snapshot());
        assertTrue(closed.get());
    }

    @Test
    public void sourceCloseFailureIsReportedEvenAfterAllWrites() {
        IllegalStateException cause = new IllegalStateException("Close failed");
        Source<String> source = source(() -> Stream.of("a").onClose(() -> { throw cause; }));
        InMemoryStore<String> target = new InMemoryStore<>("target", List.of());

        TransferException failure = assertThrows(TransferException.class,
                () -> service.execute(new TransferSpec<>(source, target)));

        assertEquals(1, failure.transferredCount());
        assertSame(cause, failure.getCause());
        assertEquals(List.of("a"), target.snapshot());
    }

    @FunctionalInterface
    private interface OpenStream {
        Stream<String> open() throws IOException;
    }

    @FunctionalInterface
    private interface WriteElement {
        void write(String element) throws IOException;
    }

    private static Source<String> source(OpenStream opener) {
        return new Source<>() {
            public String name() { return "test-source"; }
            public Stream<String> open() throws IOException { return opener.open(); }
        };
    }

    private static Target<String> target(WriteElement writer) {
        return new Target<>() {
            public String name() { return "test-target"; }
            public void write(String element) throws IOException { writer.write(element); }
        };
    }
}
