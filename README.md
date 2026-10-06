# data-transfer-service

Generic data stores and a service that transfers elements from a source to a target, written in Java with Maven.

## Run

Requirements: JDK 17 or newer and Maven 3.6.3 or newer.

```sh
mvn clean test exec:java
```

The demo prints:

```text
orders -> archive: 3 elements
  archive: [order-1, order-2, order-3]
Transfer from 'orders' to 'warehouse' failed, elements written: 2
  cause: Target 'warehouse' is unavailable
  warehouse: [order-1, order-2]
```

The second transfer fails on purpose after two writes, and the two elements already written stay in the target.

## Design

- `DataStore<T>` is a named store of elements of type `T`.
- `Source<T>` opens a finite stream of elements.
- `Target<T>` writes one element.
- `TransferSpec<T>` plugs a source into a target. The source may produce a subtype of `T`, and the target may accept a supertype.
- `TransferService` executes a transfer spec and returns the number of elements written.

A connector implements `Source`, `Target`, or both. The service depends only on these interfaces, so a new connector needs no changes to it.

`InMemoryStore<T>` is a sample connector that is both a source and a target. Reading takes a snapshot, writing appends. It rejects null elements and is not thread-safe.

`FailingTarget<T>` in the demo wraps another target and fails after a fixed number of writes, to show a partially written target.

```text
src/main/java/example/transfer/
├── DataStore, Source, Target       contracts
├── TransferSpec, TransferService   plugging and execution
├── TransferException
├── connector/InMemoryStore
└── demo/Main, FailingTarget        sample definition and execution
```

## Failures

The service writes elements one by one in encounter order and stops at the first failure. Opening, reading, or closing the source and writing to the target can fail; an `IOException` or `RuntimeException` from any of these steps becomes a checked `TransferException`. It keeps the original cause and the number of elements written before the failure. An opened source stream is always closed.

Elements already written stay in the target: there is no rollback or retry. A null transfer spec is rejected with a `NullPointerException` before the transfer starts.

## Tests

`TransferServiceTest` covers ordering, an empty source, generic variance, closing the source, and failures when opening, reading, writing, and closing, including the partial count and the error message.

## Scope

Transfers are synchronous and sequential. Batching, transactions, retries, and concurrent access are out of scope.
