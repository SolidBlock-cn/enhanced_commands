package pers.solid.ecmd.util.iterator;

import com.google.common.collect.Iterators;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;

/**
 * An iterator that executes several times of another iterator. It is similar to {@link Iterators#partition(Iterator, int)}, but will not group each batch as a list, because it consumes more memory and calculation.
 *
 * @param forward   The iterator to be used.
 * @param batchSize In each {@link #next()}, how many times it will be executed.
 * @see com.google.common.collect.Iterators
 * @see com.google.common.collect.Iterators#partition(Iterator, int)
 */
public record BatchedIterator(Iterator<@Nullable Runnable> forward, int batchSize) implements Iterator<Runnable> {
  @Override
  public boolean hasNext() {
    return forward.hasNext();
  }

  @Override
  public Runnable next() {
    return () -> {
      for (int i = 0; i < batchSize; i++) {
        if (forward.hasNext()) {
          final Runnable next = forward.next();
          if (next != null) {
            next.run();
          }
        } else {
          break;
        }
      }
    };
  }
}
