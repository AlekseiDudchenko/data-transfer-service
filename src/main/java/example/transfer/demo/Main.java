package example.transfer.demo;

import example.transfer.TransferException;
import example.transfer.TransferService;
import example.transfer.TransferSpec;
import example.transfer.connector.InMemoryStore;

import java.util.List;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        TransferService service = new TransferService();
        InMemoryStore<String> orders = new InMemoryStore<>("orders", List.of("order-1", "order-2", "order-3", "order-4"));

        InMemoryStore<String> archive = new InMemoryStore<>("archive", List.of());
        executeAndPrint(service, new TransferSpec<>(orders, archive));
        System.out.println("  archive: " + archive.snapshot());

        InMemoryStore<String> warehouse = new InMemoryStore<>("warehouse", List.of());
        executeAndPrint(service, new TransferSpec<>(orders, new FailingTarget<>(warehouse, 2)));
        System.out.println("  warehouse: " + warehouse.snapshot());
    }

    private static void executeAndPrint(TransferService service, TransferSpec<?> transferSpec) {
        try {
            long count = service.execute(transferSpec);
            System.out.println(transferSpec.source().name() + " -> " + transferSpec.target().name()
                    + ": " + count + " elements");
        } catch (TransferException failure) {
            System.out.println(failure.getMessage());
            System.out.println("  cause: " + failure.getCause().getMessage());
        }
    }
}
