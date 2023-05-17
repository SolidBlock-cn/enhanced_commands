package pers.solid.ecmd.util.iterator;

import com.google.common.collect.Iterators;

import java.util.Iterator;

/**
 * An iterator that executes several times of another iterator. It is similar to {@link Iterators#partition(Iterator, int)}, but will not group each batch as a list, because it consumes more memory and calculation.
 * @param forward The iterator to be used.
 * @param batchSize In each {@link #next()}, how many times it will be executed.
 * @see com.google.common.collect.Iterators
 * @see com.google.common.collect.Iterators#partition(Iterator, int)
 */
public record BatchedIterator<T>(Iterator<T> forward, int batchSize) implements Iterator<T> {
  @Override
  public boolean hasNext() {
    return forward.hasNext();
  }

  @Override
  public T next() {
    T value = null;
    for (int i = 0; i < batchSize; i++) {
      if (forward.hasNext()) {
        value = forward.next();
      } else {
        break;
      }
    }
    return value;
  }
}
