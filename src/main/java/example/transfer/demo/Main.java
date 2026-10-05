package example.transfer.demo;

import example.transfer.Target;
import example.transfer.TransferException;
import example.transfer.TransferService;
import example.transfer.TransferSpec;
import example.transfer.connector.InMemoryStore;

import java.io.IOException;
import java.util.List;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        TransferService service = new TransferService();
        InMemoryStore<String> orders = new InMemoryStore<>("orders", List.of("order-1", "order-2", "order-3"));

        InMemoryStore<String> archive = new InMemoryStore<>("archive", List.of());
        run(service, new TransferSpec<>(orders, archive));
        System.out.println("  archive: " + archive.snapshot());

        run(service, new TransferSpec<>(orders, new UnstableTarget("warehouse", 2)));
    }

    private static void run(TransferService service, TransferSpec<?> transferSpec) {
        try {
            long count = service.execute(transferSpec);
            System.out.println(transferSpec.source().name() + " -> " + transferSpec.target().name()
                    + ": " + count + " elements");
        } catch (TransferException failure) {
            System.out.println(failure.getMessage());
            System.out.println("  cause: " + failure.getCause().getMessage());
        }
    }

    /** A sample target that becomes unavailable after a fixed number of writes. */
    private static final class UnstableTarget implements Target<String> {
        private final String name;
        private final int capacity;
        private int written;

        UnstableTarget(String name, int capacity) {
            this.name = name;
            this.capacity = capacity;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public void write(String element) throws IOException {
            if (written == capacity) {
                throw new IOException("Target '" + name + "' is unavailable");
            }
            written++;
        }
    }
}
