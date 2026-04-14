package pers.solid.ecmd.util.iterator;

import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @see BatchedFilterIterator
 */
public record BatchedFilterIterable<T>(Iterable<@Nullable T> forward, int batchSize, Predicate<@Nullable T> predicate) implements Iterable<@Nullable T> {
  @Override
  public Iterator<@Nullable T> iterator() {
    return new BatchedFilterIterator<>(forward.iterator(), batchSize, predicate);
  }

  @Override
  public void forEach(Consumer<? super @Nullable T> action) {
    for (T t : forward) {
      if (predicate.test(t)) {
        action.accept(t);
      }
    }
  }
}
