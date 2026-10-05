package example.transfer.connector;

import example.transfer.Source;
import example.transfer.Target;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/** A sample connector that supports both source and target roles. Not thread-safe. */
public final class InMemoryStore<T> implements Source<T>, Target<T> {
    private final String name;
    private final List<T> elements;

    public InMemoryStore(String name, List<? extends T> initialElements) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(initialElements, "initialElements must not be null");
        this.elements = new ArrayList<>();
        initialElements.forEach(this::write);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Stream<T> open() {
        return snapshot().stream();
    }

    @Override
    public void write(T element) {
        elements.add(Objects.requireNonNull(element, () -> "Store '" + name + "' does not accept null elements"));
    }

    public List<T> snapshot() {
        return List.copyOf(elements);
    }
}
